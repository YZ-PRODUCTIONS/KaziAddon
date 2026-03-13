package net.kazi.kazimod.abilities.DoctorRework;

import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTriggerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTriggerComponent.HitResult;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.init.ModQuests;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class DopingRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "doping", new Pair[]{ImmutablePair.of("Injects the target with special medicine that boosts their physical powers.", (Object)null), ImmutablePair.of("§aSHIFT-USE§r: User injects themselves", (Object)null)});
    private static final int COOLDOWN = 500;
    private static final int EFFECT_TIME = 200;
    public static final AbilityCore<DopingRework> INSTANCE;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this)).addStartEvent(this::startContinuityEvent).addEndEvent(this::endContinuityEvent);
    private final HitTriggerComponent hitTriggerComponent = (new HitTriggerComponent(this)).addOnHitEvent(this::hitEvent).addTryHitEvent(this::tryHitEvent);

    public DopingRework(AbilityCore<DopingRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.hitTriggerComponent});
        this.hitTriggerComponent.setBypassSameGroupProtection();
        this.addCanUseCheck(AbilityHelper::requiresMedicBag);
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity);
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        if (entity.isCrouching()) {
            this.applyEffect(entity, entity);
        }
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.cooldownComponent.startCooldown(entity, 900.0F);
    }

    private HitTriggerComponent.HitResult tryHitEvent(LivingEntity entity, LivingEntity target, ModDamageSource source, IAbility ability) {
        return !this.continuousComponent.isContinuous() ? HitResult.PASS : HitResult.HIT;
    }

    private boolean hitEvent(LivingEntity entity, LivingEntity target, ModDamageSource source, IAbility ability) {
        source.setBypassFriendlyDamage();
        this.applyEffect(entity, target);
        return true;
    }

    private void applyEffect(LivingEntity entity, LivingEntity target) {
        target.addEffect(new EffectInstance((Effect)ModEffects.TENSION_HORMONE.get(), 600, 1));
        target.addEffect(new EffectInstance((Effect) KaziEffects.ENHANCED_MOVEMENT.get(), 600, 1));
        WyHelper.spawnParticleEffect((ParticleEffect)ModParticleEffects.FIRST_AID.get(), entity, target.getX(), target.getY(), target.getZ());
        this.continuousComponent.stopContinuity(entity);
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.isDoctor() && questProps.hasFinishedQuest(ModQuests.DOCTOR_TRIAL_03);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Doping", AbilityCategory.STYLE, DopingRework::new)).addDescriptionLine(DESCRIPTION).setSourceType(new SourceType[]{SourceType.FIST}).setUnlockCheck(DopingRework::canUnlock).build();
    }
}