package net.kazi.kazimod.effects;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.AbstractGui;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class FlashbangEffect {

    private static int flashTicks = 0;
    private static final int FLASH_DURATION = 60; // 3 seconds fade

    public static void triggerFlash() {
        flashTicks = FLASH_DURATION;
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        if (flashTicks <= 0) return;

        Minecraft mc = Minecraft.getInstance();
        MatrixStack stack = event.getMatrixStack();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Alpha goes from 1.0 at start to 0.0 at end
        float alpha = (float) flashTicks / (float) FLASH_DURATION;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, alpha);

        AbstractGui.fill(stack, 0, 0, screenWidth, screenHeight, 0xFFFFFFFF);

        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        flashTicks--;
    }
}