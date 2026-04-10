package net.kazi.kazimod.client.models;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.LivingEntity;

public class DemonWingsModel<T extends LivingEntity> extends EntityModel<T> {

    private final ModelRenderer rightWing;
    private final ModelRenderer rightWingOuter;
    private final ModelRenderer rightWingTip;
    private final ModelRenderer leftWing;
    private final ModelRenderer leftWingOuter;
    private final ModelRenderer leftWingTip;
    private final ModelRenderer darkAura;

    public DemonWingsModel() {
        this.texWidth = 128;
        this.texHeight = 64;

        // ── Right wing (inner segment) ──
        this.rightWing = new ModelRenderer(this);
        this.rightWing.setPos(-2.0F, 2.0F, 2.0F);
        // Flat wing panel angled outward
        this.rightWing.texOffs(0, 0).addBox(-12.0F, -8.0F, 0.0F, 12.0F, 16.0F, 1.0F, 0.0F, true);
        this.rightWing.zRot = 0.15F;
        this.rightWing.yRot = 0.3F;

        // ── Right wing outer segment ──
        this.rightWingOuter = new ModelRenderer(this);
        this.rightWingOuter.setPos(-12.0F, -4.0F, 0.0F);
        this.rightWingOuter.texOffs(26, 0).addBox(-10.0F, -6.0F, 0.0F, 10.0F, 14.0F, 1.0F, 0.0F, true);
        this.rightWingOuter.zRot = 0.25F;
        this.rightWingOuter.yRot = 0.15F;
        this.rightWing.addChild(this.rightWingOuter);

        // ── Right wing tip ──
        this.rightWingTip = new ModelRenderer(this);
        this.rightWingTip.setPos(-10.0F, -2.0F, 0.0F);
        this.rightWingTip.texOffs(48, 0).addBox(-8.0F, -4.0F, 0.0F, 8.0F, 10.0F, 1.0F, 0.0F, true);
        this.rightWingTip.zRot = 0.35F;
        this.rightWingTip.yRot = 0.1F;
        this.rightWingOuter.addChild(this.rightWingTip);

        // ── Left wing (mirror of right) ──
        this.leftWing = new ModelRenderer(this);
        this.leftWing.setPos(2.0F, 2.0F, 2.0F);
        this.leftWing.texOffs(0, 32).addBox(0.0F, -8.0F, 0.0F, 12.0F, 16.0F, 1.0F, 0.0F, false);
        this.leftWing.zRot = -0.15F;
        this.leftWing.yRot = -0.3F;

        this.leftWingOuter = new ModelRenderer(this);
        this.leftWingOuter.setPos(12.0F, -4.0F, 0.0F);
        this.leftWingOuter.texOffs(26, 32).addBox(0.0F, -6.0F, 0.0F, 10.0F, 14.0F, 1.0F, 0.0F, false);
        this.leftWingOuter.zRot = -0.25F;
        this.leftWingOuter.yRot = -0.15F;
        this.leftWing.addChild(this.leftWingOuter);

        this.leftWingTip = new ModelRenderer(this);
        this.leftWingTip.setPos(10.0F, -2.0F, 0.0F);
        this.leftWingTip.texOffs(48, 32).addBox(0.0F, -4.0F, 0.0F, 8.0F, 10.0F, 1.0F, 0.0F, false);
        this.leftWingTip.zRot = -0.35F;
        this.leftWingTip.yRot = -0.1F;
        this.leftWingOuter.addChild(this.leftWingTip);

        // ── Dark aura around body ──
        this.darkAura = new ModelRenderer(this);
        this.darkAura.setPos(0.0F, 0.0F, 0.0F);
        this.darkAura.texOffs(72, 0).addBox(-5.0F, -1.0F, -3.0F, 10.0F, 14.0F, 6.0F, 0.3F, false);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Gentle wing flap animation
        float wingFlap = (float) Math.sin(ageInTicks * 0.05F) * 0.1F;
        this.rightWing.zRot = 0.15F + wingFlap;
        this.leftWing.zRot = -0.15F - wingFlap;

        // Slight forward/back sway
        float sway = (float) Math.sin(ageInTicks * 0.03F) * 0.05F;
        this.rightWing.yRot = 0.3F + sway;
        this.leftWing.yRot = -0.3F - sway;

        // Wing tips flutter
        float tipFlutter = (float) Math.sin(ageInTicks * 0.08F) * 0.15F;
        this.rightWingTip.zRot = 0.35F + tipFlutter;
        this.leftWingTip.zRot = -0.35F - tipFlutter;
    }

    @Override
    public void renderToBuffer(MatrixStack matrixStack, IVertexBuilder buffer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        this.rightWing.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.leftWing.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.darkAura.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha * 0.4F);
    }
}
