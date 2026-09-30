package net.kazi.kazimod.abilities.SusuRework;

import net.MrMagicalCart.cartaddon.abilities.susu.SusuFlyAbility;
import net.minecraft.util.text.TranslationTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityType;

public final class SusuFlyRework {
    public static final AbilityCore<SusuFlyAbility> INSTANCE = new AbilityCore.Builder<>("Susu Special Fly", AbilityCategory.DEVIL_FRUITS, AbilityType.PASSIVE, SusuFlyAbility::new)
            .addDescriptionLine(new TranslationTextComponent("ability.kazimod.susu_special_fly.description.0")).build();
    private SusuFlyRework() {}
}
