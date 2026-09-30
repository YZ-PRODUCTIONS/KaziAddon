package net.kazi.kazimod.abilities.SusuRework;

import net.MrMagicalCart.cartaddon.abilities.susu.SusuLogiaAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityType;

public final class SusuLogiaRework {
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static final AbilityCore<SusuLogiaAbility> INSTANCE = new AbilityCore.Builder("Logia Invulnerability Susu", AbilityCategory.DEVIL_FRUITS, AbilityType.PASSIVE, SusuLogiaAbility::new).build();
    private SusuLogiaRework() {}
}
