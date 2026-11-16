package net.kazi.kazimod.ghoul;

import java.util.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.vector.*;
import net.minecraftforge.client.event.RenderWorldLastEvent;

/** Impact packets create bounded client meshes; no per-particle entities or network spam. */
final class GhoulVfx extends RenderState {
    private GhoulVfx(){super(null,null,null);}
    private static final RenderType TYPE=RenderType.create("ghoul_energy",DefaultVertexFormats.POSITION_COLOR,7,16384,false,true,
        RenderType.State.builder().setTextureState(NO_TEXTURE).setAlphaState(NO_ALPHA).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
        .setCullState(NO_CULL).setLightmapState(NO_LIGHTMAP).setDiffuseLightingState(NO_DIFFUSE_LIGHTING).setWriteMaskState(COLOR_WRITE).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(false));
    private static final List<Effect> EFFECTS=new ArrayList<>();private static Object world;
    private static final class Effect { int kind,type;long start;Vector3d at;float size,yaw; }
    static void receive(CompoundNBT n){Minecraft mc=Minecraft.getInstance();if(mc.level!=world){clear();world=mc.level;}if(EFFECTS.size()>=96)EFFECTS.remove(0);Effect e=new Effect();e.kind=n.getInt("Fx");e.type=n.getInt("Type");e.start=mc.level.getGameTime();e.at=new Vector3d(n.getDouble("X"),n.getDouble("Y"),n.getDouble("Z"));e.size=n.getFloat("Size");e.yaw=n.getFloat("Yaw");EFFECTS.add(e);}
    static void clear(){EFFECTS.clear();world=null;}
    private static void point(IVertexBuilder b,Matrix4f m,Vector3d p,int color,float a){b.vertex(m,(float)p.x,(float)p.y,(float)p.z).color((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F,a).endVertex();}
    private static int color(int type){return new int[]{0xff4779,0xc080ee,0xf13e57,0x44dce5}[Math.max(0,Math.min(3,type))];}
    private static void ribbon(IVertexBuilder b,Matrix4f m,Vector3d a,Vector3d c,double width,int color,float alpha){Vector3d side=c.subtract(a).cross(new Vector3d(0,1,0)).normalize().scale(width);if(side.lengthSqr()<.00001)side=new Vector3d(width,0,0);point(b,m,a.add(side),color,alpha);point(b,m,a.subtract(side),color,alpha);point(b,m,c.subtract(side),color,0);point(b,m,c.add(side),color,0);}
    static void crystal(MatrixStack s,IRenderTypeBuffer buffers,float size){
        IVertexBuilder b=buffers.getBuffer(TYPE);Matrix4f m=s.last().pose();Vector3d tip=new Vector3d(0,0,.6*size),rear=new Vector3d(0,0,-.5*size);
        for(int i=0;i<4;i++){double a=i*Math.PI/2,c=(i+1)*Math.PI/2;Vector3d p=new Vector3d(Math.cos(a)*.13*size,Math.sin(a)*.13*size,0),q=new Vector3d(Math.cos(c)*.13*size,Math.sin(c)*.13*size,0);point(b,m,tip,0xffdde9,1);point(b,m,p,0xf13b76,.95F);point(b,m,q,0xbf1c63,.95F);point(b,m,tip,0xffdde9,1);point(b,m,rear,0x8c205d,.7F);point(b,m,q,0xbf1c63,.9F);point(b,m,p,0xf13b76,.9F);point(b,m,rear,0x8c205d,.7F);}
        ribbon(b,m,rear,new Vector3d(0,0,-2*size),.08*size,0xff427c,.5F);
    }
    static void world(RenderWorldLastEvent event){Minecraft mc=Minecraft.getInstance();if(mc.level!=world){clear();world=mc.level;}if(EFFECTS.isEmpty())return;
        Vector3d camera=mc.gameRenderer.getMainCamera().getPosition();MatrixStack s=event.getMatrixStack();IRenderTypeBuffer.Impl buffers=mc.renderBuffers().bufferSource();IVertexBuilder b=buffers.getBuffer(TYPE);
        Iterator<Effect> it=EFFECTS.iterator();while(it.hasNext()){Effect e=it.next();float age=mc.level.getGameTime()-e.start+event.getPartialTicks(),life=e.kind==2?24:12,t=age/life;if(t>=1){it.remove();continue;}if(e.at.distanceToSqr(camera)>96*96)continue;
            s.pushPose();Vector3d pos=e.at.subtract(camera);s.translate(pos.x,pos.y,pos.z);s.mulPose(Vector3f.YP.rotationDegrees(180-e.yaw));Matrix4f m=s.last().pose();float fade=(1-t)*(1-t);int col=color(e.type);
            if(e.kind==0||e.kind==3){for(int i=0;i<8;i++){double a=i*Math.PI/4+.25;Vector3d from=new Vector3d(Math.cos(a)*t*e.size*.7,Math.sin(a)*t*e.size*.7,0),to=from.scale(1.8).add(0,.1,0);ribbon(b,m,from,to,.035*e.size,col,fade*.8F);}}
            if(e.kind==1||e.kind==2){double radius=(.25+t*1.8)*e.size;for(int i=0;i<32;i++){double a=i*Math.PI/16,c=(i+1)*Math.PI/16;point(b,m,new Vector3d(Math.cos(a)*radius,.035,Math.sin(a)*radius),col,fade*.65F);point(b,m,new Vector3d(Math.cos(a)*(radius-.12),.035,Math.sin(a)*(radius-.12)),col,fade*.65F);point(b,m,new Vector3d(Math.cos(c)*(radius-.12),.035,Math.sin(c)*(radius-.12)),col,fade*.65F);point(b,m,new Vector3d(Math.cos(c)*radius,.035,Math.sin(c)*radius),col,fade*.65F);}
                for(int i=0;i<7;i++){double a=i*2.4;Vector3d p=new Vector3d(Math.cos(a)*e.size*t,Math.sin(t*Math.PI)*.4,Math.sin(a)*e.size*t);ribbon(b,m,p,p.add(.14,.22,-.09),.08,0x746576,fade);}
            }
            if(e.kind==4){for(int i=0;i<20;i++){double a=-Math.PI*.7+i*Math.PI/18,c=a+Math.PI/18,r=e.size;point(b,m,new Vector3d(Math.sin(a)*r,.2+Math.cos(a)*.15,-Math.cos(a)*r),col,fade*.55F);point(b,m,new Vector3d(Math.sin(a)*(r-.17),.2,-Math.cos(a)*(r-.17)),col,0);point(b,m,new Vector3d(Math.sin(c)*(r-.17),.2,-Math.cos(c)*(r-.17)),col,0);point(b,m,new Vector3d(Math.sin(c)*r,.2+Math.cos(c)*.15,-Math.cos(c)*r),col,fade*.55F);}}
            s.popPose();
        }buffers.endBatch();
    }
}
