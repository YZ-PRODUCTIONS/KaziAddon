package net.kazi.kazimod.moku;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import java.util.List;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.*;

/** Shaded billows and feathered turbulence share two bounded mesh passes. */
public final class MokuMesh extends RenderType {
    public static final ResourceLocation TEXTURE=new ResourceLocation("kazimod","textures/vfx/moku_smoke.png");
    public static final RenderType BODY=create("kazi_moku_body",DefaultVertexFormats.POSITION_COLOR,7,131072,false,false,
            State.builder().setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setShadeModelState(SMOOTH_SHADE)
                    .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    public static final RenderType MIST=create("kazi_moku_mist",DefaultVertexFormats.POSITION_COLOR_TEX,7,65536,false,false,
            State.builder().setTextureState(new TextureState(TEXTURE,true,false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL).setShadeModelState(SMOOTH_SHADE).setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    private static final Vector3f[][] NEAR=sphere(12,6),FAR=sphere(8,4);
    private MokuMesh(String name,Runnable setup,Runnable clear){super(name,DefaultVertexFormats.POSITION_COLOR,7,256,false,false,setup,clear);}
    private static Vector3f[][] sphere(int lon,int lat) {
        Vector3f[][] grid=new Vector3f[lat+1][lon+1];
        for(int y=0;y<=lat;y++)for(int x=0;x<=lon;x++){
            double a=x*Math.PI*2/lon,b=y*Math.PI/lat;
            grid[y][x]=new Vector3f((float)(Math.cos(a)*Math.sin(b)),(float)Math.cos(b),(float)(Math.sin(a)*Math.sin(b)));
        }return grid;
    }
    private static double noise(int i,int seed){double v=Math.sin(i*127.1+seed*31.73)*43758.5453;return v-Math.floor(v);}
    private static final class Draw {
        final IVertexBuilder out;final Matrix4f matrix;final Vector4f v=new Vector4f();
        Draw(MatrixStack s,IRenderTypeBuffer b,RenderType type){out=b.getBuffer(type);matrix=s.last().pose();}
        void vertex(float x,float y,float z,float shade,float alpha){
            v.set(x,y,z,1);v.transform(matrix);out.vertex(v.x(),v.y(),v.z()).color(shade*.965F,shade*.985F,shade,alpha).endVertex();
        }
        void textured(float x,float y,float shade,float alpha,float u,float texV){
            v.set(x,y,0,1);v.transform(matrix);out.vertex(v.x(),v.y(),v.z()).color(shade*.965F,shade*.985F,shade,alpha).uv(u,texV).endVertex();
        }
    }
    private static void billow(MatrixStack s,IRenderTypeBuffer b,double x,double y,double z,float w,float h,float age,float alpha,int seed,boolean far) {
        Vector3f[][] grid=far?FAR:NEAR;Draw d=new Draw(s,b,BODY);
        for(int row=0;row<grid.length-1;row++)for(int col=0;col<grid[0].length-1;col++) {
            sphereVertex(d,grid[row][col],x,y,z,w,h,age,alpha,seed);
            sphereVertex(d,grid[row+1][col],x,y,z,w,h,age,alpha,seed);
            sphereVertex(d,grid[row+1][col+1],x,y,z,w,h,age,alpha,seed);
            sphereVertex(d,grid[row][col+1],x,y,z,w,h,age,alpha,seed);
        }
    }
    private static void sphereVertex(Draw d,Vector3f p,double x,double y,double z,float w,float h,float age,float alpha,int seed) {
        float swell=1+.09F*(float)Math.sin(p.x()*4+p.z()*5+p.y()*3+age*.055F+seed);
        float shade=.72F+.25F*(p.y()*.5F+.5F)+.025F*(float)Math.sin(p.x()*5+p.z()*3+age*.02F);
        d.vertex((float)x+p.x()*w*swell,(float)y+p.y()*h*swell,(float)z+p.z()*w*swell,shade,alpha);
    }
    private static void puff(MatrixStack s,IRenderTypeBuffer b,Quaternion camera,double x,double y,double z,float w,float h,float rotation,float alpha,int tile) {
        s.pushPose();s.translate(x,y,z);s.mulPose(camera);s.mulPose(Vector3f.ZP.rotationDegrees(rotation));Draw d=new Draw(s,b,MIST);
        float u=(tile%2+.004F)/2,v=(tile%2+.996F)/2,top=(tile%4/2+.004F)/2,bottom=(tile%4/2+.996F)/2;
        d.textured(-w,-h,.9F,alpha,u,bottom);d.textured(w,-h,.9F,alpha,v,bottom);
        d.textured(w,h,1,alpha,v,top);d.textured(-w,h,1,alpha,u,top);s.popPose();
    }
    public static void volume(MatrixStack s,IRenderTypeBuffer b,Quaternion camera,int kind,float radius,float age,float alpha,int seed,boolean far,int pass) {
        boolean world=kind==MokuVisuals.WORLD,burst=kind==MokuVisuals.BURST,ring=kind==MokuVisuals.STRIKE,launch=kind==MokuVisuals.LAUNCH;
        int count=world?(far?6:14):(far?3:7);if(pass==1)count*=2;
        float progress=MathHelper.clamp(age/18,0,1),grow=burst||ring?.25F+.75F*(1-(1-progress)*(1-progress)):1;
        for(int i=0;i<count;i++) {
            double n=noise(i,seed),angle=i*2.399963+age*(i%2==0?.008:-.006),cycle=(age*.012+n)%1;
            double distance=(world?Math.sqrt((i+.5)/count)*radius*.72:ring?radius*grow:.35+.22*Math.sin(age*.04+i));
            double x=Math.cos(angle)*distance,z=Math.sin(angle)*distance;
            double y=world?(noise(i+51,seed)-.5)*radius*.65:ring?.1+progress*.65:launch?cycle*2.7:burst?(n-.25)*radius*.8:kind==MokuVisuals.FLIGHT?-.55-cycle*.7:.2+cycle*1.8;
            float size=world?radius*(pass==0?.34F:.4F)*(float)(.8+n*.4):ring?.65F+progress*.4F:burst?radius*.55F*grow:.5F+(float)cycle*.25F;
            float envelope=world||ring||burst?1:(float)Math.sin(cycle*Math.PI);
            if(burst){x*=radius*grow;z*=radius*grow;}
            if(pass==0)billow(s,b,x,y,z,size,size*(world?.7F:1),age+i*7,alpha*envelope*.035F,seed+i,far);
            else puff(s,b,camera,x,y+(world?size*.1:0),z,size*1.6F,size*(world?1.2F:1.6F),i*71+age*(i%2==0?.25F:-.2F),alpha*envelope*(world?.45F:.6F),i);
        }
    }
    public static void projectile(MatrixStack s,IRenderTypeBuffer b,Quaternion camera,float radius,float age,float alpha,int seed,boolean far,int pass) {
        int count=far?3:7;
        for(int i=0;i<count;i++) {
            double a=i*2.39996+age*.025,x=Math.cos(a)*radius*.45,y=Math.sin(a*1.3)*radius*.35,z=Math.sin(a)*radius*.45;
            float size=radius*(.6F+.12F*(float)Math.sin(age*.04+i));
            if(pass==0)billow(s,b,x,y,z,size,size*.85F,age+i,alpha*.045F,seed+i,far);
            else puff(s,b,camera,x,y,z,size*1.7F,size*1.6F,age*.4F+i*71,alpha*.6F,i);
        }
    }
    public static void trail(MatrixStack s,IRenderTypeBuffer b,Quaternion camera,List<Vector3d> points,Vector3d origin,float radius,float age,float alpha,boolean far,int pass) {
        int stride=far?3:1;
        for(int i=0;i<points.size();i+=stride) {
            Vector3d p=points.get(i).subtract(origin);float t=i/(float)Math.max(1,points.size()),fade=1-t;
            float r=radius*(.55F+t*.55F),drift=t*t*.7F;
            if(pass==0){if(i%3==0)billow(s,b,p.x,p.y+drift,p.z,r,r*.85F,age+i,alpha*fade*.035F,i,far);}
            else puff(s,b,camera,p.x+Math.sin(age*.04-i*.6)*radius*.12,p.y+drift,p.z,r*1.5F,r*1.3F,age*.3F+i*43,alpha*fade*.6F,i);
        }
    }
}
