package net.kazi.kazimod.effects;

import java.util.Collections;
import java.util.List;
import net.kazi.kazimod.entities.EnhancementLightEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Camera recoil follows the travelling pressure front, never the player's aim. */
@Mod.EventBusSubscriber(modid = "kazimod", value = Dist.CLIENT)
public final class EnhancementCameraEffects {
    private static ClientWorld world;
    private static List<EnhancementLightEntity> nearby = Collections.emptyList();

    private EnhancementCameraEffects() { }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft client = Minecraft.getInstance();
        if (world != client.level) {
            nearby = Collections.emptyList();
            world = client.level;
        }
        Entity camera = client.getCameraEntity();
        if (world == null || camera == null || client.isPaused()) return;
        // Query at most five times per second, rather than scanning the world per frame.
        if (world.getGameTime() % 4 == 0) {
            nearby = world.getEntitiesOfClass(EnhancementLightEntity.class,
                    camera.getBoundingBox().inflate(140));
        }
    }

    @SubscribeEvent
    public static void camera(EntityViewRenderEvent.CameraSetup event) {
        Minecraft client = Minecraft.getInstance();
        Entity camera = client.getCameraEntity();
        if (world == null || world != client.level || camera == null || client.isPaused()) return;
        float partial = (float) event.getRenderPartialTicks();
        float pitch = 0, roll = 0;
        for (EnhancementLightEntity blast : nearby) {
            if (!blast.isAlive()) continue;
            float age = blast.getPhaseAge(partial) / EnhancementLightEntity.EFFECT_DURATION_SCALE;
            Vector3d origin = blast.getBlastOrigin();
            double dx = camera.getX() - origin.x;
            double dy = camera.getEyeY() - (origin.y + 2);
            double dz = camera.getZ() - origin.z;
            double yaw = Math.toRadians(blast.getBlastYaw());
            double along = dx * -Math.sin(yaw) + dz * Math.cos(yaw);
            double across = dx * Math.cos(yaw) + dz * Math.sin(yaw);
            float force;
            if (blast.isReleased()) {
                if (blast.getPhaseAge(partial) >= EnhancementLightEntity.BLAST_TICKS) continue;
                float hold = EnhancementLightEntity.BLAST_HOLD_TICKS / EnhancementLightEntity.EFFECT_DURATION_SCALE;
                float fadeTicks = EnhancementLightEntity.BLAST_FADE_TICKS / EnhancementLightEntity.EFFECT_DURATION_SCALE;
                float fade = 1 - smooth(clamp((age - hold) / fadeTicks));
                // Distance from the blast corridor allows far-end observers to feel its arrival.
                double pastEnd = along - Math.max(0, Math.min(80, along));
                float proximity = clamp(1 - (float) Math.sqrt(across * across + dy * dy + pastEnd * pastEnd) / 38);
                // Invert the pressure mesh's cubic travel curve (z = .5 + 83 * easeOut).
                float frontProgress = clamp((float) ((along - 0.5D) / 83.0D));
                float arrival = 20.0F * (1 - (float) Math.cbrt(1 - frontProgress));
                force = proximity * fade * (0.055F + kick(age - arrival)
                        + 0.7F * kick(age - hold - arrival * fadeTicks / 20.0F));
            } else {
                float proximity = clamp(1 - (float) Math.sqrt(dx * dx + dy * dy + dz * dz) / 24);
                float charge = clamp(blast.getChargeProgress());
                force = proximity * charge * charge * charge * 0.10F;
            }
            // Multiple simultaneous casts cannot accumulate an unbounded camera shake.
            float localPitch = (float) Math.sin(age * 1.8F) * force;
            float localRoll = (float) Math.sin(age * 1.35F + 0.7F) * force * 0.35F;
            if (Math.abs(localPitch) > Math.abs(pitch)) pitch = localPitch;
            if (Math.abs(localRoll) > Math.abs(roll)) roll = localRoll;
        }
        event.setPitch(event.getPitch() + Math.max(-1.1F, Math.min(1.1F, pitch)));
        event.setRoll(event.getRoll() + Math.max(-0.4F, Math.min(0.4F, roll)));
    }

    private static float kick(float age) {
        if (age < 0 || age >= 14) return 0;
        return smooth(clamp(age / 1.4F)) * (float) Math.exp(-age / 4.0F) * 1.6F;
    }

    private static float clamp(float value) { return Math.max(0, Math.min(1, value)); }
    private static float smooth(float value) { return value * value * (3 - 2 * value); }
}
