package net.kazi.kazimod.abilities.GomuRework;

import net.kazi.kazimod.entities.projectiles.GomuGomuNoDawnRocketProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.abilities.gomu.GomuHelper;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.SwingTriggerComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.gomu.GomuGomuNoRocketProjectile;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

import java.util.List;

public class GomuGomuNoRocketRework extends Ability {

    public enum RocketMode {
        NORMAL,
        DAWN_ROCKET
    }

    // How long the shooter floats up before throwing — matches Blue Hole's charge time
    private static final float GRAB_HOLD_TIME  = 30.0F;
    // Upward boost on grab start — same as Blue Hole's startChargeEvent
    private static final float LAUNCH_UP       = 3.0F;
    // Radius to grab nearby enemies — matches Blue Hole feel but wider for Nika
    private static final float GRAB_RADIUS     = 12.0F;
    // Downward throw velocity for grabbed targets
    private static final float THROW_DOWN      = 30.0F;
    private static final float THROW_LATERAL   = 4.0F;
    // Damage on ground slam — same as Blue Hole's final hit
    private static final float SLAM_DAMAGE     = 65.0F;
    // 15 seconds cooldown for Dawn Rocket
    private static final float DAWN_COOLDOWN   = 200.0F;

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "mineminenomi", "gomu_gomu_no_rocket",
            new Pair[]{ImmutablePair.of(
                    "Stretches towards a block, then launches the user on an arch depending on where they fist landed.",
                    (Object) null)});

    private static final TranslationTextComponent GOMU_GOMU_NO_ROCKET_NAME =
            new TranslationTextComponent(WyRegistry.registerName(
                    "ability.mineminenomi.gomu_gomu_no_rocket", "Gomu Gomu no Rocket"));

    private static final TranslationTextComponent GOMU_GOMU_NO_DAWN_ROCKET_NAME =
            new TranslationTextComponent(WyRegistry.registerName(
                    "ability.kazimod.gomu_gomu_no_dawn_rocket", "Gomu Gomu no Dawn Rocket"));

    private static final ResourceLocation GOMU_GOMU_NO_ROCKET_ICON =
            new ResourceLocation("mineminenomi", "textures/abilities/gomu_gomu_no_rocket.png");

    private static final ResourceLocation GOMU_GOMU_NO_DAWN_ROCKET_ICON =
            new ResourceLocation("kazimod", "textures/abilities/gomu_gomu_no_dawn_rocket.png");

    public static final AbilityCore<GomuGomuNoRocketRework> INSTANCE;

    // ── Normal rocket components ───────────────────────────────────────────────
    private final ContinuousComponent continuousComponent =
            (new ContinuousComponent(this))
                    .addStartEvent(this::startContinuityEvent)
                    .addTickEvent(this::duringContinuityEvent)
                    .addEndEvent(this::endContinuityEvent);

    private final SwingTriggerComponent swingTriggerComponent =
            (new SwingTriggerComponent(this)).addSwingEvent(this::triggerSwingEvent);

    private final ProjectileComponent projectileComponent =
            new ProjectileComponent(this, this::createProjectile);

    // ── Dawn Rocket grab-phase components (Blue Hole pattern) ─────────────────
    // dawnChargeComponent drives the "hold targets in air then slam" phase
    private final ChargeComponent dawnChargeComponent =
            (new ChargeComponent(this))
                    .addStartEvent(this::startDawnCharge)
                    .addTickEvent(this::tickDawnCharge)
                    .addEndEvent(this::endDawnCharge);

    public final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);

    private final AltModeComponent<RocketMode> altModeComponent;

    private int airTime;
    private RocketMode currentMode = RocketMode.NORMAL;

    // Targets grabbed during the Dawn Rocket grab phase
    private List<LivingEntity> grabbedTargets = new java.util.ArrayList<>();

    public GomuGomuNoRocketRework(AbilityCore<GomuGomuNoRocketRework> core) {
        super(core);
        this.altModeComponent =
                (new AltModeComponent<>(this, RocketMode.class, RocketMode.NORMAL, true))
                        .addChangeModeEvent(this::altModeChangeEvent);
        this.airTime = 0;
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.continuousComponent,
                this.swingTriggerComponent,
                this.projectileComponent,
                this.dawnChargeComponent,
                this.dealDamageComponent,
                this.altModeComponent
        });
        this.addUseEvent(this::useEvent);
    }

    // ── Exposed so the projectile can trigger the grab phase ──────────────────
    public void startGrabPhase(LivingEntity entity) {
        if (!entity.level.isClientSide) {
            AxisAlignedBB box = entity.getBoundingBox().inflate(GRAB_RADIUS);
            List<LivingEntity> nearby = entity.level.getEntitiesOfClass(
                    LivingEntity.class, box,
                    e -> e != entity && e.distanceTo(entity) <= GRAB_RADIUS
            );

            if (nearby.isEmpty()) {
                this.cooldownComponent.startCooldown(entity, 60.0F);
                return;
            }

            this.dawnChargeComponent.startCharging(entity, GRAB_HOLD_TIME);
        }
    }

    public void startCooldown(LivingEntity entity, float ticks) {
        this.cooldownComponent.startCooldown(entity, ticks);
    }

    // ── Use event ─────────────────────────────────────────────────────────────
    private void useEvent(LivingEntity entity, IAbility ability) {
        if (this.currentMode == RocketMode.DAWN_ROCKET) {
            // Dawn Rocket: skip projectile entirely — immediately do AoE grab
            if (!entity.level.isClientSide) {
                startGrabPhase(entity);
            }
        } else {
            // Normal Rocket: begin continuity so the swing can shoot the projectile
            this.continuousComponent.triggerContinuity(entity);
        }
    }

    // ── Normal rocket continuity ───────────────────────────────────────────────
    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.airTime = 0;
    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        if (!entity.isOnGround() && this.airTime < 10) {
            AbilityHelper.slowEntityFall(entity, 10);
            ++this.airTime;
        }
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        // Only apply normal cooldown here; Dawn Rocket cooldown is applied in endDawnCharge
        this.cooldownComponent.startCooldown(entity, 60.0F);
    }

    private void triggerSwingEvent(LivingEntity entity, IAbility ability) {
        // Dawn Rocket no longer uses the projectile path — guard just in case
        if (this.currentMode == RocketMode.DAWN_ROCKET) return;

        if (this.continuousComponent.isContinuous()) {
            IAbilityData props = AbilityDataCapability.get(entity);
            float speed = GomuHelper.hasGearSecondActive(props) ? 4.0F : 3.125F;

            this.projectileComponent.shoot(entity, speed, 0.0F);
            entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                    (SoundEvent) ModSounds.GOMU_SFX.get(),
                    SoundCategory.PLAYERS, 2.0F, 1.0F);
            entity.swing(Hand.MAIN_HAND, true);
            this.continuousComponent.stopContinuity(entity);
        }
    }

    // ── Dawn Rocket grab phase (Blue Hole pattern) ────────────────────────────

    private void startDawnCharge(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        this.grabbedTargets.clear();

        // Collect targets — startGrabPhase already confirmed there's at least one
        AxisAlignedBB box = entity.getBoundingBox().inflate(GRAB_RADIUS);
        List<LivingEntity> nearby = entity.level.getEntitiesOfClass(
                LivingEntity.class, box,
                e -> e != entity && e.distanceTo(entity) <= GRAB_RADIUS
        );
        this.grabbedTargets.addAll(nearby);

        // Launch shooter upward
        AbilityHelper.setDeltaMovement(entity,
                entity.getDeltaMovement().x,
                LAUNCH_UP,
                entity.getDeltaMovement().z);

        // Pull all targets up with the shooter
        for (LivingEntity target : this.grabbedTargets) {
            AbilityHelper.setDeltaMovement(target,
                    entity.getDeltaMovement().x,
                    LAUNCH_UP,
                    entity.getDeltaMovement().z);
        }

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.GOMU_SFX.get(),
                SoundCategory.PLAYERS, 2.0F, 1.0F);
    }

    private void tickDawnCharge(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        // Keep shooter floating
        AbilityHelper.slowEntityFall(entity);

        for (LivingEntity target : this.grabbedTargets) {
            if (!target.isAlive()) continue;

            // Teleport target to shooter position each tick so they stay locked to the user
            // exactly like Blue Hole's GrabEntityComponent does internally
            target.teleportTo(entity.getX(), entity.getY(), entity.getZ());
            AbilityHelper.setDeltaMovement(target, 0.0, 0.0, 0.0);

            target.addEffect(new EffectInstance((Effect) ModEffects.DIZZY.get(), 10, 0, false, false));
            target.addEffect(new EffectInstance((Effect) ModEffects.MOVEMENT_BLOCKED.get(), 10, 0, false, false));
        }
    }

    private void endDawnCharge(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        for (LivingEntity target : this.grabbedTargets) {
            if (!target.isAlive()) continue;

            // Throw in look direction with heavy downward component — same as Blue Hole:
            // look.multiply(4, 4, 4).add(0, -30, 0)
            Vector3d look = entity.getLookAngle()
                    .multiply(THROW_LATERAL, THROW_LATERAL, THROW_LATERAL)
                    .add(0.0, -THROW_DOWN, 0.0);
            AbilityHelper.setDeltaMovement(target, look);

            dealDamageComponent.hurtTarget(entity, target, SLAM_DAMAGE);
            target.addEffect(new EffectInstance((Effect) ModEffects.DIZZY.get(), 60, 1, false, false));

            ExplosionAbility explosion = AbilityHelper.newExplosion(entity, entity.level,
                    target.getX(), target.getY(), target.getZ(), 5.0F);
            explosion.setStaticDamage(3.0F);
            explosion.doExplosion();
        }

        this.grabbedTargets.clear();

        // After slamming targets down, launch the rocket projectile in the look direction
        this.projectileComponent.shoot(entity, 3.5F, 0.0F);
        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.GOMU_SFX.get(),
                SoundCategory.PLAYERS, 2.0F, 1.0F);
        entity.swing(Hand.MAIN_HAND, true);

        this.cooldownComponent.startCooldown(entity, DAWN_COOLDOWN);
    }

    // ── Alt mode ──────────────────────────────────────────────────────────────
    private void altModeChangeEvent(LivingEntity entity, IAbility ability, RocketMode mode) {
        this.currentMode = mode;
        switch (mode) {
            case DAWN_ROCKET:
                this.setDisplayName(GOMU_GOMU_NO_DAWN_ROCKET_NAME);
                this.setDisplayIcon(GOMU_GOMU_NO_DAWN_ROCKET_ICON);
                break;
            case NORMAL:
            default:
                this.setDisplayName(GOMU_GOMU_NO_ROCKET_NAME);
                this.setDisplayIcon(GOMU_GOMU_NO_ROCKET_ICON);
                break;
        }
    }

    public void switchDawnRocket(LivingEntity entity) {
        this.altModeComponent.setMode(entity, RocketMode.DAWN_ROCKET);
    }

    public void switchNoGear(LivingEntity entity) {
        this.altModeComponent.setMode(entity, RocketMode.NORMAL);
    }

    // ── Projectile factory ────────────────────────────────────────────────────
    private AbilityProjectileEntity createProjectile(LivingEntity entity) {
        if (this.currentMode == RocketMode.DAWN_ROCKET) {
            return new GomuGomuNoDawnRocketProjectile(entity.level, entity, this);
        }
        return new GomuGomuNoRocketProjectile(entity.level, entity, this);
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>(
                "Gomu Gomu no Rocket",
                AbilityCategory.DEVIL_FRUITS,
                GomuGomuNoRocketRework::new))
                .addDescriptionLine(DESCRIPTION)
                .setSourceHakiNature(SourceHakiNature.HARDENING)
                .setSourceType(new SourceType[]{SourceType.FIST})
                .build();
    }
}