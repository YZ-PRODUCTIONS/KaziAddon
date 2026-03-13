package net.kazi.kazimod.renderers.abilities;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.MrMagicalCart.cartaddon.abilities.aowrework.entities.projectiles.WeatherCloudReworkEntity;
import net.minecraftforge.fml.client.registry.IRenderFactory;

@OnlyIn(Dist.CLIENT)
public class WeatherCloudReworkRenderer extends EntityRenderer<WeatherCloudReworkEntity> {

    public WeatherCloudReworkRenderer(EntityRendererManager manager) {
        super(manager);
    }

    @Override
    public ResourceLocation getTextureLocation(WeatherCloudReworkEntity entity) {
        return null;
    }

    public static class Factory implements IRenderFactory<WeatherCloudReworkEntity> {
        @Override
        public EntityRenderer<? super WeatherCloudReworkEntity> createRenderFor(EntityRendererManager manager) {
            return new WeatherCloudReworkRenderer(manager);
        }
    }
}