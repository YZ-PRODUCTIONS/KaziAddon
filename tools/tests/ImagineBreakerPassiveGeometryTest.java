import net.kazi.kazimod.abilities.NagiAwakening.ImagineBreakerNegationGeometry;

/** Tests the production contact geometry without loading Minecraft. */
public final class ImagineBreakerPassiveGeometryTest {
    private static int checks;

    public static void main(String[] args) {
        point(0, 0, 20, 0, 0, 20, 0, 0, 0, 0, 0, 100, 2, "inside distant beam");
        point(2, 0, 80, 5, 0, 80, 0, 0, 0, 0, 0, 100, 2, "side contact far from muzzle");
        point(1, 0, 100, 1, 0, 104, 0, 0, 0, 0, 0, 100, 2, "flat far cap");
        point(2, 0, 100, 4, 0, 102, 0, 0, 0, 0, 0, 100, 2, "cap rim");
        point(1, 0, 0, 1, 0, -4, 0, 0, 0, 0, 0, 100, 2, "flat muzzle cap");
        check(contact(5,0,80,0,0,0,0,0,100,2), "three-block side boundary included");
        check(!contact(5.001,0,80,0,0,0,0,0,100,2), "outside side boundary excluded");
        check(!contact(0,0,104,0,0,0,0,0,100,2), "no rounded cap overreach");
        check(contact(0,0,103,0,0,0,0,0,100,2), "three-block cap boundary included");
        check(!contact(0,0,-3.001,0,0,0,0,0,100,2), "behind muzzle outside sphere excluded");
        point(2, 80, 0, 5, 80, 0, 0, 0, 0, 0, 100, 0, 2, "vertical beam");
        point(80, 0, 2, 80, 0, 5, 0, 0, 0, 100, 0, 0, 2, "horizontal beam");
        point(10, 20, 80, 15, 20, 80, 10, 20, 30, 10, 20, 130, 0, "zero-width ray");

        // Rotating or reversing the finite cylinder must preserve the same contact distance.
        for (int turn=0; turn<24; turn++) {
            double angle=turn*Math.PI/12, dx=Math.sin(angle), dz=Math.cos(angle);
            double px=dx*70+dz*5, pz=dz*70-dx*5;
            double[] q=closest(px,4,pz,0,4,0,dx*100,4,dz*100,2);
            close(distance(px,4,pz,q),3,"rotated boundary distance");
            double[] reverse=closest(px,4,pz,dx*100,4,dz*100,0,4,0,2);
            close(distance(px,4,pz,reverse),3,"reversed boundary distance");
            close(q[0],reverse[0],"reversing preserves contact x");
            close(q[2],reverse[2],"reversing preserves contact z");
        }
        check(closest(1,2,3,0,0,0,0,0,0,2)==null,"zero-length beam has no volume");
        check(closest(1,2,3,0,0,0,0,0,100,-1)==null,"negative radius rejected");
        for(double bad:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}) {
            check(closest(bad,0,0,0,0,0,0,0,100,2)==null,"invalid target rejected");
            check(closest(0,0,0,bad,0,0,0,0,100,2)==null,"invalid origin rejected");
            check(closest(0,0,0,0,0,0,0,0,bad,2)==null,"invalid end rejected");
            check(closest(0,0,0,0,0,0,0,0,100,bad)==null,"invalid radius rejected");
        }
        check(ImagineBreakerNegationGeometry.withinRadius(0,-3,0,3),"sphere covers below player");
        check(!ImagineBreakerNegationGeometry.withinRadius(2,2,2,3),"sphere excludes bounding-box corners");
        check(!ImagineBreakerNegationGeometry.withinRadius(Double.NaN,0,0,3),"invalid contact rejected");
        System.out.println("Imagine Breaker passive geometry: "+checks+" checks passed.");
    }

    private static double[] closest(double px,double py,double pz,double sx,double sy,double sz,
            double ex,double ey,double ez,double radius) {
        return ImagineBreakerNegationGeometry.closestCylinderPoint(px,py,pz,sx,sy,sz,ex,ey,ez,radius);
    }
    private static boolean contact(double px,double py,double pz,double sx,double sy,double sz,
            double ex,double ey,double ez,double radius) {
        double[] q=closest(px,py,pz,sx,sy,sz,ex,ey,ez,radius);
        return q!=null&&ImagineBreakerNegationGeometry.withinRadius(px-q[0],py-q[1],pz-q[2],3);
    }
    private static void point(double x,double y,double z,double px,double py,double pz,
            double sx,double sy,double sz,double ex,double ey,double ez,double radius,String label) {
        double[] q=closest(px,py,pz,sx,sy,sz,ex,ey,ez,radius);
        check(q!=null,label+" exists"); close(q[0],x,label+" x"); close(q[1],y,label+" y"); close(q[2],z,label+" z");
    }
    private static double distance(double x,double y,double z,double[] q) {
        check(q!=null,"contact exists");
        return Math.sqrt((x-q[0])*(x-q[0])+(y-q[1])*(y-q[1])+(z-q[2])*(z-q[2]));
    }
    private static void close(double actual,double expected,String label) { check(Math.abs(actual-expected)<1E-8,label); }
    private static void check(boolean value,String label) { checks++; if(!value)throw new AssertionError(label); }
}
