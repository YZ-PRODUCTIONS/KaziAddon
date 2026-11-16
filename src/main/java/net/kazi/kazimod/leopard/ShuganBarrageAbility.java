package net.kazi.kazimod.leopard;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.*;

/** Registered only to migrate old saves into Claw Strike's awakened mode. */
public final class ShuganBarrageAbility extends Ability {
    public static final AbilityCore<ShuganBarrageAbility> INSTANCE = new AbilityCore.Builder<>("Shugan Barrage", AbilityCategory.DEVIL_FRUITS, ShuganBarrageAbility::new)
            .setUnlockCheck(user -> false).setIcon(new ResourceLocation("kazimod", "textures/abilities/shugan_barrage.png"))
            .addDescriptionLine(new TranslationTextComponent("ability.kazimod.shugan_barrage.description"))
            .addAdvancedDescriptionLine(ContinuousComponent.getTooltip(40), CooldownComponent.getTooltip(240))
            .setSourceHakiNature(SourceHakiNature.HARDENING).setSourceType(SourceType.FIST).build();
    public ShuganBarrageAbility(AbilityCore<ShuganBarrageAbility> core) {
        super(core); isNew = true;
        new LeopardAltMode(this, true, true);
    }
    public boolean active() { return LeopardAltMode.get(this).active(); }
}
