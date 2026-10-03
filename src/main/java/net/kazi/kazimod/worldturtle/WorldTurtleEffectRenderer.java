package net.kazi.kazimod.worldturtle;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

public final class WorldTurtleEffectRenderer extends EntityRenderer<WorldTurtleEffectEntity>{
    private static final class State extends RenderState{
        State(){super(null,null,null);}
        static RenderType type(boolean solid){return RenderType.create(solid?"world_turtle_smoke":"world_turtle_light",DefaultVertexFormats.POSITION_COLOR,7,65536,false,true,
                RenderType.State.builder().setTransparencyState(solid?TRANSLUCENT_TRANSPARENCY:ADDITIVE_TRANSPARENCY)
                        .setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(solid?COLOR_DEPTH_WRITE:COLOR_WRITE).createCompositeState(false));}
    }
    private static final RenderType SOLID=State.type(true),LIGHT=State.type(false);
    public WorldTurtleEffectRenderer(EntityRendererManager manager){super(manager);shadowRadius=0;}
    public void render(WorldTurtleEffectEntity e,float yaw,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light){
        if(e.kind()==1&&e.age()>=220)return;
        float scale=e.kind()==2?1:2;
        stack.pushPose();stack.scale(scale,scale,scale);
        for(boolean solid:new boolean[]{true,false}){IVertexBuilder b=buffers.getBuffer(solid?SOLID:LIGHT);
            WorldTurtleMesh.render((x,y,z,c,a)->b.vertex(stack.last().pose(),x,y,z).color(c>>16&255,c>>8&255,c&255,(int)(Math.max(0,Math.min(1,a))*255)).endVertex(),
                    e.kind(),(e.kind()==0&&e.reflected()?Math.max(70,e.age()):e.age())+partial,(float)(e.sky()-e.getY())/scale,solid);
        }
        stack.popPose();
    }
    public ResourceLocation getTextureLocation(WorldTurtleEffectEntity e){return new ResourceLocation("kazimod","textures/abilities/destruction.png");}
}
