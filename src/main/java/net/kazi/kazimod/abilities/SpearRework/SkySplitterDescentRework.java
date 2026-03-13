//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.SpearRework;

import java.util.List;
import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.init.CartAbilityPools;
import net.MrMagicalCart.cartaddon.init.CartEffects;
import net.MrMagicalCart.cartaddon.init.CartParticleEffects;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityPool2;
import xyz.pixelatedw.mineminenomi.api.abilities.DropHitAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.DevilFruitHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.particles.effects.CommonExplosionParticleEffect;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class SkySplitterDescentRework extends DropHitAbility {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "sky_splitter_descent", new Pair[]{ImmutablePair.of("The user gains height and rapidly accelerates down at their enemy, stunning them.", (Object)null)});
    public static final AbilityCore<SkySplitterDescentRework> INSTANCE;
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final PoolComponent poolComponent;
    private static final int COOLDOWN = 1400;
    private static final float RANGE = 12.0F;
    private static final float DMG = 80.0F;

    public SkySplitterDescentRework(AbilityCore<SkySplitterDescentRework> core) {
        super(core);
        this.poolComponent = new PoolComponent(this, CartAbilityPools.INIT_JUMP, new AbilityPool2[]{ModAbilityPools.GRAB_ABILITY});
        this.addComponents(new AbilityComponent[]{this.poolComponent, this.poolComponent, this.animationComponent, this.dealDamageComponent, this.rangeComponent});
        this.continuousComponent.addStartEvent(100, this::startContinuityEvent).addTickEvent(this::tickContinuityEvent).addEndEvent(100, this::endContinuityEvent);
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addCanUseCheck(AbilityLimits::requiresSpear);
        this.addCanUseCheck(AbilityLimits::fruitless);
    }

    public void onLanding(LivingEntity entity) {
        this.animationComponent.stop(entity);
        List<LivingEntity> targets = this.rangeComponent.getTargetsInArea(entity, 12.0F);
        targets.remove(entity);
        AbilityDamageSource source = (AbilityDamageSource)ModDamageSource.causeAbilityDamage(entity, this.getCore()).setFistDamage();

        for(LivingEntity target : targets) {
            float damage = 80.0F;
            if (this.hitTrackerComponent.canHit(target) && this.dealDamageComponent.hurtTarget(entity, target, damage, source)) {
                target.addEffect(new EffectInstance((Effect)ModEffects.DIZZY.get(), 40));
                target.addEffect(new EffectInstance((Effect)CartEffects.HEAVYBLEEDING.get(), 60));
                AbilityHelper.disableAbilities(target, 100, (abl) -> abl.hasComponent(ModAbilityKeys.POOL) && ((PoolComponent)abl.getComponent(ModAbilityKeys.POOL).get()).containsPool(ModAbilityPools.TEKKAI_LIKE));
            }
        }

        List<LivingEntity> targets2 = this.rangeComponent.getTargetsInArea(entity, 12.0F);
        targets2.remove(entity);
        source = (AbilityDamageSource)ModDamageSource.causeAbilityDamage(entity, this.getCore()).setFistDamage();

        for(LivingEntity target : targets2) {
            if (this.hitTrackerComponent.canHit(target) && entity.canSee(target) && this.dealDamageComponent.hurtTarget(entity, target, 80.0F, source)) {
            }
        }

        if (!entity.level.isClientSide) {
            if (targets.size() > 0) {
                ((ServerWorld)entity.level).getChunkSource().broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
            }

            entity.addEffect(new EffectInstance((Effect)ModEffects.MOVEMENT_BLOCKED.get(), 10));
        }

    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        entity.addEffect(new EffectInstance((Effect)ModEffects.DIZZY.get(), 30, 0));
        Vector3d speed = WyHelper.propulsion(entity, (double)1.0F, (double)1.0F);
        AbilityHelper.setDeltaMovement(entity, speed.x, (double)4.0F, speed.z);
    }

    private void tickContinuityEvent(LivingEntity entity, IAbility ability) {
        if (this.continuousComponent.getContinueTime() < 30.0F && this.continuousComponent.getContinueTime() % 5.0F == 0.0F) {
            entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.DASH_ABILITY_SWOOSH_SFX.get(), SoundCategory.PLAYERS, 2.0F, 1.75F);
            WyHelper.spawnParticleEffect((ParticleEffect)CartParticleEffects.MERCIES_CHARGING.get(), entity, entity.getX(), entity.getY(), entity.getZ());
        }

        List<LivingEntity> targets = this.rangeComponent.getTargetsInArea(entity, 3.0F);
        targets.remove(entity);
        if (this.continuousComponent.getContinueTime() >= 30.0F) {
            Vector3d speed = entity.getLookAngle().multiply((double)6.75F, (double)1.0F, (double)6.75F);
            AbilityHelper.setDeltaMovement(entity, speed.x, (double)-5.0F, speed.z);

            for(LivingEntity target : targets) {
                this.dealDamageComponent.hurtTarget(entity, target, 80.0F);
                target.addEffect(new EffectInstance((Effect)ModEffects.MOVEMENT_BLOCKED.get(), 20, 0, false, false));
                target.addEffect(new EffectInstance((Effect)ModEffects.DIZZY.get(), 60, 0, false, false));
            }

            ExplosionAbility explosion = AbilityHelper.newExplosion((Entity)null, entity.level, entity.getX(), entity.getY(), entity.getZ(), 4.0F);
            explosion.setExplosionSound(true);
            explosion.setDamageEntities(false);
            explosion.setDamageOwner(false);
            if (DevilFruitHelper.getDifferenceToFloor(entity) > (double)3.0F) {
                explosion.setDestroyBlocks(true);
            } else {
                explosion.setDestroyBlocks(false);
            }

            explosion.setSmokeParticles(new CommonExplosionParticleEffect(3));
            explosion.doExplosion();
        }

    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.cooldownComponent.startCooldown(entity, 1400.0F);
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.getFightingStyle().equals(CartValues.SPEAR) && questProps.hasFinishedQuest(CartQuests.SPEAR_06);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Sky Splitter Descent", AbilityCategory.STYLE, SkySplitterDescentRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, DealDamageComponent.getTooltip(15.0F, 80.0F), CooldownComponent.getTooltip(1400.0F), RangeComponent.getTooltip(5.0F, 12.0F, RangeType.AOE)}).setSourceType(new SourceType[]{SourceType.SLASH}).setSourceElement(SourceElement.SHOCKWAVE).setSourceHakiNature(SourceHakiNature.SPECIAL).setUnlockCheck(SkySplitterDescentRework::canUnlock).build();
    }
}
