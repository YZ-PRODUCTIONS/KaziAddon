package net.kazi.kazimod.abilities.NagiAwakening;

import net.kazi.kazimod.config.KaziConfig;
import net.minecraft.entity.player.PlayerEntity;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUnlock;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.packets.server.SSyncAbilityDataPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

/** Grant the current awakening kit and migrate its active-to-passive conversions. */
public final class NagiAwakeningProgression {
    private static final AbilityCore<?>[] AWAKENED_MOVES = {
            IllusionShatterAbility.INSTANCE,
            DragonStrikeAbility.INSTANCE
    };
    private NagiAwakeningProgression() {}

    public static void sync(PlayerEntity player) {
        if (player == null || player.level.isClientSide || KaziConfig.INSTANCE.disableFruitChanges.get()
                || !NagiAwakeningSupport.canUnlock(player)) return;
        IAbilityData data = AbilityDataCapability.get(player);
        if (data == null) return;
        boolean changed = false;
        // Retire the former Nagi grant while keeping its registered implementation for future use.
        IAbility premonition = data.getEquippedAbility(CombatPremonitionAbility.INSTANCE);
        if (premonition != null) {
            xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper.emergencyStopAbility(player, premonition);
            data.removeEquippedAbility(CombatPremonitionAbility.INSTANCE);
            changed = true;
        }
        if (data.hasUnlockedAbility(CombatPremonitionAbility.INSTANCE)) {
            data.removeUnlockedAbility(CombatPremonitionAbility.INSTANCE);
            changed = true;
        }
        for (AbilityCore<?> core : AWAKENED_MOVES) {
            if (!data.hasUnlockedAbility(core)) {
                data.addUnlockedAbility(core, AbilityUnlock.PROGRESSION);
                changed = true;
            }
        }
        IllusionShatterAbility shatter = data.getPassiveAbility(IllusionShatterAbility.INSTANCE);
        if (shatter == null) {
            shatter = IllusionShatterAbility.INSTANCE.createAbility();
            data.addPassiveAbility(shatter);
            changed = true;
        }
        changed |= shatter.clearLegacyCooldown(player);
        DragonStrikeAbility innerDragons = data.getPassiveAbility(DragonStrikeAbility.INSTANCE);
        if (innerDragons == null) {
            innerDragons = DragonStrikeAbility.INSTANCE.createAbility();
            data.addPassiveAbility(innerDragons);
            changed = true;
        }
        changed |= innerDragons.clearLegacyCooldown(player);
        // Removed awakening IDs are discarded by native capability loading.
        // Only current abilities converted into passives need hotbar cleanup.
        // Unrelated fruit, race and haki slots retain their instances/cooldowns.
        for (int slot = 0; slot < data.getRawEquippedAbilities().size(); slot++) {
            IAbility equipped = data.getRawEquippedAbilities().get(slot);
            if (equipped == null) continue;
            if (IllusionShatterAbility.INSTANCE.equals(equipped.getCore())
                    || DragonStrikeAbility.INSTANCE.equals(equipped.getCore())) {
                data.setEquippedAbility(slot, null);
                changed = true;
            }
        }
        if (changed) WyNetwork.sendTo(new SSyncAbilityDataPacket(player.getId(), data), player);
    }
}
