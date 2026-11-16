package net.kazi.kazimod.client.goru;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.kazi.kazimod.network.EaCrimsonOverlayPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.world.ClientWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Global red tint for Ea's beam variants, independent of entity tracking and viewing distance. */
@Mod.EventBusSubscriber(modid="kazimod", value=Dist.CLIENT)
public final class EaCrimsonOverlay {
    private static final Map<UUID,EaCrimsonTint> TINTS=new HashMap<>();
    private static ClientWorld world;
    private EaCrimsonOverlay() { }

    public static void receive(EaCrimsonOverlayPacket packet) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null || mc.player==null)return;
        if(world!=mc.level)clear();
        world=mc.level;
        EaCrimsonTint tint=TINTS.get(packet.session);
        if(packet.action==EaCrimsonOverlayPacket.Action.CANCEL) {
            if(tint!=null)tint.cancel();
        } else if(packet.action==EaCrimsonOverlayPacket.Action.CHARGE) {
            // Restored visuals and duplicate notifications must not restart a cast.
            if(tint==null) {
                tint=new EaCrimsonTint(packet.winds);
                tint.startCharge();
                TINTS.put(packet.session,tint);
            }
        } else {
            // A player who joined after the charge still receives the released tint.
            if(tint==null) {
                tint=new EaCrimsonTint(packet.winds);
                TINTS.put(packet.session,tint);
            }
            tint.trigger();
        }
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null || mc.player==null || mc.level!=world) {
            clear();
        } else if(!mc.isPaused()) {
            Iterator<EaCrimsonTint> tints=TINTS.values().iterator();
            while(tints.hasNext()) {
                EaCrimsonTint tint=tints.next();
                tint.tick();
                if(!tint.isActive())tints.remove();
            }
        }
    }

    // The cinematic HUD cancels Pre ALL at HIGHEST. Draw afterwards even when
    // canceled, without restoring the HUD or changing the player's GUI preference.
    @SubscribeEvent(priority=EventPriority.LOWEST, receiveCanceled=true)
    public static void overlay(RenderGameOverlayEvent.Pre event) {
        if(event.getType()!=RenderGameOverlayEvent.ElementType.ALL)return;
        draw(event.getMatrixStack(),event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight(),event.getPartialTicks());
    }

    @SubscribeEvent public static void hiddenHud(TickEvent.RenderTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getInstance();
        // Minecraft skips the entire HUD event when F1 is enabled without a menu.
        // The render-tick fallback runs in GUI projection, only on those frames.
        if(mc.options.hideGui && mc.screen==null)
            draw(new MatrixStack(),mc.getWindow().getGuiScaledWidth(),
                    mc.getWindow().getGuiScaledHeight(),event.renderTickTime);
    }

    private static void draw(MatrixStack stack,int width,int height,float partial) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null || mc.player==null || mc.level!=world)return;
        float opacity=0;
        for(EaCrimsonTint tint:TINTS.values())opacity=Math.max(opacity,tint.opacity(mc.isPaused()?0:partial));
        int alpha=Math.round(255*opacity);
        if(alpha<=0)return;
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableAlphaTest();
        try {
            AbstractGui.fill(stack,0,0,width,height,alpha<<24|EaCrimsonTint.RGB);
        } finally {
            // Do not write a screen-sized depth surface over later hotbar/icons,
            // or let the default alpha-test cutoff truncate the final fade.
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableAlphaTest();
            RenderSystem.color4f(1,1,1,1);
        }
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggedOutEvent event) {clear();}

    private static void clear() {
        TINTS.clear();
        world=null;
    }
}
