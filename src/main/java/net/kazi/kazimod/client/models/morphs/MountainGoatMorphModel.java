package net.kazi.kazimod.client.models.morphs;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xyz.pixelatedw.mineminenomi.api.morph.MorphModel;

/** Animated wrapper for the geometry exported from the editable Blockbench project. */
@OnlyIn(Dist.CLIENT)
public final class MountainGoatMorphModel<T extends LivingEntity> extends MorphModel<T> {
    private final MountainGoatGeometry goat;

    public MountainGoatMorphModel() {
        super(0.0F);
        texWidth = 256;
        texHeight = 256;
        this.goat = new MountainGoatGeometry(this);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        goat.resetPose();
        float stride = MathHelper.clamp(limbSwingAmount, 0.0F, 1.0F);
        float gait = limbSwing * 0.85F;
        float swing = MathHelper.cos(gait) * 0.8F * stride;
        goat.frontLeftLeg.xRot += swing;
        goat.hindRightLeg.xRot += swing;
        goat.frontRightLeg.xRot -= swing;
        goat.hindLeftLeg.xRot -= swing;

        goat.head.yRot += MathHelper.clamp(netHeadYaw, -50.0F, 50.0F) * ((float) Math.PI / 180.0F);
        goat.head.xRot += MathHelper.clamp(headPitch, -35.0F, 40.0F) * ((float) Math.PI / 180.0F);
        goat.head.xRot += MathHelper.sin(ageInTicks * 0.08F) * 0.025F * (1.0F - stride);
        goat.body.y += MathHelper.sin(ageInTicks * 0.10F) * 0.08F;
        goat.tail.yRot += MathHelper.sin(ageInTicks * 0.16F) * 0.18F;
        goat.tail.zRot += MathHelper.sin(ageInTicks * 0.12F) * 0.06F;

        if (!entity.isOnGround() && !entity.isInWater()) {
            goat.frontLeftLeg.xRot -= 0.35F;
            goat.frontRightLeg.xRot -= 0.35F;
            goat.hindLeftLeg.xRot += 0.3F;
            goat.hindRightLeg.xRot += 0.3F;
        }
    }

    @Override
    public void renderToBuffer(MatrixStack stack, IVertexBuilder buffer, int light, int overlay,
                               float red, float green, float blue, float alpha) {
        goat.root.render(stack, buffer, light, overlay, red, green, blue, alpha);
    }

    @Override
    public boolean renderItemInHand(T entity, HandSide side, MatrixStack stack) {
        return false;
    }

    @Override
    public void renderFirstPersonArm(MatrixStack stack, IVertexBuilder buffer, int light, int overlay,
                                     float red, float green, float blue, float alpha, HandSide side) {
    }

    @Override
    public void renderFirstPersonLeg(MatrixStack stack, IVertexBuilder buffer, int light, int overlay,
                                     float red, float green, float blue, float alpha, HandSide side) {
    }
}
