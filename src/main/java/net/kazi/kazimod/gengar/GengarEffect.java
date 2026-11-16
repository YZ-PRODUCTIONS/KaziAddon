package net.kazi.kazimod.gengar;
import net.minecraft.entity.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
public final class GengarEffect extends Entity {
    public static final int PORTAL=0,BLAST=1,HAUNT=2,HANDS=3,TAG=4,BITE=5,HEAL=6,CURSE=7,SWEEP=8;
    private static final DataParameter<Integer> KIND=EntityDataManager.defineId(GengarEffect.class,DataSerializers.INT),LIFE=EntityDataManager.defineId(GengarEffect.class,DataSerializers.INT),TARGET=EntityDataManager.defineId(GengarEffect.class,DataSerializers.INT),START=EntityDataManager.defineId(GengarEffect.class,DataSerializers.INT),OWNER=EntityDataManager.defineId(GengarEffect.class,DataSerializers.INT);
    private static final DataParameter<Float> SIZE=EntityDataManager.defineId(GengarEffect.class,DataSerializers.FLOAT);
    public GengarEffect(EntityType<?> type,World world){super(type,world);noPhysics=true;setNoGravity(true);}
    @Override protected void defineSynchedData(){entityData.define(KIND,0);entityData.define(LIFE,20);entityData.define(TARGET,-1);entityData.define(START,0);entityData.define(OWNER,-1);entityData.define(SIZE,1F);}
    public static GengarEffect spawn(LivingEntity user,int kind,Vector3d pos,int life,float size){
        java.util.List<GengarEffect> existing=user.level.getEntitiesOfClass(GengarEffect.class,user.getBoundingBox().inflate(96),e->e.entityData.get(OWNER)==user.getId());
        if(existing.size()>=32)existing.stream().filter(e->e.kind()!=CURSE&&e.kind()!=TAG&&e.kind()!=HANDS).max(java.util.Comparator.comparingInt(e->e.tickCount)).ifPresent(Entity::remove);
        GengarEffect effect=new GengarEffect(GengarFeatures.EFFECT.get(),user.level);effect.entityData.set(KIND,kind);effect.entityData.set(LIFE,life);effect.entityData.set(SIZE,size);effect.entityData.set(START,(int)user.level.getGameTime());effect.entityData.set(OWNER,user.getId());effect.setPos(pos.x,pos.y,pos.z);user.level.addFreshEntity(effect);return effect;
    }
    public GengarEffect attach(LivingEntity target){entityData.set(TARGET,target.getId());return this;}
    public int kind(){return entityData.get(KIND);}public int life(){return entityData.get(LIFE);}public float size(){return entityData.get(SIZE);}
    public float age(float partial){return Math.max(0,(int)level.getGameTime()-entityData.get(START)+partial);}
    @Override public void tick(){super.tick();if(entityData.get(TARGET)>=0){Entity e=level.getEntity(entityData.get(TARGET));if(e!=null&&e.isAlive())setPos(e.getX(),e.getY(),e.getZ());else if(!level.isClientSide)remove();}if(!level.isClientSide&&age(0)>=life())remove();}
    @Override public AxisAlignedBB getBoundingBoxForCulling(){return getBoundingBox().inflate(size()*2+3);}
    @Override protected void readAdditionalSaveData(CompoundNBT n){remove();}@Override protected void addAdditionalSaveData(CompoundNBT n){}
    @Override public IPacket<?> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
