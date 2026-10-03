package net.kazi.kazimod.preserved.cerberus;

import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.potion.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;

/** One bounded entity per projectile/effect; no particle emitters or permanent terrain changes. */
public final class CerberusEffectEntity extends Entity {
    private static final DataParameter<Integer> KIND=EntityDataManager.defineId(CerberusEffectEntity.class,DataSerializers.INT);
    private static final DataParameter<Integer> OWNER=EntityDataManager.defineId(CerberusEffectEntity.class,DataSerializers.INT);
    private static final DataParameter<Integer> AGE=EntityDataManager.defineId(CerberusEffectEntity.class,DataSerializers.INT);
    private LivingEntity caster;
    private final Set<UUID> inside = new HashSet<>();
    public CerberusEffectEntity(EntityType<?> type,World world){super(type,world);noPhysics=true;setNoGravity(true);}
    @Override protected void defineSynchedData(){entityData.define(KIND,0);entityData.define(OWNER,-1);entityData.define(AGE,0);}
    public int kind(){return entityData.get(KIND);}
    public int age(){return entityData.get(AGE);}
    public LivingEntity owner(){Entity e=level.getEntity(entityData.get(OWNER));return e instanceof LivingEntity?(LivingEntity)e:null;}
    public static CerberusEffectEntity spawn(LivingEntity owner,int kind,Vector3d pos,Vector3d velocity){
        CerberusEffectEntity e=new CerberusEffectEntity(CerberusFeatures.EFFECT.get(),owner.level);
        e.caster=owner;e.entityData.set(OWNER,owner.getId());e.entityData.set(KIND,kind);
        e.setPos(pos.x,pos.y,pos.z);e.setDeltaMovement(velocity);owner.level.addFreshEntity(e);return e;
    }
    @Override public void tick(){
        super.tick();
        if(level.isClientSide){if(kind()==0){Vector3d p=position().add(getDeltaMovement());setPos(p.x,p.y,p.z);}return;}
        int age=age()+1;entityData.set(AGE,age);
        if(caster==null)caster=owner();
        if(caster==null || !caster.isAlive() || caster.level!=level || age>(kind()==3?160:kind()==0?70:24)) {remove();return;}
        if(kind()==0){
            Vector3d from=position(),to=from.add(getDeltaMovement());
            if(!level.hasChunkAt(new BlockPos(to)) || distanceToSqr(caster)>160*160){remove();return;}
            BlockRayTraceResult block=level.clip(new RayTraceContext(from,to,RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,this));
            Vector3d end=block.getLocation();boolean hit=block.getType()!=RayTraceResult.Type.MISS;
            double nearest=from.distanceToSqr(end);
            for(LivingEntity target:level.getEntitiesOfClass(LivingEntity.class,getBoundingBox().expandTowards(getDeltaMovement()).inflate(1.5),t->CerberusCombat.enemy(caster,t))){
                Optional<Vector3d> point=target.getBoundingBox().inflate(1.1).clip(from,to);
                if(point.isPresent() && from.distanceToSqr(point.get())<nearest){end=point.get();nearest=from.distanceToSqr(end);hit=true;}
            }
            setPos(end.x,end.y,end.z);
            if(hit){explode();remove();}
        }else if(kind()==3){
            if(!CerberusCombat.active(caster)){remove();return;}
            if(age%4==0){
                Set<UUID> next=new HashSet<>();
                double radius=CerberusTerritory.RADIUS;
                for(LivingEntity target:level.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(radius+1,CerberusTerritory.HEIGHT,radius+1),t->CerberusCombat.enemy(caster,t))){
                    if(CerberusTerritory.contains(target.getX()-getX(),target.getY()-getY(),target.getZ()-getZ())){next.add(target.getUUID());if(!inside.contains(target.getUUID()))target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN,25,1));}
                    else if(inside.contains(target.getUUID()))target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN,25,1));
                }
                inside.clear();inside.addAll(next);
            }
        }
    }
    private void explode(){
        spawn(caster,1,position(),Vector3d.ZERO);
        level.playSound(null,blockPosition(),SoundEvents.GENERIC_EXPLODE,SoundCategory.PLAYERS,1.5F,.8F);
        CerberusAbility ability=CerberusFeatures.CORES.get(CerberusAbility.Move.GATES).createAbility();
        DealDamageComponent damage=new DealDamageComponent(ability);
        for(LivingEntity target:level.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(3),t->CerberusCombat.enemy(caster,t))){
            if(distanceToSqr(target)>9 || !target.canSee(this))continue;
            if(damage.hurtTarget(caster,target,14)) {target.setSecondsOnFire(3);CerberusCombat.mark(caster,target,"GATES",false);}
        }
    }
    @Override protected void readAdditionalSaveData(CompoundNBT nbt){remove();}
    @Override public void remove(){if(caster!=null)CerberusCombat.expired(caster,this);super.remove();}
    @Override protected void addAdditionalSaveData(CompoundNBT nbt){}
    @Override public IPacket<?> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
    @Override public boolean shouldRenderAtSqrDistance(double distance){return distance<192*192;}
    @Override public AxisAlignedBB getBoundingBoxForCulling(){
        return kind()==3?getBoundingBox().inflate(CerberusTerritory.RADIUS,CerberusTerritory.HEIGHT,CerberusTerritory.RADIUS):super.getBoundingBoxForCulling();
    }
}
