package net.kazi.kazimod.models.abilities;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.entities.KurohitsugiSpikeEntity;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;

public class KurohitsugiSpikeModel extends EntityModel<KurohitsugiSpikeEntity> {
    private final ModelRenderer shaft;
    private final ModelRenderer tip;

    public KurohitsugiSpikeModel() {
        this.texWidth = 64;
        this.texHeight = 64;

        this.shaft = new ModelRenderer(this);
        this.shaft.setPos(0.0F, 0.0F, 0.0F);
        this.shaft.texOffs(0, 0).addBox(-6.0F, -6.0F, -60.0F, 12.0F, 12.0F, 52.0F, 0.0F, false);

        this.tip = new ModelRenderer(this);
        this.tip.setPos(0.0F, 0.0F, 0.0F);
        this.tip.texOffs(0, 0).addBox(-3.5F, -3.5F, -88.0F, 7.0F, 7.0F, 28.0F, 0.0F, false);
    }

    @Override
    public void setupAnim(KurohitsugiSpikeEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        this.shaft.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.tip.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
