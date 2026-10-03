package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import java.util.Arrays;
import net.kazi.kazimod.animations.supa.EaChargeAnimation;
import net.kazi.kazimod.entities.EaVfxEntity;
import net.kazi.kazimod.entities.WindsRaptureShape;
import net.kazi.kazimod.models.abilities.WindsRaptureMesh;
import net.kazi.kazimod.models.abilities.EaBeamCoreMesh;
import net.kazi.kazimod.models.abilities.EaChargeMesh;
import net.kazi.kazimod.models.abilities.EaResonanceMesh;
import net.kazi.kazimod.models.abilities.EaVfxMesh;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

/** An upright galaxy charge, followed by the fixed directional rupture. */
public final class EaVfxRenderer extends EntityRenderer<EaVfxEntity> {
    private static final ResourceLocation GALAXY_TEXTURE = new ResourceLocation("kazimod", "textures/entities/ea_galaxy.png");
    private static final RenderType BODY = States.body();
    private static final RenderType WEAPON = States.weapon();
    private static final RenderType ENERGY = States.energy();
    private static final RenderType GALAXIES = States.galaxies();
    private static final float WEAPON_SCALE = 0.65F;
    private static final float WEAPON_GRIP_Z = -0.60F;
    private static final double WINDS_HOLD_SIDE = 0.85D;
    private static final double WINDS_HOLD_DROP = 0.40D;
    private static final double WINDS_HOLD_FORWARD = 0.90D;
    // Keep the original 96-block mesh proportions, then stretch only its beam axis.
    private static final float BEAM_LENGTH_SCALE = EaVfxEntity.MAX_RANGE / 96.0F;
    private final VertexBatch body = new VertexBatch(4096);
    private final VertexBatch energy = new VertexBatch(16384);
    private final TexturedBatch galaxies = new TexturedBatch(4096);

    public EaVfxRenderer(EntityRendererManager manager) {
        super(manager);
        this.shadowRadius = 0;
    }

    @Override
    public ResourceLocation getTextureLocation(EaVfxEntity entity) { return GALAXY_TEXTURE; }

    @Override
    public boolean shouldRender(EaVfxEntity entity, ClippingHelper frustum, double x, double y, double z) {
        if (entity.isWinds()) return frustum.isVisible(new AxisAlignedBB(entity.getOrigin(),
                entity.getOrigin().add(entity.getBeamDirection().scale(entity.isReleased() ? entity.getReleaseLength() : 5))).inflate(4));
        if (entity.isPillar()) return frustum.isVisible(new AxisAlignedBB(entity.position().add(-35, -2, -35),
                entity.position().add(35, net.kazi.kazimod.models.abilities.UtaPillarMesh.PORTAL_HEIGHT + 4, 35)));
        AxisAlignedBB bounds;
        if (entity.isReleased()) {
            Vector3d origin = entity.getOrigin();
            Vector3d end = origin.add(entity.getBeamDirection().scale(entity.getReleaseLength()));
            bounds = new AxisAlignedBB(origin, end).inflate(22);
            if (entity.getPhaseAge(0) < EaChargeMesh.GALAXY_LINGER_TICKS) {
                bounds = bounds.minmax(chargeBounds(entity.getChargeCenter(1)));
            }
        } else {
            bounds = chargeBounds(entity.getChargeCenter(1));
        }
        // Use distance from the whole attack, including its far end, rather than the caster.
        Vector3d camera = this.entityRenderDispatcher.camera.getPosition();
        double dx = Math.max(bounds.minX - camera.x, Math.max(0, camera.x - bounds.maxX));
        double dy = Math.max(bounds.minY - camera.y, Math.max(0, camera.y - bounds.maxY));
        double dz = Math.max(bounds.minZ - camera.z, Math.max(0, camera.z - bounds.maxZ));
        return dx * dx + dy * dy + dz * dz <= 192 * 192 && frustum.isVisible(bounds);
    }

