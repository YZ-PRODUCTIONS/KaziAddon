package net.kazi.kazimod.kake;

import net.minecraft.entity.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public final class KakeVfxEntity extends Entity {
    public static final int SLOT=0,CAST=1,DOUBLE=2,SHOT=3,RAIN=4,SEVEN=5,STORM=6,JACKPOT=7,IMPACT=8;
    private static final DataParameter<Integer> KIND=EntityDataManager.defineId(KakeVfxEntity.class,DataSerializers.INT),
            OWNER=EntityDataManager.defineId(KakeVfxEntity.class,DataSerializers.INT),START=EntityDataManager.defineId(KakeVfxEntity.class,DataSerializers.INT),
            LIFE=EntityDataManager.defineId(KakeVfxEntity.class,DataSerializers.INT),VARIANT=EntityDataManager.defineId(KakeVfxEntity.class,DataSerializers.INT),
            REVEAL=EntityDataManager.defineId(KakeVfxEntity.class,DataSerializers.INT),RESULT=EntityDataManager.defineId(KakeVfxEntity.class,DataSerializers.INT);
    private static final DataParameter<Float> RADIUS=EntityDataManager.defineId(KakeVfxEntity.class,DataSerializers.FLOAT),ANGLE=EntityDataManager.defineId(KakeVfxEntity.class,DataSerializers.FLOAT);
    private static final DataParameter<Boolean> FOLLOW=EntityDataManager.defineId(KakeVfxEntity.class,DataSerializers.BOOLEAN);
    public KakeVfxEntity(EntityType<?> type,World world){super(type,world);noPhysics=true;setNoGravity(true);}
    protected void defineSynchedData(){entityData.define(KIND,0);entityData.define(OWNER,-1);entityData.define(START,0);entityData.define(LIFE,60);entityData.define(VARIANT,0);entityData.define(REVEAL,-1);entityData.define(RESULT,-1);entityData.define(RADIUS,1F);entityData.define(ANGLE,0F);entityData.define(FOLLOW,false);}
    public int kind(){return entityData.get(KIND);}public int variant(){return entityData.get(VARIANT);}public int result(){return entityData.get(RESULT);}
    public int age(){return Math.max(0,(int)level.getGameTime()-entityData.get(START));}public int life(){return entityData.get(LIFE);}public int revealAge(){return entityData.get(REVEAL);}
    public float radius(){return entityData.get(RADIUS);}public float angle(){return entityData.get(ANGLE);}
    public static KakeVfxEntity spawn(LivingEntity owner,int kind,Vector3d pos,int life,float radius,int variant,boolean follow){
        if(owner.level.isClientSide)return null;
        KakeVfxEntity e=new KakeVfxEntity(KakeVisuals.EFFECT.get(),owner.level);
        e.entityData.set(KIND,kind);e.entityData.set(OWNER,owner.getId());e.entityData.set(START,(int)owner.level.getGameTime());e.entityData.set(LIFE,life);
        e.entityData.set(RADIUS,radius);e.entityData.set(VARIANT,variant);e.entityData.set(FOLLOW,follow);e.entityData.set(ANGLE,owner.yRot+180);
        e.setPos(pos.x,pos.y,pos.z);e.followOwner(owner);owner.level.addFreshEntity(e);return e;
    }
    public void reveal(int number){if(level.isClientSide||kind()!=SLOT)return;entityData.set(RESULT,Math.max(0,Math.min(9,number)));entityData.set(REVEAL,age());entityData.set(LIFE,age()+44);}
    private void followOwner(Entity owner){
        if(!entityData.get(FOLLOW))return;
        if(kind()==SLOT){double a=Math.toRadians(angle()-180),x=-Math.sin(a)*2.7+Math.cos(a)*.65,z=Math.cos(a)*2.7+Math.sin(a)*.65;setPos(owner.getX()+x,owner.getY()+.05,owner.getZ()+z);}
        else {setPos(owner.getX(),owner.getY(),owner.getZ());if(!level.isClientSide)entityData.set(ANGLE,-owner.yRot);}
    }
    public void tick(){super.tick();Entity owner=level.getEntity(entityData.get(OWNER));
        if(owner!=null)followOwner(owner);
        if(!level.isClientSide&&(age()>=life()||owner==null||!owner.isAlive()))remove();
    }
    public AxisAlignedBB getBoundingBoxForCulling(){return getBoundingBox().inflate(Math.max(5,radius()+3),36,Math.max(5,radius()+3));}
    public boolean shouldRenderAtSqrDistance(double d){return d<256*256;}
    protected void readAdditionalSaveData(CompoundNBT tag){remove();}protected void addAdditionalSaveData(CompoundNBT tag){}
    public IPacket<?> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
