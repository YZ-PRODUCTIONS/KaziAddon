package net.kazi.kazimod.abilities.SusuRework;

import net.MrMagicalCart.cartaddon.abilities.susu.ShokuroAbility;
import net.minecraft.util.text.TranslationTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;

public final class ShokuroRework {
    public static final AbilityCore<ShokuroAbility> INSTANCE = new AbilityCore.Builder<>("Shokuro", AbilityCategory.DEVIL_FRUITS, ShokuroAbility::new)
            .addDescriptionLine(new TranslationTextComponent("ability.kazimod.shokuro.description.0")).build();
    private ShokuroRework() {}
}
