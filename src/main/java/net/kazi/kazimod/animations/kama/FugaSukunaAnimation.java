package net.kazi.kazimod.animations.kama;

import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import xyz.pixelatedw.mineminenomi.api.animations.Animation;
import xyz.pixelatedw.mineminenomi.api.animations.AnimationId;

public class FugaSukunaAnimation extends Animation<LivingEntity, BipedModel> {

    public FugaSukunaAnimation(AnimationId<FugaSukunaAnimation> animId) {
        super(animId);
        this.setAnimationAngles(this::angles);
    }

    public void angles(LivingEntity entity, BipedModel model, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float time     = (float) this.getTime();
        float raiseEnd = 10.0F;

        float targetLeftXRot  = (float) Math.toRadians(-90.0F);
        float targetRightXRot = (float) Math.toRadians(-90.0F);
        float targetRightYRot = (float) Math.toRadians(-17.5F);

        float targetRightY = 2.0F - 1.0F;
        float targetRightZ = 0.0F + 3.0F;

        if (time < raiseEnd) {
            float t = MathHelper.clamp(time / raiseEnd, 0.0F, 1.0F);

            model.leftArm.xRot = MathHelper.lerp(t, 0.0F, targetLeftXRot);
            model.leftArm.yRot = 0.0F;
            model.leftArm.zRot = 0.0F;
            model.leftArm.y =  2.0F;
            model.leftArm.z = -3.0F;

            model.rightArm.xRot = MathHelper.lerp(t, 0.0F, targetRightXRot);
            model.rightArm.yRot = MathHelper.lerp(t, 0.0F, targetRightYRot);
            model.rightArm.zRot = 0.0F;
            model.rightArm.y = MathHelper.lerp(t, 2.0F, targetRightY);
            model.rightArm.z = MathHelper.lerp(t, 0.0F, targetRightZ);

        } else {
            model.leftArm.xRot = targetLeftXRot;
            model.leftArm.yRot = 0.0F;
            model.leftArm.zRot = 0.0F;
            model.leftArm.y =  2.0F;
            model.leftArm.z = -3.0F;

            model.rightArm.xRot = targetRightXRot;
            model.rightArm.yRot = targetRightYRot;
            model.rightArm.zRot = 0.0F;
            model.rightArm.y = targetRightY;
            model.rightArm.z = targetRightZ;
        }
    }
}