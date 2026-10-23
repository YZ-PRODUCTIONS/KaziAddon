package net.kazi.kazimod.tusk;

import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.network.datasync.*;
import net.minecraft.util.Direction;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

public final class TuskProjectile extends AbilityProjectileEntity {
    public static final int NAIL = 0, SHOTGUN = 1, SEEKER = 2, HOLE = 3, WORMHOLE = 4, INFINITE = 5, MELEE = 6;
    private static final DataParameter<Integer> KIND = EntityDataManager.defineId(TuskProjectile.class, DataSerializers.INT),
            ACT = EntityDataManager.defineId(TuskProjectile.class, DataSerializers.INT),
            TARGET = EntityDataManager.defineId(TuskProjectile.class, DataSerializers.INT),
            FACE = EntityDataManager.defineId(TuskProjectile.class, DataSerializers.INT);
    private boolean spent;
    private Vector3d firedAt;
    public TuskProjectile(EntityType<?> type, World world) { super(type, world); }
    public TuskProjectile(LivingEntity owner, AbilityCore<?> core, int kind, float damage) {
        super(TuskFeatures.PROJECTILE.get(), owner.level, owner, core);
        entityData.set(KIND, kind); entityData.set(ACT, TuskState.act(owner));
        firedAt = owner.position(); setDamage(damage); setGravity(0); setMaxLife(kind == HOLE ? 120 : 50);
        setAffectedByImbuing(); setProjectile(); setEntityCollisionSize(kind == INFINITE ? .6 : .25); setBlockCollisionSize(.12);
        setCollideWithBlocks(kind != HOLE); setCollideWithEntities(kind != HOLE);
        if (kind == HOLE) { setPassThroughBlocks(); noPhysics = true; }
    }
    @Override public void defineSynchedData() {
        super.defineSynchedData(); entityData.define(KIND, 0); entityData.define(ACT, 1);
        entityData.define(TARGET, -1); entityData.define(FACE, Direction.UP.get3DDataValue());
    }
    public int kind() { return entityData.get(KIND); }
    public int act() { return entityData.get(ACT); }
    public Direction face() { return Direction.from3DDataValue(entityData.get(FACE)); }
    public void target(LivingEntity target) { entityData.set(TARGET, target.getId()); }
    public static List<TuskProjectile> holes(LivingEntity owner) {
        List<TuskProjectile> result = owner.level.getEntitiesOfClass(TuskProjectile.class, owner.getBoundingBox().inflate(80),
                p -> p.isAlive() && p.kind() == HOLE && p.getThrower() == owner);
        result.sort(Comparator.comparingInt((TuskProjectile p) -> p.tickCount).reversed()); return result;
    }
    @Override protected void onHitBlock(BlockRayTraceResult hit) { onModHit(hit); }
    @Override public void onModHit(RayTraceResult hit) {
        if (level.isClientSide || spent || !isAlive()) return;
        Entity entity = hit instanceof EntityRayTraceResult ? ((EntityRayTraceResult) hit).getEntity() : null;
        if (entity instanceof AbilityProjectileEntity) { super.onModHit(hit); return; }
        LivingEntity owner = getThrower();
        if (owner == null || !owner.isAlive()) { remove(); return; }
        if (entity instanceof LivingEntity) {
            LivingEntity victim = (LivingEntity) entity;
            if (!TuskFeatures.canHit(owner, victim)) return;
            spent = true;
            // Individual nails in a shotgun volley carry their own damage, rather than losing four hits to vanilla hurt time.
            if (kind() == SHOTGUN) victim.invulnerableTime = 0;
            if (victim.hurt(getDamageSource(), getDamage())) {
                TuskFeatures.landed(owner, victim, kind(), firedAt != null && victim.position().distanceToSqr(firedAt) >= 64);
                if (kind() == INFINITE) TuskFeatures.applyRotation(owner, victim);
            }
            TuskEffect.spawn(owner, TuskEffect.IMPACT, hit.getLocation(), 12, kind() == INFINITE ? 2 : .65F);
            remove(); return;
        }
        if (hit instanceof BlockRayTraceResult && kind() != HOLE) {
            spent = true;
            if ((kind() == SEEKER || kind() == NAIL && act() >= 2) && TuskState.awakened(owner)) createHole(owner, (BlockRayTraceResult) hit);
            else TuskEffect.spawn(owner, TuskEffect.IMPACT, hit.getLocation(), 10, .6F);
            remove();
        }
    }
    private void createHole(LivingEntity owner, BlockRayTraceResult hit) {
        List<TuskProjectile> active = holes(owner);
        for (int i = 0; i <= active.size() - 6; i++) active.get(i).remove();
        TuskProjectile hole = new TuskProjectile(owner, TuskAbility.core(TuskAbility.Move.HOLE), HOLE, 12 + act());
        hole.entityData.set(TARGET, entityData.get(TARGET)); hole.entityData.set(FACE, hit.getDirection().get3DDataValue());
        Vector3d at = hit.getLocation().add(Vector3d.atLowerCornerOf(hit.getDirection().getNormal()).scale(.04));
        hole.setPos(at.x, at.y, at.z); level.addFreshEntity(hole);
    }
    @Override public void tick() {
        if (!level.isClientSide && kind() == HOLE) updateHole();
        if (!level.isClientSide && kind() == INFINITE && !spent) trackTarget();
        super.tick();
        if (level.isClientSide) return;
        LivingEntity owner = getThrower();
        if (owner == null || !owner.isAlive() || !TuskState.awakened(owner) || owner.distanceToSqr(this) > 128 * 128) { remove(); return; }
        if (kind() == HOLE && !spent) for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(.6, .9, .6))) {
            if (TuskFeatures.canHit(owner, victim) && clear(position().add(0, .2, 0), victim.getBoundingBox().getCenter())) {
                onModHit(new EntityRayTraceResult(victim, victim.getBoundingBox().getCenter())); break;
            }
        }
    }
    private void trackTarget() {
        LivingEntity owner = getThrower();
        Entity found = level.getEntity(entityData.get(TARGET));
        if (owner == null || !(found instanceof LivingEntity) || !TuskFeatures.canHit(owner, (LivingEntity) found)) {
            entityData.set(TARGET, -1);
            return;
        }
        Vector3d to = found.getBoundingBox().getCenter().subtract(position());
        double distance = to.length();
        if (!Double.isFinite(distance) || distance < 1.0E-6) return;
        Vector3d desired = to.scale(Math.min(1.8, distance) / distance);
        setDeltaMovement(getDeltaMovement().scale(.65).add(desired.scale(.35)));
        hasImpulse = true;
    }
    private void updateHole() {
        setDeltaMovement(Vector3d.ZERO);
        LivingEntity owner = getThrower(); if (owner == null) return;
        Entity found = level.getEntity(entityData.get(TARGET));
        LivingEntity target = found instanceof LivingEntity && TuskFeatures.canHit(owner, (LivingEntity) found) ? (LivingEntity) found : null;
        if (target == null && tickCount % 10 == 0) {
            double closest = 16 * 16;
            for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(16))) {
                if (TuskFeatures.canHit(owner, candidate) && candidate.distanceToSqr(this) < closest && owner.canSee(candidate)) {
                    target = candidate; closest = candidate.distanceToSqr(this);
                }
            }
            if (target != null) target(target);
        }
        if (target == null || target.distanceToSqr(this) > 40 * 40) return;
        Vector3d normal = Vector3d.atLowerCornerOf(face().getNormal());
        Vector3d delta = target.getBoundingBox().getCenter().subtract(position());
        Vector3d tangent = delta.subtract(normal.scale(delta.dot(normal))).normalize().scale(.28);
        Vector3d ahead = position().add(tangent);
        if (!level.hasChunkAt(new BlockPos(ahead)) || !level.getWorldBorder().isWithinBounds(new BlockPos(ahead))) return;
        BlockRayTraceResult surface = level.clip(new RayTraceContext(ahead.add(normal.scale(.6)), ahead.subtract(normal.scale(1.6)),
                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, this));
        BlockRayTraceResult wall = level.clip(new RayTraceContext(position().add(normal.scale(.12)), ahead.add(normal.scale(.12)),
                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, this));
        if (wall.getType() == RayTraceResult.Type.BLOCK) surface = wall;
        if (surface.getType() == RayTraceResult.Type.BLOCK) {
            Vector3d at = surface.getLocation().add(Vector3d.atLowerCornerOf(surface.getDirection().getNormal()).scale(.045));
            if (at.distanceToSqr(position()) < 4) { setPos(at.x, at.y, at.z); entityData.set(FACE, surface.getDirection().get3DDataValue()); }
        }
    }
    private boolean clear(Vector3d from, Vector3d to) {
        return level.clip(new RayTraceContext(from, to, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, this)).getType() != RayTraceResult.Type.BLOCK;
    }
    @Override public AxisAlignedBB getBoundingBoxForCulling() { return super.getBoundingBoxForCulling().inflate(kind() == HOLE ? 1 : 3); }
}
