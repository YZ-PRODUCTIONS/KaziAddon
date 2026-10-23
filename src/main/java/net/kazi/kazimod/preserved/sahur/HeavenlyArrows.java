package net.kazi.kazimod.preserved.sahur;

import java.util.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.kazi.kazimod.abilities.TripelT.TripelTHelper;
import net.kazi.kazimod.entities.projectiles.LightArrowProjectile;

public final class HeavenlyArrows {
    public static final int PORTALS=5;
    public static List<SahurLightEntity> open(LivingEntity owner,int duration){
        List<SahurLightEntity> portals=new ArrayList<>();if(owner.level.isClientSide)return portals;
        for(int i=0;i<PORTALS;i++){
            double angle=Math.PI*2*i/PORTALS;
            Vector3d pos=owner.position().add(Math.cos(angle)*8,12,Math.sin(angle)*8);
            if(owner.level.hasChunkAt(new net.minecraft.util.math.BlockPos(pos))) portals.add(SahurLightEntity.spawn(owner,3,pos,duration));
        }
        return portals;
    }
    public static void volley(LivingEntity owner,List<SahurLightEntity> portals,float damage){
        if(owner.level.isClientSide)return;
        Vector3d eye=owner.getEyePosition(1);
        Vector3d aim=eye.add(owner.getLookAngle().scale(96));
        BlockRayTraceResult target=owner.level.clip(new RayTraceContext(eye,aim,
                RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,owner));
        if(target.getType()==RayTraceResult.Type.BLOCK)aim=target.getLocation();
        for(SahurLightEntity portal:portals){
            if(portal==null||!portal.isAlive())continue;
            Vector3d start=portal.position().add(0,-.35,0),direction=aim.subtract(start).normalize();
            LightArrowProjectile arrow=new LightArrowProjectile(owner.level,owner,damage);
            arrow.setPos(start.x,start.y,start.z);arrow.setDeltaMovement(direction.scale(1.25));
            owner.level.addFreshEntity(arrow);
        }
    }
    public static void close(List<SahurLightEntity> portals){for(SahurLightEntity p:portals)if(p!=null)p.remove();portals.clear();}
}
