package net.kazi.kazimod.morphs;

import com.google.common.collect.ImmutableMap;
import net.kazi.kazimod.client.models.morphs.MountainGoatMorphModel;
import net.kazi.kazimod.init.KaziItems;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Pose;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.api.morph.MorphModel;
import xyz.pixelatedw.mineminenomi.items.AkumaNoMiItem;
import xyz.pixelatedw.mineminenomi.renderers.morphs.ZoanMorphRenderer;

import java.util.Map;

public final class MountainGoatMorphInfo extends MorphInfo {
    public static final ResourceLocation TEXTURE = new ResourceLocation("kazimod", "textures/models/mountain_goat.png");
    private static final EntitySize SIZE = EntitySize.scalable(0.9F, 1.6F);
    private static final Map<Pose, EntitySize> SIZES = ImmutableMap.of(
            Pose.STANDING, SIZE,
            Pose.CROUCHING, SIZE,
            Pose.SWIMMING, SIZE,
            Pose.FALL_FLYING, SIZE);

    @Override
    public String getForm() {
        return "mountain_goat";
    }

    @Override
    public String getDisplayName() {
        return "Mountain Goat";
    }

    @Override
    public AkumaNoMiItem getDevilFruit() {
        return KaziItems.YAGI_YAGI_NO_MI_MODEL_MOUNTAIN_GOAT != null
                ? KaziItems.YAGI_YAGI_NO_MI_MODEL_MOUNTAIN_GOAT.get() : null;
    }

    @Override
    public ResourceLocation getTexture() {
        return TEXTURE;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public MorphModel getModel() {
        return new MountainGoatMorphModel<>();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public IRenderFactory getRendererFactory(LivingEntity entity) {
        return manager -> ClientRenderer.get(manager, this);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientRenderer {
        private static net.minecraft.client.renderer.entity.EntityRendererManager manager;
        private static ZoanMorphRenderer renderer;

        static ZoanMorphRenderer get(net.minecraft.client.renderer.entity.EntityRendererManager current,
                                     MountainGoatMorphInfo info) {
            // MMNM requests the factory every frame. Reuse the model and layer objects.
            if (renderer == null || manager != current) {
                manager = current;
                renderer = new ZoanMorphRenderer(current, info, false) {
                    {
                        // The quadruped has no humanoid hand/head attachment points.
                        removeLayer(net.minecraft.client.renderer.entity.layers.HeldItemLayer.class);
                        removeLayer(net.minecraft.client.renderer.entity.layers.HeadLayer.class);
                        removeLayer(net.minecraft.client.renderer.entity.layers.ArrowLayer.class);
                        removeLayer(net.minecraft.client.renderer.entity.layers.BeeStingerLayer.class);
                        addLayer(new net.kazi.kazimod.events.handlers.AstralFlightLayer(this));
                    }
                };
            }
            return renderer;
        }
    }

    @Override
    public Map<Pose, EntitySize> getSizes() {
        return SIZES;
    }

    @Override
    public double getEyeHeight() {
        return 1.2D;
    }

    @Override
    public float getShadowSize() {
        return 0.65F;
    }

    @Override
    public boolean shouldRenderFirstPersonHand() {
        return false;
    }

    @Override
    public boolean shouldRenderFirstPersonLeg() {
        return false;
    }

    @Override
    public boolean canMount() {
        return false;
    }

    @Override
    public boolean hasEqualDepthTest() {
        return true;
    }
}
