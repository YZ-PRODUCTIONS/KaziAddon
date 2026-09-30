package net.kazi.kazimod.animations.kama;

import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import xyz.pixelatedw.mineminenomi.api.animations.Animation;
import xyz.pixelatedw.mineminenomi.api.animations.AnimationId;

public class FugaSukunaAnimation
extends Animation<LivingEntity, BipedModel> {
    public FugaSukunaAnimation(AnimationId<FugaSukunaAnimation> id) {
        super(id);
        this.setAnimationAngles(this::angles);
    }

    private void angles(LivingEntity entity, BipedModel model, float limbSwing, float limbAmount, float age, float headYaw, float headPitch) {
        float time = this.getTime();
        float raise = FugaSukunaAnimation.smooth(time / 12.0f);
        float draw = FugaSukunaAnimation.smooth((time - 44.0f) / 26.0f);
        float yaw = (float)Math.toRadians(headYaw);
        float pitch = (float)Math.toRadians(headPitch);
        model.leftArm.xRot = (-1.35f + draw * (-0.22f + pitch)) * raise;
        model.leftArm.yRot = (-0.65f * (1.0f - draw) + yaw * draw) * raise;
        model.leftArm.zRot = -0.1f * (1.0f - draw) * raise;
        model.rightArm.xRot = (-1.35f - draw * 0.45f + pitch * draw) * raise;
        model.rightArm.yRot = (0.65f - draw * 0.3f + yaw * draw) * raise;
        model.rightArm.zRot = 0.1f * raise;
        model.rightArm.z = draw * 2.0f;
        model.leftArm.z = -draw;
    }

    private static float smooth(float value) {
        float t = MathHelper.clamp((float)value, (float)0.0f, (float)1.0f);
        return t * t * (3.0f - 2.0f * t);
    }
}
