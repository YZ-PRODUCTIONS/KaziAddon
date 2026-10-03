package net.kazi.kazimod.morphs;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.client.models.morphs.WorldTurtleModel;
import net.minecraft.entity.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.*;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import xyz.pixelatedw.mineminenomi.api.morph.*;
import xyz.pixelatedw.mineminenomi.items.AkumaNoMiItem;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;
import xyz.pixelatedw.mineminenomi.renderers.morphs.ZoanMorphRenderer;
import java.util.Map;

public final class WorldTurtleMorphInfo extends MorphInfo {
    public static final float SCALE = 3;
    public static final ResourceLocation TEXTURE = new ResourceLocation("kazimod", "textures/models/zoan/world_turtle.png");
    // Only the normal player-sized movement core remains; combat uses turtle parts.
    private static final EntitySize SIZE = EntitySize.scalable(.6F, 1.8F);
    private static final Map<Pose, EntitySize> SIZES = ImmutableMap.of(Pose.STANDING, SIZE, Pose.CROUCHING, SIZE, Pose.SWIMMING, SIZE, Pose.FALL_FLYING, SIZE);
    @Override public String getForm() { return "world_turtle"; }
    @Override public String getDisplayName() { return "World Turtle"; }
    @Override public AkumaNoMiItem getDevilFruit() { return ModAbilities.KAME_KAME_NO_MI; }
    @Override public ResourceLocation getTexture() { return TEXTURE; }
    @Override @OnlyIn(Dist.CLIENT) public MorphModel getModel() { return new WorldTurtleModel<>(); }
    @Override @OnlyIn(Dist.CLIENT) public IRenderFactory getRendererFactory(LivingEntity entity) {
        return manager -> ClientRenderer.get(manager, this);
    }
    @OnlyIn(Dist.CLIENT)
    private static final class ClientRenderer {
        private static net.minecraft.client.renderer.entity.EntityRendererManager manager;
        private static ZoanMorphRenderer renderer;
        static ZoanMorphRenderer get(net.minecraft.client.renderer.entity.EntityRendererManager current, WorldTurtleMorphInfo info) {
            // MMNM calls createRenderFor each frame; do not rebuild PlayerRenderer's
            // models and layers each time. Per-entity animation state stays in MOTION.
            if(renderer==null||manager!=current){manager=current;renderer=new ZoanMorphRenderer<>(current,info,false);}
            return renderer;
        }
    }
    @Override @OnlyIn(Dist.CLIENT) public void preRenderCallback(LivingEntity entity, MatrixStack stack, float partialTick) {
        stack.scale(SCALE, SCALE, SCALE);
    }
    @Override public Map<Pose, EntitySize> getSizes() {
        return SIZES;
    }
    @Override public double getEyeHeight() { return 12.8; }
    @Override public double getCameraZoom(LivingEntity entity) { return 36; }
    @Override public double getCameraHeight(LivingEntity entity) { return 2; }
    @Override public float getShadowSize() { return 2.5F; }
    @Override public boolean shouldRenderFirstPersonHand() { return false; }
    @Override public boolean shouldRenderFirstPersonLeg() { return false; }
    @Override public boolean canMount() { return false; }
    @Override public boolean hasEqualDepthTest() { return true; }
}
