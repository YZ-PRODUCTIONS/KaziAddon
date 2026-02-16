//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.NitoryuRework;

import java.util.List;
import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.entities.mobs.quests.givers.NitoryuTrainerEntity;
import net.MrMagicalCart.cartaddon.init.CartAbilityPools;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class SaiKuruRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "sai_kuru", new Pair[]{ImmutablePair.of("By spinning, the user creates two small tornadoes, which slashes and weakens nearby opponents.", (Object)null)});
    private static final int HOLD_TIME = 40;
    private static final float COOLDOWN = 300.0F;
    private static final float DAMAGE = 10.0F;
    private static final float RANGE = 9.5F;
    private final Interval damageInterval = new Interval(15);
    public static final AbilityCore<SaiKuruRework> INSTANCE;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this)).addStartEvent(this::startContinuousEvent).addTickEvent(this::tickContinuousEvent).addEndEvent(this::endContinuousEvent);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final PoolComponent poolComponent;
    private int angle;

    public SaiKuruRework(AbilityCore<SaiKuruRework> core) {
        super(core);
        this.poolComponent = new PoolComponent(this, CartAbilityPools.SPIN_LIKE, new AbilityPool2[]{ModAbilityPools.GRAB_ABILITY});
        this.angle = 0;
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.poolComponent, this.continuousComponent, this.dealDamageComponent, this.rangeComponent, this.animationComponent});
        this.addCanUseCheck(AbilityHelper::canUseSwordsmanAbilities);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if ((!(entity instanceof PlayerEntity) || AbilityLimits.canUseNitoryu((PlayerEntity)entity)) && AbilityHelper.canUseSwordsmanAbilities(entity)) {
            this.continuousComponent.triggerContinuity(entity, 40.0F);
        } else {
            entity.sendMessage(new StringTextComponent("You must be holding a sword in both hands to use this move!"), entity.getUUID());
        }
    }

    private void startContinuousEvent(LivingEntity entity, IAbility ability) {
        if (entity instanceof PlayerEntity) {
            ItemStack stack = entity.getMainHandItem();
            stack.hurtAndBreak(1, entity, (user) -> user.broadcastBreakEvent(EquipmentSlotType.MAINHAND));
            stack = entity.getOffhandItem();
            stack.hurtAndBreak(1, entity, (user) -> user.broadcastBreakEvent(EquipmentSlotType.OFFHAND));
        }

        if (entity instanceof NitoryuTrainerEntity) {
            entity.addEffect(new EffectInstance(Effects.REGENERATION, 200, 2));
        }

        this.animationComponent.start(entity, ModAnimations.BODY_ROTATION_WIDE_ARMS);
    }

    private void tickContinuousEvent(LivingEntity entity, IAbility ability) {
        if (entity instanceof PlayerEntity && !AbilityLimits.canUseNitoryu((PlayerEntity)entity) || !AbilityHelper.canUseSwordsmanAbilities(entity)) {
            this.continuousComponent.stopContinuity(entity);
        }

        if (this.damageInterval.canTick()) {
            List<LivingEntity> list = this.rangeComponent.getTargetsInArea(entity, 11F);
            AbilityDamageSource source = (AbilityDamageSource)((ModDamageSource)this.dealDamageComponent.getDamageSource(entity)).setSlash();

            for(LivingEntity target : list) {
                this.dealDamageComponent.hurtTarget(entity, target, 10.0F, source);
                if (!entity.level.isClientSide) {
                    WyHelper.spawnParticles(ParticleTypes.SWEEP_ATTACK, (ServerWorld)entity.level, target.getX(), target.getY() + (double)target.getEyeHeight(), target.getZ());
                }
            }
        }

        entity.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 5, 2, false, false));
        double radius = (double)5.0F;
        this.angle += 10;
        if (this.angle > 360) {
            this.angle = 0;
        }

        float radianAngle = (float)Math.toRadians((double)this.angle);
        double x1 = entity.getX() + radius * Math.cos((double)radianAngle);
        double z1 = entity.getZ() + radius * Math.sin((double)radianAngle);
        double x2 = entity.getX() + radius * Math.cos((double)radianAngle + Math.PI);
        double z2 = entity.getZ() + radius * Math.sin((double)radianAngle + Math.PI);
        WyHelper.spawnParticleEffect((ParticleEffect)ModParticleEffects.O_TATSUMAKI.get(), entity, x1, entity.getY(), z1);
        WyHelper.spawnParticleEffect((ParticleEffect)ModParticleEffects.O_TATSUMAKI.get(), entity, x2, entity.getY(), z2);
        if (this.continuousComponent.getContinueTime() % 5.0F == 0.0F) {
            entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.SPIN.get(), SoundCategory.PLAYERS, 2.0F, 0.75F + entity.getRandom().nextFloat() / 5.0F);
        }

    }

    private void endContinuousEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);
        this.cooldownComponent.startCooldown(entity, 240.0F);
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.getFightingStyle().equals(CartValues.NITORYU) && questProps.hasFinishedQuest(CartQuests.NITORYU_TRIAL_04);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Sai Kuru", AbilityCategory.STYLE, SaiKuruRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, DealDamageComponent.getTooltip(18.0F), ContinuousComponent.getTooltip(40.0F), RangeComponent.getTooltip(8.5F, RangeType.AOE), CooldownComponent.getTooltip(240.0F)}).setSourceHakiNature(SourceHakiNature.IMBUING).setSourceType(new SourceType[]{SourceType.SLASH}).setUnlockCheck(SaiKuruRework::canUnlock).build();
    }
}
