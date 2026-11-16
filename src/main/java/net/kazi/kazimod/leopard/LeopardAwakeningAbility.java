package net.kazi.kazimod.leopard;

import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.TranslationTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.AttributeHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;

public final class LeopardAwakeningAbility extends MorphAbility2 {
    public static final AbilityCore<LeopardAwakeningAbility> INSTANCE = new AbilityCore.Builder<>("Awakened Leopard Form", AbilityCategory.DEVIL_FRUITS, LeopardAwakeningAbility::new)
            .setUnlockCheck(LeopardFeatures::awakened)
            .setIcon(new ResourceLocation("kazimod", "textures/abilities/leopard/awakened_leopard_form.png"))
            .addDescriptionLine(new TranslationTextComponent("ability.kazimod.awakened_leopard_form.description"))
            .addAdvancedDescriptionLine(ContinuousComponent.getTooltip(), xyz.pixelatedw.mineminenomi.api.abilities.components.ChangeStatsComponent.getTooltip()).build();

    public LeopardAwakeningAbility(AbilityCore<LeopardAwakeningAbility> core) {
        super(core);
        addCanUseCheck((user, ability) -> continuousComponent.isContinuous() || LeopardFeatures.awakened(user)
                ? AbilityUseResult.success() : AbilityUseResult.fail(new TranslationTextComponent("message.kazimod.leopard.locked")));
        statsComponent.addAttributeModifier(Attributes.MAX_HEALTH, new AbilityAttributeModifier(AttributeHelper.MORPH_HEALTH_UUID, INSTANCE, "Awakened leopard vitality", 120, Operation.ADDITION));
        statsComponent.addAttributeModifier(Attributes.MOVEMENT_SPEED, new AbilityAttributeModifier(AttributeHelper.MORPH_MOVEMENT_SPEED_UUID, INSTANCE, "Awakened leopard agility", 1, Operation.MULTIPLY_BASE));
        statsComponent.addAttributeModifier(ModAttributes.PUNCH_DAMAGE, new AbilityAttributeModifier(AttributeHelper.MORPH_STRENGTH_UUID, INSTANCE, "Awakened leopard strength", 20, Operation.ADDITION));
        statsComponent.addAttributeModifier(Attributes.ARMOR, new AbilityAttributeModifier(AttributeHelper.MORPH_ARMOR_UUID, INSTANCE, "Awakened leopard armor", 10, Operation.ADDITION));
        statsComponent.addAttributeModifier(Attributes.ATTACK_SPEED, new AbilityAttributeModifier(AttributeHelper.MORPH_ATTACK_SPEED_UUID, INSTANCE, "Awakened leopard attack speed", .5, Operation.ADDITION));
        statsComponent.addAttributeModifier(ModAttributes.JUMP_HEIGHT, new AbilityAttributeModifier(AttributeHelper.MORPH_JUMP_BOOST_UUID, INSTANCE, "Awakened leopard jump", 3, Operation.ADDITION));
        statsComponent.addAttributeModifier(net.minecraftforge.common.ForgeMod.REACH_DISTANCE, new AbilityAttributeModifier(AttributeHelper.MORPH_ATTACK_REACH_UUID, INSTANCE, "Awakened leopard reach", 1.25, Operation.ADDITION));
        statsComponent.addAttributeModifier(ModAttributes.ATTACK_RANGE, new AbilityAttributeModifier(AttributeHelper.MORPH_ATTACK_REACH_UUID, INSTANCE, "Awakened leopard attack range", 1.25, Operation.ADDITION));
        statsComponent.addAttributeModifier(ModAttributes.TOUGHNESS, new AbilityAttributeModifier(AttributeHelper.MORPH_TOUGHNESS_UUID, INSTANCE, "Awakened leopard toughness", 4, Operation.ADDITION));
        statsComponent.addAttributeModifier(ModAttributes.FALL_RESISTANCE, new AbilityAttributeModifier(AttributeHelper.MORPH_FALL_RESISTANCE_UUID, INSTANCE, "Awakened leopard fall resistance", 6.5, Operation.ADDITION));
        statsComponent.addAttributeModifier(Attributes.ATTACK_DAMAGE, new AbilityAttributeModifier(java.util.UUID.fromString("b559914b-4b7f-4b5a-923a-022c606e6d0d"), INSTANCE, "Awakened leopard claws", 6, Operation.ADDITION));
        statsComponent.addAttributeModifier(ModAttributes.STEP_HEIGHT, new AbilityAttributeModifier(AttributeHelper.MORPH_STEP_HEIGHT_UUID, INSTANCE, "Awakened leopard step", .6, Operation.ADDITION));
        continuousComponent.addStartEvent((user, ability) -> {
            if (!user.level.isClientSide) user.level.playSound(null, user.blockPosition(), SoundEvents.POLAR_BEAR_WARNING, SoundCategory.PLAYERS, .8F, .75F);
        }).addTickEvent((user, ability) -> {
            if (!user.level.isClientSide && (!LeopardFeatures.awakened(user) || AbilityHelper.isWeakenedByKairosekiOrWater(user))) continuousComponent.stopContinuity(user);
        });
    }
    @Override public MorphInfo getTransformation() { return LeopardFeatures.MORPH.get(); }
    @Override public float getCooldownTicks() { return 200; }
}
