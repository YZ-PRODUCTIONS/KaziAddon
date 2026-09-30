package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.entities.UnlimitedLostWorksEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

/** Three dark gears surround the target while twenty black-and-crimson rays burst from its body. */
public class UnlimitedLostWorksRenderer extends EntityRenderer<UnlimitedLostWorksEntity> {
    private static final ResourceLocation MARK_TEXTURE =
            new ResourceLocation("kazimod", "textures/entities/unlimited_lost_works_mark.png");
    public static final float MARK_DURATION_TICKS = 12.0F * 20.0F;
    private static final double MARK_HEIGHT_ABOVE_HEAD = 1.0D;

    public UnlimitedLostWorksRenderer(EntityRendererManager manager) { super(manager); }

    @Override public ResourceLocation getTextureLocation(UnlimitedLostWorksEntity entity) {
        return RealityMarbleGearModel.LOST_TEXTURE;
    }

    @Override public boolean shouldRender(UnlimitedLostWorksEntity entity, ClippingHelper frustum,
                                          double x, double y, double z) {
        return VfxDetail.visible(entity, frustum, x, y, z, 10);
    }

    @Override public void render(UnlimitedLostWorksEntity entity, float yaw, float partial,
                                 MatrixStack stack, IRenderTypeBuffer buffer, int light) {
        float age = entity.getAge(partial);
        float alpha = UnlimitedLostWorksEntity.opacity(age);
        boolean showMark = age < MARK_DURATION_TICKS;
        if (!showMark && alpha <= 0.001F) return;
        int detail = VfxDetail.level(entity, this.entityRenderDispatcher, 10);
        stack.pushPose();
        LivingEntity target = entity.getTarget();
        if (target != null) {
            // Follow the interpolated target, including its opening levitation.
            Vector3d position = target.getPosition(partial).add(0, target.getBbHeight() * 0.5D, 0);
            stack.translate(position.x - MathHelper.lerp(partial, entity.xOld, entity.getX()),
                    position.y - MathHelper.lerp(partial, entity.yOld, entity.getY()),
                    position.z - MathHelper.lerp(partial, entity.zOld, entity.getZ()));
            if (showMark) renderMark(target, stack, buffer);
        }
        // The mark appears immediately, before the gears begin forming at 12 seconds.
        if (alpha <= 0.001F) {
            stack.popPose();
            return;
        }
        float attackAge = age - UnlimitedLostWorksEntity.MARK_DELAY_TICKS;
        float gearAge = age - UnlimitedLostWorksEntity.GEAR_START_TICKS;
        float formation = UnlimitedLostWorksEntity.smooth(gearAge / UnlimitedLostWorksEntity.FORMATION_TICKS);
        float impact = 1.0F - UnlimitedLostWorksEntity.smooth((age - UnlimitedLostWorksEntity.IMPACT_TICKS) / 12.0F);
        if (age < UnlimitedLostWorksEntity.IMPACT_TICKS) impact = 0;
        for (int gear = 0; gear < UnlimitedLostWorksEntity.GEAR_COUNT; gear++) {
            double angle = Math.toRadians(entity.getHeading() + 30.0D + gear * 120.0D);
            double radius = 3.8D + (1 - formation) * 1.4D;
            double height = 0.25D + gear * 0.65D;
            stack.pushPose();
            stack.translate(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
            stack.mulPose(Vector3f.YP.rotationDegrees((float) (-Math.toDegrees(angle) - 90.0D)));
            stack.mulPose(Vector3f.XP.rotationDegrees((float) Math.toDegrees(Math.atan2(height, radius))));
            stack.pushPose();
            stack.mulPose(Vector3f.ZP.rotationDegrees(gear * 41.0F + gearAge * (gear == 1 ? -3.0F : 3.0F)));
            float scale = 1.6F * (0.55F + 0.45F * formation);
            stack.scale(scale, scale, scale);
            RealityMarbleGearModel.renderLostBody(buffer.getBuffer(alpha >= 0.999F
                            ? RealityMarbleGearModel.LOST_METAL : RealityMarbleGearModel.LOST_FORMING),
                    stack.last().pose(), alpha, detail);
            IVertexBuilder glow = buffer.getBuffer(RealityMarbleGearModel.ENERGY);
            // Deep crimson inlays give the black silhouette an edge against dark terrain.
            stack.pushPose();
            stack.mulPose(Vector3f.XP.rotationDegrees(90));
            for (int face = -1; face <= 1; face += 2) {
                CaladbolgVisualGeometry.ring(glow, stack.last().pose(), 0.72F, face * 0.16F, 0.014F,
                        gearAge * 0.08F, 0.65F, 0.015F, 0.008F, alpha * 0.65F, VfxDetail.count(detail, 64, 40, 24));
            }
            stack.popPose();
            stack.popPose();
            stack.popPose();
        }
        // This pose is at the target's body center, outside every gear transform.
        renderRays(buffer, stack.last().pose(), entity.getHeading(), target == null ? 1.8F : target.getBbHeight(),
                age, alpha);
        if (impact > 0) {
            IVertexBuilder glow = buffer.getBuffer(RealityMarbleGearModel.ENERGY);
            for (int ring = 0; ring < 2; ring++) {
                stack.pushPose();
                if (ring == 1) stack.mulPose(Vector3f.XP.rotationDegrees(68));
                CaladbolgVisualGeometry.ring(glow, stack.last().pose(), 0.5F + (1 - impact) * 4.5F,
                        0, 0.05F + impact * 0.08F, attackAge, 1, 0.025F, 0.01F,
                        alpha * impact * 0.7F, VfxDetail.count(detail, 64, 40, 24));
                stack.popPose();
            }
        }
        stack.popPose();
    }

    private void renderMark(LivingEntity target, MatrixStack stack, IRenderTypeBuffer buffer) {
        stack.pushPose();
        // The parent pose is at body center; place the icon center one block above the head.
        stack.translate(0, target.getBbHeight() * 0.5D + MARK_HEIGHT_ABOVE_HEAD, 0);
        stack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        IVertexBuilder vertices = buffer.getBuffer(RenderType.entityTranslucent(MARK_TEXTURE));
        MatrixStack.Entry pose = stack.last();
        markVertex(vertices, pose, -0.5F, -0.5F, 0, 1);
        markVertex(vertices, pose, 0.5F, -0.5F, 1, 1);
        markVertex(vertices, pose, 0.5F, 0.5F, 1, 0);
        markVertex(vertices, pose, -0.5F, 0.5F, 0, 0);
        stack.popPose();
    }

    private static void markVertex(IVertexBuilder vertices, MatrixStack.Entry pose,
                                   float x, float y, float u, float v) {
        vertices.vertex(pose.pose(), x, y, 0).color(255, 255, 255, 255).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                .normal(pose.normal(), 0, 0, 1).endVertex();
    }

    private static void renderRays(IRenderTypeBuffer buffer, Matrix4f pose, float heading, float targetHeight,
                                   float age, float alpha) {
        if (age <= UnlimitedLostWorksEntity.PIERCE_START_TICKS) return;
        IVertexBuilder metal = buffer.getBuffer(RealityMarbleGearModel.LOST_FORMING);
        IVertexBuilder glow = buffer.getBuffer(RealityMarbleGearModel.ENERGY);
        // A fixed spherical spread keeps all twenty directions distinct and stable as the target moves.
        for (int index = 0; index < UnlimitedLostWorksEntity.RAY_COUNT; index++) {
            float rayAlpha = alpha * UnlimitedLostWorksEntity.rayOpacity(age, index);
            if (rayAlpha <= 0.001F) continue;
            float progress = UnlimitedLostWorksEntity.piercingProgress(age, index);
            double vertical = 1.0D - 2.0D * (index + 0.5D) / UnlimitedLostWorksEntity.RAY_COUNT;
            double horizontal = Math.sqrt(1.0D - vertical * vertical);
            double angle = Math.toRadians(heading) + index * Math.PI * (3.0D - Math.sqrt(5.0D));
            Vector3d direction = new Vector3d(Math.cos(angle) * horizontal, vertical, Math.sin(angle) * horizontal);
            Vector3d origin = new Vector3d(0, ((index * 3) % 7 - 3) * Math.min(targetHeight, 3.0F) * 0.08D, 0);
            float reach = (3.2F + ((index * 7) % 11) * 0.24F) * progress;
            ray(metal, glow, pose, origin, direction, reach, rayAlpha, 1.0F - progress);
        }
    }

    private static void ray(IVertexBuilder metal, IVertexBuilder glow, Matrix4f pose,
                            Vector3d origin, Vector3d direction, float reach, float alpha, float impact) {
        float width = 0.085F + impact * 0.025F;
        Vector3d side = new Vector3d(-direction.z, 0, direction.x).normalize().scale(width);
        Vector3d up = direction.cross(side).scale(0.45D);
        Vector3d a = origin.subtract(side);
        Vector3d b = origin.add(up);
        Vector3d c = origin.add(side);
        Vector3d d = origin.subtract(up);
        Vector3d tip = origin.add(direction.scale(reach));
        face(metal, pose, a, b, tip, alpha);
        face(metal, pose, b, c, tip, alpha);
        face(metal, pose, c, d, tip, alpha);
        face(metal, pose, d, a, tip, alpha);
        CaladbolgVisualGeometry.beam(glow, pose, a, tip, 0.017F + impact * 0.013F,
                1, 0.025F, 0.008F, alpha * 0.8F);
        CaladbolgVisualGeometry.beam(glow, pose, c, tip, 0.017F + impact * 0.013F,
                1, 0.025F, 0.008F, alpha * 0.8F);
        CaladbolgVisualGeometry.beam(glow, pose, origin, tip,
                0.065F + impact * 0.06F, 0.65F, 0.01F, 0, alpha * 0.16F);
    }

    private static void face(IVertexBuilder out, Matrix4f pose, Vector3d a, Vector3d b, Vector3d c, float alpha) {
        vertex(out, pose, a, 0, 0, alpha);
        vertex(out, pose, b, 1, 0, alpha);
        vertex(out, pose, c, 0.5F, 1, alpha);
        vertex(out, pose, c, 0.5F, 1, alpha);
    }

    private static void vertex(IVertexBuilder out, Matrix4f pose, Vector3d point, float u, float v, float alpha) {
        out.vertex(pose, (float) point.x, (float) point.y, (float) point.z)
                .color(0.85F, 0.72F, 0.72F, alpha).uv(u, v).endVertex();
    }
}
