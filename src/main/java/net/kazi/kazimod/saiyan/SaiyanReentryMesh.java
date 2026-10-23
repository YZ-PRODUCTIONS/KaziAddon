package net.kazi.kazimod.saiyan;

import net.kazi.kazimod.worldturtle.WorldTurtleMesh.Sink;

/** Downward bow shock and upward plasma wake; all detail is a bounded mesh, not particles. */
public final class SaiyanReentryMesh {
    private static final double TAU=Math.PI*2;
    private SaiyanReentryMesh(){}

    public static void render(Sink s,float age,float radius,float height,float heat,boolean shell){
        if(heat<=0)return;
        float nose=-height*.5F-.3F,length=height+5+radius*2;
        if(shell){
            for(int ring=0;ring<8;ring++)for(int side=0;side<24;side++){
                sheath(s,age,radius,nose,length,ring/8F,side*TAU/24,heat);
                sheath(s,age,radius,nose,length,ring/8F,(side+1)*TAU/24,heat);
                sheath(s,age,radius,nose,length,(ring+1)/8F,(side+1)*TAU/24,heat);
                sheath(s,age,radius,nose,length,(ring+1)/8F,side*TAU/24,heat);
            }
            return;
        }
        // The hot rounded front widens around the feet, then peels away along the body.
        for(int ring=0;ring<5;ring++)for(int side=0;side<32;side++){
            shock(s,radius,nose,ring/5F,side*TAU/32,heat);
            shock(s,radius,nose,ring/5F,(side+1)*TAU/32,heat);
            shock(s,radius,nose,(ring+1)/5F,(side+1)*TAU/32,heat);
            shock(s,radius,nose,(ring+1)/5F,side*TAU/32,heat);
        }
        for(int trail=0;trail<18;trail++)for(int segment=0;segment<7;segment++){
            ribbon(s,age,radius,nose,length,trail,segment/7F,-1,heat);
            ribbon(s,age,radius,nose,length,trail,segment/7F,1,heat);
            ribbon(s,age,radius,nose,length,trail,(segment+1)/7F,1,heat);
            ribbon(s,age,radius,nose,length,trail,(segment+1)/7F,-1,heat);
        }
        for(int ring=0;ring<3;ring++){
            float t=(age*.12F+ring/3F)%1;
            SaiyanMesh.ring(s,nose+t*(height+3),radius*(1.15F+t*.65F),radius*.035F,
                    0xc5e6ff,heat*(1-t)*.35F,0);
        }
        for(int spark=0;spark<14;spark++){
            double angle=spark*2.399963;float t=(age*.09F+spark*.618034F)%1;
            float r=radius*(1.2F+.65F*t),x=(float)Math.cos(angle)*r,z=(float)Math.sin(angle)*r;
            float y=nose+.7F+t*length;
            SaiyanMesh.line(s,x,y,z,x*1.04F,y+.45F+radius*.5F,z*1.04F,.02F+radius*.015F,
                    spark%3==0?0xffecb0:0xff742b,heat*(1-t)*.65F);
        }
    }
    private static void sheath(Sink s,float age,float radius,float nose,float length,float t,double angle,float heat){
        float spread=(float)(.65+1.15*Math.sin(t*Math.PI*.8))*(1-.9F*t);
        float wave=(float)Math.sin(angle*5-age*.8+t*13)*.07F*t;
        float r=radius*(spread+wave),alpha=heat*.07F*(1-t)*(1-t);
        s.vertex((float)Math.cos(angle)*r,nose+t*length,(float)Math.sin(angle)*r,0xff762e,alpha);
    }
    private static void shock(Sink s,float radius,float nose,float t,double angle,float heat){
        double arc=t*Math.PI*.5;
        float r=radius*(.12F+1.08F*(float)Math.sin(arc));
        float y=nose+radius*.9F*(1-(float)Math.cos(arc));
        int color=t<.4F?0xd8eeff:t<.8F?0xfff1c2:0xff8b38;
        float alpha=heat*(.12F+.5F*(float)Math.sin(t*Math.PI));
        s.vertex((float)Math.cos(angle)*r,y,(float)Math.sin(angle)*r,color,alpha);
    }
    private static void ribbon(Sink s,float age,float radius,float nose,float length,int trail,float t,int edge,float heat){
        double angle=trail*TAU/18+.16*Math.sin(age*.45+trail+t*7)+t*.7;
        float variation=.65F+.35F*(float)Math.sin(trail*7.3);
        float r=radius*(1.15F+.55F*(float)Math.sin(t*Math.PI))*(1-.8F*t)+(float)Math.sin(age*.8-trail+t*10)*t*.15F;
        float width=radius*(.11F+.05F*variation)*(1-t);
        float x=(float)Math.cos(angle)*r+(float)-Math.sin(angle)*width*edge;
        float z=(float)Math.sin(angle)*r+(float)Math.cos(angle)*width*edge;
        float y=nose+radius*.75F+t*length*(.6F+.4F*variation);
        float alpha=heat*(.5F+.2F*(float)Math.sin(age*.7-trail+t*9))*(1-t)*(1-t);
        int color=t<.12F?0xffefba:t<.5F?0xffa33e:0xff431c;
        s.vertex(x,y,z,color,alpha);
    }
}
