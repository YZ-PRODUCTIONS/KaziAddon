package net.kazi.kazimod.events.handlers;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierManager;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.kazi.kazimod.init.KaziAttributes;
import net.kazi.kazimod.init.KaziEffects;

@Mod.EventBusSubscriber(modid = "kazimod")
public class SizeEffectHandler {

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        LivingEntity entity = event.getEntityLiving();

        double targetScale = 1.0;
        try {
            if (KaziAttributes.SIZE.get() != null && entity.getAttributes() != null) {
                targetScale = entity.getAttributeValue(KaziAttributes.SIZE.get());
            }
        } catch (Exception e) {
            return;
        }

        float lastScale = entity.getPersistentData().getFloat("kazi_last_scale");
        if (lastScale == 0.0F) lastScale = 1.0F;

        if (Math.abs(targetScale - (double) lastScale) > 0.01) {
            entity.getPersistentData().putFloat("kazi_last_scale", (float) targetScale);
            entity.refreshDimensions();
        }
    }
}