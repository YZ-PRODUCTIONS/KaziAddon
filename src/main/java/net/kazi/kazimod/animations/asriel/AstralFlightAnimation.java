package net.kazi.kazimod.animations.asriel;

import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import xyz.pixelatedw.mineminenomi.api.animations.Animation;
import xyz.pixelatedw.mineminenomi.api.animations.AnimationId;

/** Relaxed, upright levitation; attack animations take priority over this idle pose. */
public final class AstralFlightAnimation extends Animation<LivingEntity, EntityModel> {
    public AstralFlightAnimation(AnimationId<AstralFlightAnimation> id) {
        super(id);
        setAnimationSetup((entity, stack, buffers, light, yaw, partial) ->
                stack.translate(0, Math.sin((entity.tickCount + partial) * .12) * .065, 0));
        setAnimationAngles(this::angles);
    }

    private void angles(LivingEntity entity, EntityModel model, float walk, float amount,
                        float age, float yaw, float pitch) {
        if (!(model instanceof BipedModel)) return;
        BipedModel biped = (BipedModel) model;
        float sway = MathHelper.sin(age * .09F) * .035F;
        biped.body.xRot = 0;
        biped.rightArm.xRot = -.08F + sway;
        biped.leftArm.xRot = -.08F - sway;
        biped.rightArm.yRot = -.06F;
        biped.leftArm.yRot = .06F;
        biped.rightArm.zRot = .19F + sway;
        biped.leftArm.zRot = -.19F + sway;
        biped.rightLeg.xRot = .04F + sway * .5F;
        biped.leftLeg.xRot = -.04F - sway * .5F;
        biped.rightLeg.yRot = biped.leftLeg.yRot = 0;
        biped.rightLeg.zRot = .025F;
        biped.leftLeg.zRot = -.025F;
        if (biped instanceof PlayerModel) {
            PlayerModel player = (PlayerModel) biped;
            player.rightSleeve.copyFrom(biped.rightArm);
            player.leftSleeve.copyFrom(biped.leftArm);
            player.rightPants.copyFrom(biped.rightLeg);
            player.leftPants.copyFrom(biped.leftLeg);
            player.jacket.copyFrom(biped.body);
        }
    }
}
