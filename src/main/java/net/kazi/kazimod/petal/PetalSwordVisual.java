package net.kazi.kazimod.petal;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;

/** All swords share one untextured, full-bright batch; petals are render geometry, not entities. */
public final class PetalSwordVisual {
    public static final int BANKAI_VISUAL_LIFE = 82;
    public static final int ARENA_ROWS = 4, SWORDS_PER_ROW = 64;
    private static final int BLADE_SECTIONS = 40;
    private static final double[] FRACTIONS = {0,.16,.72,1};

    private static float smooth(float t) { return HakuteikenVisual.smooth(t); }
    private static void vertex(IVertexBuilder v, Matrix4f m, double x, double y, double z,
                               float r, float g, float b, float a) {
        v.vertex(m, (float)x, (float)y, (float)z).color(r,g,b,a).endVertex();
    }
    private static void panel(IVertexBuilder v, Matrix4f m, double left, double right,
            double y, double nextLeft, double nextRight, double nextY, double z,
            float r, float g, float b, float alpha) {
        vertex(v,m,left,y,z,r,g,b,alpha);vertex(v,m,right,y,z,r,g,b,alpha);
        vertex(v,m,nextRight,nextY,z,r,g,b,alpha);vertex(v,m,nextLeft,nextY,z,r,g,b,alpha);
    }
    private static void glow(IVertexBuilder v, Matrix4f m, double x, double from, double to,
                              double width, double z, float alpha) {
        for(int side:new int[]{-1,1}) {
            vertex(v,m,x,from,z,1,.64F,.86F,alpha);vertex(v,m,x,to,z,1,.64F,.86F,alpha);
            vertex(v,m,x+side*width,to,z,1,.18F,.55F,0);vertex(v,m,x+side*width,from,z,1,.18F,.55F,0);
        }
    }

    /** Tip-down Senkei sword, measured in blocks, with a narrow white core and pink bloom. */
    public static void sword(MatrixStack s, IRenderTypeBuffer buffers, float alpha, boolean bloom) {
        if(alpha<=0)return;
        IVertexBuilder v=buffers.getBuffer(PetalMesh.TYPE);Matrix4f m=s.last().pose();
        for(int face:new int[]{-1,1}) {
            double z=face*.035;
            panel(v,m,.025,.025,0,-.085,.085,.3,z,1,.64F,.84F,alpha);
            panel(v,m,-.085,.085,.3,-.085,.085,2.7,z,1,.64F,.84F,alpha);
            panel(v,m,-.025,.025,.25,-.025,.025,2.7,z+face*.002,1,.96F,1,alpha);
            panel(v,m,-.36,.36,2.69,-.14,.14,2.78,z,1,.86F,.96F,alpha);
            panel(v,m,-.055,.055,2.76,-.055,.055,3.3,z,1,.77F,.92F,alpha);
        }
        if(bloom) {
            glow(v,m,0,.12,2.76,.3,0,alpha*.6F);
            glow(v,m,0,2.69,2.79,.62,0,alpha*.8F);
            glow(v,m,0,2.76,3.34,.19,0,alpha*.55F);
            s.pushPose();s.mulPose(Vector3f.YP.rotationDegrees(90));
            glow(v,s.last().pose(),0,.15,3.3,.23,0,alpha*.3F);s.popPose();
        }
    }

    public static void arena(MatrixStack s, IRenderTypeBuffer buffers, float age, float life,
                              float radius, int quality) {
        float fade=smooth(age/14)*(1-smooth((age-life+12)/12));
        for(int row=0;row<ARENA_ROWS;row++)for(int i=0;i<SWORDS_PER_ROW;i++) {
            double a=(i+row*.5)*Math.PI*2/SWORDS_PER_ROW;
            s.pushPose();s.translate(Math.cos(a)*radius,.55+row*4.35+Math.sin(age*.025+i*.4)*.035,Math.sin(a)*radius);
            s.mulPose(Vector3f.YP.rotationDegrees((float)(90-Math.toDegrees(a))));
            sword(s,buffers,fade,quality>100);s.popPose();
        }
        IVertexBuilder v=buffers.getBuffer(PetalMesh.TYPE);Matrix4f m=s.last().pose();
        for(double y:new double[]{SenkeiBoundary.FLOOR+.2,SenkeiBoundary.CEILING-.2}){
            for(int i=0;i<96;i++){
                double a=i*Math.PI/48,next=(i+1)*Math.PI/48;
                vertex(v,m,0,y,0,1,.42F,.73F,fade*.025F);
                vertex(v,m,Math.cos(a)*radius,y,Math.sin(a)*radius,1,.6F,.86F,fade*.11F);
                vertex(v,m,Math.cos(next)*radius,y,Math.sin(next)*radius,1,.6F,.86F,fade*.11F);
                vertex(v,m,0,y,0,1,.42F,.73F,fade*.025F);
            }
            for(int ring=1;ring<=5;ring++){
                s.pushPose();s.translate(0,y,0);PetalMesh.ring(s,buffers,radius*ring/5,fade*.55F,age);s.popPose();
            }
        }
        for(int i=0;i<72;i++) {
            double a=i*2.399+age*.012,r=radius*(.82+.16*Math.sin(i*8));
            PetalMesh.petal(v,m,Math.cos(a)*r,.4+(i%15)*.65+Math.sin(age*.04+i)*.3,Math.sin(a)*r,.065,a+age*.05,fade*.65F,false);
        }
    }

