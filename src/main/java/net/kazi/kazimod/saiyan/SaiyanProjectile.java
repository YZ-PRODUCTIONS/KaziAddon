package net.kazi.kazimod.saiyan;

import net.minecraft.entity.*;
import net.minecraft.network.datasync.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

public final class SaiyanProjectile extends AbilityProjectileEntity {
    private static final DataParameter<Integer> KIND=EntityDataManager.defineId(SaiyanProjectile.class,DataSerializers.INT);
    private static final DataParameter<Float> SIZE=EntityDataManager.defineId(SaiyanProjectile.class,DataSerializers.FLOAT);
    private static final DataParameter<Integer> FORM=EntityDataManager.defineId(SaiyanProjectile.class,DataSerializers.INT);
    private boolean detonated;
    public SaiyanProjectile(EntityType<?> type,World world){super(type,world);}
    public SaiyanProjectile(LivingEntity user,AbilityCore<?> core,int kind,float damage,float size){
        super(SaiyanFeatures.PROJECTILE.get(),user.level,user,core);
        entityData.set(KIND,kind);entityData.set(SIZE,size);entityData.set(FORM,SaiyanState.form(user));
        setDamage(damage);setGravity(0);setMaxLife(kind==2?220:90);setAffectedByImbuing();setProjectile();
        setEntityCollisionSize(size);setBlockCollisionSize(kind==2?size*2:size*.5);
        if(kind==2)setCollideWithBlocks(true);
    }
    @Override public void defineSynchedData(){super.defineSynchedData();entityData.define(KIND,0);entityData.define(SIZE,.32F);entityData.define(FORM,0);}
    public int kind(){return entityData.get(KIND);}
    public float size(){return entityData.get(SIZE);}
    public int form(){return SaiyanBalance.tier(entityData.get(FORM));}
    @Override protected void onHitBlock(BlockRayTraceResult hit){
        // Vanilla terrain impacts must reach the same detonation as entity hits.
        if(kind()==2)onModHit(hit);
        else super.onHitBlock(hit);
    }
    @Override public void onModHit(RayTraceResult result){
        if(level.isClientSide||detonated||!isAlive())return;
        if(result instanceof EntityRayTraceResult&&((EntityRayTraceResult)result).getEntity() instanceof AbilityProjectileEntity){super.onModHit(result);return;}
        if(result instanceof EntityRayTraceResult&&((EntityRayTraceResult)result).getEntity() instanceof LivingEntity
                &&getThrower()!=null&&!SaiyanAbility.canHit(getThrower(),(LivingEntity)((EntityRayTraceResult)result).getEntity()))return;
        detonated=true;
        if(kind()==2){
            setPos(result.getLocation().x,result.getLocation().y,result.getLocation().z);
            SaiyanEffect.spawn(level,getThrower(),SaiyanEffect.SPIRIT_IMPACT,position(),100,54).withForm(form());
            net.kazi.kazimod.entities.projectiles.HollowNukeExplosion.applyImpact(this);
            remove();
            return;
        }
        float radius=kind()==1?4:1.3F;
        LivingEntity owner=getThrower();
        if(owner!=null){
            Entity direct=result instanceof EntityRayTraceResult?((EntityRayTraceResult)result).getEntity():null;
            if(direct instanceof LivingEntity&&SaiyanAbility.canHit(owner,(LivingEntity)direct))direct.hurt(getDamageSource(),getDamage());
            for(LivingEntity target:level.getEntitiesOfClass(LivingEntity.class,new AxisAlignedBB(result.getLocation(),result.getLocation()).inflate(radius))){
                if(target==direct||!SaiyanAbility.canHit(owner,target)||target.distanceToSqr(this)>radius*radius)continue;
                if(level.clip(new RayTraceContext(result.getLocation(),target.getEyePosition(1),RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,this)).getType()==RayTraceResult.Type.BLOCK)continue;
                float falloff=Math.max(.25F,1-(float)target.distanceTo(this)/radius*.75F);
                target.hurt(getDamageSource(),getDamage()*falloff);
                target.knockback(.5F,getX()-target.getX(),getZ()-target.getZ());
            }
            SaiyanEffect.spawn(owner,4,result.getLocation(),14,radius).withForm(form());
        }
        remove();
    }
    @Override public AxisAlignedBB getBoundingBoxForCulling(){return super.getBoundingBoxForCulling().inflate(size()+5);}
}
