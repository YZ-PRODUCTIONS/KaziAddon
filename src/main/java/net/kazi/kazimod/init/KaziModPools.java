package net.kazi.kazimod.init;

import xyz.pixelatedw.mineminenomi.abilities.haki.HaoshokuHakiInfusionAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityPool2;

public class KaziModPools {

    // Shared pool — only one of these two abilities can be active at a time
    public static final AbilityPool2 HAKI_INFUSION = new AbilityPool2();

    public static void init() {
        HAKI_INFUSION.addAbilityCore(HaoshokuHakiInfusionAbility.INSTANCE);
    }
}
