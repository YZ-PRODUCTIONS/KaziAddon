package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.entities.InfiniteVoidBarrierEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class InfiniteVoidBarrierRenderer extends EntityRenderer<InfiniteVoidBarrierEntity> {

    public InfiniteVoidBarrierRenderer(EntityRendererManager manager) {
        super(manager);
    }

    @Override
    public void render(InfiniteVoidBarrierEntity entity, float entityYaw, float partialTicks,
                       MatrixStack stack, IRenderTypeBuffer buffer, int packedLight) {
        // No visual — SphereEntity handles visuals separately
    }

    @Override
    public ResourceLocation getTextureLocation(InfiniteVoidBarrierEntity entity) {
        return new ResourceLocation("kazimod", "textures/entity/empty.png");
    }
}