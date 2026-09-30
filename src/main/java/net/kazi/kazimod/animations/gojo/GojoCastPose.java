package net.kazi.kazimod.animations.gojo;

import net.kazi.kazimod.client.models.GojoBossModel;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.util.math.MathHelper;

final class GojoCastPose {
    private GojoCastPose() {
    }

    static void arm(EntityModel model, boolean right, float x, float y, float z, float blend) {
        ModelRenderer arm;
        if (model instanceof BipedModel) {
            arm = right ? ((BipedModel)model).rightArm : ((BipedModel)model).leftArm;
        } else if (model instanceof GojoBossModel) {
            arm = right ? ((GojoBossModel)model).rightArm : ((GojoBossModel)model).leftArm;
        } else {
            return;
        }
        arm.xRot = MathHelper.lerp((float)blend, (float)arm.xRot, (float)x);
        arm.yRot = MathHelper.lerp((float)blend, (float)arm.yRot, (float)y);
        arm.zRot = MathHelper.lerp((float)blend, (float)arm.zRot, (float)z);
    }

    static float smooth(float value) {
        float t = MathHelper.clamp((float)value, (float)0.0f, (float)1.0f);
        return t * t * (3.0f - 2.0f * t);
    }
}
