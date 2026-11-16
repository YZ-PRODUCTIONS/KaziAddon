package local.autointentdodge;

import java.util.Optional;
import net.Zitoen.zitoenpiece.abilities.haki.kenbunshoku.IntentSenseHandler;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;

final class AttackPrediction {
    private AttackPrediction() {}

    static boolean hasThreat(ClientPlayerEntity player) {
        if (player.level == null) {
            return false;
        }
        int ticks = AutoIntentDodge.LOOKAHEAD.get();
        if (AutoIntentDodge.PROJECTILES.get()) {
            for (ProjectileEntity projectile : player.level.getEntitiesOfClass(ProjectileEntity.class,
                    player.getBoundingBox().inflate(32))) {
                if (projectileThreat(player, projectile, ticks)) {
                    return true;
                }
            }
        }
        if (AutoIntentDodge.CHARGED_ATTACKS.get()) {
            for (LivingEntity attacker : player.level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(6))) {
                if (chargedThreat(player, attacker, ticks)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean friendly(PlayerEntity player, Entity attacker) {
        return attacker != null && (attacker == player || player.isAlliedTo(attacker)
                || (attacker instanceof PlayerEntity && !((PlayerEntity) attacker).canHarmPlayer(player)));
    }

    static boolean projectileThreat(ClientPlayerEntity player, ProjectileEntity projectile, int ticks) {
        if (!projectile.isAlive() || friendly(player, projectile.getOwner())) {
            return false;
        }
        AxisAlignedBB size;
        double gravity;
        double drag;
        if (projectile instanceof AbilityProjectileEntity) {
            AbilityProjectileEntity ability = (AbilityProjectileEntity) projectile;
            DamageSource source = ability.getDamageSource();
            if (ability.isStuckInGround() || (source != null && !IntentSenseHandler.canDodgeDamageSource(source))) {
                return false;
            }
            size = ability.getEntityCollisionBox();
            // Custom projectile gravity is not exposed or consistently synced. Use a short linear forecast.
            gravity = 0;
            drag = 1;
        } else if (projectile instanceof AbstractArrowEntity) {
            size = projectile.getBoundingBox();
            gravity = projectile.isNoGravity() ? 0 : 0.05;
            drag = projectile.isInWater() ? 0.6 : 0.99;
        } else {
            return false;
        }
        Vector3d velocity = projectile.getDeltaMovement();
        if (velocity.lengthSqr() < 0.0001) {
            return false;
        }
        AxisAlignedBB target = player.getBoundingBox().inflate(
                size.getXsize() * 0.5 + 0.1, size.getYsize() * 0.5 + 0.1, size.getZsize() * 0.5 + 0.1);
        Vector3d start = projectile.position();
        Vector3d playerVelocity = player.getDeltaMovement();
        for (int tick = 0; tick < ticks; tick++) {
            Vector3d end = start.add(velocity);
            Vector3d relativeStart = start.subtract(playerVelocity.scale(tick));
            Vector3d relativeEnd = end.subtract(playerVelocity.scale(tick + 1));
            Optional<Vector3d> hit = target.clip(relativeStart, relativeEnd);
            if (target.contains(relativeStart) || hit.isPresent()) {
                double distance = relativeStart.distanceTo(relativeEnd);
                double fraction = target.contains(relativeStart) || distance < 0.0001
                        ? 0 : relativeStart.distanceTo(hit.get()) / distance;
                return clearPath(player, projectile, start, start.add(velocity.scale(fraction)));
            }
            if (!clearPath(player, projectile, start, end)) {
                return false;
            }
            start = end;
            velocity = velocity.scale(drag).add(0, -gravity, 0);
        }
        return false;
    }

    private static boolean clearPath(ClientPlayerEntity player, Entity source, Vector3d start, Vector3d end) {
        return player.level.clip(new RayTraceContext(start, end, RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.NONE, source)).getType() == RayTraceResult.Type.MISS;
    }

    static boolean chargedThreat(ClientPlayerEntity player, LivingEntity attacker, int ticks) {
        if (!attacker.isAlive() || attacker.isSpectator() || friendly(player, attacker)
                || attacker.distanceToSqr(player) > 36 || !player.canSee(attacker)) {
            return false;
        }
        Vector3d eye = attacker.getEyePosition(1);
        AxisAlignedBB target = player.getBoundingBox().inflate(0.35);
        if (!target.contains(eye) && !target.clip(eye, eye.add(attacker.getLookAngle().scale(6))).isPresent()) {
            return false;
        }
        IAbilityData abilities = AbilityDataCapability.get(attacker);
        if (abilities == null) {
            return false;
        }
        for (IAbility ability : abilities.getEquippedAbilities()) {
            Optional<ChargeComponent> charge = ability.getComponent(ModAbilityKeys.CHARGE);
            Optional<DealDamageComponent> damage = ability.getComponent(ModAbilityKeys.DAMAGE);
            if (!charge.isPresent() || !damage.isPresent() || !charge.get().isCharging()) {
                continue;
            }
            float remaining = charge.get().getMaxChargeTime() - charge.get().getChargeTime();
            if (remaining > 0 && remaining <= ticks) {
                DamageSource source = damage.get().getDamageSource(attacker);
                if (source != null && IntentSenseHandler.canDodgeDamageSource(source)) {
                    return true;
                }
            }
        }
        return false;
    }
}
