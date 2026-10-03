package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.entities.BabylonVolleyPattern;
import net.kazi.kazimod.entities.GateOfBabylonEntity;
import net.kazi.kazimod.models.abilities.BabylonPortalMesh;
import net.kazi.kazimod.models.abilities.BabylonVisualTimeline;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;

/** An anchored treasury: independently opening, aiming, firing and collapsing gold gates. */
public final class GateOfBabylonRenderer extends EntityRenderer<GateOfBabylonEntity> {
    private static final ResourceLocation UNUSED = new ResourceLocation("minecraft", "textures/block/white_wool.png");
    private final BabylonVfxBatch surface = new BabylonVfxBatch(32768);
    private final BabylonVfxBatch glow = new BabylonVfxBatch(32768);
    private final BabylonVisualTimeline.Frame frame = new BabylonVisualTimeline.Frame();
    private final int[] order = new int[BabylonVolleyPattern.MAX_PORTALS];
    private final double[] distances = new double[BabylonVolleyPattern.MAX_PORTALS];
    private final Vector3d[] offsets = new Vector3d[BabylonVolleyPattern.MAX_PORTALS];

    public GateOfBabylonRenderer(EntityRendererManager manager) { super(manager); shadowRadius = 0; }
    @Override public ResourceLocation getTextureLocation(GateOfBabylonEntity entity) { return UNUSED; }
    @Override public boolean shouldRender(GateOfBabylonEntity entity, ClippingHelper frustum, double x, double y, double z) {
        return entity.shouldRender(x, y, z)
                && frustum.isVisible(new AxisAlignedBB(entity.position(), entity.position()).inflate(entity.getVisualRadius()));
    }

    @Override public void render(GateOfBabylonEntity entity, float yaw, float partial,
                                 MatrixStack stack, IRenderTypeBuffer buffers, int light) {
        float castAge = entity.visualCastAge(partial), motionAge = entity.visualAge(partial);
        float fadeAge = entity.isFading() ? entity.phaseAge(partial) : 0F;
        Vector3d aim = entity.visualAim(partial).subtract(entity.position());
        Vector3d camera = entityRenderDispatcher.camera.getPosition().subtract(entity.position());
        surface.clear(); glow.clear();

        // Draw the small set of overlapping ripple effects from farthest to nearest.
        int portalCount = entity.getPortalCount();
        for (int gate = 0; gate < portalCount; gate++) {
            offsets[gate] = entity.portalOffset(gate);
            distances[gate] = offsets[gate].distanceToSqr(camera);
            int at = gate;
            while (at > 0 && distances[order[at - 1]] < distances[gate]) {
                order[at] = order[at - 1]; at--;
            }
            order[at] = gate;
        }

        for (int sorted = 0; sorted < portalCount; sorted++) {
            int gate = order[sorted];
            BabylonVisualTimeline.sample(frame, gate, castAge, entity.getShotsFired(), entity.isFading(), fadeAge, portalCount);
            if (frame.open <= .0001F || frame.collapse >= .9999F || frame.opacity <= .0001F) continue;
            Vector3d offset = offsets[gate];
            Vector3d forward = aim.subtract(offset).normalize();
            if (forward.lengthSqr() < .000001) forward = entity.getCastDirection();
            int detail = VfxDetail.levelForDistance(Math.max(0, Math.sqrt(distances[gate]) - BabylonPortalMesh.MAX_RADIUS));

            if (frame.weaponOpacity > .001F && frame.emergence > .001F) {
                stack.pushPose();
                stack.translate(offset.x, offset.y, offset.z);
                GaeBolgVfxRenderer.orient(stack, forward);
                BabylonItemModels.renderEmerging(BabylonVolleyPattern.weaponForShot(
                        BabylonVolleyPattern.firstShotForPortal(gate, portalCount)), frame.emergence,
                        1F - frame.weaponOpacity, stack, buffers);
                stack.popPose();
            }
            surface.setFrame(offset, forward); glow.setFrame(offset, forward);
            BabylonPortalMesh.draw(surface, glow, detail, motionAge, frame.open, frame.emergence,
                    frame.recoil, frame.collapse, frame.opacity, entity.visualSeed(gate));
        }

        // Registered item renderers can switch/flush buffers. Acquire VFX builders only
        // after every item, and finish each material before acquiring the next one.
        Matrix4f pose = stack.last().pose();
        surface.draw(pose, buffers.getBuffer(BabylonVfxRenderTypes.surface()), camera);
        glow.draw(pose, buffers.getBuffer(BabylonVfxRenderTypes.glow()), camera);
    }
}
