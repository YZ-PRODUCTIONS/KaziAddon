package net.kazi.kazimod.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import xyz.pixelatedw.mineminenomi.abilities.zoumammoth.MammothHeavyPointAbility;

@Mixin(value=MammothHeavyPointAbility.class,remap=false)
public abstract class MammothHybridStatsMixin {
    // The first 15.0 is health; the following occurrence is the existing armor bonus.
    @ModifyConstant(method="<clinit>",constant=@Constant(doubleValue=15.0,ordinal=0))
    private static double kazimod$health(double original){return 30;}
}
