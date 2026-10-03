package net.kazi.kazimod.preserved.cerberus;

/** Low-poly animated surfaces; deterministic and allocation-free within the vertex loops. */
public final class CerberusVfxMesh {
    public interface Sink { void vertex(float x,float y,float z,int color,float alpha); }
    private static void v(Sink s,double x,double y,double z,int c,float a){s.vertex((float)x,(float)y,(float)z,c,a);}
    public static void render(Sink s,int kind,float age){
        if(kind==0){
            sphere(s,1.02,age,0xffffda,1,false);
            sphere(s,1.23,age,0xffbe24,.68F,true);
            sphere(s,1.4,age,0xe83812,.32F,true);
            // Local +Z trails behind the projectile; the renderer aligns -Z to velocity.
            for(int i=0;i<12;i++){
                double a=i*Math.PI/6+age*.08,b=a+.17,r=1.1,len=2.6+.6*Math.sin(age*.4+i);
                v(s,Math.cos(a)*r,Math.sin(a)*r,0,0xffb62e,.85F);
                v(s,Math.cos(b)*r,Math.sin(b)*r,0,0xffb62e,.85F);
                v(s,Math.cos(b+.5)*.2,Math.sin(b+.5)*.2,len,0x8f163c,0);
                v(s,Math.cos(a+.5)*.2,Math.sin(a+.5)*.2,len,0x8f163c,0);
            }
        }else if(kind==1){
            double r=.8+Math.min(age/12,1)*3;
            float alpha=Math.max(0,1-age/24);
            sphere(s,r,age,0xffd768,alpha*.8F,true);
            sphere(s,r*.72,age,0xffffdd,alpha*.75F,false);
            ring(s,r*1.3,.16,0,0xff7624,alpha,age);
        }else if(kind==2){
            float alpha=Math.max(0,1-age/24);
            ring(s,1+age*.45,.16,0,0xb9a1ff,alpha,age);
            ring(s,.5+age*.38,.10,.25,0xeee5ff,alpha*.7F,age);
        }else{
            float alpha=Math.min(1,age/12)*Math.min(1,Math.max(0,(160-age)/15));
            ring(s,CerberusTerritory.RADIUS,.22,.06,0x56316b,alpha*.8F,128);
            ring(s,CerberusTerritory.RADIUS-.4,.08,.07,0x9877dc,alpha*.8F,128);
            for(int i=0;i<96;i++){
                double a=i*Math.PI/48,b=a+.035,h=.5+.5*Math.sin(age*.18+i*2),r=CerberusTerritory.RADIUS-.15;
                v(s,Math.cos(a)*r,.05,Math.sin(a)*r,0x120b20,alpha);
                v(s,Math.cos(b)*r,.05,Math.sin(b)*r,0x120b20,alpha);
                v(s,Math.cos(b+.02)*r,h,Math.sin(b+.02)*r,0x835dca,0);
                v(s,Math.cos(a+.02)*r,h,Math.sin(a+.02)*r,0x835dca,0);
            }
        }
    }
    private static void sphere(Sink s,double radius,float age,int color,float alpha,boolean flame){
        for(int y=0;y<8;y++)for(int x=0;x<16;x++)for(int c=0;c<4;c++){
            double u=(x+(c==1||c==2?1:0))*Math.PI/8,w=(y+(c>=2?1:0))*Math.PI/8;
            double r=radius*(flame?1+.09*Math.sin(u*5+w*3-age*.32)+.035*Math.cos(w*9+age*.2):1);
            v(s,Math.sin(w)*Math.cos(u)*r,Math.cos(w)*r,Math.sin(w)*Math.sin(u)*r,color,alpha);
        }
    }
    private static void ring(Sink s,double r,double width,double height,int color,float alpha,float age){
        ring(s,r,width,height,color,alpha,64);
    }
    private static void ring(Sink s,double r,double width,double height,int color,float alpha,int segments){
        for(int i=0;i<segments;i++)for(int c=0;c<4;c++){
            double a=(i+(c==1||c==2?1:0))*Math.PI*2/segments;
            double radius=r+(c>=2?width:0);
            v(s,Math.cos(a)*radius,height,Math.sin(a)*radius,color,alpha);
        }
    }
}
