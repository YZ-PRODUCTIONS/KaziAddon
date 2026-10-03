package net.kazi.kazimod.preserved.tenki;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.*;

public final class TenkiRenderer extends EntityRenderer<TenkiVisualEntity> {
    private static class State extends RenderState {
        State(){super(null,null,null);}
        static RenderType type(){return RenderType.create("tenki_weather",DefaultVertexFormats.POSITION_COLOR,7,262144,false,true,RenderType.State.builder().setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));}
        static RenderType cloud(){return RenderType.create("tenki_cloud",DefaultVertexFormats.POSITION_COLOR,7,65536,false,false,RenderType.State.builder().setTransparencyState(NO_TRANSPARENCY).setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_DEPTH_WRITE).createCompositeState(false));}
    }
    public static final RenderType TYPE=State.type();
    private static final RenderType CLOUD=State.cloud();
    public TenkiRenderer(EntityRendererManager manager){super(manager);shadowRadius=0;}
    public static TenkiMesh.Sink sink(MatrixStack stack,IRenderTypeBuffer buffers){IVertexBuilder b=buffers.getBuffer(TYPE);return (x,y,z,c,a)->b.vertex(stack.last().pose(),x,y,z).color(c>>16&255,c>>8&255,c&255,(int)(a*255)).endVertex();}
    public void render(TenkiVisualEntity e,float yaw,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light){
        if(e.isInvisible())return;
        float age=e.tickCount+partial;stack.pushPose();
        if(e.kind()<=2){
            IVertexBuilder cloud=buffers.getBuffer(CLOUD);
            TenkiMesh.cloud((x,y,z,c,a)->cloud.vertex(stack.last().pose(),x,y,z).color(c>>16&255,c>>8&255,c&255,255).endVertex(),age,e.radius(),e.kind());
            TenkiMesh.Sink s=sink(stack,buffers);TenkiMesh.cloudLightning(s,age,e.radius(),e.kind());
            Vector3d camera=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(e.position());
            if(camera.lengthSqr()<160*160)TenkiMesh.weather(s,age,e.kind()==1?83:e.radius(),(float)camera.x,(float)camera.y,(float)camera.z,e.kind()==1);
        } else {
            Entity source=e.source();Vector3d d=source==null?Vector3d.ZERO:source.getDeltaMovement().normalize();
            stack.mulPose(Vector3f.YP.rotation((float)Math.atan2(-d.x,-d.z)));stack.mulPose(Vector3f.XP.rotation((float)Math.asin(Math.max(-1,Math.min(1,d.y)))));
            if(e.kind()==3)TenkiMesh.gust(sink(stack,buffers),age);else TenkiMesh.lightning(sink(stack,buffers),age,0,0,-4,0,0,4);
        }
        stack.popPose();
    }
    public ResourceLocation getTextureLocation(TenkiVisualEntity e){return new ResourceLocation("minecraft","textures/misc/white.png");}
}
