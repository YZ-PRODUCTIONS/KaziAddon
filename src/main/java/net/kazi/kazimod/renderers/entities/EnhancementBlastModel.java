package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import java.util.LinkedHashMap;
import java.util.Map;
import net.kazi.kazimod.entities.EnhancementBlastShape;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;

/** White-gold blast core with broad, uneven eruption faces and torn, pointed edges. */
final class EnhancementBlastModel {
    private static final Map<Integer, CachedMesh> CACHE = new LinkedHashMap<Integer, CachedMesh>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, CachedMesh> entry) { return size() > 16; }
    };
    private static final double ORIGIN_Y = EnhancementBlastShape.ORIGIN_Y;
    private static final double ORIGIN_Z = EnhancementBlastShape.ORIGIN_Z;
    private static final double VISUAL_LENGTH = EnhancementBlastShape.VISUAL_LENGTH;
    private static final float SURFACE_OPACITY = EnhancementBlastShape.SURFACE_OPACITY;
    static final ResourceLocation TEXTURE =
            new ResourceLocation("kazimod", "textures/entities/enhancement_blast_energy_soft.png");

    private static final class States extends RenderState {
        private States() { super(null, null, null); }
        static final TransparencyState TRANSLUCENT = TRANSLUCENT_TRANSPARENCY;
        static final TransparencyState GLOW = LIGHTNING_TRANSPARENCY;
        static final TextureState ENERGY_TEXTURE = new TextureState(TEXTURE, true, false);
        static final WriteMaskState WRITE = COLOR_WRITE;
        static final CullState CULL = NO_CULL;
        static final DepthTestState DEPTH = LEQUAL_DEPTH_TEST;
        static final ShadeModelState SHADE = SMOOTH_SHADE;
    }

    // Blend against the world without writing an opaque silhouette into its depth.
    static final RenderType TRANSLUCENT = createType("kazimod_enhancement_blast_translucent", false);
    static final RenderType GLOW = createType("kazimod_enhancement_blast_glow", true);
    private static RenderType createType(String name, boolean glow) {
        return RenderType.create(name, DefaultVertexFormats.POSITION_COLOR_TEX, 7, 262144, false, !glow,
                RenderType.State.builder()
                        .setTextureState(States.ENERGY_TEXTURE)
                        .setTransparencyState(glow ? States.GLOW : States.TRANSLUCENT)
                        .setWriteMaskState(States.WRITE).setCullState(States.CULL)
                        .setDepthTestState(States.DEPTH).setShadeModelState(States.SHADE)
                        .createCompositeState(false));
    }

    private EnhancementBlastModel() { }

    static void render(IVertexBuilder out, Matrix4f pose, float age, int seed, float alpha, int detail) {
        render(out, pose, age, seed, alpha * SURFACE_OPACITY, 1.0F, detail);
    }

    static void renderGlow(IVertexBuilder out, Matrix4f pose, float age, int seed, float alpha, float scale, int detail) {
        render(out, pose, age, seed, alpha, scale, detail);
    }

    /** Hold the same textured eruption upright, compressed around the caster's raised hand. */
    static void renderCharge(IVertexBuilder out, MatrixStack stack, float age, int seed,
                              float formation, float alpha, boolean glow, float haloScale, int detail) {
        stack.pushPose();
        transformCharge(stack, age, formation);
        // Skip the release kick: charge growth is controlled by formation, while
        // travelling pressure, jagged faces and texture flow remain animated.
        render(out, stack.last().pose(), 10.0F + age * 0.55F, seed,
                glow ? alpha : alpha * SURFACE_OPACITY, haloScale, detail);
        stack.popPose();
    }

    private static void transformCharge(MatrixStack stack, float age, float formation) {
        float pulse = 1.0F + formation * 0.035F * (float) Math.sin(age * 0.75F);
        // Local forward becomes up. Remove the blast's origin before scaling so the
        // base stays at the raised hand instead of orbiting or drifting as it grows.
        stack.mulPose(Vector3f.XP.rotationDegrees(-90.0F));
        stack.scale((0.05F + formation * 0.10F) * pulse,
                (0.03F + formation * 0.06F) * pulse,
                (float) ((2.0D + formation * 11.0D) / VISUAL_LENGTH));
        stack.translate(0.0D, -ORIGIN_Y, -ORIGIN_Z);
    }

    private static void render(IVertexBuilder out, Matrix4f pose, float age, int seed,
                               float alpha, float scale, int detail) {
        if (alpha <= 0.001F) return;
        CachedMesh cached = CACHE.get(seed);
        if (cached == null || cached.age != age) {
            cached = new CachedMesh(age, EnhancementBlastShape.generate(age, seed));
            CACHE.put(seed, cached);
        }
        // All graphics settings render the same damaging silhouette. Only cosmetic
        // layers (outer glow passes, rays and dust) vary with LOD.
        EnhancementBlastShape.Mesh mesh = cached.mesh;
        for (int i = 0; i < mesh.vertexCount(); i++) {
            float heat = mesh.heat(i);
            out.vertex(pose, mesh.x(i, scale), mesh.y(i, scale), mesh.z(i, scale))
                    .color(1.0F, 0.85F + heat * 0.15F, 0.6F + heat * 0.4F, alpha)
                    .uv(mesh.u(i), mesh.v(i)).endVertex();
        }
    }

    static float releaseFlash(float age) { return EnhancementBlastShape.releaseFlash(age); }

    static float fadeStartAge() { return EnhancementBlastShape.fadeStartAge(); }

    static float blastFlash(float effectAge) { return EnhancementBlastShape.blastFlash(effectAge); }

    static float fadeProgress(float effectAge) { return EnhancementBlastShape.fadeProgress(effectAge); }

    static double launch(float age, double ticks) { return EnhancementBlastShape.launch(age, ticks); }

    private static double upwardLift(double y) { return EnhancementBlastShape.upwardLift(y); }

    private static final class CachedMesh {
        final float age;
        final EnhancementBlastShape.Mesh mesh;

        CachedMesh(float age, EnhancementBlastShape.Mesh mesh) {
            this.age = age;
            this.mesh = mesh;
        }
    }
}