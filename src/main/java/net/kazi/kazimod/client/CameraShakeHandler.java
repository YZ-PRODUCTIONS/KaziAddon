package net.kazi.kazimod.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "kazimod", value = Dist.CLIENT)
public class CameraShakeHandler {
    private static int remainingTicks = 0;
    private static int totalTicks = 0;
    private static float intensity = 0.0F;

    public static void shake(int durationTicks, float newIntensity) {
        remainingTicks = Math.max(remainingTicks, durationTicks);
        totalTicks = Math.max(totalTicks, durationTicks);
        intensity = Math.max(intensity, newIntensity);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || remainingTicks <= 0) {
            return;
        }

        remainingTicks--;
        if (remainingTicks <= 0) {
            totalTicks = 0;
            intensity = 0.0F;
        }
    }

    @SubscribeEvent
    public static void onCameraSetup(EntityViewRenderEvent.CameraSetup event) {
        Minecraft mc = Minecraft.getInstance();
        if (remainingTicks <= 0 || totalTicks <= 0 || mc.player == null) {
            return;
        }

        float fade = Math.min(1.0F, remainingTicks / (float) totalTicks);
        float time = mc.player.tickCount + (float) event.getRenderPartialTicks();
        float strength = intensity * fade;

        event.setYaw(event.getYaw() + (float) Math.sin(time * 2.7F) * strength);
        event.setPitch(event.getPitch() + (float) Math.cos(time * 3.1F) * strength * 0.6F);
        event.setRoll(event.getRoll() + (float) Math.sin(time * 4.1F) * strength * 0.35F);
    }
}
