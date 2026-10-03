package net.kazi.kazimod.preserved.sahur;

import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.entity.projectile.*;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.kazi.kazimod.abilities.TripelT.TripelTHelper;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;

public final class HeavenlyShield {
    public static final int DURATION = 100;
    public static final float HEAL = 10;
    private static final Map<LivingEntity, Long> ACTIVE = new WeakHashMap<>();
    public static void begin(LivingEntity owner) {
        if (owner.level.isClientSide) return;
        ACTIVE.put(owner, owner.level.getGameTime() + DURATION);
        SahurLightEntity.spawn(owner, 2, owner.position(), DURATION);
    }
    public static boolean active(LivingEntity owner) {
        Long until = ACTIVE.get(owner);
        return until != null && until > owner.level.getGameTime() && owner.isAlive() && TripelTHelper.isGodForm(owner);
    }
    public static void end(LivingEntity owner) { ACTIVE.remove(owner); }
    public static void tick(LivingEntity owner) {
        if (owner.level.isClientSide || !active(owner)) return;
        AxisAlignedBB shield = owner.getBoundingBox().inflate(1.6);
        for (ProjectileEntity p : owner.level.getEntitiesOfClass(ProjectileEntity.class, shield.inflate(32))) {
            if (p.getOwner() == owner || !p.isAlive() || p.getDeltaMovement().lengthSqr() < .0001) continue;
            if (shield.contains(p.position()) || shield.clip(p.position(), p.position().add(p.getDeltaMovement())).isPresent()) reflect(owner, p);
        }
    }
    public static void impact(ProjectileImpactEvent event) {
        if (event.getEntity().level.isClientSide || !(event.getEntity() instanceof ProjectileEntity)
                || !(event.getRayTraceResult() instanceof EntityRayTraceResult)) return;
        Entity victim = ((EntityRayTraceResult) event.getRayTraceResult()).getEntity();
        if (victim instanceof LivingEntity && net.kazi.kazimod.abilities.TripelT.SwingingCounterAbility.reflectCounter(
                (LivingEntity) victim, (ProjectileEntity) event.getEntity())) {
            event.setCanceled(true);
            return;
        }
        if (victim instanceof LivingEntity && active((LivingEntity) victim)) {
            ProjectileEntity p = (ProjectileEntity) event.getEntity();
            if (p.getOwner() != victim && reflect((LivingEntity) victim, p)) event.setCanceled(true);
        }
    }
    public static boolean reflect(LivingEntity owner, ProjectileEntity projectile) {
        return reflect(owner,projectile,true);
    }
    public static boolean reflect(LivingEntity owner, ProjectileEntity projectile, boolean heal) {
        if (owner.level.isClientSide || projectile.getOwner() == owner) return false;
        Entity shooter = projectile.getOwner();
        Vector3d center = owner.position().add(0, owner.getBbHeight() * .55, 0);
        Vector3d direction = shooter != null && shooter.isAlive()
                ? shooter.position().add(0, shooter.getBbHeight() * .55, 0).subtract(center).normalize()
                : projectile.getDeltaMovement().scale(-1).normalize();
        if (direction.lengthSqr() < .001) direction = owner.getLookAngle();
        double speed = Math.max(1, projectile.getDeltaMovement().length());
        Vector3d origin = center.add(direction.scale(Math.max(owner.getBbWidth(), owner.getBbHeight()) * .5 + 1.9));
        if (projectile instanceof LightningEntity) {
            LightningEntity incoming = (LightningEntity) projectile;
            float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
            float pitch = (float) -Math.toDegrees(Math.asin(direction.y));
            LightningEntity reflected = new LightningEntity(owner, origin.x, origin.y, origin.z, yaw, pitch,
                    incoming.getLength(), 24, incoming.getParent());
            reflected.setSize(incoming.getSize()); reflected.setBoxSizeDivision(incoming.getBoxSizeDivision());
            reflected.setColor(new java.awt.Color(incoming.getColor())); reflected.setDamage(incoming.getDamage());
            reflected.setBranches(1); reflected.setSegments(1); reflected.setMaxLife(12);
            reflected.setTargetTimeToReset(9999); reflected.setLightningMovement(false);
            owner.level.addFreshEntity(reflected); incoming.remove();
        } else if (projectile instanceof ShulkerBulletEntity && shooter != null) {
            ShulkerBulletEntity reflected = new ShulkerBulletEntity(owner.level, owner, shooter, Direction.Axis.Y);
            reflected.setPos(origin.x, origin.y, origin.z);
            reflected.setDeltaMovement(direction.scale(speed));
            owner.level.addFreshEntity(reflected);
            projectile.remove();
        } else {
            if (projectile instanceof AbilityProjectileEntity) {
                ((AbilityProjectileEntity) projectile).setThrower(owner);
                ((AbilityProjectileEntity) projectile).clearTargets();
            }
            projectile.setOwner(owner);
            projectile.setPos(origin.x, origin.y, origin.z);
            projectile.setDeltaMovement(direction.scale(speed));
            projectile.yRot = (float) Math.toDegrees(Math.atan2(direction.x, direction.z));
            projectile.xRot = (float) Math.toDegrees(Math.atan2(direction.y, Math.hypot(direction.x, direction.z)));
            projectile.yRotO = projectile.yRot; projectile.xRotO = projectile.xRot;
            if (projectile instanceof DamagingProjectileEntity) {
                DamagingProjectileEntity fireball = (DamagingProjectileEntity) projectile;
                fireball.xPower = direction.x * .1; fireball.yPower = direction.y * .1; fireball.zPower = direction.z * .1;
            }
            projectile.hurtMarked = true;
        }
        if(heal)hit(owner);
        return true;
    }
    public static void hit(LivingEntity owner) {
        if (owner.level.isClientSide) return;
        owner.heal(HEAL);
        owner.level.playSound(null, owner.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundCategory.PLAYERS, .7F, 1.6F);
    }
}
