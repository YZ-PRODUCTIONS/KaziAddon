package net.kazi.kazimod.projectiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.packets.server.entities.SPinCameraPacket;
import xyz.pixelatedw.mineminenomi.packets.server.entities.SUnpinCameraPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

@Mod.EventBusSubscriber(modid = "kazimod")
public final class ProjectileClashes {
    private static final Map<UUID, Long> PINNED = new HashMap<>();

    private ProjectileClashes() {}

    public static boolean trade(AbilityProjectileEntity first, AbilityProjectileEntity second) {
        if (first == second || first.level.isClientSide) return true;
        if (!first.isAlive() || !second.isAlive() || first.getOwner() == second.getOwner()) return true;
        if (first instanceof LightningEntity || second instanceof LightningEntity) return true;
        if (first.getDamage() <= 0 || second.getDamage() <= 0) return false;

        ClashState left = (ClashState) first;
        ClashState right = (ClashState) second;
        // A ray can report the same overlap in several ticks. One impact is one trade.
        if (left.kazi$getLastClash(second.getId()) != Long.MIN_VALUE) return true;
        left.kazi$markClash(second.getId(), first.level.getGameTime());
        right.kazi$markClash(first.getId(), first.level.getGameTime());
        float leftHealth = health(first);
        float rightHealth = health(second);
        left.kazi$setClashHealth(ClashMath.remaining(leftHealth, second.getDamage()));
        right.kazi$setClashHealth(ClashMath.remaining(rightHealth, first.getDamage()));
        if (left.kazi$getClashHealth() <= 0) first.remove();
        if (right.kazi$getClashHealth() <= 0) second.remove();
        return true;
    }

    private static float health(AbilityProjectileEntity projectile) {
        ClashState state = (ClashState) projectile;
        float value = state.kazi$getClashHealth();
        if (Float.isNaN(value)) {
            value = Math.max(0, projectile.getDamage());
            state.kazi$setClashHealth(value);
        }
        return value;
    }

    public static float scan(LightningEntity beam, float reach) {
        if (beam.level.isClientSide || beam.getDamage() <= 0 || beam.getMimicVanilla() || reach <= 0) return reach;
        Vector3d start = new Vector3d(beam.getX(), beam.getEyeY(), beam.getZ());
        Vector3d direction = beam.getLookAngle().normalize();
        Vector3d end = start.add(direction.scale(reach));
        double radius = Math.min(2.5, Math.max(0.6, beam.getSize() / Math.max(1.0, beam.getBoxSizeDivision())));
        AxisAlignedBB bounds = new AxisAlignedBB(start, end).inflate(radius + 2.0);
        List<AbilityProjectileEntity> others = beam.level.getEntitiesOfClass(AbilityProjectileEntity.class, bounds,
                other -> other != beam && other.isAlive() && other.getDamage() > 0 && other.getOwner() != beam.getOwner());
        float cap = reach;
        for (AbilityProjectileEntity other : others) {
            if (other instanceof LightningEntity) {
                LightningEntity opposing = (LightningEntity) other;
                if (opposing.getMimicVanilla()) continue;
                float hit = beamIntersection(start, direction, reach, opposing, radius);
                if (hit < 0) continue;
                float otherHit = beamIntersection(
                        new Vector3d(opposing.getX(), opposing.getEyeY(), opposing.getZ()),
                        opposing.getLookAngle().normalize(), Math.max(1, opposing.getLength()), beam, radius);
                if (otherHit < 0) continue;
                if (continuous(beam) && continuous(opposing)) {
                    cap = Math.min(cap, hit);
                    if (oncePerTick(beam.getId() < opposing.getId() ? beam : opposing,
                            beam.getId() < opposing.getId() ? opposing : beam)) lockCasters(beam, opposing);
                } else if (continuous(beam)) {
                    if (oncePerTick(beam.getId() < opposing.getId() ? beam : opposing,
                            beam.getId() < opposing.getId() ? opposing : beam)
                            && erode(opposing, beam.getDamage())) opposing.remove();
                    else if (opposing.isAlive() && beam.getDamage() <= opposing.getDamage()) cap = Math.min(cap, hit);
                } else if (continuous(opposing)) {
                    if (oncePerTick(beam.getId() < opposing.getId() ? beam : opposing,
                            beam.getId() < opposing.getId() ? opposing : beam)
                            && erode(beam, opposing.getDamage())) { beam.remove(); return 0; }
                    if (beam.isAlive() && beam.getDamage() <= opposing.getDamage()) cap = Math.min(cap, hit);
                } else if (beam.getDamage() > opposing.getDamage()) {
                    opposing.remove();
                } else if (opposing.getDamage() > beam.getDamage()) {
                    cap = Math.min(cap, hit);
                } else {
                    cap = Math.min(cap, hit);
                }
                continue;
            }

            Vector3d center = other.position().add(0, other.getBbHeight() * 0.5, 0);
            double along = center.subtract(start).dot(direction);
            if (along < 0 || along > reach) continue;
            double miss = center.distanceToSqr(start.add(direction.scale(along)));
            double combined = radius + Math.max(0.2, other.getBbWidth() * 0.5);
            if (miss > combined * combined) continue;
            if (!oncePerTick(beam, other)) continue;
            if (continuous(beam)) {
                if (erode(other, beam.getDamage())) other.remove();
                else cap = Math.min(cap, (float) Math.max(0, along - 0.25));
            } else if (ClashMath.beamDestroys(beam.getDamage(), other.getDamage())) {
                other.remove();
            } else {
                cap = Math.min(cap, (float) Math.max(0, along - 0.25));
            }
        }
        return cap;
    }

