//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.DoctorRework;

import com.google.common.collect.Lists;
import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PotionEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModQuests;

public class FailedExperimentRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "failed_experiment", new Pair[]{ImmutablePair.of("Throws a random splash potion with a debuff effect at the enemy.", (Object)null)});
    private static final int COOLDOWN = 240;
    public static final AbilityCore<FailedExperimentRework> INSTANCE;
    private final AltModeComponent<Mode> altModeComponent;
    private ItemStack stack;

    public FailedExperimentRework(AbilityCore<FailedExperimentRework> core) {
        super(core);
        this.altModeComponent = (new AltModeComponent<Mode>(this, Mode.class, Mode.ATTACK_SPEED)).addChangeModeEvent(this::onAltModeChange);        this.stack = new ItemStack(Items.SPLASH_POTION);
        super.isNew = true;
        this.addCanUseCheck(AbilityHelper::requiresMedicBag);
        super.addComponents(new AbilityComponent[]{this.altModeComponent});
        super.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity livingEntity, IAbility ability) {
        if (this.altModeComponent.getCurrentMode() == FailedExperimentRework.Mode.ATTACK_SPEED) {
            this.stack = PotionUtils.setCustomEffects(this.stack, Lists.newArrayList(new EffectInstance[]{new EffectInstance(Effects.DIG_SLOWDOWN, 200, 1)}));
        }

        PotionEntity potion = new PotionEntity(livingEntity.level, livingEntity);
        potion.setItem(this.stack);
        potion.xRot -= -20.0F;
        potion.shootFromRotation(livingEntity, livingEntity.xRot, livingEntity.yRot, -20.0F, 1.0F, 0.0F);
        livingEntity.level.addFreshEntity(potion);
        if (livingEntity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)livingEntity;
            ItemStack medicBag = (ItemStack)player.inventory.armor.get(2);
            medicBag.hurtAndBreak(10, player, (user) -> user.broadcastBreakEvent(EquipmentSlotType.MAINHAND));
        }

        super.cooldownComponent.startCooldown(livingEntity, 240.0F);
    }

    private void onAltModeChange(LivingEntity entity, IAbility ability, Mode mode) {
        switch (mode) {
            case POISON:
                this.stack = PotionUtils.setCustomEffects(this.stack, Lists.newArrayList(new EffectInstance[]{new EffectInstance(ModEffects.DOKU_POISON.get(), 100, 0)}));
                break;
            case HUNGER:
                this.stack = PotionUtils.setCustomEffects(this.stack, Lists.newArrayList(new EffectInstance[]{new EffectInstance(Effects.HUNGER, 200, 3)}));
                break;
            case ATTACK_SPEED:
                this.stack = PotionUtils.setCustomEffects(this.stack, Lists.newArrayList(new EffectInstance[]{new EffectInstance(Effects.DIG_SLOWDOWN, 200, 10)}));
                break;
            case BURN:
                this.stack = PotionUtils.setCustomEffects(this.stack, Lists.newArrayList(new EffectInstance[]{new EffectInstance((Effect) KaziEffects.FLAMING_ROT.get(), 100, 0)}));
                break;
            case SLOWNESS:
                this.stack = PotionUtils.setCustomEffects(this.stack, Lists.newArrayList(new EffectInstance[]{new EffectInstance((Effect) KaziEffects.WEAKENED_MOVEMENT.get(), 200, 1)}));
                break;
            case BLINDNESS:
                this.stack = PotionUtils.setCustomEffects(this.stack, Lists.newArrayList(new EffectInstance[]{new EffectInstance(Effects.BLINDNESS, 200, 2)}));
        }

    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.isDoctor() && questProps.hasFinishedQuest(ModQuests.DOCTOR_TRIAL_02);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Failed Experiment", AbilityCategory.STYLE, FailedExperimentRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(240.0F)}).setUnlockCheck(FailedExperimentRework::canUnlock).build();
    }

    public static enum Mode {
        BURN,
        ATTACK_SPEED,
        POISON,
        HUNGER,
        SLOWNESS,
        BLINDNESS;

        private Mode() {
        }
    }
}
