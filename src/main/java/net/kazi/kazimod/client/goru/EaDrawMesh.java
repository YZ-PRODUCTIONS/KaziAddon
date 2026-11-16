package net.kazi.kazimod.client.goru;

import net.kazi.kazimod.models.abilities.EaSwordMesh;
import net.kazi.kazimod.models.abilities.EaVfxMesh;

/** Deterministic geometry on the shared 160-tick cinematic clock. No world changes. */
public final class EaDrawMesh {
    private EaDrawMesh() { }
    public static float smooth(float t) { t = Math.max(0, Math.min(1, t)); return t*t*(3-2*t); }
    public static float reach(float age) { return smooth((age-14)/32); }
    public static float draw(float age) { return smooth((age-58)/48); }
    public static float armAngle(float age) { return -(float)Math.PI/2 * reach(age) + draw(age)*.85F; }
    public static void render(EaVfxMesh.Sink solid, EaVfxMesh.Sink glow, float age, int detail) {
        if (!Float.isFinite(age) || age < 0 || age >= 160) return;
        float fade = 1-smooth((age-146)/14), pull=draw(age);
        float orb = smooth(age/18)*(1-smooth((age-93)/24))*fade;
        int segments = detail == 0 ? 32 : 20, rows = 12;
        float radius = .56F*(.92F+.08F*(float)Math.sin(age*.16));
        for (int layer=0;layer<3;layer++) {
            float r=radius*(1+layer*.14F);
            for(int row=0;row<rows;row++) for(int i=0;i<segments;i++) {
                for(int c=0;c<4;c++) {
                    double lat=Math.PI*((row+(c>=2?1:0))/(double)rows-.5);
                    double lon=Math.PI*2*(i+(c==1||c==2?1:0))/segments;
                    float x=(float)(Math.cos(lat)*Math.cos(lon)), y=(float)Math.sin(lat), z=(float)(Math.cos(lat)*Math.sin(lon));
                    glow.vertex(.34F+x*r,1.43F+y*r,.86F+z*r,layer==0?0xFFF2AD:0xFFB323,orb*(layer==0?.48F:.12F));
                }
            }
        }
        for(int ring=0;ring<3;ring++) {
            float r=radius*(1.15F+ring*.22F);
            for(int i=0;i<segments;i++) for(int c=0;c<4;c++) {
                double a=(i+(c==1||c==2?1:0))*Math.PI*2/segments+age*.018*(ring%2==0?1:-1);
                float rr=r+(c>=2?.018F:0), x=(float)Math.cos(a)*rr,y=(float)Math.sin(a)*rr;
                glow.vertex(.34F+x,1.43F+y,.86F+(float)Math.sin(a*3+ring)*.08F,0xFFE9A0,orb*.65F);
            }
        }
        if(age>=58) {
            float rotation=-pull*1.04F, cs=(float)Math.cos(rotation),sn=(float)Math.sin(rotation);
            float cutoff=-1.16F+4.1F*pull;
            float handY=1.43F-.65F*(float)Math.cos(-armAngle(age));
            float handZ=.65F*(float)Math.sin(-armAngle(age));
            EaVfxMesh.Sink body=(x,y,z,color,alpha)-> {
                z=Math.min(z,cutoff)+.60F;
                float yy=y*.62F,zz=z*.62F;
                solid.vertex(.34F+x*.62F,handY+yy*cs-zz*sn,handZ+yy*sn+zz*cs,color,alpha*fade);
            };
            EaVfxMesh.Sink light=(x,y,z,color,alpha)-> {
                z=Math.min(z,cutoff)+.60F;
                float yy=y*.62F,zz=z*.62F;
                glow.vertex(.34F+x*.62F,handY+yy*cs-zz*sn,handZ+yy*sn+zz*cs,color,alpha*fade);
            };
            EaSwordMesh.draw(body,light,detail,age,.35F+pull*.65F,smooth((age-58)/8));
        }
        // Branching cracks form a broken pane; disconnected fragments fly away at the draw's climax.
        float crack=smooth((age-70)/20)*(1-smooth((age-108)/26))*fade;
        float burst=smooth((age-90)/42);
        for(int branch=0;branch<14;branch++) {
            double angle=branch*2.39996323;
            float px=.34F,py=1.43F;
            for(int j=1;j<=5;j++) {
                double a=angle+Math.sin(branch*17+j*4)*.20;
                float distance=j*.38F*(1+burst*.9F);
                float x=.34F+(float)Math.cos(a)*distance,y=1.43F+(float)Math.sin(a)*distance;
                line(glow,px,py,x,y,.88F,.012F,0xFFE8AD,crack);
                if(j%2==0) line(glow,x,y,x+(float)Math.cos(a+.7)*.28F,y+(float)Math.sin(a+.7)*.28F,.88F,.006F,0xFF702D,crack*.7F);
                px=x;py=y;
            }
        }
        if(age>=90) for(int i=0;i<(detail==0?42:24);i++) {
            double angle=i*2.39996323+burst*.35;
            float r=.6F+(i%7)*.18F+burst*(1.4F+i%5*.38F);
            float x=.34F+(float)Math.cos(angle)*r,y=1.43F+(float)Math.sin(angle)*r-burst*burst*.6F;
            float z=.8F+(float)Math.sin(i*9)*burst*1.7F;
            float alpha=(1-smooth((age-110)/35))*fade*.48F;
            float size=.08F+(i%4)*.035F;
            for(int c=0;c<4;c++) {
                double a=c*Math.PI/2+age*.04+i;
                solid.vertex(x+(float)Math.cos(a)*size,y+(float)Math.sin(a)*size,z+(c%2)*size, i%2==0?0xFFE8A0:0xC57A3A,alpha);
            }
        }
    }
    private static void line(EaVfxMesh.Sink out,float x,float y,float xx,float yy,float z,float w,int color,float alpha) {
        float dx=xx-x,dy=yy-y,length=(float)Math.sqrt(dx*dx+dy*dy);
        if(length<.00001F) return;
        float nx=-dy/length*w,ny=dx/length*w;
        out.vertex(x-nx,y-ny,z,color,alpha);out.vertex(xx-nx,yy-ny,z,color,alpha);
        out.vertex(xx+nx,yy+ny,z,color,alpha);out.vertex(x+nx,y+ny,z,color,alpha);
    }
}
