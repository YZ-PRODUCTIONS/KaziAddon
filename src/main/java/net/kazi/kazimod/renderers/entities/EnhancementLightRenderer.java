package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import java.util.Random;
import net.kazi.kazimod.entities.EnhancementLightEntity;
import net.kazi.kazimod.entities.EnhancementBlastShape;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

/** An overhead white-gold eruption gathers, then releases into the full-length blast. */
public class EnhancementLightRenderer extends EntityRenderer<EnhancementLightEntity> {
    public EnhancementLightRenderer(EntityRendererManager manager) {
        super(manager);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(EnhancementLightEntity entity) { return EnhancementBlastModel.TEXTURE; }

    @Override
    public boolean shouldRender(EnhancementLightEntity entity, ClippingHelper frustum, double x, double y, double z) {
        return VfxDetail.visible(entity, frustum, x, y, z, entity.isReleased() ? 100 : 22);
    }

    @Override
    public void render(EnhancementLightEntity entity, float yaw, float partialTick,
                       MatrixStack stack, IRenderTypeBuffer buffer, int light) {
        float age = entity.getPhaseAge(partialTick);
        if (entity.isReleased() && age >= EnhancementLightEntity.BLAST_TICKS) return;
        // Keep the complete visual timeline synchronized with the scaled phase durations.
        float effectAge = age / EnhancementLightEntity.EFFECT_DURATION_SCALE;
        int detail = VfxDetail.level(entity, this.entityRenderDispatcher, entity.isReleased() ? 80 : 16);
        stack.pushPose();
        if (entity.isReleased()) {
            Vector3d origin = entity.getBlastOrigin();
            stack.translate(origin.x - MathHelper.lerp(partialTick, entity.xOld, entity.getX()),
                    origin.y - MathHelper.lerp(partialTick, entity.yOld, entity.getY()),
                    origin.z - MathHelper.lerp(partialTick, entity.zOld, entity.getZ()));
        }
        // Collision applies the same shared offset to the released mesh.
        stack.translate(0.0D, EnhancementBlastShape.RENDER_OFFSET_Y, 0.0D);
        stack.mulPose(Vector3f.YP.rotationDegrees(-entity.getBlastYaw()));
        if (entity.isReleased()) {
            float deployment = smooth(clamp(effectAge / 3.0F));
            float chargeFade = 1 - smooth(clamp(effectAge / 4.0F));
            if (chargeFade > 0.001F) {
                renderCharge(buffer, stack, 1.0F,
                        EnhancementLightEntity.CHARGE_TICKS / EnhancementLightEntity.EFFECT_DURATION_SCALE + effectAge,
                        entity.getId(), detail, chargeFade);
            }
            float dissolve = EnhancementBlastModel.fadeProgress(effectAge);
            float remaining = 1 - dissolve;
            float surfaceFade = deployment * remaining * remaining;
            float glowFade = deployment * remaining * (1 - dissolve * 0.35F);
            IVertexBuilder out = buffer.getBuffer(EnhancementBlastModel.TRANSLUCENT);
            EnhancementBlastModel.render(out, stack.last().pose(), effectAge, entity.getId(), surfaceFade, detail);
            // Expanded copies of the same silhouette form a soft, depth-tested halo.
            // They do not add particles, alter the hitbox, or reveal the blast through walls.
            IVertexBuilder glow = buffer.getBuffer(EnhancementBlastModel.GLOW);
            float pulse = 0.92F + 0.08F * remaining * (float) Math.sin(effectAge * 1.6F);
            float flash = EnhancementBlastModel.blastFlash(effectAge);
            // A brighter, wider flare marks both the initial impact and the start of fading.
            EnhancementBlastModel.renderGlow(glow, stack.last().pose(), effectAge, entity.getId(),
                    glowFade * pulse * (0.22F + flash * 0.16F), 1.035F + flash * 0.04F + dissolve * 0.06F, detail);
            if (detail == 0) {
                EnhancementBlastModel.renderGlow(glow, stack.last().pose(), effectAge, entity.getId(),
                        glowFade * pulse * (0.07F + flash * 0.08F), 1.10F + flash * 0.10F + dissolve * 0.18F, detail);
            }
            EnhancementBlastRays.render(buffer.getBuffer(CaladbolgVisualGeometry.ENERGY),
                    stack.last().pose(), age, entity.getId(), glowFade, detail);
            EnhancementPressureWaves.render(buffer, stack, effectAge, entity.getId(), remaining, detail);
        } else {
            renderCharge(buffer, stack, entity.getChargeProgress()
                    + partialTick / EnhancementLightEntity.CHARGE_TICKS, effectAge, entity.getId(), detail, 1.0F);
        }
        stack.popPose();
    }

    private static void renderCharge(IRenderTypeBuffer buffer, MatrixStack stack,
                                      float progress, float age, int seed, int detail, float opacity) {
        stack.pushPose();
        stack.translate(0.0D, 1.0D, 0.0D);
        float formation = smooth(clamp(progress));
        float alpha = opacity * smooth(clamp(age / 5.0F));
        // A miniature of the released model: same soft texture, torn silhouette,
        // translucent surface and layered white-gold bloom, held vertically overhead.
        stack.pushPose();
        stack.translate(0.0D, 2.25D, 0.35D);
        EnhancementBlastModel.renderCharge(buffer.getBuffer(EnhancementBlastModel.TRANSLUCENT),
                stack, age, seed, formation, alpha * (0.75F + formation * 0.25F), false, 1.0F, detail);
        IVertexBuilder glow = buffer.getBuffer(EnhancementBlastModel.GLOW);
        EnhancementBlastModel.renderCharge(glow, stack, age, seed, formation,
                alpha * (0.12F + formation * 0.12F), true, 1.045F, detail);
        if (detail == 0) {
            EnhancementBlastModel.renderCharge(glow, stack, age, seed, formation,
                    alpha * (0.035F + formation * 0.055F), true, 1.14F, detail);
        }
        stack.popPose();

        IVertexBuilder out = buffer.getBuffer(CaladbolgVisualGeometry.ENERGY);
        Matrix4f pose = stack.last().pose();
        for (int ring = 0; ring < 3; ring++) {
            float phase = (age / 28.0F + ring / 3.0F) % 1.0F;
            CaladbolgVisualGeometry.ring(out, pose, (0.7F + phase * 3.5F) * (0.4F + formation * 0.6F),
                    0.12F + ring * 0.02F, 0.07F, age * 0.15F,
                    1, 0.75F, 0.12F, alpha * (1.0F - phase) * 0.5F, VfxDetail.count(detail, 64, 40, 24));
        }

        renderChargeBursts(out, pose, formation, age, seed, alpha, detail);
        EnhancementChargeStorm.render(out, pose, age, formation, seed, alpha, detail);

        Random random = new Random(seed * 1327L);
        for (int spark = 0; spark < VfxDetail.count(detail, 24, 12, 6); spark++) {
            float phase = (age / 35.0F + random.nextFloat()) % 1.0F;
            double angle = random.nextDouble() * Math.PI * 2 + age * 0.045D;
            double radius = (1.0D - phase) * (1.5D + random.nextDouble() * 3.0D);
            Vector3d point = new Vector3d(Math.cos(angle) * radius,
                    0.4D + phase * (3.0D + formation * 9.0D), Math.sin(angle) * radius + 0.35D);
            sparkle(out, pose, point, 0.06F + phase * 0.09F,
                    alpha * (float) Math.sin(phase * Math.PI));
        }
        stack.popPose();
    }

    private static void renderChargeBursts(IVertexBuilder out, Matrix4f pose, float formation,
                                            float age, int seed, float alpha, int detail) {
        Random random = new Random(seed * 3253L);
        for (int burst = 0; burst < VfxDetail.count(detail, 18, 10, 5); burst++) {
            // Integrating the increasing rate avoids phase jumps as charge rises.
            float phase = (age / 22.0F + age * age / 2640.0F + random.nextFloat()) % 1.0F;
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double height = 3.4D + random.nextDouble() * (1.5D + formation * 8.0D);
            Vector3d direction = new Vector3d(Math.cos(angle), 0.20D + random.nextDouble() * 0.65D,
                    Math.sin(angle)).normalize();
            Vector3d origin = new Vector3d(0, height, 0.35D);
            double travel = phase * (1.0D + formation * 3.0D);
            double length = (0.35D + formation * 1.15D) * Math.min(1.0D, phase * 5.0D);
            Vector3d head = origin.add(direction.scale(0.20D + travel));
            Vector3d tail = origin.add(direction.scale(Math.max(0.08D, 0.20D + travel - length)));
            float envelope = (float) Math.sin(phase * Math.PI);
            float brightness = alpha * (0.10F + formation * 0.35F) * envelope * envelope;
            float width = 0.025F + formation * 0.055F;
            // Short, straight pressure flares; no persistent trails or extra entities.
            CaladbolgVisualGeometry.beam(out, pose, tail, head, width * 2.8F,
                    1.0F, 0.72F, 0.28F, brightness * 0.22F);
            CaladbolgVisualGeometry.beam(out, pose, tail, head, width,
                    1.0F, 0.97F, 0.80F, brightness);
        }
    }

    private static void sparkle(IVertexBuilder out, Matrix4f pose, Vector3d point, float radius, float alpha) {
        CaladbolgVisualGeometry.beam(out, pose, point.add(-radius, 0, 0), point.add(radius, 0, 0),
                radius * 0.16F, 1, 0.92F, 0.44F, alpha);
        CaladbolgVisualGeometry.beam(out, pose, point.add(0, -radius, 0), point.add(0, radius, 0),
                radius * 0.16F, 1, 1, 0.72F, alpha);
    }

    private static float clamp(float value) { return Math.max(0, Math.min(1, value)); }

    private static float smooth(float value) { return value * value * (3 - 2 * value); }
}
