package net.kazi.kazimod.gengar;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.*;
public final class GengarMesh {
    public static final int PURPLE=0xa66aff,PINK=0xf187e1,DARK=0x170c2c,GOLD=0xf4d889;
    private static final class State extends RenderState {
        State(){super(null,null,null);}
        static RenderType type(boolean glow){return RenderType.create(glow?"gengar_glow":"gengar_shadow",DefaultVertexFormats.POSITION_COLOR,7,65536,false,true,
                RenderType.State.builder().setTextureState(NO_TEXTURE).setAlphaState(NO_ALPHA).setTransparencyState(glow?LIGHTNING_TRANSPARENCY:TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL).setShadeModelState(SMOOTH_SHADE).setLightmapState(NO_LIGHTMAP).setDiffuseLightingState(NO_DIFFUSE_LIGHTING).setWriteMaskState(COLOR_WRITE).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(false));}
    }
    public static final RenderType GLOW=State.type(true),SHADOW=State.type(false);
    interface Sink{void point(Vector3d p,int color,float alpha);}
    static Sink sink(MatrixStack s,IRenderTypeBuffer b,boolean glow){IVertexBuilder out=b.getBuffer(glow?GLOW:SHADOW);Matrix4f matrix=s.last().pose();return(p,color,alpha)->out.vertex(matrix,(float)p.x,(float)p.y,(float)p.z).color(color>>16&255,color>>8&255,color&255,(int)(255*Math.max(0,Math.min(1,alpha)))).endVertex();}
    static void ring(Sink s,double radius,double width,double y,int color,float alpha){for(int i=0;i<48;i++){double a=i*Math.PI/24,b=(i+1)*Math.PI/24,r=Math.max(0,radius-width);
        s.point(new Vector3d(Math.cos(a)*r,y,Math.sin(a)*r),color,alpha);s.point(new Vector3d(Math.cos(a)*radius,y,Math.sin(a)*radius),color,alpha);s.point(new Vector3d(Math.cos(b)*radius,y,Math.sin(b)*radius),color,alpha);s.point(new Vector3d(Math.cos(b)*r,y,Math.sin(b)*r),color,alpha);}}
    static void line(Sink s,Vector3d a,Vector3d b,double width,int color,float alpha){Vector3d d=b.subtract(a).normalize(),u=d.cross(new Vector3d(0,1,0)).normalize();if(u.lengthSqr()<.01)u=new Vector3d(1,0,0);Vector3d v=d.cross(u).normalize();
        for(int i=0;i<4;i++){double x=i*Math.PI/2,y=(i+1)*Math.PI/2;Vector3d p=u.scale(Math.cos(x)*width).add(v.scale(Math.sin(x)*width)),q=u.scale(Math.cos(y)*width).add(v.scale(Math.sin(y)*width));s.point(a.add(p),color,alpha);s.point(a.add(q),color,alpha);s.point(b.add(q),color,alpha);s.point(b.add(p),color,alpha);}}
    static void sphere(Sink s,double x,double y,double z,double rx,double ry,double rz,int color,float alpha){for(int row=0;row<8;row++)for(int col=0;col<16;col++)for(int k=0;k<4;k++){
        double lat=-Math.PI/2+(row+(k>=2?1:0))*Math.PI/8,lon=(col+(k==1||k==2?1:0))*Math.PI/8;
        s.point(new Vector3d(x+Math.cos(lat)*Math.cos(lon)*rx,y+Math.sin(lat)*ry,z+Math.cos(lat)*Math.sin(lon)*rz),color,alpha);
    }}
    static void ribbon(Sink s,float age,double radius,double height,int count,int color,float alpha){for(int arm=0;arm<count;arm++)for(int i=0;i<24;i++){
        double t=i/24D,n=(i+1)/24D,a=arm*Math.PI*2/count+age*.12+t*8,b=arm*Math.PI*2/count+age*.12+n*8;
        Vector3d p=new Vector3d(Math.cos(a)*radius*(1-t*.6),t*height,Math.sin(a)*radius*(1-t*.6)),q=new Vector3d(Math.cos(b)*radius*(1-n*.6),n*height,Math.sin(b)*radius*(1-n*.6));
        line(s,p,q,.025+.035*(1-t),color,alpha*(float)(1-t));
    }}
    public static void pool(MatrixStack s,IRenderTypeBuffer b,float age,float radius,float alpha){
        ring(sink(s,b,false),radius,radius,.025,DARK,.86F*alpha);Sink glow=sink(s,b,true);
        ring(glow,radius*(1+.025*Math.sin(age*.12)),.045,.032,PURPLE,.65F*alpha);
        for(int i=0;i<3;i++){s.pushPose();s.mulPose(Vector3f.YP.rotationDegrees(age*1.5F+i*120));ring(glow,radius*(.35+i*.2),.018,.04,PINK,.18F*alpha);s.popPose();}
        for(int i=0;i<8;i++){double a=i*Math.PI/4+age*.025;Vector3d p=new Vector3d(Math.cos(a)*radius*.9,.04,Math.sin(a)*radius*.9),q=new Vector3d(Math.cos(a+.15)*radius*1.12,.08+.12*Math.sin(age*.1+i),Math.sin(a+.15)*radius*1.12);line(glow,p,q,.03,PURPLE,.4F*alpha);}
    }
    public static void orb(MatrixStack s,IRenderTypeBuffer b,float age,float size){
        sphere(sink(s,b,false),0,0,0,size,size,size,DARK,.98F);Sink glow=sink(s,b,true);
        sphere(glow,0,0,0,size*1.05,size*1.05,size*1.05,PURPLE,.16F);
        for(int i=0;i<3;i++){s.pushPose();s.mulPose(Vector3f.ZP.rotationDegrees(45+i*60));s.mulPose(Vector3f.XP.rotationDegrees(age*3+i*35));ring(sink(s,b,true),size*1.12,.04,0,i==0?PINK:PURPLE,.75F);s.popPose();}
        ribbon(glow,age,size*1.3,size*1.6,4,PURPLE,.65F);
    }
    public static void ghost(MatrixStack s,IRenderTypeBuffer b,float age,float size){
        Sink glow=sink(s,b,true);sphere(sink(s,b,false),0,0,0,size,size*.8,size*.8,0x8e5eb7,.82F);sphere(glow,0,0,0,size*1.08,size*.87,size*.87,PINK,.2F);
        for(int i=0;i<3;i++){Vector3d a=new Vector3d((i-1)*size*.35,-size*.3,size*.3),end=new Vector3d(Math.sin(age*.16+i)*size*.5,-size*(1.1+i*.1),size*.65);line(glow,a,end,size*.12,PURPLE,.55F);}
        for(int side=-1;side<=1;side+=2){line(glow,new Vector3d(side*size*.55,size*.25,-size*.73),new Vector3d(side*size*.18,0,-size*.83),size*.045,GOLD,1);}
        line(glow,new Vector3d(-size*.32,-size*.3,-size*.74),new Vector3d(size*.32,-size*.3,-size*.74),size*.06,0xffefce,.8F);
    }
    private static void hands(MatrixStack s,IRenderTypeBuffer b,float age,float life,float size){
        float phase=Math.min(1,age/8),spread=(1-phase)*2+.6F;Sink glow=sink(s,b,true);
        for(int side=-1;side<=1;side+=2){double x=side*spread*size,y=size*.65;sphere(sink(s,b,false),x,y,0,.25*size,.45*size,.4*size,0x75459d,.8F);
            for(int f=0;f<3;f++){Vector3d a=new Vector3d(x,y+.3*size,(f-1)*.28*size),p=new Vector3d(x-side*.22*size,y+.65*size,(f-1)*.32*size),tip=new Vector3d(x-side*.5*size,y+.5*size,(f-1)*.3*size);line(glow,a,p,.07*size,PURPLE,.7F);line(glow,p,tip,.045*size,PINK,.8F);}
        }
    }
    public static void effect(MatrixStack s,IRenderTypeBuffer b,int kind,float age,float life,float size){
        float t=Math.max(0,Math.min(1,age/life)),fade=Math.min(1,(life-age)/8);Sink glow=sink(s,b,true);
        switch(kind){
            case GengarEffect.PORTAL:pool(s,b,age,size*(.7F+.3F*(float)Math.sin(t*Math.PI)),fade);ribbon(glow,age,size,2.5,5,PURPLE,.45F*fade);break;
            case GengarEffect.BLAST:
                float pulse=(float)Math.sin(t*Math.PI);sphere(sink(s,b,false),0,.2,0,size*(1-t*.75),size*.75*(1-t),size*(1-t*.75),DARK,(1-t)*.8F);
                ring(glow,size*(.2+t),.15*(1-t)+.015,.1,PINK,(1-t)*.8F);ribbon(glow,-age,size*(1-t*.6),size*.8,6,PURPLE,pulse*.8F);break;
            case GengarEffect.HAUNT:for(int i=0;i<3;i++){double a=age*.06+i*Math.PI*2/3;s.pushPose();s.translate(Math.cos(a)*.7,size*.65+Math.sin(a)*.2,Math.sin(a)*.7);s.mulPose(Vector3f.YP.rotation((float)-a));ghost(s,b,age+i,.22F);s.popPose();}break;
            case GengarEffect.HANDS:hands(s,b,age,life,size*.65F);break;
            case GengarEffect.TAG:
                pool(s,b,age,size,.3F*fade);ring(glow,size,.07,.06,GOLD,.6F*fade);for(int i=0;i<8;i++){double a=i*Math.PI/4;line(glow,new Vector3d(Math.cos(a)*size,.05,Math.sin(a)*size),new Vector3d(Math.cos(a)*size,.6,Math.sin(a)*size),.025,PINK,.4F*fade);}break;
            case GengarEffect.BITE:
                float open=(float)Math.sin(Math.PI*Math.min(1,age/12));for(int row=-1;row<=1;row+=2){s.pushPose();s.translate(0,size*.6+row*open*.6,0);ring(sink(s,b,true),1.2,.1,0,PINK,fade);for(int i=0;i<10;i++){double a=i*Math.PI/5;line(sink(s,b,true),new Vector3d(Math.cos(a),0,Math.sin(a)),new Vector3d(Math.cos(a)*.85,-row*.35,Math.sin(a)*.85),.07,GOLD,fade);}s.popPose();}break;
            case GengarEffect.HEAL:ribbon(glow,age,.8,2.5,4,0xd3a8ff,fade*.7F);ring(glow,.9,.035,.1,GOLD,fade*.6F);break;
            case GengarEffect.CURSE:
                pool(s,b,age,size,.25F);ring(glow,size,.08,.06,PINK,.65F);ring(glow,size*.82,.025,.08,PURPLE,.7F);
                for(int i=0;i<12;i++){double a=i*Math.PI/6;Vector3d p=new Vector3d(Math.cos(a)*size*.88,.09,Math.sin(a)*size*.88),q=new Vector3d(Math.cos(a)*size*.97,.09,Math.sin(a)*size*.97);line(glow,p,q,.035,GOLD,.8F);}
                for(int i=0;i<6;i++){double a=i*Math.PI/3;line(glow,new Vector3d(Math.cos(a)*size,.1,Math.sin(a)*size),new Vector3d(Math.cos(a)*size,.5+t*2,Math.sin(a)*size),.025,PURPLE,.25F+t*.3F);}
                s.pushPose();s.translate(0,3+t,0);s.scale(1,.45F,1);orb(s,b,age,.5F+t*.5F);s.popPose();break;
            case GengarEffect.SWEEP:for(int i=0;i<3;i++){s.pushPose();s.mulPose(Vector3f.ZP.rotationDegrees(20+i*15));ring(sink(s,b,true),size*t,.055,.3+i*.18,PINK,fade*(1-t));s.popPose();}break;
        }
    }
    private GengarMesh(){}
}
