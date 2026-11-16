package net.kazi.kazimod.longbob;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;

/** Small, bounded liquid meshes complement the shared soft-gas volumes. */
public final class LongbobVfx {
    private static final class State extends RenderState {
        State(){super(null,null,null);}
        static RenderType liquid(){return RenderType.create("longbob_liquid",DefaultVertexFormats.POSITION_COLOR,7,16384,false,true,
                RenderType.State.builder().setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL)
                        .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));}
    }
    private static final RenderType LIQUID=State.liquid();
    public static float smooth(float t){t=MathHelper.clamp(t,0,1);return t*t*(3-2*t);}
    public static void orb(MatrixStack s,IRenderTypeBuffer b,float age,float radius){
        sphere(s,b,age,radius,0xb9f68a,.65F);sphere(s,b,age,radius*1.08F,0xe3ffc1,.2F);
        for(int i=0;i<5;i++){double a=i*Math.PI*.4+age*.04;s.pushPose();s.translate(Math.cos(a)*radius,.3*Math.sin(a*2+age*.08),Math.sin(a)*radius);sphere(s,b,age+i*7,radius*.18F,0xd8ffa9,.55F);s.popPose();}
    }
    private static void sphere(MatrixStack s,IRenderTypeBuffer b,float age,float radius,int color,float alpha){
        IVertexBuilder out=b.getBuffer(LIQUID);int rings=8,sides=16;
        for(int y=0;y<rings;y++)for(int x=0;x<sides;x++){
            for(int[] corner:new int[][]{{0,0},{1,0},{1,1},{0,1}}){
                double a=(x+corner[0])*Math.PI*2/sides,p=(y+corner[1])*Math.PI/rings;
                double r=radius*(1+.035*Math.sin(a*3+p*4+age*.11));
                float shade=(float)(.8+.2*Math.sin(p));
                out.vertex(s.last().pose(),(float)(Math.sin(p)*Math.cos(a)*r),(float)(Math.cos(p)*r),(float)(Math.sin(p)*Math.sin(a)*r))
                        .color((int)((color>>16&255)*shade),(int)((color>>8&255)*shade),(int)((color&255)*shade),(int)(alpha*255)).endVertex();
            }
        }
    }
    public static void boss(MatrixStack s,IRenderTypeBuffer b,LongbobAction action,float age,float time,boolean coated){
        if(action==LongbobAction.HYDRA&&age<52||action==LongbobAction.CHLORO&&age<32){
            float size=.18F+.3F*smooth(age/20);s.pushPose();s.translate(0,2.35,-.8);orb(s,b,time,size);s.popPose();
        }
        if(coated){
            for(int side:new int[]{-1,1})for(int i=0;i<3;i++){
                double fall=(time*.025+i*.33)%1;s.pushPose();s.translate(side*.83,1.1-fall*.5,-.23);
                sphere(s,b,time,(float)(.07*(1-fall*.5)),0xc7f890,(float)(.5*(1-fall)));s.popPose();
            }
        }
    }
    private LongbobVfx(){}
}
