package net.kazi.kazimod.events.handlers;

import net.minecraft.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.kazi.kazimod.init.KaziAttributes;

@Mod.EventBusSubscriber(modid = "kazimod", value = Dist.CLIENT)
public class SizeRenderHandler {

    private static final float EPSILON = 1.0E-4F;
    private static final float VIS_MIN = 0.1F;

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity living = event.getEntity();

        if (living.getPersistentData().getBoolean("awaken_is_morphed")) return;

        float scale = getScale(living);
        float vis = Math.abs(scale);
        vis = Math.max(vis, VIS_MIN);

        if (Math.abs(scale - 1.0F) < EPSILON) return;

        event.getMatrixStack().pushPose();
        event.getMatrixStack().scale(scale, scale, scale);
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity living = event.getEntity();

        if (living.getPersistentData().getBoolean("awaken_is_morphed")) return;

        float scale = getScale(living);
        if (Math.abs(scale - 1.0F) < EPSILON) return;

        event.getMatrixStack().popPose();
    }

    private static float getScale(LivingEntity living) {
        try {
            if (KaziAttributes.SIZE.get() != null && living.getAttributes() != null
                    && living.getAttributes().hasAttribute(KaziAttributes.SIZE.get())) {
                float scale = (float) living.getAttributeValue(KaziAttributes.SIZE.get());
                if (Float.isNaN(scale) || Float.isInfinite(scale)) return 1.0F;
                if (scale < 0.05F) scale = 0.05F;
                if (scale > 10.0F) scale = 10.0F;
                return scale;
            }
        } catch (Exception e) {
            // entity not ready
        }
        return 1.0F;
    }
}