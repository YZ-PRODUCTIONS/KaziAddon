package net.kazi.kazimod.animations;

import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import xyz.pixelatedw.mineminenomi.api.animations.Animation;
import xyz.pixelatedw.mineminenomi.api.animations.AnimationId;

public class SukunaDomainAnimation extends Animation<LivingEntity, BipedModel> {

    public SukunaDomainAnimation(AnimationId<SukunaDomainAnimation> animId) {
        super(animId);
        this.setAnimationAngles(this::angles);
    }

    public void angles(LivingEntity entity, BipedModel model, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float time = (float) this.getTime();
        float raiseEnd = 10.0F;

        float targetXRot      = (float) Math.toRadians(-122.4986F);
        float targetLeftYRot  = (float) Math.toRadians(21.469F);
        float targetLeftZRot  = (float) Math.toRadians(-13.1243F);
        float targetRightYRot = (float) Math.toRadians(-21.469F);
        float targetRightZRot = (float) Math.toRadians(13.1243F);

        // Move arms down by setting Y higher (larger Y = lower in model space)
        float targetArmY = 4.0F;

        if (time < raiseEnd) {
            float t = MathHelper.clamp(time / raiseEnd, 0.0F, 1.0F);

            model.leftArm.xRot  = MathHelper.lerp(t, 0.0F, targetXRot);
            model.leftArm.yRot  = MathHelper.lerp(t, 0.0F, targetLeftYRot);
            model.leftArm.zRot  = MathHelper.lerp(t, 0.0F, targetLeftZRot);
            model.leftArm.y     = MathHelper.lerp(t, 2.0F, targetArmY);

            model.rightArm.xRot = MathHelper.lerp(t, 0.0F, targetXRot);
            model.rightArm.yRot = MathHelper.lerp(t, 0.0F, targetRightYRot);
            model.rightArm.zRot = MathHelper.lerp(t, 0.0F, targetRightZRot);
            model.rightArm.y    = MathHelper.lerp(t, 2.0F, targetArmY);

        } else {
            model.leftArm.xRot  = targetXRot;
            model.leftArm.yRot  = targetLeftYRot;
            model.leftArm.zRot  = targetLeftZRot;
            model.leftArm.y     = targetArmY;

            model.rightArm.xRot = targetXRot;
            model.rightArm.yRot = targetRightYRot;
            model.rightArm.zRot = targetRightZRot;
            model.rightArm.y    = targetArmY;
        }
    }
}