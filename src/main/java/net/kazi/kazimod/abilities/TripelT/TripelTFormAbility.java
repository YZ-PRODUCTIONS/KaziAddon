package net.kazi.kazimod.abilities.TripelT;

import net.kazi.kazimod.init.KaziMorphs;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityAttributeModifier;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.MorphAbility2;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChangeStatsComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.AttributeHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import net.minecraftforge.common.ForgeMod;

public class TripelTFormAbility extends MorphAbility2 {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod",
            "triple_t_form",
            new Pair[]{ImmutablePair.of("Transform into Triple T, automatically equipping the Triple T Bat and gaining heavy-point combat attributes.", null)}
    );

    public static final AbilityCore<TripelTFormAbility> INSTANCE;

    private static final AbilityAttributeModifier SPEED_MODIFIER;
    private static final AbilityAttributeModifier STRENGTH_MODIFIER;
    private static final AbilityAttributeModifier ARMOR_MODIFIER;
    private static final AbilityAttributeModifier REACH_MODIFIER;
    private static final AbilityAttributeModifier STEP_HEIGHT_MODIFIER;
    private static final AbilityAttributeModifier JUMP_BOOST_MODIFIER;
    private static final AbilityAttributeModifier TOUGHNESS_MODIFIER;
    private static final AbilityAttributeModifier FALL_RESISTANCE_MODIFIER;

    public TripelTFormAbility(AbilityCore<TripelTFormAbility> core) {
        super(core);
        this.addCanUseCheck((entity, ability) -> TripelTFormWeapon.canStart(entity, this.continuousComponent.isContinuous()));
        this.continuousComponent.addStartEvent((entity, ability) -> TripelTFormWeapon.grant(entity, false))
                .addEndEvent((entity, ability) -> TripelTFormWeapon.remove(entity, false));
        this.addUseEvent((entity, ability) -> TripelTHelper.playTungSound(entity, 2.0F, 0.9F));
        this.statsComponent.addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_MODIFIER);
        this.statsComponent.addAttributeModifier(ModAttributes.PUNCH_DAMAGE, STRENGTH_MODIFIER);
        this.statsComponent.addAttributeModifier(Attributes.ARMOR, ARMOR_MODIFIER);
        this.statsComponent.addAttributeModifier(ForgeMod.REACH_DISTANCE, REACH_MODIFIER);
        this.statsComponent.addAttributeModifier(ModAttributes.ATTACK_RANGE, REACH_MODIFIER);
        this.statsComponent.addAttributeModifier(ModAttributes.STEP_HEIGHT, STEP_HEIGHT_MODIFIER);
        this.statsComponent.addAttributeModifier(ModAttributes.JUMP_HEIGHT, JUMP_BOOST_MODIFIER);
        this.statsComponent.addAttributeModifier(ModAttributes.TOUGHNESS, TOUGHNESS_MODIFIER);
        this.statsComponent.addAttributeModifier(ModAttributes.FALL_RESISTANCE, FALL_RESISTANCE_MODIFIER);
    }

    @Override
    public MorphInfo getTransformation() {
        return KaziMorphs.TRIPEL_T.get();
    }

    static {
        INSTANCE = new AbilityCore.Builder<>("Tripel T Form", AbilityCategory.DEVIL_FRUITS, TripelTFormAbility::new)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(10.0F),
                        ContinuousComponent.getTooltip(),
                        ChangeStatsComponent.getTooltip())
                .build();

        SPEED_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_MOVEMENT_SPEED_UUID, INSTANCE, "Triple T Speed Modifier", 0.19D, Operation.MULTIPLY_BASE);
        STRENGTH_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_STRENGTH_UUID, INSTANCE, "Triple T Strength Modifier", 9.0D, Operation.ADDITION);
        ARMOR_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_ARMOR_UUID, INSTANCE, "Triple T Armor Modifier", 15.0D, Operation.ADDITION);
        REACH_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_ATTACK_REACH_UUID, INSTANCE, "Triple T Reach Modifier", 0.2D, Operation.ADDITION);
        STEP_HEIGHT_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_STEP_HEIGHT_UUID, INSTANCE, "Triple T Step Height Modifier", 1.0D, Operation.ADDITION);
        JUMP_BOOST_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_JUMP_BOOST_UUID, INSTANCE, "Triple T Jump Modifier", 1.4D, Operation.ADDITION);
        TOUGHNESS_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_TOUGHNESS_UUID, INSTANCE, "Triple T Toughness Modifier", 6.0D, Operation.ADDITION);
        FALL_RESISTANCE_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_FALL_RESISTANCE_UUID, INSTANCE, "Triple T Fall Resistance Modifier", 1.75D, Operation.ADDITION);
    }
}
