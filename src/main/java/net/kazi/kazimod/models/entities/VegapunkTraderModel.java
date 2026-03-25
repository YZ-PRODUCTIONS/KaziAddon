package net.kazi.kazimod.models.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.entities.VegapunkTraderEntity;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;

public class VegapunkTraderModel extends EntityModel<VegapunkTraderEntity> {

    private final ModelRenderer head;
    private final ModelRenderer body;
    private final ModelRenderer leftArm;
    private final ModelRenderer rightArm;
    private final ModelRenderer leftLeg;
    private final ModelRenderer rightLeg;

    public VegapunkTraderModel() {
        texWidth  = 128;
        texHeight = 128;

        // ── HEAD ─────────────────────────────────────────────────────────────
        // Kept exactly as Blockbench — head already has its own correct pivot
        head = new ModelRenderer(this);
        head.setPos(2.0F, -17.0F, 1.0F);
        head.texOffs(40, 50).addBox(-6.0F, -1.0F, -5.0F, 8.0F, 4.0F, 1.0F, 0.0F, false);
        head.texOffs(0,  17).addBox(-6.0F, -1.0F, -5.0F, 8.0F, 0.0F, 8.0F, 0.0F, false);
        head.texOffs(54, 27).addBox(-6.0F, -1.0F,  2.0F, 8.0F, 4.0F, 1.0F, 0.0F, false);
        head.texOffs(54, 47).addBox(-1.0F, -3.0F, -1.0F, 3.0F, 2.0F, 0.0F, 0.0F, false);
        head.texOffs(54, 37).addBox(-3.0F, -3.0F, -2.0F, 2.0F, 8.0F, 2.0F, 0.0F, false);
        head.texOffs( 0,  0).addBox(-6.0F,  4.0F, -5.0F, 8.0F, 9.0F, 8.0F, 0.0F, false);
        head.texOffs(32, 13).addBox(-9.0F,  4.0F,  3.0F,14.0F, 9.0F, 0.0F, 0.0F, false);

        ModelRenderer tongue = new ModelRenderer(this);
        tongue.setPos(-2.0F, 13.0F, -5.0F);
        tongue.xRot = -0.2618F;
        tongue.texOffs(29, 51).addBox(-2.0F, -2.0F, -0.6F, 4.0F, 11.0F, 1.0F, 0.0F, false);
        head.addChild(tongue);

        ModelRenderer apple3 = new ModelRenderer(this);
        apple3.setPos(1.0F, 3.0F, -1.0F);
        apple3.yRot = -1.5708F;
        apple3.texOffs(54, 32).addBox(-4.0F, -4.0F, -1.0F, 8.0F, 4.0F, 1.0F, 0.0F, false);
        head.addChild(apple3);

        ModelRenderer apple2 = new ModelRenderer(this);
        apple2.setPos(-6.0F, 3.0F, -1.0F);
        apple2.yRot = -1.5708F;
        apple2.texOffs(54, 22).addBox(-4.0F, -4.0F, -1.0F, 8.0F, 4.0F, 1.0F, 0.0F, false);
        head.addChild(apple2);

        // ── BODY ─────────────────────────────────────────────────────────────
        // Pivot at (0,0,0). Body occupies y=-4 to y=5 in model space.
        body = new ModelRenderer(this);
        body.setPos(0.0F, 0.0F, 0.0F);
        body.texOffs(32, 0).addBox(-4.0F, -4.0F, -2.0F, 8.0F, 9.0F, 4.0F, 0.0F, false);

        // ── RIGHT ARM ────────────────────────────────────────────────────────
        // Pivot at shoulder: x=-5 (right side), y=-4 (top of arm = shoulder level)
        // addBox origin = absolute(-7,-4,-2) - pivot(-5,-4,0) = (-2, 0, -2)
        rightArm = new ModelRenderer(this);
        rightArm.setPos(-5.0F, -4.0F, 0.0F);
        rightArm.texOffs(14, 47).addBox(-2.0F, 0.0F, -2.0F, 3.0F, 10.0F, 4.0F, 0.0F, false);

        // ── LEFT ARM ─────────────────────────────────────────────────────────
        // Pivot at shoulder: x=5 (left side), y=-4
        // addBox origin = absolute(4,-4,-2) - pivot(5,-4,0) = (-1, 0, -2)
        leftArm = new ModelRenderer(this);
        leftArm.setPos(5.0F, -4.0F, 0.0F);
        leftArm.texOffs(0, 47).addBox(-1.0F, 0.0F, -2.0F, 3.0F, 10.0F, 4.0F, 0.0F, false);

        // ── RIGHT LEG ────────────────────────────────────────────────────────
        // Pivot at hip: y=5 (bottom of body box), x=-2
        // addBox origin = absolute(-3.9,5,-2) - pivot(-2,5,0) = (-1.9, 0, -2)
        // boot addBox   = absolute(-5,16,-3)  - pivot(-2,5,0) = (-3, 11, -3)
        rightLeg = new ModelRenderer(this);
        rightLeg.setPos(-2.0F, 5.0F, 0.0F);
        rightLeg.texOffs(16, 25).addBox(-1.9F,  0.0F, -2.0F, 4.0F, 18.0F, 4.0F, 0.0F, false);
        rightLeg.texOffs(32, 36).addBox(-3.0F, 11.0F, -3.0F, 5.0F,  8.0F, 6.0F, 0.0F, false);

        // ── LEFT LEG ─────────────────────────────────────────────────────────
        // Pivot at hip: y=5, x=2
        // addBox origin = absolute(-0.1,5,-2) - pivot(2,5,0) = (-2.1, 0, -2)
        // boot addBox   = absolute(0,16,-3)   - pivot(2,5,0) = (-2, 11, -3)
        leftLeg = new ModelRenderer(this);
        leftLeg.setPos(2.0F, 5.0F, 0.0F);
        leftLeg.texOffs(0,  25).addBox(-2.1F,  0.0F, -2.0F, 4.0F, 18.0F, 4.0F, 0.0F, false);
        leftLeg.texOffs(32, 22).addBox(-2.0F, 11.0F, -3.0F, 5.0F,  8.0F, 6.0F, 0.0F, false);
    }

    @Override
    public void setupAnim(VegapunkTraderEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        head.xRot = headPitch  * ((float) Math.PI / 180F);

        rightArm.xRot = (float) Math.cos(limbSwing * 0.6662F + (float) Math.PI) * 2.0F * limbSwingAmount * 0.5F;
        leftArm.xRot  = (float) Math.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F;
        rightArm.zRot = 0.0F;
        leftArm.zRot  = 0.0F;
        rightLeg.xRot = (float) Math.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        leftLeg.xRot  = (float) Math.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
        rightLeg.yRot = 0.0F;
        leftLeg.yRot  = 0.0F;
    }

    @Override
    public void renderToBuffer(MatrixStack matrixStack, IVertexBuilder buffer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        head.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        body.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        rightArm.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        leftArm.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        rightLeg.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        leftLeg.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}