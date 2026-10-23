package net.kazi.kazimod.saiyan;

import net.kazi.kazimod.worldturtle.WorldTurtleMesh;
import net.kazi.kazimod.worldturtle.WorldTurtleMesh.Sink;

public final class SaiyanMesh {
    private static final float TAU=(float)(Math.PI*2);
    private SaiyanMesh(){}
    public static Sink tint(Sink out,int tint){
        return (x,y,z,color,alpha)->{
            int max=Math.max(color>>16&255,Math.max(color>>8&255,color&255));
            int min=Math.min(color>>16&255,Math.min(color>>8&255,color&255));
            float white=max==0?0:(float)min/max;
            int mixed=mix(tint,0xffffff,white);
            out.vertex(x,y,z,shade(mixed,max/255F),alpha);
        };
    }
    public static void orb(Sink out,float radius,int color,float alpha,float age){
        WorldTurtleMesh.sphere(out,0,0,0,radius*1.2F,radius*1.2F,radius*1.2F,12,6,color,.12F*alpha);
        WorldTurtleMesh.sphere(out,0,0,0,radius,radius,radius,12,6,color,.55F*alpha);
        WorldTurtleMesh.sphere(out,0,0,0,radius*.73F,radius*.73F,radius*.73F,12,6,0xf5ffff,.95F*alpha);
        ring(out,0,radius*1.35F,.035F*radius,color,.75F*alpha,age*.03F);
        ring(out,0,radius*1.55F,.02F*radius,0xffffff,.5F*alpha,-age*.04F);
    }
    public static void ring(Sink s,float y,float r,float width,int color,float alpha,float tilt){
        for(int i=0;i<32;i++){
            float a=i*TAU/32,b=(i+1)*TAU/32;
            ringVertex(s,y,r-width,a,tilt,color,alpha);ringVertex(s,y,r+width,a,tilt,color,alpha);
            ringVertex(s,y,r+width,b,tilt,color,alpha);ringVertex(s,y,r-width,b,tilt,color,alpha);
        }
    }
    private static void ringVertex(Sink s,float y,float r,float a,float tilt,int c,float alpha){
        float z=(float)Math.sin(a)*r;s.vertex((float)Math.cos(a)*r,y+z*(float)Math.sin(tilt),z*(float)Math.cos(tilt),c,alpha);
    }
    public static void aura(Sink s,float age,int color,float intensity,boolean lightning){
        for(int i=0;i<12;i++){
            float angle=i*TAU/12,phase=age*.11F+i*1.9F;
            float radius=.58F+.1F*(float)Math.sin(phase),height=2.2F+.6F*(float)Math.sin(phase*.7F);
            float x=(float)Math.cos(angle)*radius,z=(float)Math.sin(angle)*radius;
            float dx=(float)Math.cos(angle+.24)*radius,dz=(float)Math.sin(angle+.24)*radius;
            s.vertex(x,0,z,color,0);s.vertex(dx,0,dz,color,0);
            s.vertex(dx*.6F,height,dz*.6F,color,.01F);s.vertex(x*1.2F,height*.55F,z*1.2F,color,.28F*intensity);
            s.vertex(x,.1F,z,0xffffff,.2F*intensity);s.vertex(dx,.1F,dz,color,.14F*intensity);
            s.vertex(dx*.8F,height*.6F,dz*.8F,color,0);s.vertex(x*.8F,height*.75F,z*.8F,color,0);
        }
        for(int i=0;i<2;i++){float phase=(age*.025F+i*.5F)%1;ring(s,phase*2.5F,.7F+phase*.35F,.018F,color,(1-phase)*.55F*intensity,0);}
        if(lightning)for(int i=0;i<3;i++){
            float a=age*.08F+i*TAU/3;
            float x=(float)Math.cos(a)*.72F,z=(float)Math.sin(a)*.72F;
            for(int j=0;j<5;j++){
                float x0=x+(float)Math.sin(j*4+Math.floor(age/3))*.17F;
                float x1=x+(float)Math.sin((j+1)*4+Math.floor(age/3))*.17F;
                line(s,x0,j*.4F+.1F,z,x1,(j+1)*.4F+.1F,z,.022F,0xd7f5ff,.9F);
            }
        }
    }
    public static void line(Sink s,float x,float y,float z,float X,float Y,float Z,float w,int color,float alpha){
        s.vertex(x-w,y,z,color,alpha);s.vertex(x+w,y,z,color,alpha);s.vertex(X+w,Y,Z,color,alpha);s.vertex(X-w,Y,Z,color,alpha);
        s.vertex(x,y-w,z,color,alpha);s.vertex(x,y+w,z,color,alpha);s.vertex(X,Y+w,Z,color,alpha);s.vertex(X,Y-w,Z,color,alpha);
    }
    /** Local +Z is travel direction. View-dependent rim lighting outlines a transparent shell. */
    public static void flightAura(Sink s,float age,float speed,float vx,float vy,float vz,boolean rim){
        float stretch=2.6F+Math.min(2.4F,speed*1.6F),pulse=1+.012F*(float)Math.sin(age*.18F);
        for(int side=0;side<40;side++)for(int row=0;row<18;row++){
            float a=side*TAU/40,b=(side+1)*TAU/40,lo=row/18F,hi=(row+1)/18F;
            flightVertex(s,a,lo,stretch,pulse,vx,vy,vz,rim);
            flightVertex(s,b,lo,stretch,pulse,vx,vy,vz,rim);
            flightVertex(s,b,hi,stretch,pulse,vx,vy,vz,rim);
            flightVertex(s,a,hi,stretch,pulse,vx,vy,vz,rim);
        }
        if(rim)for(int i=0;i<8;i++){
            float a=i*TAU/8,phase=(age*.035F+i*.173F)%1,r=.85F*(1-phase*.55F);
            float z=-.5F-phase*stretch,length=.4F+phase*.7F;
            float x=(float)Math.cos(a)*r,y=(float)Math.sin(a)*r;
            line(s,x,y,z,x*.75F,y*.75F,z-length,.016F,0xffffff,(float)Math.sin(phase*Math.PI)*.038F);
        }
    }
    private static void flightVertex(Sink s,float angle,float u,float stretch,float pulse,float vx,float vy,float vz,boolean rim){
        double t=Math.PI*Math.pow(u,1.45),radius=Math.sin(t)*1.08*pulse;
        double slope=Math.cos(t)*Math.PI*1.45*Math.pow(Math.max(.0001,u),.45)*1.08*pulse/(stretch+1.4);
        double nx=Math.cos(angle),ny=Math.sin(angle),nz=-slope,normal=Math.sqrt(1+slope*slope);
        float facing=(float)Math.min(1,Math.abs((nx*vx+ny*vy+nz*vz)/normal));
        float edge=(float)Math.pow(1-facing,10);
        float alpha=rim?edge*.092F:.0035F+.0085F*(float)Math.pow(1-facing,3);
        if(u==0||u==1)alpha*=.18F;
        s.vertex((float)(nx*radius),(float)(ny*radius),-stretch+u*(stretch+1.4F),rim?0xffffff:0xd4e4f5,alpha);
    }
    public static void transformationAura(Sink s,float age,int form,float opacity,float powerUp){
        opacity*=.35F;
        form=SaiyanBalance.tier(form);int color=SaiyanBalance.COLORS[form];
        if(form==SaiyanBalance.ULTRA_INSTINCT){ultraInstinctAura(s,age,opacity,powerUp);return;}
        float ignition=powerUp<0?1:.22F+.78F*smooth((powerUp-10)/7);
        float surge=powerUp<0?0:smooth((powerUp-12)/3)*(1-smooth((powerUp-21)/17));
        float height=(form==3?3.8F:form==4?2.8F:3.1F)+surge*1.6F;
        float radius=(form==3?1.05F:.88F)+surge*.4F;
        // Broad translucent flames keep a readable silhouette in daylight, with no particles.
        for(int layer=0;layer<2;layer++)for(int i=0;i<24;i++){
            float a=i*TAU/24,b=(i+1)*TAU/24;
            float flicker=(float)Math.sin(age*.31F+i*2.41F+layer);
            float peak=height*(.83F+.17F*flicker)*(layer==0?1:.8F);
            float r=radius*(layer==0?1:.77F);
            int c=layer==0?color:mix(color,0xffffff,.32F);
            for(int row=0;row<5;row++){
                float low=row/5F,high=(row+1)/5F;
                flameVertex(s,a,low,r,peak,c,opacity*ignition,age,i);
                flameVertex(s,b,low,r,peak,c,opacity*ignition,age,i);
                flameVertex(s,b,high,r,peak,c,opacity*ignition,age,i);
                flameVertex(s,a,high,r,peak,c,opacity*ignition,age,i);
            }
        }
        for(int i=0;i<10;i++){
            float a=i*TAU/10+age*.006F,r=radius*(1+.1F*(float)Math.sin(i+age*.2F));
            float h=height*(.82F+.18F*(float)Math.sin(age*.24F+i*1.7F));
            float x=(float)Math.cos(a)*r,z=(float)Math.sin(a)*r;
            float tx=-(float)Math.sin(a)*.075F,tz=(float)Math.cos(a)*.075F;
            int c=mix(color,0xffffff,.5F);float alpha=(.28F+surge*.25F)*opacity*ignition;
            s.vertex(x-tx,.05F,z-tz,c,0);s.vertex(x+tx,.05F,z+tz,c,0);
            s.vertex(x*.85F,h,z*.85F,c,0);s.vertex(x*1.12F,h*.52F,z*1.12F,c,alpha);
        }
        ring(s,.07F,radius,.018F,color,.24F*opacity*ignition,0);
        if(form==2||form==3||form==5){
            int bolts=form==3?5:3;
            for(int i=0;i<bolts;i++){
                float angle=i*TAU/bolts+(float)Math.floor(age/3)*1.7F;
                float x=(float)Math.cos(angle)*radius,z=(float)Math.sin(angle)*radius;
                for(int j=0;j<5;j++){
                    float y=.2F+j*.43F,Y=y+.43F;
                    float offset=(float)Math.sin(j*7.1+Math.floor(age/3))*.19F;
                    float next=(float)Math.sin((j+1)*7.1+Math.floor(age/3))*.19F;
                    line(s,x+offset,y,z,x+next,Y,z,.055F,0x4caeff,.35F*opacity);
                    line(s,x+offset,y,z,x+next,Y,z,.014F,0xf1fcff,.95F*opacity);
                }
            }
        }
        if(surge>0){
            float phase=clamp((powerUp-13)/24);
            ring(s,.12F,1+phase*6,.16F*(1-phase),0xfff9df,(1-phase)*opacity,0);
            for(int i=0;i<12;i++){
                float a=i*TAU/12,x=(float)Math.cos(a),z=(float)Math.sin(a);
                line(s,x*.6F,.15F,z*.6F,x*(1+phase*4),.1F+surge*(1+i%3),z*(1+phase*4),.07F,0xffffff,surge*.7F*opacity);
            }
        }
    }
    private static void flameVertex(Sink s,float a,float t,float radius,float height,int color,float alpha,float age,int segment){
        float profile=.72F+.4F*(float)Math.sin(t*Math.PI)-.45F*t;
        float r=radius*profile;
        float bend=.07F*(float)Math.sin(age*.22F+t*5+segment);
        float y=t*height;
        s.vertex((float)Math.cos(a+bend)*r,y,(float)Math.sin(a+bend)*r,color,
                alpha*(t==0||t==1?0:.1F+.085F*(float)Math.sin(t*Math.PI)));
    }
    private static void ultraInstinctAura(Sink s,float age,float opacity,float powerUp){
        float ignition=powerUp<0?1:.3F+.7F*smooth((powerUp-8)/12);
        for(int layer=0;layer<3;layer++)for(int i=0;i<28;i++){
            float a=i*TAU/28,b=(i+1)*TAU/28;
            float radius=layer==0?1.02F:layer==1?.81F:.62F;
            float height=(layer==0?3.4F:layer==1?2.9F:2.5F)*(1+.1F*(float)Math.sin(age*.22F+i*1.71+layer));
            int color=layer==0?(i%5==0?0xa478ff:0x4d9bff):layer==1?0x9feaff:0xf5f8ff;
            for(int row=0;row<7;row++){
                float lo=row/7F,hi=(row+1)/7F,alpha=opacity*ignition*(layer==2?.85F:.7F);
                flameVertex(s,a,lo,radius,height,color,alpha,age,i);
                flameVertex(s,b,lo,radius,height,color,alpha,age,i+1);
                flameVertex(s,b,hi,radius,height,color,alpha,age,i+1);
                flameVertex(s,a,hi,radius,height,color,alpha,age,i);
            }
        }
        for(int i=0;i<16;i++){
            float phase=(age*.018F+i*.193F)%1,a=i*TAU/16+age*.006F;
            float r=.65F+.16F*(float)Math.sin(phase*Math.PI),y=.1F+phase*2.5F;
            float x=(float)Math.cos(a)*r,z=(float)Math.sin(a)*r;
            line(s,x,y,z,x*.8F,y+.55F,z*.8F,i%4==0?.035F:.018F,i%4==0?0xc198ff:0xf5ffff,
                    (float)Math.sin(phase*Math.PI)*.8F*opacity*ignition);
        }
        if(powerUp>=0){float phase=clamp((powerUp-12)/24);ring(s,.1F,.5F+phase*5,.08F,0xdbefff,(1-phase)*opacity,0);}
    }
    public static void instinctDodge(Sink s,float age,float progress){
        float fade=1-progress;
        for(int i=0;i<12;i++){
            float a=i*TAU/12,r=.35F+progress*.25F,x=(float)Math.cos(a)*r,z=(float)Math.sin(a)*r;
            line(s,x,.15F,z,x,1.8F,z,.02F,0xdcf6ff,fade*.55F);
        }
        ring(s,.95F,.4F+progress*.8F,.04F,0xa2ccff,fade*.75F,age*.08F);
    }
    public static void transformationDust(Sink s,float age){
        float t=clamp((age-13)/27),fade=(float)Math.sin(t*Math.PI);
        if(fade<=0)return;
        for(int i=0;i<10;i++){
            float a=i*TAU/10,r=1+t*3.2F,puff=.3F+t*.55F;
            WorldTurtleMesh.sphere(s,(float)Math.cos(a)*r,.2F+t*.4F,(float)Math.sin(a)*r,
                    puff,puff*.65F,puff,8,4,i%2==0?0xc2c8d8:0x8c91a6,fade*.5F);
        }
    }
    public static void spiritImpact(Sink s,float age,float size){
        float growth=smooth(age/22),fade=1-smooth((age-24)/76),r=5+(size-5)*growth;
        if(fade<=0)return;
        float core=1-smooth((age-12)/52);
        WorldTurtleMesh.sphere(s,0,0,0,r*1.1F,r*1.1F,r*1.1F,32,16,0x278cff,.13F*fade);
        WorldTurtleMesh.sphere(s,0,0,0,r,r,r,32,16,0x75dfff,.28F*fade);
        WorldTurtleMesh.sphere(s,0,0,0,r*.84F,r*.84F,r*.84F,24,12,0xf3ffff,.85F*core);
        for(int i=0;i<4;i++){
            float p=clamp((age-i*6)/56),wave=(float)Math.sin(p*Math.PI);
            if(wave<=0)continue;
            float radius=5+p*size*1.45F;
            ring(s,.3F+i*.35F,radius,.25F+wave*.65F,0xb9faff,wave*.8F,0);
            ring(s,.3F+i*.35F,radius,.08F,0xffffff,wave,0);
        }
        for(int i=0;i<18;i++){
            float a=i*TAU/18,elevation=.2F+(i%5)*.17F;
            float tail=clamp((age-2)/34),head=clamp((age+8)/34);
            float dx=(float)Math.cos(a)*(1-elevation*.35F),dz=(float)Math.sin(a)*(1-elevation*.35F);
            float length=size*(1.3F+(i%3)*.12F),alpha=(1-smooth((age-20)/35))*.7F;
            line(s,dx*tail*length,elevation*tail*length,dz*tail*length,dx*head*length,elevation*head*length,dz*head*length,.16F,0xe8ffff,alpha);
        }
        for(int i=0;i<8;i++){
            float phase=(age*.014F+i*.125F)%1,a=i*TAU/8;
            float x=(float)Math.cos(a)*r*.8F,z=(float)Math.sin(a)*r*.8F,y=phase*r;
            line(s,x,y,z,x*.98F,y+2+phase*3,z*.98F,.035F,0xd2faff,(1-phase)*fade*.6F);
        }
    }
    private static float clamp(float t){return Math.max(0,Math.min(1,t));}
    private static float smooth(float t){t=clamp(t);return t*t*(3-2*t);}
    private static int mix(int a,int b,float t){return ((int)((a>>16&255)*(1-t)+(b>>16&255)*t)<<16)
            |((int)((a>>8&255)*(1-t)+(b>>8&255)*t)<<8)|(int)((a&255)*(1-t)+(b&255)*t);}
    public static void beam(Sink s,float length,float radius,float age){
        for(int layer=0;layer<3;layer++){
            float r=radius*(layer==0?1.5F:layer==1?1:.52F),alpha=layer==0?.13F:layer==1?.65F:1;
            int c=layer==2?0xf2ffff:0x35aaff;
            for(int i=0;i<16;i++){
                float a=i*TAU/16,b=(i+1)*TAU/16;
                s.vertex((float)Math.cos(a)*r,(float)Math.sin(a)*r,0,c,alpha);
                s.vertex((float)Math.cos(b)*r,(float)Math.sin(b)*r,0,c,alpha);
                s.vertex((float)Math.cos(b)*r*.8F,(float)Math.sin(b)*r*.8F,length,c,alpha);
                s.vertex((float)Math.cos(a)*r*.8F,(float)Math.sin(a)*r*.8F,length,c,alpha);
            }
        }
        orb(s,radius*1.1F,0x40baff,1,age);
        Sink end=(x,y,z,c,a)->s.vertex(x,y,z+length,c,a);orb(end,radius*1.9F,0x40baff,.8F,age);
        for(int i=0;i<4;i++){
            float distance=((age*.035F+i*.25F)%1)*length;
            ring((x,y,z,c,a)->s.vertex(x,z,y+distance,c,a),0,radius*1.7F,.04F,0xb5f6ff,.7F,0);
        }
    }
    public static void spike(Sink s,float x,float y,float z,float width,float tx,float ty,float tz,int color){
        float[][] p={{x-width,y,z-width},{x+width,y,z-width},{x+width,y,z+width},{x-width,y,z+width}};
        for(int i=0;i<4;i++){
            int shade=shade(color,.65F+i*.1F);float[] a=p[i],b=p[(i+1)%4];
            s.vertex(a[0],a[1],a[2],shade,1);s.vertex(b[0],b[1],b[2],shade,1);s.vertex(tx,ty,tz,shade,1);s.vertex(tx,ty,tz,shade,1);
        }
    }
    private static int shade(int c,float f){return ((int)((c>>16&255)*f)<<16)|((int)((c>>8&255)*f)<<8)|(int)((c&255)*f);}
}