    private static AxisAlignedBB chargeBounds(Vector3d center) {
        return new AxisAlignedBB(center.x - EaChargeMesh.MAX_RADIUS, center.y + EaChargeMesh.MIN_Y,
                center.z - EaChargeMesh.MAX_RADIUS, center.x + EaChargeMesh.MAX_RADIUS,
                center.y + EaChargeMesh.MAX_HEIGHT, center.z + EaChargeMesh.MAX_RADIUS);
    }

    @Override
    public void render(EaVfxEntity entity, float yaw, float partial, MatrixStack stack,
                       IRenderTypeBuffer buffers, int light) {
        if (entity.isWinds()) {
            renderWinds(entity, partial, stack, buffers);
            return;
        }
        if (entity.isPillar()) {
            body.clear(); energy.clear();
            net.kazi.kazimod.models.abilities.UtaPillarMesh.draw(body, energy, entity.getPhaseAge(partial));
            Vector3d camera = entityRenderDispatcher.camera.getPosition().subtract(entity.position());
            body.draw(stack.last().pose(), buffers.getBuffer(WEAPON), camera, false);
            energy.draw(stack.last().pose(), buffers.getBuffer(ENERGY), camera, false);
            return;
        }
        float age = entity.getPhaseAge(partial);
        float opacity = entity.isCancelled() ? 1 - smooth(age / 10.0F) : 1;
        if (opacity <= 0.001F) return;
        if (!entity.isReleased()) {
            renderCharge(entity, partial, opacity, stack, buffers);
            return;
        }
        if (age < EaChargeMesh.GALAXY_LINGER_TICKS) {
            renderLingeringGalaxies(entity, partial, age, stack, buffers);
        }
        Vector3d origin = entity.getOrigin();
        Vector3d direction = entity.getBeamDirection();
        stack.pushPose();
        translateTo(stack, entity, origin, partial);
        orient(stack, direction);
        Vector3d localCamera = localCamera(origin, direction);
        float releaseLength = entity.getReleaseLength(partial);
        int detail = VfxDetail.levelForDistance(EaVfxVisibility.distanceToBeam(localCamera.x, localCamera.y,
                localCamera.z, releaseLength, entity.getReleaseRadius(partial)));
        body.clear(); energy.clear();
        float collapseAge = age - EaVfxEntity.RELEASE_TICKS;
        opacity *= 1 - smooth(collapseAge / EaVfxEntity.FADE_TICKS);
        float modelLength = releaseLength / BEAM_LENGTH_SCALE;
        EaVfxMesh.release(body, energy, detail, age, modelLength,
                entity.getReleaseRadius(partial), collapseAge * 30.0F / EaVfxEntity.FADE_TICKS,
                opacity, entity.getId());
        // Do not retain two IVertexBuilders at once: shared render buffers can flush
        // when the render type changes. Reusable primitive batches generate the mesh once.
        float coreOpacity = EaBeamCoreMesh.viewOpacity((float) localCamera.x, (float) localCamera.y,
                (float) localCamera.z, releaseLength, entity.getReleaseRadius(partial));
        EaBeamCoreMesh.draw(body, energy, detail, age, modelLength,
                entity.getReleaseRadius(partial), opacity * coreOpacity, entity.getId());
        body.stretchLength(BEAM_LENGTH_SCALE);
        energy.stretchLength(BEAM_LENGTH_SCALE);
        body.draw(stack.last().pose(), buffers.getBuffer(BODY), localCamera, true);
        energy.draw(stack.last().pose(), buffers.getBuffer(ENERGY), localCamera, true);
        drawWeapon(stack.last().pose(), buffers, localCamera, VfxDetail.level(entity, this.entityRenderDispatcher, 4),
                age + EaVfxEntity.CHARGE_TICKS, 1, opacity, false);
        stack.popPose();
    }

