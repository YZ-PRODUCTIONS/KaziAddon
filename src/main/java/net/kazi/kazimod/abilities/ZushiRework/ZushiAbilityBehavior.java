package net.kazi.kazimod.abilities.ZushiRework;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import net.kazi.kazimod.KaziMod;
import net.kazi.kazimod.events.ZushiVisualEvents;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.vector.Vector3d;
import xyz.pixelatedw.mineminenomi.abilities.zushi.GraviZoneAbility;
import xyz.pixelatedw.mineminenomi.abilities.zushi.JigokuTabiAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.protection.DefaultProtectionRules;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.api.util.PriorityEventPool;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

/**
 * MMNM 0.10.11's Zushi continuity logic with the friend's replacement effects.
 * Keeps the original ability objects/cores, checks, bonuses, start/end handlers and saves.
 * The dependency exposes event-pool operations, but not the continuity pool itself;
 * only those stable, non-Minecraft field names are accessed reflectively. No mixins.
 */
public final class ZushiAbilityBehavior {
    private static final Map<IAbility, Boolean> INSTALLED = new WeakHashMap<>();
    private static final Access ACCESS = findAccess();
    private static boolean warned;

    private ZushiAbilityBehavior() { }

    public static boolean supports(IAbility ability) {
        return ability instanceof GraviZoneAbility || ability instanceof JigokuTabiAbility;
    }

    public static void install(IAbility ability) {
        if (ACCESS == null || !supports(ability) || INSTALLED.containsKey(ability)) return;
        ContinuousComponent continuous = ability.getComponent(ModAbilityKeys.CONTINUOUS).orElse(null);
        if (continuous == null) return;
        INSTALLED.put(ability, Boolean.FALSE);
        try {
            PriorityEventPool<ContinuousComponent.IDuringContinuousEvent> pool = ACCESS.tickEvents(continuous);
            Class<?> owner = ability instanceof GraviZoneAbility ? GraviZoneAbility.class : JigokuTabiAbility.class;
            // Remove only the dependency's own callback, never another addon's listeners.
            List<ContinuousComponent.IDuringContinuousEvent> originals = pool.getEventsStream()
                    .filter(callback -> callback.getClass().getName().startsWith(owner.getName() + "$$Lambda$"))
                    .collect(Collectors.toList());
            if (originals.size() != 1) {
                warn("Could not identify Zushi's original continuity callback; leaving this ability unchanged.", null);
                return;
            }
            ContinuousComponent.IDuringContinuousEvent replacement;
            if (ability instanceof GraviZoneAbility) {
                Interval rings = (Interval) ACCESS.gravityRingInterval.get(ability);
                DealDamageComponent damage = ability.getComponent(ModAbilityKeys.DAMAGE).orElse(null);
                if (damage == null || rings == null) return;
                replacement = (caster, current) -> tickGraviZone(caster, current, rings, damage);
            } else {
                Interval damageInterval = (Interval) ACCESS.damageInterval.get(ability);
                Interval forceInterval = (Interval) ACCESS.forceInterval.get(ability);
                RangeComponent range = ability.getComponent(ModAbilityKeys.RANGE).orElse(null);
                DealDamageComponent damage = ability.getComponent(ModAbilityKeys.DAMAGE).orElse(null);
                if (range == null || damage == null || damageInterval == null || forceInterval == null) return;
                replacement = (caster, current) -> tickJigokuTabi(caster, current, range, damage, damageInterval, forceInterval);
            }
            pool.removeEvent(originals.get(0));
            continuous.addTickEvent(PriorityEventPool.DEFAULT_PRIORITY, replacement);
            continuous.addEndEvent((caster, current) -> ZushiVisualEvents.clear(current));
            INSTALLED.put(ability, Boolean.TRUE);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warn("Could not install Zushi's replacement behavior.", exception);
        }
    }

