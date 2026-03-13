package net.kazi.kazimod.abilities.RyusokenRework;

import java.util.List;
import net.MrMagicalCart.cartaddon.init.CartAbilityPools;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import net.kazi.kazimod.init.KaziParticleEffects;

public class DragonWhirlwindAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("kazimod", "dragon_whirlwind", new Pair[]{
            ImmutablePair.of("The user charges briefly before dashing forward in any direction, spinning like a whirlwind, slashing and pulling nearby opponents towards them.", (Object) null)
    });

    // 0.5 seconds = 10 ticks
    private static final float CHARGE_TIME = 10.0F;
    // 1 second = 20 ticks
    private static final float CONTINUOUS_TIME = 20.0F;
    private static final float COOLDOWN = 160.0F;
    private static final float DAMAGE = 45.0F;
    private static final float RANGE = 5.5F;
    // How strongly targets are pulled toward the user on each hit interval
    private static final double PULL_STRENGTH = 1.2;

    public static final AbilityCore<DragonWhirlwindAbility> INSTANCE;

    private final ChargeComponent chargeComponent = (new ChargeComponent(this))
            .addStartEvent(this::onStartChargeEvent)
            .addTickEvent(this::onTickChargeEvent)
            .addEndEvent(this::onEndChargeEvent);

    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this))
            .addStartEvent(this::onStartContinuousEvent)
            .addTickEvent(this::onTickContinuousEvent)
            .addEndEvent(this::onEndContinuousEvent);

    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final PoolComponent poolComponent;

    private final Interval damageInterval = new Interval(10);

    public DragonWhirlwindAbility(AbilityCore<DragonWhirlwindAbility> core) {
        super(core);
        this.isNew = true;
        this.poolComponent = new PoolComponent(this, CartAbilityPools.RYUSOKEN, new AbilityPool2[]{CartAbilityPools.INIT_JUMP});
        this.addComponents(new AbilityComponent[]{
                this.chargeComponent, this.continuousComponent,
                this.dealDamageComponent, this.rangeComponent,
                this.hitTrackerComponent, this.animationComponent,
                this.poolComponent
        });
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        super.addCanUseCheck(AbilityHelper::canUseBrawlerAbilities);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!this.chargeComponent.isCharging() && !this.continuousComponent.isContinuous()) {
            this.chargeComponent.startCharging(entity, CHARGE_TIME);
        }
        if (this.continuousComponent.isContinuous()) {
            this.continuousComponent.stopContinuity(entity);
        }
    }

    // -------------------------
    // Charge phase
    // -------------------------

    private void onStartChargeEvent(LivingEntity entity, IAbility ability) {
        // Brief wind-up; nothing special needed on start
    }

    private void onTickChargeEvent(LivingEntity entity, IAbility ability) {
        // Lock movement while charging
        entity.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 2, 1, false, false));
        this.animationComponent.start(entity, ModAnimations.BODY_ROTATION_WIDE_ARMS);
    }

    private void onEndChargeEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity, CONTINUOUS_TIME);
    }

    // -------------------------
    // Continuous (dash) phase
    // -------------------------

    private void onStartContinuousEvent(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();
        this.damageInterval.restartIntervalToZero();

        entity.level.playSound(
                (PlayerEntity) null,
                entity.blockPosition(),
                (SoundEvent) ModSounds.SPIN.get(),
                SoundCategory.PLAYERS,
                2.0F,
                0.25F + entity.getRandom().nextFloat() / 12.0F
        );
    }

    private void onTickContinuousEvent(LivingEntity entity, IAbility ability) {
        if (!entity.isAlive()) return;

        // Force constant velocity every tick — unaffected by gravity or friction.
        entity.fallDistance = 0.0F;
        Vector3d look = entity.getLookAngle().normalize();
        Vector3d speed = look.scale(1.35);
        entity.setDeltaMovement(speed);
        entity.hurtMarked = true; // flag the client to sync the new velocity immediately

        // AOE damage + pull on interval
        if (this.damageInterval.canTick()) {
            List<LivingEntity> targets = this.rangeComponent.getTargetsInArea(entity, RANGE);
            AbilityDamageSource source = (AbilityDamageSource) ((ModDamageSource) this.dealDamageComponent.getDamageSource(entity)).setSlash();

            for (LivingEntity target : targets) {
                if (this.hitTrackerComponent.canHit(target)) {
                    this.dealDamageComponent.hurtTarget(entity, target, DAMAGE, source);

                    // Pull the target toward the user
                    Vector3d toUser = entity.position()
                            .subtract(target.position())
                            .normalize()
                            .scale(PULL_STRENGTH);
                    AbilityHelper.setDeltaMovement(target, toUser.x, toUser.y, toUser.z);
                }
            }
        }

        // Spawn small wind particles arranged in a rotating circle at the AOE radius
        if (!entity.level.isClientSide) {
            int particleCount = 16;
            double radius = RANGE;
            double angleStep = (2.0 * Math.PI) / particleCount;
            // Offset angle each tick so the ring appears to spin
            double timeOffset = this.continuousComponent.getContinueTime() * 0.4;
            for (int i = 0; i < particleCount; i++) {
                double angle = angleStep * i + timeOffset;
                double px = entity.getX() + Math.cos(angle) * radius;
                double pz = entity.getZ() + Math.sin(angle) * radius;
                // Two heights per point gives the ring vertical depth
                WyHelper.spawnParticleEffect((ParticleEffect) KaziParticleEffects.WIND.get(), entity,
                        px, entity.getY() + 0.5, pz);
                WyHelper.spawnParticleEffect((ParticleEffect) KaziParticleEffects.WIND.get(), entity,
                        px, entity.getY() + 1.5, pz);
            }

            // Periodic spin sound
            if (this.continuousComponent.getContinueTime() % 5.0F == 0.0F) {
                entity.level.playSound(
                        (PlayerEntity) null,
                        entity.blockPosition(),
                        (SoundEvent) ModSounds.SPIN.get(),
                        SoundCategory.PLAYERS,
                        2.0F,
                        0.25F + entity.getRandom().nextFloat() / 12.0F
                );
            }
        }
    }

    private void onEndContinuousEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);
        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    // -------------------------
    // Unlock check
    // -------------------------

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        }
        PlayerEntity player = (PlayerEntity) entity;
        IEntityStats props = EntityStatsCapability.get(player);
        IQuestData questProps = QuestDataCapability.get(player);
        return props.getFightingStyle().equals(CartValues.RYUSOKEN)
                && questProps.hasFinishedQuest(CartQuests.RYUSOKEN_TRIAL_03);
    }

    // -------------------------
    // Static initializer
    // -------------------------

    static {
        INSTANCE = (new AbilityCore.Builder("Dragon Whirlwind", AbilityCategory.STYLE, DragonWhirlwindAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        ContinuousComponent.getTooltip(CONTINUOUS_TIME),
                        DealDamageComponent.getTooltip(DAMAGE),
                        CooldownComponent.getTooltip(COOLDOWN),
                        RangeComponent.getTooltip(RANGE, RangeType.AOE)
                })
                .setSourceHakiNature(SourceHakiNature.HARDENING)
                .setSourceType(new SourceType[]{SourceType.FIST})
                .setUnlockCheck(DragonWhirlwindAbility::canUnlock)
                .build();
    }
}