package net.kazi.kazimod.client.models.morphs;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphModel;

public class TripelTGodMorphModel<T extends LivingEntity> extends MorphModel<T> {
    private static final float ITEM_SCALE = 1.18F;

    private final ModelRenderer head;
    private final ModelRenderer eyebrow2_r1;
    private final ModelRenderer eyebrow1_r1;
    private final ModelRenderer cheekbone2_r1;
    private final ModelRenderer cheekbone1_r1;
    private final ModelRenderer nosepart_r1;
    private final ModelRenderer nose_r1;
    private final ModelRenderer halo;
    private final ModelRenderer halo_r1;
    private final ModelRenderer halo_r2;
    private final ModelRenderer body;
    private final ModelRenderer leg1;
    private final ModelRenderer leg2;
    private final ModelRenderer wing1;
    private final ModelRenderer wingfeather_r1;
    private final ModelRenderer wing_r1;
    private final ModelRenderer wingfeather_r2;
    private final ModelRenderer wing_r2;
    private final ModelRenderer wingfeather_r3;
    private final ModelRenderer wing_r3;
    private final ModelRenderer wing2;
    private final ModelRenderer wingfeather_r4;
    private final ModelRenderer wing_r4;
    private final ModelRenderer wingfeather_r5;
    private final ModelRenderer wing_r5;
    private final ModelRenderer wingfeather_r6;
    private final ModelRenderer wing_r6;
    private final ModelRenderer arm1;
    private final ModelRenderer lowerarm1_r1;
    private final ModelRenderer upperarm1_r1;
    private final ModelRenderer arm2;
    private final ModelRenderer lowerarm2_r1;
    private final ModelRenderer upperarm2_r1;