    private void renderLingeringGalaxies(EaVfxEntity entity, float partial, float age,
                                         MatrixStack stack, IRenderTypeBuffer buffers) {
        Vector3d center = entity.getChargeCenter(partial);
        int detail = VfxDetail.level(entity, this.entityRenderDispatcher, EaChargeMesh.MAX_RADIUS);
        energy.clear(); galaxies.clear();
        EaChargeMesh.lingeringGalaxies(energy, galaxies, detail, age, EaVfxEntity.CHARGE_TICKS, 1, entity.getId());
        stack.pushPose();
        translateTo(stack, entity, center, partial);
        Vector3d camera = this.entityRenderDispatcher.camera.getPosition().subtract(center);
        galaxies.draw(stack.last().pose(), buffers.getBuffer(GALAXIES), camera);
        energy.draw(stack.last().pose(), buffers.getBuffer(ENERGY), camera, false);
        stack.popPose();
    }

    private void renderWinds(EaVfxEntity entity, float partial, MatrixStack stack, IRenderTypeBuffer buffers) {
        float age = entity.getPhaseAge(partial);
        float opacity = entity.isCancelled() ? 1 - smooth(age / 10)
                : entity.isReleased() ? WindsRaptureShape.opacity(age) : 1;
        if (opacity <= .001F) return;
        Vector3d origin = entity.getOrigin();
        Vector3d direction = entity.getBeamDirection();
        LivingEntity caster = entity.getCaster();
        if (!entity.isReleased()) {
            boolean following = !entity.isCancelled() && caster != null;
            if (following) direction = caster.getViewVector(partial).normalize();
            // Use view yaw rather than a cross product with world-up: the hold
            // remains beside the camera even when looking straight up or down.
            double viewYaw = Math.toRadians(entity.getChargeViewYaw(partial));
            Vector3d right = new Vector3d(-Math.cos(viewYaw), 0, -Math.sin(viewYaw));
            Vector3d up = right.cross(direction).normalize();
            double hand = entity.isChargeLeftHanded() ? -1 : 1;
            Vector3d eye = entity.getChargeCenter(partial).add(0, entity.getChargeEyeHeight(), 0);
            Vector3d sideAnchor = eye.add(right.scale(WINDS_HOLD_SIDE * hand)).subtract(up.scale(WINDS_HOLD_DROP));
            origin = sideAnchor.add(direction.scale(WINDS_HOLD_FORWARD));
            // Shorten only the forward reach near walls, never the side offset.
            // Cancellation uses the frozen center/direction, so it fades at the
            // side instead of snapping back across the caster's crosshair.
            BlockRayTraceResult wall = entity.level.clip(new RayTraceContext(sideAnchor, origin,
                    RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, caster));
            if (wall.getType() != RayTraceResult.Type.MISS) origin = wall.getLocation().subtract(direction.scale(.05));
        }
        body.clear(); energy.clear();
        if (entity.isReleased()) WindsRaptureMesh.release(energy, age, entity.getReleaseLength(partial), opacity);
        else WindsRaptureMesh.charge(body, energy, entity.getChargeProgress(partial) * WindsRaptureShape.CHARGE_TICKS,
                entity.getChargeProgress(partial), opacity);
        stack.pushPose();
        translateTo(stack, entity, origin, partial);
        orient(stack, direction);
        Vector3d camera = localCamera(origin, direction);
        body.draw(stack.last().pose(), buffers.getBuffer(WEAPON), camera, false);
        energy.draw(stack.last().pose(), buffers.getBuffer(ENERGY), camera, false);
        stack.popPose();
    }

