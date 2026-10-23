package net.kazi.kazimod.bomu;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.Vector3d;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;

public final class BomuVisuals {
    private BomuVisuals() {}

    public static void explode(ExplosionAbility explosion) { explode(explosion, true); }

    public static void explode(ExplosionAbility explosion, boolean visible) {
        ((BomuExplosionVisuals) explosion).kazi$bomuVisual(visible);
        explosion.doExplosion();
    }

    public static BomuVfxEntity hands(LivingEntity owner, int life) {
        BomuVfxEntity effect = BomuVfxEntity.spawn(owner.level, owner.getX(), owner.getY(), owner.getZ(),
                .45F, BomuVfxEntity.HANDS, life);
        if (effect != null) effect.attach(owner);
        return effect;
    }

    public static void stop(BomuVfxEntity effect) {
        if (effect != null && !effect.removed) effect.remove();
    }

    public static void propel(LivingEntity owner) {
        Vector3d direction = owner.getLookAngle().scale(-1);
        Vector3d origin = owner.position().add(0, owner.getBbHeight() * .45, 0).add(direction.scale(.6));
        BomuVfxEntity effect = BomuVfxEntity.spawn(owner.level, origin.x, origin.y, origin.z, 1.7F,
                BomuVfxEntity.JET, 14);
        if (effect != null) effect.direction(direction);
    }
}
