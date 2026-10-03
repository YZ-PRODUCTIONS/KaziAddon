package net.kazi.kazimod.kake;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;

public final class KakeVfxRenderer extends EntityRenderer<KakeVfxEntity> {
    public KakeVfxRenderer(EntityRendererManager manager){super(manager);shadowRadius=0;}
    public void render(KakeVfxEntity e,float yaw,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light){
        float age=e.age()+partial;if(age>=e.life())return;
        boolean far=entityRenderDispatcher.camera.getPosition().distanceToSqr(e.position())>96*96;
        stack.pushPose();
        if(e.kind()==KakeVfxEntity.SLOT){
            stack.mulPose(Vector3f.YP.rotationDegrees(-e.angle()));
            float scale=Math.min(1,Math.min(age/5,(e.life()-age)/8));stack.scale(scale,scale,scale);
        }else if(e.kind()==KakeVfxEntity.SHOT)stack.mulPose(Vector3f.YP.rotationDegrees(e.angle()));
        for(boolean glow:new boolean[]{false,true}){
            KakeMesh.Sink sink=KakeRender.sink(stack,buffers,glow);
            if(e.kind()==KakeVfxEntity.SLOT)KakeMesh.slot(sink,age,e.revealAge(),e.result(),glow);
            else KakeMesh.effect(sink,e.kind(),e.variant(),age,e.life(),e.radius(),glow,far);
        }
        stack.popPose();
    }
    public ResourceLocation getTextureLocation(KakeVfxEntity e){return new ResourceLocation("kazimod","textures/abilities/slot_spin.png");}
}