    private void renderCharge(EaVfxEntity entity, float partial, float opacity,
                              MatrixStack stack, IRenderTypeBuffer buffers) {
        Vector3d center = entity.getChargeCenter(partial);
        float progress = entity.getChargeProgress(partial);
        // Use synchronized charge progress for the same spin phase on every client.
        float chargeAge = progress * EaVfxEntity.CHARGE_TICKS;
        int detail = VfxDetail.level(entity, this.entityRenderDispatcher,
                Math.max(EaChargeMesh.MAX_RADIUS, EaChargeMesh.MAX_HEIGHT - EaChargeMesh.MIN_Y));
        body.clear(); energy.clear(); galaxies.clear();
        EaChargeMesh.charge(body, energy, galaxies, detail, chargeAge, progress, opacity, entity.getId());
        stack.pushPose();
        translateTo(stack, entity, center, partial);
        Vector3d camera = this.entityRenderDispatcher.camera.getPosition().subtract(center);
        // Charge scenery stays world-up even when the player aims straight up or down.
        body.draw(stack.last().pose(), buffers.getBuffer(BODY), camera, false);
        galaxies.draw(stack.last().pose(), buffers.getBuffer(GALAXIES), camera);
        energy.draw(stack.last().pose(), buffers.getBuffer(ENERGY), camera, false);
        stack.popPose();

        LivingEntity caster = entity.getCaster();
        Vector3d direction = !entity.isCancelled() && caster != null
                ? caster.getViewVector(partial) : entity.getBeamDirection();
        Vector3d origin = entity.getOrigin();
        if (!entity.isCancelled() && caster != null) {
            Vector3d eye = center.add(0, caster.getEyeHeight(), 0);
            origin = eye.add(direction.scale(1.4));
            BlockRayTraceResult wall = entity.level.clip(new RayTraceContext(eye, origin,
                    RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, caster));
            if (wall.getType() != RayTraceResult.Type.MISS) origin = wall.getLocation().subtract(direction.scale(0.05));
        }
        double bodyYaw = Math.toRadians(entity.getChargeBodyYaw(partial));
        Vector3d flatForward = new Vector3d(-Math.sin(bodyYaw), 0, Math.cos(bodyYaw));
        Vector3d right = new Vector3d(-Math.cos(bodyYaw), 0, -Math.sin(bodyYaw));
        float raised = EaChargeAnimation.raiseBlend(progress) * (1 - EaChargeAnimation.aimBlend(progress));
        Vector3d raisedDirection = flatForward.scale(0.28).add(0, 0.96, 0);
        Vector3d raisedOrigin = center.add(right.scale(0.15)).add(flatForward.scale(0.58)).add(0, 2.65, 0);
        Vector3d weaponOrigin = origin.add(raisedOrigin.subtract(origin).scale(raised));
        Vector3d weaponDirection = direction.add(raisedDirection.subtract(direction).scale(raised)).normalize();
        stack.pushPose();
        translateTo(stack, entity, weaponOrigin, partial);
        orient(stack, weaponDirection);
        Vector3d weaponCamera = localCamera(weaponOrigin, weaponDirection);
        drawWeapon(stack.last().pose(), buffers, weaponCamera, VfxDetail.level(entity, this.entityRenderDispatcher, 4),
                chargeAge, progress, opacity, true);
        stack.popPose();
    }

    private void drawWeapon(Matrix4f pose, IRenderTypeBuffer buffers, Vector3d camera,
                            int detail, float age, float power, float opacity, boolean charging) {
        body.clear(); energy.clear();
        EaVfxMesh.weapon(body, energy, detail, age, power, opacity);
        if (charging) EaResonanceMesh.blade(energy, detail, age, power, opacity);
        // Shrink the sword and its attached energy around the grip, keeping it in the hand.
        body.scale(WEAPON_SCALE, WEAPON_GRIP_Z);
        energy.scale(WEAPON_SCALE, WEAPON_GRIP_Z);
        // Solid metal supplies depth so circuits on the back cannot shine through the blade.
        body.draw(pose, buffers.getBuffer(WEAPON), camera, false);
        energy.draw(pose, buffers.getBuffer(ENERGY), camera, false);
    }

