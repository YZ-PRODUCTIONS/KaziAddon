//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.RyusokenRework;

import net.MrMagicalCart.cartaddon.init.CartAbilityPools;
import net.MrMagicalCart.cartaddon.init.CartParticleEffects;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.server.SAnimateHandPacket;
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
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityPool2;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class TalonRushRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "talon_rush", new Pair[]{ImmutablePair.of("The user lets out a barrage of talon attacks that damage the opponent. At the end of this ability the user strikes hard, breaking guards.", (Object)null)});
    private static final float COOLDOWN = 240.0F;
    private static final int CHARGE_TIME = 40;
    private static final float DAMAGE = 3.0F;
    private static final float RANGE = 6.5F;
    private final Interval damageInterval = new Interval(10);
    public static final AbilityCore INSTANCE;
    private final ChargeComponent chargeComponent = (new ChargeComponent(this)).addStartEvent(this::startChargeEvent).addTickEvent(this::duringChargeEvent).addEndEvent(this::endChargeEvent);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);
    private final PoolComponent poolComponent;

    private int chargeTicks = 0;

    private long chargeStartTime = 0L;

    public TalonRushRework(AbilityCore core) {
        super(core);
        this.poolComponent = new PoolComponent(this, CartAbilityPools.RYUSOKEN, new AbilityPool2[]{ModAbilityPools.GRAB_ABILITY});
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.poolComponent, this.chargeComponent, this.dealDamageComponent, this.rangeComponent, this.animationComponent, this.hitTrackerComponent});
        super.addCanUseCheck(AbilityHelper::canUseBrawlerAbilities);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.chargeComponent.isCharging()) {
            this.chargeComponent.stopCharging(entity);
        } else {
            this.chargeComponent.startCharging(entity, 40.0F);
        }
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        this.chargeStartTime = System.currentTimeMillis();
        this.hitTrackerComponent.clearHits();
        this.animationComponent.start(entity, ModAnimations.PUNCH_RUSH);
    }

    private void duringChargeEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            this.chargeTicks++;
        }
        entity.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 5, 1, false, false));
        for (LivingEntity target : this.rangeComponent.getTargetsInLine(entity, 6.5F, 4.0F)) {
            if (this.hitTrackerComponent.canHit(target) && this.damageInterval.canTick()) {
                boolean flag = this.dealDamageComponent.hurtTarget(entity, target, 3.0F);
                target.addEffect(new EffectInstance((Effect)ModEffects.MOVEMENT_BLOCKED.get(), 2, 1, false, false));
                if (flag && !entity.level.isClientSide) {
                    WyHelper.spawnParticleEffect((ParticleEffect)CartParticleEffects.RYUSOKEN_IMPACT.get(), entity, target.getX(), target.getEyeY(), target.getZ());
                }
            }
            this.hitTrackerComponent.clearHits();
        }
        if (!entity.level.isClientSide) {
            ((ServerWorld)entity.level).getChunkSource().broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
        }
        entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.SHIGAN_SFX.get(), SoundCategory.PLAYERS, 2.0F, 0.9F);
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();
        for (LivingEntity target : this.rangeComponent.getTargetsInLine(entity, 6.5F, 4.0F)) {
            if (this.hitTrackerComponent.canHit(target)) {
                boolean flag = this.dealDamageComponent.hurtTarget(entity, target, 35.0F);
                AbilityHelper.disableAbilities(target, 100, (abl) -> abl.hasComponent(ModAbilityKeys.POOL) && ((PoolComponent)abl.getComponent(ModAbilityKeys.POOL).get()).containsPool(ModAbilityPools.TEKKAI_LIKE));
                if (flag && !entity.level.isClientSide) {
                    WyHelper.spawnParticleEffect((ParticleEffect)CartParticleEffects.RYUSOKEN_IMPACT.get(), entity, target.getX(), target.getEyeY(), target.getZ());
                }
                Vector3d knockback = entity.getLookAngle().normalize().multiply((double)3.0F, (double)1.0F, (double)3.0F).add((double)0.0F, (double)0.75F, (double)0.0F);
                target.setDeltaMovement(knockback);
            }
        }
        entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.SHIGAN_SFX.get(), SoundCategory.PLAYERS, 3.0F, 0.75F);
        if (!entity.level.isClientSide) {
            ((ServerWorld)entity.level).getChunkSource().broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
        }
        this.animationComponent.stop(entity);

        long elapsedMs = System.currentTimeMillis() - this.chargeStartTime;
        float elapsedSeconds = Math.min(elapsedMs / 1000.0F, 2.0F); // cap at 2 seconds (40 ticks)
        float minCooldown = 7.0F * 20.0F;
        float maxCooldown = 12.0F * 20.0F;
        float scaledCooldown = minCooldown + ((elapsedSeconds / 2.0F) * (maxCooldown - minCooldown));
        this.cooldownComponent.startCooldown(entity, scaledCooldown);
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.getFightingStyle().equals(CartValues.RYUSOKEN) && questProps.hasFinishedQuest(CartQuests.RYUSOKEN_TRIAL_06);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Talon Rush", AbilityCategory.STYLE, TalonRushRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, DealDamageComponent.getTooltip(28.0F, 58.0F), ChargeComponent.getTooltip(40.0F), CooldownComponent.getTooltip(240.0F), RangeComponent.getTooltip(6.5F, RangeType.LINE)}).setSourceHakiNature(SourceHakiNature.HARDENING).setUnlockCheck(TalonRushRework::canUnlock).build();
    }
}