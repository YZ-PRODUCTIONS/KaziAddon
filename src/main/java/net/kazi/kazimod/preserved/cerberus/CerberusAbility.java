package net.kazi.kazimod.preserved.cerberus;

import net.minecraft.util.text.StringTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;

/** Inert save-compatibility entries, replaced by the original Cart abilities on login. */
public final class CerberusAbility extends Ability {
    public enum Move {
        FURY("Hell Hath No Fury"),
        EXECUTION("Threefold Execution"),
        HOWL("Underworld Howl"),
        VIGIL("Hound's Vigil"),
        POUNCE("Gravebound Pounce"),
        TERRITORY("Gates of the Underworld"),
        GATES("Gates of Hell");
        public final String title;
        Move(String title) { this.title=title; }
    }

    public CerberusAbility(AbilityCore<CerberusAbility> core, Move move) {
        super(core);
        this.isNew=true;
        addCanUseCheck((entity,ability)->AbilityUseResult.fail(
                new StringTextComponent("This retired Cerberus move has been restored to the original moveset. Rejoin to update your abilities.")));
    }
}