    public static double dissolveAt(int blade,int section,int column) {
        double t=(section+.5)/BLADE_SECTIONS;
        return 34+(1-t)*15+Math.sin(blade*7+section*2.3+column*5)*1.2+blade*.12;
    }
    private static double left(double t) { return -.44+.32*t*t+Math.max(0,(t-.86)/.14)*.88; }
    private static double right(double t) { return .44+.32*t*t; }

    public static void bankai(MatrixStack s, IRenderTypeBuffer buffers, float age, float life, int quality) {
        if(age<=0||age>=life)return;
        IVertexBuilder v=buffers.getBuffer(PetalMesh.TYPE);Matrix4f m=s.last().pose();
        float fade=1-smooth((age-life+16)/16);
        // Two parallel rows of bare katana blades. Clip the emerging surface at ground height.
        for(int side:new int[]{-1,1})for(int blade=0;blade<12;blade++) {
            double x=side*3.5,z=-16.5+blade*3,height=11.5+(blade%3)*.55;
            double rise=smooth((age-7-blade*.45F)/18),offset=-height*(1-rise);
            for(int section=0;section<BLADE_SECTIONS;section++) {
                double t=section/(double)BLADE_SECTIONS,next=(section+1D)/BLADE_SECTIONS;
                double y=height*t+offset,ny=height*next+offset;
                if(ny<=0)continue;
                if(y<0){t=-offset/height;y=0;}
                for(int column=0;column<3;column++) {
                    double l=left(t)+(right(t)-left(t))*FRACTIONS[column],r=left(t)+(right(t)-left(t))*FRACTIONS[column+1];
                    double nl=left(next)+(right(next)-left(next))*FRACTIONS[column],nr=left(next)+(right(next)-left(next))*FRACTIONS[column+1];
                    double elapsed=age-dissolveAt(blade,section,column);
                    if(elapsed<0) {
                        float pink=smooth((age-29-blade*.12F)/5),shade=column==0?.97F:column==1?.69F:.29F;
                        float red=MathHelper.lerp(pink,shade,1),green=MathHelper.lerp(pink,shade+.015F,.9F),blue=MathHelper.lerp(pink,shade+.025F,.96F);
                        for(int face:new int[]{-1,1})panel(v,m,x+l,x+r,y,x+nl,x+nr,ny,z+face*.08,red,green,blue,fade);
                        if(column==1&&quality>100&&pink>0)glow(v,m,x+(l+r)*.5,y,ny,.72,z,fade*pink*.45F);
                    } else if(quality>100||(section+column)%2==0) {
                        double seed=blade*31+section*2.399+column*7+side*3;
                        double angle=elapsed*.022,shrink=Math.max(.45,1-elapsed*.012);
                        double px=x+(l+r+nl+nr)*.25,pz=z+Math.sin(seed)*elapsed*.07;
                        double tx=(px*Math.cos(angle)-pz*Math.sin(angle))*shrink;
                        double tz=(px*Math.sin(angle)+pz*Math.cos(angle))*shrink;
                        double py=(y+ny)*.5+elapsed*.06+Math.sin(seed+elapsed*.16)*Math.min(.5,elapsed*.025);
                        float alpha=fade*smooth((float)elapsed/2);
                        PetalMesh.petal(v,m,tx,py,tz,.09+(column==1?.04:0),seed+elapsed*.18,alpha,false);
                        if(quality>180&&column==1)glow(v,m,tx,py-.07,py+.07,.18,tz,alpha*.35F);
                    }
                }
            }
            if(age<34) groundRing(v,m,x,z,.7+rise*.3,(float)(Math.sin(Math.PI*rise)*.75),age);
        }
        if(age<21)groundRing(v,m,.35,-.6,.2+age*.035,smooth((age-4)/4)*(1-smooth((age-15)/6)),age);
    }

    private static void groundRing(IVertexBuilder v,Matrix4f m,double x,double z,double radius,float alpha,float age) {
        if(alpha<=0)return;
        for(int i=0;i<32;i++) {
            double a=i*Math.PI/16,b=(i+1)*Math.PI/16;
            for(int side:new int[]{-1,1}) {
                double outer=radius+side*.13;
                vertex(v,m,x+Math.cos(a)*radius,.015,z+Math.sin(a)*radius,1,.87F,.96F,alpha);
                vertex(v,m,x+Math.cos(b)*radius,.015,z+Math.sin(b)*radius,1,.87F,.96F,alpha);
                vertex(v,m,x+Math.cos(b)*outer,.015,z+Math.sin(b)*outer,1,.56F,.8F,0);
                vertex(v,m,x+Math.cos(a)*outer,.015,z+Math.sin(a)*outer,1,.56F,.8F,0);
            }
        }
    }
    private PetalSwordVisual() {}
}
