package net.kazi.kazimod.client.models;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.entities.boss.gojo.GojoBossEntity;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.util.math.MathHelper;

public class GojoBossModel extends EntityModel<GojoBossEntity> {

    private final ModelRenderer head;
    private final ModelRenderer body;
    // public so GojoHollowPurpleAnimation (and other animations) can pose them
    public final ModelRenderer rightArm;
    public final ModelRenderer leftArm;
    private final ModelRenderer rightLeg;
    private final ModelRenderer leftLeg;

    public GojoBossModel() {
        this.texWidth  = 64;
        this.texHeight = 64;

        (this.head = new ModelRenderer(this)).setPos(0.0f, -2.0f, 0.0f);
        this.head.texOffs(0, 0).addBox(-5.0f, -9.0f, -4.0f, 10.0f, 9.0f, 9.0f, 0.0f, false);

        (this.body = new ModelRenderer(this)).setPos(0.0f, -2.0f, 0.0f);
        this.body.texOffs(0, 18).addBox(-5.0f, 0.0f, -2.0f, 10.0f, 13.0f, 5.0f, 0.0f, false);

        (this.rightArm = new ModelRenderer(this)).setPos(-5.0f, -2.0f, 0.0f);
        this.rightArm.texOffs(38, 0).addBox(-4.0f, 0.0f, -2.0f, 4.0f, 13.0f, 5.0f, 0.0f, false);

        (this.leftArm = new ModelRenderer(this)).setPos(5.0f, -2.0f, 0.0f);
        this.leftArm.texOffs(20, 36).addBox(0.0f, 0.0f, -2.0f, 4.0f, 13.0f, 5.0f, 0.0f, false);

        (this.rightLeg = new ModelRenderer(this)).setPos(-2.5f, 11.0f, 0.0f);
        this.rightLeg.texOffs(0, 36).addBox(-2.5f, 0.0f, -2.0f, 5.0f, 13.0f, 5.0f, 0.0f, false);

        (this.leftLeg = new ModelRenderer(this)).setPos(2.5f, 11.0f, 0.0f);
        this.leftLeg.texOffs(30, 18).addBox(-2.5f, 0.0f, -2.0f, 5.0f, 13.0f, 5.0f, 0.0f, false);
    }

    @Override
    public void setupAnim(final GojoBossEntity entity, final float limbSwing,
                          final float limbSwingAmount, final float ageInTicks,
                          final float netHeadYaw, final float headPitch) {
        this.head.yRot = netHeadYaw * 0.017453292f;
        this.head.xRot = headPitch  * 0.017453292f;

        this.rightArm.xRot = MathHelper.cos(limbSwing * 0.6662f + (float) Math.PI) * 2.0f * limbSwingAmount * 0.5f;
        this.leftArm.xRot  = MathHelper.cos(limbSwing * 0.6662f)                   * 2.0f * limbSwingAmount * 0.5f;
        this.rightArm.zRot = 0.0f;
        this.leftArm.zRot  = 0.0f;

        this.rightLeg.xRot = MathHelper.cos(limbSwing * 0.6662f)                   * 1.4f * limbSwingAmount;
        this.leftLeg.xRot  = MathHelper.cos(limbSwing * 0.6662f + (float) Math.PI) * 1.4f * limbSwingAmount;
        this.rightLeg.yRot = 0.0f;
        this.leftLeg.yRot  = 0.0f;

        this.body.xRot = 0.0f;
        this.body.yRot = 0.0f;
    }

    @Override
    public void renderToBuffer(final MatrixStack stack, final IVertexBuilder buffer,
                               final int packedLight, final int packedOverlay,
                               final float r, final float g, final float b, final float a) {
        this.head.render(stack, buffer, packedLight, packedOverlay, r, g, b, a);
        this.body.render(stack, buffer, packedLight, packedOverlay, r, g, b, a);
        this.rightArm.render(stack, buffer, packedLight, packedOverlay, r, g, b, a);
        this.leftArm.render(stack, buffer, packedLight, packedOverlay, r, g, b, a);
        this.rightLeg.render(stack, buffer, packedLight, packedOverlay, r, g, b, a);
        this.leftLeg.render(stack, buffer, packedLight, packedOverlay, r, g, b, a);
    }
}