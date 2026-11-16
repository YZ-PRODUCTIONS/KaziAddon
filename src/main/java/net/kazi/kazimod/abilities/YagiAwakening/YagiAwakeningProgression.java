package net.kazi.kazimod.abilities.YagiAwakening;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.kazi.kazimod.abilities.Yagi.HornRamAbility;
import net.kazi.kazimod.abilities.Yagi.MountainGoatFormAbility;
import net.kazi.kazimod.abilities.Yagi.MountainLeapAbility;
import net.kazi.kazimod.config.KaziConfig;
import net.minecraft.entity.player.PlayerEntity;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUnlock;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.packets.server.SSyncAbilityDataPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

/** Retire Goat's base attacks/form, like Vampire, while preserving flight and awakening bindings. */
public final class YagiAwakeningProgression {
    // Astral Flight is granted separately after awakening; never clear the complete fruit ability list.
    private static final Set<AbilityCore<?>> RETIRED_BASE_MOVES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            HornRamAbility.INSTANCE, MountainLeapAbility.INSTANCE,
            MountainGoatFormAbility.INSTANCE)));

    private YagiAwakeningProgression() {}

    public static boolean isRetired(AbilityCore<?> core) {
        return RETIRED_BASE_MOVES.contains(core);
    }

    public static void sync(PlayerEntity player) {
        if (player == null || player.level.isClientSide || KaziConfig.INSTANCE.disableFruitChanges.get()
                || !YagiAwakeningSupport.canUnlock(player)) return;
        IAbilityData data = AbilityDataCapability.get(player);
        if (data == null) return;
        boolean changed = false;
        // Stop morphs/dashes before removing their instances, so their end callbacks restore state.
        for (IAbility ability : new ArrayList<>(data.getEquippedAndPassiveAbilities())) {
            if (ability != null && isRetired(ability.getCore())) {
                AbilityHelper.emergencyStopAbility(player, ability);
            }
        }
        for (AbilityCore<?> core : RETIRED_BASE_MOVES) {
            // Native removal clears only one matching slot; legacy duplicate bindings must all go.
            while (data.removeEquippedAbility(core)) changed = true;
            if (data.getPassiveAbility(core) != null) {
                // Run native cleanup for any legacy passive copy of a retired move.
                data.removePassiveAbility(core);
                changed = true;
            }
            if (data.hasUnlockedAbility(core)) {
                data.removeUnlockedAbility(core);
                changed = true;
            }
        }
        AbilityCore<?>[] moves = {StarBlazingAbility.INSTANCE, ShockerBreakerAbility.INSTANCE,
                ChaosSaberAbility.INSTANCE, ChaosBusterAbility.INSTANCE};
        for (AbilityCore<?> core : moves) {
            if (!data.hasUnlockedAbility(core)) {
                data.addUnlockedAbility(core, AbilityUnlock.PROGRESSION);
                changed = true;
            }
        }
        if (changed) WyNetwork.sendTo(new SSyncAbilityDataPacket(player.getId(), data), player);
    }
}
