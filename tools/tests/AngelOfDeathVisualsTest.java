import net.kazi.kazimod.renderers.entities.asriel.AngelOfDeathVisuals;
import net.kazi.kazimod.renderers.entities.asriel.AsrielVisualGeometry;

/** Checks actual beam mesh bounds, camera-facing heads, curved trails and detail budgets. */
public final class AngelOfDeathVisualsTest {
    private static int checks;
    private static final int[] peaks = new int[3];
    public static void main(String[] args) {
        double[][] cameras={{0,0,40},{40,0,0},{0,40,0},{0,0,0},{20,15,-30},{-40,-10,5}};
        for(int points:new int[]{1,2,6,14,32}) for(int bend=0;bend<7;bend++) {
            double[][] trail=new double[points][3];
            for(int i=0;i<points;i++) {
                double t=i/Math.max(1.0,points-1.0),angle=t*bend*.45;
                trail[i][0]=bend==0?0:Math.sin(angle)*6;
                trail[i][1]=Math.sin(t*Math.PI)*bend*.3;
                trail[i][2]=-t*15;
            }
            for(double[] camera:cameras) for(int tint=-1;tint<8;tint++) {
                int previous=Integer.MAX_VALUE;
                for(int detail=0;detail<3;detail++) {
                    Probe probe=new Probe(trail);
                    AngelOfDeathVisuals.render(probe,trail,17.5F,tint,detail,camera[0],camera[1],camera[2]);
                    require(probe.vertices%4==0&&probe.vertices>0,"Active beams must submit complete visible faces");
                    require(probe.vertices<previous,"Each lower detail must reduce the geometry count");
                    previous=probe.vertices;
                    peaks[detail]=Math.max(peaks[detail],probe.vertices/4);
                    require(probe.vertices/4<=(detail==0?150:detail==1?90:50),"Beam exceeds its detail budget");
                    if(points>1) require(probe.minZ<-14.9,"Tail must follow the supplied historic path all the way to its end");
                }
            }
        }
        for(float age:new float[]{-1,Float.NaN,Float.POSITIVE_INFINITY}) {
            Probe p=new Probe(new double[][]{{0,0,0}});
            AngelOfDeathVisuals.render(p,new double[][]{{0,0,0}},age,0,0,0,0,20);
            require(p.vertices==0,"Invalid age cannot emit geometry");
        }
        for(double[][] invalid:new double[][][]{null,new double[0][],{{0,Double.NaN,0}},{{0,0}},{{0,0,0},null}}) {
            Probe p=new Probe(new double[][]{{0,0,0}});
            AngelOfDeathVisuals.render(p,invalid,4,0,0,0,0,20);
            require(p.vertices==0,"Invalid history cannot emit geometry");
        }
        // Every head face is a rim: no triangle covers the center of a nose viewed straight on.
        final double[][] pending=new double[4][2]; final int[] count={0};
        AngelOfDeathVisuals.render((x,y,z,r,g,b,a)->{
            pending[count[0]++]=new double[]{x,y};
            if(count[0]==4) {
                require(!coversCenter(pending[0],pending[1],pending[2])&&!coversCenter(pending[0],pending[2],pending[3]),
                        "The reference's hollow oval must stay open instead of filling into a solid dot");
                count[0]=0;
            }
        },new double[][]{{0,0,0}},4,0,0,0,0,20);
        System.out.println("Angel of death visuals: "+checks+" checks passed; peak quads "+peaks[0]+"/"+peaks[1]+"/"+peaks[2]+".");
    }
    private static boolean coversCenter(double[] a,double[] b,double[] c) {
        double den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1]);
        if(Math.abs(den)<1e-12)return false;
        double u=((b[1]-c[1])*(-c[0])+(c[0]-b[0])*(-c[1]))/den;
        double v=((c[1]-a[1])*(-c[0])+(a[0]-c[0])*(-c[1]))/den;
        return u>=0&&v>=0&&u+v<=1;
    }
    private static void require(boolean pass,String message) {checks++;if(!pass)throw new AssertionError(message);}
    private static final class Probe implements AsrielVisualGeometry.Sink {
        final double[] min={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY};
        final double[] max={Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
        int vertices;double minZ=Double.POSITIVE_INFINITY;
        Probe(double[][] path) {for(double[] p:path)for(int i=0;i<3;i++){min[i]=Math.min(min[i],p[i]);max[i]=Math.max(max[i],p[i]);}}
        public void vertex(double x,double y,double z,float r,float g,float b,float a) {
            require(Double.isFinite(x+y+z)&&Float.isFinite(r+g+b+a),"No camera or curve may generate NaN geometry");
            require(r>=0&&r<=1&&g>=0&&g<=1&&b>=0&&b<=1&&a>=0&&a<=1,"Colors and opacity stay bounded");
            double[] p={x,y,z};
            for(int i=0;i<3;i++)require(p[i]>=min[i]-AngelOfDeathVisuals.PADDING&&p[i]<=max[i]+AngelOfDeathVisuals.PADDING,
                    "The mesh must stay within the history-based frustum bounds");
            minZ=Math.min(minZ,z);vertices++;
        }
    }
}
