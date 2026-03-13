package net.kazi.kazimod.models.projectiles;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.Entity;

public class FlameArrowProjectileModel extends EntityModel<Entity> {
    private final ModelRenderer bb_main;
    private final ModelRenderer cube_r1;
    private final ModelRenderer cube_r2;
    private final ModelRenderer cube_r3;
    private final ModelRenderer cube_r4;
    private final ModelRenderer cube_r5;
    private final ModelRenderer cube_r6;
    private final ModelRenderer cube_r7;
    private final ModelRenderer cube_r8;
    private final ModelRenderer cube_r9;
    private final ModelRenderer cube_r10;

    public FlameArrowProjectileModel() {
        this.texWidth = 64;
        this.texHeight = 64;

        this.bb_main = new ModelRenderer(this);
        this.bb_main.setPos(0.0F, 24.0F, 0.0F);
        this.bb_main.texOffs(0, 29).addBox(-1.0686F, -8.0686F, -6.3632F, 2.1372F, 2.1372F, 4.7265F, 0.0F, false);
        this.bb_main.texOffs(12, 34).addBox(-0.8112F, -7.8112F, -8.224F, 1.6224F, 1.6224F, 2.548F, 0.0F, false);
        this.bb_main.texOffs(0, 0).addBox(-0.93F, -7.93F, -2.475F, 1.86F, 1.86F, 13.95F, 0.0F, false);

        this.cube_r1 = new ModelRenderer(this);
        this.cube_r1.setPos(0.0F, -3.7149F, -7.7832F);
        this.bb_main.addChild(this.cube_r1);
        this.setRotationAngle(this.cube_r1, -0.3491F, 0.0F, 0.0F);
        this.cube_r1.texOffs(0, 14).addBox(-0.78F, -3.78F, -1.225F, 1.56F, 1.56F, 7.45F, 0.0F, false);

        this.cube_r2 = new ModelRenderer(this);
        this.cube_r2.setPos(-1.1362F, -7.0F, 3.2731F);
        this.bb_main.addChild(this.cube_r2);
        this.setRotationAngle(this.cube_r2, 0.0F, 0.3054F, 1.5708F);
        this.cube_r2.texOffs(32, 24).addBox(1.0F, -2.06F, -2.3633F, 0.0F, 1.86F, 4.7265F, 0.0F, false);

        this.cube_r3 = new ModelRenderer(this);
        this.cube_r3.setPos(-1.1362F, -7.0F, 3.2731F);
        this.bb_main.addChild(this.cube_r3);
        this.setRotationAngle(this.cube_r3, 0.0F, -0.3491F, 1.5708F);
        this.cube_r3.texOffs(32, 19).addBox(-1.0F, -2.06F, -2.3633F, 0.0F, 1.86F, 4.7265F, 0.0F, false);

        this.cube_r4 = new ModelRenderer(this);
        this.cube_r4.setPos(1.4821F, -5.1361F, -4.6163F);
        this.bb_main.addChild(this.cube_r4);
        this.setRotationAngle(this.cube_r4, -0.3125F, -0.1061F, -0.3407F);
        this.cube_r4.texOffs(28, 7).addBox(-0.78F, 0.0F, -3.725F, 1.56F, 0.0F, 7.45F, 0.0F, false);

        this.cube_r5 = new ModelRenderer(this);
        this.cube_r5.setPos(-1.4842F, -5.2312F, -4.5738F);
        this.bb_main.addChild(this.cube_r5);
        this.setRotationAngle(this.cube_r5, -0.3102F, 0.1435F, 0.4203F);
        this.cube_r5.texOffs(28, 0).addBox(-0.78F, 0.0F, -3.725F, 1.56F, 0.0F, 7.45F, 0.0F, false);

        this.cube_r6 = new ModelRenderer(this);
        this.cube_r6.setPos(-1.4301F, -8.686F, -4.5362F);
        this.bb_main.addChild(this.cube_r6);
        this.setRotationAngle(this.cube_r6, 0.3012F, 0.1727F, -0.481F);
        this.cube_r6.texOffs(16, 22).addBox(-0.78F, 0.0F, -3.725F, 1.56F, 0.0F, 7.45F, 0.0F, false);

        this.cube_r7 = new ModelRenderer(this);
        this.cube_r7.setPos(1.4373F, -8.6594F, -4.531F);
        this.bb_main.addChild(this.cube_r7);
        this.setRotationAngle(this.cube_r7, 0.2978F, -0.1848F, 0.5394F);
        this.cube_r7.texOffs(0, 22).addBox(-0.78F, 0.0F, -3.725F, 1.56F, 0.0F, 7.45F, 0.0F, false);

        this.cube_r8 = new ModelRenderer(this);
        this.cube_r8.setPos(-1.7373F, -7.1386F, 13.6961F);
        this.bb_main.addChild(this.cube_r8);
        this.setRotationAngle(this.cube_r8, 0.0F, -0.3491F, 0.0F);
        this.cube_r8.texOffs(32, 14).addBox(-3.0F, -0.7914F, -12.3633F, 0.0F, 1.86F, 4.7265F, 0.0F, false);
        this.cube_r8.texOffs(12, 29).addBox(0.0F, -0.7914F, -2.3633F, 0.0F, 1.86F, 4.7265F, 0.0F, false);

        this.cube_r9 = new ModelRenderer(this);
        this.cube_r9.setPos(1.7383F, -7.1386F, 13.6957F);
        this.bb_main.addChild(this.cube_r9);
        this.setRotationAngle(this.cube_r9, 0.0F, 0.3491F, 0.0F);
        this.cube_r9.texOffs(28, 29).addBox(3.0F, -0.7914F, -12.3632F, 0.0F, 1.86F, 4.7265F, 0.0F, false);
        this.cube_r9.texOffs(20, 29).addBox(0.0F, -0.7914F, -2.3632F, 0.0F, 1.86F, 4.7265F, 0.0F, false);

        this.cube_r10 = new ModelRenderer(this);
        this.cube_r10.setPos(0.0F, -3.7072F, -5.389F);
        this.bb_main.addChild(this.cube_r10);
        this.setRotationAngle(this.cube_r10, 0.3491F, 0.0F, 0.0F);
        this.cube_r10.texOffs(16, 14).addBox(-0.78F, -4.78F, -1.225F, 1.56F, 1.56F, 7.45F, 0.0F, false);
    }

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.bb_main.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.xRot = x;
        modelRenderer.yRot = y;
        modelRenderer.zRot = z;
    }
}