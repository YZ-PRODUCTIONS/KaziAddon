package net.kazi.kazimod.abilities.GomuRework;

import java.awt.Color;
import net.kazi.kazimod.entities.projectiles.GomuGomuNoKaminariProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.math.VectorHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class GomuGomuNoKaminariAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("kazimod", "gomu_gomu_no_kaminari", new Pair[]{
            ImmutablePair.of("The user launches into the air, winds up, and unleashes a devastating light-blue lightning beam.", (Object) null)
    });

    private static final int COOLDOWN    = 600;
    private static final float CHARGE_TIME = 60.0F;
    private static final float BEAM_DURATION = 80.0F;
    private static final float VERTICAL_BOOST = 3.0F;
    private static final float RANGE     = 90.0F;
    private static final float DAMAGE    = 14.0F;
    private static final float BEAM_SIZE = 0.45F;

    public static final AbilityCore<GomuGomuNoKaminariAbility> INSTANCE;

    private final ChargeComponent chargeComponent =
            (new ChargeComponent(this))
                    .addStartEvent(this::startChargingEvent)
                    .addTickEvent(this::tickChargingEvent)
                    .addEndEvent(this::endChargingEvent);

    private final ContinuousComponent continuousComponent =
            (new ContinuousComponent(this, true))
                    .addTickEvent(this::tickBeamEvent)
                    .addEndEvent(this::endBeamEvent);

    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent           = new RangeComponent(this);
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);
    private final AnimationComponent animationComponent   = new AnimationComponent(this);

    private LightningEntity boltInner;
    private LightningEntity boltOuter;

    private final Interval damageInterval  = new Interval(10);
    private final Interval particleInterval = new Interval(4);

    public GomuGomuNoKaminariAbility(AbilityCore<GomuGomuNoKaminariAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.projectileComponent,
                this.chargeComponent,
                this.continuousComponent,
                this.dealDamageComponent,
                this.rangeComponent,
                this.animationComponent
        });
        this.addUseEvent(this::useEvent);
    }

    /** Exposes the charge component so GearFifthRework can cancel this ability on end. */
    public ChargeComponent getChargeComponent() {
        return this.chargeComponent;
    }

    /** Exposes the continuous component so GearFifthRework can cancel this ability on end. */
    public ContinuousComponent getContinuousComponent() {
        return this.continuousComponent;
    }

    /* ==================== USE ==================== */

    private void useEvent(LivingEntity entity, IAbility ability) {
        IAbilityData props = AbilityDataCapability.get(entity);
        GearFifthRework gearFifth = (GearFifthRework) props.getEquippedAbility(GearFifthRework.INSTANCE);
        boolean gearFifthActive = gearFifth != null
                && gearFifth.getContinuousComponent() != null
                && gearFifth.getContinuousComponent().isContinuous();

        if (!gearFifthActive) {
            if (entity instanceof PlayerEntity) {
                entity.sendMessage(
                        new StringTextComponent("Gomu Gomu no Kaminari can only be used during Gear Fifth!"),
                        entity.getUUID());
            }
            return;
        }

        if (!this.chargeComponent.isCharging() && !this.continuousComponent.isContinuous()) {
            this.chargeComponent.startCharging(entity, CHARGE_TIME);
        }
    }

    /* ==================== CHARGE ==================== */

    private void startChargingEvent(LivingEntity entity, IAbility ability) {
        AbilityHelper.setDeltaMovement(entity,
                entity.getDeltaMovement().x,
                VERTICAL_BOOST,
                entity.getDeltaMovement().z);

        if (!entity.level.isClientSide) {
            this.animationComponent.start(entity, ModAnimations.RAISE_RIGHT_ARM);
        }

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundCategory.PLAYERS, 1.5F, 0.6F + entity.getRandom().nextFloat() * 0.4F);
    }

    private void tickChargingEvent(LivingEntity entity, IAbility ability) {
        AbilityHelper.slowEntityFall(entity);

        if (this.chargeComponent.getChargeTime() % 10.0F == 0.0F) {
            entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER,
                    SoundCategory.PLAYERS, 2.0F, 0.7F + entity.getRandom().nextFloat() * 0.5F);
        }
    }

    private void endChargingEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            this.animationComponent.stop(entity);
        }
        this.continuousComponent.startContinuity(entity, BEAM_DURATION);
        if (!entity.level.isClientSide) {
            ((ServerWorld) entity.level).getChunkSource()
                    .broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
        }
    }

    /* ==================== BEAM ==================== */

    private void tickBeamEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            AbilityHelper.slowEntityFall(entity);

            this.projectileComponent.shoot(entity, 4.0F, 3.0F);

            BlockRayTraceResult trace = WyHelper.rayTraceBlocks(entity, (double) 0.25F);
            Direction dir = Direction.fromYRot((double) entity.yRot);
            Vector3d hitVec = trace.getLocation().add(
                    (double) dir.getStepX(),
                    (double) dir.getStepY(),
                    (double) dir.getStepZ());
            Vector3d origin = VectorHelper.calculateRotationBasedOffsetPosition(
                    entity.position(), (double) entity.yBodyRot, (double) 0.5F, 1.2, 0.8);

            if (this.boltOuter == null) {
                this.spawnBeam(entity, origin, hitVec);
            } else {
                this.boltInner.moveTo(origin.x, origin.y, origin.z, entity.yRot, entity.xRot);
                this.boltOuter.moveTo(origin.x, origin.y, origin.z, entity.yRot, entity.xRot);
            }

            if (this.particleInterval.canTick()) {
                WyHelper.spawnParticleEffect(
                        (ParticleEffect) ModParticleEffects.SANGO.get(),
                        entity,
                        trace.getLocation().x(),
                        entity.getY() + 1.0,
                        trace.getLocation().z());
            }

            if (this.damageInterval.canTick()) {
                for (LivingEntity target : this.rangeComponent.getTargetsInLine(entity, RANGE, 3.5F)) {
                    boolean flag = this.dealDamageComponent.hurtTarget(entity, target, DAMAGE);
                    if (flag) {
                        target.addEffect(new net.minecraft.potion.EffectInstance(
                                net.minecraft.potion.Effects.MOVEMENT_SLOWDOWN, 40, 2));
                    }
                }
            }
        }
    }

    private void spawnBeam(LivingEntity entity, Vector3d origin, Vector3d hitVec) {
        int segments = (int) (RANGE * 0.6F);

        this.boltInner = new LightningEntity(entity,
                origin.x, origin.y, origin.z,
                entity.yRot, entity.xRot,
                RANGE, 20.0F, this.getCore());
        this.boltInner.setSize(BEAM_SIZE * 0.75F);
        this.boltInner.setColor(new Color(200, 230, 255));
        this.boltInner.setDamage(0.0F);
        this.boltInner.setSegments(segments);
        this.boltInner.setBranches(2);
        this.boltInner.setAngle(85);
        this.boltInner.setBoxSizeDivision((double) 0.22F);
        this.boltInner.setCollideWithEntities(false);
        this.boltInner.setLightningMimic(false);
        this.boltInner.setMaxLife(120);

        this.boltOuter = new LightningEntity(entity,
                origin.x, origin.y, origin.z,
                entity.yRot, entity.xRot,
                RANGE, 20.0F, this.getCore());
        this.boltOuter.setSize(BEAM_SIZE);
        this.boltOuter.setColor(new Color(30, 144, 255));
        this.boltOuter.setDamage(DAMAGE);
        this.boltOuter.setSegments(segments + 4);
        this.boltOuter.setBranches(4);
        this.boltOuter.setAngle(100);
        this.boltOuter.setBoxSizeDivision((double) 0.22F);
        this.boltOuter.setExplosion(3, true, 0.3F);
        this.boltOuter.disableExplosionKnockback();
        this.boltOuter.setCollideWithEntities(false);
        this.boltOuter.setLightningMimic(false);
        this.boltOuter.setMaxLife(120);
        this.boltOuter.seed = this.boltInner.seed;

        entity.level.addFreshEntity(this.boltInner);
        entity.level.addFreshEntity(this.boltOuter);
    }

    private void endBeamEvent(LivingEntity entity, IAbility ability) {
        if (this.boltInner != null) {
            this.boltInner.remove();
            this.boltInner = null;
        }
        if (this.boltOuter != null) {
            this.boltOuter.remove();
            this.boltOuter = null;
        }
        this.cooldownComponent.startCooldown(entity, (float) COOLDOWN);
    }

    /* ==================== PROJECTILE ==================== */

    private GomuGomuNoKaminariProjectile createProjectile(LivingEntity entity) {
        return new GomuGomuNoKaminariProjectile(entity.level, entity);
    }

    /* ==================== UNLOCK CHECK ==================== */

    private static boolean canUnlock(LivingEntity user) {
        return DevilFruitCapability.get(user).hasAwakenedFruit();
    }

    /* ==================== STATIC INIT ==================== */

    static {
        INSTANCE = (new AbilityCore.Builder<>("Gomu Gomu no Kaminari", AbilityCategory.DEVIL_FRUITS, GomuGomuNoKaminariAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        DealDamageComponent.getTooltip(DAMAGE, DAMAGE * 2),
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        ContinuousComponent.getTooltip(BEAM_DURATION),
                        CooldownComponent.getTooltip((float) COOLDOWN),
                        RangeComponent.getTooltip(RANGE, RangeType.LINE)
                })
                .setSourceHakiNature(SourceHakiNature.HARDENING)
                .setSourceElement(SourceElement.LIGHTNING)
                .setUnlockCheck(GomuGomuNoKaminariAbility::canUnlock)
                .build();
    }
}