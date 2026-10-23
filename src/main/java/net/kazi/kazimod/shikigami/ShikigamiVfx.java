package net.kazi.kazimod.shikigami;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.*;

/** Bounded mesh effects, with no spawned particles or per-frame world searches. */
final class ShikigamiVfx {
    private static final class State extends RenderState {
        State(){super(null,null,null);}
        static RenderType type(){return RenderType.create("shikigami_mesh",DefaultVertexFormats.POSITION_COLOR,7,8192,false,true,
                RenderType.State.builder().setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL)
                        .setWriteMaskState(COLOR_WRITE).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(false));}
    }
    private static final RenderType TYPE=State.type();
    interface Sink{void point(double x,double y,double z,int color,float alpha);}
    private static Sink sink(MatrixStack stack,IRenderTypeBuffer buffers){IVertexBuilder b=buffers.getBuffer(TYPE);Vector4f p=new Vector4f(0,0,0,1);
        return(x,y,z,c,a)->{p.set((float)x,(float)y,(float)z,1);p.transform(stack.last().pose());b.vertex(p.x(),p.y(),p.z()).color(c>>16&255,c>>8&255,c&255,(int)(255*Math.max(0,Math.min(1,a)))).endVertex();};}
    private static void ring(Sink s,double radius,double width,double y,int color,float alpha){
        for(int i=0;i<48;i++){double a=i*Math.PI/24,b=(i+1)*Math.PI/24,r=Math.max(0,radius-width);
            s.point(Math.cos(a)*r,y,Math.sin(a)*r,color,alpha);s.point(Math.cos(a)*radius,y,Math.sin(a)*radius,color,alpha);
            s.point(Math.cos(b)*radius,y,Math.sin(b)*radius,color,alpha);s.point(Math.cos(b)*r,y,Math.sin(b)*r,color,alpha);}
    }
    static void portal(MatrixStack stack,IRenderTypeBuffer buffers,float age,float duration,float size){
        float open=smooth(age/8)*(1-smooth((age-duration+8)/8));Sink s=sink(stack,buffers);double r=size*open;
        ring(s,r,r,.04,0x030208,.98F);ring(s,r+.12,.2,.044,0x23102e,.65F*open);ring(s,r+.16,.035,.048,0x754899,.4F*open);
        for(int i=0;i<12;i++){double a=i*Math.PI/6+age*.02,x=Math.cos(a)*r*.9,z=Math.sin(a)*r*.9,h=(.4+.2*Math.sin(i+age*.15))*open;
            s.point(x-.06,.05,z,0x130a1d,.6F*open);s.point(x+.06,.05,z,0x130a1d,.6F*open);s.point(x+.13,h,z,0x382044,0);s.point(x+.05,h,z,0x382044,0);}
    }
    static void impact(MatrixStack stack,IRenderTypeBuffer buffers,float age){
        Sink s=sink(stack,buffers);float t=Math.min(1,age/35),alpha=(1-t)*.7F;
        ring(s,2+14*smooth(t),.7*(1-t)+.15,.12,0xc0ab91,alpha);
        ring(s,1+10*smooth(t),.3,.18,0xe5d5b4,alpha*.6F);
        for(int i=0;i<28;i++){double a=i*Math.PI*2/28,r=2+t*13,h=Math.sin(t*Math.PI)*(1+i%4*.4),x=Math.cos(a)*r,z=Math.sin(a)*r,w=.12+(i%3)*.08;
            s.point(x-w,h,z-w,0x8c7c69,alpha);s.point(x+w,h+.3,z-w,0x8c7c69,alpha);s.point(x+w,h+.3,z+w,0x8c7c69,alpha);s.point(x-w,h,z+w,0x8c7c69,alpha);}
    }
    static void tongue(ShikigamiEntity e,float partial,MatrixStack stack,IRenderTypeBuffer buffers){
        float age=e.age(partial),extension=smooth(age/7)*(1-smooth((age-32)/8));
        double yaw=Math.toRadians(e.yBodyRot);Vector3d start=new Vector3d(-Math.sin(yaw)*.95,.72,Math.cos(yaw)*.95);
        Vector3d end=e.prey().getPosition(partial).add(0,e.prey().getBbHeight()*.5,0).subtract(e.getPosition(partial));
        end=start.add(end.subtract(start).scale(extension));Vector3d d=end.subtract(start),axis=d.normalize(),u=axis.cross(new Vector3d(0,1,0)).normalize();if(u.lengthSqr()<.1)u=new Vector3d(1,0,0);
        Vector3d v=axis.cross(u).normalize();Sink s=sink(stack,buffers);
        for(int j=0;j<12;j++)for(int side=0;side<6;side++){
            double[] ts={j/12D,j/12D,(j+1)/12D,(j+1)/12D};int[] sides={side,side+1,side+1,side};
            for(int k=0;k<4;k++){double t=ts[k],a=sides[k]*Math.PI/3,r=.12*(1-.35*t);Vector3d p=start.add(d.scale(t)).add(0,-Math.sin(t*Math.PI)*.13,0).add(u.scale(Math.cos(a)*r)).add(v.scale(Math.sin(a)*r));
                s.point(p.x,p.y,p.z,side<3?0xa377af:0x674277,1);}
        }
    }
    static void dust(ShikigamiEntity e,float time,MatrixStack stack,IRenderTypeBuffer buffers){
        if(!e.isOnGround())return;Sink s=sink(stack,buffers);Vector3d forward=e.getLookAngle().multiply(1,0,1).normalize();
        for(int i=0;i<8;i++){float t=(time/14+i/8F)%1;double x=-forward.x*(.5+t*3)+Math.cos(i*7)*.8,z=-forward.z*(.5+t*3)+Math.sin(i*7)*.8,w=.12+t*.2,y=.08+t*.4;
            s.point(x-w,y,z,0xa99b84,(1-t)*.5F);s.point(x+w,y,z,0xa99b84,(1-t)*.5F);s.point(x+w,y+w,z,0xa99b84,0);s.point(x-w,y+w,z,0xa99b84,0);}
    }
    static float smooth(float p){p=Math.max(0,Math.min(1,p));return p*p*(3-2*p);}
    private ShikigamiVfx(){}
}
