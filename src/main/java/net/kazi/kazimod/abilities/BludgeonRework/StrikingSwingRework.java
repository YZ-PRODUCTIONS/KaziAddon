package net.kazi.kazimod.abilities.BludgeonRework;

import java.util.Iterator;
import java.util.List;
import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.init.CartAnimations;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.HandSide;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.DevilFruitHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.GroundParticlesEffect;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

public class StrikingSwingRework extends Ability {
   private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "striking_swing", new Pair[]{ImmutablePair.of("The user charges and swings their mace, stunning nearby enemies.", (Object)null)});
   public static final ParticleEffect PARTICLES = new GroundParticlesEffect(5, 100);
   private static final float COOLDOWN = 200.0F;
   private static final float DAMAGE = 30.0F;
   private static final float RANGE = 7.5F;
   public static final AbilityCore INSTANCE;
   private final ContinuousComponent continuousComponent = new ContinuousComponent(this);
   private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
   private final AnimationComponent animationComponent = new AnimationComponent(this);
   private final RangeComponent rangeComponent = new RangeComponent(this);
   private final ChargeComponent chargeComponent = (new ChargeComponent(this)).addStartEvent(this::startChargeEvent).addTickEvent(this::tickChargeEvent).addEndEvent(this::endChargeEvent);
   private boolean forcedStop = false;

   public StrikingSwingRework(AbilityCore core) {
      super(core);
      this.isNew = true;
      this.addComponents(new AbilityComponent[]{this.chargeComponent, this.animationComponent, this.continuousComponent, this.dealDamageComponent, this.rangeComponent});
      this.addCanUseCheck(AbilityLimits::requiresBluntWeapon);
      this.addUseEvent(this::onUseEvent);
   }

   private void onUseEvent(LivingEntity entity, IAbility ability) {
      if (!this.chargeComponent.isCharging()) {
         if (entity.getMainArm() == HandSide.RIGHT) {
            this.animationComponent.start(entity, CartAnimations.STRIKING_SWING_RIGHT, 10);
         }

         if (entity.getMainArm() == HandSide.LEFT) {
            this.animationComponent.start(entity, CartAnimations.STRIKING_SWING_LEFT, 10);
         }

         this.chargeComponent.startCharging(entity, 0.1F);
         this.forcedStop = false;
      }

   }

   private void startChargeEvent(LivingEntity player, IAbility ability) {
   }

   private void tickChargeEvent(LivingEntity player, IAbility ability) {
      if (!AbilityLimits.canUseBlunt(player)) {
         this.chargeComponent.stopCharging(player);
         this.forcedStop = true;
      }

   }

   private void endChargeEvent(LivingEntity player, IAbility ability) {
      if (!this.forcedStop) {
         ItemStack stack = player.getMainHandItem();
         stack.hurtAndBreak(1, player, (user) -> {
            user.broadcastBreakEvent(EquipmentSlotType.MAINHAND);
         });
         if (!player.level.isClientSide) {
            ((ServerWorld)player.level).getChunkSource().broadcastAndSend(player, new SAnimateHandPacket(player, 0));
         }

         PARTICLES.spawn(player.level, player.getX(), player.getY(), player.getZ(), 0.0, 0.0, 0.0);
         List<LivingEntity> targets = this.rangeComponent.getTargetsInArea(player, 4.5F);
         targets.remove(player);
         targets.removeIf((entity) -> {
            return !entity.isOnGround() && DevilFruitHelper.getDifferenceToFloor(player) > 2.5;
         });
         Iterator iterator = targets.iterator();

         while(iterator.hasNext()) {
            LivingEntity target = (LivingEntity)iterator.next();
            this.dealDamageComponent.hurtTarget(player, target, 30.0F);
            target.addEffect(new EffectInstance((Effect)ModEffects.MOVEMENT_BLOCKED.get(), 40, 0, false, false));
         }
      }

      this.cooldownComponent.startCooldown(player, 200.0F);
   }

   private static boolean canUnlock(LivingEntity entity) {
      if (!(entity instanceof PlayerEntity)) {
         return false;
      } else {
         PlayerEntity player = (PlayerEntity)entity;
         IEntityStats props = EntityStatsCapability.get(player);
         IQuestData questProps = QuestDataCapability.get(player);
         return props.getFightingStyle().equals(CartValues.BLUDGEON) && questProps.hasFinishedQuest(CartQuests.BLUDGEON_TRIAL_01);
      }
   }

   static {
      INSTANCE = (new AbilityCore.Builder("Striking Swing", AbilityCategory.STYLE, StrikingSwingRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, DealDamageComponent.getTooltip(30.0F), ChargeComponent.getTooltip(10.0F), CooldownComponent.getTooltip(200.0F), RangeComponent.getTooltip(4.5F, RangeType.AOE)}).setSourceType(new SourceType[]{SourceType.BLUNT}).setSourceElement(SourceElement.SHOCKWAVE).setSourceHakiNature(SourceHakiNature.IMBUING).setUnlockCheck(StrikingSwingRework::canUnlock).build();
      INSTANCE.setIcon(new net.minecraft.util.ResourceLocation("cartaddon", "textures/abilities/striking_swing.png"));
   }
}


