package net.kazi.kazimod.doku;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.*;
import java.util.List;

/** Bounded soft volume slices, advected independently to avoid a rotating flat cloud. */
public final class DokuGasMesh {
    public static final ResourceLocation TEXTURE=new ResourceLocation("kazimod","textures/vfx/doku_vapor.png");
    private static final class State extends RenderState {
        State(){super(null,null,null);}
        static RenderType gas(){return RenderType.create("doku_vapor",DefaultVertexFormats.POSITION_COLOR_TEX,7,32768,false,true,
                RenderType.State.builder().setTextureState(new TextureState(TEXTURE,true,false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));}
        static RenderType liquid(){return RenderType.create("doku_liquid",DefaultVertexFormats.POSITION_COLOR,7,32768,false,true,
                RenderType.State.builder().setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL)
                        .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));}
    }
    private static final RenderType GAS=State.gas(),LIQUID=State.liquid();
    private static double noise(int i,int seed){double x=Math.sin(i*127.1+seed*31.73)*43758.5453;return x-Math.floor(x);}
    public static void volume(MatrixStack stack,IRenderTypeBuffer buffers,Quaternion camera,int kind,float age,float radius,boolean red,float opacity,int seed,boolean far){
        IVertexBuilder out=buffers.getBuffer(GAS);
        int count=far?16:44;
        boolean narrow=kind==DokuGasEntity.DEMON||kind==DokuGasEntity.PROJECTILE||kind==DokuGasEntity.ROAD;
        double grow=kind==DokuGasEntity.BURST?Math.min(1,.15+age/15):Math.min(1,.25+age/20);
        double height=kind==DokuGasEntity.DEMON?radius*1.7:kind==DokuGasEntity.PROJECTILE?radius:Math.min(5,radius*.42);
        for(int i=0;i<count;i++){
            double cycle=(age*.008+noise(i,seed)*2)%1;
            double theta=noise(i+41,seed)*Math.PI*2+age*(i%2==0?.003:-.002);
            double distance=radius*Math.sqrt(noise(i+83,seed))*(.25+.75*cycle)*grow*(narrow?.42:.85);
            double x=Math.cos(theta)*distance+Math.sin(age*.025+i)*radius*.035;
            double z=Math.sin(theta)*distance+Math.cos(age*.019+i)*radius*.035;
            double y=narrow?cycle*height:height*(.1+noise(i+17,seed)*.45+cycle*.4);
            if(kind==DokuGasEntity.PROJECTILE||kind==DokuGasEntity.ROAD)y-=height*.5;
            double size=radius*(narrow?.65:.27)*(1+cycle*.65)*( .8+noise(i+5,seed)*.7);
            float alpha=(float)(Math.sin(cycle*Math.PI)*opacity*(far?.72:.42));
            int color=red?(i%4==0?0xff7265:i%3==0?0x801530:0xd63553):(i%4==0?0xd6a3f2:i%3==0?0x391c5e:0x8d49bd);
            puff(stack,out,camera,x,y,z,size,size*(.75+noise(i+7,seed)*.55),age*.2+i*73,color,alpha,i%4);
        }
    }
    public static void wisps(MatrixStack stack,IRenderTypeBuffer buffers,Quaternion camera,List<Vector3d> points,float age,float radius,boolean red,float alpha,boolean far){
        IVertexBuilder out=buffers.getBuffer(GAS);
        int step=far?4:2;
        for(int i=0;i<points.size();i+=step){
            Vector3d p=points.get(i);float fade=1-i/(float)points.size();
            double size=radius*(.7+i*.025);
            puff(stack,out,camera,p.x,p.y+.15+Math.sin(age*.06-i*.5)*.15,p.z,size,size*.8,age*.4+i*39,
                    red?0xd4476b:0xb57de4,alpha*fade*.3F,i%4);
        }
    }
    private static void puff(MatrixStack stack,IVertexBuilder out,Quaternion camera,double x,double y,double z,double w,double h,double angle,int color,float alpha,int tile){
        stack.pushPose();stack.translate(x,y,z);stack.mulPose(camera);stack.mulPose(Vector3f.ZP.rotationDegrees((float)angle));
        float u0=(tile+.002F)/4,u1=(tile+.998F)/4;
        vertex(out,stack,-w,-h,0,color,alpha,u0,.998F);vertex(out,stack,w,-h,0,color,alpha,u1,.998F);
        vertex(out,stack,w,h,0,color,alpha,u1,.002F);vertex(out,stack,-w,h,0,color,alpha,u0,.002F);stack.popPose();
    }
    private static void vertex(IVertexBuilder out,MatrixStack stack,double x,double y,double z,int color,float alpha,float u,float v){
        out.vertex(stack.last().pose(),(float)x,(float)y,(float)z).color(color>>16&255,color>>8&255,color&255,(int)(MathHelper.clamp(alpha,0,1)*255)).uv(u,v).endVertex();
    }
    public static void road(MatrixStack stack,IRenderTypeBuffer buffers,List<Vector3d> points,float radius,float age,boolean red,float alpha){
        if(points.size()<2)return;
        IVertexBuilder out=buffers.getBuffer(LIQUID);
        Vector3d[][] rings=new Vector3d[points.size()][12];
        for(int i=0;i<points.size();i++){
            Vector3d tangent=points.get(Math.max(0,i-1)).subtract(points.get(Math.min(points.size()-1,i+1))).normalize();
            Vector3d side=tangent.cross(new Vector3d(0,1,0));
            if(side.lengthSqr()<.001)side=new Vector3d(1,0,0);else side=side.normalize();
            Vector3d up=side.cross(tangent).normalize();
            double taper=Math.pow(1-i/(double)points.size(),.7),r=radius*.4*taper;
            for(int j=0;j<12;j++){double a=j*Math.PI/6;rings[i][j]=points.get(i).add(side.scale(Math.cos(a)*r)).add(up.scale(Math.sin(a)*r));}
        }
        for(int i=0;i<rings.length-1;i++)for(int j=0;j<12;j++){
            float fade=(1-i/(float)rings.length)*alpha*.55F;
            float shimmer=(float)(.5+.5*Math.sin(j*Math.PI/6+age*.08-i*.4));
            int r=red?(int)(135+90*shimmer):(int)(65+95*shimmer),g=(int)(24+35*shimmer),b=red?(int)(50+30*shimmer):(int)(125+95*shimmer);
            for(Vector3d p:new Vector3d[]{rings[i][j],rings[i+1][j],rings[i+1][(j+1)%12],rings[i][(j+1)%12]})
                out.vertex(stack.last().pose(),(float)p.x,(float)p.y,(float)p.z).color(r,g,b,(int)(fade*255)).endVertex();
        }
    }
}
