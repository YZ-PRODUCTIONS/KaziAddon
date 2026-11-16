package net.kazi.kazimod.mixin;

import net.kazi.kazimod.abilities.NagiAwakening.ImagineBreakerNegation;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;

/** Check the passive along native fruit rays before their damage or explosions. */
@Mixin(value = LightningEntity.class, remap = false)
public abstract class ImagineBreakerPassiveBeamMixin {
    @Shadow(remap = false) private float maxTravelDistance;
    @Shadow(remap = false) private float travelSpeed;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = true)
    private void kazi$negateFruitBeam(CallbackInfo ci) {
        LightningEntity beam = (LightningEntity) (Object) this;
        if (beam.level.isClientSide || !beam.isAlive() || beam.getMimicVanilla()) return;
        float reach = Math.min(maxTravelDistance, beam.getLength() + travelSpeed);
        double radius = .2D + beam.getSize() / beam.getBoxSizeDivision();
        if (!Float.isFinite(reach) || reach <= 0 || !Double.isFinite(radius) || radius < 0) return;
        Vector3d start = new Vector3d(beam.getX(), beam.getEyeY(), beam.getZ());
        Vector3d direction = beam.getLookAngle().normalize();
        // The native tick floors each ray sample on all axes. Include that
        // displacement and the true collision width in the broad query.
        AxisAlignedBB bounds = new AxisAlignedBB(start, start.add(direction.scale(reach))).inflate(radius + 1.0D);
        Entity owner = beam.getThrower();
        if (owner == null) owner = beam.getOwner();
        if (ImagineBreakerNegation.negateBeam(beam, owner, beam.getParent(), bounds,
                eye -> kazi$closestPoint(eye, start, direction, reach, radius))) {
            // Removing the entity alone would not stop this tick's remaining effects.
            ci.cancel();
        }
    }

    @Unique
    private static Vector3d kazi$closestPoint(Vector3d eye, Vector3d start, Vector3d direction,
                                             float reach, double radius) {
        Vector3d nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (int along = 0; along < reach; along++) {
            double x = Math.floor(start.x + direction.x * along);
            double y = Math.floor(start.y + direction.y * along);
            double z = Math.floor(start.z + direction.z * along);
            Vector3d point = new Vector3d(
                    Math.max(x - radius, Math.min(x + radius, eye.x)),
                    Math.max(y - radius, Math.min(y + radius, eye.y)),
                    Math.max(z - radius, Math.min(z + radius, eye.z)));
            double distance = point.distanceToSqr(eye);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = point;
                if (distance == 0) break;
            }
        }
        return nearest;
    }
}
