package net.kazi.kazimod.yami;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.*;

/** Shared unit meshes: animation transforms geometry instead of rebuilding it every frame. */
public final class YamiMesh {
    public static final ResourceLocation SMOKE=new ResourceLocation("kazimod","textures/vfx/doku_vapor.png");
    private static final double TAU=Math.PI*2;
    private static final class State extends RenderState {
        State(){super(null,null,null);}
        static RenderType ink(){return RenderType.create("yami_ink",DefaultVertexFormats.POSITION_COLOR,7,32768,false,false,
                RenderType.State.builder().setTransparencyState(TRANSLUCENT_TRANSPARENCY).setShadeModelState(SMOOTH_SHADE)
                        .setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));}
        static RenderType smoke(){return RenderType.create("yami_smoke",DefaultVertexFormats.POSITION_COLOR_TEX,7,8192,false,false,
                RenderType.State.builder().setTextureState(new TextureState(SMOKE,true,false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));}
    }
    private static final RenderType INK=State.ink(),VAPOR=State.smoke();
    private static final Mesh POOL_NEAR=buildPool(24,true),POOL_FAR=buildPool(12,false);
    private static final Mesh SPHERE_NEAR=buildSphere(12,24),SPHERE_FAR=buildSphere(6,12);
    private static final Mesh TENDRIL=buildTendril();
    private static final Mesh TORNADO_NEAR=buildTornado(16,32),TORNADO_FAR=buildTornado(8,16);

