package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.entities.CaladbolgImpactEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;

/** Successive red shock spheres around cyan ignition and stacked red-orange blast clouds. */
public class CaladbolgImpactRenderer extends EntityRenderer<CaladbolgImpactEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/block/white_wool.png");
    private static final int SEGMENTS = 64;
    // Lathed blast silhouette: narrow stem, broad overhanging cloud cap, tapered crown.
    private static final float[] HEIGHT = {0, 0.12F, 0.32F, 0.48F, 0.60F, 0.65F, 0.72F, 0.83F, 0.94F, 1.0F};
    private static final float[] WIDTH = {0.25F, 0.34F, 0.23F, 0.19F, 0.31F, 0.88F, 1.0F, 0.88F, 0.55F, 0};
    // Render-thread scratch storage; reused by every cloud layer without vector allocation.
    private static final double[][][] CLOUD_POINTS = new double[HEIGHT.length][SEGMENTS + 1][3];

    public CaladbolgImpactRenderer(EntityRendererManager manager) { super(manager); }

    @Override
    public ResourceLocation getTextureLocation(CaladbolgImpactEntity entity) { return TEXTURE; }

    @Override
    public boolean shouldRender(CaladbolgImpactEntity entity, ClippingHelper frustum, double x, double y, double z) {
        return VfxDetail.visible(entity, frustum, x, y, z, 30);
    }

    @Override
    public void render(CaladbolgImpactEntity entity, float yaw, float partialTick,
                       MatrixStack stack, IRenderTypeBuffer buffer, int light) {
        float age = entity.getVisualAge(partialTick);
        if (age >= CaladbolgImpactEntity.LIFETIME) return;
        float fade = clamp((CaladbolgImpactEntity.LIFETIME - age) / 18.0F);
        float heat = clamp((age - 7.0F) / 15.0F);
        float radius = CaladbolgImpactEntity.BASE_RADIUS
                + Math.min(age, CaladbolgImpactEntity.GROWTH_TICKS) * CaladbolgImpactEntity.EXPANSION_PER_TICK;
        float red = 0.14F + 0.86F * heat;
        float green = 0.75F - 0.50F * heat;
        float blue = 1.0F - 0.94F * heat;
        IVertexBuilder out = buffer.getBuffer(CaladbolgVisualGeometry.ENERGY);
        Matrix4f pose = stack.last().pose();
        float seed = (entity.getId() % 97) * 0.17F;
        int detail = VfxDetail.level(entity, this.entityRenderDispatcher, 25);
        int segments = VfxDetail.count(detail, 64, 32, 16);

        // A layered, uneven column unfolds into the reference's mushroom silhouette.
        float rise = 0.35F + 0.65F * clamp(age / 23.0F);
        cloud(out, pose, radius * 0.88F, radius * rise, age, seed, red, green, blue, fade * 0.28F, segments);
        if (detail < 2) {
            cloud(out, pose, radius * 0.67F, radius * rise * 1.06F, age, seed + 2,
                    1.0F, 0.86F - heat * 0.32F, 0.95F - heat * 0.75F, fade * 0.35F, segments);
        }

        // Hot, irregular spires spread around the central flash, not a smooth sphere.
        int plumes = VfxDetail.count(detail, 20, 12, 6);
        for (int plume = 0; plume < plumes; plume++) {
            double angle = plume * Math.PI * 2 / plumes + seed;
            float reach = radius * (0.35F + 0.30F * (float) (0.5D + 0.5D * Math.sin(plume * 7.13D)));
            float height = radius * (0.35F + 0.45F * (float) (0.5D + 0.5D * Math.cos(plume * 3.7D)));
            Vector3d from = CaladbolgVisualGeometry.polar(angle, radius * 0.04F, 0.3D);
            Vector3d bend = CaladbolgVisualGeometry.polar(angle + 0.12D * Math.sin(age * 0.15D + plume),
                    reach * 0.6D, height * 0.45D);
            Vector3d tip = CaladbolgVisualGeometry.polar(angle, reach, height * rise);
            float alpha = fade * (0.55F - 0.25F * clamp(age / 40.0F));
            CaladbolgVisualGeometry.beam(out, pose, from, bend, radius * 0.04F, red, green, blue, alpha);
            CaladbolgVisualGeometry.beam(out, pose, bend, tip, radius * 0.018F, 1, 0.9F, 0.65F, alpha);
            CaladbolgVisualGeometry.beam(out, pose, from, tip.scale(0.75D), radius * 0.009F, 1, 1, 0.93F, alpha);
        }

        // Three one-shot shockwaves sweep outward, then the cloud belts hang above them.
        for (int wave = 0; wave < 3; wave++) {
            float phase = (age - wave * 4) / 28.0F;
            if (phase >= 0 && phase <= 1) {
                float ringRadius = radius * (0.18F + 0.80F * phase);
                CaladbolgVisualGeometry.ring(out, pose, ringRadius, 0.35F + wave * 0.3F,
                        0.7F, age * 0.12F, red, green, blue, fade * (1 - phase) * 0.8F, segments);
            }
            float beltRadius = radius * (0.38F + wave * 0.22F);
            CaladbolgVisualGeometry.ring(out, pose, beltRadius, radius * rise * (0.23F + wave * 0.22F),
                    radius * 0.028F, age * 0.08F + wave, 1, 0.50F, 0.14F,
                    fade * clamp((age - 8) / 10) * 0.5F, segments);
        }
        // Short central overexposure, fading before the orange afterglow.
        float flash = fade * clamp(1 - age / 16.0F);
        CaladbolgVisualGeometry.beam(out, pose, new Vector3d(0, 0.1D, 0),
                new Vector3d(0, radius * rise, 0), radius * 0.16F, 0.9F, 0.98F, 1, flash);

        // This renderer only exists after a block impact, never during projectile flight.
        // Draw after the existing blast so the expanding red pulses surround its light.
        CaladbolgImpactSphere.render(buffer, pose, age, radius, seed,
                this.entityRenderDispatcher.camera.getPosition().subtract(entity.position()), detail);
    }

    private static void cloud(IVertexBuilder out, Matrix4f pose, float radius, float height,
                              float age, float seed, float red, float green, float blue, float alpha, int segments) {
        if (alpha <= 0.001F) return;
        for (int band = 0; band < HEIGHT.length; band++) {
            for (int i = 0; i <= segments; i++) {
                cloudPoint(CLOUD_POINTS[band][i], (i % segments) * Math.PI * 2 / segments,
                        band, radius, height, age, seed);
            }
        }
        for (int band = 0; band < HEIGHT.length - 1; band++) {
            for (int i = 0; i < segments; i++) {
                double a = i * Math.PI * 2 / segments;
                float shade = band == 4 ? 0.30F : 0.65F + 0.35F * (float) Math.sin(a * 6 + seed + age * 0.07F);
                cloudVertex(out, pose, CLOUD_POINTS[band][i], red * shade, green * shade, blue * shade, alpha);
                cloudVertex(out, pose, CLOUD_POINTS[band][i + 1], red * shade, green * shade, blue * shade, alpha);
                cloudVertex(out, pose, CLOUD_POINTS[band + 1][i + 1], red, green, blue, alpha * 0.8F);
                cloudVertex(out, pose, CLOUD_POINTS[band + 1][i], red, green, blue, alpha * 0.8F);
            }
        }
    }

    private static void cloudPoint(double[] point, double angle, int band, float radius, float height, float age, float seed) {
        double noise = 1 + 0.075D * Math.sin(angle * 7 + seed + age * 0.09D)
                + 0.045D * Math.sin(angle * 13 - age * 0.05D + band);
        double radial = radius * WIDTH[band] * noise;
        point[0] = Math.cos(angle) * radial;
        point[1] = height * HEIGHT[band] * (1 + 0.025D * Math.sin(angle * 5 + seed));
        point[2] = Math.sin(angle) * radial;
    }

    private static void cloudVertex(IVertexBuilder out, Matrix4f pose, double[] point,
                                      float red, float green, float blue, float alpha) {
        CaladbolgVisualGeometry.vertex(out, pose, point[0], point[1], point[2], red, green, blue, alpha);
    }

    private static float clamp(float value) { return Math.max(0, Math.min(1, value)); }
}
