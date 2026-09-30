package net.kazi.kazimod.effects;

import net.kazi.kazimod.entities.MalevolentShrineEntity;
import net.kazi.kazimod.entities.projectiles.FugaProjectile;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="kazimod", value={Dist.CLIENT})
public final class KamaCameraEffects {
    private KamaCameraEffects() {
    }

    @SubscribeEvent
    public static void shake(EntityViewRenderEvent.CameraSetup event) {
        Minecraft mc = Minecraft.getInstance();
        Entity camera = mc.getCameraEntity();
        if (mc.level == null || camera == null || mc.isPaused()) {
            return;
        }
        float partial = (float)event.getRenderPartialTicks();
        float strength = 0.0f;
        for (MalevolentShrineEntity shrine : mc.level.getEntitiesOfClass(MalevolentShrineEntity.class, camera.getBoundingBox().inflate(160.0))) {
            float radius = shrine.getDomainRadius();
            if (!(radius > 0.0f) || !(camera.distanceToSqr((Entity)shrine) <= (double)(radius * radius))) continue;
            strength = Math.max(strength, 0.45f * (1.0f - camera.distanceTo((Entity)shrine) / (radius + 20.0f)));
        }
        for (FugaProjectile fuga : mc.level.getEntitiesOfClass(FugaProjectile.class, camera.getBoundingBox().inflate(80.0))) {
            if (!fuga.isFinished()) continue;
            strength = Math.max(strength, Math.max(0.0f, 1.0f - camera.distanceTo((Entity)fuga) / 80.0f) * Math.max(0.0f, 1.0f - fuga.getEffectAge(partial) / 100.0f) * 0.8f);
        }
        float time = (float)camera.tickCount + partial;
        event.setPitch(event.getPitch() + (float)Math.sin((double)time * 1.7) * strength);
        event.setYaw(event.getYaw() + (float)Math.sin((double)time * 2.3) * strength * 0.6f);
        event.setRoll(event.getRoll() + (float)Math.sin((double)time * 1.3) * strength * 0.3f);
    }
}
