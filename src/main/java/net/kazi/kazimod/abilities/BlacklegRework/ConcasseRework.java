//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package net.kazi.kazimod.abilities.BlacklegRework;

import java.util.List;

import net.MrMagicalCart.cartaddon.abilities.blacklegextra.CartDiableJambeAbility;
import net.MrMagicalCart.cartaddon.init.CartParticleEffects;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.DropHitAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class ConcasseRework extends DropHitAbility {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("kazimod", "concasserework", new Pair[]{ImmutablePair.of("Leaps forward kicking all nearby enemies for moderate damage and knocking them down", (Object)null)});
    private static final int COOLDOWN = 300;
    private static final float RANGE = 2.0F;
    private static final float DAMAGE = 15.0F;
    public static final AbilityCore<ConcasseRework> INSTANCE;
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private Interval particleInterval = new Interval(2);

    public ConcasseRework(AbilityCore<ConcasseRework> core) {
        super(core);
        super.isNew = true;
        super.addComponents(new AbilityComponent[]{this.dealDamageComponent, this.rangeComponent, this.animationComponent});
        super.continuousComponent.addStartEvent(100, this::startContinuityEvent);
        super.continuousComponent.addEndEvent(100, this::onContinuityEnd);
        super.continuousComponent.addTickEvent(100, this::tickContinuityEvent);
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        super.hitTrackerComponent.clearHits();
        this.particleInterval.restartIntervalToZero();
        Vector3d speed = entity.getLookAngle();
        AbilityHelper.setDeltaMovement(entity, speed.x, 1.3, speed.z);
        this.animationComponent.start(entity, ModAnimations.PITCH_SPIN);
    }

    private void tickContinuityEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            if (entity instanceof PlayerEntity && ((PlayerEntity)entity).abilities.flying) {
                this.continuousComponent.stopContinuity(entity);
            } else {
                if (entity.fallDistance > 0.0F) {
                    boolean targetHurt = false;
                    List<LivingEntity> targets = this.rangeComponent.getTargetsInArea(entity, 2.0F);
                    targets.remove(entity);

                    for(LivingEntity target : targets) {
                        if (super.hitTrackerComponent.canHit(target) && entity.blockPosition().getY() > target.blockPosition().getY() && this.dealDamageComponent.hurtTarget(entity, target, 15.0F)) {
                            target.addEffect(new EffectInstance((Effect)ModEffects.UNCONSCIOUS.get(), 20, 0, false, false));
                            AbilityHelper.setDeltaMovement(target, entity.getDeltaMovement().x, (double)-1.5F, entity.getDeltaMovement().z);
                            targetHurt = true;
                        }
                    }

                    if (targetHurt) {
                        if (!entity.level.isClientSide) {
                            ((ServerWorld)entity.level).getChunkSource().broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
                        }

                        super.continuousComponent.stopContinuity(entity);
                    }
                }

                if (this.particleInterval.canTick()) {
                    CartDiableJambeAbility diableJambeAbility = (CartDiableJambeAbility)AbilityDataCapability.get(entity).getEquippedAbility(CartDiableJambeAbility.INSTANCE);
                    boolean isAbilityEnabled = diableJambeAbility != null && diableJambeAbility.isContinuous();
                    if (isAbilityEnabled && !entity.isOnGround()) {
                        if (!diableJambeAbility.isIfrit()) {
                            WyHelper.spawnParticleEffect((ParticleEffect)ModParticleEffects.CONCASSE_DIABLE.get(), entity, entity.getX(), entity.getY() + (double)1.5F, entity.getZ());
                        } else if (diableJambeAbility.isIfrit()) {
                            WyHelper.spawnParticleEffect((ParticleEffect)CartParticleEffects.CONCASSE_IFRIT.get(), entity, entity.getX(), entity.getY() + (double)1.5F, entity.getZ());
                        }
                    }
                }
            }
        }

    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);
        super.cooldownComponent.startCooldown(entity, 300.0F);
    }

    public void onLanding(LivingEntity entity) {
    }



    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.isBlackLeg() && questProps.hasFinishedQuest(CartQuests.BLACKLEG_TRIAL_01);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Concasse", AbilityCategory.STYLE, ConcasseRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(300.0F), DealDamageComponent.getTooltip(15.0F), RangeComponent.getTooltip(1.7F, RangeType.AOE)}).setSourceHakiNature(SourceHakiNature.HARDENING).setSourceType(new SourceType[]{SourceType.FIST}).setUnlockCheck(ConcasseRework::canUnlock).build();
    }
}
