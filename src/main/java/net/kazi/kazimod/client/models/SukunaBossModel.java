package net.kazi.kazimod.client.models;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.entities.boss.sukuna.SukunaBossEntity;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.util.math.MathHelper;

public class SukunaBossModel extends EntityModel<SukunaBossEntity> {

    private final ModelRenderer head;
    private final ModelRenderer body;
    public  final ModelRenderer leftArm;
    public  final ModelRenderer rightArm;
    private final ModelRenderer leftLeg;
    private final ModelRenderer rightLeg;

    public SukunaBossModel() {
        texWidth  = 64;
        texHeight = 64;

        head = new ModelRenderer(this);
        head.setPos(0.0F, -6.0F, 1.0F);
        head.texOffs(0, 0).addBox(-5.0F, -9.0F, -4.0F, 10.0F, 9.0F, 9.0F, 0.0F, false);
        head.texOffs(38, 0).addBox(-5.5F, -9.5F, -4.1F, 5.0F, 7.0F, 0.0F, 0.0F, false);

        body = new ModelRenderer(this);
        body.setPos(6.0F, -1.0F, 1.0F);
        body.texOffs(0, 18).addBox(-11.0F, -5.0F, -2.0F, 10.0F, 15.0F, 5.0F, 0.0F, false);

        rightArm = new ModelRenderer(this);
        rightArm.setPos(-5.0F, -6.0F, 1.0F);
        rightArm.texOffs(0, 38).addBox(-4.0F, 0.0F, -2.0F, 4.0F, 13.0F, 5.0F, 0.0F, false);

        ModelRenderer rightArm2 = new ModelRenderer(this);
        rightArm2.setPos(-4.0F, 4.0F, -1.0417F);
        rightArm2.xRot = -0.5439F;
        rightArm2.yRot =  0.4984F;
        rightArm2.zRot =  0.7539F;
        rightArm2.texOffs(0, 38).addBox(-2.5F, -7.5F, -2.5F, 5.0F, 15.0F, 5.0F, 0.0F, false);
        rightArm.addChild(rightArm2);

        leftArm = new ModelRenderer(this);
        leftArm.setPos(5.0F, -6.0F, 1.0F);
        leftArm.texOffs(30, 18).addBox(0.0F, 0.0F, -2.0F, 4.0F, 13.0F, 5.0F, 0.0F, false);

        ModelRenderer leftArm2 = new ModelRenderer(this);
        leftArm2.setPos(4.0F, 4.0F, -1.0F);
        leftArm2.xRot = -0.493F;
        leftArm2.yRot = -0.639F;
        leftArm2.zRot = -0.8375F;
        leftArm2.texOffs(30, 18).addBox(-1.0F, -5.0F, -2.0F, 5.0F, 15.0F, 5.0F, 0.0F, false);
        leftArm.addChild(leftArm2);

        rightLeg = new ModelRenderer(this);
        rightLeg.setPos(-2.5F, 9.0F, 0.0F);
        rightLeg.texOffs(40, 38).addBox(-2.4F, 0.0F, -1.0F, 5.0F, 15.0F, 5.0F, 0.0F, false);

        leftLeg = new ModelRenderer(this);
        leftLeg.setPos(2.5F, 9.0F, 0.0F);
        leftLeg.texOffs(20, 38).addBox(-2.6F, 0.0F, -1.0F, 5.0F, 15.0F, 5.0F, 0.0F, false);
    }

    @Override
    public void setupAnim(SukunaBossEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * 0.017453292F;
        head.xRot = headPitch  * 0.017453292F;

        rightArm.xRot = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 2.0F * limbSwingAmount * 0.3F;
        leftArm.xRot  = MathHelper.cos(limbSwing * 0.6662F)                   * 2.0F * limbSwingAmount * 0.3F;
        rightArm.zRot = 0.0F;
        leftArm.zRot  = 0.0F;

        rightLeg.xRot = MathHelper.cos(limbSwing * 0.6662F)                   * 1.4F * limbSwingAmount * 0.6F;
        leftLeg.xRot  = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount * 0.6F;
        rightLeg.yRot = 0.0F;
        leftLeg.yRot  = 0.0F;

        body.xRot = 0.0F;
        body.yRot = 0.0F;
    }

    @Override
    public void renderToBuffer(MatrixStack matrixStack, IVertexBuilder buffer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        head.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        body.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        leftArm.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        rightArm.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        leftLeg.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        rightLeg.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}