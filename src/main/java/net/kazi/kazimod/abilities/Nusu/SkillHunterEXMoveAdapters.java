package net.kazi.kazimod.abilities.Nusu;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.util.ResourceLocation;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;

/**
 * Per-move exceptions for Skill Hunter EX.  Requirement components themselves
 * are handled by the scoped mixin; this list is only for moves which need a
 * persistent world state and therefore cannot work as a one-use borrowed move.
 */
final class SkillHunterEXMoveAdapters {
    private static final Set<String> ROOM_MOVES = new HashSet<>(Arrays.asList(
            "amputate", "counter_shock", "gamma_knife", "injection_shot", "mes",
            "shambles", "takt", "radio_knife", "perennial_youth_operation", "curtain",
            "jinkaku_ishoku_shujutsu", "takt_control", "takt_emergence", "takt_hover",
            "takt_toss"));

    private SkillHunterEXMoveAdapters() {}

    static boolean supports(IAbility ability, ResourceLocation key) {
        if (!ROOM_MOVES.contains(key.getPath())) return true;
        String packageName = ability.getClass().getName();
        // Ope attacks consume a live Room object, not merely the normal
        // RequireAbilityComponent. Do not offer a roll that cannot function.
        return !(packageName.contains(".abilities.ope.")
                || packageName.contains(".abilities.opeextra.")
                || packageName.contains(".abilities.OpeRework."));
    }
}
