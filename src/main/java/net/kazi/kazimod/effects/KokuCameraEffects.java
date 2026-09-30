package net.kazi.kazimod.effects;

import net.kazi.kazimod.entities.projectiles.HollowNukeProjectile;
import net.kazi.kazimod.entities.KokuVfxEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="kazimod", value={Dist.CLIENT})
public final class KokuCameraEffects {
    private KokuCameraEffects() {
    }

    @SubscribeEvent
    public static void shake(EntityViewRenderEvent.CameraSetup event) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity camera = minecraft.getCameraEntity();
        if (minecraft.level == null || camera == null || minecraft.isPaused()) {
            return;
        }
        float partial = (float)event.getRenderPartialTicks();
        float pitch = 0.0f;
        float yaw = 0.0f;
        float roll = 0.0f;
        for (Entity effect : minecraft.level.getEntities(camera, camera.getBoundingBox().inflate(110.0),
                entity -> entity instanceof HollowNukeProjectile
                        || entity instanceof KokuVfxEntity && ((KokuVfxEntity) entity).getMode() == KokuVfxEntity.NUKE_IMPACT)) {
            float age;
            float waveRadius;
            float opacity;
            if (effect instanceof HollowNukeProjectile) {
                HollowNukeProjectile nuke = (HollowNukeProjectile) effect;
                age = nuke.getVfxAge(partial);
                waveRadius = nuke.getWaveRadius(partial);
                opacity = nuke.getVfxOpacity(partial);
            } else {
                KokuVfxEntity blast = (KokuVfxEntity) effect;
                age = blast.getNukeAge(partial);
                waveRadius = blast.getWaveRadius(partial);
                opacity = blast.getVfxOpacity(partial);
            }
            float distance = camera.distanceTo(effect);
            float falloff = Math.max(0.0f, 1.0f - distance / 110.0f);
            float phase = age < 40.0f ? age / 40.0f * 0.15f : 0.55f;
            float front = Math.abs(distance - waveRadius);
            float strength = Math.min(1.2f, phase + Math.max(0.0f, 1.0f - front / 12.0f) * 0.7f) * falloff * opacity;
            pitch += (float)Math.sin(age * 1.7) * strength;
            yaw += (float)Math.sin(age * 2.13 + 1.0) * strength * 0.6f;
            roll += (float)Math.sin(age * 1.13 + 2.0) * strength * 0.4f;
        }
        event.setPitch(event.getPitch() + Math.max(-1.5f, Math.min(1.5f, pitch)));
        event.setYaw(event.getYaw() + Math.max(-1.0f, Math.min(1.0f, yaw)));
        event.setRoll(event.getRoll() + Math.max(-0.75f, Math.min(0.75f, roll)));
    }
}
