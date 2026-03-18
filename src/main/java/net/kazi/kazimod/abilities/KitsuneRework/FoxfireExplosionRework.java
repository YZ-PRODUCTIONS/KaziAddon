package net.kazi.kazimod.abilities.KitsuneRework;

import net.MrMagicalCart.cartaddon.entities.projectiles.inukitsune.FoxfireExplosionProjectile;
import net.MrMagicalCart.cartaddon.init.CartMorphs;
import net.MrMagicalCart.cartaddon.init.CartParticleTypes;
import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleType;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class FoxfireExplosionRework extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "cartaddon", "foxfire_explosion",
            new Pair[]{ImmutablePair.of(
                    "The user erects a fixed 25-block blue-fire barrier. Enemies inside are " +
                            "scorched with Flaming Rot. Foxfire projectiles rain down from above, " +
                            "exploding on impact. Neither enemies nor the user can leave the barrier.",
                    (Object) null)});

    // Hold time: 12 seconds = 240 ticks
    private static final float HOLD_TIME     = 240.0F;
    // Cooldown range based on how long the ability was held
    private static final int   MIN_COOLDOWN  = 100;  //  5 seconds
    private static final int   MAX_COOLDOWN  = 760;  // 38 seconds

    // Barrier geometry — fixed at cast position, never moves
    private static final double BARRIER_RADIUS    = 25.0;
    private static final double BARRIER_RADIUS_SQ = BARRIER_RADIUS * BARRIER_RADIUS;
    private static final double BARRIER_HEIGHT    = 14.0;

    // Rain: one projectile every 4 ticks = ~5 per second
    private static final int RAIN_INTERVAL = 4;
    // How high above the cast position projectiles spawn
    private static final double RAIN_HEIGHT = 35.0;

    public static final AbilityCore<FoxfireExplosionRework> INSTANCE;

    private final AnimationComponent    animationComponent;
    private final RangeComponent        rangeComponent = new RangeComponent(this);
    private final RequireMorphComponent requireMorphComponent;
    private final ContinuousComponent   continuousComponent;

    // Fixed at the moment the ability is activated — does NOT follow the caster
    private Vector3d barrierCenter = null;
    private int      rainTick      = 0;

    public FoxfireExplosionRework(AbilityCore<FoxfireExplosionRework> core) {
        super(core);
        this.requireMorphComponent = new RequireMorphComponent(
                this,
                (MorphInfo) CartMorphs.KITSUNE_HYBRID.get(),
                new MorphInfo[]{(MorphInfo) CartMorphs.KITSUNE_WALK.get()});
        super.isNew = true;
        this.animationComponent = new AnimationComponent(this);
        this.continuousComponent = (new ContinuousComponent(this))
                .addStartEvent(this::onContinuityStart)
                .addTickEvent(this::duringContinuityEvent)
                .addEndEvent(this::endContinuityEvent);

        super.addComponents(new AbilityComponent[]{
                this.requireMorphComponent,
                this.animationComponent,
                this.rangeComponent,
                this.continuousComponent
        });
        super.addUseEvent(this::onUse);
    }

    // ── Activation ────────────────────────────────────────────────────────────

    private void onUse(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, ModAnimations.POINT_RIGHT_ARM);
        this.continuousComponent.triggerContinuity(entity, HOLD_TIME);
        entity.level.playSound(
                (PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.MERA_SFX.get(),
                SoundCategory.PLAYERS, 3.0F, 1.0F);
    }

    // ── Lock barrier center at cast position ──────────────────────────────────

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        // Snapshot position right as it starts — never updated again
        this.barrierCenter = new Vector3d(entity.getX(), entity.getY(), entity.getZ());
        this.rainTick = 0;
    }

    // ── Per-tick logic ────────────────────────────────────────────────────────

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        if (barrierCenter == null) return;

        double cx = barrierCenter.x;
        double cy = barrierCenter.y;
        double cz = barrierCenter.z;

        // ── 1. Push anyone (including the caster) who exits the barrier back in ─
        // We check every living entity in the area, plus the caster explicitly.
        pushBackIfOutside(entity, cx, cy, cz); // always check caster
        for (LivingEntity nearby : this.rangeComponent.getTargetsInArea(entity, (float) BARRIER_RADIUS + 4)) {
            pushBackIfOutside(nearby, cx, cy, cz);
        }

        // ── 2. Flaming Rot + weakness on enemies inside the barrier ───────────
        for (LivingEntity target : this.rangeComponent.getTargetsInArea(entity, (float) BARRIER_RADIUS)) {
            if (!isInBarrier(target, cx, cy, cz)) continue;
            // Refresh every tick so it never lapses while inside
            target.addEffect(new EffectInstance(
                    (Effect) KaziEffects.FLAMING_ROT.get(), 40, 0, false, true));
            if (!target.hasEffect(Effects.WEAKNESS)) {
                target.addEffect(new EffectInstance(Effects.WEAKNESS, 60, 0));
            }
        }

        // ── 3. Barrier wall particles — three height rings of big blue fire ────
        for (int ring = 0; ring < 4; ring++) {
            double ringY = cy + (ring * (BARRIER_HEIGHT / 3.0));
            for (int i = 0; i < 48; i++) {
                double angle = (2 * Math.PI * i) / 48.0;
                double px    = cx + Math.cos(angle) * BARRIER_RADIUS;
                double pz    = cz + Math.sin(angle) * BARRIER_RADIUS;
                SimpleParticleData data = new SimpleParticleData(
                        (ParticleType) CartParticleTypes.BLUE_FIRE.get());
                data.setLife(18);
                data.setSize(12.0F); // large, wall-filling particles
                WyHelper.spawnParticles(data, (ServerWorld) entity.level, px, ringY, pz);
            }
        }

        // ── 4. Foxfire rain ───────────────────────────────────────────────────
        if (++rainTick >= RAIN_INTERVAL) {
            rainTick = 0;
            // Spawn 3 projectiles per interval for a heavy rain feel
            for (int i = 0; i < 3; i++) {
                spawnRainProjectile(entity, cx, cy, cz);
            }
        }
    }

    // ── Spawn one falling foxfire projectile ──────────────────────────────────

    private void spawnRainProjectile(LivingEntity caster, double cx, double cy, double cz) {
        // Uniform random point inside the circle
        double angle  = WyHelper.randomDouble() * 2 * Math.PI;
        double r      = BARRIER_RADIUS * Math.sqrt(Math.abs(WyHelper.randomDouble()));
        double spawnX = cx + Math.cos(angle) * r;
        double spawnZ = cz + Math.sin(angle) * r;
        double spawnY = cy + RAIN_HEIGHT;

        FoxfireExplosionProjectile proj = new FoxfireExplosionProjectile(
                caster.level, caster, this);
        proj.setPos(spawnX, spawnY, spawnZ);
        proj.setDeltaMovement(0.0, -2.5, 0.0);
        caster.level.addFreshEntity(proj);
    }

    // ── Push entity back inside the barrier if they've crossed the wall ───────

    private void pushBackIfOutside(LivingEntity target, double cx, double cy, double cz) {
        double dx   = target.getX() - cx;
        double dz   = target.getZ() - cz;
        double distSq = dx * dx + dz * dz;

        if (distSq > BARRIER_RADIUS_SQ) {
            // Direction from entity back toward center
            double dist = Math.sqrt(distSq);
            double nx   = -dx / dist; // inward normal
            double nz   = -dz / dist;
            // Apply a firm push inward; keep existing Y velocity
            double currentYVel = target.getDeltaMovement().y;
            AbilityHelper.setDeltaMovement(target,
                    new Vector3d(nx * 1.2, Math.max(currentYVel, 0.1), nz * 1.2));
        }
    }

    // ── Cleanup + cooldown scaling ────────────────────────────────────────────

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);

        // Scale cooldown linearly from MIN to MAX based on how long it was held
        float heldTicks  = this.continuousComponent.getContinueTime();
        float fraction   = Math.min(1.0f, heldTicks / HOLD_TIME);
        int   cooldown   = MIN_COOLDOWN + (int) ((MAX_COOLDOWN - MIN_COOLDOWN) * fraction);
        super.cooldownComponent.startCooldown(entity, (float) cooldown);

        this.barrierCenter = null;
        this.rainTick      = 0;
    }

    // ── Cylinder containment check ────────────────────────────────────────────

    private boolean isInBarrier(LivingEntity target, double cx, double cy, double cz) {
        double dx = target.getX() - cx;
        double dz = target.getZ() - cz;
        return dx * dx + dz * dz <= BARRIER_RADIUS_SQ
                && target.getY() >= cy
                && target.getY() <= cy + BARRIER_HEIGHT;
    }

    // ── Static init ───────────────────────────────────────────────────────────

    static {
        INSTANCE = (new AbilityCore.Builder<FoxfireExplosionRework>(
                "Foxfire Explosion", AbilityCategory.DEVIL_FRUITS,
                FoxfireExplosionRework::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        RangeComponent.getTooltip(
                                (float) BARRIER_RADIUS, (float) BARRIER_HEIGHT, RangeType.AOE),
                        CooldownComponent.getTooltip((float) MIN_COOLDOWN, (float) MAX_COOLDOWN),
                        ContinuousComponent.getTooltip(HOLD_TIME)
                })
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        RequireMorphComponent.getTooltip()
                })
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceElement(SourceElement.FIRE)
                .build();
    }
}