package net.kazi.kazimod.mammoth;

import net.minecraft.entity.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public final class MammothVfxEntity extends Entity {
    public static final int QUAKE=0,FINALE=1,VACUUM=2;
    private static final DataParameter<Integer> KIND=EntityDataManager.defineId(MammothVfxEntity.class,DataSerializers.INT),OWNER=EntityDataManager.defineId(MammothVfxEntity.class,DataSerializers.INT),START=EntityDataManager.defineId(MammothVfxEntity.class,DataSerializers.INT),LIFE=EntityDataManager.defineId(MammothVfxEntity.class,DataSerializers.INT);
    private static final DataParameter<Float> RADIUS=EntityDataManager.defineId(MammothVfxEntity.class,DataSerializers.FLOAT);
    private boolean vacuumSoundStarted;
    public MammothVfxEntity(EntityType<?> type,World level){super(type,level);noPhysics=true;setNoGravity(true);}
    @Override protected void defineSynchedData(){entityData.define(KIND,0);entityData.define(OWNER,-1);entityData.define(START,0);entityData.define(LIFE,20);entityData.define(RADIUS,5F);}
    public int kind(){return entityData.get(KIND);}public int age(){return Math.max(0,(int)level.getGameTime()-entityData.get(START));}public int life(){return entityData.get(LIFE);}public float radius(){return entityData.get(RADIUS);}
    public LivingEntity owner(){Entity owner=level.getEntity(entityData.get(OWNER));return owner instanceof LivingEntity?(LivingEntity)owner:null;}
    public static MammothVfxEntity spawn(LivingEntity owner,int kind,Vector3d position,float radius,int life){
        if(owner.level.isClientSide)return null;
        MammothVfxEntity e=new MammothVfxEntity(MammothFeatures.EFFECT.get(),owner.level);e.entityData.set(KIND,kind);e.entityData.set(OWNER,owner.getId());e.entityData.set(START,(int)owner.level.getGameTime());e.entityData.set(RADIUS,radius);e.entityData.set(LIFE,life);e.setPos(position.x,position.y,position.z);owner.level.addFreshEntity(e);return e;
    }
    @Override public void tick(){super.tick();LivingEntity owner=owner();
        if(kind()==VACUUM&&owner!=null)setPos(owner.getX(),owner.getY(),owner.getZ());
        if(level.isClientSide&&kind()==VACUUM&&!vacuumSoundStarted&&owner!=null&&age()<life()){
            vacuumSoundStarted=true;
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->MammothClient.playVacuumSound(this));
        }
        if(!level.isClientSide&&(age()>=life()||owner==null||!owner.isAlive()||!MammothCombat.active(owner)))remove();
    }
    @Override public AxisAlignedBB getBoundingBoxForCulling(){return getBoundingBox().inflate(radius()+5);}
    @Override public boolean shouldRenderAtSqrDistance(double d){return d<192*192;}
    @Override protected void readAdditionalSaveData(CompoundNBT nbt){remove();}
    @Override protected void addAdditionalSaveData(CompoundNBT nbt){}
    @Override public IPacket<?> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
