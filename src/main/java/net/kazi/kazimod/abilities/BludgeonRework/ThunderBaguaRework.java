package net.kazi.kazimod.abilities.BludgeonRework;

import java.util.Iterator;
import java.util.List;
import net.MrMagicalCart.cartaddon.abilities.oni.OniHelper;
import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.init.CartAnimations;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.DamageSource;
import net.minecraft.util.HandSide;
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
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

public class ThunderBaguaRework extends Ability {
   private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "thunder_bagua", new Pair[]{ImmutablePair.of("The user dashes forward and bashes their target, sending them flying. (Shorter charge time with §aOni Awakening§r)", new Object[]{"§a" + Math.round(19.999998F) + "%§r"})});
   private static final float COOLDOWN = 280.0F;
   private static final int CHARGE_TIME = 20;
   private static final float DAMAGE = 40.0F;
   private static final float RANGE = 2.5F;
   public static final AbilityCore INSTANCE;
   private final ChargeComponent chargeComponent = (new ChargeComponent(this)).addStartEvent(this::startChargeEvent).addTickEvent(this::duringChargeEvent).addEndEvent(this::endChargeEvent);
   private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
   private final RangeComponent rangeComponent = new RangeComponent(this);
   private final AnimationComponent animationComponent = new AnimationComponent(this);
   private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);
   private final ContinuousComponent continuousComponent = (new ContinuousComponent(this, true)).addStartEvent(this::startContinuityEvent).addTickEvent(this::duringContinuityEvent).addEndEvent(this::endContinuityEvent);
   private final PoolComponent poolComponent;
   private boolean hasFallDamage;
   private final DamageTakenComponent damageTakenComponent;

   public ThunderBaguaRework(AbilityCore core) {
      super(core);
      this.poolComponent = new PoolComponent(this, ModAbilityPools.GRAB_ABILITY, new AbilityPool2[0]);
      this.damageTakenComponent = new DamageTakenComponent(this, this::onDamageTaken, DamageState.ATTACK);
      this.isNew = true;
      this.addComponents(new AbilityComponent[]{this.damageTakenComponent, this.continuousComponent, this.poolComponent, this.chargeComponent, this.dealDamageComponent, this.rangeComponent, this.animationComponent, this.hitTrackerComponent});
      this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
      this.addCanUseCheck(AbilityLimits::requiresBluntWeapon);
      this.addUseEvent(this::onUseEvent);
   }

   private void onUseEvent(LivingEntity entity, IAbility ability) {
      if (!OniHelper.hasAwakeningActive(entity)) {
         this.chargeComponent.startCharging(entity, 25.0F);
      } else {
         this.chargeComponent.startCharging(entity, 20.0F);
      }

   }

   private void startChargeEvent(LivingEntity entity, IAbility ability) {
      this.hitTrackerComponent.clearHits();
   }

   private void duringChargeEvent(LivingEntity entity, IAbility ability) {
      entity.addEffect(new EffectInstance((Effect)ModEffects.MOVEMENT_BLOCKED.get(), 5, 1, false, false));
   }

   private void endChargeEvent(LivingEntity entity, IAbility ability) {
      this.continuousComponent.startContinuity(entity, 12.5F);
   }

   private void startContinuityEvent(LivingEntity entity, IAbility ability) {
      ItemStack stack = entity.getMainHandItem();
      stack.hurtAndBreak(1, entity, (user) -> {
         user.broadcastBreakEvent(EquipmentSlotType.MAINHAND);
      });
      this.hasFallDamage = false;
      this.hitTrackerComponent.clearHits();
      if (entity.getMainArm() == HandSide.RIGHT) {
         this.animationComponent.start(entity, CartAnimations.DESTROYER_OF_DEATH_RIGHT);
      } else if (entity.getMainArm() == HandSide.LEFT) {
         this.animationComponent.start(entity, CartAnimations.DESTROYER_OF_DEATH_LEFT);
      }

      Vector3d look = entity.getLookAngle().normalize();
      Vector3d speed = look.scale(entity.isOnGround() ? 5.0 : 4.0);
      AbilityHelper.setDeltaMovement(entity, speed.x, speed.y, speed.z);
   }

   private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
      if (entity.isAlive()) {
         List targets = this.rangeComponent.getTargetsInArea(entity, 2.5F);
         Iterator iterator = targets.iterator();

         while(iterator.hasNext()) {
            LivingEntity target = (LivingEntity)iterator.next();
            AbilityDamageSource source = (AbilityDamageSource)this.dealDamageComponent.getDamageSource(entity);
            if (this.hitTrackerComponent.canHit(target) && this.dealDamageComponent.hurtTarget(entity, target, 40.0F, source)) {
            }
         }
      }

   }

   private void endContinuityEvent(LivingEntity entity, IAbility ability) {
      this.animationComponent.stop(entity);
      this.cooldownComponent.startCooldown(entity, 280.0F);
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
         return props.getFightingStyle().equals(CartValues.BLUDGEON) && questProps.hasFinishedQuest(CartQuests.BLUDGEON_TRIAL_02);
      }
   }

   static {
      INSTANCE = (new AbilityCore.Builder("Thunder Bagua", AbilityCategory.STYLE, ThunderBaguaRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, ChargeComponent.getTooltip(20.0F, 25.0F), CooldownComponent.getTooltip(280.0F), DealDamageComponent.getTooltip(40.0F), RangeComponent.getTooltip(2.5F, RangeType.AOE)}).setSourceHakiNature(SourceHakiNature.IMBUING).setSourceType(new SourceType[]{SourceType.BLUNT}).setUnlockCheck(ThunderBaguaRework::canUnlock).build();
      INSTANCE.setIcon(new net.minecraft.util.ResourceLocation("cartaddon", "textures/abilities/thunder_bagua.png"));
   }
}



