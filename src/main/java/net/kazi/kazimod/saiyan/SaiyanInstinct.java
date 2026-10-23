package net.kazi.kazimod.saiyan;

import net.minecraft.entity.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.world.ProtectedAreasData;
import xyz.pixelatedw.mineminenomi.api.protection.ProtectedArea;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

/** Uses the same pre-damage component stage as Future Sight and Kami-E. */
public final class SaiyanInstinct {
    private SaiyanInstinct() {}

    public static float onAttack(LivingEntity user,DamageSource source,float amount){
        if(!(user instanceof ServerPlayerEntity)||!SaiyanBalance.canDodge(amount)
                ||SaiyanState.form(user)!=SaiyanBalance.ULTRA_INSTINCT||!SaiyanState.hasFruit(user)
                ||!user.isAlive()||user.hasEffect(ModEffects.ABILITY_OFF.get())
                ||AbilityHelper.isWeakenedByKairosekiOrWater(user)||!AbilityHelper.canUseMomentumAbilities(user)
                ||AbilityHelper.isGrabbing(user)||user.isPassenger())return amount;
        if(source instanceof ModDamageSource&&((ModDamageSource)source).isUnavoidable()
                ||source instanceof EntityDamageSource&&((EntityDamageSource)source).isThorns())return amount;
        Entity attacker=source.getEntity();
        if(!(attacker instanceof LivingEntity)&&source.getDirectEntity() instanceof ProjectileEntity)
            attacker=((ProjectileEntity)source.getDirectEntity()).getOwner();
        // Environmental damage is not an attack and has nobody to dodge behind.
        if(!(attacker instanceof LivingEntity)||attacker==user||!attacker.isAlive()||attacker.level!=user.level)return amount;
        long now=user.level.getGameTime();
        if(now>=SaiyanState.get(user).getLong("InstinctNextEffect")){
            SaiyanState.get(user).putLong("InstinctNextEffect",now+4);
            SaiyanEffect.spawn(user,SaiyanEffect.INSTINCT_DODGE,user.position(),9,1);
            Vector3d safe=findBehind((ServerPlayerEntity)user,(LivingEntity)attacker);
            if(safe!=null){
                float yaw=(float)(Math.atan2(attacker.getZ()-safe.z,attacker.getX()-safe.x)*180/Math.PI)-90;
                ((ServerPlayerEntity)user).connection.teleport(safe.x,safe.y,safe.z,yaw,user.xRot);
                user.setDeltaMovement(Vector3d.ZERO);user.fallDistance=0;
                SaiyanEffect.spawn(user,SaiyanEffect.INSTINCT_DODGE,user.position(),9,1);
            }
            SaiyanFeatures.play(user,user.getRandom().nextBoolean()?ModSounds.DODGE_1.get():ModSounds.DODGE_2.get(),.65F,1.2F);
        }
        // No general invulnerability window: a simultaneous attack of 30+ damage still lands.
        return 0;
    }

    private static Vector3d findBehind(ServerPlayerEntity user,LivingEntity attacker){
        if(user.distanceToSqr(attacker)>96*96||!canTeleport(user,user.blockPosition()))return null;
        Vector3d forward=attacker.getLookAngle().multiply(1,0,1).normalize();
        if(forward.lengthSqr()<.01)forward=Vector3d.directionFromRotation(0,attacker.yRot);
        Vector3d right=new Vector3d(forward.z,0,-forward.x);
        double distance=(attacker.getBbWidth()+user.getBbWidth())*.5+1;
        for(double side:new double[]{0,.8,-.8,1.5,-1.5})for(int up=0;up<=2;up++){
            Vector3d candidate=attacker.position().subtract(forward.scale(distance)).add(right.scale(side)).add(0,up,0);
            AxisAlignedBB box=user.getBoundingBox().move(candidate.subtract(user.position()));
            BlockPos low=new BlockPos(box.minX,box.minY,box.minZ),high=new BlockPos(box.maxX,box.maxY,box.maxZ);
            if(candidate.y<=0||box.maxY>=user.level.getMaxBuildHeight()
                    ||!user.level.hasChunkAt(low)||!user.level.hasChunkAt(high)
                    ||!user.level.getWorldBorder().isWithinBounds(low)||!user.level.getWorldBorder().isWithinBounds(high)
                    ||!canTeleport(user,low)||!canTeleport(user,high))continue;
            if(user.level.noCollision(user,box)&&!user.level.containsAnyLiquid(box))return candidate;
        }
        return null;
    }

    private static boolean canTeleport(LivingEntity user,BlockPos pos){
        ProtectedArea area=ProtectedAreasData.get(user.level).getProtectedArea(pos.getX(),pos.getY(),pos.getZ());
        return area==null||area.canHurtEntities();
    }
}
