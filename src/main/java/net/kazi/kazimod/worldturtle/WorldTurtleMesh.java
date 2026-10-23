package net.kazi.kazimod.worldturtle;

/** Bounded mesh effects: no particle entities or per-frame random allocations. */
public final class WorldTurtleMesh {
    public interface Sink {void vertex(float x,float y,float z,int color,float alpha);}
    private static final float TAU=(float)(Math.PI*2);
    public static boolean intersectsShield(double x,double y,double z,double dx,double dy,double dz){
        x/=22;y/=13;z/=22;dx/=22;dy/=13;dz/=22;
        double a=dx*dx+dy*dy+dz*dz,b=2*(x*dx+y*dy+z*dz),c=x*x+y*y+z*z-1;
        if(c<=0)return true;if(a<1e-12)return false;double d=b*b-4*a*c;if(d<0)return false;
        double t=(-b-Math.sqrt(d))/(2*a);return t>=0&&t<=1;
    }
    public static void render(Sink s,int kind,float age,float skyOffset,boolean solid){
        if(kind==2){if(solid)return;
            float fade=Math.min(1,age/7)*Math.min(1,(100-age)/7);
            sphere(s,0,0,0,22,13,22,24,12,0xffd64c,.09F*fade);
            ring(s,0,0,0,22,.18F,0xffed9a,.7F*fade,0);
            ring(s,0,0,0,22,.12F,0xffd13f,.55F*fade,age*.013F);
            for(int i=0;i<8;i++){float a=i*TAU/8+age*.009F;float x=(float)Math.cos(a)*21,z=(float)Math.sin(a)*21;
                sphere(s,x,0,z,.25F,1.2F,.25F,6,4,0xfff5c4,.7F*fade);
            }return;
        }
        if(kind==0){
            float portal=Math.min(1,age/16)*Math.max(0,Math.min(1,(70-age)/15));
            if(solid){disc(s,skyOffset,8*portal,0x030209,1);if(age>=20)bomb(s,Math.min(1,(age-20)/6));}
            else{ring(s,0,skyOffset,0,8*portal,.3F,0x7852b3,.85F*portal,0);ring(s,0,skyOffset+.04F,0,9*portal,.1F,0xb09ddd,.6F*portal,0);}
            return;
        }
        float growth=Math.min(1,age/90),fade=Math.max(0,Math.min(1,(220-age)/60));
        if(solid){
            float stem=5+growth*52,cap=9+growth*22;
            for(int i=0;i<8;i++){float y=stem*i/8f,r=4+growth*3+(float)Math.sin(i*1.7+age*.012)*1.1F;
                cloud(s,(float)Math.sin(i*2.1)*growth*2,y,(float)Math.cos(i*1.8)*growth*2,r,stem/10+3,r,12,6,i%2==0?0x524c48:0x75685a,.9F*fade,age*.015F+i);
            }
            for(int i=0;i<16;i++){float a=TAU*i/16,orbit=cap*.62F,puff=cap*.4F;
                float y=stem+(float)Math.sin(i*2.7+age*.016)*2;
                cloud(s,(float)Math.cos(a)*orbit,y,(float)Math.sin(a)*orbit,puff,cap*.36F,puff,12,6,i%3==0?0x82776a:0x504b46,.94F*fade,age*.012F+i);
            }
            cloud(s,0,stem+2,0,cap*.72F,cap*.3F,cap*.72F,16,8,0x6b6258,.96F*fade,age*.01F);
        }else{
            float fire=Math.max(0,1-age/90);
            sphere(s,0,3+growth*10,0,2+Math.min(22,age*.65F),5+growth*12,2+Math.min(22,age*.65F),20,10,age<16?0xfffaf0:0xff8b22,.8F*fire);
            for(int i=0;i<3;i++){float radius=Math.max(0,(age-i*6)*.8F);if(radius>0&&radius<65)ring(s,0,.4F+i*.25F,0,radius,1.2F,0xffd1a0,(1-radius/65)*.7F,0);}
            if(age>12&&age<130)ring(s,0,10+growth*34,0,12+growth*28,.7F,0xb7aba0,.35F*fade,0);
        }
    }
    private static void bomb(Sink s,float alpha){
        sphere(s,0,3,0,1.4F,3,1.4F,12,8,0x303238,alpha);
        for(int i=0;i<4;i++){float a=TAU*i/4,x=(float)Math.cos(a)*1.8F,z=(float)Math.sin(a)*1.8F;box(s,x-.3F,4,z-.3F,x+.3F,6.8F,z+.3F,0x555a61,alpha);}
        ring(s,0,3.7F,0,1.4F,.15F,0xc9ae4d,alpha,0);
    }
    public static void sphere(Sink s,float x,float y,float z,float rx,float ry,float rz,int sides,int rows,int color,float alpha){
        if(alpha<=0)return;
        for(int j=0;j<rows;j++){float p=(float)(-Math.PI/2+Math.PI*j/rows),P=(float)(-Math.PI/2+Math.PI*(j+1)/rows);
            for(int i=0;i<sides;i++){float a=TAU*i/sides,A=TAU*(i+1)/sides;
                point(s,x,y,z,rx,ry,rz,p,a,color,alpha);point(s,x,y,z,rx,ry,rz,p,A,color,alpha);point(s,x,y,z,rx,ry,rz,P,A,color,alpha);point(s,x,y,z,rx,ry,rz,P,a,color,alpha);
            }
        }
    }
    private static void cloud(Sink s,float x,float y,float z,float rx,float ry,float rz,int sides,int rows,int color,float alpha,float phase){
        if(alpha<=0)return;
        for(int j=0;j<rows;j++){float p=(float)(-Math.PI/2+Math.PI*j/rows),P=(float)(-Math.PI/2+Math.PI*(j+1)/rows);
            for(int i=0;i<sides;i++){float a=TAU*i/sides,A=TAU*(i+1)/sides;
                smokePoint(s,x,y,z,rx,ry,rz,p,a,color,alpha,phase);smokePoint(s,x,y,z,rx,ry,rz,p,A,color,alpha,phase);
                smokePoint(s,x,y,z,rx,ry,rz,P,A,color,alpha,phase);smokePoint(s,x,y,z,rx,ry,rz,P,a,color,alpha,phase);
            }
        }
    }
    private static void smokePoint(Sink s,float x,float y,float z,float rx,float ry,float rz,float p,float a,int c,float alpha,float phase){
        float nx=(float)(Math.cos(p)*Math.cos(a)),ny=(float)Math.sin(p),nz=(float)(Math.cos(p)*Math.sin(a));
        float noise=1+.075F*(float)(Math.sin(a*5+p*7+phase)*Math.cos(p*4-phase));
        float shade=.58F+.42F*Math.max(0,-nx*.35F+ny*.8F-nz*.4F);
        int color=((int)((c>>16&255)*shade)<<16)|((int)((c>>8&255)*shade)<<8)|(int)((c&255)*shade);
        s.vertex(x+nx*rx*noise,y+ny*ry*noise,z+nz*rz*noise,color,alpha);
    }
    private static void point(Sink s,float x,float y,float z,float rx,float ry,float rz,float p,float a,int c,float alpha){s.vertex(x+(float)(Math.cos(p)*Math.cos(a))*rx,y+(float)Math.sin(p)*ry,z+(float)(Math.cos(p)*Math.sin(a))*rz,c,alpha);}
    private static void disc(Sink s,float y,float r,int c,float alpha){for(int i=0;i<48;i++){float a=TAU*i/48,A=TAU*(i+1)/48;s.vertex(0,y,0,c,alpha);s.vertex(0,y,0,c,alpha);s.vertex((float)Math.cos(a)*r,y,(float)Math.sin(a)*r,c,alpha);s.vertex((float)Math.cos(A)*r,y,(float)Math.sin(A)*r,c,alpha);}}
    public static void ring(Sink s,float x,float y,float z,float radius,float width,int c,float alpha,float tilt){
        for(int i=0;i<64;i++){float a=TAU*i/64,A=TAU*(i+1)/64;
            ringPoint(s,x,y,z,radius-width,a,c,alpha,tilt);ringPoint(s,x,y,z,radius+width,a,c,alpha,tilt);ringPoint(s,x,y,z,radius+width,A,c,alpha,tilt);ringPoint(s,x,y,z,radius-width,A,c,alpha,tilt);
        }
    }
    private static void ringPoint(Sink s,float x,float y,float z,float r,float a,int c,float alpha,float tilt){float v=(float)Math.sin(a)*r;s.vertex(x+(float)Math.cos(a)*r,y+v*(float)Math.sin(tilt)*.59F,z+v*(float)Math.cos(tilt),c,alpha);}
    private static void box(Sink s,float x,float y,float z,float X,float Y,float Z,int c,float a){
        float[][]p={{x,y,z},{X,y,z},{X,Y,z},{x,Y,z},{x,y,Z},{X,y,Z},{X,Y,Z},{x,Y,Z}};
        int[][]faces={{0,1,2,3},{5,4,7,6},{4,0,3,7},{1,5,6,2},{3,2,6,7},{4,5,1,0}};
        for(int[]f:faces)for(int i:f)s.vertex(p[i][0],p[i][1],p[i][2],c,a);
    }
}
