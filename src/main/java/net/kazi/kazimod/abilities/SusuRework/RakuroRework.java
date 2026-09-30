package net.kazi.kazimod.abilities.SusuRework;

import net.MrMagicalCart.cartaddon.abilities.susu.RakuroAbility;
import net.minecraft.util.text.TranslationTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;

public final class RakuroRework {
    public static final AbilityCore<RakuroAbility> INSTANCE = new AbilityCore.Builder<>("Rakuro", AbilityCategory.DEVIL_FRUITS, RakuroAbility::new)
            .addDescriptionLine(new TranslationTextComponent("ability.kazimod.rakuro.description.0")).build();
    private RakuroRework() {}
}
