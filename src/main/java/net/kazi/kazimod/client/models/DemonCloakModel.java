package net.kazi.kazimod.client.models;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.LivingEntity;

public class DemonCloakModel<T extends LivingEntity> extends BipedModel<T> {

    private final ModelRenderer shoulderR;
    private final ModelRenderer shoulderL;
    private final ModelRenderer tendrilR;
    private final ModelRenderer tendrilL;
    private final ModelRenderer tendrilR2;
    private final ModelRenderer tendrilL2;
    private final ModelRenderer tendrilR3;
    private final ModelRenderer tendrilL3;
    private final ModelRenderer backCape;
    private final ModelRenderer capeLower;

    public DemonCloakModel() {
        super(0.3F, 0.0F, 128, 64);

        // No head overlay
        this.head.visible = false;
        this.hat.visible = false;

        // ── Shoulder pads ──
        this.shoulderR = new ModelRenderer(this);
        this.shoulderR.setPos(-5.0F, 2.0F, 0.0F);
        this.shoulderR.texOffs(64, 0).addBox(-3.5F, -3.0F, -3.5F, 7, 4, 7, 0.0F);

        this.shoulderL = new ModelRenderer(this);
        this.shoulderL.setPos(5.0F, 2.0F, 0.0F);
        this.shoulderL.texOffs(92, 0).addBox(-3.5F, -3.0F, -3.5F, 7, 4, 7, 0.0F);

        // ── Right tendril chain (dark wisp from shoulder) ──
        this.tendrilR = new ModelRenderer(this);
        this.tendrilR.setPos(-6.0F, 0.0F, 0.0F);
        this.tendrilR.texOffs(64, 12).addBox(-1.5F, -5.0F, -1.5F, 3, 7, 3, 0.0F);
        this.tendrilR.zRot = 0.5F;
        this.tendrilR.xRot = -0.15F;

        this.tendrilR2 = new ModelRenderer(this);
        this.tendrilR2.setPos(0.0F, -5.0F, 0.0F);
        this.tendrilR2.texOffs(76, 12).addBox(-1.0F, -5.0F, -1.0F, 2, 5, 2, 0.0F);
        this.tendrilR2.zRot = 0.25F;
        this.tendrilR.addChild(this.tendrilR2);

        this.tendrilR3 = new ModelRenderer(this);
        this.tendrilR3.setPos(0.0F, -5.0F, 0.0F);
        this.tendrilR3.texOffs(84, 12).addBox(-0.5F, -4.0F, -0.5F, 1, 4, 1, 0.0F);
        this.tendrilR3.zRot = 0.2F;
        this.tendrilR2.addChild(this.tendrilR3);

        // ── Left tendril chain ──
        this.tendrilL = new ModelRenderer(this);
        this.tendrilL.setPos(6.0F, 0.0F, 0.0F);
        this.tendrilL.texOffs(64, 24).addBox(-1.5F, -5.0F, -1.5F, 3, 7, 3, 0.0F);
        this.tendrilL.zRot = -0.5F;
        this.tendrilL.xRot = -0.15F;

        this.tendrilL2 = new ModelRenderer(this);
        this.tendrilL2.setPos(0.0F, -5.0F, 0.0F);
        this.tendrilL2.texOffs(76, 24).addBox(-1.0F, -5.0F, -1.0F, 2, 5, 2, 0.0F);
        this.tendrilL2.zRot = -0.25F;
        this.tendrilL.addChild(this.tendrilL2);

        this.tendrilL3 = new ModelRenderer(this);
        this.tendrilL3.setPos(0.0F, -5.0F, 0.0F);
        this.tendrilL3.texOffs(84, 24).addBox(-0.5F, -4.0F, -0.5F, 1, 4, 1, 0.0F);
        this.tendrilL3.zRot = -0.2F;
        this.tendrilL2.addChild(this.tendrilL3);

        // ── Back cape ──
        this.backCape = new ModelRenderer(this);
        this.backCape.setPos(0.0F, 0.5F, 2.5F);
        this.backCape.texOffs(64, 36).addBox(-5.0F, 0.0F, 0.0F, 10, 14, 1, 0.0F);
        this.backCape.xRot = 0.06F;

        this.capeLower = new ModelRenderer(this);
        this.capeLower.setPos(0.0F, 14.0F, 0.0F);
        this.capeLower.texOffs(86, 36).addBox(-4.5F, 0.0F, 0.0F, 9, 10, 1, 0.0F);
        this.backCape.addChild(this.capeLower);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // Tendril wave
        float wave = (float) Math.sin(ageInTicks * 0.06) * 0.2F;
        this.tendrilR.zRot = 0.5F + wave;
        this.tendrilL.zRot = -0.5F - wave;

        float wave2 = (float) Math.sin(ageInTicks * 0.09) * 0.3F;
        this.tendrilR2.zRot = 0.25F + wave2;
        this.tendrilL2.zRot = -0.25F - wave2;

        float wave3 = (float) Math.sin(ageInTicks * 0.12) * 0.4F;
        this.tendrilR3.zRot = 0.2F + wave3;
        this.tendrilL3.zRot = -0.2F - wave3;

        // Tendril forward/back sway
        float sway = (float) Math.sin(ageInTicks * 0.04) * 0.1F;
        this.tendrilR.xRot = -0.15F + sway;
        this.tendrilL.xRot = -0.15F - sway;

        // Cape physics
        float capeRot = 0.06F + limbSwingAmount * 0.35F;
        capeRot += (float) Math.sin(ageInTicks * 0.04) * 0.04F;
        this.backCape.xRot = capeRot;

        float lowerRot = (float) Math.sin(ageInTicks * 0.06) * 0.08F;
        lowerRot += limbSwingAmount * 0.25F;
        this.capeLower.xRot = lowerRot;
    }

    @Override
    public void renderToBuffer(MatrixStack matrixStack, IVertexBuilder buffer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        // BipedModel body/arms/legs overlay
        super.renderToBuffer(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);

        // Custom parts
        this.shoulderR.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.shoulderL.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.tendrilR.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.tendrilL.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.backCape.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha * 0.9F);
    }
}
