package net.kazi.kazimod.goro;

import net.kazi.kazimod.abilities.GoroRework.*;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;

public final class GoroVisuals {
    private static final ThreadLocal<Boolean> BLUE_IMPACT = new ThreadLocal<>();
    private GoroVisuals() {}

    public static boolean isBlueImpact() { return Boolean.TRUE.equals(BLUE_IMPACT.get()); }

    // The native explosion particle callback has no owner, so carry its color through this call only.
    public static void blueImpact(xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility explosion) {
        Boolean previous = BLUE_IMPACT.get();
        BLUE_IMPACT.set(true);
        try { explosion.doExplosion(); }
        finally {
            if (previous == null) BLUE_IMPACT.remove();
            else BLUE_IMPACT.set(previous);
        }
    }

    public static int kind(LightningEntity bolt) {
        boolean pillar = bolt.getParent() == ElThorRework.INSTANCE || bolt.getParent() == RaigoRework.INSTANCE
                || bolt.getParent() == xyz.pixelatedw.mineminenomi.abilities.goro.ElThorAbility.INSTANCE
                || bolt.getParent() == xyz.pixelatedw.mineminenomi.abilities.goro.RaigoAbility.INSTANCE;
        if (!pillar) return 0;
        return bolt.getDamage() <= 0 ? -1 : 1;
    }

    public static void mark(LightningEntity bolt) {
        ((GoroBoltVisuals) bolt).kazi$goroKind(kind(bolt));
    }
}