    private Vector3d localCamera(Vector3d origin, Vector3d direction) {
        Vector3d forward = direction.lengthSqr() > 0.00001 ? direction.normalize() : new Vector3d(0, 0, 1);
        double directionYaw = Math.atan2(forward.x, forward.z);
        Vector3d right = new Vector3d(Math.cos(directionYaw), 0, -Math.sin(directionYaw));
        Vector3d up = forward.cross(right);
        Vector3d cameraOffset = this.entityRenderDispatcher.camera.getPosition().subtract(origin);
        return new Vector3d(cameraOffset.dot(right), cameraOffset.dot(up), cameraOffset.dot(forward));
    }

    private static void translateTo(MatrixStack stack, EaVfxEntity entity, Vector3d origin, float partial) {
        stack.translate(origin.x - MathHelper.lerp(partial, entity.xOld, entity.getX()),
                origin.y - MathHelper.lerp(partial, entity.yOld, entity.getY()),
                origin.z - MathHelper.lerp(partial, entity.zOld, entity.getZ()));
    }

    private static void orient(MatrixStack stack, Vector3d direction) {
        if (direction.lengthSqr() < 0.00001) return;
        direction = direction.normalize();
        stack.mulPose(Vector3f.YP.rotationDegrees((float) Math.toDegrees(Math.atan2(direction.x, direction.z))));
        stack.mulPose(Vector3f.XP.rotationDegrees((float) -Math.toDegrees(Math.asin(MathHelper.clamp(direction.y, -1, 1)))));
    }

    private static float smooth(float value) {
        value = MathHelper.clamp(value, 0, 1);
        return value * value * (3 - 2 * value);
    }

    private static final class VertexBatch implements EaVfxMesh.Sink, EaChargeMesh.Sink {
        private float[] points;
        private int[] colors;
        private int count;

        private VertexBatch(int capacity) {
            points = new float[capacity * 4];
            colors = new int[capacity];
        }

        private void clear() { count = 0; }

        private void stretchLength(float factor) {
            for (int i = 0; i < count; i++) points[i * 4 + 2] *= factor;
        }

        private void scale(float factor, float pivotZ) {
            for (int i = 0; i < count; i++) {
                int offset = i * 4;
                points[offset] *= factor;
                points[offset + 1] *= factor;
                points[offset + 2] = pivotZ + (points[offset + 2] - pivotZ) * factor;
            }
        }

        @Override
        public void vertex(float x, float y, float z, int color, float alpha) {
            if (count == colors.length) {
                colors = Arrays.copyOf(colors, colors.length * 2);
                points = Arrays.copyOf(points, points.length * 2);
            }
            int offset = count * 4;
            points[offset] = x; points[offset + 1] = y; points[offset + 2] = z;
            points[offset + 3] = alpha;
            colors[count++] = color;
        }

        private void draw(Matrix4f pose, IVertexBuilder builder, Vector3d camera, boolean beam) {
            float axisFade = beam && camera.z > -4 && camera.z < EaVfxEntity.MAX_RANGE + 4
                    ? 1 - smooth(((float) Math.sqrt(camera.x * camera.x + camera.y * camera.y) - 1.25F) / 1.75F) : 0;
            for (int base = 0; base < count; base += 4) {
                int first = base * 4;
                // Entirely sub-byte-alpha quads cannot contribute to the color buffer.
                if (points[first + 3] <= 0.0039F && points[first + 7] <= 0.0039F
                        && points[first + 11] <= 0.0039F && points[first + 15] <= 0.0039F) continue;
                for (int i = base; i < base + 4; i++) {
                    int offset = i * 4, color = colors[i];
                    double dx = points[offset] - camera.x, dy = points[offset + 1] - camera.y;
                    double dz = points[offset + 2] - camera.z;
                    // Let players inside the rupture see through nearby sheets; the distant
                    // silhouette is unaffected. This also keeps the caster's view readable.
                    float proximity = EaVfxVisibility.proximity(dx, dy, dz);
                    // Looking directly down the attack stacks every distant helix. Reduce
                    // only its narrow central cone, leaving the surrounding vortex strong.
                    float centerOpacity = EaVfxVisibility.centerOpacity(dx, dy, dz, axisFade);
                    builder.vertex(pose, points[offset], points[offset + 1], points[offset + 2])
                            .color(color >> 16 & 255, color >> 8 & 255, color & 255,
                                    MathHelper.clamp((int) (points[offset + 3] * proximity * centerOpacity * 255), 0, 255)).endVertex();
                }
            }
        }
    }

