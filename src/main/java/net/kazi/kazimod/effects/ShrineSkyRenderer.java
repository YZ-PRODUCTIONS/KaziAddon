package net.kazi.kazimod.effects;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.world.DimensionRenderInfo;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ISkyRenderHandler;

/** Forge sky hook: draws a tinted sky layer without changing ClientWorld's color methods. */
@OnlyIn(Dist.CLIENT)
public final class ShrineSkyRenderer implements ISkyRenderHandler {
    private static ShrineSkyRenderer installed;
    private static ClientWorld currentWorld;
    private final DimensionRenderInfo dimension;
    private final ISkyRenderHandler previous;

    private ShrineSkyRenderer(DimensionRenderInfo dimension) {
        this.dimension = dimension;
        this.previous = dimension.getSkyRenderHandler();
    }

    public static void updateWorld(ClientWorld world) {
        if (currentWorld == world) return;
        if (installed != null && installed.dimension.getSkyRenderHandler() == installed) {
            installed.dimension.setSkyRenderHandler(installed.previous);
        }
        currentWorld = world;
        installed = world == null ? null : new ShrineSkyRenderer(world.effects());
        if (installed != null) installed.dimension.setSkyRenderHandler(installed);
    }

    @Override
    public void render(int ticks, float partial, MatrixStack stack, ClientWorld world, Minecraft minecraft) {
        // Temporarily restore the earlier hook so the vanilla/other mod sky renders without recursion.
        dimension.setSkyRenderHandler(previous);
        try {
            minecraft.levelRenderer.renderSky(stack, partial);
        } finally {
            if (dimension.getSkyRenderHandler() == previous) dimension.setSkyRenderHandler(this);
        }
        float strength = ShrineSkyEffects.strength(partial);
        if (strength <= 0.0F || minecraft.getCameraEntity() == null
                || minecraft.getCameraEntity().isInWaterOrBubble()
                || minecraft.getCameraEntity().isInLava()) return;
        IRenderTypeBuffer.Impl buffer = minecraft.renderBuffers().bufferSource();
        IVertexBuilder vertices = buffer.getBuffer(States.TINT);
        Matrix4f pose = stack.last().pose();
        int alpha = (int) (strength * 220.0F);
        float radius = 100.0F;
        // Six camera-centred faces; terrain is rendered afterwards, so it is never overlaid.
        for (int axis = 0; axis < 3; axis++) {
            for (int side = -1; side <= 1; side += 2) {
                for (int corner = 0; corner < 4; corner++) {
                    float a = corner == 0 || corner == 3 ? -radius : radius;
                    float b = corner < 2 ? -radius : radius;
                    float fixed = side * radius;
                    float x = axis == 0 ? fixed : a;
                    float y = axis == 1 ? fixed : axis == 0 ? a : b;
                    float z = axis == 2 ? fixed : b;
                    vertices.vertex(pose, x, y, z).color(71, 3, 6, alpha).endVertex();
                }
            }
        }
        buffer.endBatch(States.TINT);
    }

    private static final class States extends RenderState {
        static final RenderType TINT = RenderType.create("kazimod_shrine_sky_tint",
                DefaultVertexFormats.POSITION_COLOR, 7, 1024, false, false,
                RenderType.State.builder().setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(NO_DEPTH_TEST).setWriteMaskState(COLOR_WRITE)
                        .setCullState(NO_CULL).createCompositeState(false));

        private States() { super(null, null, null); }
    }
}
