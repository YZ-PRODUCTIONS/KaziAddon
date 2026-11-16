package net.kazi.kazimod.ope;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Vector3f;

/** Small, fixed meshes: curved membranes and tapered light ribbons, without particle entities. */
public final class OpeMesh extends RenderState {
    public static final RenderType SURFACE = type("kazimod_ope_surface", TRANSLUCENT_TRANSPARENCY);
    public static final RenderType GLOW = type("kazimod_ope_glow", LIGHTNING_TRANSPARENCY);
    private static final float PI = (float)Math.PI, TAU = 2 * PI;
    private OpeMesh() { super(null, null, null); }
    private static RenderType type(String name, TransparencyState transparency) {
        return RenderType.create(name, DefaultVertexFormats.POSITION_COLOR, 7, 65536, false, true,
                RenderType.State.builder().setTransparencyState(transparency).setCullState(NO_CULL)
                        .setWriteMaskState(COLOR_WRITE).setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setTextureState(NO_TEXTURE).setAlphaState(NO_ALPHA)
                        .setShadeModelState(SMOOTH_SHADE).createCompositeState(false));
    }
    public static void curtain(MatrixStack stack, IRenderTypeBuffer buffers, float age, float alpha, boolean far, boolean firstPerson) {
        IVertexBuilder skin = buffers.getBuffer(SURFACE);
        int rows = far ? 10 : 18, columns = far ? 24 : 40;
        float pulse = 1 + .012F * (float)Math.sin(age * .2F);
        for (int row = 0; row < rows; row++) for (int col = 0; col < columns; col++) for (int corner = 0; corner < 4; corner++) {
            float latitude = -PI / 2 + PI * (row + (corner >= 2 ? 1 : 0)) / rows;
            float longitude = TAU * (col + (corner == 1 || corner == 2 ? 1 : 0)) / columns;
            float nx = (float)Math.cos(latitude) * (float)Math.cos(longitude);
            float ny = (float)Math.sin(latitude), nz = (float)Math.cos(latitude) * (float)Math.sin(longitude);
            float rim = (float)Math.pow(1 - Math.abs(nz), 3);
            float shimmer = .5F + .5F * (float)Math.sin(latitude * 17 - age * .11F + longitude * 2);
            vertex(skin, stack, nx * 1.6F * pulse, ny * 1.75F * pulse, nz * .55F,
                    0x67CFEF, alpha * (.018F + rim * .19F + shimmer * .012F) * (firstPerson ? .65F : 1));
        }
        IVertexBuilder glow = buffers.getBuffer(GLOW);
        arc(glow, stack, 1.61F, 1.76F, 0, 0, TAU, .045F, 0x6EDCFF, alpha * .52F, far ? 40 : 80, false);
        arc(glow, stack, 1.61F, 1.76F, .01F, 0, TAU, .012F, 0xD4FCFF, alpha * .75F, far ? 40 : 80, false);
        for (int i = 0; i < 3; i++) {
            float phase = (age * .018F + i / 3F) % 1;
            float size = .32F + phase * 1.28F;
            arc(glow, stack, size, size * 1.09F, -.55F * (float)Math.sqrt(Math.max(0, 1 - size * size / 2.56F)),
                    .25F + i * 1.7F, 2.05F, .015F, 0xC1F7FF, alpha * .18F * (float)Math.sin(phase * PI), 32, true);
        }
        stack.pushPose(); stack.mulPose(Vector3f.YP.rotationDegrees(72));
        arc(glow, stack, .58F, 1.72F, 0, .2F, PI * .72F, .022F, 0xB5F5FF, alpha * .28F, 28, true);
        stack.popPose();
        // A soft curved specular highlight gives the transparent shell a readable volume.
        arc(glow, stack, 1.40F, 1.53F, -.24F, .65F, .62F, .07F, 0xEBFFFF, alpha * .3F, 24, true);
    }
    public static void radio(MatrixStack stack, IRenderTypeBuffer buffers, float age, float alpha, boolean far) {
        IVertexBuilder out = buffers.getBuffer(GLOW);
        for (int i = 0; i < 4; i++) {
            float phase = (age + i * 2.5F) % 10 / 10F;
            float visibility = (float)Math.sin(phase * PI) * alpha;
            stack.pushPose();
            stack.translate((i % 2 == 0 ? -.18 : .18), (i / 2 == 0 ? -.15 : .15), -.3 + phase * .65);
            stack.mulPose(Vector3f.ZP.rotationDegrees(i * 71 + 25));
            stack.mulPose(Vector3f.YP.rotationDegrees(i % 2 == 0 ? 18 : -24));
            float radius = .85F + phase * .6F, start = -.5F + phase * 1.1F;
            int steps = far ? 18 : 32;
            arc(out, stack, radius, radius * .5F, 0, start, PI * 1.36F, .27F, 0x6415D4, visibility * .2F, steps, true);
            arc(out, stack, radius, radius * .5F, .008F, start, PI * 1.36F, .105F, 0xA63CFF, visibility * .75F, steps, true);
            arc(out, stack, radius, radius * .5F, .016F, start, PI * 1.36F, .027F, 0xEDCBFF, visibility, steps, true);
            arc(out, stack, radius * 1.13F, radius * .57F, -.1F, start - .2F, PI, .015F, 0xC173FF, visibility * .45F, steps, true);
            stack.popPose();
        }
    }
    public static void cut(MatrixStack stack, IRenderTypeBuffer buffers, float width, float age, float amount) {
        IVertexBuilder out = buffers.getBuffer(GLOW);
        float offset = Math.min(.35F, width * .28F) * amount;
        for (int half = 0; half < 2; half++) {
            stack.pushPose(); stack.translate(half == 0 ? offset : -offset * .35F, half == 0 ? .28F * amount : 0, 0);
            stack.mulPose(Vector3f.XP.rotationDegrees(90));
            float radius = Math.max(.2F, width * .53F);
            arc(out, stack, radius, radius * .65F, 0, 0, TAU, .025F, 0xBAF7FF, amount * .8F, 40, false);
            arc(out, stack, radius, radius * .65F, 0, age * .06F, PI, .07F, 0x56D6F5, amount * .22F, 24, true);
            stack.popPose();
        }
    }
    private static void arc(IVertexBuilder out, MatrixStack stack, float rx, float ry, float z, float start, float length,
                            float width, int color, float alpha, int steps, boolean taper) {
        for (int i = 0; i < steps; i++) for (int corner = 0; corner < 4; corner++) {
            float t = (i + (corner == 1 || corner == 2 ? 1 : 0)) / (float)steps;
            float a = start + length * t;
            float shape = taper ? (float)Math.sin(t * PI) : 1;
            float edge = corner >= 2 ? -1 : 1;
            float thickness = width * shape * edge;
            vertex(out, stack, (rx + thickness) * (float)Math.cos(a), (ry + thickness) * (float)Math.sin(a), z,
                    color, alpha * (taper ? Math.min(1, shape * 3) : 1));
        }
    }
    private static void vertex(IVertexBuilder out, MatrixStack stack, float x, float y, float z, int color, float alpha) {
        out.vertex(stack.last().pose(), x, y, z).color((color >> 16 & 255) / 255F,
                (color >> 8 & 255) / 255F, (color & 255) / 255F, Math.max(0, Math.min(1, alpha))).endVertex();
    }
}
