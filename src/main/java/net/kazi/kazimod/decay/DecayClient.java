package net.kazi.kazimod.decay;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.ParticleStatus;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.entity.*;
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
public final class DecayClient {
    public static void init(IEventBus bus){bus.addListener((FMLClientSetupEvent e)->RenderingRegistry.registerEntityRenderingHandler(DecayFeatures.FIELD.get(),Renderer::new));MinecraftForge.EVENT_BUS.addListener(DecayClient::camera);}
    public static void receive(DecayPacket p){Minecraft mc=Minecraft.getInstance();if(mc.level!=null&&p.data!=null){Entity e=mc.level.getEntity(p.id);if(e instanceof LivingEntity)e.getPersistentData().put(DecayFeatures.TAG,p.data);}}
    public static void pose(PlayerModel<?> model,LivingEntity p,float time){int action=DecayFeatures.action(p);if(action<0)return;float age=p.level.getGameTime()-DecayFeatures.state(p).getLong("Start")+(time-(int)time);float blend=MathHelper.clamp(age/6,0,1);float out=MathHelper.clamp((DecayFeatures.state(p).getLong("End")-p.level.getGameTime())/5F,0,1);blend*=out;
        if(action==1||action==5){
            float kneel=blend*(action==5?.65F:.4F);model.body.xRot+=kneel;
            // The player torso pivots at the neck. Offset it so its bottom stays at the hips.
            float c=MathHelper.cos(model.body.xRot),s=MathHelper.sin(model.body.xRot);
            model.body.y=model.rightLeg.y-12*c;model.body.z=model.rightLeg.z-12*s;
            model.head.y=model.body.y;model.head.z=model.body.z;model.head.xRot-=kneel*.35F;
            model.rightArm.y=model.leftArm.y=model.body.y+2*c;
            model.rightArm.z=model.leftArm.z=model.body.z+2*s;
            model.rightArm.xRot=MathHelper.lerp(blend,model.rightArm.xRot,-.65F);
            model.rightArm.zRot=MathHelper.lerp(blend,model.rightArm.zRot,-.15F);
            model.leftArm.xRot=MathHelper.lerp(blend,model.leftArm.xRot,.15F);
            model.rightLeg.xRot+=kneel*.35F;model.leftLeg.xRot-=kneel*.25F;
        }
        else if(action==3){model.rightArm.xRot=MathHelper.lerp(blend,model.rightArm.xRot,-1.45F);model.leftArm.xRot=MathHelper.lerp(blend,model.leftArm.xRot,-1.15F);model.rightArm.yRot=-.5F*blend;model.leftArm.yRot=.5F*blend;}
        else {float thrust=(float)Math.sin(Math.min(1,age/10)*Math.PI*.5);model.rightArm.xRot=MathHelper.lerp(blend,model.rightArm.xRot,-1.7F*thrust);model.rightArm.yRot=-.12F*blend;model.body.yRot-=.15F*blend;if(action==2){model.body.xRot+=.4F*blend;model.leftArm.xRot=.8F*blend;}}
        model.hat.copyFrom(model.head);model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);model.leftPants.copyFrom(model.leftLeg);
    }
    private static void camera(EntityViewRenderEvent.CameraSetup e){Minecraft mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;float shake=0;for(DecayField f:mc.level.getEntitiesOfClass(DecayField.class,mc.player.getBoundingBox().inflate(32),f->f.kind()==5)){float age=f.age(0);if(age>=60&&age<85)shake=Math.max(shake,(1-(age-60)/25)*.7F*(float)Math.max(0,1-mc.player.distanceTo(f)/32));}if(shake>0){float t=mc.player.tickCount;e.setPitch(e.getPitch()+(float)Math.sin(t*2.1)*shake);e.setYaw(e.getYaw()+(float)Math.cos(t*2.7)*shake*.5F);}}
    private static final class Renderer extends EntityRenderer<DecayField>{Renderer(EntityRendererManager m){super(m);}public ResourceLocation getTextureLocation(DecayField e){return new ResourceLocation("minecraft","textures/block/stone.png");}public void render(DecayField f,float yaw,float partial,MatrixStack s,IRenderTypeBuffer b,int light){Minecraft mc=Minecraft.getInstance();if(f.kind()!=1&&f.kind()!=5&&f.kind()!=7&&f.owner()==mc.player&&mc.options.getCameraType().isFirstPerson())return;int quality=mc.options.particles==ParticleStatus.MINIMAL?60:mc.options.particles==ParticleStatus.DECREASED?120:200;DecayMesh.render(f,partial,s,b,quality);}}
}
