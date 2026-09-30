package net.kazi.kazimod.abilities.BludgeonRework;

import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.entities.projectiles.bludgeon.KundaliDragonSwarmProjectile;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RepeaterComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class KundaliDragonSwarmRework extends Ability {
   public static final AbilityCore INSTANCE;
   private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "kundali_dragon_swarm", new Pair[]{ImmutablePair.of("The user lets out a barrage of their staff.", (Object)null)});
   private final ContinuousComponent continuousComponent = (new ContinuousComponent(this)).addStartEvent(this::startContinuityEvent);
   private final RepeaterComponent repeaterComponent = (new RepeaterComponent(this)).addTriggerEvent(this::repeaterTriggerEvent).addStopEvent(this::repeaterStopEvent);
   private int waves = 30;
   private int punchesPerWave = 5;

   public KundaliDragonSwarmRework(AbilityCore core) {
      super(core);
      this.isNew = true;
      this.addComponents(new AbilityComponent[]{this.continuousComponent, this.repeaterComponent});
      this.addUseEvent(this::onUseEvent);
      this.addCanUseCheck(AbilityLimits::requiresBluntWeapon);
   }

   private void onUseEvent(LivingEntity entity, IAbility ability) {
      if (this.continuousComponent.isContinuous()) {
         this.repeaterComponent.stop(entity);
      } else {
         this.continuousComponent.triggerContinuity(entity);
      }

   }

   private void startContinuityEvent(LivingEntity entity, IAbility ability) {
      if (!entity.level.isClientSide) {
         this.repeaterComponent.start(entity, this.waves, 1);
      }

   }

   private void repeaterTriggerEvent(LivingEntity entity, IAbility ability) {
      float speed = 2.6F;
      int projectileSpace = 2;
      float projDmageReduction = 0.6F;

      for(int i = 0; i < this.punchesPerWave; ++i) {
         AbilityProjectileEntity projectile = new KundaliDragonSwarmProjectile(entity.level, entity);
         Vector3d look = entity.getLookAngle();
         projectile.xRot = (float)look.x;
         projectile.yRot = (float)look.y;
         projectile.setKnockbackStrength(0);
         projectile.setEntityCollisionSize(1.25);
         projectile.setMaxLife(5);
         projectile.setDamage(projectile.getDamage() * (1.0F - projDmageReduction));
         projectile.setMaxLife((int)((double)projectile.getMaxLife() * 0.75));
         double px = entity.getX() + WyHelper.randomWithRange(-projectileSpace, projectileSpace) + WyHelper.randomDouble();
         double py = entity.getEyeY() + WyHelper.randomWithRange(0, projectileSpace) + WyHelper.randomDouble();
         double pz = entity.getZ() + WyHelper.randomWithRange(-projectileSpace, projectileSpace) + WyHelper.randomDouble();
         projectile.moveTo(px, py, pz, 0.0F, 0.0F);
         entity.level.addFreshEntity(projectile);
         projectile.shootFromRotation(entity, entity.xRot, entity.yRot, 0.0F, speed, 3.0F);
      }

      entity.swing(Hand.MAIN_HAND, true);
   }

   private void repeaterStopEvent(LivingEntity entity, IAbility ability) {
      this.continuousComponent.stopContinuity(entity);
      super.cooldownComponent.startCooldown(entity, WyHelper.secondsToTicks(10.0F));
   }

   public void setWaveDetails(int waves, int punchesPerWave) {
      this.waves = waves;
      this.punchesPerWave = punchesPerWave;
   }

   private static boolean canUnlock(LivingEntity entity) {
      if (!(entity instanceof PlayerEntity)) {
         return false;
      } else {
         PlayerEntity player = (PlayerEntity)entity;
         IEntityStats props = EntityStatsCapability.get(player);
         IQuestData questProps = QuestDataCapability.get(player);
         return props.getFightingStyle().equals(CartValues.BLUDGEON) && questProps.hasFinishedQuest(CartQuests.BLUDGEON_TRIAL_04);
      }
   }

   static {
      INSTANCE = (new AbilityCore.Builder("Kundali Dragon Swarm", AbilityCategory.STYLE, KundaliDragonSwarmRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(240.0F)}).addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips()).setSourceType(new SourceType[]{SourceType.BLUNT}).setSourceHakiNature(SourceHakiNature.IMBUING).setUnlockCheck(KundaliDragonSwarmRework::canUnlock).build();
      INSTANCE.setIcon(new net.minecraft.util.ResourceLocation("cartaddon", "textures/abilities/kundali_dragon_swarm.png"));
   }
}



