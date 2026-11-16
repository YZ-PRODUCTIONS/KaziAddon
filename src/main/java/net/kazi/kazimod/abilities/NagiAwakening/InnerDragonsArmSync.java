package net.kazi.kazimod.abilities.NagiAwakening;

import net.kazi.kazimod.init.KaziPacketHandler;
import net.kazi.kazimod.network.InnerDragonsArmPacket;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;

/** State changes and new observers receive the server's actual passive state. */
@Mod.EventBusSubscriber(modid = "kazimod")
public final class InnerDragonsArmSync {
    private InnerDragonsArmSync() { }

    public static void send(LivingEntity user, boolean transformed) {
        if (user.level.isClientSide) return;
        KaziPacketHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> user),
                packet(user, transformed));
    }

    @SubscribeEvent
    public static void tracking(PlayerEvent.StartTracking event) {
        if (!(event.getPlayer() instanceof ServerPlayerEntity) || !(event.getTarget() instanceof PlayerEntity)) return;
        LivingEntity target = (LivingEntity) event.getTarget();
        IAbilityData data = AbilityDataCapability.get(target);
        DragonStrikeAbility ability = data == null ? null : data.getPassiveAbility(DragonStrikeAbility.INSTANCE);
        // Also send false: a viewer may have missed restoration while outside tracking range.
        KaziPacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) event.getPlayer()),
                packet(target, ability != null && ability.hasTransformedArm(target)));
    }

    private static InnerDragonsArmPacket packet(LivingEntity user, boolean transformed) {
        return new InnerDragonsArmPacket(user.getId(), user.getUUID(), user.level.dimension().location(), transformed);
    }
}
