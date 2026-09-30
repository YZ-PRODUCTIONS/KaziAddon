package net.kazi.kazimod.effects;

import net.kazi.kazimod.effects.ShrineSkyProfile;
import net.kazi.kazimod.entities.MalevolentShrineEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="kazimod", value={Dist.CLIENT})
public final class ShrineSkyEffects {
    private static ClientWorld world;
    private static float previous;
    private static float current;

    private ShrineSkyEffects() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ShrineSkyRenderer.updateWorld(mc.level);
        if (world != mc.level || mc.level == null || mc.getCameraEntity() == null) {
            world = mc.level;
            current = 0.0f;
            previous = 0.0f;
        }
        if (mc.level == null || mc.getCameraEntity() == null || mc.isPaused()) {
            return;
        }
        previous = current;
        float target = 0.0f;
        Vector3d camera = mc.gameRenderer.getMainCamera().getPosition();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof MalevolentShrineEntity) || !entity.isAlive()) continue;
            MalevolentShrineEntity shrine = (MalevolentShrineEntity)entity;
            target = Math.max(target, ShrineSkyProfile.strength(camera.distanceTo(shrine.getDomainOrigin()), shrine.getDomainRadius()));
        }
        current = ShrineSkyProfile.approach(current, target);
    }

    public static float strength(float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (world == null || world != mc.level || mc.getCameraEntity() == null) {
            return 0.0f;
        }
        return MathHelper.lerp((float)MathHelper.clamp((float)partial, (float)0.0f, (float)1.0f), (float)previous, (float)current);
    }

    public static Vector3d tint(Vector3d original, float partial, double r, double g, double b) {
        float strength = ShrineSkyEffects.strength(partial);
        if (strength <= 0.0f) {
            return original;
        }
        return new Vector3d(ShrineSkyProfile.tint(original.x, r, strength), ShrineSkyProfile.tint(original.y, g, strength), ShrineSkyProfile.tint(original.z, b, strength));
    }

    @SubscribeEvent
    public static void fog(EntityViewRenderEvent.FogColors event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCameraEntity() == null || mc.getCameraEntity().isInWaterOrBubble() || mc.getCameraEntity().isInLava()) {
            return;
        }
        float amount = ShrineSkyEffects.strength((float)event.getRenderPartialTicks()) * 0.85f;
        if (amount <= 0.0f) {
            return;
        }
        event.setRed((float)ShrineSkyProfile.tint(event.getRed(), 0.19, amount));
        event.setGreen((float)ShrineSkyProfile.tint(event.getGreen(), 0.018, amount));
        event.setBlue((float)ShrineSkyProfile.tint(event.getBlue(), 0.026, amount));
    }
}
