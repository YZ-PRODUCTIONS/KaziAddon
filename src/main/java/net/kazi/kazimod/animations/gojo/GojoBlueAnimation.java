package net.kazi.kazimod.animations.gojo;

import net.kazi.kazimod.animations.gojo.GojoCastPose;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.entity.LivingEntity;
import xyz.pixelatedw.mineminenomi.api.animations.Animation;
import xyz.pixelatedw.mineminenomi.api.animations.AnimationId;

public class GojoBlueAnimation
extends Animation<LivingEntity, EntityModel> {
    public GojoBlueAnimation(AnimationId<GojoBlueAnimation> id) {
        super(id);
        this.setAnimationAngles(this::angles);
    }

    private void angles(LivingEntity entity, EntityModel model, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float time = this.getTime();
        float raise = GojoCastPose.smooth(time / 6.0f);
        float sweep = GojoCastPose.smooth((time - 5.0f) / 15.0f);
        float yaw = (float)Math.toRadians(netHeadYaw);
        GojoCastPose.arm(model, true, -2.15f + sweep * 0.5f + (float)Math.toRadians(headPitch), yaw - 0.65f * (1.0f - sweep), 0.22f * (1.0f - sweep), raise);
        GojoCastPose.arm(model, false, -0.3f, -0.15f, -0.2f, raise);
    }
}
