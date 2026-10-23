package net.kazi.kazimod.mixin.balance.client;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.kazi.kazimod.entities.WhiteTornadoEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class TenkiTornadoBoundsMixin {
    @Inject(method="getBoundingBoxForCulling",at=@At("HEAD"),cancellable=true)
    private void kazimod$fullFunnelBounds(CallbackInfoReturnable<AxisAlignedBB> cir){
        if((Object)this instanceof WhiteTornadoEntity){
            WhiteTornadoEntity e=(WhiteTornadoEntity)(Object)this;double r=e.getSize()*net.kazi.kazimod.preserved.tenki.TenkiMesh.TORNADO_VISUAL_SCALE;
            cir.setReturnValue(new AxisAlignedBB(e.getX()-r,e.getY()-1,e.getZ()-r,e.getX()+r,e.getY()+r*3+2,e.getZ()+r));
        }
    }
}
