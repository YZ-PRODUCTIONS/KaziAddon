package net.kazi.kazimod.animations.gojo;

import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xyz.pixelatedw.mineminenomi.api.animations.Animation;
import xyz.pixelatedw.mineminenomi.models.entities.living.HumanoidModel;

@OnlyIn(Dist.CLIENT)
public class GojoHollowPurpleChargeAnimation extends Animation {

    private static final float TARGET_RIGHT_ARM_X = (float) Math.toRadians(-160.0F);
    private static final float TARGET_RIGHT_ARM_Y = (float) Math.toRadians(-10.0F);
    private static final float TARGET_RIGHT_ARM_Z = (float) Math.toRadians(0.0F);

    private static final float TARGET_LEFT_ARM_X = (float) Math.toRadians(-140.0F);
    private static final float TARGET_LEFT_ARM_Y = (float) Math.toRadians(10.0F);
    private static final float TARGET_LEFT_ARM_Z = (float) Math.toRadians(0.0F);

    private static final float RAISE_END = 10.0F;

    private float rightArmXProgress = 0.0F;
    private float rightArmYProgress = 0.0F;
    private float rightArmZProgress = 0.0F;
    private float leftArmXProgress = 0.0F;
    private float leftArmYProgress = 0.0F;
    private float leftArmZProgress = 0.0F;

    @Override
    public void init(Object model) {
        this.rightArmXProgress = 0.0F;
        this.rightArmYProgress = 0.0F;
        this.rightArmZProgress = 0.0F;
        this.leftArmXProgress = 0.0F;
        this.leftArmYProgress = 0.0F;
        this.leftArmZProgress = 0.0F;
    }

    @Override
    public void tick(Object model, float limbSwing, float limbSwingAmount,
                     float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        HumanoidModel humanoid = (HumanoidModel) model;
        ModelRenderer rightArm = humanoid.rightArm;
        ModelRenderer leftArm = humanoid.leftArm;

        this.rightArmXProgress = Math.min(this.rightArmXProgress + (1.0F / RAISE_END), 1.0F);
        this.rightArmYProgress = Math.min(this.rightArmYProgress + (1.0F / RAISE_END), 1.0F);
        this.rightArmZProgress = Math.min(this.rightArmZProgress + (1.0F / RAISE_END), 1.0F);
        this.leftArmXProgress = Math.min(this.leftArmXProgress + (1.0F / RAISE_END), 1.0F);
        this.leftArmYProgress = Math.min(this.leftArmYProgress + (1.0F / RAISE_END), 1.0F);
        this.leftArmZProgress = Math.min(this.leftArmZProgress + (1.0F / RAISE_END), 1.0F);

        rightArm.xRot = TARGET_RIGHT_ARM_X * this.rightArmXProgress;
        rightArm.yRot = TARGET_RIGHT_ARM_Y * this.rightArmYProgress;
        rightArm.zRot = TARGET_RIGHT_ARM_Z * this.rightArmZProgress;

        leftArm.xRot = TARGET_LEFT_ARM_X * this.leftArmXProgress;
        leftArm.yRot = TARGET_LEFT_ARM_Y * this.leftArmYProgress;
        leftArm.zRot = TARGET_LEFT_ARM_Z * this.leftArmZProgress;
    }
}