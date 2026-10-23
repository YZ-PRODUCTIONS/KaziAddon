package net.kazi.kazimod.kake;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Vector4f;

public final class KakeRender {
    private static final class State extends RenderState {
        State(){super(null,null,null);}
        static RenderType type(boolean glow){return RenderType.create(glow?"kake_luminous":"kake_models",DefaultVertexFormats.POSITION_COLOR,7,65536,false,glow,
                RenderType.State.builder().setTransparencyState(glow?ADDITIVE_TRANSPARENCY:NO_TRANSPARENCY).setCullState(NO_CULL)
                        .setWriteMaskState(glow?COLOR_WRITE:COLOR_DEPTH_WRITE).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(false));}
    }
    public static final RenderType SOLID=State.type(false),GLOW=State.type(true);
    public static KakeMesh.Sink sink(MatrixStack stack,IRenderTypeBuffer buffers,boolean glow){
        IVertexBuilder buffer=buffers.getBuffer(glow?GLOW:SOLID);Vector4f vertex=new Vector4f(0,0,0,1);
        return(x,y,z,c,a)->{vertex.set((float)x,(float)y,(float)z,1);vertex.transform(stack.last().pose());buffer.vertex(vertex.x(),vertex.y(),vertex.z()).color(c>>16&255,c>>8&255,c&255,(int)(Math.max(0,Math.min(1,a))*255)).endVertex();};
    }
}
