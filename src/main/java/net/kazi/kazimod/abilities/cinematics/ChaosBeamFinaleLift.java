package net.kazi.kazimod.abilities.cinematics;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

/** Gentle, collision-aware ascent shared by the server and the caster's cinematic camera client. */
public final class ChaosBeamFinaleLift {
    public static final double HEIGHT = 6;
    public static final int RISE_TICKS = 288;
    private static final double MAX_STEP = .08;
    private static final String GRAVITY_TAG = "kazimodChaosFinaleGravityLock";
    private final LivingEntity caster;
    private final World world;
    private final Vector3d origin;
    private boolean ownsGravity;

    public ChaosBeamFinaleLift(LivingEntity caster) {
        this.caster = caster;
        this.world = caster.level;
        this.origin = caster.position();
    }

    public static double height(double age) {
        if (!Double.isFinite(age)) return 0;
        double t = Math.max(0, Math.min(1, age / RISE_TICKS));
        return HEIGHT * t * t * (3 - 2 * t);
    }

    /** Manual movement also works while a detached camera stops the player's movement packets. */
    public static void advance(LivingEntity caster, Vector3d origin, double age) {
        if (caster.isPassenger()) caster.stopRiding();
        caster.setJumping(false);
        caster.setSprinting(false);
        caster.xxa = caster.yya = caster.zza = 0;
        caster.setDeltaMovement(Vector3d.ZERO);
        double rise = Math.max(0, Math.min(MAX_STEP, origin.y + height(age) - caster.getY()));
        if (rise > 0) caster.move(MoverType.SELF, new Vector3d(0, rise, 0));
        caster.setDeltaMovement(Vector3d.ZERO);
        caster.fallDistance = 0;
    }

    public void tick(double age) {
        // Acquire after the beam entity has released its own gravity lock.
        if (!caster.isNoGravity()) {
            ownsGravity = true;
            caster.getPersistentData().putBoolean(GRAVITY_TAG, true);
            caster.setNoGravity(true);
        }
        advance(caster, origin, age);
    }

    public void finish(boolean completed) {
        if (ownsGravity) recover(caster);
        ownsGravity = false;
        if (caster.level != world || caster.removed || !caster.isAlive()) return;
        caster.setDeltaMovement(Vector3d.ZERO);
        caster.fallDistance = 0;
        // The cinematic must not injure its low-health caster when control returns in midair.
        if (caster.getY() > origin.y + .1 && !caster.isNoGravity())
            caster.addEffect(new EffectInstance(Effects.SLOW_FALLING, 100, 0, false, false, false));
        if (completed && caster instanceof ServerPlayerEntity)
            caster.teleportTo(caster.getX(), caster.getY(), caster.getZ());
    }

    /** Recover only gravity claimed by this cinematic, including after an interrupted server save. */
    public static void recover(LivingEntity caster) {
        if (caster.getPersistentData().getBoolean(GRAVITY_TAG)) {
            caster.setNoGravity(false);
            caster.getPersistentData().remove(GRAVITY_TAG);
        }
    }
}
