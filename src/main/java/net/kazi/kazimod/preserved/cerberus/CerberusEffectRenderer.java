package net.kazi.kazimod.preserved.cerberus;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.*;

public final class CerberusEffectRenderer extends EntityRenderer<CerberusEffectEntity> {
    private static class State extends RenderState {
        State(){super(null,null,null);}
        static RenderType type(){return RenderType.create("cerberus_hellfire",DefaultVertexFormats.POSITION_COLOR,7,32768,false,true,
                RenderType.State.builder().setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL)
                        .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));}
    }
    public static final RenderType TYPE=State.type();
    public CerberusEffectRenderer(EntityRendererManager manager){super(manager);shadowRadius=0;}
    @Override public void render(CerberusEffectEntity entity,float yaw,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light){
        stack.pushPose();
        if(entity.kind()==0){
            Vector3d d=entity.getDeltaMovement().normalize();
            stack.mulPose(Vector3f.YP.rotation((float)Math.atan2(-d.x,-d.z)));
            stack.mulPose(Vector3f.XP.rotation((float)Math.asin(d.y)));
        }
        IVertexBuilder b=buffers.getBuffer(TYPE);
        CerberusVfxMesh.render((x,y,z,c,a)->b.vertex(stack.last().pose(),x,y,z)
                .color((c>>16)&255,(c>>8)&255,c&255,(int)(a*255)).endVertex(),entity.kind(),entity.age()+partial);
        stack.popPose();
    }
    @Override public ResourceLocation getTextureLocation(CerberusEffectEntity e){return CerberusFeatures.icon("gates");}
}