    public TripelTGodMorphModel() {
        super(0.0F);
        texWidth = 128;
        texHeight = 128;

        head = new ModelRenderer(this);
        head.setPos(0.0492F, -24.1091F, -1.5999F);
        head.texOffs(0, 0).addBox(-6.5492F, -5.8909F, -0.9001F, 12.0F, 16.0F, 12.0F, 0.0F, false);
        head.texOffs(90, 73).addBox(0.9508F, -2.8909F, -1.3001F, 5.0F, 5.0F, 1.0F, 0.0F, false);
        head.texOffs(12, 94).addBox(-7.0492F, -2.8909F, -1.3001F, 5.0F, 5.0F, 1.0F, 0.0F, false);

        eyebrow2_r1 = new ModelRenderer(this);
        eyebrow2_r1.setPos(-4.0492F, -2.8909F, -0.4001F);
        head.addChild(eyebrow2_r1);
        setRotationAngle(eyebrow2_r1, 0.0F, 0.0F, -0.0436F);
        eyebrow2_r1.texOffs(90, 79).addBox(-3.0F, -1.0F, -1.0F, 5.0F, 1.0F, 2.0F, 0.0F, false);

        eyebrow1_r1 = new ModelRenderer(this);
        eyebrow1_r1.setPos(3.9508F, -2.8909F, -0.4001F);
        head.addChild(eyebrow1_r1);
        setRotationAngle(eyebrow1_r1, 0.0F, 0.0F, 0.0436F);
        eyebrow1_r1.texOffs(82, 14).addBox(-3.0F, -1.0F, -1.0F, 5.0F, 1.0F, 2.0F, 0.0F, false);

        cheekbone2_r1 = new ModelRenderer(this);
        cheekbone2_r1.setPos(1.9508F, 4.1091F, 1.5999F);
        head.addChild(cheekbone2_r1);
        setRotationAngle(cheekbone2_r1, 0.0F, 0.0F, 0.1309F);
        cheekbone2_r1.texOffs(82, 7).addBox(-2.0F, -3.0F, -3.0F, 6.0F, 3.0F, 4.0F, 0.0F, false);

        cheekbone1_r1 = new ModelRenderer(this);
        cheekbone1_r1.setPos(-4.0492F, 4.1091F, 1.5999F);
        head.addChild(cheekbone1_r1);
        setRotationAngle(cheekbone1_r1, 0.0F, 0.0F, -0.1309F);
        cheekbone1_r1.texOffs(82, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 3.0F, 4.0F, 0.0F, false);

        nosepart_r1 = new ModelRenderer(this);
        nosepart_r1.setPos(-0.0492F, 6.1091F, -1.4001F);
        head.addChild(nosepart_r1);
        setRotationAngle(nosepart_r1, -0.2182F, 0.0F, 0.0F);
        nosepart_r1.texOffs(12, 89).addBox(-3.0F, -2.0F, -1.0F, 5.0F, 2.0F, 3.0F, 0.0F, false);

        nose_r1 = new ModelRenderer(this);
        nose_r1.setPos(-1.5492F, 3.1091F, -1.4001F);
        head.addChild(nose_r1);
        setRotationAngle(nose_r1, -0.2182F, 0.0F, 0.0F);
        nose_r1.texOffs(90, 64).addBox(-0.5F, -4.0F, -0.5F, 3.0F, 7.0F, 2.0F, 0.0F, false);

        halo = new ModelRenderer(this);
        halo.setPos(4.9508F, -7.8909F, -2.4001F);
        head.addChild(halo);
        halo.texOffs(80, 38).addBox(-13.0F, -2.0F, -1.0F, 15.0F, 2.0F, 2.0F, 0.0F, false);
        halo.texOffs(80, 42).addBox(-13.0F, -2.0F, 14.0F, 15.0F, 2.0F, 2.0F, 0.0F, false);

        halo_r1 = new ModelRenderer(this);
        halo_r1.setPos(-14.0F, 0.0F, 12.0F);
        halo.addChild(halo_r1);
        setRotationAngle(halo_r1, 0.0F, -1.5708F, 0.0F);
        halo_r1.texOffs(80, 34).addBox(-13.0F, -2.0F, -1.0F, 17.0F, 2.0F, 2.0F, 0.0F, false);

        halo_r2 = new ModelRenderer(this);
        halo_r2.setPos(3.0F, 0.0F, 12.0F);
        halo.addChild(halo_r2);
        setRotationAngle(halo_r2, 0.0F, -1.5708F, 0.0F);
        halo_r2.texOffs(80, 30).addBox(-13.0F, -2.0F, -1.0F, 17.0F, 2.0F, 2.0F, 0.0F, false);

        body = new ModelRenderer(this);
        body.setPos(-0.5F, -4.0F, 3.5F);
        body.texOffs(0, 28).addBox(-5.5F, -10.0F, -5.5F, 11.0F, 20.0F, 11.0F, 0.0F, false);

        leg1 = new ModelRenderer(this);
        leg1.setPos(-3.5F, 18.0F, 2.0F);
        leg1.texOffs(30, 82).addBox(-2.0F, -12.5F, -0.5F, 4.0F, 16.0F, 4.0F, 0.0F, false);
        leg1.texOffs(80, 18).addBox(-2.5F, 3.5F, -6.5F, 5.0F, 2.0F, 10.0F, 0.0F, false);

        leg2 = new ModelRenderer(this);
        leg2.setPos(2.5F, 18.0F, 2.0F);
        leg2.texOffs(46, 82).addBox(-2.0F, -12.5F, -0.5F, 4.0F, 16.0F, 4.0F, 0.0F, false);
        leg2.texOffs(0, 77).addBox(-2.5F, 3.5F, -6.5F, 5.0F, 2.0F, 10.0F, 0.0F, false);

        wing1 = new ModelRenderer(this);
        wing1.setPos(18.0428F, -17.4597F, 14.4081F);

        wingfeather_r1 = new ModelRenderer(this);
        wingfeather_r1.setPos(12.7573F, -1.3607F, 5.3867F);
        wing1.addChild(wingfeather_r1);
        setRotationAngle(wingfeather_r1, 0.3492F, -0.3109F, -0.2869F);
        wingfeather_r1.texOffs(0, 59).addBox(-8.5F, -9.0F, 0.0F, 17.0F, 18.0F, 0.0F, 0.0F, false);

        wing_r1 = new ModelRenderer(this);
        wing_r1.setPos(-1.6132F, -3.5751F, -1.7366F);
        wing1.addChild(wing_r1);
        setRotationAngle(wing_r1, 0.3492F, -0.3109F, -0.2869F);
        wing_r1.texOffs(80, 60).addBox(5.564F, -3.8001F, -0.4401F, 12.0F, 2.0F, 2.0F, 0.0F, false);

        wingfeather_r2 = new ModelRenderer(this);
        wingfeather_r2.setPos(0.992F, 3.3299F, 1.6801F);
        wing1.addChild(wingfeather_r2);
        setRotationAngle(wingfeather_r2, 0.4059F, -0.3276F, -0.4915F);
        wingfeather_r2.texOffs(62, 64).addBox(-7.0F, -9.0F, 0.0F, 14.0F, 18.0F, 0.0F, 0.0F, false);

        wing_r2 = new ModelRenderer(this);
        wing_r2.setPos(-1.6132F, -3.5751F, -1.7366F);
        wing1.addChild(wing_r2);
        setRotationAngle(wing_r2, 0.3959F, -0.2472F, -0.456F);
        wing_r2.texOffs(48, 23).addBox(-7.5481F, -3.0657F, -0.4708F, 14.0F, 3.0F, 2.0F, 0.0F, false);

        wingfeather_r3 = new ModelRenderer(this);
        wingfeather_r3.setPos(-7.7321F, 10.0147F, -1.6109F);
        wing1.addChild(wingfeather_r3);
        setRotationAngle(wingfeather_r3, 0.5142F, -0.1901F, -0.8751F);
        wingfeather_r3.texOffs(44, 46).addBox(-9.0F, -9.0F, 0.0F, 18.0F, 18.0F, 0.0F, 0.0F, false);

        wing_r3 = new ModelRenderer(this);
        wing_r3.setPos(-1.6132F, -3.5751F, -1.7366F);
        wing1.addChild(wing_r3);
        setRotationAngle(wing_r3, 0.5142F, -0.1901F, -0.8751F);
        wing_r3.texOffs(80, 51).addBox(-18.2311F, -5.3408F, -0.0051F, 14.0F, 3.0F, 2.0F, 0.0F, false);

        wing2 = new ModelRenderer(this);
        wing2.setPos(-19.0428F, -17.4597F, 14.4081F);

        wingfeather_r4 = new ModelRenderer(this);
        wingfeather_r4.setPos(-12.7573F, -1.3607F, 5.3867F);
        wing2.addChild(wingfeather_r4);
        setRotationAngle(wingfeather_r4, 0.3492F, 0.3109F, 0.2869F);
        wingfeather_r4.texOffs(0, 59).addBox(-8.5F, -9.0F, 0.0F, 17.0F, 18.0F, 0.0F, 0.0F, true);

        wing_r4 = new ModelRenderer(this);
        wing_r4.setPos(1.6132F, -3.5751F, -1.7366F);
        wing2.addChild(wing_r4);
        setRotationAngle(wing_r4, 0.3492F, 0.3109F, 0.2869F);
        wing_r4.texOffs(80, 60).addBox(-17.564F, -3.8001F, -0.4401F, 12.0F, 2.0F, 2.0F, 0.0F, true);

        wingfeather_r5 = new ModelRenderer(this);
        wingfeather_r5.setPos(-0.992F, 3.3299F, 1.6801F);
        wing2.addChild(wingfeather_r5);
        setRotationAngle(wingfeather_r5, 0.4059F, 0.3276F, 0.4915F);
        wingfeather_r5.texOffs(62, 64).addBox(-7.0F, -9.0F, 0.0F, 14.0F, 18.0F, 0.0F, 0.0F, true);

        wing_r5 = new ModelRenderer(this);
        wing_r5.setPos(1.6132F, -3.5751F, -1.7366F);
        wing2.addChild(wing_r5);
        setRotationAngle(wing_r5, 0.3959F, 0.2472F, 0.456F);
        wing_r5.texOffs(48, 23).addBox(-6.4519F, -3.0657F, -0.4708F, 14.0F, 3.0F, 2.0F, 0.0F, true);

        wingfeather_r6 = new ModelRenderer(this);
        wingfeather_r6.setPos(7.7321F, 10.0147F, -1.6109F);
        wing2.addChild(wingfeather_r6);
        setRotationAngle(wingfeather_r6, 0.5142F, 0.1901F, 0.8751F);
        wingfeather_r6.texOffs(44, 46).addBox(-9.0F, -9.0F, 0.0F, 18.0F, 18.0F, 0.0F, 0.0F, true);

        wing_r6 = new ModelRenderer(this);
        wing_r6.setPos(1.6132F, -3.5751F, -1.7366F);
        wing2.addChild(wing_r6);
        setRotationAngle(wing_r6, 0.5142F, 0.1901F, 0.8751F);
        wing_r6.texOffs(80, 51).addBox(4.2311F, -5.3408F, -0.0051F, 14.0F, 3.0F, 2.0F, 0.0F, true);

        arm1 = new ModelRenderer(this);
        arm1.setPos(-7.8F, -4.5F, 3.0F);

        lowerarm1_r1 = new ModelRenderer(this);
        lowerarm1_r1.setPos(-0.7F, 4.5F, -0.5F);
        arm1.addChild(lowerarm1_r1);
        setRotationAngle(lowerarm1_r1, -0.2618F, 0.0F, 0.0F);
        lowerarm1_r1.texOffs(74, 82).addBox(-1.1F, -5.0F, -1.5F, 3.0F, 10.0F, 3.0F, 0.0F, false);

        upperarm1_r1 = new ModelRenderer(this);
        upperarm1_r1.setPos(0.3F, -4.5F, 0.5F);
        arm1.addChild(upperarm1_r1);
        setRotationAngle(upperarm1_r1, 0.0F, 0.0F, 0.1309F);
        upperarm1_r1.texOffs(62, 82).addBox(-1.5F, -5.0F, -1.5F, 3.0F, 10.0F, 3.0F, 0.0F, false);

        arm2 = new ModelRenderer(this);
        arm2.setPos(6.8F, -4.5F, 3.0F);

        lowerarm2_r1 = new ModelRenderer(this);
        lowerarm2_r1.setPos(0.0F, 0.0F, 0.0F);
        arm2.addChild(lowerarm2_r1);
        setRotationAngle(lowerarm2_r1, -0.2618F, 0.0F, 0.0F);
        lowerarm2_r1.texOffs(0, 89).addBox(-1.2F, -0.5239F, -0.8183F, 3.0F, 10.0F, 3.0F, 0.0F, false);

        upperarm2_r1 = new ModelRenderer(this);
        upperarm2_r1.setPos(0.0F, 0.0F, 0.0F);
        arm2.addChild(upperarm2_r1);
        setRotationAngle(upperarm2_r1, 0.0F, 0.0F, -0.1309F);
        upperarm2_r1.texOffs(86, 82).addBox(-1.2101F, -9.5007F, -1.0F, 3.0F, 10.0F, 3.0F, 0.0F, false);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        resetPose();
        net.kazi.kazimod.preserved.sahur.SahurAnimator.apply(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch,
                true, head, body, arm1, arm2, leg1, leg2, lowerarm1_r1, lowerarm2_r1, wing1, wing2, halo);
    }

    @Override
    public void renderToBuffer(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        head.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        body.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        leg1.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        leg2.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        wing1.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        wing2.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
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
        matrixStack.translate(side == HandSide.RIGHT ? -0.06D : 0.105D, -0.1D, 0.04D);
        matrixStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
    }

    private void resetPose() {
        head.xRot = head.yRot = head.zRot = 0.0F;
        body.xRot = body.yRot = body.zRot = 0.0F;
        halo.xRot = halo.yRot = halo.zRot = 0.0F;
        leg1.xRot = leg1.yRot = leg1.zRot = 0.0F;
        leg2.xRot = leg2.yRot = leg2.zRot = 0.0F;
        wing1.xRot = wing1.yRot = wing1.zRot = 0.0F;
        wing2.xRot = wing2.yRot = wing2.zRot = 0.0F;
        arm1.xRot = arm1.yRot = arm1.zRot = 0.0F;
        arm2.xRot = arm2.yRot = arm2.zRot = 0.0F;
    }

    private void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.xRot = x;
        modelRenderer.yRot = y;
        modelRenderer.zRot = z;
    }
}
