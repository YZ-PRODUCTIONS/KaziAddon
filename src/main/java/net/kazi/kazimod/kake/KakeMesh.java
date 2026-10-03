package net.kazi.kazimod.kake;

/** Bounded, untextured casino geometry. All motion is evaluated on the client. */
public final class KakeMesh {
    public interface Sink {void vertex(double x,double y,double z,int color,float alpha);}
    public static final int GOLD=0xffcb58,PALE=0xfff2c5,RED=0xb9284f,TEAL=0x26c9ba,INK=0x161b2b,WHITE=0xf5f4e9;
    private static final String[] DIGITS={"111101101101111","010110010010111","111001111100111","111001111001111","101101111001001","111100111001111","111100111101111","111001001001001","111101111101111","111101111001111"};
    private KakeMesh(){}
    public static Sink offset(Sink s,double x,double y,double z){return(a,b,c,k,o)->s.vertex(a+x,b+y,c+z,k,o);}
    public static Sink scale(Sink s,double f){return(x,y,z,k,a)->s.vertex(x*f,y*f,z*f,k,a);}
    public static Sink rotate(Sink s,double angle,int axis){
        double c=Math.cos(angle),n=Math.sin(angle);
        if(axis==0)return(x,y,z,k,a)->s.vertex(x,y*c-z*n,y*n+z*c,k,a);
        if(axis==1)return(x,y,z,k,a)->s.vertex(x*c+z*n,y,-x*n+z*c,k,a);
        return(x,y,z,k,a)->s.vertex(x*c-y*n,x*n+y*c,z,k,a);
    }
    public static Sink opacity(Sink s,float opacity){return(x,y,z,k,a)->s.vertex(x,y,z,k,a*opacity);}
    private static int shade(int c,double f){return Math.min(255,(int)((c>>16&255)*f))<<16|Math.min(255,(int)((c>>8&255)*f))<<8|Math.min(255,(int)((c&255)*f));}
    private static void quad(Sink s,int c,float a,double x,double y,double z,double X,double Y,double Z,double u,double v,double w,double U,double V,double W){
        s.vertex(x,y,z,c,a);s.vertex(X,Y,Z,c,a);s.vertex(u,v,w,c,a);s.vertex(U,V,W,c,a);
    }
    public static void face(Sink s,double x,double y,double z,double w,double h,int c,float a){quad(s,c,a,x,y,z,x+w,y,z,x+w,y+h,z,x,y+h,z);}
    public static void box(Sink s,double x,double y,double z,double w,double h,double d,int c){
        face(s,x,y,z+d,w,h,c,1);face(s,x,y,z,w,h,shade(c,.7),1);
        quad(s,shade(c,.82),1,x,y,z,x,y,z+d,x,y+h,z+d,x,y+h,z);
        quad(s,shade(c,.9),1,x+w,y,z,x+w,y+h,z,x+w,y+h,z+d,x+w,y,z+d);
        quad(s,shade(c,1.12),1,x,y+h,z,x,y+h,z+d,x+w,y+h,z+d,x+w,y+h,z);
        quad(s,shade(c,.55),1,x,y,z,x+w,y,z,x+w,y,z+d,x,y,z+d);
    }
    public static void ring(Sink s,double radius,double width,double y,int color,float alpha,int segments){
        for(int i=0;i<segments;i++){double a=i*Math.PI*2/segments,b=(i+1)*Math.PI*2/segments;
            quad(s,color,alpha,Math.cos(a)*radius,y,Math.sin(a)*radius,Math.cos(b)*radius,y,Math.sin(b)*radius,
                    Math.cos(b)*(radius-width),y,Math.sin(b)*(radius-width),Math.cos(a)*(radius-width),y,Math.sin(a)*(radius-width));}
    }
    private static void disk(Sink s,double radius,double z,int color,int segments){
        for(int i=0;i<segments;i++){double a=i*Math.PI*2/segments,b=(i+1)*Math.PI*2/segments;
            quad(s,color,1,0,0,z,Math.cos(a)*radius,Math.sin(a)*radius,z,Math.cos(b)*radius,Math.sin(b)*radius,z,0,0,z);}
    }
    private static void cylinder(Sink s,double r,double thickness,int color,int segments){
        disk(s,r,thickness/2,color,segments);disk(s,r,-thickness/2,shade(color,.75),segments);
        for(int i=0;i<segments;i++){double a=i*Math.PI*2/segments,b=(i+1)*Math.PI*2/segments;
            quad(s,shade(color,(i%2==0)?.72:.9),1,Math.cos(a)*r,Math.sin(a)*r,-thickness/2,Math.cos(b)*r,Math.sin(b)*r,-thickness/2,
                    Math.cos(b)*r,Math.sin(b)*r,thickness/2,Math.cos(a)*r,Math.sin(a)*r,thickness/2);}
    }
    public static void digit(Sink s,int value,double pixel,int color){
        String bits=DIGITS[Math.floorMod(value,10)];
        for(int row=0;row<5;row++)for(int col=0;col<3;col++)if(bits.charAt(row*3+col)=='1')face(s,(col-1.5)*pixel,(2.5-row)*pixel,0,pixel*.88,-pixel*.88,color,1);
    }
    private static void suit(Sink s,int which,double size,int color){
        String[] symbols={"0010001110111110111000100","0101011111111110111000100","0010001110111111010100100","0010001110101011111100100"};
        String bits=symbols[Math.floorMod(which,4)];double p=size/5;
        for(int y=0;y<5;y++)for(int x=0;x<5;x++)if(bits.charAt(y*5+x)=='1')face(s,(x-2.5)*p,(2.5-y)*p,0,p,-p,color,1);
    }
    public static void coin(Sink s,boolean glow){
        if(glow){ring(rotate(s,Math.PI/2,0),.56,.075,0,GOLD,.22F,16);return;}
        cylinder(s,.44,.10,0xd39630,16);disk(s,.375,.052,GOLD,16);disk(s,.30,.054,0xe7ab39,16);
        suit(offset(s,0,0,.057),0,.42,PALE);suit(rotate(offset(s,0,0,-.057),Math.PI,1),0,.42,GOLD);
        for(int i=0;i<12;i++){double a=i*Math.PI/6;face(s,Math.cos(a)*.34-.015,Math.sin(a)*.34-.015,.058,.03,.03,PALE,1);}
    }
    public static void chip(Sink s,int seed,boolean glow){
        int color=(seed&1)==0?RED:TEAL;
        if(glow){ring(rotate(s,Math.PI/2,0),.59,.07,0,color,.2F,16);return;}
        cylinder(s,.5,.13,color,16);disk(s,.35,.068,INK,16);disk(s,.30,.07,color,16);
        for(int i=0;i<8;i++){Sink edge=rotate(s,i*Math.PI/4,2);face(edge,-.055,.365,.071,.11,.12,WHITE,1);}
        suit(offset(s,0,0,.073),seed,.37,PALE);disk(s,.32,-.069,INK,16);digit(rotate(offset(s,0,0,-.071),Math.PI,1),7,.065,GOLD);
    }
    public static void card(Sink s,int seed,boolean glow){
        if(glow){face(s,-.39,-.58,-.014,.78,1.16,TEAL,.10F);return;}
        box(s,-.35,-.53,-.015,.7,1.06,.03,INK);face(s,-.32,-.50,.017,.64,1,WHITE,1);
        int color=(seed&1)==0?RED:INK;suit(offset(s,0,0,.019),seed,.40,color);
        digit(offset(s,-.23,.35,.02),seed%9+1,.045,color);digit(rotate(offset(s,.23,-.35,.02),Math.PI,2),seed%9+1,.045,color);
        Sink back=rotate(s,Math.PI,1);face(back,-.32,-.50,.017,.64,1,TEAL,1);face(back,-.27,-.45,.019,.54,.90,INK,1);
        for(int i=-2;i<=2;i++)suit(offset(back,0,i*.16,.022),0,.13,GOLD);
    }
    private static void diceFace(Sink s,int value){
        face(s,-.43,-.43,.502,.86,.86,WHITE,1);
        for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++){
            boolean pip=(value%2==1&&x==0&&y==0)||(value>=2&&x==y&&x!=0)||(value>=4&&x==-y&&x!=0)||(value==6&&x!=0&&y==0);
            if(pip)face(s,x*.25-.068,y*.25-.068,.505,.136,.136,INK,1);
        }
    }
    public static void dice(Sink s,boolean glow){
        if(glow){ring(s,.86,.035,-.54,GOLD,.25F,20);return;}
        box(s,-.5,-.5,-.5,1,1,1,GOLD);
        int[] faces={1,2,6,5};for(int i=0;i<4;i++)diceFace(rotate(s,i*Math.PI/2,1),faces[i]);
        diceFace(rotate(s,Math.PI/2,0),3);diceFace(rotate(s,-Math.PI/2,0),4);
    }
    public static double reelPosition(float age,float reveal,int result,int reel){
        if(reveal<0||result<0)return age*.95+reel*2.73;
        double start=reveal*.95+reel*2.73,end=Math.ceil((start+result)/10)*10-result+10;
        double t=Math.max(0,Math.min(1,(age-reveal)/(6.0+reel*3)));
        return start+(end-start)*(1-Math.pow(1-t,3));
    }
    public static void slot(Sink s,float age,float reveal,int result,boolean glow){
        if(glow){
            int color=result==0?RED:result>=7?TEAL:GOLD;
            for(int i=0;i<17;i++){double x=i<9?-.9+i*.225:(i<13?-.98:.98),y=i<9?2.25:.55+(i%4)*.38;
                float a=(float)(.4+.25*Math.sin(age*.7-i));box(opacity(s,a),x-.035,y-.035,.46,.07,.07,.025,color);}
            if(result>=0&&age>=reveal&&age-reveal<18){double r=.7+(age-reveal)*.10;ring(s,r,.09,.1,color,(float)Math.max(0,.5-(age-reveal)/36),32);}
            return;
        }
        box(s,-1.04,0,-.5,2.08,.20,1.03,INK);box(s,-.97,.2,-.45,1.94,.26,.9,0x852541);
        box(s,-.91,.46,-.48,1.82,1.68,.21,INK);
        box(s,-1,.46,-.4,.25,1.60,.83,RED);box(s,.75,.46,-.4,.25,1.60,.83,RED);
        box(s,-1.02,1.91,-.44,2.04,.34,.9,GOLD);box(s,-.92,1.98,.47,1.84,.20,.04,INK);
        box(s,-.80,.58,.27,1.60,.25,.19,GOLD);box(s,-.73,.63,.47,1.46,.13,.04,INK);
        box(s,-.78,.83,-.22,1.56,.18,.67,0x861f39);box(s,-.76,1.64,-.2,1.52,.20,.63,RED);
        box(s,-.81,.98,.4,.04,.66,.06,GOLD);box(s,.77,.98,.4,.04,.66,.06,GOLD);
        for(int reel=0;reel<3;reel++){
            double x=(reel-1)*.50;Sink drum=offset(s,x,1.32,.07);
            double position=reelPosition(age,reveal,result,reel);
            for(int d=0;d<10;d++){
                Sink panel=rotate(drum,(d+position)*Math.PI*2/10,0);
                face(panel,-.225,-.10,.31,.45,.20,WHITE,1);
                digit(offset(panel,0,0,.313),d,.037,d==0?RED:d>=7?0x128576:INK);
            }
            if(result>=0&&age-reveal>=12){
                face(s,x-.225,1.105,.405,.45,.43,WHITE,1);
                digit(offset(s,x,1.32,.409),result,.074,result==0?RED:result>=7?0x128576:INK);
            }
            if(reel<2)box(s,x+.23,.97,.38,.04,.7,.08,GOLD);
            digit(offset(s,(reel-1)*.19,2.075,.514),result>=0?result:7,.027,GOLD);
        }
        box(s,1.00,.86,-.07,.13,.28,.2,GOLD);
        Sink lever=rotate(offset(s,1.12,1.02,.04),-.30-Math.sin(Math.min(age,12)/12*Math.PI)*.8,2);
        box(lever,-.035,0,-.035,.07,.74,.07,0xc2c7cf);box(lever,-.10,.67,-.10,.20,.20,.20,TEAL);
        box(s,-.33,.32,.46,.66,.13,.10,GOLD);box(s,-.26,.36,.565,.52,.035,.01,INK);
    }
    private static void roulette(Sink s,double radius,float age,float alpha,boolean glow,int segments){
        if(!glow)return;
        ring(s,radius,Math.max(.11,radius*.009),.04,GOLD,alpha,segments);
        ring(s,radius+.12,Math.max(.18,radius*.019),.035,GOLD,alpha*.2F,segments);
        ring(s,radius*.88,Math.max(.055,radius*.004),.05,PALE,alpha*.85F,segments);
        for(int i=0;i<segments;i++){
            double a=i*Math.PI*2/segments,b=(i+.86)*Math.PI*2/segments,r=radius*.97,in=radius*.91;
            quad(s,i%3==0?TEAL:i%2==0?RED:GOLD,alpha*.85F,Math.cos(a)*r,.06,Math.sin(a)*r,Math.cos(b)*r,.06,Math.sin(b)*r,
                    Math.cos(b)*in,.06,Math.sin(b)*in,Math.cos(a)*in,.06,Math.sin(a)*in);
        }
    }
    private static void helix(Sink s,double radius,double height,float age,int color,float alpha,int segments){
        for(int i=0;i<segments;i++){
            double a=i/(double)segments*Math.PI*4+age*.025,b=(i+1)/(double)segments*Math.PI*4+age*.025;
            double y=i/(double)segments*height,Y=(i+1)/(double)segments*height;
            quad(s,color,alpha*.6F,Math.cos(a)*radius,y,Math.sin(a)*radius,Math.cos(b)*radius,Y,Math.sin(b)*radius,
                    Math.cos(b)*radius,Y+.95,Math.sin(b)*radius,Math.cos(a)*radius,y+.95,Math.sin(a)*radius);
            quad(s,color,Math.min(1,alpha*2),Math.cos(a)*radius,y+.38,Math.sin(a)*radius,Math.cos(b)*radius,Y+.38,Math.sin(b)*radius,
                    Math.cos(b)*radius,Y+.54,Math.sin(b)*radius,Math.cos(a)*radius,y+.54,Math.sin(a)*radius);
        }
    }
    public static void effect(Sink s,int kind,int variant,float age,float life,float radius,boolean glow,boolean far){
        float fade=(float)Math.min(1,Math.min(age/5,Math.max(0,(life-age)/10)));
        s=opacity(s,fade);int segments=far?24:64;
        if(kind==0)return;
        if(kind==1||kind==8){
            if(glow){double r=.3+age*(kind==8?.24:.14);ring(s,r,.10,.09,variant==3?TEAL:GOLD,(float)Math.max(0,1-age/life),32);
                for(int i=0;i<8;i++){Sink ray=rotate(s,i*Math.PI/4,1);quad(ray,PALE,fade,0,.1,r*.75,.07,.1,r,0,.1,r*1.35,-.07,.1,r);}}
            if(kind==8){
                if(variant==3&&glow){
                    for(int i=0;i<2;i++){Sink slash=rotate(offset(s,0,.8,0),(i==0?1:-1)*.7,2);
                        face(slash,-2.5,-.07,.01,5,.14,TEAL,Math.max(0,1-age/life));face(slash,-2,-.02,.02,4,.04,PALE,Math.max(0,1-age/life));}
                }else for(int i=0;i<6;i++){
                    double a=i*Math.PI/3,r=age*.12,y=.3+Math.sin(Math.min(1,age/life)*Math.PI)*1.4;
                    Sink shard=scale(rotate(offset(s,Math.cos(a)*r,y,Math.sin(a)*r),age*.18+i,1),Math.max(.02,.24*(1-age/life)));
                    if(variant==2||variant==7)dice(shard,glow);else if(variant==6)chip(shard,i,glow);else coin(shard,glow);
                }
            }
            return;
        }
        if(kind==2){
            for(int i=0;i<2;i++){Sink d=rotate(offset(s,Math.cos(age*.10+i*Math.PI)*1.15,1.4,Math.sin(age*.10+i*Math.PI)*1.15),age*.12,1);dice(scale(d,.6),glow);}
            if(glow)roulette(s,1.6,age,.65F,true,32);return;
        }
        if(kind==3){
            Sink muzzle=rotate(offset(s,0,1.3,1.1),Math.PI/2,0);
            if(glow){ring(muzzle,.75,.09,0,GOLD,.7F,32);ring(muzzle,.95,.04,0,TEAL,.5F,32);}
            for(int i=0;i<6;i++){double a=age*.18+i*Math.PI/3;coin(scale(offset(s,Math.cos(a)*.75,1.3+Math.sin(a)*.75,1.1),.33),glow);}return;
        }
        if(kind==4){
            roulette(offset(s,0,20,0),24,age,.6F,glow,segments);
            if(glow)for(int i=0;i<(far?12:32);i++){double a=i*2.39996,r=4+(i%7)*2.8,y=20-((age*.45+i*2)%20);Sink beam=offset(s,Math.cos(a)*r,y,Math.sin(a)*r);face(beam,-.075,-1.7,0,.15,3.4,i%2==0?TEAL:GOLD,.5F);face(rotate(beam,Math.PI/2,1),-.075,-1.7,0,.15,3.4,PALE,.2F);}
            return;
        }
        if(kind==5){
            roulette(s,4,age,.8F,glow,32);if(glow)digit(offset(scale(s,2),0,1,.05),7,.25,PALE);return;
        }
        if(kind==6){
            roulette(s,radius,age,.55F,glow,segments);
            if(glow){for(int i=0;i<3;i++)helix(rotate(s,i*Math.PI*2/3,1),radius,30,age,i==1?TEAL:GOLD,.3F,segments);
                ring(s,radius,.09,30,GOLD,.4F,segments);}
            for(int i=0;i<(far?8:24);i++){
                double a=i*Math.PI*2/(far?8:24)+age*.017;double y=2+(i%4)*7+Math.sin(age*.05+i);
                Sink token=rotate(offset(s,Math.cos(a)*(radius-1),y,Math.sin(a)*(radius-1)),-a,1);
                if(i%3==0)chip(scale(token,1.5),i,glow);else card(scale(token,2.4),i,glow);
            }return;
        }
        if(kind==7){
            roulette(s,radius,age,.28F,glow,segments);roulette(s,2.7,age,.85F,glow,32);
            for(int i=0;i<8;i++){double a=i*Math.PI/4+age*.025;coin(scale(rotate(offset(s,Math.cos(a)*2,1.0+Math.sin(age*.07+i)*.4,Math.sin(a)*2),a,1),.7),glow);}
            if(glow){
                for(int i=0;i<12;i++){double a=i*Math.PI/6;Sink ray=rotate(s,a,1);quad(ray,GOLD,.22F,-.035,.2,2,.035,.2,2,.10,4.5+(i%3),3,-.10,4.5+(i%3),3);}
                for(int i=0;i<3;i++)digit(offset(s,(i-1)*.45,3.2,0),7,.10,PALE);
            }
        }
    }
    public static void trail(Sink s,int kind,float age,boolean far){
        int color=kind==2?TEAL:kind==3?RED:GOLD;
        double length=kind==1?1.3:kind==3?2.5:2;
        quad(s,color,.24F,-.20,0,-.10,.20,0,-.10,.035,0,-length,-.035,0,-length);
        quad(s,color,.16F,0,-.20,-.10,0,.20,-.10,0,.035,-length,0,-.035,-length);
        quad(s,PALE,.48F,-.045,0,-.10,.045,0,-.10,0,0,-length*1.2,0,0,-length*1.2);
        if(!far)ring(rotate(s,Math.PI/2,0),.55,.025,-((age*.14)%1.5),color,.2F,16);
    }
}
