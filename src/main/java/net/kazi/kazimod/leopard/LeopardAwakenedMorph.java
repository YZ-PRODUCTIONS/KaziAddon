package net.kazi.kazimod.leopard;

import java.util.Map;
import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Pose;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.api.morph.MorphModel;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;
import xyz.pixelatedw.mineminenomi.items.AkumaNoMiItem;
import xyz.pixelatedw.mineminenomi.renderers.morphs.ZoanMorphRenderer;

public final class LeopardAwakenedMorph extends MorphInfo {
    private static final EntitySize SIZE = EntitySize.scalable(1.05F, 3.0F);
    private static final Map<Pose, EntitySize> SIZES = ImmutableMap.of(Pose.STANDING, SIZE, Pose.CROUCHING, EntitySize.scalable(1.05F, 2.7F), Pose.SWIMMING, SIZE, Pose.FALL_FLYING, SIZE);
    @Override public String getForm() { return "leopard_awakened"; }
    @Override public String getDisplayName() { return "ability.kazimod.awakened_leopard_form"; }
    @Override public AkumaNoMiItem getDevilFruit() { return ModAbilities.NEKO_NEKO_NO_MI_LEOPARD; }
    @Override public ResourceLocation getTexture() { return new ResourceLocation("kazimod", "textures/zoan/leopard_awakened.png"); }
    @Override @OnlyIn(Dist.CLIENT) public MorphModel getModel() { return new LeopardModel<>(2); }
    @Override @OnlyIn(Dist.CLIENT) public IRenderFactory getRendererFactory(LivingEntity user) { return manager -> Cache.get(manager, this); }
    @OnlyIn(Dist.CLIENT) private static final class Cache {
        private static net.minecraft.client.renderer.entity.EntityRendererManager manager;
        private static ZoanMorphRenderer renderer;
        static ZoanMorphRenderer get(net.minecraft.client.renderer.entity.EntityRendererManager current, LeopardAwakenedMorph morph) {
            if (renderer == null || manager != current) { manager = current; renderer = new ZoanMorphRenderer<>(current, morph, false); }
            return renderer;
        }
    }
    @Override public Map<Pose, EntitySize> getSizes() { return SIZES; }
    @Override public double getEyeHeight() { return 2.75; }
    @Override public double getCameraZoom(LivingEntity user) { return 6; }
    @Override public float getShadowSize() { return 1; }
    @Override public boolean hasEqualDepthTest() { return true; }
    @Override public boolean shouldRenderFirstPersonHand() { return false; }
    @Override public boolean shouldRenderFirstPersonLeg() { return false; }
}
