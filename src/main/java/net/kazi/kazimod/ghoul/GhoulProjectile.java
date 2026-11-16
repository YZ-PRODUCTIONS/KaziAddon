package net.kazi.kazimod.ghoul;
import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.network.datasync.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
public final class GhoulProjectile extends AbilityProjectileEntity {
    private static final DataParameter<Boolean> PIERCING=EntityDataManager.defineId(GhoulProjectile.class,DataSerializers.BOOLEAN);
    private final Set<UUID> hit=new HashSet<>();
    public GhoulProjectile(EntityType<?> type,World world){super(type,world);}
    public GhoulProjectile(LivingEntity p,AbilityCore<?> c,float damage,boolean piercing){super(GhoulFeatures.SHARD.get(),p.level,p,c);entityData.set(PIERCING,piercing);setDamage(damage);setGravity(0);setMaxLife(35);setAffectedByImbuing();setProjectile();setEntityCollisionSize(.25);setBlockCollisionSize(.12);if(piercing)setPassThroughEntities();}
    @Override public void defineSynchedData(){super.defineSynchedData();entityData.define(PIERCING,false);}
    public boolean piercing(){return entityData.get(PIERCING);}
    @Override protected void onHitBlock(BlockRayTraceResult r){onModHit(r);}
    @Override public void onModHit(RayTraceResult r){
        if(level.isClientSide)return;
        if(r instanceof EntityRayTraceResult){Entity e=((EntityRayTraceResult)r).getEntity();if(e instanceof AbilityProjectileEntity){super.onModHit(r);return;}
            if(e instanceof LivingEntity&&getThrower()!=null&&GhoulFeatures.canHit(getThrower(),(LivingEntity)e)&&hit.add(e.getUUID())){e.hurt(getDamageSource(),getDamage());GhoulFeatures.effect(getThrower(),0,e.getBoundingBox().getCenter(),piercing()?1.3F:.6F);if(!piercing()||hit.size()>=4)remove();}
        }else if(r instanceof BlockRayTraceResult){if(getThrower()!=null)GhoulFeatures.effect(getThrower(),0,r.getLocation(),.5F);remove();}
    }
}