    private static boolean erode(AbilityProjectileEntity target, float damage) {
        ClashState state = (ClashState) target;
        state.kazi$setClashHealth(ClashMath.remaining(health(target), damage));
        return state.kazi$getClashHealth() <= 0;
    }

    private static boolean oncePerTick(AbilityProjectileEntity first, AbilityProjectileEntity second) {
        ClashState state = (ClashState) first;
        long now = first.level.getGameTime();
        if (state.kazi$getLastClash(second.getId()) == now) return false;
        state.kazi$markClash(second.getId(), now);
        return true;
    }

    private static float beamIntersection(Vector3d start, Vector3d direction, float reach,
                                          LightningEntity other, double radius) {
        Vector3d otherStart = new Vector3d(other.getX(), other.getEyeY(), other.getZ());
        Vector3d otherDirection = other.getLookAngle().normalize();
        double otherReach = Math.max(1, other.getLength());
        double spacing = Math.max(0.5, radius);
        for (double along = 0; along <= reach; along += spacing) {
            Vector3d point = start.add(direction.scale(along));
            double projected = Math.max(0, Math.min(otherReach, point.subtract(otherStart).dot(otherDirection)));
            double combined = radius + Math.min(2.5, Math.max(0.6, other.getSize() / Math.max(1.0, other.getBoxSizeDivision())));
            if (point.distanceToSqr(otherStart.add(otherDirection.scale(projected))) <= combined * combined)
                return (float) Math.max(0, along - 0.25);
        }
        return -1;
    }

    private static boolean continuous(LightningEntity beam) {
        Entity owner = beam.getOwner();
        if (!(owner instanceof LivingEntity) || beam.getParent() == null) return false;
        IAbility ability = AbilityDataCapability.get((LivingEntity) owner).getEquippedAbility(beam.getParent());
        if (ability == null) return false;
        for (AbilityComponent<?> component : ability.getComponents().values()) {
            if (component instanceof ContinuousComponent && ((ContinuousComponent) component).isContinuous()) return true;
        }
        return false;
    }

    private static void lockCasters(LightningEntity first, LightningEntity second) {
        Entity a = first.getOwner();
        Entity b = second.getOwner();
        if (!(a instanceof LivingEntity) || !(b instanceof LivingEntity)) return;
        lock((LivingEntity) a, b);
        lock((LivingEntity) b, a);
    }

    private static void lock(LivingEntity caster, Entity opponent) {
        caster.addEffect(new EffectInstance(ModEffects.PARALYSIS.get(), 4, 0, false, false, true));
        Vector3d toward = opponent.position().add(0, opponent.getBbHeight() * 0.5, 0)
                .subtract(caster.position().add(0, caster.getEyeHeight(), 0));
        double flat = Math.sqrt(toward.x * toward.x + toward.z * toward.z);
        caster.yRot = (float) Math.toDegrees(Math.atan2(-toward.x, toward.z));
        caster.xRot = (float) -Math.toDegrees(Math.atan2(toward.y, flat));
        caster.yHeadRot = caster.yRot;
        if (caster instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) caster;
            WyNetwork.sendTo(SPinCameraPacket.pinClampedYawAndPitch(caster.yRot, 0, caster.xRot, 0), player);
            PINNED.put(player.getUUID(), player.level.getGameTime());
        }
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level.isClientSide || !(event.player instanceof ServerPlayerEntity)) return;
        UUID id = event.player.getUUID();
        Long last = PINNED.get(id);
        if (last != null && event.player.level.getGameTime() - last > 2) {
            WyNetwork.sendTo(new SUnpinCameraPacket(), (ServerPlayerEntity) event.player);
            PINNED.remove(id);
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        PINNED.remove(event.getPlayer().getUUID());
    }
}
