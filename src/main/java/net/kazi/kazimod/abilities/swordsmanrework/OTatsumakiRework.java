//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.swordsmanrework;

import java.util.List;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
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
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.init.ModQuests;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class OTatsumakiRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "o_tatsumaki", new Pair[]{ImmutablePair.of("By spinning, the user creates a small tornado, which slashes and weakens nearby opponents", (Object)null)});
    private static final int HOLD_TIME = 60;
    private static final float COOLDOWN = 240.0F;
    private static final float DAMAGE = 30.0F;
    private static final float RANGE = 5.5F;
    private final Interval damageInterval = new Interval(15);
    public static final AbilityCore<OTatsumakiRework> INSTANCE;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this)).addStartEvent(100, this::onStartContinuousEvent).addTickEvent(100, this::onTickContinuousEvent).addEndEvent(100, this::onEndContinuousEvent);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final AnimationComponent animationComponent = new AnimationComponent(this);

    public OTatsumakiRework(AbilityCore<OTatsumakiRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.dealDamageComponent, this.rangeComponent, this.animationComponent});
        this.addCanUseCheck(AbilityHelper::canUseSwordsmanAbilities);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity, 60.0F);
    }

    private void onStartContinuousEvent(LivingEntity entity, IAbility ability) {
        ItemStack stack = entity.getMainHandItem();
        stack.hurtAndBreak(1, entity, (user) -> user.broadcastBreakEvent(EquipmentSlotType.MAINHAND));
        this.damageInterval.restartIntervalToZero();
        this.animationComponent.start(entity, ModAnimations.BODY_ROTATION_WIDE_ARMS);
        entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.SPIN.get(), SoundCategory.PLAYERS, 2.0F, 0.75F + entity.getRandom().nextFloat() / 4.0F);
    }

    private void onEndContinuousEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);
        this.cooldownComponent.startCooldown(entity, 240.0F);
    }

    private void onTickContinuousEvent(LivingEntity entity, IAbility ability) {
        if (this.damageInterval.canTick()) {
            List<LivingEntity> list = this.rangeComponent.getTargetsInArea(entity, 5.5F);
            AbilityDamageSource source = (AbilityDamageSource)((ModDamageSource)this.dealDamageComponent.getDamageSource(entity)).setSlash();

            for(LivingEntity target : list) {
                this.dealDamageComponent.hurtTarget(entity, target, 30.0F, source);
                if (!entity.level.isClientSide) {
                    WyHelper.spawnParticles(ParticleTypes.SWEEP_ATTACK, (ServerWorld)entity.level, target.getX(), target.getY() + (double)target.getEyeHeight(), target.getZ());
                }
            }
        }

        entity.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 5, 1, false, false));
        if (!entity.level.isClientSide) {
            WyHelper.spawnParticleEffect((ParticleEffect)ModParticleEffects.O_TATSUMAKI.get(), entity, entity.getX(), entity.getY(), entity.getZ());
            if (this.continuousComponent.getContinueTime() % 5.0F == 0.0F) {
                entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.SPIN.get(), SoundCategory.PLAYERS, 2.0F, 0.75F + entity.getRandom().nextFloat() / 4.0F);
            }
        }

    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.isSwordsman() && questProps.hasFinishedQuest(ModQuests.SWORDSMAN_TRIAL_04);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("O Tatsumaki", AbilityCategory.STYLE, OTatsumakiRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(240.0F), ContinuousComponent.getTooltip(60.0F), DealDamageComponent.getTooltip(30.0F), RangeComponent.getTooltip(5.5F, RangeType.AOE)}).setSourceHakiNature(SourceHakiNature.IMBUING).setSourceType(new SourceType[]{SourceType.SLASH}).setUnlockCheck(OTatsumakiRework::canUnlock).build();
    }
}