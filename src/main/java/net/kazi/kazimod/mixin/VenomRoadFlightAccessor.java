package net.kazi.kazimod.mixin;

import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xyz.pixelatedw.mineminenomi.abilities.PropelledFlightAbility;

@Mixin(value=PropelledFlightAbility.class,remap=false)
public interface VenomRoadFlightAccessor {
    @Accessor("oldMovement") void kazi$resetRoadMovement(Vector3d movement);
}
