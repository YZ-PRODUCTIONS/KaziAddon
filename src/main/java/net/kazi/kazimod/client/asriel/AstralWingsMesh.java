package net.kazi.kazimod.client.asriel;

import java.util.ArrayList;
import java.util.List;
import net.kazi.kazimod.renderers.entities.asriel.AsrielFlowPattern;

/** Cached concave wing surfaces with animated rainbow color and a gentle, independent wing beat. */
public final class AstralWingsMesh {
    public interface Sink { void vertex(float x,float y,float z,int rgb,float alpha); }
    // Trace of the supplied Angel of Death silhouette (right wing, mirrored at render time).
    // Coordinates preserve the original raised outer hook and unequal, drooping lower lobes.
    private static final float SCALE=.0091F;
    private static final float[][] TIPS={point(588,330),point(546,356),point(481,376),
            point(429,338),point(401,311)};
    private static final float[][] OUTLINE=outline();
    private static final float[][] TRIANGLES=triangulate();
    private static final float[][] SURFACES={surface(3),surface(2),surface(1)};
    private static final float[] STARS=stars();
    private static final float COLOR_BRIGHTNESS=.85F;
    private AstralWingsMesh() { }

    public static void render(Sink out,float age,boolean flying,int detail) {
        if(!Float.isFinite(age)) return;
        detail=Math.max(0,Math.min(2,detail));
        float beat=(float)Math.sin(age*(flying?.13F:.055F));
        float hinge=.20F+beat*(flying?.23F:.065F);
        float[] surface=SURFACES[detail];
        for(int side=-1;side<=1;side+=2) {
            // Cached triangle subdivisions prevent concave feather notches from being filled in.
            for(int i=0;i<surface.length;i+=6) {
                for(int vertex=0;vertex<4;vertex++) {
                    int index=i+Math.min(vertex,2)*2;
                    float x=surface[index],y=surface[index+1];
                    emit(out,side,x,y,0,age,hinge,color(x,y,age,0),.78F);
                }
            }
            for(int i=0;i<OUTLINE.length;i++) {
                float[] a=OUTLINE[i],b=OUTLINE[(i+1)%OUTLINE.length];
                if(detail==0) line(out,side,a[0],a[1],b[0],b[1],.045F,age,hinge,.09F,0);
                line(out,side,a[0],a[1],b[0],b[1],.012F,age,hinge,.92F,.75F);
            }
            // Ribs descend from the outer shoulder, following the reference's hanging lobes.
            for(int rib=0;rib<TIPS.length;rib++) {
                float rootX=(535-366)*SCALE,rootY=(214-108)*SCALE;
                float px=rootX,py=rootY;
                int steps=detail==2?4:8;
                for(int j=1;j<=steps;j++) {
                    float t=j/(float)steps,u=1-t;
                    float x=u*u*rootX+2*u*t*((rootX+TIPS[rib][0])*.5F)+t*t*TIPS[rib][0];
                    float y=u*u*rootY+2*u*t*.35F+t*t*TIPS[rib][1];
                    line(out,side,px,py,x,y,.007F,age,hinge,.16F,.55F);px=x;py=y;
                }
            }
            if(detail<2) for(int i=0;i<STARS.length;i+=2) {
                if(detail==1 && i%4!=0)continue;
                float x=STARS[i],y=STARS[i+1];
                float sparkle=.5F+.5F*(float)Math.sin(age*.075F+i*1.71F),size=.012F+.014F*sparkle;
                line(out,side,x-size,y,x+size,y,.005F,age,hinge,.4F+sparkle*.4F,1);
                line(out,side,x,y-size,x,y+size,.005F,age,hinge,.4F+sparkle*.4F,1);
            }
        }
    }
    private static void emit(Sink out,int side,float x,float y,float depth,float age,float hinge,int rgb,float alpha) {
        float curl=(float)Math.sin(age*.07F+x*1.8F)*.045F*x;
        float bend=hinge*x/3.6F;
        out.vertex(side*x*(float)Math.cos(bend),y+(float)Math.sin(age*.055F+x)*.024F*x,
                .06F+x*(float)Math.sin(bend)+curl+depth,rgb,alpha);
    }
    private static void line(Sink out,int side,float x,float y,float xx,float yy,float width,float age,float hinge,float alpha,float white) {
        float dx=xx-x,dy=yy-y,len=(float)Math.sqrt(dx*dx+dy*dy);if(len<.00001F)return;
        float nx=-dy/len*width,ny=dx/len*width;
        emit(out,side,x-nx,y-ny,-.003F,age,hinge,color(x,y,age,white),alpha);
        emit(out,side,xx-nx,yy-ny,-.003F,age,hinge,color(xx,yy,age,white),alpha);
        emit(out,side,xx+nx,yy+ny,-.003F,age,hinge,color(xx,yy,age,white),alpha);
        emit(out,side,x+nx,y+ny,-.003F,age,hinge,color(x,y,age,white),alpha);
    }
    private static int color(float x,float y,float age,float white) {
        float phase=AsrielFlowPattern.phase(age);
        float flow=phase+x*.115F+y*.075F;
        float hue=flow-(float)Math.floor(flow);
        float noise=(float)Math.sin(x*4+y*3.5F+phase*Math.PI*2)*.035F;
        float brightness=(.76F+noise)*AsrielFlowPattern.shade(flow,y);
        float h=hue*6,f=AsrielFlowPattern.smooth(h-(int)h),p=brightness*.16F,q=brightness*(1-.84F*f),t=brightness*(1-.84F*(1-f));
        float r,g,b;
        switch((int)h) {
            case 0:r=brightness;g=t;b=p;break;case 1:r=q;g=brightness;b=p;break;
            case 2:r=p;g=brightness;b=t;break;case 3:r=p;g=q;b=brightness;break;
            case 4:r=t;g=p;b=brightness;break;default:r=brightness;g=p;b=q;
        }
        return Math.round((r+(1-r)*white)*255*COLOR_BRIGHTNESS)<<16 | Math.round((g+(1-g)*white)*255*COLOR_BRIGHTNESS)<<8 | Math.round((b+(1-b)*white)*255*COLOR_BRIGHTNESS);
    }
    private static float[] point(float x,float y) { return new float[]{(x-366)*SCALE,(214-y)*SCALE}; }
    private static float[][] outline() {
        // Explicit contour landmarks avoid making the reference's asymmetric feather cuts uniform.
        float[][] reference={
                {390,202},{396,184},{401,167},{418,164},{445,154},{477,139},
                {501,125},{513,113},{517,103},{512,97},{503,92},
                {520,83},{532,79},{545,79},{556,87},{566,105},{574,128},
                {584,160},{597,185},{614,207},{631,226},{640,238},
                {640,252},{623,249},{616,252},{611,261},{608,282},
                {600,308},{588,330},{585,309},{579,295},
                {578,320},{566,341},{546,356},{546,339},{539,316},{534,293},
                {529,315},{518,339},{501,361},{481,376},
                {486,354},{483,330},{478,303},{475,295},
                {471,310},{455,324},{439,333},{429,338},
                {436,321},{440,303},{438,287},
                {435,296},{423,305},{401,311},{409,299},{413,282},
                {414,254},{410,237},{399,222},{391,215},{398,207}
        };
        List<float[]> points=new ArrayList<>();
        points.add(new float[]{.12F,.04F});
        for(float[] p:reference) points.add(point(p[0],p[1]));
        points.add(new float[]{.12F,-.07F});
        return points.toArray(new float[0][]);
    }
    private static float cross(float[] a,float[] b,float[] c) {return (b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0]);}
    private static float[][] triangulate() {
        List<Integer> indices=new ArrayList<>();for(int i=0;i<OUTLINE.length;i++)indices.add(i);
        float area=0;for(int i=0;i<OUTLINE.length;i++){float[] a=OUTLINE[i],b=OUTLINE[(i+1)%OUTLINE.length];area+=a[0]*b[1]-b[0]*a[1];}
        float sign=area>0?1:-1;List<float[]> triangles=new ArrayList<>();
        while(indices.size()>3) {
            boolean clipped=false;
            for(int i=0;i<indices.size();i++) {
                int ia=indices.get((i+indices.size()-1)%indices.size()),ib=indices.get(i),ic=indices.get((i+1)%indices.size());
                float[] a=OUTLINE[ia],b=OUTLINE[ib],c=OUTLINE[ic];if(cross(a,b,c)*sign<.000001F)continue;
                boolean contains=false;
                for(int k:indices) if(k!=ia&&k!=ib&&k!=ic) {
                    float[] p=OUTLINE[k];if(cross(a,b,p)*sign>=0&&cross(b,c,p)*sign>=0&&cross(c,a,p)*sign>=0){contains=true;break;}
                }
                if(contains)continue;
                triangles.add(new float[]{a[0],a[1],b[0],b[1],c[0],c[1]});indices.remove(i);clipped=true;break;
            }
            if(!clipped)throw new IllegalStateException("Invalid astral wing outline");
        }
        float[] a=OUTLINE[indices.get(0)],b=OUTLINE[indices.get(1)],c=OUTLINE[indices.get(2)];
        triangles.add(new float[]{a[0],a[1],b[0],b[1],c[0],c[1]});return triangles.toArray(new float[0][]);
    }
    private static float[] surface(int divisions) {
        List<Float> points=new ArrayList<>();
        for(float[] t:TRIANGLES)for(int row=0;row<divisions;row++)for(int col=0;col<divisions-row;col++) {
            vertex(points,t,col/(float)divisions,row/(float)divisions);
            vertex(points,t,(col+1F)/divisions,row/(float)divisions);
            vertex(points,t,col/(float)divisions,(row+1F)/divisions);
            if(col+row<divisions-1) {
                vertex(points,t,(col+1F)/divisions,row/(float)divisions);
                vertex(points,t,(col+1F)/divisions,(row+1F)/divisions);
                vertex(points,t,col/(float)divisions,(row+1F)/divisions);
            }
        }
        float[] out=new float[points.size()];for(int i=0;i<out.length;i++)out[i]=points.get(i);return out;
    }
    private static void vertex(List<Float> out,float[] t,float u,float v) {
        out.add(t[0]*(1-u-v)+t[2]*u+t[4]*v);out.add(t[1]*(1-u-v)+t[3]*u+t[5]*v);
    }
    private static boolean inside(float x,float y) {
        boolean inside=false;
        for(int a=0,b=OUTLINE.length-1;a<OUTLINE.length;b=a++) {
            float[] p=OUTLINE[a],q=OUTLINE[b];
            if((p[1]>y)!=(q[1]>y)&&x<(q[0]-p[0])*(y-p[1])/(q[1]-p[1])+p[0])inside=!inside;
        }
        return inside;
    }
    private static float[] stars() {
        List<Float> points=new ArrayList<>();
        for(int i=0;i<36;i++) {
            float x=.2F+((i*.618034F)%1)*2.3F,y=-1.4F+((i*.414214F)%1)*2.7F;
            if(inside(x,y)){points.add(x);points.add(y);}
        }
        float[] out=new float[points.size()];for(int i=0;i<out.length;i++)out[i]=points.get(i);return out;
    }
}
