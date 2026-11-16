package net.kazi.kazimod.ope;

import net.minecraft.client.renderer.entity.model.*;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;
import xyz.pixelatedw.mineminenomi.api.animations.*;

public final class WilleAnimation extends Animation<LivingEntity, EntityModel> {
    public WilleAnimation(AnimationId<WilleAnimation> id) { super(id); setAnimationAngles(this::pose); }
    private void pose(LivingEntity user, EntityModel entityModel, float swing, float amount, float age, float yaw, float pitch) {
        if (!(entityModel instanceof BipedModel)) return;
        BipedModel model = (BipedModel)entityModel;
        boolean left = user.getMainArm() == HandSide.LEFT;
        ModelRenderer hand = left ? model.leftArm : model.rightArm;
        ModelRenderer brace = left ? model.rightArm : model.leftArm;
        float blend = MathHelper.clamp(getTime() / 6F, 0, 1);
        blend = blend * blend * (3 - 2 * blend);
        hand.xRot = MathHelper.lerp(blend, hand.xRot, -(float)Math.PI / 2 + (float)Math.toRadians(pitch));
        hand.yRot = MathHelper.lerp(blend, hand.yRot, (float)Math.toRadians(yaw));
        hand.zRot *= 1 - blend;
        brace.xRot = MathHelper.lerp(blend, brace.xRot, -1.32F + (float)Math.toRadians(pitch) * .5F);
        brace.yRot = MathHelper.lerp(blend, brace.yRot, left ? -.7F : .7F);
        brace.zRot *= 1 - blend;
        model.rightLeg.xRot = MathHelper.lerp(blend, model.rightLeg.xRot, .12F);
        model.leftLeg.xRot = MathHelper.lerp(blend, model.leftLeg.xRot, -.16F);
    }
}
