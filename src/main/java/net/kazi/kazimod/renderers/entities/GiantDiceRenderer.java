package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;
import net.kazi.kazimod.entities.GiantDiceEntity;
import net.kazi.kazimod.models.projectiles.DiceProjectileModel;

/**
 * GiantDiceRenderer — same model/texture as DiceProjectileRenderer but
 * 7× bigger: scale 0.42f (original 0.06f × 7).
 */
public class GiantDiceRenderer extends EntityRenderer<GiantDiceEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("kazimod", "textures/entities/projectiles/diceprojectiletexture.png");

    private final DiceProjectileModel model = new DiceProjectileModel();

    public GiantDiceRenderer(EntityRendererManager manager) {
        super(manager);
    }

    @Override
    public ResourceLocation getTextureLocation(GiantDiceEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(GiantDiceEntity entity, float entityYaw, float partialTick,
                       MatrixStack stack, IRenderTypeBuffer buffer, int packedLight) {
        stack.pushPose();

        float speed = entity.isDescending() ? 30f : 3f;
        float spin  = (entity.level.getGameTime() + partialTick) * speed;

        stack.mulPose(Vector3f.YP.rotationDegrees(spin));
        stack.mulPose(Vector3f.XP.rotationDegrees(spin * 0.6f));

        // 7× bigger than base dice (0.06 × 7 = 0.42)
        stack.scale(3.4f, 3.4f, 3.4f);

        model.renderToBuffer(stack, buffer.getBuffer(RenderType.entitySolid(TEXTURE)),
                packedLight, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);

        stack.popPose();
        super.render(entity, entityYaw, partialTick, stack, buffer, packedLight);
    }

    public static class Factory implements net.minecraftforge.fml.client.registry.IRenderFactory<GiantDiceEntity> {
        @Override
        public EntityRenderer<? super GiantDiceEntity> createRenderFor(EntityRendererManager mgr) {
            return new GiantDiceRenderer(mgr);
        }
    }
}