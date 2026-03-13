package net.kazi.kazimod.animations.toki;

import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import xyz.pixelatedw.mineminenomi.api.animations.Animation;
import xyz.pixelatedw.mineminenomi.api.animations.AnimationId;

public class ChronostasisAnimation extends Animation<LivingEntity, BipedModel> {

    public ChronostasisAnimation(AnimationId<ChronostasisAnimation> animId) {
        super(animId);
        this.setAnimationAngles(this::angles);
    }

    public void angles(LivingEntity entity, BipedModel model,
                       float limbSwing, float limbSwingAmount,
                       float ageInTicks, float netHeadYaw, float headPitch) {

        float time    = (float) this.getTime();
        float raisEnd = 5.0F; // 0.25 s = 5 ticks

        // Target pose: arms raise forward and cup inward like holding a ball.
        float targetXRot      = (float) Math.toRadians(-100.0F);
        float targetLeftYRot  = (float) Math.toRadians(45.0F);  // more inward
        float targetRightYRot = (float) Math.toRadians(-45.0F); // more inward

        float defaultArmY = 2.0F;
        float targetArmY  = defaultArmY + 3.5F;

        if (time < raisEnd) {
            float t = MathHelper.clamp(time / raisEnd, 0.0F, 1.0F);

            model.leftArm.xRot  = MathHelper.lerp(t, 0.0F, targetXRot);
            model.leftArm.yRot  = MathHelper.lerp(t, 0.0F, targetLeftYRot);
            model.leftArm.zRot  = 0.0F;
            model.leftArm.y     = MathHelper.lerp(t, defaultArmY, targetArmY);

            model.rightArm.xRot = MathHelper.lerp(t, 0.0F, targetXRot);
            model.rightArm.yRot = MathHelper.lerp(t, 0.0F, targetRightYRot);
            model.rightArm.zRot = 0.0F;
            model.rightArm.y    = MathHelper.lerp(t, defaultArmY, targetArmY);

        } else {
            // Hold cupped pose for the rest of the charge and release.
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