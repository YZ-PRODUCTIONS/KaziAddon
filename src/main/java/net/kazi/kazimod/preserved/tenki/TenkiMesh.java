package net.kazi.kazimod.preserved.tenki;

/** Fixed-budget, deterministic geometry. No particle emitters or world allocations. */
public final class TenkiMesh {
    public interface Sink { void vertex(float x,float y,float z,int color,float alpha); }
    private static final double TAU=Math.PI*2;
    public static final float TORNADO_VISUAL_SCALE=1.5F;
    private static final float[][] CLOUDS=new float[3][];
    private static void v(Sink s,double x,double y,double z,int c,float a){s.vertex((float)x,(float)y,(float)z,c,Math.max(0,Math.min(1,a)));}
    private static double hash(int n){double x=Math.sin(n*127.1+311.7)*43758.5453;return x-Math.floor(x);}
    private static int shade(int c,double f){return ((int)((c>>16&255)*f)<<16)|((int)((c>>8&255)*f)<<8)|(int)((c&255)*f);}
    private static void box(Sink s,double x,double y,double z,double w,double h,double d,int c,float a){
        double[][] p={{x-w,y-h,z-d},{x+w,y-h,z-d},{x+w,y+h,z-d},{x-w,y+h,z-d},{x-w,y-h,z+d},{x+w,y-h,z+d},{x+w,y+h,z+d},{x-w,y+h,z+d}};
        int[][] faces={{0,1,2,3},{5,4,7,6},{4,0,3,7},{1,5,6,2},{3,2,6,7},{4,5,1,0}};
        for(int i=0;i<6;i++)for(int n:faces[i])v(s,p[n][0],p[n][1],p[n][2],shade(c,i==4?1:i==5?.55:.8),a);
    }
    private static void line(Sink s,double x,double y,double z,double xx,double yy,double zz,double w,int c,float a){
        v(s,x-w,y,z,c,a);v(s,xx-w,yy,zz,c,a);v(s,xx+w,yy,zz,c,a);v(s,x+w,y,z,c,a);
        v(s,x,y-w,z,c,a);v(s,xx,yy-w,zz,c,a);v(s,xx,yy+w,zz,c,a);v(s,x,y+w,z,c,a);
    }
    private static void billow(Sink s,double x,double y,double z,double w,double h,double d,int c){
        for(int row=0;row<4;row++)for(int seg=0;seg<12;seg++)for(int corner=0;corner<4;corner++){
            double lat=-Math.PI/2+(row+(corner>=2?1:0))*Math.PI/4;
            double angle=(seg+(corner==1||corner==2?1:0))*TAU/12;
            double shade=.64+.25*(Math.sin(lat)+1)/2+.1*(Math.cos(angle)+1)/2;
            v(s,x+Math.cos(lat)*Math.cos(angle)*w,y+Math.sin(lat)*h,z+Math.cos(lat)*Math.sin(angle)*d,shade(c,shade),.91F);
        }
    }
    public static void cloud(Sink s,float age,float radius,int kind){
        float[] mesh=CLOUDS[kind];
        if(mesh==null){
            java.util.ArrayList<Float> vertices=new java.util.ArrayList<>();
            buildCloud((x,y,z,c,a)->{vertices.add(x);vertices.add(y);vertices.add(z);vertices.add((float)c);},1,kind);
            mesh=new float[vertices.size()];for(int i=0;i<mesh.length;i++)mesh[i]=vertices.get(i);CLOUDS[kind]=mesh;
        }
        float bob=(float)Math.sin(age*.015)*.2F;
        for(int i=0;i<mesh.length;i+=4)s.vertex(mesh[i]*radius,mesh[i+1]+bob,mesh[i+2]*radius,(int)mesh[i+3],1);
    }
    public static void cloudLightning(Sink s,float age,float radius,int kind){
        if(kind==2&&((int)age%65)<5)lightning(s,age,radius*.35,-2,0,radius*.2,-38,0);
    }
    private static void buildCloud(Sink s,float radius,int kind){
        int color=kind==1?0x424b53:0x25282e;
        billow(s,0,1,0,radius*.67,4.5,radius*.67,shade(color,.88));
        for(int i=0;i<10;i++){
            double angle=i*2.39996,r=Math.sqrt(hash(i+3))*radius*.77;
            double x=Math.cos(angle)*r,z=Math.sin(angle)*r,y=hash(i+44)*4;
            double w=radius*(.14+hash(i+92)*.1),d=radius*(.14+hash(i+72)*.09),h=3+hash(i+6)*4;
            billow(s,x,y,z,w,h,d,color);
        }
    }
    public static void weather(Sink s,float age,float radius,float cameraX,float cameraY,float cameraZ,boolean gale){
        // Keep the storm dense around the viewer while clipping every streak to the actual storm disc.
        for(int i=0;i<(gale?140:85);i++){
            double x=cameraX+(hash(i+100)-.5)*46,z=cameraZ+(hash(i+400)-.5)*46;
            double y=cameraY+((hash(i+200)*28-age*(gale?.85:.6))%28+28)%28-14;
            if(x*x+z*z>radius*radius||y>0||y< -50)continue;
            line(s,x,y,z,x+(gale?1.2:.25),y-2.5,z+.35,.012,0x9db6c6,.32F);
        }
        if(!gale)return;
        for(int i=0;i<80;i++){
            double x=cameraX+((age*1.7+hash(i+600)*50)%50)-25,z=cameraZ+(hash(i+700)-.5)*44;
            double y=cameraY+(hash(i+800)-.5)*24;
            if(x*x+z*z>radius*radius||y>0||y< -50)continue;
            for(int j=0;j<7;j++)line(s,x-j*1.35,y+Math.sin(j*.4+age*.14+i)*.45,z+j*.18,x-(j+1)*1.35,y+Math.sin((j+1)*.4+age*.14+i)*.45,z+(j+1)*.18,.05,0xe2edf0,(1-j/7F)*.5F);
        }
    }
    public static void tornado(Sink s,float age,float size,int mode){
        double height=size*3,spin=age*(mode==1?.21:.14);
        int base=mode==2?0x50596b:mode==1?0x8caaac:0xa6b0b6;
        // Layered tapered shells with a wandering center and uneven turbulent edges.
        for(int layer=0;layer<2;layer++)for(int y=0;y<28;y++)for(int i=0;i<40;i++){
            for(int corner=0;corner<4;corner++){
                double t=(y+(corner>=2?1:0))/28.0,a=(i+(corner==1||corner==2?1:0))*TAU/40+spin+t*7;
                double r=size*(.08+.44*Math.pow(t,1.45))*(1+layer*.18)*(1+.065*Math.sin(a*5+t*13+age*.09));
                double bendX=Math.sin(t*2.5+age*.025)*size*t*.10,bendZ=Math.cos(t*3+age*.02)*size*t*.08;
                double turbulence=.08*Math.sin(t*65-a*3+age*.18);
                v(s,bendX+Math.cos(a)*r,t*height,bendZ+Math.sin(a)*r,shade(base,.68+.22*(Math.cos(a-spin)+1)/2+turbulence),layer==0?.63F:.16F);
            }
        }
        for(int band=0;band<7;band++)for(int i=0;i<64;i++){
            double t=i/64.0,u=(i+1)/64.0;
            double y=(t+band)/7,yy=(u+band)/7,a=t*TAU+spin+band*.7,b=u*TAU+spin+band*.7;
            double r=size*(.10+.48*Math.pow(y,1.45)),rr=size*(.10+.48*Math.pow(yy,1.45));
            line(s,Math.cos(a)*r,y*height,Math.sin(a)*r,Math.cos(b)*rr,yy*height,Math.sin(b)*rr,size*.025,mode==2?0x9ba6c5:0xe0e8e8,.48F);
        }
        for(int i=0;i<22;i++){
            double a=i*2.4+spin*1.4,r=size*(.25+hash(i+40)*.45),y=(hash(i+80)*5+age*.04)%5;
            box(s,Math.cos(a)*r,y,Math.sin(a)*r,.12+hash(i)*.15,.1,.15,0x726c61,.8F);
        }
        if(mode==2&&((int)age%16)<5)for(int i=0;i<3;i++){
            double a=i*TAU/3+Math.floor(age/16);lightning(s,age,Math.cos(a)*size*.5,height,Math.sin(a)*size*.5,Math.cos(a+1)*size*.12,1,Math.sin(a+1)*size*.12);
        }
    }
    public static void gust(Sink s,float age){
        for(int band=0;band<5;band++)for(int i=0;i<32;i++){
            double a=i*TAU/32+age*.16+band*.5,b=a+TAU/32,r=1.1+band*.55;
            line(s,Math.cos(a)*r,Math.sin(a)*r,band*.55,Math.cos(b)*r,Math.sin(b)*r,band*.55,.065,0xdaedef,.26F+band*.04F);
        }
        for(int i=0;i<12;i++){double a=i*TAU/12;line(s,Math.cos(a)*2.3,Math.sin(a)*2.3,-1,Math.cos(a)*2.8,Math.sin(a)*2.8,5,.025,0xefffff,.5F);}
    }
    public static void lightning(Sink s,float age,double x,double y,double z,double xx,double yy,double zz){
        for(int i=0;i<12;i++){
            double t=i/12.0,u=(i+1)/12.0,j=i==0?0:(hash(i+(int)(age/3)*20)-.5)*1.5,k=i==11?0:(hash(i+1+(int)(age/3)*20)-.5)*1.5;
            line(s,x+(xx-x)*t+j,y+(yy-y)*t,z+(zz-z)*t,x+(xx-x)*u+k,y+(yy-y)*u,z+(zz-z)*u,.17,0x709bd7,.25F);
            line(s,x+(xx-x)*t+j,y+(yy-y)*t,z+(zz-z)*t,x+(xx-x)*u+k,y+(yy-y)*u,z+(zz-z)*u,.045,0xe9f5ff,1);
        }
    }
}
