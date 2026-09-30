package net.kazi.kazimod.abilities.SupaRework;

import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

/** Dismantle's line acquisition, separated from the later damaging impact. */
public final class UnlimitedLostWorksAttack {
    public static final float RANGE = 20.0F;
    public static final float WIDTH = 3.0F;
    public static final float BASE_DAMAGE = 0.1F;
    public static final int HIT_COUNT = 5;
    public static final float DAMAGE_PER_HIT = BASE_DAMAGE / HIT_COUNT;
    public static final int BLINDNESS_TICKS = 20;
    public static final int SLOW_TICKS = 100;
    public static final int OPENING_CONTROL_TICKS = 20;

    private UnlimitedLostWorksAttack() { }

    public static LivingEntity findTarget(LivingEntity caster, RangeComponent range) {
        if (caster.level.isClientSide) return null;
        PlayerEntity nearestPlayer = null;
        LivingEntity nearestOther = null;
        double playerDistance = RANGE * RANGE;
        double otherDistance = RANGE * RANGE;
        for (LivingEntity candidate : range.getTargetsInLine(caster, RANGE, WIDTH)) {
            if (!isValidTarget(caster, candidate) || candidate.hasEffect(ModEffects.SILENT.get())) continue;
            double distance = caster.distanceToSqr(candidate);
            if (candidate instanceof PlayerEntity) {
                if (distance <= playerDistance) {
                    nearestPlayer = (PlayerEntity) candidate;
                    playerDistance = distance;
                }
            } else if (distance <= otherDistance) {
                nearestOther = candidate;
                otherDistance = distance;
            }
        }
        // No hurtTarget(..., 0): weapon/Haki bonuses could otherwise damage the mark.
        return nearestPlayer != null ? nearestPlayer : nearestOther;
    }

    public static boolean isValidTarget(LivingEntity caster, LivingEntity target) {
        return caster != null && caster.isAlive() && target != null
                && caster != target && target.isAlive() && target.level == caster.level
                && !target.isSpectator()
                && (!(target instanceof PlayerEntity) || !((PlayerEntity) target).isCreative());
    }

    public static void applyOpeningControl(LivingEntity caster, LivingEntity target) {
        if (!isValidTarget(caster, target) || caster.level.isClientSide) return;
        target.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(), OPENING_CONTROL_TICKS, 0, false, false));
        target.addEffect(new EffectInstance(Effects.LEVITATION, OPENING_CONTROL_TICKS, 0, false, false));
    }

    public static boolean pierce(LivingEntity caster, LivingEntity target, DealDamageComponent damage, boolean applyEffects) {
        if (!isValidTarget(caster, target) || caster.level.isClientSide) return false;
        AbilityDamageSource source = (AbilityDamageSource) damage.getDamageSource(caster);
        // Lost Works has no Haki or weapon source. Its rays directly bypass armor.
        source.setHakiNature(SourceHakiNature.UNKNOWN);
        source.setSourceTypes(new java.util.ArrayList<>());
        source.bypassArmor();
        // Each ray is a separate strike. Temporarily clear vanilla hurt immunity for this
        // attempt, then retain the normal immunity window for other attacks.
        int previousInvulnerability = target.invulnerableTime;
        boolean hit;
        target.invulnerableTime = 0;
        try {
            hit = damage.hurtTarget(caster, target, DAMAGE_PER_HIT, source);
        } finally {
            target.invulnerableTime = Math.max(previousInvulnerability, target.invulnerableTime);
        }
        if (!hit) return false;
        if (applyEffects) {
            target.addEffect(new EffectInstance(Effects.BLINDNESS, BLINDNESS_TICKS, 0));
            target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, SLOW_TICKS, 0));
            target.addEffect(new EffectInstance(KaziEffects.WEAKENED_MOVEMENT.get(), SLOW_TICKS, 0));
        }
        return true;
    }
}
