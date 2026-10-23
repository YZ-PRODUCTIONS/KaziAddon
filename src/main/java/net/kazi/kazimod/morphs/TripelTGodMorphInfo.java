package net.kazi.kazimod.morphs;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.client.models.morphs.TripelTGodMorphModel;
import net.kazi.kazimod.init.KaziItems;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Pose;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import xyz.pixelatedw.mineminenomi.items.AkumaNoMiItem;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.api.morph.MorphModel;
import xyz.pixelatedw.mineminenomi.renderers.morphs.ZoanMorphRenderer;

import java.util.Map;

public class TripelTGodMorphInfo extends MorphInfo {

    private static final EntitySize STANDING_SIZE = EntitySize.scalable(1.1F, 3.25F);
    private static final EntitySize CROUCHING_SIZE = EntitySize.scalable(1.1F, 3.0F);
    private static final ResourceLocation TEXTURE = new ResourceLocation("kazimod", "textures/models/tripel_t_god.png");

    @Override
    public String getForm() {
        return "tripel_t_god";
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public MorphModel getModel() {
        return new TripelTGodMorphModel<>();
    }

    @Override
    public String getDisplayName() {
        return "Tripel T God";
    }

    @Override
    public ResourceLocation getTexture() {
        return TEXTURE;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public IRenderFactory getRendererFactory(LivingEntity entity) {
        boolean slim = entity instanceof AbstractClientPlayerEntity
                && "slim".equals(((AbstractClientPlayerEntity) entity).getModelName());
        return manager -> new ZoanMorphRenderer<>(manager, this, slim);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void preRenderCallback(LivingEntity entity, MatrixStack matrixStack, float partialTickTime) {
        matrixStack.scale(1.18F, 1.18F, 1.18F);
    }

    @Override
    public AkumaNoMiItem getDevilFruit() {
        return KaziItems.KI_KI_NO_MI_MODEL_TUNG_TUNG_TUNG_SAHUR != null ? KaziItems.KI_KI_NO_MI_MODEL_TUNG_TUNG_TUNG_SAHUR.get() : null;
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
    public double getEyeHeight() {
        return 2.95D;
    }

    @Override
    public float getShadowSize() {
        return 0.95F;
    }

    @Override
    public boolean canMount() {
        return false;
    }

    @Override
    public boolean hasEqualDepthTest() {
        return true;
    }

    @Override
    public Map<Pose, EntitySize> getSizes() {
        return ImmutableMap.of(Pose.STANDING, STANDING_SIZE, Pose.CROUCHING, CROUCHING_SIZE);
    }
}
