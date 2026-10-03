package net.kazi.kazimod.mahoraga;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Bounded, per-opponent observations rather than random aggression switches. */
public final class CombatMemory {
    private final Map<UUID, Profile> opponents = new LinkedHashMap<>();
    public static final class Profile {
        public double aggression, ranged, retreat;
        public int samples;
        private double lastDistance = -1;
        public void sample(double distance, boolean attacking, boolean usingRanged) {
            aggression = aggression * .93 + (attacking && distance < 7 ? .18 : distance < 5 ? .08 : 0);
            ranged = ranged * .95 + (usingRanged ? .22 : 0);
            retreat = retreat * .93 + (lastDistance >= 0 && distance > lastDistance + .6 ? .16 : 0);
            lastDistance = distance;
            samples++;
        }
        public boolean aggressive() { return aggression > .65; }
        public boolean kiting() { return retreat > .45 || ranged > .65; }
    }
    public Profile opponent(UUID id) {
        if (!opponents.containsKey(id) && opponents.size() >= 32) opponents.remove(opponents.keySet().iterator().next());
        return opponents.computeIfAbsent(id, key -> new Profile());
    }
    public static boolean incoming(double dx, double dy, double dz, double vx, double vy, double vz, double radius) {
        double speed = vx*vx + vy*vy + vz*vz;
        if (speed < .01) return false;
        double t = -(dx*vx + dy*vy + dz*vz) / speed;
        if (t < 0 || t > 10) return false;
        double x=dx+vx*t, y=dy+vy*t, z=dz+vz*t;
        return x*x + y*y + z*z < radius*radius;
    }
}
