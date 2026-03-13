package net.kazi.kazimod.animations.tenki;

import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import xyz.pixelatedw.mineminenomi.api.animations.Animation;
import xyz.pixelatedw.mineminenomi.api.animations.AnimationId;

public class LightningFuryAnimation extends Animation<LivingEntity, BipedModel> {

    public LightningFuryAnimation(AnimationId<LightningFuryAnimation> animId) {
        super(animId);
        this.setAnimationAngles(this::angles);
    }

    public void angles(LivingEntity entity, BipedModel model, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float time     = (float) this.getTime();
        float raiseEnd = 2.5F;

        // 45 degrees downward — arm swings forward and down like a strike
        float targetRightArmXRot = (float) Math.toRadians(-65.0F);
        float targetRightArmYRot = (float) Math.toRadians(-16.2701F);
        float targetRightArmZRot = (float) Math.toRadians(11.792F);

        if (time < raiseEnd) {
            float t = MathHelper.clamp(time / raiseEnd, 0.0F, 1.0F);

            model.rightArm.xRot = MathHelper.lerp(t, model.rightArm.xRot, targetRightArmXRot);
            model.rightArm.yRot = MathHelper.lerp(t, model.rightArm.yRot, targetRightArmYRot);
            model.rightArm.zRot = MathHelper.lerp(t, model.rightArm.zRot, targetRightArmZRot);

        } else {
            model.rightArm.xRot = targetRightArmXRot;
            model.rightArm.yRot = targetRightArmYRot;
            model.rightArm.zRot = targetRightArmZRot;
        }
    }
}
