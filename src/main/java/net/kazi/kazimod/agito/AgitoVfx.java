package net.kazi.kazimod.agito;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Vector4f;

/** Batched, short mesh ribbons; no particle entities or world scans. */
final class AgitoVfx {
    interface Sink {void vertex(double x,double y,double z,int color,float alpha);}
    private static final class State extends RenderState {
        State(){super(null,null,null);}
        static RenderType type(boolean glow){return RenderType.create(glow?"agito_glow":"agito_shadow",DefaultVertexFormats.POSITION_COLOR,7,8192,false,true,
                RenderType.State.builder().setTransparencyState(glow?ADDITIVE_TRANSPARENCY:TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(false));}
    }
    private static final RenderType SHADOW=State.type(false),GLOW=State.type(true);
    static Sink sink(MatrixStack stack,IRenderTypeBuffer buffers,boolean glow){
        IVertexBuilder buffer=buffers.getBuffer(glow?GLOW:SHADOW);Vector4f point=new Vector4f(0,0,0,1);
        return(x,y,z,c,a)->{point.set((float)x,(float)y,(float)z,1);point.transform(stack.last().pose());
            buffer.vertex(point.x(),point.y(),point.z()).color(c>>16&255,c>>8&255,c&255,(int)(255*Math.max(0,Math.min(1,a)))).endVertex();};
    }
    static void arc(Sink s,double radius,double width,double y,double begin,double end,int color,float alpha,int segments){
        for(int i=0;i<segments;i++){
            double a=begin+(end-begin)*i/segments,b=begin+(end-begin)*(i+1)/segments,inner=Math.max(0,radius-width);
            s.vertex(Math.cos(a)*inner,y,Math.sin(a)*inner,color,alpha);s.vertex(Math.cos(a)*radius,y,Math.sin(a)*radius,color,alpha);
            s.vertex(Math.cos(b)*radius,y,Math.sin(b)*radius,color,alpha);s.vertex(Math.cos(b)*inner,y,Math.sin(b)*inner,color,alpha);
        }
    }
    static void portal(MatrixStack stack,IRenderTypeBuffer buffers,float age){
        float open=smooth(age/12)*(1-smooth((age-70)/10));double radius=2.8*open;
        Sink shadow=sink(stack,buffers,false);
        arc(shadow,radius,radius,.035,0,Math.PI*2,0x05030a,.97F,56);
        for(int i=0;i<3;i++)arc(shadow,radius+i*.12,.18,.04+i*.002,0,Math.PI*2,0x21132e,(.28F-i*.06F)*open,56);
        for(int i=0;i<8;i++){
            double phase=age*.025+i*Math.PI/4,x=Math.cos(phase)*radius*.88,z=Math.sin(phase)*radius*.88,h=(.7+.4*Math.sin(age*.08+i))*open;
            shadow.vertex(x-.05,.06,z,0x090512,.7F*open);shadow.vertex(x+.05,.06,z,0x090512,.7F*open);
            shadow.vertex(x+.16,h,z+.13,0x30173d,0);shadow.vertex(x+.1,h,z+.13,0x30173d,0);
        }
        Sink glow=sink(stack,buffers,true);
        for(int i=0;i<6;i++)arc(glow,radius*.94,.025,.055,age*.025+i*Math.PI/3,age*.025+i*Math.PI/3+.5,0x9f64c9,.3F*open,10);
    }
    static void electricity(MatrixStack stack,IRenderTypeBuffer buffers,float time,float alpha){
        Sink s=sink(stack,buffers,true);int frame=(int)(time/2);
        for(int bolt=0;bolt<7;bolt++){
            double previousX=0,previousY=0,previousZ=0;
            for(int j=0;j<=7;j++){
                double angle=bolt*Math.PI*2/7+j*.55+frame*.28;
                double radius=6.3+1.2*Math.sin(frame*1.7+bolt*7+j*11),y=-5.5+j*1.6;
                double x=Math.cos(angle)*radius,z=Math.sin(angle)*radius;
                if(j>0){
                    line(s,previousX,previousY,previousZ,x,y,z,.85,0x7620ec,.28F*alpha);
                    line(s,previousX,previousY,previousZ,x,y,z,.3,0xb964ff,.9F*alpha);
                    line(s,previousX,previousY,previousZ,x,y,z,.11,0xf8dfff,alpha);
                }
                previousX=x;previousY=y;previousZ=z;
            }
        }
    }
    private static void line(Sink s,double x,double y,double z,double X,double Y,double Z,double width,int color,float alpha){
        s.vertex(x-width,y,z,color,alpha);s.vertex(x+width,y,z,color,alpha);
        s.vertex(X+width,Y,Z,color,alpha);s.vertex(X-width,Y,Z,color,alpha);
        s.vertex(x,y,z-width,color,alpha);s.vertex(x,y,z+width,color,alpha);
        s.vertex(X,Y,Z+width,color,alpha);s.vertex(X,Y,Z-width,color,alpha);
    }
    static void healing(MatrixStack stack,IRenderTypeBuffer buffers,float age){
        Sink s=sink(stack,buffers,true);float alpha=smooth(age/14);
        arc(s,.68,.035,.055,0,Math.PI*2,0xc3a1ff,.5F*alpha,40);
        for(int i=0;i<3;i++){
            float p=(age/42+i/3F)%1;
            arc(s,.48+.08*Math.sin(p*Math.PI),.018,.1+p*1.9,0,Math.PI*2,0xe4d6ff,(float)Math.sin(p*Math.PI)*.35F*alpha,32);
        }
        for(int strand=0;strand<3;strand++)for(int j=0;j<18;j++){
            double a=j*.18+age*.06+strand*Math.PI*2/3,b=a+.18,y=j*.1;
            line(s,Math.cos(a)*.5,y,Math.sin(a)*.5,Math.cos(b)*.5,y+.1,Math.sin(b)*.5,.009,0xd6eeb7,.5F*alpha);
        }
    }
    static float smooth(float p){p=Math.max(0,Math.min(1,p));return p*p*(3-2*p);}
    private AgitoVfx(){}
}
