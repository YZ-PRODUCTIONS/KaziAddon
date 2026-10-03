package net.kazi.kazimod.worldturtle;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.*;

public final class SupernovaRenderer extends EntityRenderer<SupernovaEntity> {
    private static final class State extends RenderState {
        State(){super(null,null,null);}
        static RenderType type(){return RenderType.create("world_turtle_supernova",DefaultVertexFormats.POSITION_COLOR,7,65536,false,true,
                RenderType.State.builder().setTransparencyState(ADDITIVE_TRANSPARENCY).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(false));}
    }
    private static final RenderType LIGHT=State.type();
    private final Vector4f vertex=new Vector4f(0,0,0,1);
    public SupernovaRenderer(EntityRendererManager manager){super(manager);shadowRadius=0;}
    public void render(SupernovaEntity e,float yaw,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light){
        IVertexBuilder buffer=buffers.getBuffer(LIGHT);float age=e.age()+partial;
        WorldTurtleMesh.Sink sink=(x,y,z,c,a)->{
            vertex.set(x,y,z,1);vertex.transform(stack.last().pose());
            buffer.vertex(vertex.x(),vertex.y(),vertex.z()).color(c>>16&255,c>>8&255,c&255,(int)(Math.max(0,Math.min(1,a))*255)).endVertex();
        };
        for(int i=0;i<5;i++){
            Vector3d offset=e.star(i).subtract(e.position());stack.pushPose();stack.translate(offset.x,offset.y,offset.z);
            SupernovaMesh.star(sink,age,i);
            if(age>=40){Vector3d d=e.end(i).subtract(e.star(i));float length=(float)d.length();
                if(length>.01){float y=(float)Math.toDegrees(Math.atan2(d.x,d.z)),p=(float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z)));
                    stack.mulPose(Vector3f.YP.rotationDegrees(y));stack.mulPose(Vector3f.XP.rotationDegrees(p));SupernovaMesh.beam(sink,length,age);}}
            stack.popPose();
        }
    }
    public ResourceLocation getTextureLocation(SupernovaEntity e){return new ResourceLocation("kazimod","textures/abilities/supernova.png");}
}
