package net.kazi.kazimod.client;

import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.util.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "kazimod", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class NumaMudTexture {
    private static final ResourceLocation BLOCK = new ResourceLocation("cartaddon", "swamp_block");
    private static final ResourceLocation PHOTO = new ResourceLocation("kazimod", "blocks/numa_mud_photo");

    @SubscribeEvent public static void stitch(TextureStitchEvent.Pre event) {
        if (event.getMap().location().equals(AtlasTexture.LOCATION_BLOCKS)) event.addSprite(PHOTO);
    }

    @SubscribeEvent public static void bake(ModelBakeEvent event) {
        TextureAtlasSprite photo = Minecraft.getInstance().getTextureAtlas(AtlasTexture.LOCATION_BLOCKS).apply(PHOTO);
        event.getModelRegistry().replaceAll((id, model) -> id.getNamespace().equals(BLOCK.getNamespace())
                && id.getPath().equals(BLOCK.getPath()) ? new PhotoModel(model, photo) : model);
    }

    private static final class PhotoModel extends BakedModelWrapper<IBakedModel> {
        private final TextureAtlasSprite photo;
        private final Map<Direction, List<BakedQuad>> faces = new EnumMap<>(Direction.class);
        private final List<BakedQuad> general;

        PhotoModel(IBakedModel original, TextureAtlasSprite photo) {
            super(original); this.photo = photo;
            general = replace(original.getQuads(null, null, new Random(0)));
            for (Direction face : Direction.values()) faces.put(face, replace(original.getQuads(null, face, new Random(0))));
        }

        private List<BakedQuad> replace(List<BakedQuad> source) {
            List<BakedQuad> result = new ArrayList<>(source.size());
            for (BakedQuad quad : source) {
                int[] vertices = quad.getVertices().clone();
                int stride = vertices.length / 4;
                TextureAtlasSprite old = quad.getSprite();
                for (int vertex = 0; vertex < 4; vertex++) {
                    int uv = vertex * stride + 4;
                    float u = Float.intBitsToFloat(vertices[uv]), v = Float.intBitsToFloat(vertices[uv + 1]);
                    vertices[uv] = Float.floatToRawIntBits(photo.getU(16 * (u - old.getU0()) / (old.getU1() - old.getU0())));
                    vertices[uv + 1] = Float.floatToRawIntBits(photo.getV(16 * (v - old.getV0()) / (old.getV1() - old.getV0())));
                }
                result.add(new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), photo, quad.isShade()));
            }
            return Collections.unmodifiableList(result);
        }

        @Override public TextureAtlasSprite getParticleIcon() { return photo; }
        @Override public TextureAtlasSprite getParticleTexture(IModelData data) { return photo; }
        @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random random) { return side == null ? general : faces.get(side); }
        @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random random, IModelData data) { return getQuads(state, side, random); }
    }
}
