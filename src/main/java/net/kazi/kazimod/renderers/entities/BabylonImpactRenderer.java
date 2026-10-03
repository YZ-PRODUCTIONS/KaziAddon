package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.entities.BabylonImpactEntity;
import net.kazi.kazimod.models.abilities.BabylonFlightMesh;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;

/** Gold contact crowns face away from the struck surface. */
public final class BabylonImpactRenderer extends EntityRenderer<BabylonImpactEntity> {
    private static final ResourceLocation UNUSED = new ResourceLocation("minecraft", "textures/block/white_wool.png");
    private final BabylonVfxBatch surface = new BabylonVfxBatch(2048);
    private final BabylonVfxBatch glow = new BabylonVfxBatch(4096);

    public BabylonImpactRenderer(EntityRendererManager manager) { super(manager); shadowRadius = 0; }
    @Override public ResourceLocation getTextureLocation(BabylonImpactEntity entity) { return UNUSED; }
    @Override public boolean shouldRender(BabylonImpactEntity entity, ClippingHelper frustum, double x, double y, double z) {
        return VfxDetail.visible(entity, frustum, x, y, z, BabylonFlightMesh.IMPACT_RADIUS);
    }

    @Override public void render(BabylonImpactEntity entity, float yaw, float partial,
                                 MatrixStack stack, IRenderTypeBuffer buffers, int light) {
        float age = entity.getVisualAge(partial);
        if (age >= BabylonFlightMesh.IMPACT_TICKS) return;
        Vector3d outward = entity.getOutwardDirection();
        Vector3d camera = BabylonVfxBatch.localCamera(entity, entityRenderDispatcher, partial, outward);
        surface.clear();
        glow.clear();
        BabylonFlightMesh.impact(surface, glow, VfxDetail.level(entity, entityRenderDispatcher, BabylonFlightMesh.IMPACT_RADIUS),
                age, entity.isBlockImpact(), entity.getVisualSeed());
        stack.pushPose();
        GaeBolgVfxRenderer.orient(stack, outward);
        Matrix4f pose = stack.last().pose();
        surface.draw(pose, buffers.getBuffer(BabylonVfxRenderTypes.surface()), camera);
        glow.draw(pose, buffers.getBuffer(BabylonVfxRenderTypes.glow()), camera);
        stack.popPose();
    }
}
