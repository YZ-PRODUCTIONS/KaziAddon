package net.kazi.kazimod.doku;

import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
import java.util.ArrayDeque;
import java.util.Deque;

/** One tracked volume per source; fades after the source stops refreshing it. */
public final class DokuGasEntity extends Entity {
    public static final int CLOUD=0, DEMON=1, PROJECTILE=2, BURST=3, ROAD=4;
    private static final DataParameter<Integer> KIND=EntityDataManager.defineId(DokuGasEntity.class,DataSerializers.INT);
    private static final DataParameter<Integer> OWNER=EntityDataManager.defineId(DokuGasEntity.class,DataSerializers.INT);
    private static final DataParameter<Integer> FADE=EntityDataManager.defineId(DokuGasEntity.class,DataSerializers.INT);
    private static final DataParameter<Float> RADIUS=EntityDataManager.defineId(DokuGasEntity.class,DataSerializers.FLOAT);
    private static final DataParameter<Boolean> RED=EntityDataManager.defineId(DokuGasEntity.class,DataSerializers.BOOLEAN);
    private int refreshed, fadeStart=-1;
    private final Deque<Vector3d> trail=new ArrayDeque<>();
    public DokuGasEntity(EntityType<? extends DokuGasEntity> type,World world){super(type,world);noPhysics=true;setNoGravity(true);}
    public static DokuGasEntity refresh(DokuGasEntity old,Entity source,int kind,float radius,boolean red,boolean follow){
        if(source.level.isClientSide||!Float.isFinite(radius)||radius<=0)return old;
        if(old==null||!old.isAlive()||old.level!=source.level||old.fadeStart>=0){
            old=new DokuGasEntity(KaziEntities.DOKU_GAS.get(),source.level);
            old.entityData.set(KIND,kind);old.entityData.set(OWNER,follow?source.getId():-1);
            old.setPos(source.getX(),source.getY(),source.getZ());
            old.entityData.set(RADIUS,Math.min(32,radius));old.entityData.set(RED,red);
            source.level.addFreshEntity(old);
        }
        old.refreshed=old.tickCount;old.entityData.set(RADIUS,Math.min(32,radius));old.entityData.set(RED,red);return old;
    }
    public void finish(){if(!level.isClientSide&&fadeStart<0){fadeStart=tickCount;entityData.set(FADE,1);}}
    public Entity owner(){return level.getEntity(entityData.get(OWNER));}
    public int kind(){return entityData.get(KIND);}
    public float radius(){return entityData.get(RADIUS);}
    public boolean red(){return entityData.get(RED);}
    public boolean fading(){return entityData.get(FADE)>0;}
    public Deque<Vector3d> trail(){return trail;}
    public float opacity(float partial){return Math.min(1,(tickCount+partial)/10F)*(fadeStart<0?1:Math.max(0,1-(tickCount-fadeStart+partial)/20F));}
    @Override public void tick(){
        super.tick();Entity owner=owner();
        if(owner!=null&&owner.isAlive()&&fadeStart<0)setPos(owner.getX(),owner.getY(),owner.getZ());
        if(level.isClientSide){
            if(entityData.get(FADE)>0&&fadeStart<0)fadeStart=tickCount;
            if(kind()==ROAD||kind()==PROJECTILE){
                Vector3d point=position().add(0,kind()==ROAD?-.35:0,0);
                if(!trail.isEmpty()&&trail.peekFirst().distanceToSqr(point)>144)trail.clear();
                if(trail.isEmpty()||trail.peekFirst().distanceToSqr(point)>.04){trail.addFirst(point);while(trail.size()>32)trail.removeLast();}
                else if(trail.size()>2&&tickCount%2==0)trail.removeLast();
            }
        }else{
            if(tickCount-refreshed>(kind()==BURST?16:6)||(entityData.get(OWNER)>=0&&(owner==null||!owner.isAlive())))finish();
            if(fadeStart>=0&&tickCount-fadeStart>=20)remove();
        }
    }
    @Override public AxisAlignedBB getBoundingBoxForCulling(){return getBoundingBox().inflate(kind()==ROAD?40:radius()*2+4);}
    @Override public boolean shouldRenderAtSqrDistance(double distance){return distance<160*160;}
    @Override protected void defineSynchedData(){entityData.define(KIND,CLOUD);entityData.define(OWNER,-1);entityData.define(FADE,0);entityData.define(RADIUS,3F);entityData.define(RED,false);}
    @Override protected void readAdditionalSaveData(CompoundNBT nbt){}
    @Override protected void addAdditionalSaveData(CompoundNBT nbt){}
    @Override public IPacket<?> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
