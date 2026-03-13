package net.kazi.kazimod.models.abilities;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.Entity;

public class TimeBubbleModel<T extends Entity> extends EntityModel<T> {

    private final ModelRenderer fullring;
    private final ModelRenderer ring4_r1;
    private final ModelRenderer ring3_r1;
    private final ModelRenderer ring3_r2;
    private final ModelRenderer ring2_r1;
    private final ModelRenderer ring3_r3;
    private final ModelRenderer ring2_r2;
    private final ModelRenderer ball;

    public TimeBubbleModel() {
        texWidth = 512;
        texHeight = 512;

        fullring = new ModelRenderer(this);
        fullring.setPos(1.0063F, -6.0F, 1.0F);
        fullring.texOffs(0, 160).addBox(-32.0063F, -3.0F, -32.0F, 64.0F, 6.0F, 0.0F, 0.0F, false);
        fullring.texOffs(160, 12).addBox(-32.0063F, -3.0F, 32.0F, 64.0F, 6.0F, 0.0F, 0.0F, false);

        ring4_r1 = new ModelRenderer(this);
        ring4_r1.setPos(-32.0063F, 0.0F, -1.0F);
        fullring.addChild(ring4_r1);
        setRotationAngle(ring4_r1, 0.0F, 1.5708F, 0.0F);
        ring4_r1.texOffs(160, 6).addBox(-33.0F, -3.0F, 0.15F, 64.0F, 6.0F, 0.0F, 0.0F, false);

        ring3_r1 = new ModelRenderer(this);
        ring3_r1.setPos(-33.0063F, 5.0F, 7.0F);
        fullring.addChild(ring3_r1);
        setRotationAngle(ring3_r1, 0.0F, -1.5708F, 0.0F);
        ring3_r1.texOffs(160, 6).addBox(-39.0F, -8.0F, -1.0F, 64.0F, 6.0F, 0.0F, 0.0F, false);

        ring3_r2 = new ModelRenderer(this);
        ring3_r2.setPos(30.9937F, 0.0F, 0.0F);
        fullring.addChild(ring3_r2);
        setRotationAngle(ring3_r2, 0.0F, 1.5708F, 0.0F);
        ring3_r2.texOffs(160, 0).addBox(-32.0F, -3.0F, 0.9F, 64.0F, 6.0F, 0.0F, 0.0F, false);

        ring2_r1 = new ModelRenderer(this);
        ring2_r1.setPos(30.9937F, 5.0F, 7.0F);
        fullring.addChild(ring2_r1);
        setRotationAngle(ring2_r1, 0.0F, -1.5708F, 0.0F);
        ring2_r1.texOffs(160, 0).addBox(-39.0F, -8.0F, -1.0F, 64.0F, 6.0F, 0.0F, 0.0F, false);

        ring3_r3 = new ModelRenderer(this);
        ring3_r3.setPos(-0.0063F, 0.0F, 32.0F);
        fullring.addChild(ring3_r3);
        setRotationAngle(ring3_r3, 0.0F, 3.1416F, 0.0F);
        ring3_r3.texOffs(160, 12).addBox(-32.0F, -3.0F, 0.1F, 64.0F, 6.0F, 0.0F, 0.0F, false);

        ring2_r2 = new ModelRenderer(this);
        ring2_r2.setPos(-7.0062F, 5.0F, -32.0F);
        fullring.addChild(ring2_r2);
        setRotationAngle(ring2_r2, 0.0F, 3.1416F, 0.0F);
        ring2_r2.texOffs(0, 160).addBox(-39.0F, -8.0F, -0.1F, 64.0F, 6.0F, 0.0F, 0.0F, false);

        ball = new ModelRenderer(this);
        ball.setPos(20.0F, 14.0F, -18.0F);
        ball.texOffs(160, 18).addBox(-39.0F, -40.0F, -1.0F, 40.0F, 40.0F, 0.0F, 0.0F, false);
        ball.texOffs(160, 58).addBox(-39.0F, -40.0F, 39.0F, 40.0F, 40.0F, 0.0F, 0.0F, false);
        ball.texOffs(0, 0).addBox(-39.0F, 0.0F, -1.0F, 40.0F, 0.0F, 40.0F, 0.0F, false);
        ball.texOffs(0, 40).addBox(-39.0F, -40.0F, -1.0F, 40.0F, 0.0F, 40.0F, 0.0F, false);
        ball.texOffs(0, 80).addBox(1.0F, -40.0F, -1.0F, 0.0F, 40.0F, 40.0F, 0.0F, false);
        ball.texOffs(80, 80).addBox(-39.0F, -40.0F, -1.0F, 0.0F, 40.0F, 40.0F, 0.0F, false);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Slowly spin the ring over time.
        fullring.yRot = ageInTicks * 0.03F;
    }

    @Override
    public void renderToBuffer(MatrixStack matrixStack, IVertexBuilder buffer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        fullring.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        ball.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    private void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.xRot = x;
        modelRenderer.yRot = y;
        modelRenderer.zRot = z;
    }
}