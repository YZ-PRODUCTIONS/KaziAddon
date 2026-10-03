package net.kazi.kazimod.mixin.balance.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.kazi.kazimod.entities.WhiteTornadoEntity;
import net.kazi.kazimod.preserved.tenki.*;
import xyz.pixelatedw.mineminenomi.entities.TornadoEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="net.kazi.kazimod.renderers.abilities.WhiteTornadoRenderer",remap=false)
public abstract class TenkiTornadoRendererMixin {
    @Inject(method="render",at=@At("HEAD"),cancellable=true)
    private void kazimod$funnel(TornadoEntity e,float yaw,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light,CallbackInfo ci){
        WhiteTornadoEntity tornado=(WhiteTornadoEntity)e;
        int mode=tornado.getRed()<.1F?2:tornado.getGreen()>tornado.getRed()+.1F?1:0;
        TenkiMesh.tornado(TenkiRenderer.sink(stack,buffers),e.tickCount+partial,e.getSize()*TenkiMesh.TORNADO_VISUAL_SCALE,mode);ci.cancel();
    }
}
