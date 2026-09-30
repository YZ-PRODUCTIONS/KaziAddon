package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.entities.MalevolentShrineEntity;
import net.kazi.kazimod.models.abilities.KamaVfxMesh;
import net.kazi.kazimod.models.abilities.MalevolentShrineModel;
import net.kazi.kazimod.renderers.entities.KamaVfxRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.entity.MobEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.client.registry.IRenderFactory;

@OnlyIn(value=Dist.CLIENT)
public class MalevolentShrineRenderer
extends MobRenderer<MalevolentShrineEntity, MalevolentShrineModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("kazimod", "textures/models/malevolent_shrine.png");

    public MalevolentShrineRenderer(EntityRendererManager manager) {
        super(manager, new MalevolentShrineModel(), 1.0f);
    }

    public ResourceLocation getTextureLocation(MalevolentShrineEntity entity) {
        return TEXTURE;
    }

    public void render(MalevolentShrineEntity entity, float entityYaw, float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        matrixStack.pushPose();
        matrixStack.scale(5.0f, 5.0f, 5.0f);
        super.render(entity, entityYaw, partialTicks, matrixStack, buffer, packedLight);
        matrixStack.popPose();
        if (entity.getDomainRadius() > 0.0f) {
            Vector3d origin = entity.getDomainOrigin();
            matrixStack.pushPose();
            matrixStack.translate(origin.x - MathHelper.lerp((double)partialTicks, (double)entity.xOld, (double)entity.getX()), origin.y - MathHelper.lerp((double)partialTicks, (double)entity.yOld, (double)entity.getY()), origin.z - MathHelper.lerp((double)partialTicks, (double)entity.zOld, (double)entity.getZ()));
            Vector3f right = new Vector3f(1.0f, 0.0f, 0.0f);
            Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
            right.transform(this.entityRenderDispatcher.cameraOrientation());
            up.transform(this.entityRenderDispatcher.cameraOrientation());
            KamaVfxMesh.domain(KamaVfxRenderer.slashSink(matrixStack, buffer), entity.getVfxAge(partialTicks), entity.getDomainRadius(), right.x(), right.y(), right.z(), up.x(), up.y(), up.z());
            matrixStack.popPose();
        }
    }

    public static class Factory
    implements IRenderFactory<MalevolentShrineEntity> {
        public EntityRenderer<? super MalevolentShrineEntity> createRenderFor(EntityRendererManager manager) {
            return new MalevolentShrineRenderer(manager);
        }
    }
}
