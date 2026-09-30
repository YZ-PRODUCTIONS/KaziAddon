package net.kazi.kazimod.abilities.SusuRework;

import net.MrMagicalCart.cartaddon.abilities.susu.HijonnaKukuuAbility;
import net.minecraft.util.text.TranslationTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;

public final class HijonnaKukuuRework {
    public static final AbilityCore<HijonnaKukuuAbility> INSTANCE = new AbilityCore.Builder<>("Hijonna Kukuu", AbilityCategory.DEVIL_FRUITS, HijonnaKukuuAbility::new)
            .addDescriptionLine(new TranslationTextComponent("ability.kazimod.hijonna_kukuu.description.0")).build();
    private HijonnaKukuuRework() {}
}