    private static void tickGraviZone(LivingEntity caster, IAbility ability, Interval rings, DealDamageComponent damage) {
        if (caster.level.isClientSide) return;
        // Preserve the base interval's state while replacing its old particle-ring calls.
        rings.canTick();
        boolean reject = ability.getComponent(ModAbilityKeys.ALT_MODE)
                .map(mode -> mode.getCurrentMode() == GraviZoneAbility.Mode.REJECT).orElse(false);
        double radius = reject ? 3.0D : 8.0D;
        ZushiVisualEvents.refreshZone(caster, ability, (float) radius);
        List<Entity> targets = WyHelper.getNearbyEntities(caster.position(), caster.level, radius,
                ModEntityPredicates.getEnemyFactions(caster), Entity.class);
        for (Entity target : targets) {
            if (!reject) {
                target.setPos(target.xo, target.yo, target.zo);
                if (target instanceof LivingEntity) {
                    ((LivingEntity) target).addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(), 5, 0, false, false));
                }
            } else {
                boolean causedDamage = !(target instanceof LivingEntity)
                        || damage.hurtTarget(caster, (LivingEntity) target, 10.0F);
                if (causedDamage) {
                    Vector3d direction = target.position().subtract(caster.position()).add(0.0D, -1.0D, 0.0D).normalize();
                    AbilityHelper.setDeltaMovement(target, direction.x * 4.5D, (double) 0.2F, direction.z * 4.5D);
                }
            }
        }
    }

    private static void tickJigokuTabi(LivingEntity caster, IAbility ability, RangeComponent range,
                                       DealDamageComponent damage, Interval damageInterval, Interval forceInterval) {
        if (caster.level.isClientSide) return;
        int force;
        try {
            force = ACCESS.force.getInt(ability);
        } catch (IllegalAccessException exception) {
            warn("Could not read Jigoku Tabi's force.", exception);
            return;
        }
        List<LivingEntity> targets = range.getTargetsInArea(caster, 24.0F);
        // Use the actual gameplay target list and pre-increment force, as the jar's redirect does.
        ZushiVisualEvents.refreshPressure(caster, ability, targets, force);
        ModDamageSource source = ModDamageSource.causeAbilityDamage(caster, ability)
                .bypassLogia().setPiercing(1.0F).setUnavoidable();
        for (LivingEntity target : targets) {
            AbilityHelper.setDeltaMovement(target, 0.0D, target.getDeltaMovement().y - 4.0D, 0.0D);
            // Keep the dependency's per-target interval behavior, including its original cadence.
            if (damageInterval.canTick()) {
                target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 25, 5, false, false));
                damage.hurtTarget(caster, target, (float) (force * 2), source);
                AbilityHelper.createSphere(caster.level, target.blockPosition(), force, 2, false,
                        Blocks.AIR, 2, DefaultProtectionRules.CORE_FOLIAGE_ORE);
            }
        }
        if (forceInterval.canTick()) {
            try {
                ACCESS.force.setInt(ability, force + 1);
            } catch (IllegalAccessException exception) {
                warn("Could not update Jigoku Tabi's force.", exception);
            }
        }
    }

    private static Access findAccess() {
        try {
            return new Access();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            KaziMod.LOGGER.warn("Zushi behavior hooks are unavailable for this dependency version; original abilities remain active.", exception);
            return null;
        }
    }

    private static void warn(String message, Throwable exception) {
        if (!warned) {
            warned = true;
            KaziMod.LOGGER.warn(message, exception);
        }
    }

    private static final class Access {
        final Field ticks = field(ContinuousComponent.class, "tickContinuousEvents");
        final Field gravityRingInterval = field(GraviZoneAbility.class, "gravityRingInterval");
        final Field force = field(JigokuTabiAbility.class, "force");
        final Field damageInterval = field(JigokuTabiAbility.class, "damageInterval");
        final Field forceInterval = field(JigokuTabiAbility.class, "addForceInterval");

        Access() throws ReflectiveOperationException { }

        @SuppressWarnings("unchecked")
        PriorityEventPool<ContinuousComponent.IDuringContinuousEvent> tickEvents(ContinuousComponent component)
                throws IllegalAccessException {
            return (PriorityEventPool<ContinuousComponent.IDuringContinuousEvent>) ticks.get(component);
        }

        private static Field field(Class<?> owner, String name) throws ReflectiveOperationException {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        }
    }
}
