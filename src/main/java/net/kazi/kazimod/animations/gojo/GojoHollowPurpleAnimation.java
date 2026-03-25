package net.kazi.kazimod.animations.gojo;

import net.kazi.kazimod.client.models.GojoBossModel;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import xyz.pixelatedw.mineminenomi.api.animations.Animation;
import xyz.pixelatedw.mineminenomi.api.animations.AnimationId;

/**
 * Hollow Purple charge animation.
 *
 * Phase 1 — Charge (0–10 ticks):
 *   Both arms raise and angle inward, hands meeting in front of the chest,
 *   fingers interlaced — matching the anime pose where Gojo brings Red and
 *   Blue together before firing.
 *
 * Phase 2 — Fire (5-tick lerp):
 *   Both arms thrust forward toward the target as the projectile launches.
 *
 * Works for both the player's BipedModel and the boss's GojoBossModel.
 */
public class GojoHollowPurpleAnimation extends Animation<LivingEntity, EntityModel> {

    public static GojoHollowPurpleAnimation INSTANCE;

    private static final float RAISE_END = 10.0f;
    private static final float FIRE_LERP = 5.0f;

    // ── Charge pose: both arms raised, angled inward so hands clasp together ──
    private static final float CHARGE_RIGHT_ARM_X = (float) Math.toRadians(-140.0);
    private static final float CHARGE_RIGHT_ARM_Y = (float) Math.toRadians(20.0);
    private static final float CHARGE_RIGHT_ARM_Z = (float) Math.toRadians(10.0);

    private static final float CHARGE_LEFT_ARM_X  = (float) Math.toRadians(-140.0);
    private static final float CHARGE_LEFT_ARM_Y  = (float) Math.toRadians(-20.0);
    private static final float CHARGE_LEFT_ARM_Z  = (float) Math.toRadians(-10.0);

    // ── Fire pose: both arms thrust outward toward the target ─────────────────
    private static final float FIRE_RIGHT_ARM_X   = (float) Math.toRadians(-80.0);
    private static final float FIRE_RIGHT_ARM_Y   = (float) Math.toRadians(-10.0);
    private static final float FIRE_RIGHT_ARM_Z   = 0.0f;

    private static final float FIRE_LEFT_ARM_X    = (float) Math.toRadians(-80.0);
    private static final float FIRE_LEFT_ARM_Y    = (float) Math.toRadians(10.0);
    private static final float FIRE_LEFT_ARM_Z    = 0.0f;

    private boolean firing;
    private float   fireTime;

    public GojoHollowPurpleAnimation(final AnimationId<GojoHollowPurpleAnimation> animId) {
        super(animId);
        this.firing   = false;
        this.fireTime = 0.0f;
        (GojoHollowPurpleAnimation.INSTANCE = this).setAnimationAngles(this::angles);
    }

    public void triggerFire() {
        this.firing   = true;
        this.fireTime = 0.0f;
    }

    public void reset() {
        this.firing   = false;
        this.fireTime = 0.0f;
    }

    @SuppressWarnings("rawtypes")
    public void angles(final LivingEntity entity, final EntityModel model,
                       final float limbSwing, final float limbSwingAmount,
                       final float ageInTicks, final float netHeadYaw, final float headPitch) {

        // Resolve the right/left arm ModelRenderer for whichever model type is in use
        final ModelRenderer rightArm;
        final ModelRenderer leftArm;

        if (model instanceof BipedModel) {
            final BipedModel biped = (BipedModel) model;
            rightArm = biped.rightArm;
            leftArm  = biped.leftArm;
        } else if (model instanceof GojoBossModel) {
            final GojoBossModel bossModel = (GojoBossModel) model;
            rightArm = bossModel.rightArm;
            leftArm  = bossModel.leftArm;
        } else {
            return; // unknown model type — skip silently
        }

        final float time = (float) this.getTime();

        if (!this.firing) {
            // ── Charge phase: lerp both arms up into clasped-hands pose ───────
            final float t = MathHelper.clamp(time / RAISE_END, 0.0f, 1.0f);

            rightArm.xRot = MathHelper.lerp(t, 0.0f, CHARGE_RIGHT_ARM_X);
            rightArm.yRot = MathHelper.lerp(t, 0.0f, CHARGE_RIGHT_ARM_Y);
            rightArm.zRot = MathHelper.lerp(t, 0.0f, CHARGE_RIGHT_ARM_Z);

            leftArm.xRot  = MathHelper.lerp(t, 0.0f, CHARGE_LEFT_ARM_X);
            leftArm.yRot  = MathHelper.lerp(t, 0.0f, CHARGE_LEFT_ARM_Y);
            leftArm.zRot  = MathHelper.lerp(t, 0.0f, CHARGE_LEFT_ARM_Z);

        } else {
            // ── Fire phase: lerp both arms into the thrust-forward pose ────────
            ++this.fireTime;
            final float t = MathHelper.clamp(this.fireTime / FIRE_LERP, 0.0f, 1.0f);

            rightArm.xRot = MathHelper.lerp(t, CHARGE_RIGHT_ARM_X, FIRE_RIGHT_ARM_X);
            rightArm.yRot = MathHelper.lerp(t, CHARGE_RIGHT_ARM_Y, FIRE_RIGHT_ARM_Y);
            rightArm.zRot = MathHelper.lerp(t, CHARGE_RIGHT_ARM_Z, FIRE_RIGHT_ARM_Z);

            leftArm.xRot  = MathHelper.lerp(t, CHARGE_LEFT_ARM_X, FIRE_LEFT_ARM_X);
            leftArm.yRot  = MathHelper.lerp(t, CHARGE_LEFT_ARM_Y, FIRE_LEFT_ARM_Y);
            leftArm.zRot  = MathHelper.lerp(t, CHARGE_LEFT_ARM_Z, FIRE_LEFT_ARM_Z);
        }
    }
}