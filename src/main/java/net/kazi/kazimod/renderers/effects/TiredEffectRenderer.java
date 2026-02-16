package net.kazi.kazimod.renderers.effects;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "kazimod", value = Dist.CLIENT)
public class TiredEffectRenderer {

    // Track when each player last experienced the blackout
    private static final Map<UUID, Long> lastBlackoutTime = new HashMap<>();
    private static final long BLACKOUT_INTERVAL = 7000; // 7 seconds in milliseconds
    private static final long BLACKOUT_DURATION = 250; // 0.25 seconds in milliseconds

    @SubscribeEvent
    public static void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        // Only render during the ALL phase to cover the entire screen
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        PlayerEntity player = mc.player;

        if (player == null) {
            return;
        }

        // Check if player has the tired effect
        EffectInstance tiredEffect = player.getEffect(KaziEffects.TIRED.get());
        if (tiredEffect == null) {
            // Remove player from tracking when effect is gone
            lastBlackoutTime.remove(player.getUUID());
            return;
        }

        long currentTime = System.currentTimeMillis();
        UUID playerUUID = player.getUUID();

        // Initialize tracking for this player if not present
        if (!lastBlackoutTime.containsKey(playerUUID)) {
            lastBlackoutTime.put(playerUUID, currentTime);
        }

        long timeSinceLastBlackout = currentTime - lastBlackoutTime.get(playerUUID);

        // Check if we should trigger a blackout
        if (timeSinceLastBlackout >= BLACKOUT_INTERVAL) {
            // Reset the timer
            lastBlackoutTime.put(playerUUID, currentTime);
        }

        // Check if we're currently in a blackout period
        long timeInCycle = currentTime - lastBlackoutTime.get(playerUUID);
        if (timeInCycle <= BLACKOUT_DURATION) {
            renderBlackScreen(event.getMatrixStack(), mc);
        }
    }

    private static void renderBlackScreen(MatrixStack matrixStack, Minecraft mc) {
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        // Set up rendering
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();

        // Draw a black rectangle covering the entire screen
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();

        matrixStack.pushPose();
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        buffer.vertex(0, height, -90).color(0, 0, 0, 255).endVertex();
        buffer.vertex(width, height, -90).color(0, 0, 0, 255).endVertex();
        buffer.vertex(width, 0, -90).color(0, 0, 0, 255).endVertex();
        buffer.vertex(0, 0, -90).color(0, 0, 0, 255).endVertex();
        tessellator.end();
        matrixStack.popPose();

        // Restore rendering state
        RenderSystem.enableTexture();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}