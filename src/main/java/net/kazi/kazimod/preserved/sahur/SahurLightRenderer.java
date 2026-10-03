package net.kazi.kazimod.preserved.sahur;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

public final class SahurLightRenderer extends EntityRenderer<SahurLightEntity> {
    private static final class State extends RenderState {
        State(){super(null,null,null);}
        static RenderType type(){return RenderType.create("triple_t_heavenly_light",DefaultVertexFormats.POSITION_COLOR,7,65536,false,true,
                RenderType.State.builder().setTransparencyState(ADDITIVE_TRANSPARENCY).setCullState(NO_CULL)
                        .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));}
    }
    public static final RenderType TYPE=State.type();
    public SahurLightRenderer(EntityRendererManager manager){super(manager);shadowRadius=0;}
    public void render(SahurLightEntity e,float yaw,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light){
        IVertexBuilder b=buffers.getBuffer(TYPE);
        SahurLightMesh.render((x,y,z,c,a)->b.vertex(stack.last().pose(),x,y,z).color(c>>16&255,c>>8&255,c&255,(int)(a*255)).endVertex(),e.kind(),e.age()+partial,e.life());
    }
    public ResourceLocation getTextureLocation(SahurLightEntity e){return new ResourceLocation("kazimod","textures/abilities/divine_resonance.png");}
}
