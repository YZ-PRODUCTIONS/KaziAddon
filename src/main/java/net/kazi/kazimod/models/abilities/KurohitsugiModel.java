package net.kazi.kazimod.models.abilities;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.entities.KurohitsugiEntity;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;

public class KurohitsugiModel extends EntityModel<KurohitsugiEntity> {
    private final ModelRenderer bbMain;

    public KurohitsugiModel() {
        this.texWidth = 256;
        this.texHeight = 256;

        this.bbMain = new ModelRenderer(this);
        this.bbMain.setPos(0.0F, 0.0F, 0.0F);
        this.bbMain.texOffs(0, 0).addBox(-17.5F, -35.0F, -17.5F, 35.0F, 70.0F, 35.0F, 0.0F, false);
    }

    @Override
    public void setupAnim(KurohitsugiEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        this.bbMain.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
