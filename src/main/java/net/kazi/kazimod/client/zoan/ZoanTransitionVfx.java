package net.kazi.kazimod.client.zoan;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

final class ZoanTransitionVfx {
    private static final class State extends RenderState {
        private State() { super(null, null, null); }
        static RenderType type() {
            return RenderType.create("zoan_transformation", DefaultVertexFormats.POSITION_COLOR, 7, 16384, false, true,
                    RenderType.State.builder().setTextureState(NO_TEXTURE).setAlphaState(NO_ALPHA)
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL)
                            .setLightmapState(NO_LIGHTMAP).setDiffuseLightingState(NO_DIFFUSE_LIGHTING)
                            .setWriteMaskState(COLOR_WRITE).createCompositeState(false));
        }
    }
    private static final RenderType TYPE = State.type();

    static void render(MatrixStack stack, IRenderTypeBuffer buffers, ZoanTransitionMotion motion,
                       float time, float height, float width) {
        float envelope = (float)Math.sin(Math.PI * time);
        if (envelope <= .001F) return;
        IVertexBuilder out = buffers.getBuffer(TYPE);
        float radius = Math.min(12, Math.max(.65F, width * .58F));
        height = Math.min(24, Math.max(1.6F, height));
        float burst = ZoanTransitionMotion.smooth((time - .22F) / .45F);
        float ringRadius = radius * (.6F + burst * 1.3F);
        float ringAlpha = envelope * (1 - burst * .65F) * .45F;
        for (int i = 0; i < 40; i++) {
            double a = i * Math.PI / 20, b = (i + 1) * Math.PI / 20;
            quad(out, stack, ringRadius, ringRadius + .10F + radius * .04F, a, b, .05F, .05F, motion.color, ringAlpha);
        }
        // Broad rising ribbons conceal the model handover, then open away from the body.
        for (int band = 0; band < 5; band++) {
            for (int i = 0; i < 14; i++) {
                float u = i / 14F, v = (i + 1) / 14F;
                double a = band * Math.PI * .4 + u * 2.7 + time * 5;
                double b = band * Math.PI * .4 + v * 2.7 + time * 5;
                float y0 = height * (u * .86F + time * .13F);
                float y1 = height * (v * .86F + time * .13F);
                float r0 = radius * (.65F + .20F * (float)Math.sin(u * Math.PI) + burst * .5F);
                float alpha = envelope * .48F * (float)Math.sin(u * Math.PI);
                quad(out, stack, r0, r0 + radius * .07F + .05F, a, b, y0, y1, motion.color, alpha);
            }
        }
    }

    private static void quad(IVertexBuilder out, MatrixStack stack, float inner, float outer, double a, double b,
                             float ya, float yb, int rgb, float alpha) {
        vertex(out, stack, (float)Math.cos(a) * inner, ya, (float)Math.sin(a) * inner, rgb, alpha);
        vertex(out, stack, (float)Math.cos(b) * inner, yb, (float)Math.sin(b) * inner, rgb, alpha);
        vertex(out, stack, (float)Math.cos(b) * outer, yb + .10F, (float)Math.sin(b) * outer, rgb, alpha);
        vertex(out, stack, (float)Math.cos(a) * outer, ya + .10F, (float)Math.sin(a) * outer, rgb, alpha);
    }

    private static void vertex(IVertexBuilder out, MatrixStack stack, float x, float y, float z, int rgb, float alpha) {
        out.vertex(stack.last().pose(), x, y, z).color((rgb >> 16 & 255) / 255F, (rgb >> 8 & 255) / 255F,
                (rgb & 255) / 255F, alpha).endVertex();
    }
}
