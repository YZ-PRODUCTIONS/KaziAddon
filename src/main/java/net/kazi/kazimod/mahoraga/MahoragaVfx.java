package net.kazi.kazimod.mahoraga;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Vector4f;

/** Small, batched meshes; no persistent particles or per-frame world searches. */
final class MahoragaVfx {
    interface Sink { void vertex(double x,double y,double z,int color,float alpha); }
    private static final class State extends RenderState {
        State(){super(null,null,null);}
        static RenderType type(boolean glow){return RenderType.create(glow?"mahoraga_glow":"mahoraga_shadow",DefaultVertexFormats.POSITION_COLOR,7,8192,false,true,
                RenderType.State.builder().setTransparencyState(glow?ADDITIVE_TRANSPARENCY:TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(false));}
    }
    private static final RenderType SHADOW=State.type(false),GLOW=State.type(true);
    static Sink sink(MatrixStack stack,IRenderTypeBuffer buffers,boolean glow){
        IVertexBuilder buffer=buffers.getBuffer(glow?GLOW:SHADOW);Vector4f point=new Vector4f(0,0,0,1);
        return(x,y,z,c,a)->{point.set((float)x,(float)y,(float)z,1);point.transform(stack.last().pose());
            buffer.vertex(point.x(),point.y(),point.z()).color(c>>16&255,c>>8&255,c&255,(int)(255*Math.max(0,Math.min(1,a)))).endVertex();};
    }
    static void ring(Sink s,double radius,double width,double y,int color,float alpha,int segments){arc(s,radius,width,y,0,Math.PI*2,color,alpha,segments);}
    static void arc(Sink s,double radius,double width,double y,double begin,double end,int color,float alpha,int segments){
        for(int i=0;i<segments;i++){
            double a=begin+(end-begin)*i/segments,b=begin+(end-begin)*(i+1)/segments;
            double inner=Math.max(0,radius-width),outer=radius;
            s.vertex(Math.cos(a)*inner,y,Math.sin(a)*inner,color,alpha);s.vertex(Math.cos(a)*outer,y,Math.sin(a)*outer,color,alpha);
            s.vertex(Math.cos(b)*outer,y,Math.sin(b)*outer,color,alpha);s.vertex(Math.cos(b)*inner,y,Math.sin(b)*inner,color,alpha);
        }
    }
    static void portal(MatrixStack stack,IRenderTypeBuffer buffers,float age){
        float open=smooth(age/12)*(1-smooth((age-70)/10));double radius=3.1*open;
        Sink shadow=sink(stack,buffers,false),glow=sink(stack,buffers,true);
        ring(shadow,radius,radius,.035,0x06040c,.96F,64);
        for(int i=0;i<4;i++)ring(shadow,radius+i*.12,.18,.038+i*.002,0x181225,(.32F-i*.06F)*open,64);
        for(int i=0;i<8;i++){
            double phase=age*.025+i*Math.PI/4,r=radius*(.65+.05*(i%3));
            arc(glow,r,.025,.055,phase,phase+.6,0x69558b,.3F*open,10);
            double x=Math.cos(phase)*radius*.87,z=Math.sin(phase)*radius*.87,h=(.6+.6*Math.sin(age*.08+i))*open,w=.06;
            shadow.vertex(x-w,.06,z,0x070510,.7F*open);shadow.vertex(x+w,.06,z,0x070510,.7F*open);
            shadow.vertex(x+.18,h,z+.13,0x221735,0);shadow.vertex(x+.12,h,z+.13,0x221735,0);
        }
    }
    static void impact(MatrixStack stack,IRenderTypeBuffer buffers,float age){
        if(age<0 || age>18)return;float p=age/18,alpha=(1-p)*.8F;
        Sink s=sink(stack,buffers,true);ring(s,.5+p*8,.15*(1-p)+.04,.1,0xc9e4eb,alpha,64);
        ring(s,.3+p*6,.06,.14,0xeaf4f7,alpha*.6F,48);
        for(int i=0;i<12;i++)arc(s,1+p*7,.32*(1-p),.06,i*Math.PI/6,i*Math.PI/6+.1,0xadaaa2,alpha,3);
    }
    static void pressure(MatrixStack stack,IRenderTypeBuffer buffers,float age,float scale,float fade){
        Sink s=sink(stack,buffers,true);
        // Rings lie across the local Z axis, which the renderer aligns with velocity.
        for(int i=0;i<4;i++){
            double z=-i*.75,rad=scale*(.7+i*.2+.05*Math.sin(age*.6+i));float a=fade*(.55F-i*.09F);
            for(int j=0;j<40;j++){
                double u=j*Math.PI/20,v=(j+1)*Math.PI/20,w=.075*scale;
                s.vertex(Math.cos(u)*(rad-w),Math.sin(u)*(rad-w),z,0xd8f5ff,a);
                s.vertex(Math.cos(u)*rad,Math.sin(u)*rad,z,0xf4fdff,a);
                s.vertex(Math.cos(v)*rad,Math.sin(v)*rad,z,0xf4fdff,a);
                s.vertex(Math.cos(v)*(rad-w),Math.sin(v)*(rad-w),z,0xd8f5ff,a);
            }
        }
        for(int i=0;i<8;i++){
            double a=i*Math.PI/4+age*.02,x=Math.cos(a)*scale,y=Math.sin(a)*scale;
            s.vertex(x*.72,y*.72,.25,0xffffff,.5F*fade);s.vertex(x*.76+.025,y*.76+.025,.25,0xffffff,.5F*fade);
            s.vertex(x*1.5+.025,y*1.5+.025,-3.7,0x9fc2d4,0);s.vertex(x*1.5,y*1.5,-3.7,0x9fc2d4,0);
        }
    }
    static float smooth(float p){p=Math.max(0,Math.min(1,p));return p*p*(3-2*p);}
    private MahoragaVfx() {}
}
