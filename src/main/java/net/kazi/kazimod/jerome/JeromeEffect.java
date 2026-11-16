package net.kazi.kazimod.jerome;

import net.minecraft.entity.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public final class JeromeEffect extends Entity {
    public static final int SHOCK=0,BLAST=1,HIT=2,SLAM=3;
    private static final DataParameter<Integer> KIND=EntityDataManager.defineId(JeromeEffect.class,DataSerializers.INT),LIFE=EntityDataManager.defineId(JeromeEffect.class,DataSerializers.INT),START=EntityDataManager.defineId(JeromeEffect.class,DataSerializers.INT);
    private static final DataParameter<Float> SIZE=EntityDataManager.defineId(JeromeEffect.class,DataSerializers.FLOAT);
    public JeromeEffect(EntityType<?> type,World world){super(type,world);noPhysics=true;setNoGravity(true);}
    @Override protected void defineSynchedData(){entityData.define(KIND,0);entityData.define(LIFE,20);entityData.define(START,0);entityData.define(SIZE,1F);}
    public static JeromeEffect spawn(LivingEntity owner,int kind,Vector3d pos,int life,float size){
        JeromeEffect effect=new JeromeEffect(JeromeFeatures.EFFECT.get(),owner.level);
        effect.entityData.set(KIND,kind);effect.entityData.set(LIFE,life);effect.entityData.set(SIZE,size);effect.entityData.set(START,(int)owner.level.getGameTime());
        effect.setPos(pos.x,pos.y,pos.z);owner.level.addFreshEntity(effect);return effect;
    }
    public JeromeEffect direction(float yaw,float pitch){yRot=yaw;xRot=pitch;return this;}
    public int kind(){return entityData.get(KIND);}public int life(){return entityData.get(LIFE);}public float size(){return entityData.get(SIZE);}
    public float age(float partial){return Math.max(0,(int)level.getGameTime()-entityData.get(START)+partial);}
    @Override public void tick(){super.tick();if(!level.isClientSide&&age(0)>=life())remove();}
    @Override public AxisAlignedBB getBoundingBoxForCulling(){return getBoundingBox().inflate(size()+3);}
    @Override protected void readAdditionalSaveData(CompoundNBT n){remove();}
    @Override protected void addAdditionalSaveData(CompoundNBT n){}
    @Override public IPacket<?> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}


