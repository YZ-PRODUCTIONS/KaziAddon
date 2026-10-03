package net.kazi.kazimod.entities;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.kazi.kazimod.abilities.GoruRework.EaAbility;
import net.kazi.kazimod.init.KaziPacketHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.util.DamageSource;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkHooks;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;

/** One server-owned cast: a following charge, fixed rupture, then a harmless collapse. */
public final class EaVfxEntity extends Entity {
    public static final int CHARGE_TICKS = EaRuptureShape.CHARGE_TICKS;
    public static final int RELEASE_TICKS = EaRuptureShape.RELEASE_TICKS;
    public static final int FADE_TICKS = EaRuptureShape.FADE_TICKS;
    public static final float MAX_RANGE = (float)EaRuptureShape.MAX_RANGE;
    public static final float MAX_RADIUS = (float)EaRuptureShape.MAX_RADIUS;
    private static final DataParameter<CompoundNBT> STATE = EntityDataManager.defineId(EaVfxEntity.class, DataSerializers.COMPOUND_TAG);
    private static final DataParameter<Float> PROGRESS = EntityDataManager.defineId(EaVfxEntity.class, DataSerializers.FLOAT);
    private final Set<UUID> hitTargets = new HashSet<>();
    private LivingEntity owner;
    private EaAbility ability;
    private boolean collapsed;
    private Vector3d pendingBlockImpact;
    private boolean terrainQueued;
    private final Queue<TerrainPass> terrainPasses = new ArrayDeque<>();
    private static final float IMPACT_RADIUS = 30.0F;

    public EaVfxEntity(EntityType<? extends EaVfxEntity> type, World world) {
        super(type, world);
        noPhysics = true;
        noCulling = true;
        setNoGravity(true);
    }

    public void beginCharge(LivingEntity caster, EaAbility source) {
        beginCharge(caster, source, false);
    }

    public void beginCharge(LivingEntity caster, EaAbility source, boolean winds) {
        owner = caster;
        ability = source;
        CompoundNBT state = new CompoundNBT();
        state.putInt("owner", caster.getId());
        state.putBoolean("winds", winds);
        state.putLong("start", level.getGameTime());
        entityData.set(STATE, state);
        followCaster();
    }

    public boolean isWinds() { return entityData.get(STATE).getBoolean("winds"); }
    public int getChargeTicks() { return isWinds() ? WindsRaptureShape.CHARGE_TICKS : CHARGE_TICKS; }

