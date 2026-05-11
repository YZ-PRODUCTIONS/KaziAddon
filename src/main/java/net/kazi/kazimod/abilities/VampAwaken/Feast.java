package net.kazi.kazimod.abilities.VampAwaken;

import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityPool2;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.GrabEntityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.GrabEntityComponent.GrabState;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.math.EasingFunctionHelper;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.CommonExplosionParticleEffect;

public class Feast extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "feast",
            new Pair[]{ImmutablePair.of("Grabs an opponent from the back and launches it into the ground", (Object) null)});

    private static final int PULL_TIME = 200;
    private static final int CHARGE_TIME = 20;
    private static final float COOLDOWN = 400.0F;
    private static final float DAMAGE = 40.0F;
    private static final float HEAL_ON_GRAB_HIT = 20.0F;

    private static final float DASH_DURATION = 20.0F;
    private static final double DASH_SPEED_GROUND = 5.0;
    private static final double DASH_SPEED_AIR = 4.0;

    private static final float GRAB_REACH = 4.0F;
    private static final float GRAB_WIDTH = 2.2F;

    public static final AbilityCore<Feast> INSTANCE;

    private final ContinuousComponent continuousComponent =
            (new ContinuousComponent(this, true))
                    .addStartEvent(this::onContinuityStart)
                    .addTickEvent(this::onContinuityTick)
                    .addEndEvent(this::onContinuityEnd);

    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);

    private final ChargeComponent chargeComponent =
            (new ChargeComponent(this))
                    .addStartEvent(this::onChargeStart)
                    .addTickEvent(this::onChargeTick)
                    .addEndEvent(this::onChargeEnd);

    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final GrabEntityComponent grabComponent =
            new GrabEntityComponent(this, true, false, true, 2.0F);
    private final PoolComponent poolComponent;

    public Feast(AbilityCore<Feast> core) {
        super(core);
        this.poolComponent = new PoolComponent(this, ModAbilityPools.GRAB_ABILITY, new AbilityPool2[0]);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.continuousComponent,
                this.dealDamageComponent,
                this.chargeComponent,
                this.animationComponent,
                this.grabComponent,
                this.poolComponent
        });
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!this.continuousComponent.isContinuous() && !this.chargeComponent.isCharging()) {
            this.continuousComponent.triggerContinuity(entity, DASH_DURATION);
        }
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            Vector3d look = entity.getLookAngle().normalize();
            Vector3d speed = look.scale(entity.isOnGround() ? DASH_SPEED_GROUND : DASH_SPEED_AIR);
            AbilityHelper.setDeltaMovement(entity, speed.x, speed.y, speed.z);
        }
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            if (!super.canUse(entity).isFail()) {
                if (!this.grabComponent.hasGrabbedEntity()) {
                    this.grabComponent.grabNearest(entity, GRAB_REACH, GRAB_WIDTH, false);
                }
                if (this.grabComponent.hasGrabbedEntity()) {
                    this.continuousComponent.stopContinuity(entity);
                }
            } else {
                this.continuousComponent.stopContinuity(entity);
            }
        }
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            if (this.grabComponent.getState() != GrabState.GRABBED) {
                this.grabComponent.release(entity);
            }
            if (!this.grabComponent.canContinueGrab(entity)) {
                super.cooldownComponent.startCooldown(entity, 180.0F);
            } else {
                this.chargeComponent.startCharging(entity, (float) CHARGE_TIME);
            }
        }
    }

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, ModAnimations.POINT_ARMS);
    }

    public void onChargeTick(LivingEntity entity, IAbility ability) {
        entity.addEffect(new EffectInstance(Effects.DAMAGE_RESISTANCE, 40, 1, false, false));
        if (!entity.level.isClientSide) {
            if (!super.canUse(entity).isFail() && this.grabComponent.canContinueGrab(entity)) {
                LivingEntity target = this.grabComponent.getGrabbedEntity();
                entity.addEffect(new EffectInstance((Effect) ModEffects.MOVEMENT_BLOCKED.get(), 2, 1));
                target.addEffect(new EffectInstance((Effect) ModEffects.GRABBED.get(), 2, 3));
                float distance = 1.0F;
                Vector3d lookVec = entity.getLookAngle().normalize();
                Vector3d pos = (new Vector3d(
                        lookVec.x * distance,
                        entity.getBbHeight(),
                        lookVec.z * distance))
                        .scale(this.chargeComponent.getChargePercentage());
                AbilityHelper.setDeltaMovement(target,
                        entity.position()
                                .subtract(pos.x, -EasingFunctionHelper.easeInOutSine((float) pos.y), pos.z)
                                .subtract(target.position()), true);
            } else {
                this.chargeComponent.stopCharging(entity);
            }
        }
    }

    public void onChargeEnd(LivingEntity entity, IAbility ability) {
        if (this.grabComponent.hasGrabbedEntity()) {
            LivingEntity target = this.grabComponent.getGrabbedEntity();
            DamageSource source = this.dealDamageComponent.getDamageSource(entity);
            if (this.dealDamageComponent.hurtTarget(entity, target, DAMAGE)) {
                entity.setHealth(Math.min(entity.getMaxHealth(), entity.getHealth() + HEAL_ON_GRAB_HIT));
                target.addEffect(new EffectInstance((Effect) ModEffects.FRAGILE.get(), 60, 0));
            }
            ExplosionAbility explosion = AbilityHelper.newExplosion(
                    entity, entity.level, target.getX(), target.getY(), target.getZ(), 1.0F);
            explosion.setStaticDamage(6.0F);
            explosion.setExplosionSound(true);
            explosion.setDamageOwner(false);
            explosion.setDestroyBlocks(false);
            explosion.setFireAfterExplosion(false);
            explosion.setSmokeParticles(new CommonExplosionParticleEffect(1));
            explosion.setDamageEntities(true);
            explosion.setDamageSource(source);
            this.grabComponent.release(entity);
        }
        this.animationComponent.stop(entity);
        super.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Feast", AbilityCategory.DEVIL_FRUITS, Feast::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN),
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        DealDamageComponent.getTooltip(DAMAGE)
                })
                .setSourceHakiNature(SourceHakiNature.HARDENING)
                .setSourceType(new SourceType[]{SourceType.FIST})
                .build();
    }
}
