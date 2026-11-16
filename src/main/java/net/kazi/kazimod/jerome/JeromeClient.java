package net.kazi.kazimod.jerome;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class JeromeClient {
    public static void init(IEventBus bus){bus.addListener((FMLClientSetupEvent e)->{
        RenderingRegistry.registerEntityRenderingHandler(JeromeFeatures.BOSS.get(),JeromeRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(JeromeFeatures.SPHERE.get(),m->new EntityRenderer<JeromeProjectile>(m){
            @Override public ResourceLocation getTextureLocation(JeromeProjectile e){return JeromeEntity.TEXTURE;}
            @Override public void render(JeromeProjectile e,float yaw,float partial,MatrixStack s,IRenderTypeBuffer b,int light){
                JeromeVfx.orb(s,b,e.tickCount+partial,e.size());super.render(e,yaw,partial,s,b,light);
            }
        });
        RenderingRegistry.registerEntityRenderingHandler(JeromeFeatures.EFFECT.get(),m->new EntityRenderer<JeromeEffect>(m){
            @Override public ResourceLocation getTextureLocation(JeromeEffect e){return JeromeEntity.TEXTURE;}
            @Override public void render(JeromeEffect e,float yaw,float partial,MatrixStack s,IRenderTypeBuffer b,int light){
                s.pushPose();if(e.kind()==JeromeEffect.SLAM){s.mulPose(Vector3f.YP.rotationDegrees(-e.yRot));s.mulPose(Vector3f.XP.rotationDegrees(e.xRot));}
                JeromeVfx.effect(s,b,e.kind(),e.age(partial),e.life(),e.size());s.popPose();super.render(e,yaw,partial,s,b,light);
            }
        });
    });}
    private JeromeClient(){}
}


