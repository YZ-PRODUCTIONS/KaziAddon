package net.kazi.kazimod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xyz.pixelatedw.mineminenomi.api.abilities.MorphAbility2;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChangeStatsComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.MorphComponent;

@Mixin(value = MorphAbility2.class, remap = false)
public interface MorphAbility2Accessor {

    @Accessor("continuousComponent")
    ContinuousComponent kazi$getContinuousComponent();

    @Accessor("statsComponent")
    ChangeStatsComponent kazi$getStatsComponent();

    @Accessor("morphComponent")
    MorphComponent kazi$getMorphComponent();
}
