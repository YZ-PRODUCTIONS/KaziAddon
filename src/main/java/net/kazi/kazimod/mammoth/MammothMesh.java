package net.kazi.kazimod.mammoth;

public final class MammothMesh {
    public interface Sink{void vertex(double x,double y,double z,int color,float alpha);}
    private static final double TAU=Math.PI*2;
    private static double clamp(double p){return Math.max(0,Math.min(1,p));}
    private static void quad(Sink s,double[]a,double[]b,double[]c,double[]d,int color,float alpha){for(double[]p:new double[][]{a,b,c,d})s.vertex(p[0],p[1],p[2],color,alpha);}
    private static void strip(Sink s,double x,double y,double z,double X,double Y,double Z,double width,int color,float alpha){
        double dx=X-x,dz=Z-z,len=Math.hypot(dx,dz);double nx=len<.001?width:dz/len*width,nz=len<.001?0:-dx/len*width;
        quad(s,new double[]{x-nx,y,z-nz},new double[]{X-nx,Y,Z-nz},new double[]{X+nx,Y,Z+nz},new double[]{x+nx,y,z+nz},color,alpha);
    }
    private static void ring(Sink s,double radius,double y,double width,int color,float alpha,int sides){
        for(int i=0;i<sides;i++){double a=TAU*i/sides,b=TAU*(i+1)/sides;
            quad(s,new double[]{Math.cos(a)*radius,y,Math.sin(a)*radius},new double[]{Math.cos(b)*radius,y,Math.sin(b)*radius},
                    new double[]{Math.cos(b)*(radius+width),y,Math.sin(b)*(radius+width)},new double[]{Math.cos(a)*(radius+width),y,Math.sin(a)*(radius+width)},color,alpha);}
    }
    private static void rock(Sink s,double x,double y,double z,double size,double tilt,int color,float a){
        double[][]p={{-size,0,-size},{size,0,-size},{size,0,size},{-size,0,size},{-size,size*.38,-size},{size,size*.38,-size},{size,size*.38,size},{-size,size*.38,size}};
        for(double[]v:p){double X=v[0]*Math.cos(tilt)-v[1]*Math.sin(tilt),Y=v[0]*Math.sin(tilt)+v[1]*Math.cos(tilt);v[0]=X+x;v[1]=Y+y;v[2]+=z;}
        quad(s,p[4],p[5],p[6],p[7],color,a);quad(s,p[0],p[1],p[5],p[4],0x403a32,a);quad(s,p[1],p[2],p[6],p[5],0x54493d,a);quad(s,p[2],p[3],p[7],p[6],0x3a362e,a);quad(s,p[3],p[0],p[4],p[7],0x645849,a);
    }
    public static void ground(Sink s,float age,float life,float radius,boolean finale,boolean soft,boolean far,float[]heights,int[]colors){
        double p=clamp(age/life),travel=clamp(p*1.8),r=radius*travel;float alpha=(float)((1-p)*(1-p));
        int count=far?12:24;
        if(soft){ring(s,r,.085,finale?.7:.28,0xe9dec1,alpha*.65F,far?40:72);ring(s,Math.max(0,r-1.5),.045,.5,0xc6bd9f,alpha*.12F,40);return;}
        for(int i=0;i<count;i++){
            int id=far?i*2:i;double a=TAU*id/24,rad=radius*(.23+.67*((id*17%23)/23.0));
            double hit=clamp((r-rad)/2),lift=Math.sin(hit*Math.PI)*(finale?2.4:.6)*(1-p),h=heights==null?0:heights[id];
            if(r>rad&&p<.94)rock(s,Math.cos(a)*rad,h+lift,Math.sin(a)*rad,finale?.65:.38,Math.sin(age*.25+id)*lift*.6,colors==null?0x827665:colors[id],alpha);
            double inner=radius*.18,outer=Math.min(r,rad+2),bend=a+.065*Math.sin(id*13);
            if(outer>inner)strip(s,Math.cos(a)*inner,h+.04,Math.sin(a)*inner,Math.cos(bend)*outer,h+.04,Math.sin(bend)*outer,finale?.085:.035,0x24211e,alpha);
        }
    }
    public static void wind(Sink s,float age,boolean soft){
        int count=36;double width=soft?.22:.085;float alpha=soft?.23F:.93F;
        for(int i=0;i<count;i++){
            double a=-1.35+2.7*i/count,b=-1.35+2.7*(i+1)/count;
            double x=Math.sin(a)*3.1,X=Math.sin(b)*3.1,y=(Math.cos(a)-.45)*1.15,Y=(Math.cos(b)-.45)*1.15;
            quad(s,new double[]{x,y-width,0},new double[]{X,Y-width,0},new double[]{X,Y+width,0},new double[]{x,y+width,0},soft?0xc8f5ff:0xf6fcff,alpha);
            if(soft)strip(s,x,y,-.15,X,Y,-1.0-.35*Math.cos(age*.4+i),.1,0xc4edf5,.12F);
        }
        if(!soft)for(int i=0;i<7;i++){double x=-2.5+i*.83;strip(s,x,.2,-.4,x*.8,.15,-2.4,.035,0xe6f5ff,.65F);}
    }
    public static void vacuum(Sink s,float age,float life,double tx,double ty,double tz,boolean soft,boolean far){
        int strands=far?10:20,steps=far?7:12;float envelope=(float)Math.min(1,Math.min(age/8,(life-age)/8));
        for(int i=0;i<strands;i++){
            double angle=TAU*i/strands,phase=(age*.032+i*.173)%1;
            for(int j=0;j<steps;j++){
                double a=Math.max(0,phase-.3+j*.3/steps),b=Math.max(0,phase-.3+(j+1)*.3/steps);
                if(b<=a)continue;double[]p=flow(a,angle,tx,ty,tz),q=flow(b,angle,tx,ty,tz);
                strip(s,p[0],p[1],p[2],q[0],q[1],q[2],soft?.16:.045,soft?0xc6e5e3:0xf4f5e8,envelope*(soft?.12F:.66F)*(float)Math.sin(Math.PI*j/steps));
            }
        }
    }
    private static double[]flow(double p,double angle,double tx,double ty,double tz){
        double r=MammothCombatMath.VACUUM_RADIUS*(1-p),a=angle+p*1.7;
        return new double[]{Math.cos(a)*r+tx*p,(.7+2.2*(.5+.5*Math.sin(angle*3)))*(1-p)+ty*p,Math.sin(a)*r+tz*p};
    }
}
