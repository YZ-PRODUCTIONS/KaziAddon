package net.kazi.kazimod.abilities.BariRework;

import net.kazi.kazimod.entities.InfiniteVoidBarrierEntity;
import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.TargetsPredicate;
import xyz.pixelatedw.mineminenomi.entities.SphereEntity;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;

import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BariAwaken extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "barrier_guard",
            new Pair[]{
                    ImmutablePair.of("Creates a barrier sphere around the user or an ally, providing heavy protection.", (Object) null),
            });

    private static final float DURATION      = 200.0F;
    private static final float COOLDOWN      = 400.0F;
    private static final float ALLY_RANGE    = 20.0F;
    private static final int   ALLY_DURATION = 160;
    private static final float RADIUS        = 5.0F;
    private static final Color SPHERE_COLOR  = new Color(100, 200, 255, 180);

    private static final ResourceLocation GUARD_ICON = new ResourceLocation("kazimod", "textures/abilities/barrier_guard.png");
    private static final ResourceLocation ALLY_ICON  = new ResourceLocation("kazimod", "textures/abilities/barrier_guard_ally.png");

    public static final AbilityCore<BariAwaken> INSTANCE;

    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this, true))
            .addStartEvent(100, this::onContinuityStart)
            .addTickEvent(this::onContinuityTick)
            .addEndEvent(this::onContinuityEnd);

    private final AltModeComponent<Mode> altModeComponent;
    private final AnimationComponent     animationComponent;

    private SphereEntity              selfSphere   = null;
    private InfiniteVoidBarrierEntity selfBarrier  = null;
    private Vector3d                  selfPosition = null;

    private final Map<UUID, AllyShield> allyShields = new HashMap<>();

    public BariAwaken(AbilityCore<BariAwaken> core) {
        super(core);
        this.altModeComponent   = (new AltModeComponent<>(this, Mode.class, Mode.GUARD)).addChangeModeEvent(this::onAltModeChange);
        this.animationComponent = new AnimationComponent(this);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.continuousComponent,
                this.altModeComponent,
                this.animationComponent
        });
        super.setDisplayIcon(GUARD_ICON);
        this.addUseEvent(this::onUseEvent);
        this.addTickEvent(this::onAbilityTick);
    }

    // ── Alt mode icon swap — mirrors Tekkai's onAltModeChange ─────────────────

    private void onAltModeChange(LivingEntity entity, IAbility ability, Mode mode) {
        if (mode == Mode.GUARD) {
            super.setDisplayIcon(GUARD_ICON);
        } else if (mode == Mode.ALLY) {
            super.setDisplayIcon(ALLY_ICON);
        }
    }

    // ── Use ───────────────────────────────────────────────────────────────────

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.altModeComponent.isMode(Mode.ALLY)) {
            LivingEntity target = getTargetAlly(entity);
            if (target != null) {
                AllyShield existing = allyShields.remove(target.getUUID());
                if (existing != null) existing.remove();

                SphereEntity              s = spawnSphere(entity.level, entity, target.getX(), target.getY(), target.getZ());
                InfiniteVoidBarrierEntity b = spawnBarrier(entity.level, entity, target.getX(), target.getY(), target.getZ());
                allyShields.put(target.getUUID(), new AllyShield(target, s, b, ALLY_DURATION));

                target.addEffect(new EffectInstance(Effects.DAMAGE_RESISTANCE, ALLY_DURATION, 4, false, false));
                target.addEffect(new EffectInstance((Effect) ModEffects.GUARDING.get(), ALLY_DURATION, 1, false, false));
            }
            super.cooldownComponent.startCooldown(entity, COOLDOWN);
        } else {
            this.continuousComponent.triggerContinuity(entity, DURATION);
        }
    }

    // ── Continuous events (GUARD mode) ────────────────────────────────────────

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, ModAnimations.CROSSED_ARMS);
        if (!entity.level.isClientSide) {
            this.selfPosition = new Vector3d(entity.getX(), entity.getY(), entity.getZ());
            this.selfSphere   = spawnSphere(entity.level, entity, selfPosition.x, selfPosition.y, selfPosition.z);
            this.selfBarrier  = spawnBarrier(entity.level, entity, selfPosition.x, selfPosition.y, selfPosition.z);
        }
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        // Lock the user in place
        entity.addEffect(new EffectInstance((Effect) ModEffects.GUARDING.get(), 2, 1, false, false));
        entity.addEffect(new EffectInstance(Effects.DAMAGE_RESISTANCE, 2, 4, false, false));
        if (!entity.level.isClientSide && selfPosition != null) {
            if (selfSphere  != null && selfSphere.isAlive())
                selfSphere.setPos(selfPosition.x, selfPosition.y, selfPosition.z);
            if (selfBarrier != null && selfBarrier.isAlive())
                selfBarrier.setPos(selfPosition.x, selfPosition.y, selfPosition.z);

            // Float and lock all entities inside the barrier (enemies/others)
            trapEntitiesInside(entity, selfPosition);
        }
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        cleanupSelf();
        this.selfPosition = null;
        this.animationComponent.stop(entity);
        entity.removeEffect((Effect) ModEffects.GUARDING.get());
        entity.removeEffect(Effects.DAMAGE_RESISTANCE);
        super.cooldownComponent.startCooldown(entity, this.continuousComponent.getContinueTime() + COOLDOWN);
    }

    // ── Global tick — ally shields + their trapped entities ───────────────────

    private void onAbilityTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        Iterator<Map.Entry<UUID, AllyShield>> it = allyShields.entrySet().iterator();
        while (it.hasNext()) {
            AllyShield shield = it.next().getValue();

            if (shield.target.isAlive()) {
                Vector3d center = new Vector3d(shield.target.getX(), shield.target.getY(), shield.target.getZ());

                if (shield.sphere  != null && shield.sphere.isAlive())
                    shield.sphere.setPos(center.x, center.y, center.z);
                if (shield.barrier != null && shield.barrier.isAlive())
                    shield.barrier.setPos(center.x, center.y, center.z);

                // Float and lock entities inside the ally's barrier too
                trapEntitiesInside(entity, center);
            }

            shield.ticksRemaining--;
            if (shield.ticksRemaining <= 0 || !shield.target.isAlive()) {
                shield.remove();
                it.remove();
            }
        }
    }

    // ── Trap logic — floats + locks all non-owner entities inside the sphere ──

    private void trapEntitiesInside(LivingEntity owner, Vector3d center) {
        AxisAlignedBB box = new AxisAlignedBB(
                center.x - RADIUS, center.y - RADIUS, center.z - RADIUS,
                center.x + RADIUS, center.y + RADIUS, center.z + RADIUS);

        List<LivingEntity> inside = owner.level.getEntitiesOfClass(
                LivingEntity.class, box,
                e -> e != owner && e.isAlive()
                        && e.position().distanceTo(center) <= RADIUS);

        for (LivingEntity trapped : inside) {
            // Zero out all movement — no walking, no jumping, no knockback
            AbilityHelper.setDeltaMovement(trapped, 0.0, 0.0, 0.0);
            // Apply movement blocked so the game itself doesn't let them move
            trapped.addEffect(new EffectInstance((Effect) ModEffects.MOVEMENT_BLOCKED.get(), 2, 5, false, false));
            // Float them — counteract gravity so they hover in place
            trapped.setNoGravity(true);
        }
    }

    // ── Spawn helpers ─────────────────────────────────────────────────────────

    private SphereEntity spawnSphere(net.minecraft.world.World world, LivingEntity owner,
                                     double x, double y, double z) {
        if (world.isClientSide) return null;
        SphereEntity s = new SphereEntity(world, owner);
        s.setColor(SPHERE_COLOR);
        s.setRadius(RADIUS);
        s.setDetailLevel(16);
        s.setAnimationSpeed(1);
        s.setPos(x, y, z);
        world.addFreshEntity(s);
        return s;
    }

    private InfiniteVoidBarrierEntity spawnBarrier(net.minecraft.world.World world, LivingEntity owner,
                                                   double x, double y, double z) {
        if (world.isClientSide) return null;
        InfiniteVoidBarrierEntity b = new InfiniteVoidBarrierEntity(
                (EntityType<? extends Entity>) KaziEntities.INFINITE_VOID_BARRIER.get(), world);
        b.setSpawner(owner);
        b.setRadius(RADIUS);
        b.setPos(x, y, z);
        world.addFreshEntity(b);
        return b;
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    private void cleanupSelf() {
        if (selfSphere  != null) { if (selfSphere.isAlive())  selfSphere.remove();  selfSphere  = null; }
        if (selfBarrier != null) { if (selfBarrier.isAlive()) selfBarrier.remove(); selfBarrier = null; }
    }

    // ── Ally hitscan ──────────────────────────────────────────────────────────

    private LivingEntity getTargetAlly(LivingEntity entity) {
        Vector3d start = entity.getEyePosition(1.0F);
        Vector3d look  = entity.getLookAngle();
        Vector3d end   = start.add(look.scale(ALLY_RANGE));

        TargetsPredicate friendlyPred = (new TargetsPredicate()).testFriendlyFaction();
        LivingEntity best     = null;
        double       bestDist = Double.MAX_VALUE;

        for (LivingEntity candidate : entity.level.getEntitiesOfClass(
                LivingEntity.class,
                entity.getBoundingBox().expandTowards(look.scale(ALLY_RANGE)).inflate(2.0),
                e -> e != entity && e.isAlive() && friendlyPred.test(entity, e))) {

            java.util.Optional<Vector3d> hit = candidate.getBoundingBox().clip(start, end);
            if (hit.isPresent()) {
                double dist = start.distanceTo(hit.get());
                if (dist < bestDist) {
                    bestDist = dist;
                    best     = candidate;
                }
            }
        }
        return best;
    }

    // ── Inner class ───────────────────────────────────────────────────────────

    private static class AllyShield {
        final LivingEntity              target;
        final SphereEntity              sphere;
        final InfiniteVoidBarrierEntity barrier;
        int ticksRemaining;

        AllyShield(LivingEntity target, SphereEntity sphere, InfiniteVoidBarrierEntity barrier, int ticks) {
            this.target         = target;
            this.sphere         = sphere;
            this.barrier        = barrier;
            this.ticksRemaining = ticks;
        }

        void remove() {
            if (sphere  != null && sphere.isAlive())  sphere.remove();
            if (barrier != null && barrier.isAlive()) barrier.remove();
        }
    }

    // ── Static init ───────────────────────────────────────────────────────────

    static {
        INSTANCE = (new AbilityCore.Builder<>("Barrier Guard", AbilityCategory.DEVIL_FRUITS, BariAwaken::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN),
                        ContinuousComponent.getTooltip(DURATION)
                }).build();
    }

    public enum Mode {
        GUARD,
        ALLY
    }
}