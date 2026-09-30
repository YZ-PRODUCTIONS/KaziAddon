package net.kazi.kazimod.abilities.SupaRework;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import xyz.pixelatedw.mineminenomi.abilities.supa.SparklingDaisyAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.*;

/** Exact Kazi-owned copy of the base Sparkling Daisy implementation. */
public final class SparklingDaisyRework extends SparklingDaisyAbility {
    public static final AbilityCore<SparklingDaisyRework> INSTANCE = new AbilityCore.Builder<SparklingDaisyRework>(
            "Sparkling Daisy", AbilityCategory.DEVIL_FRUITS, SparklingDaisyRework::new)
            .setIcon(new ResourceLocation("mineminenomi", "textures/abilities/sparkling_daisy.png"))
            .addDescriptionLine(new StringTextComponent(
                    "Launches the user forward, slicing anything in their path"))
            .addAdvancedDescriptionLine(
                    AbilityDescriptionLine.NEW_LINE,
                    CooldownComponent.getTooltip(300.0F),
                    RangeComponent.getTooltip(1.6F, RangeComponent.RangeType.AOE),
                    DealDamageComponent.getTooltip(25.0F))
            .setSourceHakiNature(SourceHakiNature.HARDENING)
            .setSourceType(SourceType.FIST)
            .build();

    @SuppressWarnings({"rawtypes", "unchecked"})
    public SparklingDaisyRework(AbilityCore<SparklingDaisyRework> core) {
        super((AbilityCore) core);
    }
}
