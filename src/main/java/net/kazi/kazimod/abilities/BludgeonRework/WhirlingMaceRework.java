package net.kazi.kazimod.abilities.BludgeonRework;

import java.util.Iterator;
import java.util.List;
import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class WhirlingMaceRework extends Ability {
   private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "whirling_mace", new Pair[]{ImmutablePair.of("The user spins around hitting all nearby enemies with their blunt weapon.", new Object[]{"§a" + Math.round(19.999998F) + "%§r"})});
   private static final int HOLD_TIME = 40;
   private static final float COOLDOWN = 240.0F;
   private static final float DAMAGE = 23.0F;
   private static final float RANGE = 6.0F;
   public static final AbilityCore INSTANCE;
   private final ContinuousComponent continuousComponent = (new ContinuousComponent(this)).addStartEvent(100, this::onStartContinuousEvent).addTickEvent(100, this::onTickContinuousEvent).addEndEvent(100, this::onEndContinuousEvent);
   private final RangeComponent rangeComponent = new RangeComponent(this);
   private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
   private final AnimationComponent animationComponent = new AnimationComponent(this);
   private final PoolComponent poolComponent;

   public WhirlingMaceRework(AbilityCore core) {
      super(core);
      this.poolComponent = new PoolComponent(this, ModAbilityPools.TEKKAI_LIKE, new AbilityPool2[]{ModAbilityPools.GRAB_ABILITY});
      this.isNew = true;
      this.addComponents(new AbilityComponent[]{this.poolComponent, this.continuousComponent, this.rangeComponent, this.dealDamageComponent, this.animationComponent});
      this.addCanUseCheck(AbilityLimits::requiresBluntWeapon);
      this.addUseEvent(this::onUseEvent);
   }

   private void onUseEvent(LivingEntity entity, IAbility ability) {
      this.continuousComponent.triggerContinuity(entity, 40.0F);
   }

   private void onStartContinuousEvent(LivingEntity entity, IAbility ability) {
      ItemStack stack = entity.getMainHandItem();
      stack.hurtAndBreak(1, entity, (user) -> {
         user.broadcastBreakEvent(EquipmentSlotType.MAINHAND);
      });
      this.animationComponent.start(entity, ModAnimations.SPIN_TO_WIN);
   }

   private void onEndContinuousEvent(LivingEntity entity, IAbility ability) {
      this.animationComponent.stop(entity);
      this.cooldownComponent.startCooldown(entity, 240.0F);
   }

   private void onTickContinuousEvent(LivingEntity entity, IAbility ability) {
      List list = this.rangeComponent.getTargetsInArea(entity, 6.0F);
      Iterator var4 = list.iterator();

      while(var4.hasNext()) {
         LivingEntity target = (LivingEntity)var4.next();
         if (this.dealDamageComponent.hurtTarget(entity, target, 10.0F)) {
            Vector3d speed = WyHelper.propulsion(entity, 1.5, 1.5);
            AbilityHelper.setDeltaMovement(target, speed.x, 1.5, speed.z);
         }
      }

      entity.addEffect(new EffectInstance((Effect)ModEffects.PHYSICAL_MOVING_GUARD.get(), 2, 1, false, false));
      if (!AbilityLimits.canUseBlunt(entity)) {
         this.continuousComponent.stopContinuity(entity);
      }

   }

   private static boolean canUnlock(LivingEntity entity) {
      if (!(entity instanceof PlayerEntity)) {
         return false;
      } else {
         PlayerEntity player = (PlayerEntity)entity;
         IEntityStats props = EntityStatsCapability.get(player);
         IQuestData questProps = QuestDataCapability.get(player);
         return props.getFightingStyle().equals(CartValues.BLUDGEON) && questProps.hasFinishedQuest(CartQuests.BLUDGEON_TRIAL_03);
      }
   }

   static {
      INSTANCE = (new AbilityCore.Builder("Whirling Mace", AbilityCategory.STYLE, WhirlingMaceRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, DealDamageComponent.getTooltip(15.0F), ContinuousComponent.getTooltip(40.0F), CooldownComponent.getTooltip(240.0F), RangeComponent.getTooltip(6.0F, RangeType.AOE)}).setSourceHakiNature(SourceHakiNature.IMBUING).setSourceType(new SourceType[]{SourceType.BLUNT}).setUnlockCheck(WhirlingMaceRework::canUnlock).build();
      INSTANCE.setIcon(new net.minecraft.util.ResourceLocation("cartaddon", "textures/abilities/whirling_mace.png"));
   }
}



