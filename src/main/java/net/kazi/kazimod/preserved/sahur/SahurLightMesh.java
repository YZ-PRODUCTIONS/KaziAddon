package net.kazi.kazimod.preserved.sahur;

/** Fixed-budget geometry shared by runtime rendering and offline checks. */
public final class SahurLightMesh {
    public interface Sink { void vertex(float x, float y, float z, int rgb, float alpha); }
    private static final int GOLD = 0xffcd52, IVORY = 0xfff3c9, WHITE = 0xfffff4;
    private static final double TAU = Math.PI * 2;
    private static float clamp(float n) { return Math.max(0, Math.min(1, n)); }
    private static void v(Sink s, double x, double y, double z, int c, float a) { s.vertex((float)x, (float)y, (float)z, c, clamp(a)); }
    private static void ring(Sink s, double radius, double y, double width, int color, float alpha, double phase) {
        for (int i=0;i<64;i++) {
            double a=TAU*i/64+phase,b=TAU*(i+1)/64+phase;
            v(s,Math.cos(a)*(radius-width),y,Math.sin(a)*(radius-width),color,alpha);
            v(s,Math.cos(b)*(radius-width),y,Math.sin(b)*(radius-width),color,alpha);
            v(s,Math.cos(b)*(radius+width),y,Math.sin(b)*(radius+width),color,alpha);
            v(s,Math.cos(a)*(radius+width),y,Math.sin(a)*(radius+width),color,alpha);
        }
    }
    private static void shaft(Sink s,double radius,double bottom,double top,int color,float alpha) {
        for(int i=0;i<32;i++) {
            double a=TAU*i/32,b=TAU*(i+1)/32;
            v(s,Math.cos(a)*radius,bottom,Math.sin(a)*radius,color,alpha);
            v(s,Math.cos(b)*radius,bottom,Math.sin(b)*radius,color,alpha);
            v(s,Math.cos(b)*radius,top,Math.sin(b)*radius,color,alpha*.8F);
            v(s,Math.cos(a)*radius,top,Math.sin(a)*radius,color,alpha*.8F);
        }
    }
    private static void spark(Sink s,double x,double y,double z,double size,int color,float alpha) {
        v(s,x-size,y,z,color,alpha);v(s,x,y+size*2,z,color,alpha);
        v(s,x+size,y,z,color,alpha);v(s,x,y-size*2,z,color,alpha);
        v(s,x,y,z-size,color,alpha);v(s,x,y+size*2,z,color,alpha);
        v(s,x,y,z+size,color,alpha);v(s,x,y-size*2,z,color,alpha);
    }
    public static void render(Sink s,int kind,float age,int life) {
        if(kind==0||kind==1){Sink original=s;s=(x,y,z,c,a)->original.vertex(x*3,y,z*3,c,a);}
        float fade=clamp((life-age)/12),open=clamp(age/9);
        if(kind==3){
            float alpha=fade*open;
            ring(s,3.2,0,.14,GOLD,alpha,0);ring(s,2.8,-.08,.05,WHITE,alpha,0);
            ring(s,2.35,-.13,.09,GOLD,alpha*.8F,0);ring(s,1.0,-.16,.03,IVORY,alpha*.8F,0);
            for(int i=0;i<32;i++){
                double a=TAU*i/32+age*.013,b=a+.04,r=2.55;
                v(s,Math.cos(a)*1.15,-.1,Math.sin(a)*1.15,GOLD,alpha*.25F);
                v(s,Math.cos(b)*1.15,-.1,Math.sin(b)*1.15,GOLD,alpha*.25F);
                v(s,Math.cos(b)*2.7,-.1,Math.sin(b)*2.7,WHITE,alpha*.45F);
                v(s,Math.cos(a)*2.7,-.1,Math.sin(a)*2.7,WHITE,alpha*.45F);
                spark(s,Math.cos(a)*r,-.2,Math.sin(a)*r,.07,WHITE,alpha);
            }
            shaft(s,.18,-.9,0,WHITE,alpha*.7F);return;
        }
        if(kind==2) {
            double radius=2.9*(.85+.15*open),center=1.7;
            // Fine latitude panels form an enclosing luminous shield, not a solid opaque ball.
            for(int j=0;j<12;j++)for(int i=0;i<40;i++) {
                double p=-Math.PI/2+Math.PI*j/12,q=-Math.PI/2+Math.PI*(j+1)/12;
                double a=TAU*i/40+age*.008,b=TAU*(i+1)/40+age*.008;
                float alpha=(float)(.035+.045*Math.pow(Math.sin(a*4+p*3+age*.09),2))*fade*open;
                v(s,Math.cos(p)*Math.cos(a)*radius,center+Math.sin(p)*radius,Math.cos(p)*Math.sin(a)*radius,IVORY,alpha);
                v(s,Math.cos(p)*Math.cos(b)*radius,center+Math.sin(p)*radius,Math.cos(p)*Math.sin(b)*radius,IVORY,alpha);
                v(s,Math.cos(q)*Math.cos(b)*radius,center+Math.sin(q)*radius,Math.cos(q)*Math.sin(b)*radius,GOLD,alpha);
                v(s,Math.cos(q)*Math.cos(a)*radius,center+Math.sin(q)*radius,Math.cos(q)*Math.sin(a)*radius,GOLD,alpha);
            }
            for(int j=0;j<5;j++){double y=.1+j*.85,r=Math.sqrt(Math.max(0,radius*radius-(y-center)*(y-center)));ring(s,r,y,.025,GOLD,.72F*fade*open,0);}
            for(int k=0;k<8;k++)for(int j=0;j<16;j++){
                double a=TAU*k/8+age*.008,p=-Math.PI/2+Math.PI*j/16,q=-Math.PI/2+Math.PI*(j+1)/16;
                double w=.012;
                v(s,Math.cos(p)*Math.cos(a-w)*radius,center+Math.sin(p)*radius,Math.cos(p)*Math.sin(a-w)*radius,GOLD,.48F*fade*open);
                v(s,Math.cos(p)*Math.cos(a+w)*radius,center+Math.sin(p)*radius,Math.cos(p)*Math.sin(a+w)*radius,GOLD,.48F*fade*open);
                v(s,Math.cos(q)*Math.cos(a+w)*radius,center+Math.sin(q)*radius,Math.cos(q)*Math.sin(a+w)*radius,GOLD,.48F*fade*open);
                v(s,Math.cos(q)*Math.cos(a-w)*radius,center+Math.sin(q)*radius,Math.cos(q)*Math.sin(a-w)*radius,GOLD,.48F*fade*open);
            }
            for(int i=0;i<16;i++){double a=TAU*i/16+age*.016;spark(s,Math.cos(a)*radius,1.7+.5*Math.sin(a*2),Math.sin(a)*radius,.09,WHITE,fade*open);}
            ring(s,2.5,4.3,.11,GOLD,fade*open,0);
            return;
        }
        float power=kind==0?clamp(age/40):1;
        // Concentric heavenly apertures, slowly counter-rotating ladder rings and falling rays.
        for(int j=0;j<4;j++) ring(s,(3.1+j*1.1)*(.65+.35*power),50+j*.32,.07+j*.018,j%2==0?WHITE:GOLD,fade*open*.85F,0);
        for(int i=0;i<24;i++){double a=TAU*i/24+age*.012,r=6.2;spark(s,Math.cos(a)*r,50.2,Math.sin(a)*r,.23,IVORY,fade*open);}
        ring(s,1.35,.07,.055,GOLD,fade*.75F,0);
        if(kind==0){
            for(int i=0;i<20;i++){double a=TAU*i/20+age*.025,y=(age*.6+i*2.7)%48;spark(s,Math.cos(a)*1.6,y,Math.sin(a)*1.6,.07,IVORY,power*fade*.65F);}
            shaft(s,.1,0,50,WHITE,power*.15F);return;
        }
        double bottom=50*(1-clamp(age/6));
        shaft(s,.55,bottom,51,WHITE,.9F*fade);
        shaft(s,.95,bottom,51,IVORY,.62F*fade);
        shaft(s,1.35,bottom,51,GOLD,.34F*fade);
        shaft(s,1.65,bottom,51,GOLD,.1F*fade);
        for(int j=0;j<8;j++){double y=(48+j*6.25-age*.65)%50;if(y<0)y+=50;ring(s,1.6+.2*Math.sin(age*.08+j),y,.045,IVORY,.6F*fade,0);}
        for(int i=0;i<32;i++){double a=TAU*i/32+age*.007,y=(i*3.3-age*.8)%50;if(y<0)y+=50;spark(s,Math.cos(a)*2.1,y,Math.sin(a)*2.1,.1,WHITE,fade*.7F);}
        if(age>5){float impact=clamp((age-5)/14);ring(s,1.4+impact*6,.12,.09,GOLD,(1-impact)*fade,0);ring(s,1.4+impact*3,.2,.15,WHITE,(1-impact)*fade,0);}
    }
    public static void arrow(Sink s,float age){
        // The arrow points down local -Z. Crossed fins keep it readable from every angle.
        for(int axis=0;axis<2;axis++){
            final boolean swap=axis==1;Sink face=swap?(x,y,z,c,a)->s.vertex(y,x,z,c,a):s;
            v(face,0,0,-1.8,WHITE,1);v(face,-.36,0,-.8,GOLD,.9F);v(face,0,0,-1.03,IVORY,1);v(face,.36,0,-.8,GOLD,.9F);
            v(face,-.055,0,-1.1,WHITE,1);v(face,.055,0,-1.1,WHITE,1);v(face,.055,0,1.6,IVORY,.85F);v(face,-.055,0,1.6,IVORY,.85F);
            v(face,-.04,0,1.0,GOLD,.8F);v(face,-.34,0,1.65,GOLD,.1F);v(face,.34,0,1.65,GOLD,.1F);v(face,.04,0,1.0,GOLD,.8F);
            v(face,-.14,0,.2,GOLD,.22F);v(face,.14,0,.2,GOLD,.22F);v(face,.025,0,5.5,GOLD,0);v(face,-.025,0,5.5,GOLD,0);
        }
        for(int i=0;i<6;i++){double a=i*2.4+age*.2;spark(s,Math.cos(a)*.16,Math.sin(a)*.16,1+i*.6,.035,IVORY,(1-i/6F)*.5F);}
    }
}
