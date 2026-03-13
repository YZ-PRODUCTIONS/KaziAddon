package net.kazi.kazimod.animations.toki;

import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import xyz.pixelatedw.mineminenomi.api.animations.Animation;
import xyz.pixelatedw.mineminenomi.api.animations.AnimationId;

/**
 * Converted from the Blockbench 1.19 export to 1.16.5 Animation API.
 *
 * Blockbench keyframes:
 *   t=0.00s  both arms at rest (0, 0, 0)
 *   t=0.25s  both arms xRot -100deg, posY +2 blocks forward/up
 *   (holds for the remaining 0.5 s of the 0.75 s charge)
 *
 * In 1.16.5 ticks: 0.25 s = 5 ticks, full charge = 15 ticks.
 */
public class TimeTheftAnimation extends Animation<LivingEntity, BipedModel> {

    public TimeTheftAnimation(AnimationId<TimeTheftAnimation> animId) {
        super(animId);
        this.setAnimationAngles(this::angles);
    }

    public void angles(LivingEntity entity, BipedModel model,
                       float limbSwing, float limbSwingAmount,
                       float ageInTicks, float netHeadYaw, float headPitch) {

        float time   = (float) this.getTime();
        float raiseEnd = 5.0F; // 0.25 s * 20 ticks/s = 5 ticks

        // Target pose from Blockbench: arms pitched -100 deg forward,
        // cupped slightly inward toward each other (matching the two-ball screenshot).
        float targetXRot      = (float) Math.toRadians(-100.0F);
        float targetLeftYRot  = (float) Math.toRadians(20.0F);   // left arm angles slightly right
        float targetRightYRot = (float) Math.toRadians(-20.0F);  // right arm angles slightly left

        // BipedModel arm Y origin is 2.0 in 1.16.5; shift up matches posVec(0, 2, -6).
        float defaultArmY = 2.0F;
        float targetArmY  = defaultArmY - 2.0F; // move up in model space (lower Y = higher visually)

        if (time < raiseEnd) {
            // Raise phase: lerp from rest to cupped pose over 5 ticks.
            float t = MathHelper.clamp(time / raiseEnd, 0.0F, 1.0F);

            model.leftArm.xRot  = MathHelper.lerp(t, 0.0F, targetXRot);
            model.leftArm.yRot  = MathHelper.lerp(t, 0.0F, targetLeftYRot);
            model.leftArm.zRot  = 0.0F;
            model.leftArm.y     = MathHelper.lerp(t, defaultArmY, targetArmY);

            model.rightArm.xRot = MathHelper.lerp(t, 0.0F, targetXRot);
            model.rightArm.yRot = MathHelper.lerp(t, 0.0F, targetRightYRot);
            model.rightArm.zRot = 0.0F;
            model.rightArm.y    = MathHelper.lerp(t, defaultArmY, targetArmY);

        } else {
            // Hold cupped pose for remainder of charge (ticks 5-15).
            model.leftArm.xRot  = targetXRot;
            model.leftArm.yRot  = targetLeftYRot;
            model.leftArm.zRot  = 0.0F;
            model.leftArm.y     = targetArmY;

            model.rightArm.xRot = targetXRot;
            model.rightArm.yRot = targetRightYRot;
            model.rightArm.zRot = 0.0F;
            model.rightArm.y    = targetArmY;
        }
    }
}