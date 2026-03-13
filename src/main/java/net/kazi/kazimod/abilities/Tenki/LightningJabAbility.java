package net.kazi.kazimod.abilities.Tenki;

import java.awt.Color;
import net.MrMagicalCart.cartaddon.init.CartAbilityPools;
import net.kazi.kazimod.entities.projectiles.GomuGomuNoKaminariProjectile;
import net.kazi.kazimod.init.KaziAnimations;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.abilities.goro.ElThorAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityPool2;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent.DamageState;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.GrabEntityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.GrabEntityComponent.GrabState;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.math.VectorHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.entities.LightningDischargeEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

public class LightningJabAbility extends Ability {

    // ── Mode enum ─────────────────────────────────────────────────────────────
    public enum JabMode { NORMAL, LIGHTNING_FURY }

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "lightning_jab",
            new Pair[]{
                    ImmutablePair.of("The user dashes forward and grabs a nearby target, holding them in place before launching them away with a powerful lightning-charged strike.", (Object) null)
            }
    );

    // ── Display names & icons ─────────────────────────────────────────────────
    private static final TranslationTextComponent LIGHTNING_JAB_NAME =
            new TranslationTextComponent(WyRegistry.registerName("ability.kazimod.lightning_jab", "Lightning Jab"));
    private static final TranslationTextComponent LIGHTNING_FURY_NAME =
            new TranslationTextComponent(WyRegistry.registerName("ability.kazimod.lightning_fury", "Lightning Fury"));

    private static final ResourceLocation LIGHTNING_JAB_ICON =
            new ResourceLocation("kazimod", "textures/abilities/lightning_jab.png");
    private static final ResourceLocation LIGHTNING_FURY_ICON =
            new ResourceLocation("kazimod", "textures/abilities/lightning_fury.png");

    private static final int   COOLDOWN      = 300;
    private static final int   FURY_COOLDOWN = 700; // 35 seconds
    private static final float RANGE         = 2.5F;
    private static final float DAMAGE        = 50.0F;

    private static final float BEAM_RANGE    = 90.0F;
    private static final float BEAM_DAMAGE   = 10.0F;
    private static final float BEAM_SIZE     = 0.45F;
    private static final float BEAM_DURATION = 40.0F;

    public static final AbilityCore<LightningJabAbility> INSTANCE;

    // ── Components ────────────────────────────────────────────────────────────
    private final ContinuousComponent continuousComponent =
            (new ContinuousComponent(this, true))
                    .addStartEvent(this::onContinuityStart)
                    .addTickEvent(this::onContinuityTick)
                    .addEndEvent(this::onContinuityEnd);

    private final AltModeComponent<JabMode> altModeComponent;
    private final RangeComponent       rangeComponent      = new RangeComponent(this);
    private final GrabEntityComponent  grabEntityComponent;
    private final DealDamageComponent  dealDamageComponent = new DealDamageComponent(this);
    private final ChargeComponent      chargeComponent     =
            (new ChargeComponent(this))
                    .addStartEvent(this::startChargeEvent)
                    .addTickEvent(this::tickChargeEvent)
                    .addEndEvent(this::endChargeEvent);
    private final PoolComponent        poolComponent;
    private final HitTrackerComponent  hitTrackerComponent;
    private final AnimationComponent   animationComponent  = new AnimationComponent(this);
    private final DamageTakenComponent damageTakenComponent;
    private final ProjectileComponent  projectileComponent;

    private final Interval lightningInterval = new Interval(5);
    private final Interval damageInterval    = new Interval(10);
    private final Interval particleInterval  = new Interval(4);
    boolean hasFallDamage;

    // Mode state
    private JabMode      currentMode  = JabMode.NORMAL;
    private boolean      firingBeam   = false;
    private LivingEntity furyTarget   = null;
    private LightningEntity boltInner = null;
    private LightningEntity boltOuter = null;

    public LightningJabAbility(AbilityCore<LightningJabAbility> core) {
        super(core);
        this.altModeComponent     = (new AltModeComponent<>(this, JabMode.class, JabMode.NORMAL, true))
                .addChangeModeEvent(this::onModeChange);
        this.poolComponent        = new PoolComponent(this, CartAbilityPools.INIT_JUMP, new AbilityPool2[]{ModAbilityPools.GRAB_ABILITY});
        this.hitTrackerComponent  = new HitTrackerComponent(this);
        this.damageTakenComponent = new DamageTakenComponent(this, this::onDamageTaken, DamageState.ATTACK);
        this.grabEntityComponent  = new GrabEntityComponent(this, true, false, true, 2.0F);
        this.projectileComponent  = new ProjectileComponent(this, this::createProjectile);
        this.hasFallDamage        = false;

        super.isNew = true;
        super.addComponents(new AbilityComponent[]{
                this.altModeComponent,
                this.damageTakenComponent,
                this.hitTrackerComponent,
                this.chargeComponent,
                this.poolComponent,
                this.dealDamageComponent,
                this.continuousComponent,
                this.rangeComponent,
                this.grabEntityComponent,
                this.animationComponent,
                this.projectileComponent
        });

        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        super.addUseEvent(this::onUseEvent);
    }

    // ── Mode change ───────────────────────────────────────────────────────────
    private void onModeChange(LivingEntity entity, IAbility ability, JabMode mode) {
        this.currentMode = mode;
        switch (mode) {
            case LIGHTNING_FURY:
                this.setDisplayName(LIGHTNING_FURY_NAME);
                this.setDisplayIcon(LIGHTNING_FURY_ICON);
                break;
            case NORMAL:
            default:
                this.setDisplayName(LIGHTNING_JAB_NAME);
                this.setDisplayIcon(LIGHTNING_JAB_ICON);
                break;
        }
    }

    public void switchFuryMode(LivingEntity entity) {
        this.altModeComponent.setMode(entity, JabMode.LIGHTNING_FURY);
    }

    public void switchNormalMode(LivingEntity entity) {
        this.altModeComponent.setMode(entity, JabMode.NORMAL);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!this.continuousComponent.isContinuous() && !this.chargeComponent.isCharging()) {
            this.continuousComponent.triggerContinuity(entity, 30.0F);
        }
    }

    // ── Dash to grab ──────────────────────────────────────────────────────────
    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();
        if (!entity.level.isClientSide) {
            Vector3d look  = entity.getLookAngle().normalize();
            Vector3d speed = look.scale(entity.isOnGround() ? 4.0 : 3.0);
            AbilityHelper.setDeltaMovement(entity, speed.x, speed.y, speed.z);
        }
        this.animationComponent.start(entity, KaziAnimations.LIGHTNING_FURY);
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {

            // ── Fury beam phase ───────────────────────────────────────────────
            if (firingBeam && furyTarget != null) {
                AbilityHelper.slowEntityFall(entity);

                this.projectileComponent.shoot(entity, 4.0F, 3.0F);

                Vector3d origin = VectorHelper.calculateRotationBasedOffsetPosition(
                        entity.position(), (double) entity.yBodyRot, 0.5, 1.2, 0.8);

                if (boltOuter == null) {
                    spawnFuryBeam(entity, origin);
                } else {
                    boltInner.moveTo(origin.x, origin.y, origin.z, entity.yRot, entity.xRot);
                    boltOuter.moveTo(origin.x, origin.y, origin.z, entity.yRot, entity.xRot);
                }

                if (particleInterval.canTick()) {
                    WyHelper.spawnParticleEffect(
                            (ParticleEffect) ModParticleEffects.SANGO.get(),
                            entity,
                            furyTarget.getX(),
                            furyTarget.getY() + 1.0,
                            furyTarget.getZ());
                }

                if (damageInterval.canTick()) {
                    for (LivingEntity target : this.rangeComponent.getTargetsInLine(entity, BEAM_RANGE, 3.5F)) {
                        if (this.dealDamageComponent.hurtTarget(entity, target, BEAM_DAMAGE)) {
                            target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 40, 2));
                        }
                    }
                }
                return;
            }

            // ── Normal grab phase ─────────────────────────────────────────────
            LivingEntity grabbed = this.grabEntityComponent.getGrabbedEntity();
            if (!super.canUse(entity).isFail() && (grabbed == null || this.grabEntityComponent.canContinueGrab(entity))) {
                if (grabbed == null) {
                    this.grabEntityComponent.grabNearest(entity, 8.0F, 2.0F, false);
                }
            } else {
                this.continuousComponent.stopContinuity(entity);
            }

            if (this.grabEntityComponent.hasGrabbedEntity()) {
                this.continuousComponent.stopContinuity(entity);
            }
        }
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            stopFuryBeam();
            furyTarget = null;
            firingBeam = false;

            if (this.grabEntityComponent.getState() != GrabState.GRABBED) {
                this.grabEntityComponent.release(entity);
                this.animationComponent.stop(entity);
                super.cooldownComponent.startCooldown(entity, (float) COOLDOWN);
            } else if (!this.grabEntityComponent.canContinueGrab(entity)) {
                this.animationComponent.stop(entity);
                super.cooldownComponent.startCooldown(entity, (float) COOLDOWN);
            } else {
                this.chargeComponent.startCharging(entity, 10.0F);
            }
        }
    }

    // ── Hold & charge lightning ───────────────────────────────────────────────
    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        this.hasFallDamage = false;
        this.lightningInterval.restartIntervalToZero();
    }

    private void tickChargeEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            if (!super.canUse(entity).isFail() && this.grabEntityComponent.canContinueGrab(entity)) {
                LivingEntity grabbed = this.grabEntityComponent.getGrabbedEntity();
                entity.addEffect(new EffectInstance((Effect) ModEffects.MOVEMENT_BLOCKED.get(), 2, 1));
                grabbed.addEffect(new EffectInstance((Effect) ModEffects.GRABBED.get(), 2, 3));
                grabbed.teleportTo(entity.getX(), entity.getY(), entity.getZ());

                if (this.lightningInterval.canTick()) {
                    spawnLightning(entity, 0.3F, 5);
                }
            } else {
                this.chargeComponent.stopCharging(entity);
            }
        }
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        if (this.grabEntityComponent.hasGrabbedEntity()) {
            LivingEntity grabbed = this.grabEntityComponent.getGrabbedEntity();

            if (this.dealDamageComponent.hurtTarget(entity, grabbed, DAMAGE)) {
                grabbed.addEffect(new EffectInstance((Effect) ModEffects.DIZZY.get(), 40, 0));
            }

            Vector3d look = entity.getLookAngle()
                    .multiply(4.0, 0.0, 4.0)
                    .add(0.0, 1.4, 0.0);
            AbilityHelper.setDeltaMovement(grabbed, look);

            spawnLightning(entity, 4.0F, 15);

            this.animationComponent.stop(entity);

            if (!entity.level.isClientSide && this.currentMode == JabMode.LIGHTNING_FURY) {
                furyTarget = grabbed;
                firingBeam = true;
                damageInterval.restartIntervalToZero();
                particleInterval.restartIntervalToZero();
                this.continuousComponent.triggerContinuity(entity, BEAM_DURATION);
            }

            this.grabEntityComponent.release(entity);
        }

        this.animationComponent.stop(entity);
        super.cooldownComponent.startCooldown(entity,
                (float)(this.currentMode == JabMode.LIGHTNING_FURY ? FURY_COOLDOWN : COOLDOWN));
    }

    // ── Fury beam helpers ─────────────────────────────────────────────────────
    private void spawnFuryBeam(LivingEntity entity, Vector3d origin) {
        int segments = (int)(BEAM_RANGE * 0.6F);

        boltInner = new LightningEntity(entity,
                origin.x, origin.y, origin.z,
                entity.yRot, entity.xRot,
                BEAM_RANGE, 20.0F, this.getCore());
        boltInner.setSize(BEAM_SIZE * 0.75F);
        boltInner.setColor(new Color(255, 255, 180));
        boltInner.setDamage(0.0F);
        boltInner.setSegments(segments);
        boltInner.setBranches(2);
        boltInner.setAngle(85);
        boltInner.setBoxSizeDivision(0.22);
        boltInner.setCollideWithEntities(false);
        boltInner.setLightningMimic(false);
        boltInner.setMaxLife(120);

        boltOuter = new LightningEntity(entity,
                origin.x, origin.y, origin.z,
                entity.yRot, entity.xRot,
                BEAM_RANGE, 20.0F, this.getCore());
        boltOuter.setSize(BEAM_SIZE);
        boltOuter.setColor(ElThorAbility.YELLOW_THUNDER);
        boltOuter.setDamage(BEAM_DAMAGE);
        boltOuter.setSegments(segments + 4);
        boltOuter.setBranches(4);
        boltOuter.setAngle(100);
        boltOuter.setBoxSizeDivision(0.22);
        boltOuter.setExplosion(3, true, 0.3F);
        boltOuter.disableExplosionKnockback();
        boltOuter.setCollideWithEntities(false);
        boltOuter.setLightningMimic(false);
        boltOuter.setMaxLife(120);
        boltOuter.seed = boltInner.seed;

        entity.level.addFreshEntity(boltInner);
        entity.level.addFreshEntity(boltOuter);

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundCategory.PLAYERS, 2.0F, 0.8F + entity.getRandom().nextFloat() * 0.4F);
    }

    private void stopFuryBeam() {
        if (boltInner != null) { boltInner.remove(); boltInner = null; }
        if (boltOuter != null) { boltOuter.remove(); boltOuter = null; }
    }

    private GomuGomuNoKaminariProjectile createProjectile(LivingEntity entity) {
        return new GomuGomuNoKaminariProjectile(entity.level, entity);
    }

    // ── Discharge spark effect ────────────────────────────────────────────────
    private void spawnLightning(LivingEntity entity, float size, int aliveTicks) {
        Vector3d pos = VectorHelper.calculateRotationBasedOffsetPosition(
                entity.position(), (double) entity.yBodyRot, 0.5, 1.15, 0.8);
        LightningDischargeEntity discharge = new LightningDischargeEntity(
                entity, pos.x, pos.y, pos.z, entity.yRot, entity.xRot);
        discharge.setAliveTicks(aliveTicks);
        discharge.setLightningLength(size);
        discharge.setColor(Color.WHITE);
        discharge.setOutlineColor(ElThorAbility.YELLOW_THUNDER);
        discharge.setRenderTransparent();
        discharge.setDetails(20);
        discharge.setDensity(40);
        discharge.setSize(size);
        discharge.setSkipSegments(1);
        entity.level.addFreshEntity(discharge);
    }

    private float onDamageTaken(LivingEntity entity, IAbility ability, DamageSource damageSource, float damage) {
        if (!this.hasFallDamage && damageSource == DamageSource.FALL) {
            this.hasFallDamage = true;
            return 0.0F;
        }
        return damage;
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Lightning Jab", AbilityCategory.DEVIL_FRUITS, LightningJabAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        DealDamageComponent.getTooltip(DAMAGE),
                        ChargeComponent.getTooltip(10.0F),
                        ContinuousComponent.getTooltip(30.0F),
                        CooldownComponent.getTooltip((float) COOLDOWN),
                        RangeComponent.getTooltip(RANGE, RangeType.LINE)
                })
                .setSourceHakiNature(SourceHakiNature.IMBUING)
                .setSourceType(new SourceType[]{SourceType.FIST})
                .build();
    }
}