package net.kazi.kazimod.abilities.DoctorRework;

import java.util.function.Predicate;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.PunchAbility2;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HealComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTriggerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTriggerComponent.HitResult;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.init.ModQuests;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class FirstAidRework extends PunchAbility2 {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "first_aid", new Pair[]{ImmutablePair.of("Touch the target to heal them for 15% of their max health.", (Object)null)});
    private static final float COOLDOWN = 300.0F;
    private static final float HEAL_PERCENT = 0.10F; // 15% of target's max health
    public static final AbilityCore<FirstAidRework> INSTANCE;
    private final HealComponent healComponent = new HealComponent(this);

    public FirstAidRework(AbilityCore<FirstAidRework> core) {
        super(core);
        this.addComponents(new AbilityComponent[]{this.healComponent});
        this.hitTriggerComponent.setBypassSameGroupProtection();
        this.hitTriggerComponent.addTryHitEvent(80, this::hitEvent);
    }

    private HitTriggerComponent.HitResult hitEvent(LivingEntity entity, LivingEntity target, ModDamageSource source, IAbility ability) {
        if (this.canActivate().test(entity)) {
            float healAmount = target.getMaxHealth() * HEAL_PERCENT;
            this.healComponent.healTarget(entity, target, healAmount);
            target.addEffect(new EffectInstance(Effects.REGENERATION, 400, 1));
            WyHelper.spawnParticleEffect((ParticleEffect)ModParticleEffects.FIRST_AID.get(), entity, target.getX(), target.getY(), target.getZ());
            this.increaseUses();
            return HitResult.FAIL;
        } else {
            return HitResult.PASS;
        }
    }

    public boolean onHitEffect(LivingEntity entity, LivingEntity target, ModDamageSource source) {
        return true;
    }

    public Predicate<LivingEntity> canActivate() {
        return (entity) -> this.continuousComponent.isContinuous();
    }

    public float getPunchCooldown() {
        return COOLDOWN;
    }

    public int getUseLimit() {
        return 1;
    }

    public boolean isParallel() {
        return true;
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.isDoctor() && questProps.hasFinishedQuest(ModQuests.DOCTOR_TRIAL_01);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("First Aid", AbilityCategory.STYLE, FirstAidRework::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN)
                })
                .setSourceType(new SourceType[]{SourceType.FIST})
                .setUnlockCheck(FirstAidRework::canUnlock)
                .build();
    }
}