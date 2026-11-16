package net.kazi.kazimod.pika;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.*;

/** Feathered, full-bright light geometry; one additive material and no particle instances. */
public final class PikaMesh extends RenderType {
    public static final RenderType LIGHT=create("kazi_pika_light",DefaultVertexFormats.POSITION_COLOR,7,262144,false,false,
            State.builder().setTransparencyState(LIGHTNING_TRANSPARENCY).setCullState(NO_CULL)
                    .setShadeModelState(SMOOTH_SHADE).setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    private static final int N=48;
    private static final float[] C=new float[N+1],S=new float[N+1];
    static {for(int i=0;i<=N;i++){C[i]=(float)Math.cos(i*Math.PI*2/N);S[i]=(float)Math.sin(i*Math.PI*2/N);}}
    private PikaMesh(String name,Runnable setup,Runnable clear){super(name,DefaultVertexFormats.POSITION_COLOR,7,256,false,false,setup,clear);}
    private static final class Draw {
        final IVertexBuilder out; final Matrix4f m; final Vector4f v=new Vector4f();
        Draw(MatrixStack stack,IRenderTypeBuffer buffers){out=buffers.getBuffer(LIGHT);m=stack.last().pose();}
        void v(float x,float y,float z,float warmth,float alpha) {
            v.set(x,y,z,1);v.transform(m);
            out.vertex(v.x(),v.y(),v.z()).color(1,MathHelper.lerp(warmth,.53F,.99F),MathHelper.lerp(warmth,.025F,.86F),alpha).endVertex();
        }
    }
    public static void flare(MatrixStack stack,IRenderTypeBuffer buffers,Quaternion camera,float radius,float alpha,float rotation,boolean far) {
        stack.pushPose();if(camera!=null)stack.mulPose(camera);stack.mulPose(Vector3f.ZP.rotationDegrees(rotation));
        Draw d=new Draw(stack,buffers);int step=far?4:2;
        for(int i=0;i<N;i+=step) {
            d.v(0,0,0,.75F,.35F*alpha);d.v(C[i]*radius,S[i]*radius,0,0,0);
            d.v(C[i+step]*radius,S[i+step]*radius,0,0,0);d.v(0,0,0,.75F,.35F*alpha);
        }
        for(int k=0;k<4;k++) {
            float angle=k*(float)Math.PI/2,c=(float)Math.cos(angle),s=(float)Math.sin(angle);
            float len=radius*(k%2==0?1.65F:1.3F),w=radius*.13F;
            d.v(0,0,0,1,alpha);d.v(-s*w,c*w,0,.45F,0);
            d.v(c*len,s*len,0,.1F,0);d.v(s*w,-c*w,0,.45F,0);
            d.v(0,0,.002F,1,alpha);d.v(-s*w*.17F,c*w*.17F,.002F,1,alpha*.7F);
            d.v(c*len*.8F,s*len*.8F,.002F,.65F,0);d.v(s*w*.17F,-c*w*.17F,.002F,1,alpha*.7F);
        }
        stack.popPose();
    }
    public static void ring(MatrixStack stack,IRenderTypeBuffer buffers,float radius,float width,float alpha,boolean far) {
        Draw d=new Draw(stack,buffers);int step=far?4:2;
        for(int i=0;i<N;i+=step)for(int edge=-1;edge<=1;edge+=2) {
            d.v(C[i]*radius,S[i]*radius,0,.8F,alpha);d.v(C[i+step]*radius,S[i+step]*radius,0,.8F,alpha);
            d.v(C[i+step]*(radius+edge*width),S[i+step]*(radius+edge*width),0,.1F,0);
            d.v(C[i]*(radius+edge*width),S[i]*(radius+edge*width),0,.1F,0);
        }
    }
    public static void beam(MatrixStack stack,IRenderTypeBuffer buffers,float radius,float length,float age,float alpha,boolean far) {
        beam(stack,buffers,radius,length,age,alpha,far,false);
    }
    public static void beam(MatrixStack stack,IRenderTypeBuffer buffers,float radius,float length,float age,float alpha,boolean far,boolean trail) {
        Draw d=new Draw(stack,buffers);int step=far?8:4;
        for(int layer=0;layer<2;layer++) {
            float r=radius*(layer==0?3.2F:1.35F),a=alpha*(layer==0?.09F:.24F),warmth=layer==0?.12F:.5F;
            // Intersecting radial ribbons fade to zero at their edges, unlike opaque nested tubes.
            for(int i=0;i<N;i+=N/8) {
                d.v(0,0,0,warmth,a);d.v(C[i]*r,S[i]*r,0,0,0);
                d.v(C[i]*r*(trail?.05F:1),S[i]*r*(trail?.05F:1),length,0,0);
                d.v(0,0,length,warmth,trail?0:a);
            }
        }
        float r=radius*.35F;
        for(int i=0;i<N;i+=step) {
            d.v(C[i]*r,S[i]*r,0,1,alpha*.75F);d.v(C[i+step]*r,S[i+step]*r,0,1,alpha*.75F);
            d.v(C[i+step]*r*(trail?0:1),S[i+step]*r*(trail?0:1),length,1,trail?0:alpha*.75F);
            d.v(C[i]*r*(trail?0:1),S[i]*r*(trail?0:1),length,1,trail?0:alpha*.75F);
        }
        if(!far&&!trail)for(int i=0;i<3;i++) {
            float z=((age*.065F+i/3F)%1)*length;
            stack.pushPose();stack.translate(0,0,z);ring(stack,buffers,radius*1.6F,radius*.7F,alpha*.3F,false);stack.popPose();
        }
    }
    public static void crescent(MatrixStack stack,IRenderTypeBuffer buffers,float radius,float age,float alpha,boolean far) {
        Draw d=new Draw(stack,buffers);int count=far?12:24;
        for(int layer=0;layer<3;layer++)for(int i=0;i<count;i++) {
            float a=-1.5F+3F*i/count,b=-1.5F+3F*(i+1)/count;
            float width=layer==0?1.2F:layer==1?.42F:.12F,warm=layer==2?1:.15F,opacity=alpha*(layer==0?.12F:layer==1?.4F:.9F);
            float ca=(float)Math.cos(a),sa=(float)Math.sin(a),cb=(float)Math.cos(b),sb=(float)Math.sin(b);
            d.v(0,sa*radius,ca*radius,warm,opacity*ca);d.v(0,sb*radius,cb*radius,warm,opacity*cb);
            d.v(0,sb*radius,cb*radius-width*cb,0,0);d.v(0,sa*radius,ca*radius-width*ca,0,0);
            d.v(-width*.16F,sa*radius,ca*radius,.1F,0);d.v(-width*.16F,sb*radius,cb*radius,.1F,0);
            d.v(width*.16F,sb*radius,cb*radius,1,opacity*.5F);d.v(width*.16F,sa*radius,ca*radius,1,opacity*.5F);
        }
    }
    public static void burst(MatrixStack stack,IRenderTypeBuffer buffers,Quaternion camera,float radius,float age,float alpha,boolean far) {
        float t=MathHelper.clamp(age/22,0,1),r=radius*(.2F+.8F*(1-(1-t)*(1-t)));
        flare(stack,buffers,camera,r*(1.2F-t*.5F),alpha,0,far);
        stack.pushPose();stack.mulPose(Vector3f.XP.rotationDegrees(90));
        ring(stack,buffers,r*1.5F,Math.max(.12F,r*.06F),alpha*.7F,far);stack.popPose();
        if(!far) {
            stack.pushPose();if(camera!=null)stack.mulPose(camera);
            ring(stack,buffers,r*.8F,r*.08F,alpha*.5F,false);stack.popPose();
            for(int i=0;i<8;i++) {
                stack.pushPose();stack.mulPose(Vector3f.YP.rotationDegrees(i*137.5F));
                stack.mulPose(Vector3f.XP.rotationDegrees((float)Math.toDegrees(Math.asin(-.8+1.6*i/7))));
                stack.translate(0,0,r*.2F);beam(stack,buffers,.025F+radius*.012F,r,age,alpha*.6F,true,true);stack.popPose();
            }
        }
    }
    public static void charge(MatrixStack stack,IRenderTypeBuffer buffers,Quaternion camera,float age,float alpha,boolean cut,boolean far) {
        float pulse=1+(float)Math.sin(age*.22)*.08F;
        stack.pushPose();stack.translate(0,1.1,0);flare(stack,buffers,camera,pulse*(cut?1.2F:.7F),alpha,0,far);stack.popPose();
        stack.pushPose();stack.translate(0,.05,0);stack.mulPose(Vector3f.XP.rotationDegrees(90));
        ring(stack,buffers,(cut?2.3F:1)*pulse,.14F,alpha*.4F,far);stack.popPose();
        int count=far?3:7;
        for(int i=0;i<count;i++) {
            float a=age*.045F+i*(float)Math.PI*2/count,phase=(age*.04F+i/(float)count)%1;
            stack.pushPose();stack.translate(Math.cos(a)*(1-phase),.1+phase*1.9,Math.sin(a)*(1-phase));
            flare(stack,buffers,camera,.15F+phase*.15F,alpha*(1-phase),45,true);stack.popPose();
        }
    }
}
