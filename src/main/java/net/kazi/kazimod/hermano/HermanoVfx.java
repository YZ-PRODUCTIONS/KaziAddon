package net.kazi.kazimod.hermano;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.*;

/** Fixed-size mesh effects, no per-frame entities or particle clouds. */
public final class HermanoVfx {
    private static final int RED=0xff3026,ORANGE=0xff9c35,CORE=0xffe8b1;
    private static class State extends RenderState {
        State(){super(null,null,null);}
        static RenderType create(boolean glow){return RenderType.create(glow?"hermano_glow":"hermano_energy",DefaultVertexFormats.POSITION_COLOR,7,32768,false,true,
                RenderType.State.builder().setTextureState(NO_TEXTURE).setAlphaState(NO_ALPHA).setTransparencyState(glow?LIGHTNING_TRANSPARENCY:TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL).setShadeModelState(SMOOTH_SHADE).setLightmapState(NO_LIGHTMAP).setDiffuseLightingState(NO_DIFFUSE_LIGHTING).setWriteMaskState(COLOR_WRITE).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(false));}
    }
    private static final RenderType GLOW=State.create(true),ENERGY=State.create(false);
    public static float smooth(float x){x=Math.max(0,Math.min(1,x));return x*x*(3-2*x);}
    private interface Sink{void vertex(double x,double y,double z,int color,float alpha);}
    private static Sink sink(MatrixStack s,IRenderTypeBuffer b,boolean glow){IVertexBuilder out=b.getBuffer(glow?GLOW:ENERGY);Matrix4f mat=s.last().pose();return(x,y,z,c,a)->out.vertex(mat,(float)x,(float)y,(float)z).color(c>>16&255,c>>8&255,c&255,(int)(255*Math.max(0,Math.min(1,a)))).endVertex();}
    private static void sphere(Sink s,double rx,double ry,double rz,int color,float alpha){
        for(int row=0;row<8;row++)for(int col=0;col<16;col++)for(int k=0;k<4;k++){
            double lat=-Math.PI/2+(row+(k>=2?1:0))*Math.PI/8,lon=(col+(k==1||k==2?1:0))*Math.PI/8;
            s.vertex(Math.cos(lat)*Math.cos(lon)*rx,Math.sin(lat)*ry,Math.cos(lat)*Math.sin(lon)*rz,color,alpha);
        }
    }
    private static void ring(Sink s,double radius,double width,double height,int color,float alpha){
        for(int i=0;i<40;i++)for(int k=0;k<4;k++){double a=(i+(k>=2?1:0))*Math.PI/20,r=radius-(k==0||k==3?width:0);s.vertex(Math.cos(a)*r,height,Math.sin(a)*r,color,alpha);}
    }
    private static void ray(Sink s,Vector3d a,Vector3d b,double width,int color,float alpha){
        Vector3d dir=b.subtract(a).normalize(),u=dir.cross(new Vector3d(0,1,0)).normalize();if(u.lengthSqr()<.1)u=new Vector3d(1,0,0);Vector3d v=dir.cross(u);
        for(int i=0;i<4;i++){double x=i*Math.PI/2,y=(i+1)*Math.PI/2;Vector3d p=u.scale(Math.cos(x)*width).add(v.scale(Math.sin(x)*width)),q=u.scale(Math.cos(y)*width).add(v.scale(Math.sin(y)*width));
            for(Vector3d at:new Vector3d[]{a.add(p),a.add(q),b.add(q.scale(.4)),b.add(p.scale(.4))})s.vertex(at.x,at.y,at.z,color,alpha);}
    }
    public static void orb(MatrixStack s,IRenderTypeBuffer b,float age,float size){
        sphere(sink(s,b,false),size,size,size,RED,.8F);sphere(sink(s,b,true),size*.75,size*.75,size*.75,ORANGE,.9F);sphere(sink(s,b,true),size*.4,size*.4,size*.4,CORE,1);
        for(int i=0;i<3;i++){s.pushPose();s.mulPose(Vector3f.XP.rotationDegrees(45+i*60));s.mulPose(Vector3f.ZP.rotationDegrees(age*(i%2==0?4:-3)+i*57));ring(sink(s,b,true),size*1.14,.06,0,ORANGE,.7F);s.popPose();}
    }
    private static void aura(MatrixStack s,IRenderTypeBuffer b,float age,float strength){
        s.pushPose();s.translate(0,1.6,0);sphere(sink(s,b,false),1.3,2.1,.9,RED,.09F*strength);s.popPose();
        Sink glow=sink(s,b,true);for(int i=0;i<12;i++){
            double a=i*Math.PI/6,r=.85+.12*Math.sin(age*.18+i),y=(age*.065+i*.31)%2.9;
            ray(glow,new Vector3d(Math.cos(a)*r,y,Math.sin(a)*r*.65),new Vector3d(Math.cos(a)*(r*.75),y+.55,Math.sin(a)*r*.4),.035,RED,.55F*strength);
        }
        ring(glow,1.5,.035,.06,ORANGE,.3F*strength);
    }
    public static void boss(MatrixStack s,IRenderTypeBuffer b,HermanoAction action,float age,float time,boolean full){
        if(full||action==HermanoAction.FULL_POWER)aura(s,b,time,action==HermanoAction.FULL_POWER?smooth(age/35):.65F);
        if(action==HermanoAction.IMPACT&&age<43){s.pushPose();s.translate(-.8,2.45,-1.9);orb(s,b,time,.12F+.72F*smooth(age/36));s.popPose();}
        if(action==HermanoAction.COUNTER&&age<23){s.pushPose();s.translate(0,1.8,0);sphere(sink(s,b,false),1.35,1.9,1.1,RED,.14F);s.mulPose(Vector3f.XP.rotationDegrees(90));ring(sink(s,b,true),1.6,.06,0,ORANGE,.65F);s.popPose();}
        if(action==HermanoAction.GLARE&&age<19){for(int side:new int[]{-1,1}){s.pushPose();s.translate(side*.14,2.91,-.27);orb(s,b,time,.07F+.03F*smooth(age/16));s.popPose();}}
        if(action==HermanoAction.RUSH&&age>=14&&age<30){Sink glow=sink(s,b,true);for(int i=0;i<8;i++){double a=i*Math.PI/4;ray(glow,new Vector3d(Math.cos(a),1.7+Math.sin(a),.3),new Vector3d(Math.cos(a)*.7,1.7+Math.sin(a),3),.025,ORANGE,.5F);}}
        if(action==HermanoAction.FULL_POWER&&age<52)ring(sink(s,b,true),9,.05,.04,RED,.12F+.2F*smooth(age/50));
    }
    public static void effect(MatrixStack s,IRenderTypeBuffer b,int kind,float age,float life,float size){
        float t=Math.max(0,Math.min(1,age/life)),fade=1-t;Sink glow=sink(s,b,true);
        if(kind==HermanoEffect.GLARE){
            for(int i=0;i<3;i++){s.pushPose();s.translate(0,0,size*t+i*.4);s.mulPose(Vector3f.XP.rotationDegrees(90));ring(sink(s,b,true),.5+t*2.5,.08,0,CORE,fade*.6F);s.popPose();}
            return;
        }
        ring(glow,size*(.1+t),.15*(1-t)+.02,.08,ORANGE,fade*.85F);
        if(kind==HermanoEffect.SHOCK)return;
        if(kind==HermanoEffect.BLAST){sphere(sink(s,b,false),size*smooth(t*3),size*.8*smooth(t*3),size*smooth(t*3),RED,fade*.28F);sphere(glow,size*.6*fade,size*.6*fade,size*.6*fade,ORANGE,fade*.7F);}
        for(int i=0;i<12;i++){
            double a=i*Math.PI/6,h=Math.sin(i*2.4)*.7;Vector3d d=new Vector3d(Math.cos(a),h,Math.sin(a));
            ray(glow,d.scale(size*t*.4),d.scale(size*(.35+t)),.04*fade,kind==HermanoEffect.HIT?CORE:ORANGE,fade);
        }
    }
    private HermanoVfx(){}
}
