package net.kazi.kazimod.abilities.BludgeonRework;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import net.MrMagicalCart.cartaddon.abilities.modifiedhuman.ModifiedHumanHelper;
import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.entities.projectiles.bludgeon.ShinsokuHakujakuProjectile;
import net.MrMagicalCart.cartaddon.init.CartAnimations;
import net.MrMagicalCart.cartaddon.init.CartParticleEffects;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.HandSide;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.TargetHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class ShinsokuHakujakuRework extends Ability {
   private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "shinsoku_hakujaku", new Pair[]{ImmutablePair.of("The user dashes and drags nearby targets. At the end of this ability, the user fires a White Serpent that applies dizzy and knockback. (Fruitless)", (Object)null)});
   private static final float COOLDOWN = 800.0F;
   private static final float RANGE = 20.0F;
   private static final float DMG = 40.0F;
   private static final float CHARGE_TIME = 55.0F;
   public static final AbilityCore INSTANCE;
   private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);
   private final AnimationComponent animationComponent = new AnimationComponent(this);
   private final ChargeComponent chargeComponent = (new ChargeComponent(this)).addStartEvent(this::startChargeEvent).addTickEvent(this::tickChargeEvent).addEndEvent(this::endChargeEvent);
   private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
   private final RangeComponent rangeComponent = new RangeComponent(this);
   private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);
   private final PoolComponent poolComponent;

   public ShinsokuHakujakuRework(AbilityCore core) {
      super(core);
      this.poolComponent = new PoolComponent(this, ModAbilityPools.GRAB_ABILITY, new AbilityPool2[0]);
      this.isNew = true;
      this.addComponents(new AbilityComponent[]{this.poolComponent, this.dealDamageComponent, this.rangeComponent, this.hitTrackerComponent, this.projectileComponent, this.animationComponent, this.chargeComponent});
      this.addCanUseCheck(AbilityLimits::requiresBluntWeapon);
      this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
      super.addCanUseCheck(ModifiedHumanHelper::checkModifiedHuamn);
      this.addCanUseCheck(AbilityLimits::fruitless);
      this.addUseEvent(this::useEvent);
   }

   private void useEvent(LivingEntity entity, IAbility ability) {
      this.chargeComponent.startCharging(entity, 55.0F);
   }

   private void startChargeEvent(LivingEntity entity, IAbility ability) {
      this.hitTrackerComponent.clearHits();
      if (entity.getMainArm() == HandSide.RIGHT) {
         this.animationComponent.start(entity, CartAnimations.WHITE_SERPENT_RIGHT);
      } else {
         this.animationComponent.start(entity, CartAnimations.WHITE_SERPENT_LEFT);
      }

   }

   private void tickChargeEvent(LivingEntity entity, IAbility ability) {
      WyHelper.spawnParticleEffect((ParticleEffect)CartParticleEffects.WHITE_SERPENT.get(), entity, entity.getX(), entity.getY(), entity.getZ());
      entity.addEffect(new EffectInstance((Effect)ModEffects.MOVEMENT_BLOCKED.get(), 2, 0, false, false));
      if (!AbilityLimits.canUseBlunt(entity)) {
         this.chargeComponent.stopCharging(entity);
      }

   }

   private void endChargeEvent(LivingEntity entity, IAbility ability) {
      if (AbilityLimits.canUseBlunt(entity) && this.chargeComponent.getChargeTime() == 55.0F) {
         ItemStack stack = entity.getMainHandItem();
         stack.hurtAndBreak(1, entity, (user) -> {
            user.broadcastBreakEvent(EquipmentSlotType.MAINHAND);
         });
         BlockPos blockpos = WyHelper.rayTraceBlockSafe(entity, 20.0F);
         AbilityDamageSource source = (AbilityDamageSource)((ModDamageSource)this.dealDamageComponent.getDamageSource(entity)).setSlash();
         Vector3d startPos = entity.position();
         float actualTeleportDistance = 20.0F;

         for(double f = 0.0; f < 1.0; f += 0.13) {
            double x = MathHelper.lerp(f, startPos.x(), (double)blockpos.getX());
            double y = MathHelper.lerp(f, startPos.y(), (double)blockpos.getY());
            double z = MathHelper.lerp(f, startPos.z(), (double)blockpos.getZ());
            Vector3d pos = new Vector3d(x, y, z);
            List projectiles = WyHelper.getNearbyEntities(pos, entity.level, (double)entity.getBbWidth(), (double)entity.getBbHeight(), (double)entity.getBbWidth(), (Predicate)null, new Class[]{ProjectileEntity.class});
            if (!projectiles.isEmpty()) {
               projectiles.sort(TargetHelper.closestComparator(startPos));
               actualTeleportDistance = MathHelper.sqrt(((ProjectileEntity)projectiles.get(0)).distanceToSqr(startPos));
               break;
            }
         }

         blockpos = WyHelper.rayTraceBlockSafe(entity, actualTeleportDistance);
         List targets = this.rangeComponent.getTargetsInLine(entity, actualTeleportDistance, 2.5F);
         Iterator iterator = targets.iterator();

         while(iterator.hasNext()) {
            LivingEntity target = (LivingEntity)iterator.next();
            if (this.hitTrackerComponent.canHit(target)) {
               boolean flag = this.dealDamageComponent.hurtTarget(entity, target, 40.0F, source);
               if (flag && !entity.level.isClientSide) {
                  WyHelper.spawnParticles(ParticleTypes.SWEEP_ATTACK, (ServerWorld)entity.level, target.getX(), target.getY() + (double)target.getEyeHeight(), target.getZ());
                  target.stopRiding();
                  target.teleportToWithTicket((double)blockpos.getX(), (double)blockpos.getY(), (double)blockpos.getZ());
               }
            }
         }

         entity.stopRiding();
         entity.teleportToWithTicket((double)blockpos.getX(), (double)blockpos.getY(), (double)blockpos.getZ());
         if (!entity.level.isClientSide) {
            ((ServerWorld)entity.level).getChunkSource().broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
         }

         entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.DASH_ABILITY_SWOOSH_SFX.get(), SoundCategory.PLAYERS, 2.0F, 0.7F);
         this.projectileComponent.shoot(entity, 5.0F, 0.0F);
      }

      this.animationComponent.stop(entity);
      this.cooldownComponent.startCooldown(entity, 900.0F);
   }

   private ShinsokuHakujakuProjectile createProjectile(LivingEntity entity) {
      ShinsokuHakujakuProjectile proj = new ShinsokuHakujakuProjectile(entity.level, entity);
      return proj;
   }

   private static boolean canUnlock(LivingEntity entity) {
      if (!(entity instanceof PlayerEntity)) {
         return false;
      } else {
         PlayerEntity player = (PlayerEntity)entity;
         IEntityStats props = EntityStatsCapability.get(player);
         IQuestData questProps = QuestDataCapability.get(player);
         return props.getFightingStyle().equals(CartValues.BLUDGEON) && questProps.hasFinishedQuest(CartQuests.BLUDGEON_TRIAL_08);
      }
   }

   static {
      INSTANCE = (new AbilityCore.Builder("Shinsoku Hakujaku", AbilityCategory.STYLE, ShinsokuHakujakuRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, DealDamageComponent.getTooltip(40.0F), ChargeComponent.getTooltip(55.0F), CooldownComponent.getTooltip(900.0F), RangeComponent.getTooltip(20.0F, RangeType.LINE)}).addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips()).setSourceElement(SourceElement.SHOCKWAVE).setSourceType(new SourceType[]{SourceType.BLUNT}).setSourceHakiNature(SourceHakiNature.IMBUING).setUnlockCheck(ShinsokuHakujakuRework::canUnlock).build();
      INSTANCE.setIcon(new net.minecraft.util.ResourceLocation("cartaddon", "textures/abilities/shinsoku_hakujaku.png"));
   }
}



