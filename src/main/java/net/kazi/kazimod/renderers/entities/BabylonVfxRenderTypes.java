package net.kazi.kazimod.renderers.entities;

import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

/** Separate amber matter and emitted light, with world depth testing on both. */
final class BabylonVfxRenderTypes extends RenderState {
    private static final RenderType SURFACE = create("kazimod_babylon_surface", TRANSLUCENT_TRANSPARENCY);
    private static final RenderType GLOW = create("kazimod_babylon_glow", LIGHTNING_TRANSPARENCY);

    private BabylonVfxRenderTypes() { super(null, null, null); }

    private static RenderType create(String name, TransparencyState blend) {
        return RenderType.create(name, DefaultVertexFormats.POSITION_COLOR, 7, 262144,
                false, blend == TRANSLUCENT_TRANSPARENCY,
                RenderType.State.builder().setTransparencyState(blend).setCullState(NO_CULL)
                        .setWriteMaskState(COLOR_WRITE).setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setShadeModelState(SMOOTH_SHADE).createCompositeState(false));
    }

    static RenderType surface() { return SURFACE; }
    static RenderType glow() { return GLOW; }
}
