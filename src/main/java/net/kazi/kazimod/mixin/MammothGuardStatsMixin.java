package net.kazi.kazimod.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import xyz.pixelatedw.mineminenomi.abilities.zoumammoth.MammothGuardPointAbility;

@Mixin(value=MammothGuardPointAbility.class,remap=false)
public abstract class MammothGuardStatsMixin {
    @ModifyConstant(method="<clinit>",constant=@Constant(doubleValue=25.0))
    private static double kazimod$health(double original){return 75;}
}
