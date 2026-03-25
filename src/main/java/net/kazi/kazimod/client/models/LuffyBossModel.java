package net.kazi.kazimod.client.models;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.entities.boss.luffy.LuffyBossEntity;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.util.math.MathHelper;

/**
 * Slim (Alex) player model for LuffyBossEntity.
 * Pivot positions and box offsets copied exactly from vanilla BipedModel
 * with slim arm variant (3px wide arms, not 4px).
 *
 * Pivot  head:     ( 0,  0,  0)
 * Pivot  body:     ( 0,  0,  0)
 * Pivot  rightArm: (-5,  2,  0)   box: (-3,-2,-2)  size 3×12×4
 * Pivot  leftArm:  ( 5,  2,  0)   box: ( 0,-2,-2)  size 3×12×4
 * Pivot  rightLeg: (-2, 12,  0)   box: (-2, 0,-2)  size 4×12×4
 * Pivot  leftLeg:  ( 2, 12,  0)   box: (-2, 0,-2)  size 4×12×4
 */
public class LuffyBossModel extends EntityModel<LuffyBossEntity> {

    private final ModelRenderer head;
    private final ModelRenderer body;
    private final ModelRenderer rightArm;
    private final ModelRenderer leftArm;
    private final ModelRenderer rightLeg;
    private final ModelRenderer leftLeg;

    public LuffyBossModel() {
        texWidth  = 64;
        texHeight = 64;

        // ── Head ──────────────────────────────────────────────────────────────
        head = new ModelRenderer(this);
        head.setPos(0.0F, 0.0F, 0.0F);
        head.texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, 0.0F);

        // ── Body ──────────────────────────────────────────────────────────────
        body = new ModelRenderer(this);
        body.setPos(0.0F, 0.0F, 0.0F);
        body.texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, 0.0F);

        // ── Right arm (slim: 3px wide) ─────────────────────────────────────
        // Pivot at (-5, 2, 0) — flush against right side of body
        rightArm = new ModelRenderer(this);
        rightArm.setPos(-5.0F, 2.0F, 0.0F);
        rightArm.texOffs(40, 16).addBox(-2.0F, -2.0F, -2.0F, 3, 12, 4, 0.0F);

        // ── Left arm (slim: 3px wide) ──────────────────────────────────────
        // Pivot at (5, 2, 0) — flush against left side of body
        leftArm = new ModelRenderer(this);
        leftArm.setPos(5.0F, 2.0F, 0.0F);
        leftArm.texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 3, 12, 4, 0.0F);

        // ── Right leg ──────────────────────────────────────────────────────
        rightLeg = new ModelRenderer(this);
        rightLeg.setPos(-2.0F, 12.0F, 0.0F);
        rightLeg.texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, 0.0F);

        // ── Left leg ───────────────────────────────────────────────────────
        leftLeg = new ModelRenderer(this);
        leftLeg.setPos(2.0F, 12.0F, 0.0F);
        leftLeg.texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, 0.0F);
    }

    @Override
    public void setupAnim(LuffyBossEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw  * 0.017453292F;
        head.xRot = headPitch   * 0.017453292F;

        // Arm swing — MathHelper.sin × 0.5 (matches GojoBossModel)
        rightArm.xRot = MathHelper.sin(limbSwing * 0.6662F + (float) Math.PI) * 2.0F * limbSwingAmount * 0.5F;
        leftArm.xRot  = MathHelper.sin(limbSwing * 0.6662F)                   * 2.0F * limbSwingAmount * 0.5F;
        rightArm.zRot = 0.0F;
        leftArm.zRot  = 0.0F;

        // Leg swing — × 1.4
        rightLeg.xRot = MathHelper.sin(limbSwing * 0.6662F)                   * 1.4F * limbSwingAmount;
        leftLeg.xRot  = MathHelper.sin(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
    }

    @Override
    public void renderToBuffer(MatrixStack matrixStack, IVertexBuilder buffer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        head.render(matrixStack,     buffer, packedLight, packedOverlay, red, green, blue, alpha);
        body.render(matrixStack,     buffer, packedLight, packedOverlay, red, green, blue, alpha);
        rightArm.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        leftArm.render(matrixStack,  buffer, packedLight, packedOverlay, red, green, blue, alpha);
        rightLeg.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        leftLeg.render(matrixStack,  buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}