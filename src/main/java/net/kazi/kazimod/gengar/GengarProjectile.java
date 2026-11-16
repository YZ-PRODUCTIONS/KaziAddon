package net.kazi.kazimod.gengar;
import net.minecraft.entity.*;
import net.minecraft.network.datasync.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
public final class GengarProjectile extends AbilityProjectileEntity {
    private static final DataParameter<Integer> KIND=EntityDataManager.defineId(GengarProjectile.class,DataSerializers.INT),TARGET=EntityDataManager.defineId(GengarProjectile.class,DataSerializers.INT);
    private static final DataParameter<Float> POWER=EntityDataManager.defineId(GengarProjectile.class,DataSerializers.FLOAT);
    private boolean spent;
    public GengarProjectile(EntityType<?> type,World world){super(type,world);}
    public GengarProjectile(LivingEntity user,AbilityCore<?> core,int kind,float power){
        super(GengarFeatures.PROJECTILE.get(),user.level,user,core);entityData.set(KIND,kind);entityData.set(POWER,power);
        setDamage(power);setGravity(0);setMaxLife(kind==0?70:60);setAffectedByImbuing();setProjectile();setEntityCollisionSize(kind==0?.65:.35);setBlockCollisionSize(.25);
    }
    @Override public void defineSynchedData(){super.defineSynchedData();entityData.define(KIND,0);entityData.define(TARGET,-1);entityData.define(POWER,20F);}
    public int kind(){return entityData.get(KIND);}public float size(){return kind()==0?.4F+.5F*Math.max(0,Math.min(1,(entityData.get(POWER)-20)/25)):.45F;}
    public void target(LivingEntity target){entityData.set(TARGET,target.getId());}
    @Override public void tick(){
        if(!level.isClientSide){LivingEntity user=getThrower();if(user==null||!user.isAlive()||!GengarFeatures.active(user)){remove();return;}
            Entity target=level.getEntity(entityData.get(TARGET));if(kind()==1&&target instanceof LivingEntity&&GengarFeatures.canHit(user,(LivingEntity)target)&&distanceToSqr(target)<32*32){
                Vector3d toward=target.getBoundingBox().getCenter().subtract(position()).normalize().scale(.8);setDeltaMovement(getDeltaMovement().scale(.8).add(toward.scale(.2)));
            }
        }super.tick();
    }
    @Override protected void onHitBlock(BlockRayTraceResult hit){onModHit(hit);}
    @Override public void onModHit(RayTraceResult hit){
        if(level.isClientSide||spent||!isAlive())return;
        Entity entity=hit instanceof EntityRayTraceResult?((EntityRayTraceResult)hit).getEntity():null;
        if(entity instanceof AbilityProjectileEntity){super.onModHit(hit);return;}
        LivingEntity user=getThrower();if(user==null){remove();return;}
        if(entity instanceof LivingEntity&&!GengarFeatures.canHit(user,(LivingEntity)entity))return;
        spent=true;
        if(kind()==1){if(entity instanceof LivingEntity){LivingEntity victim=(LivingEntity)entity;if(victim.hurt(getDamageSource(),getDamage()))GengarFeatures.haunt(user,victim);}}
        else{
            float radius=2+size()*2;
            for(LivingEntity victim:level.getEntitiesOfClass(LivingEntity.class,new AxisAlignedBB(hit.getLocation(),hit.getLocation()).inflate(radius))){
                if(!GengarFeatures.canHit(user,victim)||victim.position().distanceToSqr(hit.getLocation())>radius*radius)continue;
                Vector3d start=hit.getLocation().subtract(getDeltaMovement().normalize().scale(.2));
                if(level.clip(new RayTraceContext(start,victim.getBoundingBox().getCenter(),RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,this)).getType()==RayTraceResult.Type.BLOCK)continue;
                victim.hurt(getDamageSource(),getDamage());
            }
        }
        GengarEffect.spawn(user,kind()==0?GengarEffect.BLAST:GengarEffect.HAUNT,hit.getLocation(),24,kind()==0?2+size()*2:1);remove();
    }
    @Override public AxisAlignedBB getBoundingBoxForCulling(){return super.getBoundingBoxForCulling().inflate(3);}
}
