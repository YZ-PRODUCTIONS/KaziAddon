//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.PikaRework;

import java.util.List;
import java.util.function.Predicate;
import net.MrMagicalCart.cartaddon.cartapi.CartRegistry;
import net.MrMagicalCart.cartaddon.entities.projectiles.pikaclone.DivineCutProjectile;
import net.MrMagicalCart.cartaddon.init.CartAnimations;
import net.MrMagicalCart.cartaddon.init.CartParticleEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUseResult;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ItemSpawnComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.TargetHelper;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.init.ModWeapons;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class MaxAccelerationRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "max_acceleration", new Pair[]{ImmutablePair.of("The user accelerates upwards, and dashes with immense speed, doing massive damage.", (Object)null), ImmutablePair.of("The user summons light from within themselves, unleashing a devastating cut", (Object)null)});
    private static final float COOLDOWN = 500.0F;
    private static final int CONTINUOUS_TIME = 30;
    private static final int CHARGE_TIME = 15;
    private static final float DAMAGE = 60.0F;
    private static final float RANGE = 2.5F;
    private static final float MAX_TELEPORT_DISTANCE = 90.0F;
    private static final float COOLDOWN2 = 500.0F;
    private static final int CHARGE_TIME2 = 35;
    public static final AbilityCore<MaxAccelerationRework> INSTANCE;
    private final ChargeComponent chargeComponent = (new ChargeComponent(this)).addStartEvent(this::startChargeEvent).addTickEvent(this::duringChargeEvent).addEndEvent(this::endChargeEvent);
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this)).addStartEvent(this::startContinuousEvent).addTickEvent(this::duringContinuousEvent).addEndEvent(this::endContinuityEvent);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);
    private final AltModeComponent<Mode> altModeComponent;
    private static final ResourceLocation DEFAULT_ICON = new ResourceLocation("mineminenomi", "textures/abilities/max_acceleration.png");
    private static final ResourceLocation ALT_ICON = new ResourceLocation("mineminenomi", "textures/abilities/divine_cut.png");
    private static final TranslationTextComponent DEFAULT_NAME = new TranslationTextComponent(CartRegistry.registerName("ability.mineminenomi.max_acceleration", "Max Acceleration"));
    private static final TranslationTextComponent ALT_NAME = new TranslationTextComponent(CartRegistry.registerName("ability.mineminenomi.divine_cut", "Divine Cut"));
    private final ProjectileComponent projectileComponent;
    private final ItemSpawnComponent itemSpawnComponent;

    public MaxAccelerationRework(AbilityCore<MaxAccelerationRework> core) {
        super(core);
        this.altModeComponent = (new AltModeComponent(this, Mode.class, MaxAccelerationRework.Mode.MAX_ACCELERATION)).addChangeModeEvent(this::onAltModeChange);
        this.projectileComponent = new ProjectileComponent(this, this::createProjectile);
        this.itemSpawnComponent = new ItemSpawnComponent(this);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.itemSpawnComponent, this.altModeComponent, this.projectileComponent, this.chargeComponent, this.continuousComponent, this.dealDamageComponent, this.rangeComponent, this.animationComponent, this.hitTrackerComponent});
        this.addCanUseCheck(this::canUse);
        this.addUseEvent(this::onUseEvent);
    }

    public AbilityUseResult canUse(LivingEntity entity, IAbility ability) {
        return this.altModeComponent.getCurrentMode() == MaxAccelerationRework.Mode.MAX_ACCELERATION ? AbilityHelper.canUseMomentumAbilities(entity, this) : AbilityHelper.requiresOnGround(entity, this);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.altModeComponent.getCurrentMode() == MaxAccelerationRework.Mode.MAX_ACCELERATION) {
            if (!this.continuousComponent.isContinuous() && !this.chargeComponent.isCharging()) {
                this.continuousComponent.startContinuity(entity, 30.0F);
            }
        } else {
            if (!entity.getMainHandItem().getItem().equals(ModWeapons.AMA_NO_MURAKUMO.get()) && !entity.getMainHandItem().isEmpty()) {
                entity.sendMessage(new StringTextComponent("You need Ama no Murakumo in your hand to use this ability!"), entity.getUUID());
                return;
            }

            if (!this.chargeComponent.isCharging()) {
                this.chargeComponent.startCharging(entity, 35.0F);
            }
        }

    }

    private void startContinuousEvent(LivingEntity entity, IAbility ability) {
        if (this.altModeComponent.getCurrentMode() == MaxAccelerationRework.Mode.MAX_ACCELERATION) {
            entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.PIKA_CHARGE_SFX.get(), SoundCategory.PLAYERS, 50.0F, 1.0F);
            entity.addEffect(new EffectInstance((Effect)ModEffects.DIZZY.get(), 30, 0));
            Vector3d speed = WyHelper.propulsion(entity, (double)1.0F, (double)1.0F);
            AbilityHelper.setDeltaMovement(entity, speed.x, (double)4.0F, speed.z);
        }

    }

    private void duringContinuousEvent(LivingEntity entity, IAbility ability) {
        if (this.altModeComponent.getCurrentMode() == MaxAccelerationRework.Mode.MAX_ACCELERATION) {
            ExplosionAbility explosion = AbilityHelper.newExplosion(entity, entity.level, entity.getX(), entity.getY(), entity.getZ(), 3.0F);
            explosion.setExplosionSound(false);
            explosion.setDamageOwner(false);
            explosion.setDestroyBlocks(true);
            explosion.setDamageEntities(false);
            explosion.doExplosion();
            entity.addEffect(new EffectInstance((Effect)ModEffects.VANISH.get(), 5, 0, false, false));
            RayTraceResult mop = WyHelper.rayTraceBlocksAndEntities(entity, (double)-0.5F);
            double i = mop.getLocation().x;
            double j = mop.getLocation().y;
            double k = mop.getLocation().z;
            if (this.continuousComponent.getContinueTime() % 5.0F == 0.0F) {
                WyHelper.spawnParticleEffect((ParticleEffect)CartParticleEffects.JEWEL.get(), entity, i, j + (double)2.0F, k);
            }
        }

    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        if (this.altModeComponent.getCurrentMode() == MaxAccelerationRework.Mode.MAX_ACCELERATION) {
            this.chargeComponent.startCharging(entity, 15.0F);
        }

    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();
        this.animationComponent.start(entity, CartAnimations.DIVINE_CUT);
        if (this.altModeComponent.getCurrentMode() == MaxAccelerationRework.Mode.DIVINE_CUT) {
            if (entity.getMainHandItem().isEmpty()) {
                this.itemSpawnComponent.spawnItem(entity, new ItemStack((IItemProvider)ModWeapons.AMA_NO_MURAKUMO.get()));
            }

            entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.PIKA_SFX.get(), SoundCategory.PLAYERS, 5.0F, 0.85F + entity.getRandom().nextFloat() / 8.0F);
        }

    }

    private void duringChargeEvent(LivingEntity entity, IAbility ability) {
        if (this.altModeComponent.getCurrentMode() == MaxAccelerationRework.Mode.MAX_ACCELERATION) {
            entity.addEffect(new EffectInstance((Effect)ModEffects.VANISH.get(), 5, 0, false, false));
            RayTraceResult mop = WyHelper.rayTraceBlocksAndEntities(entity, (double)-0.5F);
            double i = mop.getLocation().x;
            double j = mop.getLocation().y;
            double k = mop.getLocation().z;
            WyHelper.spawnParticleEffect((ParticleEffect)CartParticleEffects.JEWEL.get(), entity, i, j, k);
            ExplosionAbility explosion = AbilityHelper.newExplosion(entity, entity.level, entity.getX(), entity.getY(), entity.getZ(), 3.0F);
            explosion.setExplosionSound(false);
            explosion.setDamageOwner(false);
            explosion.setDestroyBlocks(true);
            explosion.setDamageEntities(false);
            explosion.doExplosion();
        } else {
            AbilityHelper.setDeltaMovement(entity, (double)0.0F, (double)0.0F, (double)0.0F);
            if (this.chargeComponent.getChargeTime() % 10.0F == 0.0F) {
                entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.PIKA_SFX.get(), SoundCategory.PLAYERS, 5.0F, 0.85F + entity.getRandom().nextFloat() / 8.0F);
            }

            WyHelper.spawnParticleEffect((ParticleEffect)ModParticleEffects.PIKA_CHARGING.get(), entity, entity.getX(), entity.getY(), entity.getZ());
            WyHelper.spawnParticleEffect((ParticleEffect)CartParticleEffects.DIVINE_CUT.get(), entity, entity.getX(), entity.getY(), entity.getZ());
        }

    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        if (this.altModeComponent.getCurrentMode() == MaxAccelerationRework.Mode.MAX_ACCELERATION) {
            BlockPos blockpos = WyHelper.rayTraceBlockSafe(entity, 90.0F);
            AbilityDamageSource source = (AbilityDamageSource)((ModDamageSource)this.dealDamageComponent.getDamageSource(entity));
            Vector3d startPos = entity.position();
            float actualTeleportDistance = 85.0F;

            for(double f = (double)0.0F; f < (double)1.0F; f += 0.13) {
                double x = MathHelper.lerp(f, startPos.x(), (double)blockpos.getX());
                double y = MathHelper.lerp(f, startPos.y(), (double)blockpos.getY());
                double z = MathHelper.lerp(f, startPos.z(), (double)blockpos.getZ());
                Vector3d pos = new Vector3d(x, y, z);
                List<ProjectileEntity> projectiles = WyHelper.getNearbyEntities(pos, entity.level, (double)entity.getBbWidth(), (double)entity.getBbHeight(), (double)entity.getBbWidth(), (Predicate)null, new Class[]{ProjectileEntity.class});
                if (!projectiles.isEmpty()) {
                    projectiles.sort(TargetHelper.closestComparator(startPos));
                    actualTeleportDistance = MathHelper.sqrt(((ProjectileEntity)projectiles.get(0)).distanceToSqr(startPos));
                    break;
                }
            }

            blockpos = WyHelper.rayTraceBlockSafe(entity, actualTeleportDistance);

            for(LivingEntity target : this.rangeComponent.getTargetsInLine(entity, actualTeleportDistance, 5.5F)) {
                if (this.hitTrackerComponent.canHit(target)) {
                    boolean flag = this.dealDamageComponent.hurtTarget(entity, target, 60.0F, source);
                    if (flag && !entity.level.isClientSide) {
                        WyHelper.spawnParticleEffect((ParticleEffect)ModParticleEffects.FLASH.get(), target, target.getX(), target.getY() + (double)target.getEyeHeight(), target.getZ());
                    }
                }
            }

            entity.stopRiding();
            entity.teleportToWithTicket((double)blockpos.getX(), (double)blockpos.getY(), (double)blockpos.getZ());
            if (!entity.level.isClientSide) {
                ((ServerWorld)entity.level).getChunkSource().broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
            }

            entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.PIKA_SFX.get(), SoundCategory.PLAYERS, 2.0F, 1.0F);
            this.cooldownComponent.startCooldown(entity, 500.0F);
        } else {
            entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.PIKA_SFX.get(), SoundCategory.PLAYERS, 5.0F, 0.5F);
            this.itemSpawnComponent.despawnItems(entity);
            this.projectileComponent.shoot(entity, 3.0F, 1.0F);
            if (!entity.level.isClientSide) {
                ((ServerWorld)entity.level).getChunkSource().broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
            }

            this.cooldownComponent.startCooldown(entity, 500.0F);
        }

        this.animationComponent.stop(entity);
    }

    private DivineCutProjectile createProjectile(LivingEntity entity) {
        DivineCutProjectile proj = new DivineCutProjectile(entity.level, entity);
        return proj;
    }

    private void onAltModeChange(LivingEntity entity, IAbility ability, Enum<?> mode) {
        if (mode == MaxAccelerationRework.Mode.MAX_ACCELERATION) {
            super.setDisplayName(DEFAULT_NAME);
            super.setDisplayIcon(DEFAULT_ICON);
        } else if (mode == MaxAccelerationRework.Mode.DIVINE_CUT) {
            super.setDisplayName(ALT_NAME);
            super.setDisplayIcon(ALT_ICON);
        }

    }

    static {
        INSTANCE = (new AbilityCore.Builder("Max Acceleration", AbilityCategory.DEVIL_FRUITS, MaxAccelerationRework::new)).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{(e, a) -> DEFAULT_NAME.copy().setStyle(Style.EMPTY.withColor(TextFormatting.GREEN)), (e, a) -> DESCRIPTION[0], DealDamageComponent.getTooltip(60.0F), ChargeComponent.getTooltip(15.0F), ContinuousComponent.getTooltip(30.0F), CooldownComponent.getTooltip(500.0F), RangeComponent.getTooltip(90.0F, RangeType.LINE)}).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, (e, a) -> ALT_NAME.copy().setStyle(Style.EMPTY.withColor(TextFormatting.GREEN)), (e, a) -> DESCRIPTION[1], ChargeComponent.getTooltip(35.0F), CooldownComponent.getTooltip(500.0F)}).addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips()).setSourceHakiNature(SourceHakiNature.SPECIAL).setSourceType(new SourceType[]{SourceType.INDIRECT, SourceType.PROJECTILE}).build();
    }

    public static enum Mode {
        MAX_ACCELERATION,
        DIVINE_CUT;

        private Mode() {
        }
    }
}
