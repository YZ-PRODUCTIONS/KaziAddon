package net.kazi.kazimod.worldturtle;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class WorldTurtleClient {
    public static boolean isLocalOwner(net.minecraft.entity.LivingEntity owner){return owner==Minecraft.getInstance().player;}
    private static final List<WorldTurtleEffectEntity> NEARBY=new ArrayList<>();
    private static final List<SupernovaEntity> STARS=new ArrayList<>();
    private static final Map<WorldTurtleEffectEntity,Float> VISIBILITY=new IdentityHashMap<>();
    private static net.minecraft.world.World cachedWorld;
    public static void init(IEventBus bus){
        bus.addListener((FMLClientSetupEvent e)->{
            RenderingRegistry.registerEntityRenderingHandler(WorldTurtleEffects.EFFECT.get(),WorldTurtleEffectRenderer::new);
            RenderingRegistry.registerEntityRenderingHandler(WorldTurtleEffects.SUPERNOVA.get(),SupernovaRenderer::new);
            RenderingRegistry.registerEntityRenderingHandler(WorldTurtleEffects.PART.get(),manager->
                    new net.minecraft.client.renderer.entity.EntityRenderer<WorldTurtlePartEntity>(manager){
                        public net.minecraft.util.ResourceLocation getTextureLocation(WorldTurtlePartEntity entity){
                            return net.kazi.kazimod.morphs.WorldTurtleMorphInfo.TEXTURE;
                        }
                    });
            e.enqueueWork((Runnable)()->{net.kazi.kazimod.client.models.morphs.WorldTurtleModel.preload();});
        });
        MinecraftForge.EVENT_BUS.addListener(WorldTurtleClient::tick);
        MinecraftForge.EVENT_BUS.addListener(WorldTurtleClient::overlay);
        MinecraftForge.EVENT_BUS.addListener(WorldTurtleClient::camera);
    }
    private static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;Minecraft mc=Minecraft.getInstance();
        if(mc.level!=cachedWorld||mc.player==null){NEARBY.clear();STARS.clear();VISIBILITY.clear();cachedWorld=mc.level;}
        if(mc.level==null||mc.player==null||mc.isPaused())return;
        Vector3d camera=mc.gameRenderer.getMainCamera().getPosition();
        if(mc.player.tickCount%2==0){
            AxisAlignedBB area=new AxisAlignedBB(camera,camera).inflate(WorldTurtleCameraEffects.RANGE);
            NEARBY.clear();NEARBY.addAll(mc.level.getEntitiesOfClass(WorldTurtleEffectEntity.class,area));
            STARS.clear();STARS.addAll(mc.level.getEntitiesOfClass(SupernovaEntity.class,area));
            VISIBILITY.keySet().retainAll(NEARBY);
        }
        // Occlusion is sampled once per tick, never for every rendered frame.
        for(WorldTurtleEffectEntity effect:NEARBY)if(effect.isAlive()&&effect.kind()==1&&effect.age()<70){
            Vector3d light=effect.position().add(0,4,0);
            boolean blocked=mc.level.clip(new RayTraceContext(camera,light,RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,mc.player)).getType()==RayTraceResult.Type.BLOCK;
            float target=blocked?.12F:1F;
            Float old=VISIBILITY.get(effect);
            VISIBILITY.put(effect,old==null?target:old+(target-old)*.4F);
        }
    }
    private static void overlay(RenderGameOverlayEvent.Post event){
        if(event.getType()!=RenderGameOverlayEvent.ElementType.ALL)return;Minecraft mc=Minecraft.getInstance();if(mc.player==null)return;
        float flash=0;for(WorldTurtleEffectEntity e:NEARBY)if(e.isAlive()&&e.kind()==1&&e.age()<70){
            float distance=(float)mc.gameRenderer.getMainCamera().getPosition().distanceTo(e.position());
            flash=Math.max(flash,WorldTurtleCameraEffects.flash(e.age()+event.getPartialTicks(),distance,VISIBILITY.getOrDefault(e,.12F)));
        }
        if(flash>.005F)AbstractGui.fill(event.getMatrixStack(),0,0,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight(),((int)(flash*255)<<24)|0xfffbed);
    }
    private static void camera(EntityViewRenderEvent.CameraSetup event){
        Minecraft mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
        float shake=0;for(WorldTurtleEffectEntity e:NEARBY)if(e.isAlive()&&e.kind()==1){
            float distance=(float)event.getInfo().getPosition().distanceTo(e.position());
            shake=Math.max(shake,WorldTurtleCameraEffects.destructionShake(e.age()+event.getRenderPartialTicks(),distance));
        }
        for(SupernovaEntity star:STARS)if(star.isAlive()){
            double distance=event.getInfo().getPosition().distanceTo(star.position());
            shake=Math.max(shake,WorldTurtleCameraEffects.supernovaShake(star.age()+event.getRenderPartialTicks(),distance));
        }
        // Use bounded camera offsets, not player rotation or movement changes.
        double time=mc.level.getGameTime()%100000+event.getRenderPartialTicks();
        event.setPitch(event.getPitch()+(float)(Math.sin(time*2.17)*.7+Math.sin(time*.83)*.3)*shake);
        event.setYaw(event.getYaw()+(float)(Math.sin(time*1.71)*.5+Math.sin(time*.61)*.2)*shake);
        event.setRoll(event.getRoll()+(float)Math.sin(time*1.37)*shake*.45F);
    }
}
