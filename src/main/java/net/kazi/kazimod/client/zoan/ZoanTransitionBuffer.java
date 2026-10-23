package net.kazi.kazimod.client.zoan;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import net.kazi.kazimod.mixin.client.ZoanTransitionRenderAccessors;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

/** Retains each layer's texture while allowing ordinary cutout models to fade. */
final class ZoanTransitionBuffer implements IRenderTypeBuffer {
    private final IRenderTypeBuffer buffers;
    private final float alpha;
    private final Map<RenderType, IVertexBuilder> cache = new IdentityHashMap<>();
    ZoanTransitionBuffer(IRenderTypeBuffer buffers, float alpha) { this.buffers = buffers; this.alpha = alpha; }

    @Override public IVertexBuilder getBuffer(RenderType type) {
        return cache.computeIfAbsent(type, original -> {
            RenderType faded = original;
            if (original.format().equals(DefaultVertexFormats.NEW_ENTITY) && original instanceof ZoanTransitionRenderAccessors.Composite) {
                RenderType.State state = ((ZoanTransitionRenderAccessors.Composite)original).kazimod$state();
                Optional<ResourceLocation> texture = ((ZoanTransitionRenderAccessors.Texture)
                        ((ZoanTransitionRenderAccessors.State)(Object)state).kazimod$textureState()).kazimod$texture();
                if (texture.isPresent()) faded = RenderType.entityTranslucent(texture.get());
            }
            return new FadedVertex(buffers.getBuffer(faded), alpha);
        });
    }

    private static final class FadedVertex implements IVertexBuilder {
        private final IVertexBuilder out;
        private final float alpha;
        FadedVertex(IVertexBuilder out, float alpha) { this.out = out; this.alpha = alpha; }
        @Override public IVertexBuilder vertex(double x, double y, double z) { out.vertex(x, y, z); return this; }
        @Override public IVertexBuilder color(int r, int g, int b, int a) { out.color(r, g, b, Math.round(a * alpha)); return this; }
        @Override public IVertexBuilder uv(float u, float v) { out.uv(u, v); return this; }
        @Override public IVertexBuilder overlayCoords(int u, int v) { out.overlayCoords(u, v); return this; }
        @Override public IVertexBuilder uv2(int u, int v) { out.uv2(u, v); return this; }
        @Override public IVertexBuilder normal(float x, float y, float z) { out.normal(x, y, z); return this; }
        @Override public void endVertex() { out.endVertex(); }
    }
}
