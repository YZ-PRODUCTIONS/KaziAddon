package net.kazi.kazimod.yami;

import java.util.Map;
import java.util.WeakHashMap;
import net.kazi.kazimod.mahoraga.MahoragaFeatures;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.api.events.ProjectileHitEvent;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;

@Mod.EventBusSubscriber(modid="kazimod")
public final class YamiCombat {
    private static final Map<LivingEntity,Long> KUROZU=new WeakHashMap<>();
    private static final Map<LivingEntity,Long> STUNNED=new WeakHashMap<>();
    public static void stun(LivingEntity target) {
        target.addEffect(new net.minecraft.potion.EffectInstance(xyz.pixelatedw.mineminenomi.init.ModEffects.MOVEMENT_BLOCKED.get(),6,0,false,false));
        if(target.hasEffect(xyz.pixelatedw.mineminenomi.init.ModEffects.MOVEMENT_BLOCKED.get()))
            STUNNED.put(target,target.level.getGameTime()+6);
    }
    @SubscribeEvent public static void stunnedAttack(LivingAttackEvent event) {
        Entity attacker=event.getSource().getDirectEntity();
        if(attacker instanceof LivingEntity&&!attacker.level.isClientSide) {
            Long until=STUNNED.get(attacker);
            if(until!=null&&until>attacker.level.getGameTime()
                    &&((LivingEntity)attacker).hasEffect(xyz.pixelatedw.mineminenomi.init.ModEffects.MOVEMENT_BLOCKED.get()))event.setCanceled(true);
        }
    }
    public static boolean enemy(LivingEntity owner,LivingEntity target) {
        return target.isAlive()&&!target.isSpectator()
                &&!(target instanceof PlayerEntity&&((PlayerEntity)target).isCreative())
                &&!MahoragaFeatures.allied(owner,target)&&ModEntityPredicates.getEnemyFactions(owner).test(target);
    }
    public static void beginKurozu(LivingEntity owner) {
        if(!owner.level.isClientSide)KUROZU.put(owner,owner.level.getGameTime()+3);
    }
    public static void endKurozu(LivingEntity owner){KUROZU.remove(owner);}
    public static boolean canAbsorb(LivingEntity owner,ProjectileEntity projectile) {
        Entity shooter=projectile.getOwner();
        return projectile.isAlive()&&(shooter==null||!MahoragaFeatures.allied(owner,shooter))
                &&projectileDamage(projectile)<40;
    }
    public static double projectileDamage(ProjectileEntity projectile) {
        if(projectile instanceof AbilityProjectileEntity) {
            AbilityProjectileEntity ability=(AbilityProjectileEntity)projectile;
            double damage=ability.getDamage();
            LivingEntity shooter=ability.getThrower();
            if(shooter!=null)damage*=xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability.get(shooter).getDamageMultiplier();
            return damage>=0&&Double.isFinite(damage)?damage:Double.POSITIVE_INFINITY;
        }
        if(projectile instanceof TridentEntity)return 8;
        if(projectile instanceof AbstractArrowEntity) {
            AbstractArrowEntity arrow=(AbstractArrowEntity)projectile;
            double damage=Math.ceil(arrow.getBaseDamage()*arrow.getDeltaMovement().length());
            return arrow.isCritArrow()?damage+Math.floor(damage/2)+1:damage;
        }
        if(projectile instanceof SmallFireballEntity)return 5;
        if(projectile instanceof FireballEntity)return 14*((FireballEntity)projectile).explosionPower+1;
        if(projectile instanceof DragonFireballEntity)return 6;
        if(projectile instanceof WitherSkullEntity)return 8;
        if(projectile instanceof ShulkerBulletEntity)return 4;
        if(projectile instanceof LlamaSpitEntity)return 1;
        if(projectile instanceof SnowballEntity)return 3;
        if(projectile instanceof EggEntity||projectile instanceof net.minecraft.entity.item.ExperienceBottleEntity)return 0;
        if(projectile instanceof PotionEntity)return 12;
        // Unknown mod projectiles are not assumed to be weak.
        return Double.POSITIVE_INFINITY;
    }
    public static Vector3d hand(LivingEntity owner){return owner.getEyePosition(1).add(owner.getLookAngle().scale(.9));}
    public static void pullProjectiles(LivingEntity owner) {
        if(owner.level.isClientSide)return;
        beginKurozu(owner);
        Vector3d center=hand(owner),forward=owner.getLookAngle();
        Vector3d end=center.add(forward.scale(24));
        AxisAlignedBB mouth=owner.getBoundingBox().inflate(1.1);
        for(ProjectileEntity projectile:owner.level.getEntitiesOfClass(ProjectileEntity.class,new AxisAlignedBB(center,end).inflate(5),p->canAbsorb(owner,p))) {
            Vector3d offset=projectile.position().subtract(center);
            double along=offset.dot(forward),side=offset.lengthSqr()-along*along;
            if(offset.lengthSqr()>9&&(along<0||along>24||side>25))continue;
            if(mouth.contains(projectile.position())||mouth.clip(projectile.position(),projectile.position().add(projectile.getDeltaMovement())).isPresent()) {
                projectile.remove();continue;
            }
            Vector3d velocity=projectile.getDeltaMovement().scale(.45).add(offset.normalize().scale(-1.1));
            if(velocity.lengthSqr()>6.25)velocity=velocity.normalize().scale(2.5);
            projectile.setDeltaMovement(velocity);projectile.hasImpulse=true;projectile.hurtMarked=true;
            if(projectile instanceof DamagingProjectileEntity) {
                DamagingProjectileEntity fireball=(DamagingProjectileEntity)projectile;
                Vector3d force=offset.normalize().scale(-.1);
                fireball.xPower=force.x;fireball.yPower=force.y;fireball.zPower=force.z;
            }
        }
    }
    private static boolean intercept(ProjectileEntity projectile,RayTraceResult hit) {
        if(projectile.level.isClientSide||!(hit instanceof EntityRayTraceResult))return false;
        Entity entity=((EntityRayTraceResult)hit).getEntity();
        if(entity instanceof net.minecraftforge.entity.PartEntity)entity=((net.minecraftforge.entity.PartEntity<?>)entity).getParent();
        if(!(entity instanceof LivingEntity))return false;
        LivingEntity owner=(LivingEntity)entity;Long until=KUROZU.get(owner);
        if(until==null||until<owner.level.getGameTime()||!owner.isAlive()||!canAbsorb(owner,projectile))return false;
        projectile.remove();return true;
    }
    @SubscribeEvent public static void vanillaImpact(ProjectileImpactEvent event) {
        if(event.getEntity() instanceof ProjectileEntity&&intercept((ProjectileEntity)event.getEntity(),event.getRayTraceResult()))event.setCanceled(true);
    }
    @SubscribeEvent public static void abilityImpact(ProjectileHitEvent event) {
        if(intercept(event.getProjectile(),event.getHit()))event.setCanceled(true);
    }
    public static Vector3d portal(LivingEntity owner) {
        Vector3d start=owner.getEyePosition(1),look=owner.getLookAngle(),end=start.add(look.scale(3));
        BlockRayTraceResult hit=owner.level.clip(new RayTraceContext(start,end,RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,owner));
        return hit.getType()==RayTraceResult.Type.BLOCK?hit.getLocation().subtract(look.scale(.35)):end;
    }
}
