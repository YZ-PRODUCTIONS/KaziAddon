package net.kazi.kazimod.saiyan;

import net.minecraft.entity.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public final class SaiyanEffect extends Entity {
    public static final int SPIRIT_IMPACT=6, TRANSFORMATION_AURA=7, FLIGHT_AURA=8, INSTINCT_DODGE=9;
    public static final int INSTINCT_DRAGON=10, FINISHER_IMPACT=11, METEOR_REENTRY=12;
    private static final DataParameter<Integer> KIND=EntityDataManager.defineId(SaiyanEffect.class,DataSerializers.INT),
            OWNER=EntityDataManager.defineId(SaiyanEffect.class,DataSerializers.INT),LIFE=EntityDataManager.defineId(SaiyanEffect.class,DataSerializers.INT);
    private static final DataParameter<Integer> START=EntityDataManager.defineId(SaiyanEffect.class,DataSerializers.INT);
    private static final DataParameter<Integer> FORM=EntityDataManager.defineId(SaiyanEffect.class,DataSerializers.INT);
    private static final DataParameter<Float> SIZE=EntityDataManager.defineId(SaiyanEffect.class,DataSerializers.FLOAT);
    private static final DataParameter<Integer> TARGET=EntityDataManager.defineId(SaiyanEffect.class,DataSerializers.INT),
            PHASE=EntityDataManager.defineId(SaiyanEffect.class,DataSerializers.INT),PHASE_START=EntityDataManager.defineId(SaiyanEffect.class,DataSerializers.INT);
    private static final DataParameter<Float> CARRY_YAW=EntityDataManager.defineId(SaiyanEffect.class,DataSerializers.FLOAT),
            CARRY_PITCH=EntityDataManager.defineId(SaiyanEffect.class,DataSerializers.FLOAT);
    public SaiyanEffect(EntityType<?> type,World world){super(type,world);noPhysics=true;setNoGravity(true);}
    protected void defineSynchedData(){entityData.define(KIND,0);entityData.define(OWNER,-1);entityData.define(LIFE,20);entityData.define(START,0);entityData.define(SIZE,1F);entityData.define(FORM,0);
        entityData.define(TARGET,-1);entityData.define(PHASE,0);entityData.define(PHASE_START,0);entityData.define(CARRY_YAW,0F);entityData.define(CARRY_PITCH,0F);}
    public int target(){return entityData.get(TARGET);}public int phase(){return entityData.get(PHASE);}
    public float phaseAge(){return Math.max(0,level.getGameTime()-entityData.get(PHASE_START));}
    public float carryYaw(){return entityData.get(CARRY_YAW);}public float carryPitch(){return entityData.get(CARRY_PITCH);}
    public void carry(Vector3d point,float yaw,float pitch,int target,int phase){
        setPos(point.x,point.y,point.z);entityData.set(TARGET,target);entityData.set(CARRY_YAW,yaw);entityData.set(CARRY_PITCH,pitch);
        if(phase!=phase()){entityData.set(PHASE,phase);entityData.set(PHASE_START,(int)level.getGameTime());}
    }
    public int kind(){return entityData.get(KIND);}public int life(){return entityData.get(LIFE);}public float size(){return entityData.get(SIZE);}
    public int age(){return Math.max(0,(int)level.getGameTime()-entityData.get(START));}
    public LivingEntity owner(){Entity e=level.getEntity(entityData.get(OWNER));return e instanceof LivingEntity?(LivingEntity)e:null;}
    public int form(){return entityData.get(FORM);}
    public SaiyanEffect withForm(int form){entityData.set(FORM,SaiyanBalance.tier(form));return this;}
    public static SaiyanEffect spawn(LivingEntity owner,int kind,Vector3d pos,int life,float size){
        return spawn(owner.level,owner,kind,pos,life,size);
    }
    public static SaiyanEffect spawn(World world,LivingEntity owner,int kind,Vector3d pos,int life,float size){
        SaiyanEffect e=new SaiyanEffect(SaiyanFeatures.EFFECT.get(),world);
        e.entityData.set(KIND,kind);e.entityData.set(OWNER,owner==null?-1:owner.getId());e.entityData.set(START,(int)world.getGameTime());
        e.entityData.set(FORM,owner==null?0:SaiyanState.form(owner));
        e.entityData.set(LIFE,life);e.entityData.set(SIZE,size);e.setPos(pos.x,pos.y,pos.z);world.addFreshEntity(e);return e;
    }
    public void tick(){
        super.tick();LivingEntity owner=owner();
        if(kind()==TRANSFORMATION_AURA||kind()==FLIGHT_AURA){
            if(!level.isClientSide)remove();
            return;
        }
        if(kind()>=1&&kind()<=3&&owner!=null)setPos(owner.getX(),owner.getY(),owner.getZ());
        if(!level.isClientSide&&(age()>=life()||((kind()!=SPIRIT_IMPACT&&kind()!=FINISHER_IMPACT)&&(owner==null||!owner.isAlive()))))remove();
    }
    public boolean shouldRenderAtSqrDistance(double d){return d<(kind()==INSTINCT_DRAGON?512*512:256*256);}
    public AxisAlignedBB getBoundingBoxForCulling(){return getBoundingBox().inflate(kind()==INSTINCT_DRAGON?70:kind()==METEOR_REENTRY?32:size()*3+12);}
    protected void readAdditionalSaveData(CompoundNBT n){remove();}protected void addAdditionalSaveData(CompoundNBT n){}
    public IPacket<?> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
