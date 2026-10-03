package net.kazi.kazimod.client.models.morphs;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphModel;

public class TripelTMorphModel<T extends LivingEntity> extends MorphModel<T> {
    private static final float ITEM_SCALE = 1.08F;

    private final ModelRenderer head;
    private final ModelRenderer eyebrow2_r1;
    private final ModelRenderer eyebrow1_r1;
    private final ModelRenderer nose_r1;
    private final ModelRenderer nosepart_r1;
    private final ModelRenderer cheekbone1_r1;
    private final ModelRenderer cheekbone2_r1;
    private final ModelRenderer body;
    private final ModelRenderer leg1;
    private final ModelRenderer leg2;
    private final ModelRenderer arm1;
    private final ModelRenderer lowerarm1_r1;
    private final ModelRenderer upperarm1_r1;
    private final ModelRenderer arm2;
    private final ModelRenderer lowerarm2_r1;
    private final ModelRenderer upperarm2_r1;

    public TripelTMorphModel() {
        super(0.0F);
        texWidth = 128;
        texHeight = 128;

        head = new ModelRenderer(this);
        head.setPos(-0.5008F, -23.2323F, -1.3332F);
        head.texOffs(0, 0).addBox(-5.9992F, -6.7677F, -1.1668F, 12.0F, 16.0F, 12.0F, 0.0F, false);
        head.texOffs(0, 66).addBox(1.5008F, -3.7677F, -1.5668F, 5.0F, 5.0F, 1.0F, 0.0F, false);
        head.texOffs(68, 18).addBox(-6.4992F, -3.7677F, -1.5668F, 5.0F, 5.0F, 1.0F, 0.0F, false);

        eyebrow2_r1 = new ModelRenderer(this);
        eyebrow2_r1.setPos(-3.4992F, -3.7677F, -0.6668F);
        head.addChild(eyebrow2_r1);
        setRotationAngle(eyebrow2_r1, 0.0F, 0.0F, -0.0436F);
        eyebrow2_r1.texOffs(70, 65).addBox(-3.0F, -1.0F, -1.0F, 5.0F, 1.0F, 2.0F, 0.0F, false);

        eyebrow1_r1 = new ModelRenderer(this);
        eyebrow1_r1.setPos(4.5008F, -3.7677F, -0.6668F);
        head.addChild(eyebrow1_r1);
        setRotationAngle(eyebrow1_r1, 0.0F, 0.0F, 0.0436F);
        eyebrow1_r1.texOffs(68, 24).addBox(-3.0F, -1.0F, -1.0F, 5.0F, 1.0F, 2.0F, 0.0F, false);

        nose_r1 = new ModelRenderer(this);
        nose_r1.setPos(-0.9992F, 2.2323F, -1.6668F);
        head.addChild(nose_r1);
        setRotationAngle(nose_r1, -0.2182F, 0.0F, 0.0F);
        nose_r1.texOffs(60, 65).addBox(-0.5F, -4.0F, -0.5F, 3.0F, 7.0F, 2.0F, 0.0F, false);

        nosepart_r1 = new ModelRenderer(this);
        nosepart_r1.setPos(0.5008F, 5.2323F, -1.6668F);
        head.addChild(nosepart_r1);
        setRotationAngle(nosepart_r1, -0.2182F, 0.0F, 0.0F);
        nosepart_r1.texOffs(64, 13).addBox(-3.0F, -2.0F, -1.0F, 5.0F, 2.0F, 3.0F, 0.0F, false);

        cheekbone1_r1 = new ModelRenderer(this);
        cheekbone1_r1.setPos(-3.4992F, 3.2323F, 1.3332F);
        head.addChild(cheekbone1_r1);
        setRotationAngle(cheekbone1_r1, 0.0F, 0.0F, -0.1309F);
        cheekbone1_r1.texOffs(48, 20).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 3.0F, 4.0F, 0.0F, false);

        cheekbone2_r1 = new ModelRenderer(this);
        cheekbone2_r1.setPos(2.5008F, 3.2323F, 1.3332F);
        head.addChild(cheekbone2_r1);
        setRotationAngle(cheekbone2_r1, 0.0F, 0.0F, 0.1309F);
        cheekbone2_r1.texOffs(0, 59).addBox(-2.0F, -3.0F, -3.0F, 6.0F, 3.0F, 4.0F, 0.0F, false);

        body = new ModelRenderer(this);
        body.setPos(-0.5F, -4.0F, 3.5F);
        body.texOffs(0, 28).addBox(-5.5F, -10.0F, -5.5F, 11.0F, 20.0F, 11.0F, 0.0F, false);

