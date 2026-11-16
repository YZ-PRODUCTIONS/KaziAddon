package net.kazi.kazimod.decay;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.*;

public final class DecayMesh extends RenderState {
    public static final RenderType TYPE=RenderType.create("sabi_decay",DefaultVertexFormats.POSITION_COLOR,7,262144,false,true,RenderType.State.builder().setTextureState(NO_TEXTURE).setAlphaState(NO_ALPHA).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setShadeModelState(SMOOTH_SHADE).setCullState(NO_CULL).setLightmapState(NO_LIGHTMAP).setDiffuseLightingState(NO_DIFFUSE_LIGHTING).setWriteMaskState(COLOR_WRITE).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(false));
    private DecayMesh(){super(null,null,null);}
    private static void v(IVertexBuilder b,Matrix4f m,Vector3d p,float shade,float alpha){b.vertex(m,(float)p.x,(float)p.y,(float)p.z).color(shade,shade*.96F,shade*.95F,alpha).endVertex();}
    public static void line(IVertexBuilder b,Matrix4f m,Vector3d a,Vector3d c,Vector3d width,float shade,float alpha){v(b,m,a.subtract(width),shade,alpha);v(b,m,c.subtract(width),shade,alpha);v(b,m,c.add(width),shade,alpha);v(b,m,a.add(width),shade,alpha);}
    private static void fragment(IVertexBuilder b,Matrix4f m,Vector3d p,double size,double rotation,float alpha){Vector3d u=new Vector3d(Math.cos(rotation)*size,Math.sin(rotation)*size,0),w=new Vector3d(-Math.sin(rotation)*size*.65,Math.cos(rotation)*size*.65,size*.5);v(b,m,p.subtract(u),.28F,alpha);v(b,m,p.add(w),.75F,alpha);v(b,m,p.add(u),.9F,alpha);v(b,m,p.subtract(w),.42F,alpha);}
    private static double noise(int seed){double n=Math.sin(seed*127.1+311.7)*43758.5453;return n-Math.floor(n);}
    public static void render(DecayField f,float partial,MatrixStack s,IRenderTypeBuffer buffers,int quality){
        float age=f.age(partial),fade=Math.min(1,Math.min(age/3,(f.life()-age)/14));if(fade<=0)return;IVertexBuilder b=buffers.getBuffer(TYPE);Matrix4f m=s.last().pose();int kind=f.kind();
        if(kind==5){catastrophe(f,partial,s,buffers,quality);return;}
        if(kind==1){int[] nodes=f.nodes();double wave=f.wave(partial);int step=Math.max(1,nodes.length/4/(quality*3));java.util.List<Vector3d> edge=new java.util.ArrayList<>();
            for(int i=0;i<nodes.length;i+=4*step){double x=nodes[i]-f.getX(),y=nodes[i+1]+1.025-f.getY(),z=nodes[i+2]-f.getZ(),d=Math.sqrt(x*x+z*z);int seed=nodes[i]*157+nodes[i+2]*89;
                if(age<f.warning()||d>wave){if(d>2.5)continue;float pulse=(float)(.3+.25*Math.sin(age*.3));crack(b,m,x,y,z,seed,1,pulse);continue;}
                double elapsed=(wave-d)/.65;crack(b,m,x,y,z,seed,1,fade*(elapsed<8?1:.8F));if(d>wave-2.5)edge.add(new Vector3d(x+.5,y,z+.5));
                if(elapsed<20&&Math.floorMod(seed,3)==0){double lift=Math.sin(Math.min(1,elapsed/20)*Math.PI)*(kind==5?2.3:.9);fragment(b,m,new Vector3d(x+.5,y+lift,z+.5),.10+noise(seed)*.22,age*.07+seed,fade*(float)(1-elapsed/24));}
            }
            if(age>=f.warning()&&!edge.isEmpty())for(int i=0;i<quality;i++){double a=i*2.39996;double elapsed=Math.max(0,age-f.warning());double h=(.25+noise(i+15)*2.8)*(kind==5?1.4:.7);Vector3d p=edge.get((i*31)%edge.size()).add(noise(i+65)-.5,h+Math.sin(age*.06+i)*.2,noise(i+90)-.5);
                // Overlapping soft-edged ash sheets, with transparent edges instead of opaque cubes.
                double sz=(.65+noise(i+30)*1.05)*(kind==5?1.4:1);Vector3d u=new Vector3d(Math.cos(a)*sz,0,Math.sin(a)*sz),up=new Vector3d(0,sz*.9,0);float alpha=fade*.6F*(float)Math.min(1,elapsed/7);
                for(int k=0;k<8;k++){double aa=k*Math.PI*.25,bb=(k+1)*Math.PI*.25;float shade=(float)(.48+noise(i)*.22);v(b,m,p,shade,alpha);v(b,m,p.add(u.scale(Math.cos(aa))).add(up.scale(Math.sin(aa))),.75F,0);v(b,m,p.add(u.scale(Math.cos(bb))).add(up.scale(Math.sin(bb))),.75F,0);v(b,m,p,shade,alpha);}
                if(i%3==0)fragment(b,m,p.add(0,noise(i+4)*1.7,0),.035+noise(i+9)*.07,age*.12+i,fade*.7F);
            }
        }else if(kind==7){Vector3d end=f.end().subtract(f.position());Vector3d last=Vector3d.ZERO;for(int i=1;i<=24;i++){double t=i/24D;Vector3d at=end.scale(t).add(Math.sin(i*3)*.18*Math.sin(t*Math.PI),Math.cos(i*2)*.15*Math.sin(t*Math.PI),Math.sin(i)*.15);line(b,m,last,at,new Vector3d(.025,0,.025),.15F,fade);line(b,m,last,at,new Vector3d(.006,.008,.006),.87F,fade*.8F);last=at;}}
        else {double h=f.target()==null?1.8:f.target().getBbHeight(),width=f.target()==null?.65:Math.min(3,f.target().getBbWidth()*.55);int count=kind==3?quality/2:quality/3;
            for(int i=0;i<count;i++){double a=i*2.39996+age*.015,y=noise(i+5)*h,rad=width+(kind==6?age*.004:Math.sin(age*.15+i)*.08);Vector3d p=new Vector3d(Math.cos(a)*rad,y,Math.sin(a)*rad);if(kind==3&&f.owner()!=null&&p.normalize().dot(f.owner().getLookAngle())<0)continue;
                if(i%3==0){Vector3d q=p.add(Math.cos(a+.8)*.16,.12,Math.sin(a+.8)*.16);line(b,m,p,q,new Vector3d(.012,0,.012),.12F,fade);}
                fragment(b,m,p.add(0,Math.max(0,age-8)*.013,0),kind==3?.055:.025,age*.07+i,fade*.75F);
            }
            if(kind==4){double r=.65;for(int i=0;i<28;i++){double a=i*Math.PI/14,c=(i+1)*Math.PI/14;line(b,m,new Vector3d(Math.cos(a)*r,.035,Math.sin(a)*r),new Vector3d(Math.cos(c)*r,.035,Math.sin(c)*r),new Vector3d(.025,0,.025),.8F,fade);}}
        }
    }
    private static void catastrophe(DecayField f,float partial,MatrixStack s,IRenderTypeBuffer buffers,int quality){
        IVertexBuilder b=buffers.getBuffer(TYPE);Matrix4f m=s.last().pose();float age=f.age(partial);
        float warning=Math.min(1,age/12),waveFade=(float)Math.max(0,Math.min(1,(f.warning()+f.size()/.65+18-age)/18));
        for(int i=0;i<192;i++){
            double a=i*Math.PI/96,next=(i+1)*Math.PI/96;
            double radius=age<f.warning()?f.size():f.wave(partial);
            if(waveFade>0)line(b,m,new Vector3d(Math.cos(a)*radius,.04,Math.sin(a)*radius),new Vector3d(Math.cos(next)*radius,.04,Math.sin(next)*radius),
                    new Vector3d(Math.cos(a)*.09,0,Math.sin(a)*.09),.72F,(age<f.warning()?.14F:.45F)*warning*waveFade);
            double safe=CatastropheTerrain.SAFE_RADIUS;
            if(age<f.warning()+20)line(b,m,new Vector3d(Math.cos(a)*safe,.05,Math.sin(a)*safe),new Vector3d(Math.cos(next)*safe,.05,Math.sin(next)*safe),new Vector3d(.025,0,.025),.8F,warning*.55F);
        }
        int[] dust=f.dust();int entries=dust.length/5,stride=Math.max(1,(int)Math.ceil(entries/(double)quality));
        for(int i=0;i+4<dust.length;i+=5*stride){
            float elapsed=age-dust[i+4];if(elapsed<0||elapsed>=CatastropheTerrain.DUST_LIFE)continue;
            int seed=dust[i]*157+dust[i+2]*89,color=dust[i+3];
            float alpha=(1-elapsed/CatastropheTerrain.DUST_LIFE)*.55F;
            Vector3d origin=new Vector3d(dust[i]+.5-f.getX(),dust[i+1]+.6-f.getY(),dust[i+2]+.5-f.getZ());
            // Coarse chips shrink into expanding, soft ash sheets at real removed-block positions.
            for(int chip=0;chip<4;chip++){
                double a=seed+chip*2.399,r=.15+elapsed*.025;
                Vector3d p=origin.add(Math.cos(a)*r,elapsed*.055+noise(seed+chip)*.4,Math.sin(a)*r);
                fragment(b,m,p,Math.max(.015,.19*(1-elapsed/12)),a+elapsed*.14,alpha);
            }
            for(int puff=0;puff<2;puff++){
                double a=seed+puff*Math.PI*.5,sz=.4+elapsed*.065;
                Vector3d p=origin.add(Math.sin(a)*elapsed*.025,elapsed*.065+puff*.3,Math.cos(a)*elapsed*.025);
                Vector3d u=new Vector3d(Math.cos(a)*sz,0,Math.sin(a)*sz),up=new Vector3d(0,sz*.75,0);
                for(int k=0;k<8;k++){
                    double aa=k*Math.PI/4,bb=(k+1)*Math.PI/4;
                    dustVertex(b,m,p,color,alpha);dustVertex(b,m,p.add(u.scale(Math.cos(aa))).add(up.scale(Math.sin(aa))),color,0);
                    dustVertex(b,m,p.add(u.scale(Math.cos(bb))).add(up.scale(Math.sin(bb))),color,0);dustVertex(b,m,p,color,alpha);
                }
            }
        }
    }
    private static void dustVertex(IVertexBuilder b,Matrix4f m,Vector3d p,int color,float alpha){
        float r=.28F+((color>>16)&255)/255F*.48F,g=.28F+((color>>8)&255)/255F*.48F,blue=.28F+(color&255)/255F*.48F;
        b.vertex(m,(float)p.x,(float)p.y,(float)p.z).color(r,g,blue,alpha).endVertex();
    }
    private static void crack(IVertexBuilder b,Matrix4f m,double x,double y,double z,int seed,double size,float alpha){Vector3d p=new Vector3d(x,y,z+.2+noise(seed)*.6);for(int j=0;j<4;j++){Vector3d q=new Vector3d(x+(j+1)*.25*size,y,z+(.2+noise(j==3?seed+157:seed+j+1)*.6)*size);Vector3d w=q.subtract(p).cross(new Vector3d(0,1,0)).normalize().scale(.02+noise(seed+8)*.035);line(b,m,p,q,w,.045F,alpha);line(b,m,p.add(0,.001,0),q.add(0,.001,0),w.scale(.17),.8F,alpha*.5F);if(j==1){line(b,m,q,new Vector3d(x+.2+noise(seed+200)*.6,y,z),new Vector3d(.015,0,0),.06F,alpha);line(b,m,q,new Vector3d(x+.2+noise(seed+289)*.6,y,z+1),new Vector3d(.015,0,0),.06F,alpha);}p=q;}}
}
