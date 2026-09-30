package net.kazi.kazimod.abilities.BludgeonRework;

import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.entities.projectiles.bludgeon.VajraArrowProjectile;
import net.MrMagicalCart.cartaddon.init.CartParticleEffects;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class VajraArrowRework extends Ability {
   private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "vajra_arrow", new Pair[]{ImmutablePair.of("The user fires a long-range projectile-like shockwave at their opponent.", (Object)null)});
   private static final float COOLDOWN = 500.0F;
   public static final AbilityCore INSTANCE;
   private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);
   private final AnimationComponent animationComponent = new AnimationComponent(this);
   private final ChargeComponent chargeComponent = (new ChargeComponent(this)).addStartEvent(this::startChargeEvent).addTickEvent(this::tickChargeEvent).addEndEvent(this::endChargeEvent);

   public VajraArrowRework(AbilityCore core) {
      super(core);
      this.isNew = true;
      this.addComponents(new AbilityComponent[]{this.projectileComponent, this.animationComponent, this.chargeComponent});
      this.addCanUseCheck(AbilityLimits::requiresBluntWeapon);
      this.addUseEvent(this::useEvent);
   }

   private void useEvent(LivingEntity entity, IAbility ability) {
      this.chargeComponent.startCharging(entity, 30.0F);
   }

   private void startChargeEvent(LivingEntity entity, IAbility ability) {
   }

   private void tickChargeEvent(LivingEntity entity, IAbility ability) {
      WyHelper.spawnParticleEffect((ParticleEffect)CartParticleEffects.VAJRA_ARROW.get(), entity, entity.getX(), entity.getY(), entity.getZ());
      entity.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 2, 1, false, false));
      if (!AbilityLimits.canUseBlunt(entity)) {
         this.chargeComponent.stopCharging(entity);
      }

   }

   private void endChargeEvent(LivingEntity entity, IAbility ability) {
      if (AbilityLimits.canUseBlunt(entity) && this.chargeComponent.getChargeTime() == 30.0F) {
         this.projectileComponent.shoot(entity, 3.0F, 1.2F);
         if (!entity.level.isClientSide) {
            ((ServerWorld)entity.level).getChunkSource().broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
         }
      }

      this.cooldownComponent.startCooldown(entity, 640.0F);
   }

   private VajraArrowProjectile createProjectile(LivingEntity entity) {
      VajraArrowProjectile proj = new VajraArrowProjectile(entity.level, entity);
      return proj;
   }

   private static boolean canUnlock(LivingEntity entity) {
      if (!(entity instanceof PlayerEntity)) {
         return false;
      } else {
         PlayerEntity player = (PlayerEntity)entity;
         IEntityStats props = EntityStatsCapability.get(player);
         IQuestData questProps = QuestDataCapability.get(player);
         return props.getFightingStyle().equals(CartValues.BLUDGEON) && questProps.hasFinishedQuest(CartQuests.BLUDGEON_TRIAL_05);
      }
   }

   static {
      INSTANCE = (new AbilityCore.Builder("Vajra Arrow", AbilityCategory.STYLE, VajraArrowRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, ChargeComponent.getTooltip(30.0F), CooldownComponent.getTooltip(500.0F)}).addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips()).setSourceElement(SourceElement.SHOCKWAVE).setSourceHakiNature(SourceHakiNature.IMBUING).setUnlockCheck(VajraArrowRework::canUnlock).build();
      INSTANCE.setIcon(new net.minecraft.util.ResourceLocation("cartaddon", "textures/abilities/vajra_arrow.png"));
   }
}



