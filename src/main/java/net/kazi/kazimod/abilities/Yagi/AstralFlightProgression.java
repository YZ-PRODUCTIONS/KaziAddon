package net.kazi.kazimod.abilities.Yagi;

import java.util.ArrayList;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.events.ability.UnlockAbilityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.api.events.onefruit.EatDevilFruitEvent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.packets.server.SSyncAbilityDataPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

/** Grant flight only after awakening, and retire previously granted base-form copies safely. */
@Mod.EventBusSubscriber(modid = "kazimod")
public final class AstralFlightProgression {
    private AstralFlightProgression() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) { sync(event.getPlayer()); }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) { sync(event.getPlayer()); }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEatFruit(EatDevilFruitEvent.Post event) { sync(event.getPlayer()); }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onUnlock(UnlockAbilityEvent event) {
        if (event.getAbilityCore() == AstralFlightAbility.INSTANCE
                && !AstralFlightAbility.canUnlock(event.getEntityLiving())) event.setResult(Event.Result.DENY);
    }

    @SubscribeEvent public static void eligibilityTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level.isClientSide
                && event.player.tickCount % 20 == 0 && !AstralFlightAbility.canUnlock(event.player)) sync(event.player);
    }

    public static void sync(PlayerEntity player) {
        if (player == null || player.level.isClientSide) return;
        IAbilityData data = AbilityDataCapability.get(player);
        if (data == null) return;
        if (!AstralFlightAbility.canUnlock(player)) {
            boolean changed = false;
            for (IAbility ability : new ArrayList<>(data.getEquippedAndPassiveAbilities())) {
                if (ability != null && ability.getCore() == AstralFlightAbility.INSTANCE)
                    AbilityHelper.emergencyStopAbility(player, ability);
            }
            while (data.removeEquippedAbility(AstralFlightAbility.INSTANCE)) changed = true;
            if (data.getPassiveAbility(AstralFlightAbility.INSTANCE) != null) {
                data.removePassiveAbility(AstralFlightAbility.INSTANCE);
                changed = true;
            }
            if (data.hasUnlockedAbility(AstralFlightAbility.INSTANCE)) {
                data.removeUnlockedAbility(AstralFlightAbility.INSTANCE);
                changed = true;
            }
            if (changed) WyNetwork.sendToAllTrackingAndSelf(new SSyncAbilityDataPacket(player.getId(), data), player);
            return;
        }
        // An existing instance can be intentionally paused or disabled; never replace it.
        if (data.hasUnlockedAbility(AstralFlightAbility.INSTANCE)
                || data.getPassiveAbility(AstralFlightAbility.INSTANCE) != null) return;
        // The native unlock path checks the core, bans and UnlockAbilityEvent, then creates PASSIVE instances.
        if (AbilityHelper.checkAndUnlockAbility(player, AstralFlightAbility.INSTANCE)) {
            WyNetwork.sendToAllTrackingAndSelf(new SSyncAbilityDataPacket(player.getId(), data), player);
        }
    }
}
