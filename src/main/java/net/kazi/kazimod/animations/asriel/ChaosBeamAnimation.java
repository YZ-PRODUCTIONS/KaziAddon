package net.kazi.kazimod.animations.asriel;

import net.kazi.kazimod.abilities.YagiAwakening.ChaosBeamPattern;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import xyz.pixelatedw.mineminenomi.api.animations.Animation;
import xyz.pixelatedw.mineminenomi.api.animations.AnimationId;

/** Lift both palms apart, bring them together, then hold the merged core forward. */
public final class ChaosBeamAnimation extends Animation<LivingEntity, EntityModel> {
    public ChaosBeamAnimation(AnimationId<ChaosBeamAnimation> id) { super(id); setAnimationAngles(this::angles); }
    private void angles(LivingEntity entity, EntityModel model, float walk, float amount, float age, float yaw, float pitch) {
        if (!(model instanceof BipedModel)) return;
        float time = getTime() + MathHelper.clamp(age - entity.tickCount, 0F, 1F);
        float merge = ChaosBeamPattern.smooth((time - 12F) / 23F);
        float blend = ChaosBeamPattern.smooth(time / 7F);
        BipedModel biped = (BipedModel) model;
        pose(biped.rightArm, -1, merge, blend, pitch);
        pose(biped.leftArm, 1, merge, blend, pitch);
    }
    private static void pose(ModelRenderer arm, int side, float merge, float blend, float pitch) {
        arm.xRot = MathHelper.lerp(blend, arm.xRot, -(float)Math.PI / 2F + pitch * ((float)Math.PI / 180F));
        arm.yRot = MathHelper.lerp(blend, arm.yRot, side * (.95F * (1F-merge) - .23F * merge));
        arm.zRot = MathHelper.lerp(blend, arm.zRot, side * .10F * (1F-merge));
    }
}