    public static final int PILLAR_IMPACT_TICK = net.kazi.kazimod.models.abilities.UtaPillarMesh.IMPACT_TICK;
    public static final float PILLAR_RADIUS = net.kazi.kazimod.models.abilities.UtaPillarMesh.MAX_RADIUS * 1.5F;
    public void beginPillar(LivingEntity caster, EaAbility source) {
        owner = caster;
        ability = source;
        Vector3d eye = caster.getEyePosition(1);
        Vector3d end = eye.add(caster.getLookAngle().scale(MAX_RANGE));
        BlockRayTraceResult hit = level.clip(new RayTraceContext(eye, end,
                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, caster));
        Vector3d target = hit.getType() == RayTraceResult.Type.MISS ? end : hit.getLocation();
        // Include entities under the crosshair, but never select through the first block.
        double nearest = eye.distanceToSqr(target);
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                new AxisAlignedBB(eye, target).inflate(1), this::isEnemy)) {
            java.util.Optional<Vector3d> intercept = candidate.getBoundingBox().inflate(0.2).clip(eye, target);
            if (intercept.isPresent() && eye.distanceToSqr(intercept.get()) < nearest) {
                nearest = eye.distanceToSqr(intercept.get());
                target = intercept.get();
            }
        }
        setPos(target.x, target.y, target.z);
        CompoundNBT state = new CompoundNBT();
        state.putInt("owner", caster.getId());
        state.putBoolean("pillar", true);
        state.putLong("start", level.getGameTime());
        entityData.set(STATE, state);
    }
    public boolean isPillar() { return entityData.get(STATE).getBoolean("pillar"); }

    private void tickPillar() {
        float age = getPhaseAge(0);
        double bottom = net.kazi.kazimod.models.abilities.UtaPillarMesh.bottomAt(age);
        double top = Math.min(net.kazi.kazimod.models.abilities.UtaPillarMesh.PORTAL_HEIGHT,
                bottom + net.kazi.kazimod.models.abilities.UtaPillarMesh.HEIGHT);
        setBoundingBox(new AxisAlignedBB(getX() - PILLAR_RADIUS, getY() + bottom, getZ() - PILLAR_RADIUS,
                getX() + PILLAR_RADIUS, getY() + top, getZ() + PILLAR_RADIUS));
        if (level.isClientSide) return;
        if (owner == null || !owner.isAlive() || owner.level != level) { remove(); return; }
        if (age >= net.kazi.kazimod.models.abilities.UtaPillarMesh.DROP_TICK
                && age < PILLAR_IMPACT_TICK && tickCount % 4 == 0) shake(6, 1.2F);
        if (!collapsed && age >= PILLAR_IMPACT_TICK) {
            collapsed = true;
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox(), this::isEnemy)) {
                Vector3d delta = target.position().subtract(position());
                if (ability.hit(owner, target, EaAbility.Mode.PILLAR) && !AbilityHelper.isDodging(target)) {
                    Vector3d push = new Vector3d(delta.x, 0, delta.z).normalize().scale(2.5);
                    AbilityHelper.setDeltaMovement(target, push.x, 1.2, push.z);
                }
            }
            shake(30, 3.5F);
        }
        if (age >= PILLAR_IMPACT_TICK + 45) remove();
    }

    public void setChargeProgress(float progress) {
        entityData.set(PROGRESS, MathHelper.clamp(progress, 0, 1));
    }

    public float getChargeProgress(float partial) {
        float progress = entityData.get(PROGRESS);
        return MathHelper.clamp(progress + (isCancelled() ? 0 : partial / getChargeTicks()), 0, 1);
    }

    public void release(LivingEntity caster) {
        if (level.isClientSide || isReleased() || isCancelled()) return;
        followCaster();
        Vector3d direction = caster.getLookAngle().normalize();
        Vector3d origin = position();
        float maxRange = isWinds() ? WindsRaptureShape.MAX_RANGE : MAX_RANGE;
        Vector3d end = origin.add(direction.scale(maxRange));
        BlockRayTraceResult hit = level.clip(new RayTraceContext(origin, end,
                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, caster));
        float range = hit.getType() == RayTraceResult.Type.MISS ? maxRange : (float)origin.distanceTo(hit.getLocation());
        pendingBlockImpact = !isWinds() && hit.getType() == RayTraceResult.Type.BLOCK ? hit.getLocation() : null;
        CompoundNBT state = entityData.get(STATE).copy();
        Vector3d center = getChargeCenter(1);
        state.putDouble("cx", center.x); state.putDouble("cy", center.y); state.putDouble("cz", center.z);
        state.putInt("phase", 1);
        state.putLong("start", level.getGameTime());
        state.putDouble("x", origin.x); state.putDouble("y", origin.y); state.putDouble("z", origin.z);
        state.putDouble("dx", direction.x); state.putDouble("dy", direction.y); state.putDouble("dz", direction.z);
        state.putFloat("range", range);
        entityData.set(STATE, state);
        entityData.set(PROGRESS, 1.0F);
        shake(isWinds() ? 10 : 38, isWinds() ? .7F : 3.5F);
        refreshBounds();
    }

    public void cancel() {
        if (level.isClientSide || isReleased() || isCancelled()) return;
        CompoundNBT state = entityData.get(STATE).copy();
        Vector3d direction = getBeamDirection();
        Vector3d center = getChargeCenter(1);
        state.putDouble("cx", center.x); state.putDouble("cy", center.y); state.putDouble("cz", center.z);
        state.putFloat("cyaw", getChargeBodyYaw(1));
        if (isWinds()) {
            // Freeze the visual side-hold before switching phase. Body yaw alone
            // cannot recover the camera's side when the caster looks vertically.
            state.putFloat("holdYaw", getChargeViewYaw(1));
            state.putFloat("holdEyeHeight", getChargeEyeHeight());
            state.putBoolean("holdLeftHanded", isChargeLeftHanded());
        }
        state.putInt("phase", 2);
        state.putLong("start", level.getGameTime());
        state.putDouble("dx", direction.x); state.putDouble("dy", direction.y); state.putDouble("dz", direction.z);
        entityData.set(STATE, state);
    }

    public boolean isReleased() { return entityData.get(STATE).getInt("phase") == 1; }
    public boolean isCancelled() { return entityData.get(STATE).getInt("phase") == 2; }
    public float getPhaseAge(float partial) {
        return Math.max(0, level.getGameTime() - entityData.get(STATE).getLong("start") + partial);
    }
    public LivingEntity getCaster() {
        if (owner != null) return owner;
        Entity found = level.getEntity(entityData.get(STATE).getInt("owner"));
        return found instanceof LivingEntity ? (LivingEntity)found : null;
    }
    public Vector3d getOrigin() {
        CompoundNBT state = entityData.get(STATE);
        return isReleased() ? new Vector3d(state.getDouble("x"), state.getDouble("y"), state.getDouble("z")) : position();
    }
    /** The charge stays at the caster's feet, then freezes on release or cancellation. */
    public Vector3d getChargeCenter(float partial) {
        CompoundNBT state = entityData.get(STATE);
        if ((isCancelled() || isReleased()) && state.contains("cx")) {
            return new Vector3d(state.getDouble("cx"), state.getDouble("cy"), state.getDouble("cz"));
        }
        LivingEntity caster = getCaster();
        if (caster != null) return new Vector3d(MathHelper.lerp(partial, caster.xOld, caster.getX()),
                MathHelper.lerp(partial, caster.yOld, caster.getY()),
                MathHelper.lerp(partial, caster.zOld, caster.getZ()));
        return position().subtract(getBeamDirection().scale(1.4)).add(0, -1.62, 0);
    }
    public float getChargeBodyYaw(float partial) {
        if (isCancelled()) return entityData.get(STATE).getFloat("cyaw");
        LivingEntity caster = getCaster();
        return caster == null ? 0 : caster.yBodyRotO
                + MathHelper.wrapDegrees(caster.yBodyRot - caster.yBodyRotO) * partial;
    }
    public float getChargeViewYaw(float partial) {
        CompoundNBT state = entityData.get(STATE);
        if (isCancelled() && state.contains("holdYaw")) return state.getFloat("holdYaw");
        LivingEntity caster = getCaster();
        return caster == null ? getChargeBodyYaw(partial) : MathHelper.rotLerp(partial, caster.yRotO, caster.yRot);
    }
    public float getChargeEyeHeight() {
        CompoundNBT state = entityData.get(STATE);
        if (isCancelled() && state.contains("holdEyeHeight")) return state.getFloat("holdEyeHeight");
        LivingEntity caster = getCaster();
        return caster == null ? 1.62F : caster.getEyeHeight();
    }
    public boolean isChargeLeftHanded() {
        CompoundNBT state = entityData.get(STATE);
        if (isCancelled() && state.contains("holdLeftHanded")) return state.getBoolean("holdLeftHanded");
        LivingEntity caster = getCaster();
        return caster != null && caster.getMainArm() == HandSide.LEFT;
    }
    public Vector3d getBeamDirection() {
        if (isReleased() || isCancelled()) {
            CompoundNBT state = entityData.get(STATE);
            return new Vector3d(state.getDouble("dx"), state.getDouble("dy"), state.getDouble("dz"));
        }
        LivingEntity caster = getCaster();
        return caster != null ? caster.getLookAngle().normalize() : new Vector3d(0, 0, 1);
    }
    public float getReleaseLength() { return getReleaseLength(0); }
    public float getReleaseLength(float partial) {
        if (isWinds()) return WindsRaptureShape.length(getPhaseAge(partial), entityData.get(STATE).getFloat("range"));
        return (float)EaRuptureShape.length(getPhaseAge(partial), entityData.get(STATE).getFloat("range"));
    }
    public float getReleaseRadius() { return getReleaseRadius(0); }
    public float getReleaseRadius(float partial) { return isWinds() ? WindsRaptureShape.MAX_RADIUS
            : (float)EaRuptureShape.radius(getPhaseAge(partial)); }

    private void followCaster() {
        Vector3d eye = owner.getEyePosition(1);
        Vector3d pos = eye.add(owner.getLookAngle().scale(1.4));
        BlockRayTraceResult wall = level.clip(new RayTraceContext(eye, pos,
                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, owner));
        if (wall.getType() != RayTraceResult.Type.MISS) pos = wall.getLocation().subtract(owner.getLookAngle().scale(0.05));
        setPos(pos.x, pos.y, pos.z);
    }

    @Override public void tick() {
        super.tick();
        if (isPillar()) { tickPillar(); return; }
        if (level.isClientSide) {
            // Spawn and metadata are separate packets. Do not consume the audio start
            // window against the placeholder state before the cast timestamp arrives.
            if (entityData.get(STATE).contains("start")) {
                if (isWinds()) DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> net.kazi.kazimod.effects.WindsRaptureSoundController.tick(this));
                else DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> net.kazi.kazimod.effects.EaSoundController.tick(this));
            }
            return;
        }
        if (isCancelled()) {
            if (getPhaseAge(0) >= EaRuptureShape.CANCEL_TICKS) remove();
            return;
        }
        if (!isReleased()) {
            IAbilityData data = owner == null ? null : AbilityDataCapability.get(owner);
            if (owner == null || !owner.isAlive() || owner.level != level || data == null
                    || data.getEquippedAbility(EaAbility.INSTANCE) != ability || !ability.isChargingVfx(this)) {
                cancel();
                return;
            }
            followCaster();
            if (!isWinds() && tickCount % 16 == 0 && getChargeProgress(0) > 0.5) shake(12, 0.45F);
        } else {
            float age = getPhaseAge(0);
            if (isWinds()) {
                if (age >= WindsRaptureShape.RELEASE_TICKS + WindsRaptureShape.FADE_TICKS) { remove(); return; }
                if (WindsRaptureShape.damaging(age) && owner != null && owner.isAlive() && owner.level == level) damageTargets();
                refreshBounds();
                return;
            }
            if (age >= RELEASE_TICKS + FADE_TICKS && terrainPasses.isEmpty()) { remove(); return; }
            if (!terrainQueued && age >= EaRuptureShape.DEPLOY_TICKS) {
                terrainQueued = true;
                Vector3d origin = getOrigin();
                terrainPasses.add(new TerrainPass(origin, getBeamDirection(), getReleaseLength(), MAX_RADIUS));
            }
            if (pendingBlockImpact != null && age >= EaRuptureShape.DEPLOY_TICKS) {
                // Fire once when the growing beam reaches its solid-terrain endpoint.
                // Hollow Nuke's normal visual radius is 60 blocks; this version is half-sized.
                KokuVfxEntity.impact(level, pendingBlockImpact, KokuVfxEntity.RED_NUKE_IMPACT, IMPACT_RADIUS, 0.5F, 1.5F);
                pendingBlockImpact = null;
            }
            eraseTerrain();
            if (EaRuptureShape.damaging(age) && owner != null && owner.isAlive() && owner.level == level) damageTargets();
            if (!collapsed && age >= RELEASE_TICKS) {
                collapsed = true;
                shake(20, 2.0F);
            }
        }
        refreshBounds();
    }

    /** Bound both scanning and edits while erasing terrain along the beam. */
    private void eraseTerrain() {
        int scanned = 0;
        int erased = 0;
        while (!terrainPasses.isEmpty() && scanned < 8192 && erased < 512) {
            TerrainPass pass = terrainPasses.peek();
            if (!pass.positions.hasNext()) {
                terrainPasses.remove();
                continue;
            }
            BlockPos pos = pass.positions.next();
            scanned++;
            if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) continue;
            BlockState block = level.getBlockState(pos);
            if (block.isAir(level, pos) || block.getDestroySpeed(level, pos) < 0 || !pass.contains(pos)) continue;
            // Erasure produces neither item drops nor thousands of break particles.
            if (level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) erased++;
        }
    }

    private static final class TerrainPass {
        private final Vector3d origin;
        private final Vector3d direction;
        private final double length;
        private final double radius;
        private final Iterator<BlockPos> positions;

        private TerrainPass(Vector3d origin, Vector3d direction, double length, double radius) {
            this.origin = origin;
            this.direction = direction;
            this.length = length;
            this.radius = radius;
            Vector3d end = origin.add(direction.scale(length));
            AxisAlignedBB bounds = new AxisAlignedBB(origin, end).inflate(radius);
            positions = BlockPos.betweenClosed(new BlockPos(bounds.minX, bounds.minY, bounds.minZ),
                    new BlockPos(bounds.maxX, bounds.maxY, bounds.maxZ)).iterator();
        }

        private boolean contains(BlockPos pos) {
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            return EaRuptureShape.intersects(origin.x, origin.y, origin.z,
                    direction.x, direction.y, direction.z, length, radius,
                    x, y, z, x + 1, y + 1, z + 1);
        }
    }

    private void damageTargets() {
        Vector3d start = getOrigin();
        Vector3d dir = getBeamDirection();
        AxisAlignedBB bounds = new AxisAlignedBB(start, start.add(dir.scale(getReleaseLength()))).inflate(getReleaseRadius());
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, bounds, this::isEnemy)) {
            AxisAlignedBB box = target.getBoundingBox();
            if (hitTargets.contains(target.getUUID()) || !EaRuptureShape.intersects(start.x, start.y, start.z,
                    dir.x, dir.y, dir.z, getReleaseLength(), getReleaseRadius(),
                    box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ)) continue;
            // The rupture stops at solid terrain; side lobes cannot hit through cover either.
            BlockRayTraceResult wall = level.clip(new RayTraceContext(start, box.getCenter(),
                    RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, owner));
            if (wall.getType() != RayTraceResult.Type.MISS) continue;
            hitTargets.add(target.getUUID());
            boolean dodging = AbilityHelper.isDodging(target);
            if (ability.hit(owner, target, isWinds() ? EaAbility.Mode.WINDS : EaAbility.Mode.EA)
                    && !dodging && !AbilityHelper.isDodging(target)) {
                AbilityHelper.setDeltaMovement(target, dir.x * 2.0, Math.max(0.65, dir.y * 2.0), dir.z * 2.0);
            }
        }
    }

    private boolean isEnemy(LivingEntity target) {
        return target != owner && target.isAlive() && !target.isSpectator()
                && !(target instanceof PlayerEntity && ((PlayerEntity)target).isCreative())
                && !owner.isAlliedTo(target) && !target.isAlliedTo(owner)
                && ModEntityPredicates.getEnemyFactions(owner).test(target);
    }

    private void shake(int ticks, float intensity) {
        if (!(level instanceof ServerWorld)) return;
        Vector3d center = isReleased() ? getOrigin().add(getBeamDirection().scale(getReleaseLength() * 0.5)) : position();
        double shakeRange = isPillar() ? 192 : 100;
        for (ServerPlayerEntity player : ((ServerWorld)level).players()) {
            double distance = player.position().distanceTo(center);
            if (distance < shakeRange) KaziPacketHandler.sendCameraShake(player, ticks,
                    intensity * (float)(1 - distance / shakeRange));
        }
    }

    private void refreshBounds() {
        if (isReleased()) {
            setBoundingBox(new AxisAlignedBB(getOrigin(), getOrigin().add(getBeamDirection().scale(getReleaseLength())))
                    .inflate(isWinds() ? WindsRaptureShape.MAX_RADIUS + 1 : MAX_RADIUS + 8));
        } else setBoundingBox(new AxisAlignedBB(position(), position()).inflate(isWinds() ? 6 : 16));
    }
    @Override protected void defineSynchedData() {
        entityData.define(STATE, new CompoundNBT());
        entityData.define(PROGRESS, 0.0F);
    }
    @Override protected void readAdditionalSaveData(CompoundNBT tag) { remove(); }
    @Override protected void addAdditionalSaveData(CompoundNBT tag) { }
    @Override public boolean isPickable() { return false; }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public IPacket<?> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 224 * 224; }
}
