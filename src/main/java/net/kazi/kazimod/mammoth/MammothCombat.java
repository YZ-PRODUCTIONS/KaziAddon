package net.kazi.kazimod.mammoth;

import java.util.List;
import java.util.stream.Collectors;
import net.kazi.kazimod.init.KaziPacketHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.api.protection.ProtectedArea;
import xyz.pixelatedw.mineminenomi.data.world.ProtectedAreasData;
import xyz.pixelatedw.mineminenomi.init.ModMorphs;

public final class MammothCombat {
    public static boolean full(LivingEntity e){return ModMorphs.MAMMOTH_GUARD.get().isActive(e);}
    public static boolean active(LivingEntity e){return full(e)||ModMorphs.MAMMOTH_HEAVY.get().isActive(e);}
    public static boolean enemy(LivingEntity owner,LivingEntity target){
        if(target instanceof net.kazi.kazimod.worldturtle.WorldTurtlePartEntity)return false;
        if(target==owner||!target.isAlive()||target.isSpectator()||owner.isAlliedTo(target)||owner.hasPassenger(target)||target.hasPassenger(owner))return false;
        if(target instanceof PlayerEntity&&((PlayerEntity)target).isCreative())return false;
        if(owner instanceof PlayerEntity&&target instanceof PlayerEntity&&!((PlayerEntity)owner).canHarmPlayer((PlayerEntity)target))return false;
        BlockPos p=target.blockPosition();ProtectedArea area=ProtectedAreasData.get(owner.level).getProtectedArea(p.getX(),p.getY(),p.getZ());
        return area==null||area.canHurtEntities();
    }
    public static List<LivingEntity> targets(LivingEntity owner,double radius,double height){
        AxisAlignedBB bounds=new AxisAlignedBB(owner.position(),owner.position()).inflate(radius,height,radius);
        return owner.level.getEntitiesOfClass(LivingEntity.class,bounds,t->enemy(owner,t)).stream()
                .filter(t->owner.canSee(t)).collect(Collectors.toList());
    }
    public static Vector3d mouth(LivingEntity owner){
        boolean full=full(owner);double[] p=MammothCombatMath.vacuumTip(full,owner.tickCount,MathHelper.wrapDegrees(owner.yHeadRot-owner.yBodyRot),owner.xRot);
        double scale=(full?1.85:2.9)*.9375/16,a=Math.toRadians(owner.yBodyRot),x=p[0]*scale,z=p[2]*scale;
        return owner.position().add(Math.cos(a)*x+Math.sin(a)*z,p[1]*scale,Math.sin(a)*x-Math.cos(a)*z);
    }
    public static Vector3d ground(LivingEntity owner){
        Vector3d from=owner.position().add(0,1,0);
        BlockRayTraceResult hit=owner.level.clip(new RayTraceContext(from,from.add(0,-7,0),RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,owner));
        return hit.getType()==RayTraceResult.Type.BLOCK?hit.getLocation().add(0,.025,0):owner.position();
    }
    public static void quake(LivingEntity owner,float radius,boolean finale){
        if(owner.level.isClientSide)return;
        MammothVfxEntity.spawn(owner,finale?MammothVfxEntity.FINALE:MammothVfxEntity.QUAKE,ground(owner),radius,finale?28:18);
        shake(owner,radius+12,finale?20:8,finale?.7F:.26F);
    }
    public static void shake(LivingEntity owner,double radius,int ticks,float strength){
        if(!(owner.level instanceof ServerWorld))return;
        for(ServerPlayerEntity player:((ServerWorld)owner.level).players()){
            double d=player.position().distanceTo(owner.position());if(d<radius)KaziPacketHandler.sendCameraShake(player,ticks,strength*(float)(1-d/radius));
        }
    }
    private MammothCombat(){}
}
