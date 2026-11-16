package net.kazi.kazimod.abilities.NagiAwakening;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.kazi.kazimod.entities.DragonStrikeEntity;
import net.kazi.kazimod.entities.DragonAttackVfxEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

/** Server-side special payloads. Each summon owns its own windup and 15-second timer. */
public final class DragonSpecialAttacks {
    private static final int MAX_TARGETS = 8;

    private DragonSpecialAttacks() {}

    public static boolean execute(DragonStrikeEntity dragon, LivingEntity target, DragonStrikeAbility ability) {
        if (!ability.canSpecialHit(dragon, target)) return false;
        Vector3d start = dragon.getEyePosition(1), end = target.getBoundingBox().getCenter();
        if (start.distanceToSqr(end) > dragon.getKind().special.range * dragon.getKind().special.range) return false;
        switch (dragon.getKind().special) {
            case NULLIFYING_ROAR:
                roar(dragon, ability);
                return true;
            case RENDING_BITE:
                DragonAttackVfxEntity.spawn(dragon, DragonVfxPhase.SPECIAL, start, end);
                sound(dragon, SoundEvents.PLAYER_ATTACK_STRONG, .9F, .65F);
                if (ability.specialHit(dragon, target, 14)) {
                    target.addEffect(new EffectInstance(ModEffects.BLEEDING.get(), 100, 0));
                }
                return true;
            case VENOM_SPRAY:
                breath(dragon, target, ability, false);
                return true;
            case MIND_BEAM:
                beam(dragon, target, ability, false);
                return true;
            case SOULFIRE_BREATH:
                breath(dragon, target, ability, true);
                return true;
            case CRYSTAL_PRISON:
                prison(dragon, target, ability);
                return true;
            case THUNDER_LANCE:
                beam(dragon, target, ability, true);
                return true;
            case HEAVENLY_SLAM:
                return dragon.beginGrabSlam(target);
            default:
                return false;
        }
    }

    private static void roar(DragonStrikeEntity dragon, DragonStrikeAbility ability) {
        Vector3d center = dragon.getEyePosition(1);
        double radius = dragon.getKind().special.range;
        sound(dragon, SoundEvents.ENDER_DRAGON_GROWL, 1.1F, .65F);
        DragonAttackVfxEntity.spawn(dragon, DragonVfxPhase.SPECIAL, center, center);
        int hits = 0;
        for (LivingEntity candidate : nearby(dragon, radius)) {
            if (center.distanceToSqr(candidate.getBoundingBox().getCenter()) > radius * radius) continue;
            if (!ability.specialHit(dragon, candidate, 12)) continue;
            boolean fruit = NagiAwakeningSupport.hasFruitPowers(candidate);
            ImagineBrokenEffect.apply(candidate, 100);
            if (fruit) NagiAwakeningSupport.playNullificationSound(dragon.getCaster(), candidate.position());
            if (++hits >= MAX_TARGETS) break;
        }
    }

    private static void breath(DragonStrikeEntity dragon, LivingEntity target,
                               DragonStrikeAbility ability, boolean soul) {
        Vector3d start = dragon.getEyePosition(1);
        Vector3d direction = target.getBoundingBox().getCenter().subtract(start).normalize();
        double range = dragon.getKind().special.range;
        double cosine = Math.cos(Math.toRadians(soul ? 35 : 28));
        sound(dragon, soul ? SoundEvents.BLAZE_SHOOT : SoundEvents.GENERIC_SPLASH, .9F, soul ? .65F : .8F);
        DragonAttackVfxEntity.spawn(dragon, DragonVfxPhase.SPECIAL, start, traceEnd(dragon, start, direction, range));
        int hits = 0;
        for (LivingEntity candidate : nearby(dragon, range)) {
            Vector3d offset = candidate.getBoundingBox().getCenter().subtract(start);
            double distance = offset.length();
            if (distance > range || distance > .01 && offset.scale(1 / distance).dot(direction) < cosine) continue;
            if (!ability.specialHit(dragon, candidate, soul ? 14 : 10)) continue;
            candidate.addEffect(new EffectInstance(soul ? Effects.HUNGER : Effects.POISON, 120, soul ? 1 : 0));
            if (!soul) candidate.clearFire();
            if (++hits >= MAX_TARGETS) break;
        }
        // One breath cannot heal without bound in a crowd.
        if (soul && hits > 0) dragon.heal(Math.min(8, hits * 2));
    }

