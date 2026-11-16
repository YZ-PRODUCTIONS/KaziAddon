package net.kazi.kazimod.hermano;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class HermanoClient {
    public static void init(IEventBus bus){bus.addListener((FMLClientSetupEvent e)->{
        RenderingRegistry.registerEntityRenderingHandler(HermanoFeatures.BOSS.get(),HermanoRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(HermanoFeatures.IMPACT.get(),m->new EntityRenderer<HermanoProjectile>(m){
            @Override public ResourceLocation getTextureLocation(HermanoProjectile e){return HermanoEntity.TEXTURE;}
            @Override public void render(HermanoProjectile e,float yaw,float partial,MatrixStack s,IRenderTypeBuffer b,int light){
                HermanoVfx.orb(s,b,e.tickCount+partial,e.size());super.render(e,yaw,partial,s,b,light);
            }
        });
        RenderingRegistry.registerEntityRenderingHandler(HermanoFeatures.EFFECT.get(),m->new EntityRenderer<HermanoEffect>(m){
            @Override public ResourceLocation getTextureLocation(HermanoEffect e){return HermanoEntity.TEXTURE;}
            @Override public void render(HermanoEffect e,float yaw,float partial,MatrixStack s,IRenderTypeBuffer b,int light){
                s.pushPose();if(e.kind()==HermanoEffect.GLARE){s.mulPose(Vector3f.YP.rotationDegrees(-e.yRot));s.mulPose(Vector3f.XP.rotationDegrees(e.xRot));}
                HermanoVfx.effect(s,b,e.kind(),e.age(partial),e.life(),e.size());s.popPose();super.render(e,yaw,partial,s,b,light);
            }
        });
    });}
    private HermanoClient(){}
}
