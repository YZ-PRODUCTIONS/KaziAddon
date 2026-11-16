import net.kazi.kazimod.abilities.SupaRework.MarbleBoundaryMath;

public final class MarbleBoundaryMathTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static double clip(double x, double dx) {
        return MarbleBoundaryMath.fraction(x,0,0,dx,0,0,90,1);
    }
    public static void main(String[] args) {
        check(MarbleBoundaryMath.teleportExits(0, 10000, 90), "Teleport escape blocked");
        check(MarbleBoundaryMath.teleportExits(100, 8100, 90), "Teleport onto outer shell blocked");
        check(!MarbleBoundaryMath.teleportExits(10000, 0, 90), "Teleport entry allowed");
        check(!MarbleBoundaryMath.teleportExits(100, 400, 90), "Teleport within marble allowed");
        check(!MarbleBoundaryMath.teleportExits(10000, 40000, 90), "Outside teleport allowed");
        check(!MarbleBoundaryMath.teleportExits(0, 10000, 0), "Inactive radius cannot trap entities");
        check(clip(95,-10)==1, "Entry must remain unobstructed");
        check(clip(95,10)==1, "Outside movement remains free");
        check(clip(0,10)==1, "Inside movement remains free");
        check(clip(85,10)<.4, "Exit stops before hitbox crosses shell");
        check(clip(89.5,1)==0, "An overlapping hitbox cannot move outward");
        check(clip(89.5,-1)==1, "An overlapping hitbox can move inward");
        check(clip(89.5,-200)<.9, "A fast inward move cannot escape opposite wall");
        check(clip(100,-300)<.64, "Outside-to-outside tunneling stops at exit");
        check(MarbleBoundaryMath.fraction(0,85,0,0,10,0,90,1)<.4, "Roof blocks exit");
        check(MarbleBoundaryMath.fraction(0,-85,0,0,-10,0,90,1)<.4, "Underground shell blocks exit");
        for (int angle=0;angle<360;angle++) {
            double x=Math.cos(Math.toRadians(angle)),z=Math.sin(Math.toRadians(angle));
            double t=MarbleBoundaryMath.fraction(x*85,0,z*85,x*20,0,z*20,90,1);
            check(t>=0 && t<.201, "All directions contain outward movement");
        }
        System.out.println("376 one-way Reality Marble movement and teleport checks passed.");
    }
}
