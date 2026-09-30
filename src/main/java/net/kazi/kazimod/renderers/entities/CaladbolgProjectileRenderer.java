package net.kazi.kazimod.renderers.entities;

import java.util.Random;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.entities.projectiles.CaladbolgProjectile;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;

/** White-blue lance with six flickering, forked electrical discharges. */
public class CaladbolgProjectileRenderer extends EntityRenderer<CaladbolgProjectile> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/block/white_wool.png");
    private final Random random = new Random();

    public CaladbolgProjectileRenderer(EntityRendererManager manager) { super(manager); }

    @Override
    public ResourceLocation getTextureLocation(CaladbolgProjectile entity) { return TEXTURE; }

    @Override
    public boolean shouldRender(CaladbolgProjectile entity, ClippingHelper frustum, double x, double y, double z) {
        return VfxDetail.visible(entity, frustum, x, y, z, entity.hasCrimsonVisuals() ? 21 : 12);
    }

    @Override
    public void render(CaladbolgProjectile entity, float yaw, float partialTick,
                       MatrixStack stack, IRenderTypeBuffer buffer, int light) {
        if (entity.isFinished()) return;
        if (entity.hasCrimsonVisuals()) {
            stack.pushPose();
            Vector3d direction = entity.getDeltaMovement();
            if (direction.lengthSqr() < 0.0001) direction = entity.getLookAngle();
            GaeBolgVfxRenderer.orient(stack, direction);
            IVertexBuilder crimson = buffer.getBuffer(CaladbolgVisualGeometry.ENERGY);
            Matrix4f matrix = stack.last().pose();
            GaeBolgGeometry.flight(entity.tickCount + partialTick,
                    VfxDetail.level(entity,this.entityRenderDispatcher,20),
                    (x,y,z,r,g,b,a) -> crimson.vertex(matrix,(float)x,(float)y,(float)z).color(r,g,b,a).endVertex());
            stack.popPose();
            return;
        }
        if (!entity.hasVisuals()) return;
        int detail = VfxDetail.level(entity, this.entityRenderDispatcher, 8);
        IVertexBuilder out = buffer.getBuffer(CaladbolgVisualGeometry.ENERGY);
        Matrix4f pose = stack.last().pose();
        Vector3d forward = entity.getDeltaMovement().normalize();
        if (forward.lengthSqr() < 0.01D) forward = entity.getLookAngle();
        Vector3d rear = forward.scale(-4.0D);
        CaladbolgVisualGeometry.beam(out, pose, rear, forward.scale(1.4D), 0.32F, 0.05F, 0.35F, 1.0F, 0.32F);
        CaladbolgVisualGeometry.beam(out, pose, rear.scale(0.7D), forward, 0.09F, 0.7F, 0.94F, 1.0F, 0.95F);

        // The seed changes every two ticks, never per frame: stable at different FPS.
        random.setSeed(entity.getId() * 1327L + entity.tickCount / 2);
        int segments = VfxDetail.count(detail, 7, 5, 3);
        for (int bolt = 0; bolt < VfxDetail.count(detail, 6, 4, 2); bolt++) {
            Vector3d direction = new Vector3d(random.nextDouble() * 2 - 1,
                    random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1).normalize();
            Vector3d end = direction.scale(3.0D + random.nextDouble() * 4.0D).add(rear.scale(0.4D));
            Vector3d previous = Vector3d.ZERO;
            for (int segment = 1; segment <= segments; segment++) {
                Vector3d point = end.scale(segment / (double) segments).add(
                        (random.nextDouble() - 0.5D) * 0.8D,
                        (random.nextDouble() - 0.5D) * 0.8D,
                        (random.nextDouble() - 0.5D) * 0.8D);
                float width = 0.10F * (1.0F - segment / (float) segments * 0.7F);
                if (detail == 0) CaladbolgVisualGeometry.beam(out, pose, previous, point, width * 3, 0.02F, 0.22F, 1, 0.3F);
                CaladbolgVisualGeometry.beam(out, pose, previous, point, width, 0.28F, 0.75F, 1, 0.85F);
                CaladbolgVisualGeometry.beam(out, pose, previous, point, width * 0.3F, 0.8F, 0.97F, 1, 1);
                if (segment == (segments + 1) / 2) {
                    Vector3d fork = point.add(direction.cross(forward).scale(1.8D)).add(forward.scale(-1));
                    CaladbolgVisualGeometry.beam(out, pose, point, fork, width * 0.6F, 0.2F, 0.65F, 1, 0.8F);
                }
                previous = point;
            }
        }
    }
}