    private static void beam(DragonStrikeEntity dragon, LivingEntity target,
                             DragonStrikeAbility ability, boolean lightning) {
        Vector3d start = dragon.getEyePosition(1);
        Vector3d direction = target.getBoundingBox().getCenter().subtract(start).normalize();
        Vector3d end = traceEnd(dragon, start, direction, dragon.getKind().special.range);
        sound(dragon, lightning ? SoundEvents.LIGHTNING_BOLT_THUNDER : SoundEvents.BEACON_POWER_SELECT,
                lightning ? .65F : .9F, lightning ? 1.7F : .55F);
        DragonAttackVfxEntity.spawn(dragon, DragonVfxPhase.SPECIAL, start, end);
        double width = lightning ? .35 : .7;
        List<LivingEntity> candidates = new ArrayList<>(dragon.level.getEntitiesOfClass(LivingEntity.class,
                new AxisAlignedBB(start, end).inflate(width)));
        candidates.sort(Comparator.comparingDouble(candidate -> candidate.position().distanceToSqr(start)));
        int hits = 0;
        for (LivingEntity candidate : candidates) {
            AxisAlignedBB box = candidate.getBoundingBox().inflate(width);
            if (!box.contains(start) && !box.clip(start, end).isPresent()) continue;
            if (!ability.specialHit(dragon, candidate, lightning ? 18 : 14)) continue;
            if (lightning) {
                AbilityHelper.setDeltaMovement(candidate, direction.x * 1.1, .65, direction.z * 1.1);
            } else {
                candidate.addEffect(new EffectInstance(Effects.CONFUSION, 120, 0));
                candidate.addEffect(new EffectInstance(Effects.WEAKNESS, 80, 0));
            }
            if (++hits >= MAX_TARGETS) break;
        }
    }

    private static void prison(DragonStrikeEntity dragon, LivingEntity target, DragonStrikeAbility ability) {
        sound(dragon, SoundEvents.GLASS_BREAK, .8F, .65F);
        if (ability.specialHit(dragon, target, 10)) {
            target.addEffect(new EffectInstance(ModEffects.FROZEN.get(), 60, 0));
            Vector3d center = target.getBoundingBox().getCenter();
            DragonAttackVfxEntity.spawn(dragon, DragonVfxPhase.SPECIAL, center, center, 60, target);
        }
    }

    /** Stop before a wall or unloaded chunk; no world clip is attempted across unloaded terrain. */
    private static Vector3d traceEnd(DragonStrikeEntity dragon, Vector3d start, Vector3d direction, double range) {
        Vector3d previous = start;
        for (double distance = .5; distance <= range; distance += .5) {
            Vector3d point = start.add(direction.scale(distance));
            if (!DragonStrikeAbility.clearPath(dragon, previous, point)) break;
            previous = point;
        }
        return previous;
    }

    private static List<LivingEntity> nearby(DragonStrikeEntity dragon, double radius) {
        List<LivingEntity> targets = new ArrayList<>(dragon.level.getEntitiesOfClass(LivingEntity.class,
                dragon.getBoundingBox().inflate(radius)));
        targets.sort(Comparator.comparingDouble(dragon::distanceToSqr));
        return targets;
    }

    private static void sound(DragonStrikeEntity dragon, SoundEvent sound, float volume, float pitch) {
        dragon.level.playSound(null, dragon.blockPosition(), sound, SoundCategory.PLAYERS, volume, pitch);
    }
}
