package net.kazi.kazimod.gengar;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.*;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
public final class GengarClient {
    public static void init(IEventBus bus){bus.addListener((FMLClientSetupEvent event)->{
        RenderingRegistry.registerEntityRenderingHandler(GengarFeatures.PROJECTILE.get(),ProjectileRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(GengarFeatures.EFFECT.get(),EffectRenderer::new);GengarModel.rig();
    });MinecraftForge.EVENT_BUS.addListener(GengarClient::world);}
    public static void receive(GengarPacket packet){Minecraft mc=Minecraft.getInstance();if(mc.level==null||packet.data==null)return;Entity entity=mc.level.getEntity(packet.entity);if(entity instanceof LivingEntity)entity.getPersistentData().put(GengarFeatures.TAG,packet.data);}
    private static void world(RenderWorldLastEvent event){
        Minecraft mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;MatrixStack s=event.getMatrixStack();IRenderTypeBuffer.Impl b=mc.renderBuffers().bufferSource();
        float p=event.getPartialTicks();Vector3d camera=mc.gameRenderer.getMainCamera().getPosition();int rendered=0;
        for(PlayerEntity user:mc.level.players()){
            if(!GengarFeatures.active(user)||user.isInvisible()||!user.isAlive()||user.distanceToSqr(mc.player)>96*96||rendered++>=12)continue;
            Vector3d at=new Vector3d(MathHelper.lerp(p,user.xOld,user.getX()),MathHelper.lerp(p,user.yOld,user.getY()),MathHelper.lerp(p,user.zOld,user.getZ()));
            float age=user.tickCount+p;int action=GengarFeatures.action(user);s.pushPose();s.translate(at.x-camera.x,at.y-camera.y,at.z-camera.z);GengarMesh.pool(s,b,age,action==4?1.4F:2,.7F);s.popPose();
            if(action==2){float charge=MathHelper.clamp((user.level.getGameTime()-GengarFeatures.state(user).getLong("Start")+p)/40,0,1);Vector3d ball=at.add(0,user.getEyeHeight(),0).add(user.getLookAngle().scale(2.4));
                s.pushPose();s.translate(ball.x-camera.x,ball.y-camera.y,ball.z-camera.z);GengarMesh.orb(s,b,age,.2F+charge*.7F);s.popPose();}
        }
        b.endBatch(GengarMesh.SHADOW);b.endBatch(GengarMesh.GLOW);
    }
    private static final class ProjectileRenderer extends EntityRenderer<GengarProjectile>{
        ProjectileRenderer(EntityRendererManager m){super(m);}
        @Override public void render(GengarProjectile e,float yaw,float partial,MatrixStack s,IRenderTypeBuffer b,int light){
            s.pushPose();try{float age=e.tickCount+partial;
                if(e.kind()==0)GengarMesh.orb(s,b,age,e.size());else{s.mulPose(Vector3f.YP.rotationDegrees(180-MathHelper.rotLerp(partial,e.yRotO,e.yRot)));GengarMesh.ghost(s,b,age,.45F);}
            }finally{s.popPose();}
        }
        @Override public ResourceLocation getTextureLocation(GengarProjectile e){return GengarMorphInfo.TEXTURE;}
    }
    private static final class EffectRenderer extends EntityRenderer<GengarEffect>{
        EffectRenderer(EntityRendererManager m){super(m);}
        @Override public void render(GengarEffect e,float yaw,float partial,MatrixStack s,IRenderTypeBuffer b,int light){
            if(e.distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition())>96*96)return;
            s.pushPose();try{GengarMesh.effect(s,b,e.kind(),e.age(partial),e.life(),e.size());}finally{s.popPose();}
        }
        @Override public ResourceLocation getTextureLocation(GengarEffect e){return GengarMorphInfo.TEXTURE;}
    }
    private GengarClient(){}
}
