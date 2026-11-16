package net.kazi.kazimod.gomu;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

/** Shared aiming geometry. The existing projectile remains the damage-bearing fist. */
public final class GomuLimbs {
    private GomuLimbs() {}

    public static boolean supports(AbilityProjectileEntity p) {
        // Gatling shares its projectile types with Pistol, so classify the individual shot.
        if (((GomuLimbData)p).kazi$gatling()) return false;
        ResourceLocation id = p.getType().getRegistryName();
        if (id == null || !(id.getNamespace().equals("mineminenomi") || id.getNamespace().equals("kazimod"))) return false;
        switch (id.getPath()) {
            case "gomu_gomu_no_pistol": case "gomu_gomu_no_jet_pistol":
            case "gomu_gomu_no_elephant_gun": case "gomu_gomu_no_king_kong_gun":
            case "gomu_gomu_no_bajrang_gun": case "gomu_gomu_no_bajrang_gun_rework":
            case "gomu_gomu_bajrang_gun_rework":
            case "gomu_gomu_no_bazooka": case "gomu_gomu_no_jet_bazooka":
            case "gomu_gomu_no_grizzly_magnum": case "gomu_gomu_no_leo_bazooka":
            case "gomu_gomu_no_rocket": case "gomu_gomu_no_dawn_rocket":
            case "gomu_gomu_no_star_gun":
            case "gomu_gomu_no_red_roc": case "gomu_gomu_red_roc": return true;
            default: return false;
        }
    }

    public static boolean paired(AbilityProjectileEntity p) {
        String name = p.getType().getRegistryName().getPath();
        return name.contains("bazooka") || name.contains("magnum");
    }

    public static Vector3d position(LivingEntity owner, float partial) {
        return new Vector3d(MathHelper.lerp(partial, owner.xo, owner.getX()),
                MathHelper.lerp(partial, owner.yo, owner.getY()), MathHelper.lerp(partial, owner.zo, owner.getZ()));
    }

    public static double scale(LivingEntity owner) {
        return MathHelper.clamp(owner.getBbHeight() / (owner.isCrouching() ? 1.5 : 1.8), .5, 24);
    }

    public static Vector3d right(float yaw) {
        double a = Math.toRadians(yaw);
        return new Vector3d(-Math.cos(a), 0, -Math.sin(a));
    }

    public static Vector3d shoulder(LivingEntity owner, int side, float partial) {
        double size = scale(owner);
        float yaw = owner.yBodyRotO + partial * MathHelper.wrapDegrees(owner.yBodyRot - owner.yBodyRotO);
        return position(owner, partial).add(0, owner.getBbHeight() - .425 * size, 0)
                .add(right(yaw).scale(side * .35 * size));
    }

    public static Vector3d endpoint(AbilityProjectileEntity p, float reach, float partial) {
        LivingEntity owner = p.getThrower();
        GomuLimbData data = (GomuLimbData)p;
        float yaw = owner.yRotO + partial * MathHelper.wrapDegrees(owner.yRot - owner.yRotO);
        float pitch = owner.xRotO + partial * (owner.xRot - owner.xRotO);
        double y = Math.toRadians(yaw), x = Math.toRadians(pitch);
        Vector3d look = new Vector3d(-Math.sin(y) * Math.cos(x), -Math.sin(x), Math.cos(y) * Math.cos(x));
        Vector3d right = right(yaw), up = right.cross(look).normalize();
        double widen = MathHelper.clamp(reach / 6, 0, 1);
        double side = data.kazi$spreadX() + (paired(p) ? data.kazi$arm() * .55 * scale(owner) : 0);
        return shoulder(owner, data.kazi$arm(), partial).add(look.scale(reach))
                .add(right.scale(side * widen)).add(up.scale(data.kazi$spreadY() * widen));
    }

    public static void launch(AbilityProjectileEntity p) {
        if (!supports(p) || p.level.isClientSide || p.getThrower() == null) return;
        float speed = (float)p.getDeltaMovement().length();
        if (!Float.isFinite(speed) || speed < .001F) return;
        LivingEntity owner = p.getThrower();
        GomuLimbData data = (GomuLimbData)p;
        Vector3d offset = p.position().subtract(owner.getEyePosition(1));
        boolean barrage = p.getPersistentData().getBoolean("KaziGomuBarrage") || offset.lengthSqr() > 4 && !paired(p);
        if (data.kazi$arm() == 0) {
            int side = owner.getMainArm() == HandSide.RIGHT ? 1 : -1;
            if (paired(p)) side = offset.dot(right(owner.yRot)) >= 0 ? 1 : -1;
            else if (barrage) side = (p.getId() & 1) == 0 ? 1 : -1;
            data.kazi$arm(side);
        }
        Vector3d look = owner.getLookAngle(), right = right(owner.yRot);
        data.kazi$spread(barrage ? (float)MathHelper.clamp(offset.dot(right), -8, 8) : 0,
                barrage ? (float)MathHelper.clamp(offset.dot(right.cross(look).normalize()), -8, 8) : 0);
        data.kazi$speed(speed);
        data.kazi$reach(0);
        p.getPersistentData().putUUID("KaziGomuOwner", owner.getUUID());
        Vector3d root = shoulder(owner, data.kazi$arm(), 1);
        p.setPos(root.x, root.y, root.z);
        p.xo = p.xOld = root.x; p.yo = p.yOld = root.y; p.zo = p.zOld = root.z;
    }

    public static void steer(AbilityProjectileEntity p) {
        GomuLimbData data = (GomuLimbData)p;
        if (data.kazi$arm() == 0 || p.level.isClientSide) return;
        LivingEntity owner = p.getThrower();
        double maxDistance = Math.max(512, p.getMaxLife() * data.kazi$speed() + 64);
        if (owner == null || !owner.isAlive() || owner.level != p.level
                || owner.position().distanceToSqr(p.position()) > maxDistance * maxDistance) {
            p.remove(); return;
        }
        // A reflected fist belongs to its new caster and must not remain attached to the old one.
        if (!p.getPersistentData().hasUUID("KaziGomuOwner")
                || !owner.getUUID().equals(p.getPersistentData().getUUID("KaziGomuOwner"))) {
            data.kazi$arm(0); return;
        }
        float reach = data.kazi$reach() + data.kazi$speed();
        data.kazi$reach(reach);
        Vector3d target = endpoint(p, reach, 1);
        p.setDeltaMovement(target.subtract(p.position()));
        p.hasImpulse = true;
    }

    public static double fistWidth(AbilityProjectileEntity p) {
        String name = p.getType().getRegistryName().getPath();
        if (name.contains("bajrang")) return name.contains("rework") ? 37.5 : 13.875;
        if (name.contains("red_roc")) return 3.75;
        if (name.contains("elephant") || name.contains("grizzly")) return 2.5;
        if (name.contains("king_kong") || name.contains("leo_")) return 1.25;
        return .25;
    }
}