    private static final class TexturedBatch implements EaChargeMesh.TexturedSink {
        private float[] points;
        private int[] colors;
        private int count;

        private TexturedBatch(int capacity) {
            points = new float[capacity * 6];
            colors = new int[capacity];
        }

        private void clear() { count = 0; }

        @Override
        public void vertex(float x, float y, float z, float u, float v, int color, float alpha) {
            if (count == colors.length) {
                colors = Arrays.copyOf(colors, colors.length * 2);
                points = Arrays.copyOf(points, points.length * 2);
            }
            int offset = count * 6;
            points[offset] = x; points[offset + 1] = y; points[offset + 2] = z;
            points[offset + 3] = u; points[offset + 4] = v; points[offset + 5] = alpha;
            colors[count++] = color;
        }

        private void draw(Matrix4f pose, IVertexBuilder builder, Vector3d camera) {
            for (int base = 0; base < count; base += 4) {
                int first = base * 6;
                if (points[first + 5] <= 0.0039F && points[first + 11] <= 0.0039F
                        && points[first + 17] <= 0.0039F && points[first + 23] <= 0.0039F) continue;
                for (int i = base; i < base + 4; i++) {
                    int offset = i * 6, color = colors[i];
                    double dx = points[offset] - camera.x, dy = points[offset + 1] - camera.y;
                    double dz = points[offset + 2] - camera.z;
                    float proximity = EaVfxVisibility.proximity(dx, dy, dz);
                    builder.vertex(pose, points[offset], points[offset + 1], points[offset + 2])
                            .color(color >> 16 & 255, color >> 8 & 255, color & 255,
                                    MathHelper.clamp((int) (points[offset + 5] * proximity * 255), 0, 255))
                            .uv(points[offset + 3], points[offset + 4]).endVertex();
                }
            }
        }
    }

    private static final class States extends RenderState {
        private States() { super(null, null, null); }

        private static RenderType body() {
            return RenderType.create("kazimod_ea_dark_space", DefaultVertexFormats.POSITION_COLOR,
                    7, 262144, false, true, RenderType.State.builder()
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE)
                            .setCullState(NO_CULL).createCompositeState(false));
        }

        private static RenderType energy() {
            return RenderType.create("kazimod_ea_rupture", DefaultVertexFormats.POSITION_COLOR,
                    7, 524288, false, false, RenderType.State.builder()
                            .setTransparencyState(LIGHTNING_TRANSPARENCY)
                            .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE)
                            .setCullState(NO_CULL).createCompositeState(false));
        }

        private static RenderType weapon() {
            return RenderType.create("kazimod_ea_sword", DefaultVertexFormats.POSITION_COLOR,
                    7, 262144, false, true, RenderType.State.builder()
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_DEPTH_WRITE)
                            .setCullState(NO_CULL).createCompositeState(false));
        }

        private static RenderType galaxies() {
            return RenderType.create("kazimod_ea_galaxies", DefaultVertexFormats.POSITION_COLOR_TEX,
                    7, 262144, false, false, RenderType.State.builder()
                            .setTextureState(new TextureState(GALAXY_TEXTURE, true, false))
                            .setTransparencyState(LIGHTNING_TRANSPARENCY)
                            .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE)
                            .setCullState(NO_CULL).createCompositeState(false));
        }
    }
}
