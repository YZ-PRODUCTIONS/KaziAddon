package net.kazi.kazimod.effects;

import net.kazi.kazimod.entities.EnteiBlastEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="kazimod", value={Dist.CLIENT})
public final class EnteiCameraEffects {
    private EnteiCameraEffects() {
    }

    @SubscribeEvent
    public static void shake(EntityViewRenderEvent.CameraSetup event) {
        Minecraft mc = Minecraft.getInstance();
        Entity camera = mc.getCameraEntity();
        if (mc.level == null || camera == null || mc.isPaused()) {
            return;
        }
        float pitch = 0.0f;
        float roll = 0.0f;
        float partial = (float)event.getRenderPartialTicks();
        for (EnteiBlastEntity blast : mc.level.getEntitiesOfClass(EnteiBlastEntity.class, camera.getBoundingBox().inflate(110.0))) {
            float age = blast.getAge(partial);
            float distance = camera.distanceTo((Entity)blast);
            float falloff = Math.max(0.0f, 1.0f - distance / (blast.getMaximumRadius() + 35.0f));
            float front = Math.max(0.0f, 1.0f - Math.abs(distance - blast.getWaveRadius(partial)) / 8.0f);
            float strength = (0.3f + front * 0.9f) * falloff * blast.getOpacity(partial);
            pitch = (float)((double)pitch + Math.sin(age * 1.9f) * (double)strength);
            roll = (float)((double)roll + Math.sin(age * 2.3f) * (double)strength * 0.5);
        }
        event.setPitch(event.getPitch() + Math.max(-1.3f, Math.min(1.3f, pitch)));
        event.setRoll(event.getRoll() + Math.max(-0.6f, Math.min(0.6f, roll)));
    }
}
