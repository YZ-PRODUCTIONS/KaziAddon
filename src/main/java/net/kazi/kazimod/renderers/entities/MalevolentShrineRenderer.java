package net.kazi.kazimod.renderers;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.entities.MalevolentShrineEntity;
import net.kazi.kazimod.models.abilities.MalevolentShrineModel;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MalevolentShrineRenderer extends MobRenderer<MalevolentShrineEntity, MalevolentShrineModel> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("kazimod", "textures/models/malevolent_shrine.png");

    public MalevolentShrineRenderer(EntityRendererManager manager) {
        super(manager, new MalevolentShrineModel(), 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(MalevolentShrineEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(MalevolentShrineEntity entity, float entityYaw, float partialTicks,
                       MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        matrixStack.pushPose();
        matrixStack.scale(5.0F, 5.0F, 5.0F);
        super.render(entity, entityYaw, partialTicks, matrixStack, buffer, packedLight);
        matrixStack.popPose();
    }

    public static class Factory implements net.minecraftforge.fml.client.registry.IRenderFactory<MalevolentShrineEntity> {
        @Override
        public net.minecraft.client.renderer.entity.EntityRenderer<? super MalevolentShrineEntity> createRenderFor(EntityRendererManager manager) {
            return new MalevolentShrineRenderer(manager);
        }
    }
}