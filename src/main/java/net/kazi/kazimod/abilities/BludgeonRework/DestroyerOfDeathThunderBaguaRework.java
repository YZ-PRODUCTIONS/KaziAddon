package net.kazi.kazimod.abilities.BludgeonRework;

import java.awt.Color;
import java.util.Iterator;
import java.util.List;
import net.MrMagicalCart.cartaddon.abilities.modifiedhuman.ModifiedHumanHelper;
import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.init.CartAnimations;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.HandSide;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent.DamageState;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.HakiHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.haki.HakiDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.haki.IHakiData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.entities.LightningDischargeEntity;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class DestroyerOfDeathThunderBaguaRework extends Ability {
   private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "destroyer_of_death_thunder_bagua", new Pair[]{ImmutablePair.of("The user rushes towards their target, dealing massive damage. Applies dizzy, and knocks the opponent downwards. (Fruitless)", (Object)null)});
   private static final int HOLD_TIME = 15;
   private static final int COOLDOWN = 900;
   private static final float DAMAGE = 110.0F;
   public static final AbilityCore INSTANCE;
   private final ContinuousComponent continuousComponent = (new ContinuousComponent(this, true)).addStartEvent(this::startContinuityEvent).addTickEvent(this::duringContinuityEvent).addEndEvent(this::endContinuityEvent);
   private final RangeComponent rangeComponent = new RangeComponent(this);
   private final ChargeComponent chargeComponent = (new ChargeComponent(this)).addStartEvent(this::startChargeEvent).addTickEvent(this::tickChargeEvent).addEndEvent(this::endChargeEvent);
   private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
   private final AnimationComponent animationComponent = new AnimationComponent(this);
   private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);
   private final DamageTakenComponent damageTakenComponent;
   private boolean hasFallDamage;
   public static int overuse = 1250;
   private LightningDischargeEntity discharge;
   private Color color;
   private int radius;
   private int haoMastery;
   private final PoolComponent poolComponent;

   public DestroyerOfDeathThunderBaguaRework(AbilityCore core) {
      super(core);
      this.damageTakenComponent = new DamageTakenComponent(this, this::onDamageTaken, DamageState.ATTACK);
      this.color = new Color(16711680);
      this.radius = 0;
      this.haoMastery = 0;
      this.poolComponent = new PoolComponent(this, ModAbilityPools.GRAB_ABILITY, new AbilityPool2[0]);
      this.isNew = true;
      this.addComponents(new AbilityComponent[]{this.poolComponent, this.chargeComponent, this.damageTakenComponent, this.continuousComponent, this.rangeComponent, this.dealDamageComponent, this.animationComponent, this.hitTrackerComponent});
      this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
      this.addCanUseCheck(AbilityLimits::requiresBluntWeapon);
      super.addCanUseCheck(ModifiedHumanHelper::checkModifiedHuamn);
      this.addCanUseCheck(AbilityLimits::fruitless);
      this.addUseEvent(this::useEvent);
   }

   private void useEvent(LivingEntity entity, IAbility ability) {
      if (!HakiHelper.hasInfusionActive(entity) && entity instanceof PlayerEntity) {
         entity.sendMessage(new StringTextComponent("You need to activate Hao Infusion to use this move!"), entity.getUUID());
      } else {
         if (!WyHelper.isInChallengeDimension(entity.level)) {
            IEntityStats props = EntityStatsCapability.get(entity);
            boolean isOnMaxOveruse = HakiHelper.checkForHakiOveruse(entity, props.isHuman() ? (int)((double)overuse * 0.7) : overuse);
            if (isOnMaxOveruse) {
               return;
            }
         }

         if (!this.continuousComponent.isContinuous()) {
            this.chargeComponent.startCharging(entity, 15.0F);
         }

      }
   }

   private void startChargeEvent(LivingEntity entity, IAbility ability) {
      entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.HAKI_RELEASE_SFX.get(), SoundCategory.PLAYERS, 3.0F, 0.5F + entity.getRandom().nextFloat());
      IHakiData hakiProps = HakiDataCapability.get(entity);
      float haoLevel = hakiProps.getTotalHakiExp() / 100.0F;
      if (haoLevel <= 1.0F) {
         this.radius = 10;
         this.haoMastery = 0;
      } else if (haoLevel > 1.0F && haoLevel <= 1.75F) {
         this.radius = 25;
         this.haoMastery = 1;
      } else if (haoLevel > 1.75F) {
         this.radius = 40;
         this.haoMastery = 2;
      }

      if (entity instanceof PlayerEntity) {
         this.color = new Color(HakiHelper.getHaoshokuColour(entity));
      }

      this.discharge = new LightningDischargeEntity(entity, entity.getX(), entity.getY() + 1.5, entity.getZ(), entity.yRot, entity.xRot);
      this.discharge.setAliveTicks(-1);
      this.discharge.setUpdateRate(8);
      this.discharge.setLightningLength((float)(this.radius * 2));
      this.discharge.setColor(new Color(0, 0, 0, 100));
      this.discharge.setOutlineColor(this.color);
      this.discharge.setRenderTransparent();
      this.discharge.setDetails(16);
      int density = this.haoMastery == 2 ? 32 : 16;
      this.discharge.setDensity(density);
      this.discharge.setSize(1.0F);
      this.discharge.setSkipSegments(1);
      if (this.haoMastery == 0) {
         this.discharge.setSplit();
      }

      if (entity instanceof PlayerEntity) {
         entity.level.addFreshEntity(this.discharge);
         if (this.discharge != null) {
            this.discharge.setAliveTicks(40);
         }
      }

   }

   private void tickChargeEvent(LivingEntity entity, IAbility ability) {
      entity.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN.getEffect(), 2, 1, false, false));
      if (this.chargeComponent.getChargeTime() % 5.0F == 0.0F) {
         this.discharge.setPos(entity.getX(), entity.getY() + 1.0, entity.getZ());
      }

      if (this.chargeComponent.getChargeTime() % 10.0F == 0.0F) {
         entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.HAKI_RELEASE_SFX.get(), SoundCategory.PLAYERS, 3.0F, 0.5F + entity.getRandom().nextFloat());
      }

      if (this.discharge != null && !entity.isAlive()) {
         this.discharge.setAliveTicks(0);
      }

   }

   private void endChargeEvent(LivingEntity entity, IAbility ability) {
      if (this.discharge != null) {
         this.discharge.setAliveTicks(30);
      }

      this.continuousComponent.startContinuity(entity, 12.5F);
   }

   private void startContinuityEvent(LivingEntity entity, IAbility ability) {
      this.hasFallDamage = false;
      this.hitTrackerComponent.clearHits();
      if (entity.getMainArm() == HandSide.RIGHT) {
         this.animationComponent.start(entity, CartAnimations.DESTROYER_OF_DEATH_RIGHT);
      } else if (entity.getMainArm() == HandSide.LEFT) {
         this.animationComponent.start(entity, CartAnimations.DESTROYER_OF_DEATH_LEFT);
      }

      Vector3d look = entity.getLookAngle().normalize();
      Vector3d speed = look.scale(entity.isOnGround() ? 6.0 : 5.0);
      AbilityHelper.setDeltaMovement(entity, speed.x, speed.y, speed.z);
   }

   private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
      if (entity.isAlive()) {
         List targets = this.rangeComponent.getTargetsInArea(entity, 2.5F);

         LivingEntity target;
         for(Iterator iterator = targets.iterator(); iterator.hasNext(); AbilityHelper.setDeltaMovement(target, entity.getDeltaMovement().x, -6.0, entity.getDeltaMovement().x)) {
            target = (LivingEntity)iterator.next();
            AbilityDamageSource source = (AbilityDamageSource)this.dealDamageComponent.getDamageSource(entity);
            source.setUnavoidable();
            if (this.hitTrackerComponent.canHit(target) && this.dealDamageComponent.hurtTarget(entity, target, 110.0F, source)) {
               target.addEffect(new EffectInstance((Effect)ModEffects.DIZZY.get(), 20, 0, false, false));
               target.addEffect(new EffectInstance((Effect)ModEffects.PARALYSIS.get(), 20, 0, false, false, true));
            }
         }
      }

   }

   private void endContinuityEvent(LivingEntity entity, IAbility ability) {
      this.animationComponent.stop(entity);
      this.cooldownComponent.startCooldown(entity, 900.0F);
   }

   private float onDamageTaken(LivingEntity entity, IAbility ability, DamageSource damageSource, float damage) {
      if (!this.hasFallDamage && damageSource == DamageSource.FALL) {
         this.hasFallDamage = true;
         return 0.0F;
      } else {
         return damage;
      }
   }

   private static boolean canUnlock(LivingEntity entity) {
      if (!(entity instanceof PlayerEntity)) {
         return false;
      } else {
         PlayerEntity player = (PlayerEntity)entity;
         IEntityStats props = EntityStatsCapability.get(player);
         IQuestData questProps = QuestDataCapability.get(player);
         return props.getFightingStyle().equals(CartValues.BLUDGEON) && questProps.hasFinishedQuest(CartQuests.BLUDGEON_TRIAL_07);
      }
   }

   static {
      INSTANCE = (new AbilityCore.Builder("Destroyer of Death Thunder Bagua", AbilityCategory.STYLE, DestroyerOfDeathThunderBaguaRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, ChargeComponent.getTooltip(15.0F), ContinuousComponent.getTooltip(12.5F), DealDamageComponent.getTooltip(110.0F), CooldownComponent.getTooltip(900.0F), RangeComponent.getTooltip(2.0F, RangeType.AOE)}).setSourceType(new SourceType[]{SourceType.BLUNT}).setSourceHakiNature(SourceHakiNature.IMBUING).setUnlockCheck(DestroyerOfDeathThunderBaguaRework::canUnlock).build();
      INSTANCE.setIcon(new net.minecraft.util.ResourceLocation("cartaddon", "textures/abilities/destroyer_of_death_thunder_bagua.png"));
   }
}



