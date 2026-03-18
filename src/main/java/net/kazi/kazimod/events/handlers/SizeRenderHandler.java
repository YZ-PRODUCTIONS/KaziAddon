package net.kazi.kazimod.events.handlers;

import net.kazi.kazimod.init.KaziAttributes;
import net.minecraft.entity.LivingEntity;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import xyz.pixelatedw.mineminenomi.api.events.ability.RenderMorphEvent;
import xyz.pixelatedw.mineminenomi.api.helpers.MorphHelper;

public class SizeRenderHandler {

    private static final float EPSILON = 1.0e-4f;
    private static final float VIS_MIN = 0.05f;
    private static final float VIS_MAX = 10.0f;

    @SubscribeEvent
    public void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (hasMorphActive(entity)) return;
        if (entity.getPersistentData().getBoolean("awaken_is_morphed")) return;

        float scale = getKaziScale(entity);
        if (Math.abs(scale - 1.0f) < EPSILON) return;

        event.getMatrixStack().pushPose();
        event.getMatrixStack().scale(scale, scale, scale);
    }

    @SubscribeEvent
    public void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (hasMorphActive(entity)) return;
        if (entity.getPersistentData().getBoolean("awaken_is_morphed")) return;

        float scale = getKaziScale(entity);
        if (Math.abs(scale - 1.0f) < EPSILON) return;

        event.getMatrixStack().popPose();
    }

    @SubscribeEvent
    public void onRenderMorphPre(RenderMorphEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity entity = (LivingEntity) event.getEntity();

        float scale = getKaziScale(entity);
        if (Math.abs(scale - 1.0f) < EPSILON) return;

        entity.getPersistentData().putBoolean("kazi_morph_scale_pushed", true);
        event.getMatrixStack().pushPose();
        event.getMatrixStack().scale(scale, scale, scale);
    }

    @SubscribeEvent
    public void onRenderMorphPost(RenderMorphEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity entity = (LivingEntity) event.getEntity();

        if (!entity.getPersistentData().getBoolean("kazi_morph_scale_pushed")) return;
        entity.getPersistentData().remove("kazi_morph_scale_pushed");
        event.getMatrixStack().popPose();
    }

    private static boolean hasMorphActive(LivingEntity entity) {
        try {
            return MorphHelper.getZoanInfo(entity) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static float getKaziScale(LivingEntity entity) {
        try {
            if (KaziAttributes.SIZE.get() == null) return 1.0f;
            if (entity.getAttributes() == null) return 1.0f;
            if (!entity.getAttributes().hasAttribute(KaziAttributes.SIZE.get())) return 1.0f;

            float scale = (float) entity.getAttributeValue(KaziAttributes.SIZE.get());

            if (Float.isNaN(scale) || Float.isInfinite(scale)) return 1.0f;

            return Math.min(Math.max(scale, VIS_MIN), VIS_MAX);
        } catch (Exception e) {
            return 1.0f;
        }
    }
}