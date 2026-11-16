package net.kazi.kazimod.events;

import java.util.ArrayList;
import java.util.List;
import net.MrMagicalCart.cartaddon.abilities.opeextra.KRoomAnesthesiaAbility;
import net.MrMagicalCart.cartaddon.abilities.opeextra.ReworkedRoomAbility;
import net.kazi.kazimod.abilities.OpeRework.KRoomAnesthesiaRework;
import net.kazi.kazimod.config.KaziConfig;
import net.kazi.kazimod.ope.OpeRoomMode;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.events.ability.UnlockAbilityEvent;
import xyz.pixelatedw.mineminenomi.data.entity.ability.*;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.packets.server.SSyncAbilityDataPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

@Mod.EventBusSubscriber(modid = "kazimod")
public final class OpeRoomMigration {
    // Move old slots before Mine Mine no Mi's join-time unlock validation can remove them.
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void join(EntityJoinWorldEvent event) {
        if (!event.getWorld().isClientSide && event.getEntity() instanceof ServerPlayerEntity)
            migrate((ServerPlayerEntity)event.getEntity());
    }

    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) { migrate(event.getPlayer()); }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void preventLegacyUnlock(UnlockAbilityEvent event) {
        if (event.getEntityLiving() instanceof PlayerEntity && !KaziConfig.INSTANCE.disableFruitChanges.get()
                && isLegacy(event.getAbilityCore())) event.setResult(Event.Result.DENY);
    }

    private static boolean isLegacy(AbilityCore<?> core) {
        return core == KRoomAnesthesiaRework.INSTANCE || core == KRoomAnesthesiaAbility.INSTANCE;
    }

    public static void migrate(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity) || KaziConfig.INSTANCE.disableFruitChanges.get()
                || !DevilFruitCapability.get(player).hasDevilFruit(ModAbilities.OPE_OPE_NO_MI)) return;

        IAbilityData data = AbilityDataCapability.get(player);
        ReworkedRoomAbility room = data.getEquippedAbility(ReworkedRoomAbility.INSTANCE);
        boolean changed = false;
        if (OpeRoomMode.selected(room)) {
            // The coat is cast state, not part of the saved mode selection.
            room.getComponent(ModAbilityKeys.CHARGE).filter(c -> c.isCharging()).ifPresent(c -> c.forceStopCharging(player));
            room.getComponent(ModAbilityKeys.CONTINUOUS).ifPresent(c -> c.stopContinuity(player));
            if (!OpeRoomMode.unlocked(player)) room.getComponent(ModAbilityKeys.ALT_MODE).ifPresent(c -> c.revertToDefault(player));
            changed = true;
        }

        List<IAbility> slots = new ArrayList<>(data.getRawEquippedAbilities());
        int legacySlot = -1;
        for (int i = 0; i < slots.size(); i++) {
            IAbility ability = slots.get(i);
            if (ability == null || !isLegacy(ability.getCore())) continue;
            data.removeEquippedAbility(ability);
            if (legacySlot == -1) legacySlot = i;
            changed = true;
        }
        if (data.hasUnlockedAbility(KRoomAnesthesiaRework.INSTANCE)) {
            data.removeUnlockedAbility(KRoomAnesthesiaRework.INSTANCE);
            changed = true;
        }
        if (data.hasUnlockedAbility(KRoomAnesthesiaAbility.INSTANCE)) {
            data.removeUnlockedAbility(KRoomAnesthesiaAbility.INSTANCE);
            changed = true;
        }
        if (room == null && legacySlot >= 0) {
            room = ReworkedRoomAbility.INSTANCE.createAbility();
            data.addUnlockedAbility(ReworkedRoomAbility.INSTANCE, AbilityUnlock.PROGRESSION);
            data.setEquippedAbility(legacySlot, room);
            if (OpeRoomMode.unlocked(player)) room.getComponent(ModAbilityKeys.ALT_MODE)
                    .ifPresent(c -> c.setMode(player, OpeRoomMode.Mode.K_ROOM));
        }
        if (changed) {
            KRoomAnesthesiaRework.updateKRoomSwordTint(player, false);
            WyNetwork.sendTo(new SSyncAbilityDataPacket(player.getId(), data), (ServerPlayerEntity)player);
        }
    }
    private OpeRoomMigration() {}
}
