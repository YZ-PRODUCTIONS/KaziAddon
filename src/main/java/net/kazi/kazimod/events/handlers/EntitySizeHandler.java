package net.kazi.kazimod.events.handlers;

import net.kazi.kazimod.KaziMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Pose;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.kazi.kazimod.init.KaziAttributes;
import net.kazi.kazimod.init.KaziEffects;

@Mod.EventBusSubscriber(modid = "kazimod")
public class EntitySizeHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntitySize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity entity = (LivingEntity) event.getEntity();

        double scale = 1.0;
        try {
            if (KaziAttributes.SIZE.get() != null && entity.getAttributes() != null) {
                scale = entity.getAttributeValue(KaziAttributes.SIZE.get());
            }
        } catch (Throwable e) {
            return;
        }

        // Clamp scale to safe range
        if (Double.isNaN(scale) || Double.isInfinite(scale)) scale = 1.0;
        if (scale < 0.05) scale = 0.05;
        if (scale > 10.0) scale = 10.0;

        if (Math.abs(scale - 1.0) < 0.001) return; // no change needed

        float s = (float) scale;
        EntitySize base = event.getNewSize();
        event.setNewSize(base.scale(s), true);
        float eye = Math.max(0.05F, base.height * s * 0.85F);
        event.setNewEyeHeight(eye);
    }
}