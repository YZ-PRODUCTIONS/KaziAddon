package net.kazi.kazimod.abilities.Yagi;

import net.kazi.kazimod.init.KaziMorphs;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityAttributeModifier;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.MorphAbility2;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChangeStatsComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AttributeHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;

/** The normal Zoan toggle owns both the synchronized morph and its temporary attributes. */
public final class MountainGoatFormAbility extends MorphAbility2 {
    public static final float COOLDOWN_TICKS = 200.0F;

    public static final AbilityCore<MountainGoatFormAbility> INSTANCE =
            new AbilityCore.Builder<>("Mountain Goat Transformation", AbilityCategory.DEVIL_FRUITS,
                    MountainGoatFormAbility::new)
                    .setIcon(new ResourceLocation("kazimod", "textures/abilities/mountain_goat_transformation.png"))
                    .addDescriptionLine(new TranslationTextComponent("ability.kazimod.mountain_goat_transformation.description.0"))
                    .addAdvancedDescriptionLine(CooldownComponent.getTooltip(COOLDOWN_TICKS),
                            ContinuousComponent.getTooltip(), ChangeStatsComponent.getTooltip())
                    .build();

    public MountainGoatFormAbility(AbilityCore<MountainGoatFormAbility> core) {
        super(core);
        this.statsComponent.addAttributeModifier(Attributes.MOVEMENT_SPEED,
                new AbilityAttributeModifier(AttributeHelper.MORPH_MOVEMENT_SPEED_UUID, INSTANCE,
                        "Mountain Goat movement speed", 0.20D, Operation.MULTIPLY_BASE));
        this.statsComponent.addAttributeModifier(ModAttributes.STEP_HEIGHT,
                new AbilityAttributeModifier(AttributeHelper.MORPH_STEP_HEIGHT_UUID, INSTANCE,
                        "Mountain Goat step height", 0.5D, Operation.ADDITION));
        this.statsComponent.addAttributeModifier(ModAttributes.JUMP_HEIGHT,
                new AbilityAttributeModifier(AttributeHelper.MORPH_JUMP_BOOST_UUID, INSTANCE,
                        "Mountain Goat jump height", 0.5D, Operation.ADDITION));
        this.statsComponent.addAttributeModifier(ModAttributes.FALL_RESISTANCE,
                new AbilityAttributeModifier(AttributeHelper.MORPH_FALL_RESISTANCE_UUID, INSTANCE,
                        "Mountain Goat fall resistance", 1.0D, Operation.ADDITION));
    }

    @Override
    public MorphInfo getTransformation() {
        return KaziMorphs.MOUNTAIN_GOAT.get();
    }

    @Override
    public float getCooldownTicks() {
        return COOLDOWN_TICKS;
    }
}
