//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.BrawlerRework;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SPlayerPositionLookPacket;
import net.minecraft.network.play.server.SPlayerPositionLookPacket.Flags;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityPool2;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.GrabEntityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTriggerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.GrabEntityComponent.GrabState;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.HakiHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class SpinningBrawlRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "spinning_brawl", new Pair[]{ImmutablePair.of("Grabs a nearby enemy spinning them around damaging any nearby entity it touches, and ending by throwing the grabbed entity a few blocks away.", (Object)null)});
    private static final double THROW_POWER_XZ = (double)2.0F;
    private static final double THROW_POWER_Y = (double)0.5F;
    private static final int SPIN_DAMAGE = 10;
    private static final int MAIN_DAMAGE = 50;
    private static final int COOLDOWN = 180;
    private static final int CHARGE_TIME = 60;
    private static final int THROW_TIME = 40;
    private static final int PULL_TIME = 200;
    public static final AbilityCore<SpinningBrawlRework> INSTANCE;
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final ChargeComponent chargeComponent = (new ChargeComponent(this)).addStartEvent(100, this::onChargeStart).addTickEvent(this::onChargeTick).addEndEvent(this::onChargeEnd);
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this, true)).addTickEvent(this::onContinuityTick).addEndEvent(this::onContinuityEnd);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final GrabEntityComponent grabComponent = (new GrabEntityComponent(this, true, false, true, 2.0F)).addPullStartEvent(this::onPullStart).addPullEndEvent(this::onPullEnd);
    private final HitTriggerComponent hitTriggerComponent = (new HitTriggerComponent(this)).addOnHitEvent(this::onHitEvent);
    private final PoolComponent poolComponent;
    private final Interval clearHitsInterval;

    public SpinningBrawlRework(AbilityCore<SpinningBrawlRework> core) {
        super(core);
        this.poolComponent = new PoolComponent(this, ModAbilityPools.GRAB_ABILITY, new AbilityPool2[0]);
        this.clearHitsInterval = new Interval(20);
        super.isNew = true;
        super.addComponents(new AbilityComponent[]{this.dealDamageComponent, this.chargeComponent, this.continuousComponent, this.hitTrackerComponent, this.animationComponent, this.grabComponent, this.hitTriggerComponent, this.poolComponent});
        super.addCanUseCheck(AbilityHelper::canUseBrawlerAbilities);
        super.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.chargeComponent.isCharging()) {
            this.chargeComponent.stopCharging(entity);
        } else {
            this.clearHitsInterval.restartIntervalToZero();
            if (!this.continuousComponent.isContinuous() || this.grabComponent.getState() != GrabState.IDLE && this.grabComponent.getState() != GrabState.PULLING) {
                if (this.grabComponent.getState() == GrabState.IDLE && this.grabComponent.grabNearest(entity, 5.5F, 2.5F, false)) {
                    this.grabComponent.triggerPulling(entity);
                } else {
                    this.continuousComponent.startContinuity(entity, 40.0F);
                }
            } else {
                this.grabComponent.release(entity);
                this.continuousComponent.stopContinuity(entity);
            }
        }

    }

    private boolean onHitEvent(LivingEntity entity, LivingEntity target, ModDamageSource source, IAbility ability) {
        if (this.continuousComponent.isContinuous() && !this.grabComponent.hasGrabbedEntity()) {
            if (this.grabComponent.grabManually(entity, target)) {
                this.grabComponent.startPulling(entity);
            }

            target.addEffect(new EffectInstance((Effect)ModEffects.ANTI_KNOCKBACK.get(), 1));
            return false;
        } else {
            return true;
        }
    }

    public void onPullStart(LivingEntity entity, IAbility ability) {
        this.continuousComponent.setThresholdTime(entity, 200.0F);
    }

    public void onPullEnd(LivingEntity entity, IAbility ability) {
        this.continuousComponent.stopContinuity(entity);
        if (this.grabComponent.canContinueGrab(entity)) {
            this.chargeComponent.startCharging(entity, 60.0F);
        }

    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (this.grabComponent.getState() != GrabState.IDLE) {
            if (!this.grabComponent.canContinueGrab(entity)) {
                this.continuousComponent.stopContinuity(entity);
            } else if (this.grabComponent.getState() == GrabState.THROWN) {
                LivingEntity grabbedTarget = this.grabComponent.getGrabbedEntity();
                if (grabbedTarget.isOnGround()) {
                    this.grabComponent.release(entity);
                    this.continuousComponent.stopContinuity(entity);
                } else {
                    List<LivingEntity> targets = WyHelper.getNearbyLiving(grabbedTarget.position(), entity.level, (double)grabbedTarget.getBbWidth(), (double)grabbedTarget.getBbHeight(), (double)grabbedTarget.getBbWidth(), (Predicate)null);
                    targets.remove(grabbedTarget);
                    Vector3d dir = entity.getLookAngle().normalize().scale((double)2.0F);

                    for(LivingEntity target : targets) {
                        if (this.hitTrackerComponent.canHit(target) && this.dealDamageComponent.hurtTarget(entity, target, 10.0F)) {
                            AbilityHelper.setDeltaMovement(target, dir.x, (double)0.5F, dir.z);
                        }
                    }

                    if (this.clearHitsInterval.canTick()) {
                        this.hitTrackerComponent.clearHits();
                    }
                }
            }
        }


    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            if (this.grabComponent.getState() != GrabState.GRABBED) {
                this.grabComponent.release(entity);
            }
            if (!this.grabComponent.canContinueGrab(entity)) {
                if (!super.cooldownComponent.isOnCooldown()) {
                    super.cooldownComponent.startCooldown(entity, 10.0F);
                }
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
                LivingEntity grabbedTarget = this.grabComponent.getGrabbedEntity();
                entity.addEffect(new EffectInstance((Effect)ModEffects.MOVEMENT_BLOCKED.get(), 2, 1));
                grabbedTarget.addEffect(new EffectInstance((Effect)ModEffects.GRABBED.get(), 2, 3));
                Vector3d direction = grabbedTarget.position().subtract(entity.position()).normalize();
                float targetYaw = (float)Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0F;
                float adjustedYaw = (targetYaw + 10.0F) % 360.0F;
                entity.yRot = entity.yRotO = adjustedYaw;
                entity.xRot = entity.xRotO = 0.0F;
                if (entity instanceof PlayerEntity) {
                    Set<SPlayerPositionLookPacket.Flags> flags = EnumSet.of(Flags.X, Flags.Y, Flags.Z);
                    ((ServerPlayerEntity)entity).connection.teleport(entity.getX(), entity.getY(), entity.getZ(), entity.yRot, entity.xRot, flags);
                }

                float distance = 2.0F;
                Vector3d lookVec = entity.getLookAngle().normalize();
                Vector3d pos = new Vector3d(lookVec.x * (double)distance, (double)(entity.getEyeHeight() / 2.0F) + lookVec.y * (double)distance, lookVec.z * (double)distance);
                AbilityHelper.setDeltaMovement(grabbedTarget, entity.position().add(pos).subtract(grabbedTarget.position()), true);
                List<LivingEntity> targets = WyHelper.getNearbyLiving(entity.position(), entity.level, (double)grabbedTarget.getBbWidth(), (double)grabbedTarget.getBbHeight(), (double)grabbedTarget.getBbWidth(), ModEntityPredicates.getEnemyFactions(entity));
                targets.remove(grabbedTarget);
                if (!HakiHelper.hasHardeningActive(entity)) {
                    targets.removeIf((targetx) -> DevilFruitCapability.get(entity).isLogia());
                }

                Vector3d dir = lookVec.scale((double)2.0F);

                for(LivingEntity target : targets) {
                    if (this.hitTrackerComponent.canHit(target) && this.dealDamageComponent.hurtTarget(entity, target, 10.0F)) {
                        AbilityHelper.setDeltaMovement(target, dir.x, (double)0.5F, dir.z);
                    }
                }

                if (this.clearHitsInterval.canTick()) {
                    this.hitTrackerComponent.clearHits();
                }
            } else {
                this.chargeComponent.stopCharging(entity);
            }
        }

    }

    public void onChargeEnd(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            if (this.grabComponent.hasGrabbedEntity()) {
                LivingEntity grabbedTarget = this.grabComponent.getGrabbedEntity();
                if (this.dealDamageComponent.hurtTarget(entity, grabbedTarget, 50.0F)) {
                }
                this.grabComponent.throwTarget(entity, (double)2.0F, (double)0.5F);
                this.continuousComponent.startContinuity(entity, 40.0F);
                super.cooldownComponent.startCooldown(entity, 180.0F); // full cooldown here
            } else {
                super.cooldownComponent.startCooldown(entity, 180.0F);
            }
            this.animationComponent.stop(entity);
        }
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.isBrawler() && questProps.hasFinishedQuest(CartQuests.BRAWLER_TRIAL_02);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Spinning Brawl", AbilityCategory.STYLE, SpinningBrawlRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(180.0F), ChargeComponent.getTooltip(60.0F), DealDamageComponent.getTooltip(50.0F)}).setSourceHakiNature(SourceHakiNature.HARDENING).setUnlockCheck(SpinningBrawlRework::canUnlock).build();
    }
}
