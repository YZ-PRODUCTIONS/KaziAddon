package net.kazi.kazimod.animations.asriel;

import net.kazi.kazimod.abilities.YagiAwakening.ChaosSaberPattern;
import net.kazi.kazimod.renderers.entities.asriel.ChaosSaberMotion;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import xyz.pixelatedw.mineminenomi.api.animations.Animation;
import xyz.pixelatedw.mineminenomi.api.animations.AnimationId;

/** The caster's arms follow the same continuous curves as the two projected swords. */
public final class ChaosSaberAnimation extends Animation<LivingEntity, EntityModel> {
    public ChaosSaberAnimation(AnimationId<ChaosSaberAnimation> id) {
        super(id);
        setAnimationAngles(this::angles);
    }

    private void angles(LivingEntity entity, EntityModel model, float limbSwing,
                        float limbAmount, float ageInTicks, float headYaw, float headPitch) {
        // Zoan and other non-humanoid renderers do not necessarily have arms.
        if (!(model instanceof BipedModel)) return;
        float partial = MathHelper.clamp(ageInTicks - entity.tickCount, 0.0F, 1.0F);
        float age = getTime() + partial;
        float blend = smooth(age / 10.0F)
                * (1.0F - smooth((age - ChaosSaberPattern.SPARKLE_TICK) / 10.0F));
        BipedModel biped = (BipedModel) model;
        pose(biped.rightArm, age, -1, blend);
        pose(biped.leftArm, age, 1, blend);
    }

    private static void pose(ModelRenderer arm, float age, int side, float blend) {
        float bladePitch = (float) ChaosSaberMotion.pitch(age, side);
        float pitch = bladePitch - (float) Math.PI / 2.0F;
        // Biped models face local -Z; the projected blades face local +Z.
        float yaw = -(float) ChaosSaberMotion.yaw(age, side);
        float raised = MathHelper.clamp(bladePitch / (float) ChaosSaberMotion.RAISED_PITCH, 0.0F, 1.0F);
        arm.xRot = MathHelper.lerp(blend, arm.xRot, pitch);
        arm.yRot = MathHelper.lerp(blend, arm.yRot, yaw);
        arm.zRot = MathHelper.lerp(blend, arm.zRot, side * 0.22F * raised);
    }

    private static float smooth(float value) {
        float t = MathHelper.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}
