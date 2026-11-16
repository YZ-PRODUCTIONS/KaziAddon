package net.kazi.kazimod.hermano;

import net.minecraft.entity.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public final class HermanoEffect extends Entity {
    public static final int SHOCK=0,BLAST=1,HIT=2,GLARE=3;
    private static final DataParameter<Integer> KIND=EntityDataManager.defineId(HermanoEffect.class,DataSerializers.INT),LIFE=EntityDataManager.defineId(HermanoEffect.class,DataSerializers.INT),START=EntityDataManager.defineId(HermanoEffect.class,DataSerializers.INT);
    private static final DataParameter<Float> SIZE=EntityDataManager.defineId(HermanoEffect.class,DataSerializers.FLOAT);
    public HermanoEffect(EntityType<?> type,World world){super(type,world);noPhysics=true;setNoGravity(true);}
    @Override protected void defineSynchedData(){entityData.define(KIND,0);entityData.define(LIFE,20);entityData.define(START,0);entityData.define(SIZE,1F);}
    public static HermanoEffect spawn(LivingEntity owner,int kind,Vector3d pos,int life,float size){
        HermanoEffect effect=new HermanoEffect(HermanoFeatures.EFFECT.get(),owner.level);
        effect.entityData.set(KIND,kind);effect.entityData.set(LIFE,life);effect.entityData.set(SIZE,size);effect.entityData.set(START,(int)owner.level.getGameTime());
        effect.setPos(pos.x,pos.y,pos.z);owner.level.addFreshEntity(effect);return effect;
    }
    public HermanoEffect direction(float yaw,float pitch){yRot=yaw;xRot=pitch;return this;}
    public int kind(){return entityData.get(KIND);}public int life(){return entityData.get(LIFE);}public float size(){return entityData.get(SIZE);}
    public float age(float partial){return Math.max(0,(int)level.getGameTime()-entityData.get(START)+partial);}
    @Override public void tick(){super.tick();if(!level.isClientSide&&age(0)>=life())remove();}
    @Override public AxisAlignedBB getBoundingBoxForCulling(){return getBoundingBox().inflate(size()+3);}
    @Override protected void readAdditionalSaveData(CompoundNBT n){remove();}
    @Override protected void addAdditionalSaveData(CompoundNBT n){}
    @Override public IPacket<?> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
