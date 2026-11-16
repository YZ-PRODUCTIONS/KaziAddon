package net.kazi.kazimod.abilities.SupaRework;

import java.util.Optional;
import net.MrMagicalCart.cartaddon.init.CartEffects;
import net.MrMagicalCart.cartaddon.init.CartParticleEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

/** Completes the borrowed dagger dash even when its strike misses. */
public final class TracedFinalExecution {
    private static final double MAX_DISTANCE = 50.0D;
    private static final float WIDTH = 2.5F;
    private static final double COLLISION_STEP = 0.25D;

    private TracedFinalExecution() {}

    public static void finish(LivingEntity user, IAbility ability, DealDamageComponent damage,
            RangeComponent range, HitTrackerComponent hits, AnimationComponent animation) {
        // The native charge still runs on both sides; only the server moves or damages anyone.
        if (user.level.isClientSide) {
            animation.stop(user);
            return;
        }
        try {
            Vector3d start = user.position();
            Vector3d direction = user.getLookAngle().normalize();
            Vector3d destination = destination(user, direction);
            float distance = (float) destination.distanceTo(start);
            AbilityDamageSource source = (AbilityDamageSource) ((ModDamageSource)
                    damage.getDamageSource(user)).setSlash();
            if (distance > 1.0E-5F) {
                for (LivingEntity target : range.getTargetsInLine(user, distance, WIDTH)) {
                    // The line's width must not let the hit reach through the marble's wall.
                    if (RealityMarbleBoundary.blocksTeleport(user, target.getX(), target.getY(), target.getZ())
                            || !hits.canHit(target)) continue;
                    float missingHealth = target.getMaxHealth() - target.getHealth();
                    float amount = 25.0F + 5.0F + missingHealth * 100.0F / target.getMaxHealth();
                    if (target.isAlive())
                        target.addEffect(new EffectInstance(CartEffects.HEAVYBLEEDING.get(), 200, 0, false, false));
                    if (damage.hurtTarget(user, target, amount, source))
                        WyHelper.spawnParticleEffect(CartParticleEffects.FINAL_EXECUTION.get(), user,
                                target.getX(), target.getEyeY(), target.getZ());
                    // Keep the native per-hit cooldown; the component accepts its first start.
                    ability.getComponent(ModAbilityKeys.COOLDOWN).ifPresent(component ->
                            component.startCooldown(user, 300.0F + amount * 10.0F));
                }
            }
            // Cart's original callback puts these inside its target loop, so a miss never dashes.
            user.stopRiding();
            user.teleportToWithTicket(destination.x, destination.y, destination.z);
            ((ServerWorld) user.level).getChunkSource().broadcastAndSend(user, new SAnimateHandPacket(user, 0));
            user.level.playSound(null, user.blockPosition(), ModSounds.DASH_ABILITY_SWOOSH_SFX.get(),
                    SoundCategory.PLAYERS, 2.0F, 1.0F);
            ability.getComponent(ModAbilityKeys.COOLDOWN).ifPresent(component -> component.startCooldown(user, 600.0F));
        } finally {
            animation.stop(user);
        }
    }

    private static Vector3d destination(LivingEntity user, Vector3d direction) {
        Vector3d start = user.position();
        Vector3d eye = user.getEyePosition(1.0F);
        Vector3d end = eye.add(direction.scale(MAX_DISTANCE));
        BlockRayTraceResult block = user.level.clip(new RayTraceContext(eye, end,
                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, user));
        double distance = block.getType() == RayTraceResult.Type.BLOCK
                ? Math.max(0.0D, eye.distanceTo(block.getLocation()) - 0.05D) : MAX_DISTANCE;
        end = eye.add(direction.scale(distance));
        AxisAlignedBB corridor = new AxisAlignedBB(eye, end).inflate(user.getBbWidth(), user.getBbHeight(), user.getBbWidth());
        for (ProjectileEntity projectile : user.level.getEntitiesOfClass(ProjectileEntity.class, corridor)) {
            if (!projectile.isAlive() || isFriendly(user, projectile)) continue;
            AxisAlignedBB box = projectile.getBoundingBox().inflate(user.getBbWidth() * 0.5D,
                    user.getBbHeight() * 0.5D, user.getBbWidth() * 0.5D);
            if (box.contains(eye)) {
                distance = 0.0D;
                break;
            }
            Optional<Vector3d> hit = box.clip(eye, end);
            if (hit.isPresent()) distance = Math.min(distance, eye.distanceTo(hit.get()));
        }
        Vector3d bounded = RealityMarbleBoundary.clampTeleportDestination(user,
                start.add(direction.scale(distance)));
        distance = Math.min(distance, bounded.distanceTo(start));
        // Test the whole hitbox along the route, not just an eye ray or the final block.
        // At most 200 checks for the native fifty-block dash.
        double safeDistance = 0.0D;
        int steps = (int) Math.ceil(distance / COLLISION_STEP);
        for (int step = 1; step <= steps; step++) {
            double next = Math.min(distance, step * COLLISION_STEP);
            if (!user.level.noCollision(user, user.getBoundingBox().move(direction.scale(next)))) break;
            safeDistance = next;
        }
        return start.add(direction.scale(safeDistance));
    }

    private static boolean isFriendly(LivingEntity user, ProjectileEntity projectile) {
        Entity owner = projectile instanceof AbilityProjectileEntity
                ? ((AbilityProjectileEntity) projectile).getThrower() : null;
        if (owner == null) owner = projectile.getOwner();
        return owner != null && (owner == user || user.isAlliedTo(owner) || owner.isAlliedTo(user)
                || (owner instanceof LivingEntity && !ModEntityPredicates.getEnemyFactions(user).test(owner)));
    }
}
