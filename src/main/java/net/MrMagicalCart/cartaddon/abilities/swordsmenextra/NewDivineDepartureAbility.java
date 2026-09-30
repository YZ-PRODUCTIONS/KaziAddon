package net.MrMagicalCart.cartaddon.abilities.swordsmenextra;

import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;

/**
 * Compatibility alias for addons compiled against the short-lived package used
 * by newer Cart builds. CartAddon 0.7.5 keeps Divine Departure under saber.
 */
@Deprecated
public final class NewDivineDepartureAbility {

    public static final AbilityCore<net.MrMagicalCart.cartaddon.abilities.saber.NewDivineDepartureAbility> INSTANCE =
            net.MrMagicalCart.cartaddon.abilities.saber.NewDivineDepartureAbility.INSTANCE;

    private NewDivineDepartureAbility() {
    }
}
