package net.kazi.kazimod.longbob;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import xyz.pixelatedw.mineminenomi.models.entities.projectiles.HydraModel;

public final class LongbobClient {
    public static final ResourceLocation LIQUID_TEXTURE=new ResourceLocation("minecraft","textures/block/white_concrete.png");
    public static void init(IEventBus bus){bus.addListener((FMLClientSetupEvent e)->{
        RenderingRegistry.registerEntityRenderingHandler(LongbobFeatures.BOSS.get(),LongbobRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(LongbobFeatures.PROJECTILE.get(),PoisonRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(LongbobFeatures.CLOUD.get(),manager->new EntityRenderer<LongbobCloud>(manager){
            @Override public ResourceLocation getTextureLocation(LongbobCloud cloud){return LIQUID_TEXTURE;}
        });
    });}
    public static final class PoisonRenderer extends EntityRenderer<LongbobProjectile> {
        private final HydraModel<Entity> head=new HydraModel<>();
        public PoisonRenderer(EntityRendererManager manager){super(manager);shadowRadius=0;head.neck.visible=false;head.spine.visible=false;}
        @Override public ResourceLocation getTextureLocation(LongbobProjectile projectile){return LIQUID_TEXTURE;}
        @Override public void render(LongbobProjectile projectile,float yaw,float partial,MatrixStack s,IRenderTypeBuffer b,int light){
            if(projectile.kind()==LongbobProjectile.HYDRA){
                Vector3d d=projectile.getDeltaMovement();float direction=(float)(MathHelper.atan2(d.z,d.x)*180/Math.PI)-90;
                float pitch=(float)(-MathHelper.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))*180/Math.PI);
                s.pushPose();s.mulPose(Vector3f.YP.rotationDegrees(180-direction));s.mulPose(Vector3f.XP.rotationDegrees(-pitch));s.scale(3.6F,-3.6F,3.6F);
                head.jaw2.xRot=.3F+MathHelper.sin((projectile.tickCount+partial)*.2F)*.09F;
                head.renderToBuffer(s,b.getBuffer(RenderType.entityTranslucent(LIQUID_TEXTURE)),15728880,OverlayTexture.NO_OVERLAY,.69F,1,.43F,.8F);s.popPose();
            }else LongbobVfx.orb(s,b,projectile.tickCount+partial,.65F);
            super.render(projectile,yaw,partial,s,b,light);
        }
    }
    private LongbobClient(){}
}
