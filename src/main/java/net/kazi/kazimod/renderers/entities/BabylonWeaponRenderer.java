package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.entities.projectiles.BabylonWeaponEntity;
import net.kazi.kazimod.models.abilities.BabylonFlightMesh;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;

/** The original treasure item flies inside a narrow, softly feathered comet. */
public final class BabylonWeaponRenderer extends EntityRenderer<BabylonWeaponEntity> {
    private static final ResourceLocation UNUSED = new ResourceLocation("minecraft", "textures/block/white_wool.png");
    private final BabylonVfxBatch surface = new BabylonVfxBatch(2048);
    private final BabylonVfxBatch glow = new BabylonVfxBatch(4096);

    public BabylonWeaponRenderer(EntityRendererManager manager) { super(manager); shadowRadius = 0; }
    @Override public ResourceLocation getTextureLocation(BabylonWeaponEntity entity) { return UNUSED; }
    @Override public boolean shouldRender(BabylonWeaponEntity entity, ClippingHelper frustum, double x, double y, double z) {
        return VfxDetail.visible(entity, frustum, x, y, z, BabylonFlightMesh.TRAIL_RADIUS);
    }

    @Override public void render(BabylonWeaponEntity entity, float yaw, float partial,
                                 MatrixStack stack, IRenderTypeBuffer buffers, int light) {
        Vector3d velocity = entity.getDeltaMovement();
        Vector3d camera = BabylonVfxBatch.localCamera(entity, entityRenderDispatcher, partial, velocity);
        stack.pushPose();
        GaeBolgVfxRenderer.orient(stack, velocity);
        // Item rendering can switch and flush shared builders. Obtain VFX buffers
        // only after the model is finished and never retain a builder between passes.
        BabylonItemModels.render(entity.getVariant(), 1, stack, buffers);
        surface.clear();
        glow.clear();
        BabylonFlightMesh.trail(surface, glow, VfxDetail.level(entity, entityRenderDispatcher, BabylonFlightMesh.TRAIL_RADIUS),
                Math.max(0, entity.tickCount - 1 + partial), 1, entity.getId() * 31 + entity.getVariant(), velocity.length());
        Matrix4f pose = stack.last().pose();
        surface.draw(pose, buffers.getBuffer(BabylonVfxRenderTypes.surface()), camera);
        glow.draw(pose, buffers.getBuffer(BabylonVfxRenderTypes.glow()), camera);
        stack.popPose();
    }
}
