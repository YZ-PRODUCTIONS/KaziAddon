package net.kazi.kazimod.animations.gojo;

import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import xyz.pixelatedw.mineminenomi.api.animations.Animation;
import xyz.pixelatedw.mineminenomi.api.animations.AnimationId;

public class GojoRedAnimation extends Animation<LivingEntity, BipedModel> {

    public GojoRedAnimation(AnimationId<GojoRedAnimation> animId) {
        super(animId);
        this.setAnimationAngles(this::angles);
    }

    public void angles(LivingEntity entity, BipedModel model, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float time     = (float) this.getTime();
        float raiseEnd = 10.0F;

        float targetXRot = (float) Math.toRadians(-100.0F); // arm raised forward, not fully vertical
        float targetYRot = (float) Math.toRadians(-10.0F); // slight inward angle

        if (time < raiseEnd) {
            float t = MathHelper.clamp(time / raiseEnd, 0.0F, 1.0F);

            model.rightArm.xRot = MathHelper.lerp(t, 0.0F, targetXRot);
            model.rightArm.yRot = MathHelper.lerp(t, 0.0F, targetYRot);
            model.rightArm.zRot = 0.0F;

        } else {
            model.rightArm.xRot = targetXRot;
            model.rightArm.yRot = targetYRot;
            model.rightArm.zRot = 0.0F;
        }
    }
}