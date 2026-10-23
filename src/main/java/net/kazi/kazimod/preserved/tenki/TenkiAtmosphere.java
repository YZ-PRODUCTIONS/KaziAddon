package net.kazi.kazimod.preserved.tenki;

import net.minecraft.client.Minecraft;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="kazimod",value=Dist.CLIENT)
public final class TenkiAtmosphere {
    private static float intensity;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getInstance();if(mc.level==null||mc.player==null){intensity=0;return;}
        Vector3d pos=mc.gameRenderer.getMainCamera().getPosition();float target=0;
        for(TenkiVisualEntity e:mc.level.getEntitiesOfClass(TenkiVisualEntity.class,new AxisAlignedBB(pos,pos).inflate(100,65,100))){
            if(e.kind()!=1)continue;Vector3d d=pos.subtract(e.position());
            if(d.y>1||d.y< -52)continue;
            float edge=(float)Math.sqrt(d.x*d.x+d.z*d.z);target=Math.max(target,Math.max(0,Math.min(1,(83-edge)/12)));
        }
        intensity+=(target-intensity)*.1F;
        if(intensity>.15F&&mc.level.getGameTime()%60==0&&!mc.isPaused())mc.level.playLocalSound(pos.x,pos.y,pos.z,SoundEvents.ELYTRA_FLYING,SoundCategory.WEATHER,intensity*.7F,.65F,false);
    }
    @SubscribeEvent public static void fog(EntityViewRenderEvent.FogColors event){
        event.setRed(event.getRed()*(1-intensity*.55F));event.setGreen(event.getGreen()*(1-intensity*.48F));event.setBlue(event.getBlue()*(1-intensity*.36F));
    }
}
