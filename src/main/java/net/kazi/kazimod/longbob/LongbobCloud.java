package net.kazi.kazimod.longbob;

import net.kazi.kazimod.doku.DokuGasEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

/** Chloro Ball's temporary poisonous ground cloud; never leaves purple poison blocks. */
public final class LongbobCloud extends Entity {
    private LivingEntity owner;
    private float radius=6;
    private int life=100;
    private DokuGasEntity gas;
    public LongbobCloud(EntityType<?> type,World world){super(type,world);noPhysics=true;setNoGravity(true);}
    public static LongbobCloud spawn(LivingEntity owner,Vector3d at,float radius,int life){
        LongbobCloud cloud=new LongbobCloud(LongbobFeatures.CLOUD.get(),owner.level);cloud.owner=owner;cloud.radius=radius;cloud.life=life;
        cloud.setPos(at.x,at.y,at.z);owner.level.addFreshEntity(cloud);return cloud;
    }
    @Override public void tick(){
        super.tick();if(level.isClientSide)return;
        if(owner==null||!owner.isAlive()||owner.level!=level||tickCount>=life){remove();return;}
        gas=DokuGasEntity.refresh(gas,this,DokuGasEntity.CLOUD,radius,false,false).green(true);
        if(tickCount%20!=0)return;
        Vector3d at=position().add(0,.6,0);
        for(LivingEntity target:level.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(radius))){
            if(target==owner||!target.isAlive()||owner.isAlliedTo(target)||target instanceof PlayerEntity&&(((PlayerEntity)target).isCreative()||target.isSpectator()))continue;
            if(target.getBoundingBox().getCenter().distanceToSqr(at)>radius*radius)continue;
            if(level.clip(new RayTraceContext(at,target.getBoundingBox().getCenter(),RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,this)).getType()!=RayTraceResult.Type.MISS)continue;
            LongbobEntity.poison(target,200,0);
        }
    }
    @Override public void remove(){if(gas!=null)gas.finish();super.remove();}
    @Override protected void defineSynchedData(){}
    @Override protected void readAdditionalSaveData(CompoundNBT n){}
    @Override protected void addAdditionalSaveData(CompoundNBT n){}
    @Override public IPacket<?> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
