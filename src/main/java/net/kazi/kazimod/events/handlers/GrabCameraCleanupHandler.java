package net.kazi.kazimod.events.handlers;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.GrabEntityComponent;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.packets.server.entities.SUnpinCameraPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

@Mod.EventBusSubscriber(modid = "kazimod")
public class GrabCameraCleanupHandler {

    private static final String WAS_GRABBED_TAG = "kazimodWasGrabbed";

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayerEntity)) {
            return;
        }

        ServerPlayerEntity player = (ServerPlayerEntity) event.player;
        boolean hasGrabbedEffect = player.hasEffect(ModEffects.GRABBED.get());
        boolean isActuallyGrabbed = isActuallyGrabbed(player);
        boolean wasGrabbed = player.getPersistentData().getBoolean(WAS_GRABBED_TAG);

        if (hasGrabbedEffect && !isActuallyGrabbed) {
            player.removeEffect(ModEffects.GRABBED.get());
            hasGrabbedEffect = false;
        }

        if (!hasGrabbedEffect && wasGrabbed) {
            WyNetwork.sendTo(new SUnpinCameraPacket(), player);
        } else if (!hasGrabbedEffect && player.tickCount % 20 == 0) {
            // Safety net for stale camera pins that outlive the grabbed effect.
            WyNetwork.sendTo(new SUnpinCameraPacket(), player);
        }

        player.getPersistentData().putBoolean(WAS_GRABBED_TAG, hasGrabbedEffect || isActuallyGrabbed);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntityLiving() instanceof ServerPlayerEntity) {
            forceReleaseGrabState((ServerPlayerEntity) event.getEntityLiving());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            forceReleaseGrabState((ServerPlayerEntity) event.getPlayer());
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            forceReleaseGrabState((ServerPlayerEntity) event.getPlayer());
        }
    }

    private static void forceReleaseGrabState(ServerPlayerEntity player) {
        player.removeEffect(ModEffects.GRABBED.get());
        player.getPersistentData().putBoolean(WAS_GRABBED_TAG, false);
        WyNetwork.sendTo(new SUnpinCameraPacket(), player);
    }

    private static boolean isActuallyGrabbed(LivingEntity victim) {
        for (LivingEntity nearby : victim.level.getEntitiesOfClass(
                LivingEntity.class,
                victim.getBoundingBox().inflate(12.0D))) {

            if (nearby == victim) {
                continue;
            }

            IAbilityData data = AbilityDataCapability.getLazy(nearby).orElse(null);
            if (data == null) {
                continue;
            }

            for (IAbility ability : data.getEquippedAndPassiveAbilities()) {
                if (!ability.hasComponent(ModAbilityKeys.GRAB)) {
                    continue;
                }

                GrabEntityComponent grabComponent = (GrabEntityComponent) ability.getComponent(ModAbilityKeys.GRAB).orElse(null);
                if (grabComponent != null && grabComponent.hasGrabbedEntity() && grabComponent.getGrabbedEntity() == victim) {
                    return true;
                }
            }
        }

        return false;
    }
}
