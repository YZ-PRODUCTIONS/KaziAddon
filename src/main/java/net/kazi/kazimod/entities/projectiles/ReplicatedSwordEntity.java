package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.abilities.SupaRework.ProjectionAbility;
import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;

/** A sword waiting in a portal becomes a normal, colliding ability projectile on release. */
public class ReplicatedSwordEntity extends AbilityProjectileEntity {
    private static final double HOMING_RANGE = 18.0D;
    private static final double HOMING_CONE_DOT = Math.cos(Math.toRadians(30.0D));
    private static final double HOMING_STRENGTH = 0.04D;
    private LivingEntity homingTarget;
    private static final DataParameter<Boolean> FIRED = EntityDataManager.defineId(ReplicatedSwordEntity.class, DataSerializers.BOOLEAN);
    private Vector3d offset = Vector3d.ZERO;
    private int flightTicks;
    private final long visualCreatedAt;
    private long visualLaunchedAt = -1L;

    public ReplicatedSwordEntity(EntityType<? extends ReplicatedSwordEntity> type, World world) {
        super(type, world);
        this.visualCreatedAt = world.getGameTime();
        setGravity(0);
        setNoGravity(true);
    }

    public ReplicatedSwordEntity(LivingEntity caster, Vector3d offset) {
        super(KaziEntities.REPLICATED_SWORD.get(), caster.level, caster, ProjectionAbility.INSTANCE,
                ProjectionAbility.INSTANCE.getSourceElement(), ProjectionAbility.INSTANCE.getSourceHakiNature(),
                ProjectionAbility.INSTANCE.getSourceTypes());
        this.visualCreatedAt = caster.level.getGameTime();
        this.offset = offset;
        setPos(caster.getX() + offset.x, caster.getY() + offset.y, caster.getZ() + offset.z);
        setDamage(10.0F);
        setGravity(0);
        setNoGravity(true);
        setMaxLife(400);
        setDeltaMovement(Vector3d.ZERO);
    }

    @Override public void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(FIRED, false);
    }

    public boolean isFired() { return entityData.get(FIRED); }

    /** Visual-only timelines keep formation and launch separate from projectile lifetime. */
    public float getVisualAge(float partialTick) {
        return Math.max(0, level.getGameTime() - this.visualCreatedAt + partialTick);
    }

    public float getFlightVisualAge(float partialTick) {
        return this.visualLaunchedAt < 0 ? 0
                : Math.max(0, level.getGameTime() - this.visualLaunchedAt + partialTick);
    }

    @Override
    public void onSyncedDataUpdated(DataParameter<?> key) {
        super.onSyncedDataUpdated(key);
        if (FIRED.equals(key) && isFired()) this.visualLaunchedAt = level.getGameTime();
    }

    @Override public void onModHit(net.minecraft.util.math.RayTraceResult hit) {
        if (!isFired()) return;
        if (!level.isClientSide && hit instanceof net.minecraft.util.math.EntityRayTraceResult) {
            net.minecraft.entity.Entity target = ((net.minecraft.util.math.EntityRayTraceResult) hit).getEntity();
            LivingEntity owner = getThrower();
            if (target == owner || (owner != null && (owner.isAlliedTo(target)
                    || !xyz.pixelatedw.mineminenomi.init.ModEntityPredicates.getEnemyFactions(owner).test(target)))) return;
            // Rapid consecutive swords must not be swallowed by vanilla hurt immunity.
            target.invulnerableTime = 0;
        }
        super.onModHit(hit);
        if (!level.isClientSide && hit.getType() != net.minecraft.util.math.RayTraceResult.Type.MISS) remove();
    }

    public void launch(Vector3d target) {
        entityData.set(FIRED, true);
        // MMNM uses separate collision boxes, not the rendered model's size.
        // Only launched swords grow from 0.3 to 2.4 blocks on each axis.
        double width = getType().getWidth() * 8.0D;
        double height = getType().getHeight() * 8.0D;
        setEntityCollisionSize(width, height, width);
        setBlockCollisionSize(width, height, width);
        Vector3d direction = target.subtract(position()).normalize();
        shoot(direction.x, direction.y, direction.z, 3.75F, 0);
        hurtMarked = true;
    }

    @Override public void tick() {
        if (!isFired()) {
            if (!level.isClientSide) {
                LivingEntity caster = getThrower();
                if (caster == null || !caster.isAlive()) { remove(); return; }
                ProjectionAbility ability = AbilityDataCapability.get(caster).getEquippedAbility(ProjectionAbility.INSTANCE);
                if (ability == null || !ability.isReplicatingSwords()) { remove(); return; }
                setPos(caster.getX() + offset.x, caster.getY() + offset.y, caster.getZ() + offset.z);
                Vector3d look = caster.getLookAngle();
                yRot = (float)Math.toDegrees(Math.atan2(look.x, look.z));
                xRot = (float)-Math.toDegrees(Math.asin(look.y));
            }
            return;
        }
        if (!level.isClientSide) applyWeakHoming();
        super.tick();
        if (!level.isClientSide && ++flightTicks >= 80) remove();
    }

    /** Small course corrections, not a lock-on: missed swords never turn around. */
    private void applyWeakHoming() {
        LivingEntity owner = getThrower();
        Vector3d motion = getDeltaMovement();
        double speed = motion.length();
        if (owner == null || !owner.isAlive() || speed < 1.0E-5D) return;
        Vector3d forward = motion.scale(1.0D / speed);
        if (!isHomingTarget(homingTarget, owner, forward)) homingTarget = null;
        // Retain the selected target; stagger scans instead of searching every sword every tick.
        if (homingTarget == null && (flightTicks + getId()) % 4 == 0) {
            double bestScore = -Double.MAX_VALUE;
            for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox().inflate(HOMING_RANGE), target -> target != owner && target.isAlive())) {
                if (!isHomingTarget(candidate, owner, forward)) continue;
                Vector3d delta = homingAim(candidate).subtract(position());
                double score = forward.dot(delta.normalize()) * 4.0D - delta.length() * 0.02D;
                if (score > bestScore) {
                    bestScore = score;
                    homingTarget = candidate;
                }
            }
        }
        if (homingTarget == null) return;
        Vector3d desired = homingAim(homingTarget).subtract(position()).normalize();
        Vector3d steered = forward.scale(1.0D - HOMING_STRENGTH).add(desired.scale(HOMING_STRENGTH));
        setDeltaMovement(steered.normalize().scale(speed));
        hurtMarked = true;
    }

    private boolean isHomingTarget(LivingEntity target, LivingEntity owner, Vector3d forward) {
        if (target == null || !target.isAlive() || target.level != level || target == owner
                || target.isSpectator() || owner.isAlliedTo(target) || target.isAlliedTo(owner)
                || (target instanceof PlayerEntity && ((PlayerEntity) target).isCreative())
                || !ModEntityPredicates.getEnemyFactions(owner).test(target)) return false;
        Vector3d aim = homingAim(target);
        Vector3d delta = aim.subtract(position());
        if (delta.lengthSqr() < 1.0E-6D || delta.lengthSqr() > HOMING_RANGE * HOMING_RANGE
                || forward.dot(delta.normalize()) < HOMING_CONE_DOT) return false;
        return level.clip(new RayTraceContext(position(), aim, RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.NONE, this)).getType() == RayTraceResult.Type.MISS;
    }

    private static Vector3d homingAim(LivingEntity target) {
        return target.position().add(0, target.getBbHeight() * 0.5D, 0);
    }
}
