//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.DoctorRework;

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
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTriggerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTriggerComponent.HitResult;
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

public class AntidoteShotRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "antidote_shot", new Pair[]{ImmutablePair.of("Injects the target with an antidote making them immune to certain negative effects.", (Object)null), ImmutablePair.of("§aSHIFT-USE§r: User injects themselves", (Object)null)});
    private static final int COOLDOWN = 300;
    private static final int EFFECT_TIME = 200;
    public static final AbilityCore<AntidoteShotRework> INSTANCE;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this)).addEndEvent(this::endContinuityEvent);
    private final HitTriggerComponent hitTriggerComponent = (new HitTriggerComponent(this)).addTryHitEvent(this::tryHitEvent).addOnHitEvent(this::onHitEvent);

    public AntidoteShotRework(AbilityCore<AntidoteShotRework> core) {
        super(core);
        super.isNew = true;
        super.addComponents(new AbilityComponent[]{this.continuousComponent, this.hitTriggerComponent});
        this.hitTriggerComponent.setBypassSameGroupProtection();
        this.addCanUseCheck(AbilityHelper::requiresMedicBag);
        super.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.continuousComponent.isContinuous()) {
            this.continuousComponent.stopContinuity(entity);
        } else {
            this.continuousComponent.triggerContinuity(entity);
            if (entity.isCrouching()) {
                this.applyAntidoteEffect(entity, entity);
            }
        }
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        super.cooldownComponent.startCooldown(entity, 300.0F);
    }

    private HitTriggerComponent.HitResult tryHitEvent(LivingEntity entity, LivingEntity target, ModDamageSource source, IAbility ability) {
        return this.continuousComponent.isContinuous() ? HitResult.HIT : HitResult.PASS;
    }

    private boolean onHitEvent(LivingEntity entity, LivingEntity target, ModDamageSource source, IAbility ability) {
        this.applyAntidoteEffect(entity, target);
        return true;
    }

    private void applyAntidoteEffect(LivingEntity entity, LivingEntity target) {
        target.addEffect(new EffectInstance((Effect)ModEffects.ANTIDOTE.get(), 200, 0));
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
            return props.isDoctor() && questProps.hasFinishedQuest(ModQuests.DOCTOR_TRIAL_04);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Antidote Shot", AbilityCategory.STYLE, AntidoteShotRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(300.0F)}).setUnlockCheck(AntidoteShotRework::canUnlock).build();
    }
}
