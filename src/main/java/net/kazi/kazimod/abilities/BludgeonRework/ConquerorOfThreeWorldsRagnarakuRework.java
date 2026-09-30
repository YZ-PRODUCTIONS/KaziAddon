package net.kazi.kazimod.abilities.BludgeonRework;

import java.util.Iterator;
import java.util.List;
import net.MrMagicalCart.cartaddon.abilities.oni.OniHelper;
import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.init.CartAbilityPools;
import net.MrMagicalCart.cartaddon.init.CartAnimations;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
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
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class ConquerorOfThreeWorldsRagnarakuRework extends DropHitAbility {
   private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "conqueror_of_three_worlds_ragnaraku", new Pair[]{ImmutablePair.of("The user leaps into the air, and delivers a devastating slam. (With §aOni Awakening§r knocks out opponents)", new Object[]{"§a" + Math.round(19.999998F) + "%§r"})});
   public static final AbilityCore INSTANCE;
   private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
   private final RangeComponent rangeComponent = new RangeComponent(this);
   private final AnimationComponent animationComponent = new AnimationComponent(this);
   private final PoolComponent poolComponent;
   private static final int COOLDOWN = 600;
   private static final float RANGE = 15.0F;
   private static final int AWK_COOLDOWN = 800;
   private static final float AWK_DAMAGE = 80.0F;
   private static final float DMG = 70.0F;
   private boolean awakened;

   public ConquerorOfThreeWorldsRagnarakuRework(AbilityCore core) {
      super(core);
      this.poolComponent = new PoolComponent(this, CartAbilityPools.INIT_JUMP, new AbilityPool2[0]);
      this.awakened = false;
      this.addComponents(new AbilityComponent[]{this.poolComponent, this.animationComponent, this.dealDamageComponent, this.rangeComponent});
      this.continuousComponent.addStartEvent(100, this::startContinuityEvent).addTickEvent(this::tickContinuityEvent).addEndEvent(100, this::endContinuityEvent);
      this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
      this.addCanUseCheck(AbilityLimits::requiresBluntWeapon);
   }

   public void onLanding(LivingEntity entity) {
      this.animationComponent.stop(entity);
      List targets = this.rangeComponent.getTargetsInArea(entity, 5.0F);
      targets.remove(entity);
      AbilityDamageSource source = (AbilityDamageSource)ModDamageSource.causeAbilityDamage(entity, this.getCore()).setFistDamage();
      Iterator var = targets.iterator();

      while(var.hasNext()) {
         LivingEntity target = (LivingEntity)var.next();
         float damage = 30.0F;
         int duration = 100;
         if (OniHelper.hasAwakeningActive(entity)) {
            damage = 80.0F;
            duration = 140;
         }

         if (this.continuousComponent.getContinueTime() < 30.0F) {
            damage = 20.0F;
         }

         if (this.hitTrackerComponent.canHit(target) && entity.canSee(target) && this.dealDamageComponent.hurtTarget(entity, target, damage, source)) {
            target.addEffect(new EffectInstance((Effect)ModEffects.DIZZY.get(), 20));
            target.addEffect(new EffectInstance((Effect)ModEffects.ANTI_KNOCKBACK.get(), 20));
            if (OniHelper.hasAwakeningActive(entity) && damage > 20.0F) {
               target.addEffect(new EffectInstance((Effect)ModEffects.UNCONSCIOUS.get(), 25));
            }

            AbilityHelper.disableAbilities(target, duration, (abl) -> {
               return abl.hasComponent(ModAbilityKeys.POOL) && ((PoolComponent)abl.getComponent(ModAbilityKeys.POOL).get()).containsPool(ModAbilityPools.TEKKAI_LIKE);
            });
         }
      }

      if (!entity.level.isClientSide) {
         if (targets.size() > 0) {
            ((ServerWorld)entity.level).getChunkSource().broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
         }

         entity.addEffect(new EffectInstance((Effect)ModEffects.MOVEMENT_BLOCKED.get(), 10));
         entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.GURA_SFX.get(), SoundCategory.PLAYERS, 5.0F, 2.0F);
      }

   }

   private void startContinuityEvent(LivingEntity entity, IAbility ability) {
      entity.addEffect(new EffectInstance((Effect)ModEffects.DIZZY.get(), 30, 0));
      this.awakened = false;
      Vector3d speed = WyHelper.propulsion(entity, 1.0, 1.0);
      AbilityHelper.setDeltaMovement(entity, speed.x, 4.0, speed.z);
      this.animationComponent.start(entity, CartAnimations.CONQUEROR_OF_THREE_WORLDS);
   }

   private void tickContinuityEvent(LivingEntity entity, IAbility ability) {
      if (this.continuousComponent.getContinueTime() >= 30.0F) {
         Vector3d speed = entity.getLookAngle().multiply(2.75, 1.0, 2.75);
         AbilityHelper.setDeltaMovement(entity, speed.x, -7.0, speed.z);
         if (this.continuousComponent.getContinueTime() == 30.0F) {
            entity.level.playSound((PlayerEntity)null, entity.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 50.0F, 0.9F);
         }
      }

      if (OniHelper.hasAwakeningActive(entity)) {
         this.awakened = true;
      }

      if (!AbilityLimits.canUseBlunt(entity)) {
         this.continuousComponent.stopContinuity(entity);
      }

   }

   private void endContinuityEvent(LivingEntity entity, IAbility ability) {
      if (!this.awakened) {
         this.cooldownComponent.startCooldown(entity, 760.0F);
      } else {
         this.cooldownComponent.startCooldown(entity, 900.0F);
      }

      this.animationComponent.stop(entity);
   }

   private static boolean canUnlock(LivingEntity entity) {
      if (!(entity instanceof PlayerEntity)) {
         return false;
      } else {
         PlayerEntity player = (PlayerEntity)entity;
         IEntityStats props = EntityStatsCapability.get(player);
         IQuestData questProps = QuestDataCapability.get(player);
         return props.getFightingStyle().equals(CartValues.BLUDGEON) && questProps.hasFinishedQuest(CartQuests.BLUDGEON_TRIAL_06);
      }
   }

   static {
      INSTANCE = (new AbilityCore.Builder("Conqueror of Three Worlds Ragnaraku", AbilityCategory.STYLE, ConquerorOfThreeWorldsRagnarakuRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, DealDamageComponent.getTooltip(70.0F, 80.0F), CooldownComponent.getTooltip(600.0F, 800.0F), RangeComponent.getTooltip(5.0F, RangeType.AOE)}).setSourceType(new SourceType[]{SourceType.BLUNT}).setSourceHakiNature(SourceHakiNature.IMBUING).setUnlockCheck(ConquerorOfThreeWorldsRagnarakuRework::canUnlock).build();
      INSTANCE.setIcon(new net.minecraft.util.ResourceLocation("cartaddon", "textures/abilities/conqueror_of_three_worlds_ragnaraku.png"));
   }
}



