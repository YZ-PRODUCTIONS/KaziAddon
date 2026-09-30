package net.kazi.kazimod.animations.gojo;

import net.kazi.kazimod.animations.gojo.GojoCastPose;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.entity.LivingEntity;
import xyz.pixelatedw.mineminenomi.api.animations.Animation;
import xyz.pixelatedw.mineminenomi.api.animations.AnimationId;

public class GojoRedAnimation
extends Animation<LivingEntity, EntityModel> {
    public GojoRedAnimation(AnimationId<GojoRedAnimation> animId) {
        super(animId);
        this.setAnimationAngles(this::angles);
    }

    public void angles(LivingEntity entity, EntityModel model, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float time = this.getTime();
        float raise = GojoCastPose.smooth(time / 5.0f);
        float aim = (float)Math.toRadians(netHeadYaw);
        float flick = GojoCastPose.smooth((time - 16.0f) / 4.0f);
        GojoCastPose.arm(model, true, (float)Math.toRadians(-112.0f + headPitch) + flick * 0.33f, aim + 0.2f * (1.0f - flick), -0.06f * (1.0f - flick), raise);
        GojoCastPose.arm(model, false, -0.12f, 0.0f, -0.06f, raise);
    }
}
