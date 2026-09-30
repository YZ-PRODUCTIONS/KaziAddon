package net.kazi.kazimod.abilities.SusuRework;

import net.MrMagicalCart.cartaddon.abilities.susu.SusuImmunityAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityType;

public final class SusuImmunityRework {
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static final AbilityCore<SusuImmunityAbility> INSTANCE = new AbilityCore.Builder("Susu Immunities", AbilityCategory.DEVIL_FRUITS, AbilityType.PASSIVE, SusuImmunityAbility::new).build();
    private SusuImmunityRework() {}
}