        leg1 = new ModelRenderer(this);
        leg1.setPos(-3.5F, 18.0F, 2.0F);
        leg1.texOffs(48, 0).addBox(-2.0F, -12.5F, -0.5F, 4.0F, 16.0F, 4.0F, 0.0F, false);
        leg1.texOffs(44, 40).addBox(-2.5F, 3.5F, -6.5F, 5.0F, 2.0F, 10.0F, 0.0F, false);

        leg2 = new ModelRenderer(this);
        leg2.setPos(2.5F, 18.0F, 2.0F);
        leg2.texOffs(44, 52).addBox(-2.0F, -12.5F, -0.5F, 4.0F, 16.0F, 4.0F, 0.0F, false);
        leg2.texOffs(44, 28).addBox(-2.5F, 3.5F, -6.5F, 5.0F, 2.0F, 10.0F, 0.0F, false);

        arm1 = new ModelRenderer(this);
        arm1.setPos(-7.8F, -4.5F, 3.0F);

        lowerarm1_r1 = new ModelRenderer(this);
        lowerarm1_r1.setPos(-0.7F, 4.5F, -0.5F);
        arm1.addChild(lowerarm1_r1);
        setRotationAngle(lowerarm1_r1, -0.2618F, 0.0F, 0.0F);
        lowerarm1_r1.texOffs(20, 59).addBox(-1.1F, -5.0F, -1.5F, 3.0F, 10.0F, 3.0F, 0.0F, false);

        upperarm1_r1 = new ModelRenderer(this);
        upperarm1_r1.setPos(0.3F, -4.5F, 0.5F);
        arm1.addChild(upperarm1_r1);
        setRotationAngle(upperarm1_r1, 0.0F, 0.0F, 0.1309F);
        upperarm1_r1.texOffs(32, 59).addBox(-1.5F, -5.0F, -1.5F, 3.0F, 10.0F, 3.0F, 0.0F, false);

        arm2 = new ModelRenderer(this);
        arm2.setPos(6.8F, -4.5F, 3.0F);

        lowerarm2_r1 = new ModelRenderer(this);
        lowerarm2_r1.setPos(2.7F, 4.5F, -0.5F);
        arm2.addChild(lowerarm2_r1);
        setRotationAngle(lowerarm2_r1, -0.2618F, 0.0F, 0.0F);
        lowerarm2_r1.texOffs(64, 0).addBox(-3.9F, -5.0F, -1.5F, 3.0F, 10.0F, 3.0F, 0.0F, false);

        upperarm2_r1 = new ModelRenderer(this);
        upperarm2_r1.setPos(-0.3F, -4.5F, 0.5F);
        arm2.addChild(upperarm2_r1);
        setRotationAngle(upperarm2_r1, 0.0F, 0.0F, -0.1309F);
        upperarm2_r1.texOffs(60, 52).addBox(-1.5F, -5.0F, -1.5F, 3.0F, 10.0F, 3.0F, 0.0F, false);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        resetPose();
        net.kazi.kazimod.preserved.sahur.SahurAnimator.apply(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch,
                false, head, body, arm1, arm2, leg1, leg2, lowerarm1_r1, lowerarm2_r1, null, null, null);
    }

    @Override
    public void renderToBuffer(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        head.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        body.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        leg1.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        leg2.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        arm1.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        arm2.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void renderFirstPersonArm(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, HandSide side) {
        (side == HandSide.RIGHT ? arm1 : arm2).render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void renderFirstPersonLeg(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, HandSide side) {
        (side == HandSide.RIGHT ? leg2 : leg1).render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void translateToHand(HandSide side, MatrixStack matrixStack) {
        ModelRenderer arm = side == HandSide.RIGHT ? arm1 : arm2;
        net.kazi.kazimod.preserved.sahur.SahurAnimator.hand(matrixStack, arm, side == HandSide.RIGHT ? lowerarm1_r1 : lowerarm2_r1);
        matrixStack.translate(side == HandSide.RIGHT ? -0.07D : 0.105D, -0.1D, 0.04D);
        matrixStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
    }

    private void resetPose() {
        head.xRot = head.yRot = head.zRot = 0.0F;
        body.xRot = body.yRot = body.zRot = 0.0F;
        leg1.xRot = leg1.yRot = leg1.zRot = 0.0F;
        leg2.xRot = leg2.yRot = leg2.zRot = 0.0F;
        arm1.xRot = arm1.yRot = arm1.zRot = 0.0F;
        arm2.xRot = arm2.yRot = arm2.zRot = 0.0F;
    }

    private void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.xRot = x;
        modelRenderer.yRot = y;
        modelRenderer.zRot = z;
    }
}
