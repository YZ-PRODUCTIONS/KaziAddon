package net.kazi.kazimod.worldturtle;

import java.util.*;
import net.kazi.kazimod.abilities.KameRework.SupernovaAbility;
import net.minecraft.entity.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
import xyz.pixelatedw.mineminenomi.api.protection.ProtectedArea;
import xyz.pixelatedw.mineminenomi.data.world.ProtectedAreasData;

public final class SupernovaEntity extends Entity {
    private static final DataParameter<Integer> OWNER=EntityDataManager.defineId(SupernovaEntity.class,DataSerializers.INT);
    private static final DataParameter<Integer> START=EntityDataManager.defineId(SupernovaEntity.class,DataSerializers.INT);
    private static final DataParameter<Float> ANGLE=EntityDataManager.defineId(SupernovaEntity.class,DataSerializers.FLOAT);
    private static final List<DataParameter<Rotations>> ENDS=ends();
    private static List<DataParameter<Rotations>> ends(){List<DataParameter<Rotations>> r=new ArrayList<>();for(int i=0;i<5;i++)r.add(EntityDataManager.defineId(SupernovaEntity.class,DataSerializers.ROTATIONS));return r;}
    public SupernovaEntity(EntityType<?> type,World world){super(type,world);noPhysics=true;setNoGravity(true);}
    protected void defineSynchedData(){entityData.define(OWNER,-1);entityData.define(START,0);entityData.define(ANGLE,0F);for(DataParameter<Rotations> end:ENDS)entityData.define(end,new Rotations(0,0,0));}
    public int age(){return Math.max(0,(int)level.getGameTime()-entityData.get(START));}
    public LivingEntity owner(){Entity e=level.getEntity(entityData.get(OWNER));return e instanceof LivingEntity?(LivingEntity)e:null;}
    public float angle(){return entityData.get(ANGLE);}
    public Vector3d star(int index){double a=angle()+index*Math.PI*2/5;return position().add(Math.cos(a)*26,16,Math.sin(a)*26);}
    public Vector3d end(int index){Rotations r=entityData.get(ENDS.get(index));return position().add(r.getX(),r.getY(),r.getZ());}
    public static SupernovaEntity spawn(LivingEntity owner){
        SupernovaEntity e=new SupernovaEntity(WorldTurtleEffects.SUPERNOVA.get(),owner.level);
        e.entityData.set(OWNER,owner.getId());e.entityData.set(START,(int)owner.level.getGameTime());e.entityData.set(ANGLE,(float)Math.toRadians(owner.yBodyRot));
        e.setPos(owner.getX(),owner.getY(),owner.getZ());owner.level.addFreshEntity(e);
        owner.level.playSound(null,owner.blockPosition(),SoundEvents.BEACON_POWER_SELECT,SoundCategory.PLAYERS,5,.55F);return e;
    }
    public void tick(){
        LivingEntity owner=owner();if(owner!=null)setPos(owner.getX(),owner.getY(),owner.getZ());
        if(level.isClientSide)return;
        if(!WorldTurtleHitboxes.active(owner)||age()>=SupernovaAbility.DURATION){remove();return;}
        SupernovaAbility ability=SupernovaAbility.active(owner);if(ability==null){remove();return;}
        if(age()==SupernovaAbility.CHARGE)level.playSound(null,blockPosition(),SoundEvents.BEACON_ACTIVATE,SoundCategory.PLAYERS,8,.5F);
        if(age()<SupernovaAbility.CHARGE)return;
        if(age()%2==0){
            Vector3d eye=owner.getEyePosition(1),aim=eye.add(owner.getLookAngle().scale(128));
            BlockRayTraceResult target=level.clip(new RayTraceContext(eye,aim,RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,owner));
            if(target.getType()==RayTraceResult.Type.BLOCK)aim=target.getLocation();
            for(int i=0;i<5;i++){
                Vector3d star=star(i),delta=aim.subtract(star);if(delta.lengthSqr()<1)delta=owner.getLookAngle();
                Vector3d far=star.add(delta.normalize().scale(Math.min(160,Math.max(1,delta.length()))));
                Vector3d point=level.clip(new RayTraceContext(star,far,RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,this)).getLocation().subtract(position());
                entityData.set(ENDS.get(i),new Rotations((float)point.x,(float)point.y,(float)point.z));
            }
        }
        if(age()%10==0){
            Set<UUID> hit=new HashSet<>();
            for(int i=0;i<5;i++){
                Vector3d start=star(i),end=end(i);
                for(LivingEntity target:level.getEntitiesOfClass(LivingEntity.class,new AxisAlignedBB(start,end).inflate(2.5))){
                    LivingEntity actual=target instanceof WorldTurtlePartEntity?((WorldTurtlePartEntity)target).owner():target;
                    if(actual==null||actual==owner||!actual.isAlive()||actual.isSpectator()||owner.isAlliedTo(actual)||hit.contains(actual.getUUID()))continue;
                    if(!target.getBoundingBox().inflate(2.5).clip(start,end).isPresent())continue;
                    BlockPos pos=target.blockPosition();ProtectedArea area=ProtectedAreasData.get(level).getProtectedArea(pos.getX(),pos.getY(),pos.getZ());
                    if(area!=null&&!area.canHurtEntities())continue;
                    if(target instanceof WorldTurtlePartEntity&&((WorldTurtlePartEntity)target).isShell())continue;
                    hit.add(actual.getUUID());ability.hit(owner,actual);
                }
            }
        }
    }
    public AxisAlignedBB getBoundingBoxForCulling(){return getBoundingBox().inflate(190);}
    public boolean shouldRenderAtSqrDistance(double distance){return distance<320*320;}
    protected void readAdditionalSaveData(CompoundNBT n){remove();}protected void addAdditionalSaveData(CompoundNBT n){}
    public IPacket<?> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
