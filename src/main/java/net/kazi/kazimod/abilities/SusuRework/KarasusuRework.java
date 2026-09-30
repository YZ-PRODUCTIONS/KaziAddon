package net.kazi.kazimod.abilities.SusuRework;

import net.MrMagicalCart.cartaddon.abilities.susu.KarasusuAbility;
import net.minecraft.util.text.TranslationTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;

public final class KarasusuRework {
    public static final AbilityCore<KarasusuAbility> INSTANCE = new AbilityCore.Builder<>("Karasusu", AbilityCategory.DEVIL_FRUITS, KarasusuAbility::new)
            .addDescriptionLine(new TranslationTextComponent("ability.kazimod.karasusu.description.0")).build();
    private KarasusuRework() {}
}
