package net.kazi.kazimod.mixin.balance;

import net.kazi.kazimod.entities.WhiteTornadoEntity;
import net.kazi.kazimod.abilities.Tenki.TornadoWrathAbility.TornadoMode;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="net.kazi.kazimod.abilities.Tenki.TornadoWrathAbility",remap=false)
public abstract class TenkiTornadoModeMixin {
    @Shadow private WhiteTornadoEntity ridingTornado;
    @Shadow private TornadoMode currentMode;
    @Inject(method="applyTornadoColor",at=@At("RETURN"))
    private void kazimod$galeTint(CallbackInfo ci){if(ridingTornado!=null&&currentMode==TornadoMode.GALE_WRATH)ridingTornado.setColor(.72F,.92F,.86F);}
}
