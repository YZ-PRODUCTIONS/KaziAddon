package net.kazi.kazimod.mochi;

import net.MrMagicalCart.cartaddon.particles.effects.mochi.*;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

public final class MochiVisuals {
    public static final int COATING=0,GRAB=1,FLOW=2,RIVER=3,BARRAGE=4,SLAM=5,IMPACT=6;
    private MochiVisuals(){}
    public static int kind(ParticleEffect<?> effect) {
        if(effect instanceof MochiParticleEffect)return COATING;
        if(effect instanceof MochiGrabbedParticleEffect)return GRAB;
        if(effect instanceof NagareMochiParticleEffect)return FLOW;
        if(effect instanceof PastryRiverParticleEffect)return RIVER;
        if(effect instanceof MochiBarrageParticleEffect)return BARRAGE;
        if(effect instanceof ZanGiriMochiParticleEffect)return SLAM;
        return -1;
    }
    public static boolean projectile(Entity entity) {
        ResourceLocation id=entity.getType().getRegistryName();
        if(id==null||!id.getNamespace().equals("cartaddon"))return false;
        switch(id.getPath()) {
            case "mochi_hadan":case "gatling_mochi":case "mochi_better_elephant_gun":case "yaki_mochi":
            case "kuri_mochi_projectile":case "mochi_grab":case "pastry_river":case "mochi_barrage":
            case "down_slam":case "chikara_mochi":case "kaku_mochi_right":case "kaku_mochi_left":case "anemone":case "donut":return true;
            default:return false;
        }
    }
    public static float radius(int kind){return kind==SLAM?10:kind==RIVER?8:kind==BARRAGE?3:kind==FLOW?2:1;}
    public static boolean follows(int kind){return kind==COATING||kind==GRAB;}
}
