package net.kazi.kazimod.hermano;

import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.datasync.*;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.damagesource.*;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;

public final class HermanoProjectile extends AbilityProjectileEntity {
    private static final DataParameter<Boolean> POWER=EntityDataManager.defineId(HermanoProjectile.class,DataSerializers.BOOLEAN);
    private boolean spent;
    public HermanoProjectile(EntityType<?> type,World world){super(type,world);}
    public HermanoProjectile(HermanoEntity boss,boolean full){
        super(HermanoFeatures.IMPACT.get(),boss.level,boss,SourceElement.SHOCKWAVE,SourceHakiNature.SPECIAL,SourceType.PROJECTILE);
        entityData.set(POWER,full);ModDamageSource source=boss.source("power_impact",true).setProjectile();
        setDamage(boss.infused(source,full?42:32));setDamageSource(source);setGravity(0);setProjectile();setMaxLife(85);
        setEntityCollisionSize(full?1.15:.8);setBlockCollisionSize(.6);
    }
    @Override public void defineSynchedData(){super.defineSynchedData();entityData.define(POWER,false);}
    public float size(){return entityData.get(POWER)?1.2F:.85F;}
    @Override protected void onHitBlock(BlockRayTraceResult hit){onModHit(hit);}
    @Override public void onModHit(RayTraceResult hit){
        if(level.isClientSide||spent||!isAlive())return;
        Entity victim=hit instanceof EntityRayTraceResult?((EntityRayTraceResult)hit).getEntity():null;
        if(victim instanceof AbilityProjectileEntity){super.onModHit(hit);return;}
        LivingEntity owner=getThrower();if(owner==null){remove();return;}
        if(victim==owner||victim!=null&&owner.isAlliedTo(victim))return;
        spent=true;float radius=entityData.get(POWER)?7:5;
        Vector3d at=hit.getLocation().subtract(getDeltaMovement().normalize().scale(.25));
        ModDamageSource source=new ModDamageSource("el_hermano_power_impact").causeEntityDamageFromSource(owner);
        source.setSourceElement(SourceElement.SHOCKWAVE).setProjectile().setInternal().bypassLogia().setHakiNature(SourceHakiNature.SPECIAL).setAreaOfEffect();
        for(LivingEntity target:level.getEntitiesOfClass(LivingEntity.class,new AxisAlignedBB(at,at).inflate(radius))){
            if(target==owner||!target.isAlive()||owner.isAlliedTo(target)||target instanceof PlayerEntity&&(((PlayerEntity)target).isCreative()||target.isSpectator()))continue;
            double distance=target.getBoundingBox().getCenter().distanceTo(at);if(distance>radius)continue;
            if(level.clip(new RayTraceContext(at,target.getBoundingBox().getCenter(),RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,this)).getType()!=RayTraceResult.Type.MISS)continue;
            if(target.hurt(source,getDamage()*(float)(1-.4*distance/radius))){Vector3d d=target.position().subtract(at);target.knockback(2,-d.x,-d.z);}
        }
        HermanoEffect.spawn(owner,HermanoEffect.BLAST,at,26,radius);playSound(SoundEvents.GENERIC_EXPLODE,1.3F,.7F);remove();
    }
    @Override public AxisAlignedBB getBoundingBoxForCulling(){return super.getBoundingBoxForCulling().inflate(3);}
}
