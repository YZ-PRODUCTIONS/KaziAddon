package net.kazi.kazimod.agito;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MovementInput;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class AgitoClient {
    static void init(IEventBus bus){
        bus.addListener((FMLClientSetupEvent e)->RenderingRegistry.registerEntityRenderingHandler(AgitoFeatures.AGITO.get(),AgitoRenderer::new));
        MinecraftForge.EVENT_BUS.addListener((InputUpdateEvent e)->{
            if(e.getPlayer()!=Minecraft.getInstance().player || !e.getPlayer().hasEffect(AgitoFeatures.EMBRACE.get()))return;
            MovementInput input=e.getMovementInput();
            input.forwardImpulse=0;input.leftImpulse=0;input.jumping=false;input.shiftKeyDown=false;
            input.up=false;input.down=false;input.left=false;input.right=false;
            e.getPlayer().setSprinting(false);e.getPlayer().setDeltaMovement(Vector3d.ZERO);
        });
    }
    private AgitoClient(){}
}
