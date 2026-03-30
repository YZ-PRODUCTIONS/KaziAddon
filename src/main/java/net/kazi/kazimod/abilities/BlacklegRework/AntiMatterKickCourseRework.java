//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.BlacklegRework;

import java.util.function.Predicate;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.PunchAbility2;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

public class AntiMatterKickCourseRework extends PunchAbility2 {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "antimanner_kick_course", new Pair[]{ImmutablePair.of("Kicks an enemy and launches them vertically", (Object)null)});
    private static final float COOLDOWN = 200.0F;
    public static final AbilityCore<AntiMatterKickCourseRework> INSTANCE;

    public AntiMatterKickCourseRework(AbilityCore<AntiMatterKickCourseRework> core) {
        super(core);
    }

    public float getPunchDamage() {
        return 35.0F;
    }

    public boolean onHitEffect(LivingEntity entity, LivingEntity target, ModDamageSource source) {
        AbilityHelper.setDeltaMovement(target, target.getDeltaMovement().add((double)0.0F, 1.2000000000000002, (double)0.0F));
        target.addEffect(new EffectInstance(Effects.CONFUSION, 50, 0, false, false));
        target.addEffect(new EffectInstance((Effect)ModEffects.DIZZY.get(), 10, 0, false, false));
        return true;
    }

    public Predicate<LivingEntity> canActivate() {
        return (entity) -> this.continuousComponent.isContinuous() && entity.getMainHandItem().isEmpty();
    }

    public int getUseLimit() {
        return 1;
    }

    public float getPunchCooldown() {
        return 220.0F;
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
            return props.isBlackLeg() && questProps.hasFinishedQuest(CartQuests.BLACKLEG_TRIAL_03);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Anti-Manner Kick Course", AbilityCategory.STYLE, AntiMatterKickCourseRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, DealDamageComponent.getTooltip(35.0F), CooldownComponent.getTooltip(220.0F), ContinuousComponent.getTooltip()}).setSourceHakiNature(SourceHakiNature.HARDENING).setSourceType(new SourceType[]{SourceType.FIST}).setUnlockCheck(AntiMatterKickCourseRework::canUnlock).build();
    }
}
