package net.kazi.kazimod.models.projectiles;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;
import net.kazi.kazimod.entities.projectiles.DiceProjectile;

/**
 * Dice renderer — scale 0.12f (2× the original 0.06f).
 * Texture path confirmed from bytecode: textures/entities/projectiles/diceprojectiletexture.png
 */
public class DiceProjectileRenderer extends EntityRenderer<DiceProjectile> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("kazimod", "textures/entities/projectiles/diceprojectiletexture.png");

    private final DiceProjectileModel model = new DiceProjectileModel();

    public DiceProjectileRenderer(EntityRendererManager manager) {
        super(manager);
    }

    @Override
    public ResourceLocation getTextureLocation(DiceProjectile entity) {
        return TEXTURE;
    }

    @Override
    public void render(DiceProjectile entity, float entityYaw, float partialTick,
                       MatrixStack stack, IRenderTypeBuffer buffer, int packedLight) {
        stack.pushPose();

        float yaw   = entity.yRotO + (entity.yRot   - entity.yRotO)   * partialTick;
        float pitch = entity.xRotO + (entity.xRot   - entity.xRotO)   * partialTick;
        float spin  = (entity.level.getGameTime() + partialTick) * 8f;

        stack.mulPose(Vector3f.YP.rotationDegrees(yaw));
        stack.mulPose(Vector3f.XP.rotationDegrees(pitch));
        stack.mulPose(Vector3f.ZP.rotationDegrees(spin));

        stack.scale(0.12f, 0.12f, 0.12f);

        model.renderToBuffer(stack, buffer.getBuffer(RenderType.entitySolid(TEXTURE)),
                packedLight, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);

        stack.popPose();
        super.render(entity, entityYaw, partialTick, stack, buffer, packedLight);
    }
}