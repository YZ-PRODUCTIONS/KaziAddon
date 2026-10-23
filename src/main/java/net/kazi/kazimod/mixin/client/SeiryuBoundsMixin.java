package net.kazi.kazimod.mixin.client;

import net.kazi.kazimod.seiryu.*;
import net.minecraft.entity.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class SeiryuBoundsMixin {
    @Inject(method="getBoundingBoxForCulling",at=@At("RETURN"),cancellable=true)
    private void kazimod$wholeDragon(CallbackInfoReturnable<AxisAlignedBB> cir) {
        Object entity=this;
        if(!(entity instanceof LivingEntity)||!SeiryuFlight.active((LivingEntity)entity))return;
        Vector3d[] points=SeiryuClient.points((LivingEntity)entity,1);
        AxisAlignedBB bounds=cir.getReturnValue();
        double scale=SeiryuFlight.scale((LivingEntity)entity);
        for(int i=0;i<SeiryuSpine.SEGMENTS;i++)bounds=bounds.minmax(SeiryuSpine.bounds(points,i,scale));
        cir.setReturnValue(bounds);
    }
}