    private static final class Mesh {
        final float[] vertices;
        Mesh(List<Float> values){vertices=new float[values.size()];for(int i=0;i<vertices.length;i++)vertices[i]=values.get(i);}
        void draw(MatrixStack stack,IRenderTypeBuffer buffers,float alpha) {
            IVertexBuilder out=buffers.getBuffer(INK);Matrix4f pose=stack.last().pose();Vector4f point=new Vector4f();
            for(int i=0;i<vertices.length;i+=5) {
                int c=(int)vertices[i+3],a=(int)(MathHelper.clamp(alpha*vertices[i+4],0,1)*255);
                point.set(vertices[i],vertices[i+1],vertices[i+2],1);point.transform(pose);
                out.vertex(point.x(),point.y(),point.z()).color(c>>16&255,c>>8&255,c&255,a).endVertex();
            }
        }
    }
    private static void vertex(List<Float> out,Vector3d point,int color,float alpha) {
        out.add((float)point.x);out.add((float)point.y);out.add((float)point.z);out.add((float)color);out.add(alpha);
    }
    private static void quad(List<Float> out,Vector3d a,Vector3d b,Vector3d c,Vector3d d,int color,float alpha) {
        vertex(out,a,color,alpha);vertex(out,b,color,alpha);vertex(out,c,color,alpha);vertex(out,d,color,alpha);
    }
    private static Vector3d poolPoint(double a,double t) {
        double r=t*(1+.08*Math.sin(a*5)+.04*Math.cos(a*9));
        return new Vector3d(Math.cos(a)*r,.025,Math.sin(a)*r);
    }
    private static Mesh buildPool(int sides,boolean folds) {
        List<Float> out=new ArrayList<>();
        for(int ring=0;ring<2;ring++)for(int i=0;i<sides;i++) {
            double a=i*TAU/sides,b=(i+1)*TAU/sides;
            vertex(out,poolPoint(a,ring*.5),0x09070e,.94F);vertex(out,poolPoint(b,ring*.5),0x09070e,.94F);
            vertex(out,poolPoint(b,(ring+1)*.5),0x09070e,ring==1?0:.94F);
            vertex(out,poolPoint(a,(ring+1)*.5),0x09070e,ring==1?0:.94F);
        }
        if(folds)for(int arm=0;arm<2;arm++)for(int i=0;i<14;i++) {
            double t=i/14.0,u=(i+1)/14.0;
            Vector3d a=spiral(t,arm),b=spiral(u,arm);
            Vector3d side=b.subtract(a).cross(new Vector3d(0,1,0)).normalize().scale(.02*Math.sin(t*Math.PI));
            quad(out,a.add(side),b.add(side),b.subtract(side),a.subtract(side),0x302337,.4F);
        }
        return new Mesh(out);
    }
    private static Vector3d spiral(double t,int arm) {
        double a=arm*Math.PI+t*5.7,r=.08+.85*t;
        return new Vector3d(Math.cos(a)*r,.045,Math.sin(a)*r);
    }
    private static Mesh buildSphere(int rows,int cols) {
        List<Float> out=new ArrayList<>();Vector3d[][] points=new Vector3d[rows+1][cols+1];
        for(int j=0;j<=rows;j++)for(int k=0;k<=cols;k++) {
            double v=j*Math.PI/rows,u=k*TAU/cols,r=1+.018*Math.sin(u*7+v*9);
            points[j][k]=new Vector3d(Math.sin(v)*Math.cos(u)*r,Math.cos(v)*r,Math.sin(v)*Math.sin(u)*r);
        }
        for(int j=0;j<rows;j++)for(int k=0;k<cols;k++) {
            int shade=3+(int)(4*(.5+.5*Math.sin(k*.8+j*.65)));
            int color=(5+shade)<<16|(3+shade/2)<<8|10+shade;
            quad(out,points[j][k],points[j+1][k],points[j+1][k+1],points[j][k+1],color,1);
        }
        return new Mesh(out);
    }
    private static Mesh buildTendril() {
        int steps=10,sides=5;Vector3d[] centers=new Vector3d[steps+1];Vector3d[][] rings=new Vector3d[steps+1][sides];
        List<Float> out=new ArrayList<>();
        for(int j=0;j<=steps;j++) {
            double t=j/(double)steps,bend=t*2.8,spread=.55*(1-.3*t)+Math.sin(t*Math.PI)*.3+t*t*t*.55;
            centers[j]=new Vector3d(Math.cos(bend)*spread,t-.28*t*t*t,Math.sin(bend)*spread);
        }
        for(int j=0;j<=steps;j++) {
            double t=j/(double)steps;Vector3d tangent=centers[Math.min(steps,j+1)].subtract(centers[Math.max(0,j-1)]).normalize();
            Vector3d radial=new Vector3d(centers[j].x,0,centers[j].z).normalize();
            Vector3d side=radial.subtract(tangent.scale(radial.dot(tangent))).normalize(),up=tangent.cross(side).normalize();
            double width=.28*Math.sqrt(1-t*t);
            for(int k=0;k<sides;k++){double a=k*TAU/sides;rings[j][k]=centers[j].add(side.scale(Math.cos(a)*width)).add(up.scale(Math.sin(a)*width));}
        }
        for(int j=0;j<steps;j++)for(int k=0;k<sides;k++) {
            int c=k==0?0x23182e:k==1?0x160e20:0x0a0710;
            float a=Math.min(1,(steps-j)/3F)*.8F,b=Math.min(1,(steps-j-1)/3F)*.8F;
            vertex(out,rings[j][k],c,a);vertex(out,rings[j+1][k],c,b);
            vertex(out,rings[j+1][(k+1)%sides],c,b);vertex(out,rings[j][(k+1)%sides],c,a);
        }
        return new Mesh(out);
    }
    private static Vector3d funnel(double angle,double height,double extra) {
        double radius=.13+.87*Math.pow(height,.72)+extra;
        return new Vector3d(Math.cos(angle)*radius,height,Math.sin(angle)*radius);
    }
    private static Mesh buildTornado(int rows,int columns) {
        List<Float> out=new ArrayList<>();
        for(int row=0;row<rows;row++)for(int col=0;col<columns;col++) {
            double lo=row/(double)rows,hi=(row+1)/(double)rows,a=col*TAU/columns,b=(col+1)*TAU/columns;
            float lower=.6F*(float)Math.sin((.08+lo*.9)*Math.PI),upper=.6F*(float)Math.sin((.08+hi*.9)*Math.PI);
            vertex(out,funnel(a,lo,0),0x100a18,lower);vertex(out,funnel(b,lo,0),0x100a18,lower);
            vertex(out,funnel(b,hi,0),0x100a18,upper);vertex(out,funnel(a,hi,0),0x100a18,upper);
        }
        // Feathered edges keep the cached helical ribbons from reading as rigid bands.
        int steps=rows*4;
        for(int ribbon=0;ribbon<3;ribbon++)for(int i=0;i<steps;i++) {
            double t=i/(double)steps,u=(i+1)/(double)steps,a=ribbon*TAU/3+t*TAU*2.4,b=ribbon*TAU/3+u*TAU*2.4;
            double width=.045;int color=ribbon==1?0x4a3654:0x040208;
            float alpha=(float)(.82*Math.sin(Math.PI*t)),next=(float)(.82*Math.sin(Math.PI*u));
            vertex(out,funnel(a,Math.max(0,t-width),.015),color,0);vertex(out,funnel(b,Math.max(0,u-width),.015),color,0);
            vertex(out,funnel(b,u,.015),color,next);vertex(out,funnel(a,t,.015),color,alpha);
            vertex(out,funnel(a,t,.015),color,alpha);vertex(out,funnel(b,u,.015),color,next);
            vertex(out,funnel(b,Math.min(1,u+width),.015),color,0);vertex(out,funnel(a,Math.min(1,t+width),.015),color,0);
        }
        return new Mesh(out);
    }
    public static void tornado(MatrixStack s,IRenderTypeBuffer b,float radius,float age,float alpha,int seed,boolean far) {
        pool(s,b,radius,age*2,alpha*.6F,seed,far);
        float rise=Math.min(1,age/12);
        s.pushPose();s.scale(radius*rise,24*rise,radius*rise);
        s.mulPose(Vector3f.YP.rotationDegrees(age*6+(seed&255)));
        (far?TORNADO_FAR:TORNADO_NEAR).draw(s,b,alpha);s.popPose();
    }
    private static void tornadoSmoke(MatrixStack s,IRenderTypeBuffer b,Quaternion camera,float radius,float age,float alpha,int seed,boolean far) {
        IVertexBuilder out=b.getBuffer(VAPOR);int count=far?3:8;
        for(int i=0;i<count;i++) {
            double t=(i/(double)count+age*.009)%1,a=i*2.4+age*.08;
            Vector3d pos=funnel(a,t,0);double size=radius*(.12+.12*t);
            puff(s,out,camera,pos.x*radius,pos.y*24,pos.z*radius,size,size*1.3,age+i*35,
                    i%3==0?0x38273f:0x08050d,alpha*.65F*(float)Math.sin(t*Math.PI),i%4);
        }
    }
    private static double noise(int i,int seed){double n=Math.sin(i*127.1+seed*17.73)*43758.5453;return n-Math.floor(n);}
    public static void pool(MatrixStack s,IRenderTypeBuffer buffers,float radius,float age,float alpha,int seed,boolean far) {
        s.pushPose();s.scale(radius,1,radius);s.mulPose(Vector3f.YP.rotationDegrees((seed&255)-age*.6F));
        (far?POOL_FAR:POOL_NEAR).draw(s,buffers,alpha);s.popPose();
    }
    public static void tendrils(MatrixStack s,IRenderTypeBuffer buffers,float radius,float height,float age,float alpha,int seed,int count,boolean far) {
        if(far)return;
        for(int i=0;i<Math.min(count,3);i++) {
            s.pushPose();s.mulPose(Vector3f.YP.rotationDegrees((seed&255)+i*137+age*.7F));
            s.scale(radius,height*(.8F+.15F*MathHelper.sin(age*.035F+i)),radius);
            TENDRIL.draw(s,buffers,alpha);s.popPose();
        }
    }
    private static void sphereGeometry(MatrixStack s,IRenderTypeBuffer buffers,float radius,float age,float alpha,int seed,boolean far) {
        s.pushPose();float pulse=radius*(1+.014F*MathHelper.sin(age*.06F));s.scale(pulse,pulse,pulse);
        s.mulPose(Vector3f.YP.rotationDegrees(age*.45F+(seed&255)));(far?SPHERE_FAR:SPHERE_NEAR).draw(s,buffers,alpha);s.popPose();
        if(!far&&radius>.5F) {
            s.pushPose();s.mulPose(Vector3f.ZP.rotationDegrees(135));s.translate(0,-radius*.3,0);
            tendrils(s,buffers,radius*.9F,radius*1.9F,age,alpha*.85F,seed,2,false);s.popPose();
        }
    }
    public static void sphere(MatrixStack s,IRenderTypeBuffer buffers,Quaternion camera,float radius,float age,float alpha,int seed,boolean far) {
        sphereGeometry(s,buffers,radius,age,alpha,seed,far);
        vapor(s,buffers,camera,radius,radius*1.7F,age,alpha,seed,far?3:8,true);
    }
    public static void vapor(MatrixStack s,IRenderTypeBuffer buffers,Quaternion camera,float radius,float height,float age,float alpha,int seed,int count,boolean sphere) {
        IVertexBuilder out=buffers.getBuffer(VAPOR);
        for(int i=0;i<Math.min(count,8);i++) {
            double cycle=(age*.008+noise(i,seed))%1,a=noise(i+29,seed)*TAU+age*.009;
            double r=radius*(sphere?.7+.25*noise(i+54,seed):.25+.65*noise(i+54,seed));
            double y=height*(sphere?noise(i+7,seed)-.5:cycle*.8),size=radius*.4*(1+cycle*.4);
            float opacity=alpha*(float)Math.sin(cycle*Math.PI)*.8F;
            puff(s,out,camera,Math.cos(a)*r,y,Math.sin(a)*r,size,size*(sphere?1:1.25),age*.28+i*67,
                    i%3==0?0x211727:0x050408,opacity,i%4);
        }
    }
    private static void puff(MatrixStack s,IVertexBuilder out,Quaternion camera,double x,double y,double z,double w,double h,double angle,int c,float alpha,int tile) {
        s.pushPose();s.translate(x,y,z);s.mulPose(camera);s.mulPose(Vector3f.ZP.rotationDegrees((float)angle));
        float u0=(tile+.003F)/4,u1=(tile+.997F)/4;
        tex(out,s,-w,-h,c,alpha,u0,.997F);tex(out,s,w,-h,c,alpha,u1,.997F);
        tex(out,s,w,h,c,alpha,u1,.003F);tex(out,s,-w,h,c,alpha,u0,.003F);s.popPose();
    }
    private static void tex(IVertexBuilder out,MatrixStack s,double x,double y,int c,float alpha,float u,float v) {
        out.vertex(s.last().pose(),(float)x,(float)y,0).color(c>>16&255,c>>8&255,c&255,(int)(MathHelper.clamp(alpha,0,1)*255)).uv(u,v).endVertex();
    }
    public static void effectGeometry(MatrixStack s,IRenderTypeBuffer b,int kind,float radius,float age,float alpha,int seed,boolean far) {
        if(kind==YamiEffects.TORNADO){tornado(s,b,radius,age,alpha,seed,far);return;}
        if(kind==YamiEffects.PORTAL){pool(s,b,radius,age*5,alpha,seed,far);return;}
        if(kind==YamiEffects.PULL||kind==YamiEffects.HAND)return;
        if(kind==YamiEffects.COAT){sphereGeometry(s,b,radius*.43F,age,alpha,seed,true);return;}
        float r=kind==YamiEffects.BURST?radius*Math.min(1,.2F+age/12):radius;
        pool(s,b,r,age,alpha,seed,far);
        // Ground pools are flat; only large bursts need volumetric curls.
        if(kind!=YamiEffects.POOL)tendrils(s,b,r*.8F,height(kind,r),age,alpha,seed,kind==YamiEffects.RELEASE?3:2,far);
    }
    public static void effectSmoke(MatrixStack s,IRenderTypeBuffer b,Quaternion camera,int kind,float radius,float age,float alpha,int seed,boolean far) {
        if(kind==YamiEffects.TORNADO) {
            tornadoSmoke(s,b,camera,radius,age,alpha,seed,far);return;
        }
        // The portal is already oriented into the aim plane; billboard smoke would rotate twice.
        if(kind==YamiEffects.PORTAL)return;
        if(kind==YamiEffects.HAND)return;
        if(kind==YamiEffects.COAT||kind==YamiEffects.PULL) {
            vapor(s,b,camera,radius*(kind==YamiEffects.COAT?.43F:.65F),radius,age,alpha,seed,far?2:4,true);return;
        }
        float r=kind==YamiEffects.BURST?radius*Math.min(1,.2F+age/12):radius;
        vapor(s,b,camera,r,height(kind,r),age,alpha,seed,far?1:kind==YamiEffects.POOL?3:8,false);
    }
    private static float height(int kind,float r){return kind==YamiEffects.RELEASE?r*3.4F:kind==YamiEffects.CHARGE?r*2.3F:kind==YamiEffects.POOL?r*.3F:r*.8F;}
    public static void effect(MatrixStack s,IRenderTypeBuffer b,Quaternion camera,int kind,float radius,float age,float alpha,int seed,boolean far) {
        effectGeometry(s,b,kind,radius,age,alpha,seed,far);effectSmoke(s,b,camera,kind,radius,age,alpha,seed,far);
    }
}
