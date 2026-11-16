package net.kazi.kazimod.leopard;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.*;

/** Registered only to migrate old saves into Ferocious Leap's awakened mode. */
public final class PredatorHuntAbility extends Ability {
    public static final AbilityCore<PredatorHuntAbility> INSTANCE = new AbilityCore.Builder<>("Predator Hunt", AbilityCategory.DEVIL_FRUITS, PredatorHuntAbility::new)
            .setUnlockCheck(user -> false).setIcon(new ResourceLocation("mineminenomi", "textures/abilities/ferocious_leap.png"))
            .addDescriptionLine(new TranslationTextComponent("ability.kazimod.predator_hunt.description"))
            .addAdvancedDescriptionLine(CooldownComponent.getTooltip(280))
            .setSourceHakiNature(SourceHakiNature.HARDENING).setSourceType(SourceType.FIST).build();
    public PredatorHuntAbility(AbilityCore<PredatorHuntAbility> core) {
        super(core); isNew = true;
        new LeopardAltMode(this, false, true);
    }
    public boolean active() { return LeopardAltMode.get(this).active(); }
    public boolean pinning() { return LeopardAltMode.get(this).pinning(); }
    public float strikeTime() { return LeopardAltMode.get(this).strikeTime(); }
}
