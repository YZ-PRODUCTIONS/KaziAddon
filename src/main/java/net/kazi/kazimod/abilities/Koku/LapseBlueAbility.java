package net.kazi.kazimod.abilities.Koku;

import net.kazi.kazimod.entities.projectiles.HollowNukeProjectile;
import net.kazi.kazimod.init.KaziAnimations;
import net.kazi.kazimod.init.KaziParticleEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.GrabEntityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.DevilFruitHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class LapseBlueAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "lapse_blue",
            new Pair[]{ImmutablePair.of("Concentrates the attractive force of Infinity, pulling a target toward you. Once close enough, grab and launch them into the air, then teleport to slam them into the ground.", (Object) null)}
    );

    private static final float COOLDOWN = 400.0F;
    private static final float PULL_CONTINUITY_TIME = 100.0F;
    private static final float CHARGE_TIME = 60.0F;
    private static final float PULL_RANGE = 25.0F;
    private static final float GRAB_INITIATE_DISTANCE = 6.0F;
    private static final float PUNCH_DAMAGE = 10.0F;
    private static final float SLAM_DAMAGE = 25.0F;
    private static final float SLAM_TELEPORT_DELAY = 20.0F;

    public static final AbilityCore<LapseBlueAbility> INSTANCE;

    private final ContinuousComponent pullComponent =
            (new ContinuousComponent(this, true))
                    .addStartEvent(this::onPullStart)
                    .addTickEvent(this::onPullTick)
                    .addEndEvent(this::onPullEnd);

    private final ChargeComponent chargeComponent =
            (new ChargeComponent(this))
                    .addStartEvent(this::onChargeStart)
                    .addTickEvent(this::onChargeTick)
                    .addEndEvent(this::onChargeEnd);

    private final GrabEntityComponent grabEntityComponent =
            new GrabEntityComponent(this, false, true, 1.0F);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);
    private final PoolComponent poolComponent;

    private final KokuChargeVisual pullVisual = new KokuChargeVisual();
    private int pullVisualTicks;

    private LivingEntity slamTarget = null;
    private boolean teleported = false;
    private boolean slammed = false;

    public LapseBlueAbility(AbilityCore<LapseBlueAbility> core) {
        super(core);
        this.poolComponent = new PoolComponent(this, ModAbilityPools.GRAB_ABILITY);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.pullComponent,
                this.chargeComponent,
                this.grabEntityComponent,
                this.dealDamageComponent,
                this.rangeComponent,
                this.animationComponent,
                this.hitTrackerComponent,
                this.poolComponent
        });
        this.addCanUseCheck(this::canUseCheck);
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addUseEvent(this::onUseEvent);
        this.addRemoveEvent((entity, ability) -> this.pullVisual.stop());
    }

    private AbilityUseResult canUseCheck(LivingEntity entity, IAbility ability) {
        IAbilityData data = AbilityDataCapability.get(entity);
        HollowPurpleAbility hollowPurple = (HollowPurpleAbility) data.getEquippedAbility(HollowPurpleAbility.INSTANCE);
        if (hollowPurple != null && hollowPurple.isCharging()) {
            return AbilityUseResult.fail(null);
        }
        if (HollowNukeProjectile.ACTIVE_PROJECTILES.containsKey(entity.getUUID())) {
            return AbilityUseResult.fail(null);
        }
        // Shared cooldown with Max Output: Lapse Blue
        MaxOutputLapseBlueAbility maxBlue = (MaxOutputLapseBlueAbility) data.getEquippedAbility(MaxOutputLapseBlueAbility.INSTANCE);
        if (maxBlue != null && maxBlue.isOnCooldown()) {
            return AbilityUseResult.fail(null);
        }
        return AbilityUseResult.success();
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.pullComponent.isContinuous()) {
            this.pullComponent.stopContinuity(entity);
            return;
        }
        if (!this.chargeComponent.isCharging()) {
            this.pullComponent.triggerContinuity(entity, PULL_CONTINUITY_TIME);
        }
    }

    private void onPullStart(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();
        this.slamTarget = null;
        this.teleported = false;
        this.slammed = false;
        this.animationComponent.start(entity, KaziAnimations.GOJO_BLUE);
        this.pullVisualTicks = 0;
        this.pullVisual.start(entity, ability, net.kazi.kazimod.entities.KokuVfxEntity.BLUE_PULL, (int) PULL_CONTINUITY_TIME);
    }

    private void onPullTick(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            Vector3d look = entity.getLookAngle().normalize();

            this.pullVisual.update(++this.pullVisualTicks / PULL_CONTINUITY_TIME);

            for (int i = 5; i <= (int) PULL_RANGE; i += 3) {
                Vector3d point = entity.position().add(0, 1, 0).add(look.scale(i));

                for (LivingEntity target : WyHelper.getNearbyLiving(
                        point, entity.level, 4.0,
                        ModEntityPredicates.getEnemyFactions(entity))) {

                    Vector3d toUser = entity.position()
                            .add(0, 1, 0)
                            .subtract(target.position())
                            .normalize();
                    AbilityHelper.setDeltaMovement(target,
                            toUser.x * 0.8, toUser.y * 0.8, toUser.z * 0.8);

                    target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 10, 3));
                    target.addEffect(new EffectInstance(
                            (Effect) ModEffects.MOVEMENT_BLOCKED.get(), 10, 3));

                    if (target.distanceTo(entity) <= GRAB_INITIATE_DISTANCE) {
                        if (this.grabEntityComponent.grabNearest(entity, false)) {
                            this.pullComponent.stopContinuity(entity);
                            return;
                        }
                    }
                }
            }
        }
    }

    private void onPullEnd(LivingEntity entity, IAbility ability) {
        this.pullVisual.stop();
        if (!entity.level.isClientSide) {
            if (this.grabEntityComponent.hasGrabbedEntity()) {
                this.chargeComponent.startCharging(entity, CHARGE_TIME);
            } else {
                this.animationComponent.stop(entity);
                super.cooldownComponent.startCooldown(entity, COOLDOWN);
            }
        }
    }

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        this.teleported = false;
        this.slammed = false;
        if (!entity.level.isClientSide) {
            if (this.grabEntityComponent.hasGrabbedEntity()) {
                LivingEntity grabbed = this.grabEntityComponent.getGrabbedEntity();
                this.slamTarget = grabbed;

                this.grabEntityComponent.release(entity);

                this.dealDamageComponent.hurtTarget(entity, grabbed, PUNCH_DAMAGE);
                grabbed.addEffect(new EffectInstance(
                        (Effect) ModEffects.DIZZY.get(), 40, 0, false, false));

                ((ServerWorld) entity.level).getChunkSource()
                        .broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));

                AbilityHelper.setDeltaMovement(grabbed, 0.0, 3.0, 0.0);
            } else {
                this.animationComponent.stop(entity);
                super.cooldownComponent.startCooldown(entity, COOLDOWN);
            }
        }
    }

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            if (slamTarget != null && slamTarget.isAlive() && !slammed) {
                if (!teleported) {
                    AbilityHelper.slowEntityFall(slamTarget);
                    slamTarget.addEffect(new EffectInstance(
                            (Effect) ModEffects.MOVEMENT_BLOCKED.get(), 10, 5, false, false));
                    slamTarget.addEffect(new EffectInstance(
                            (Effect) ModEffects.NO_HANDS.get(), 10, 0, false, false));
                    slamTarget.addEffect(new EffectInstance(
                            (Effect) ModEffects.DIZZY.get(), 10, 0, false, false));
                }

                if (!teleported && this.chargeComponent.getChargeTime() >= SLAM_TELEPORT_DELAY) {
                    teleported = true;
                    entity.teleportTo(
                            slamTarget.getX(),
                            slamTarget.getY() + 5.0,
                            slamTarget.getZ()
                    );
                    AbilityHelper.setDeltaMovement(slamTarget, 0.0, -5.0, 0.0);
                    AbilityHelper.setDeltaMovement(entity, 0.0, -5.0, 0.0);
                }

                if (teleported) {
                    AbilityHelper.slowEntityFall(entity);
                    double distToFloor = DevilFruitHelper.getDifferenceToFloor(slamTarget);

                    slamTarget.addEffect(new EffectInstance(
                            (Effect) ModEffects.DIZZY.get(), 10, 0, false, false));
                    slamTarget.addEffect(new EffectInstance(
                            (Effect) ModEffects.ANTI_KNOCKBACK.get(), 10, 0, false, false));

                    if (distToFloor > 2.0 && this.hitTrackerComponent.canHit(slamTarget)) {
                        ((ServerWorld) entity.level).getChunkSource()
                                .broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
                        this.dealDamageComponent.hurtTarget(entity, slamTarget, SLAM_DAMAGE);
                    }

                    if (distToFloor > 1.0) {
                        AbilityHelper.setDeltaMovement(slamTarget,
                                slamTarget.getDeltaMovement().x, -5.0,
                                slamTarget.getDeltaMovement().z);
                    }

                    if (distToFloor <= 1.5 && !slammed) {
                        slammed = true;
                        ExplosionAbility explosion = AbilityHelper.newExplosion(
                                entity, entity.level,
                                slamTarget.getX(), slamTarget.getY(), slamTarget.getZ(), 5.0F);
                        explosion.setStaticDamage(5.0F);
                        explosion.doExplosion();
                        this.chargeComponent.stopCharging(entity);
                    }
                }
            } else if (slamTarget == null || !slamTarget.isAlive()) {
                this.chargeComponent.stopCharging(entity);
            }
        }
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            slamTarget = null;
            teleported = false;
            slammed = false;
            this.animationComponent.stop(entity);
            super.cooldownComponent.startCooldown(entity, COOLDOWN);
        }
    }

    public boolean isOnCooldown() {
        return this.cooldownComponent.isOnCooldown();
    }

    public void startCooldown(LivingEntity entity) {
        this.cooldownComponent.startCooldown(entity, 700.0F);
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Lapse: Blue", AbilityCategory.DEVIL_FRUITS, LapseBlueAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        DealDamageComponent.getTooltip(PUNCH_DAMAGE, SLAM_DAMAGE),
                        ContinuousComponent.getTooltip(PULL_CONTINUITY_TIME),
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        CooldownComponent.getTooltip(COOLDOWN),
                        RangeComponent.getTooltip(PULL_RANGE, RangeType.LINE)
                })
                .setSourceElement(SourceElement.SHOCKWAVE)
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceType(new SourceType[]{SourceType.FIST, SourceType.INDIRECT})
                .build();
    }
}
