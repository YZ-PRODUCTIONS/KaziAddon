package net.kazi.kazimod.animations.gojo;

import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import xyz.pixelatedw.mineminenomi.api.animations.Animation;
import xyz.pixelatedw.mineminenomi.api.animations.AnimationId;

public class GojoDomainAnimation extends Animation<LivingEntity, BipedModel> {

    public GojoDomainAnimation(AnimationId<GojoDomainAnimation> animId) {
        super(animId);
        this.setAnimationAngles(this::angles);
    }

    public void angles(LivingEntity entity, BipedModel model, float limbSwing, float limbSwingAmount,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        float time = (float) this.getTime();
        float raiseEnd = 8.0F;

        // Right arm — bent upward close to body, hand near chin
        float targetRightXRot = (float) Math.toRadians(-130.0F);
        float targetRightYRot = (float) Math.toRadians(20.0F);
        float targetRightZRot = (float) Math.toRadians(40.0F);
        float targetRightY    = 2.0F + 3.0F; // +3 moves arm down in model space

        float t = MathHelper.clamp(time / raiseEnd, 0.0F, 1.0F);

        model.rightArm.xRot = MathHelper.lerp(t, 0.0F, targetRightXRot);
        model.rightArm.yRot = MathHelper.lerp(t, 0.0F, targetRightYRot);
        model.rightArm.zRot = MathHelper.lerp(t, 0.0F, targetRightZRot);
        model.rightArm.y    = MathHelper.lerp(t, 2.0F, targetRightY);
    }
}