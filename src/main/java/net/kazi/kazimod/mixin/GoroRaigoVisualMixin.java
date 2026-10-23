package net.kazi.kazimod.mixin;

import net.kazi.kazimod.goro.GoroVfxEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.RaigoProjectile;

@Mixin(value=RaigoProjectile.class,remap=false)
public abstract class GoroRaigoVisualMixin {
    @Shadow private boolean dealtAOE;

    @ModifyConstant(method="onTickEvent",constant=@Constant(intValue=25))
    private int kazi$noRaigoParticles(int count) { return 0; }

    // Only the first 10 is the cosmetic bolt loop; the second configures its segments.
    @ModifyConstant(method="onTickEvent",constant=@Constant(intValue=10,ordinal=0))
    private int kazi$noDecorativeEntities(int count) { return 0; }

    @Inject(method="onBlockImpactEvent",at=@At("HEAD"))
    private void kazi$raigoImpact(BlockPos hit,CallbackInfo ci) {
        RaigoProjectile ball=(RaigoProjectile)(Object)this;
        if(!dealtAOE)GoroVfxEntity.spawn(ball.level,ball.position(),GoroVfxEntity.IMPACT,ball.getSize()*.85F,36);
    }
}
