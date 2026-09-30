package net.kazi.kazimod.entities;

import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.kazi.kazimod.abilities.SupaRework.EnhancementAbility;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;

/** One overhead charge becomes one stationary, oriented blast volume on release. */
public class EnhancementLightEntity extends Entity {
    public static final float EFFECT_DURATION_SCALE = 1.5F;
    public static final int CHARGE_TICKS = 6 * 20;
    public static final int BLAST_HOLD_TICKS = 6 * 20;
    public static final int BLAST_FADE_TICKS = (int) (16 * EFFECT_DURATION_SCALE);
    public static final int BLAST_TICKS = BLAST_HOLD_TICKS + BLAST_FADE_TICKS;
    private static final double LAUNCH_UPWARD_SPEED = 3.5D;
    private static final double LAUNCH_FORWARD_SPEED = 1.5D;
    private static final DataParameter<Boolean> RELEASED =
            EntityDataManager.defineId(EnhancementLightEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> PHASE_START =
            EntityDataManager.defineId(EnhancementLightEntity.class, DataSerializers.INT);
    private static final DataParameter<Float> CHARGE_PROGRESS =
            EntityDataManager.defineId(EnhancementLightEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<CompoundNBT> RELEASE_TRANSFORM =
            EntityDataManager.defineId(EnhancementLightEntity.class, DataSerializers.COMPOUND_TAG);
    private EnhancementBlastHitbox releasedHitbox;
    private LivingEntity owner;
    private EnhancementAbility sourceAbility;
    private final Set<UUID> hitTargets = new HashSet<>();
    private List<LivingEntity> targetsInBlast = Collections.emptyList();

    public EnhancementLightEntity(EntityType<? extends EnhancementLightEntity> type, World world) {
        super(type, world);
        this.noPhysics = true;
        this.noCulling = true;
        this.setNoGravity(true);
    }

    public void beginCharge(LivingEntity caster, EnhancementAbility ability) {
        this.owner = caster;
        this.sourceAbility = ability;
        followCaster(caster);
        this.entityData.set(PHASE_START, (int) this.level.getGameTime());
        refreshBounds();
    }

    public void setChargeProgress(float progress) {
        this.entityData.set(CHARGE_PROGRESS, Math.max(0.0F, Math.min(1.0F, progress)));
    }

    public float getChargeProgress() { return this.entityData.get(CHARGE_PROGRESS); }

    public boolean isReleased() { return this.entityData.get(RELEASED); }

    public float getPhaseAge(float partialTick) {
        int elapsed = (int) this.level.getGameTime() - this.entityData.get(PHASE_START);
        return Math.max(0.0F, elapsed + partialTick);
    }

    public void release(LivingEntity caster) {
        if (this.level.isClientSide || isReleased()) return;
        followCaster(caster);
        // Publish position and yaw together so clients cannot mix a new release
        // direction with the previous charge position from a movement packet.
        CompoundNBT transform = new CompoundNBT();
        transform.putDouble("x", getX());
        transform.putDouble("y", getY());
        transform.putDouble("z", getZ());
        transform.putFloat("yaw", this.yRot);
        this.entityData.set(RELEASE_TRANSFORM, transform);
        this.releasedHitbox = new EnhancementBlastHitbox(getX(), getY(), getZ(), this.yRot);
        this.entityData.set(PHASE_START, (int) this.level.getGameTime());
        this.entityData.set(RELEASED, true);
        refreshBounds();
        updateHitboxTargets(); // The visible deployment determines how much of the blast can hit.
    }

    private void followCaster(LivingEntity caster) {
        this.moveTo(caster.getX(), caster.getY(), caster.getZ(), caster.yRot, 0.0F);
        this.yRotO = this.yRot;
        this.xRotO = this.xRot;
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(Vector3d.ZERO);
        if (!this.level.isClientSide) {
            if (isReleased()) {
                if (getPhaseAge(0.0F) >= BLAST_TICKS) {
                    this.targetsInBlast = Collections.emptyList();
                    this.remove();
                    return;
                }
                updateHitboxTargets();
            } else {
                if (!isOwnerCharging()) {
                    this.remove();
                    return;
                }
                followCaster(this.owner);
            }
        }
        refreshBounds();
    }

    private boolean isOwnerCharging() {
        if (this.owner == null || !this.owner.isAlive() || this.owner.level != this.level) return false;
        IAbilityData data = AbilityDataCapability.get(this.owner);
        EnhancementAbility ability = data == null ? null : data.getEquippedAbility(EnhancementAbility.INSTANCE);
        return ability != null && ability.isChargingLight(this);
    }

    private EnhancementBlastHitbox blastHitbox() {
        if (this.releasedHitbox != null) return this.releasedHitbox;
        CompoundNBT transform = this.entityData.get(RELEASE_TRANSFORM);
        if (!transform.isEmpty()) {
            this.releasedHitbox = new EnhancementBlastHitbox(transform.getDouble("x"),
                    transform.getDouble("y"), transform.getDouble("z"), transform.getFloat("yaw"));
            return this.releasedHitbox;
        }
        // Charging has no damaging volume and continues to follow the caster.
        return new EnhancementBlastHitbox(getX(), getY(), getZ(), this.yRot);
    }

    public Vector3d getBlastOrigin() { return isReleased() ? blastHitbox().getOrigin() : position(); }

    public float getBlastYaw() { return isReleased() ? blastHitbox().getYaw() : this.yRot; }

    private EnhancementBlastHitbox activeBlastHitbox() {
        EnhancementBlastHitbox hitbox = blastHitbox();
        hitbox.update(getPhaseAge(0) / EFFECT_DURATION_SCALE, getId());
        return hitbox;
    }

    /** The animated mesh changes shape inside the fixed release position and direction. */
    public AxisAlignedBB getBlastBounds() { return activeBlastHitbox().getBounds(); }

    /** Test the actual main blast and closed eruption lobes, including their empty gaps. */
    public boolean intersectsBlast(AxisAlignedBB target) {
        return isReleased() && activeBlastHitbox().intersects(target);
    }

    private void updateHitboxTargets() {
        this.targetsInBlast = this.level.getEntitiesOfClass(LivingEntity.class, getBlastBounds(),
                target -> target != this.owner && target.isAlive() && !target.isSpectator()
                        && intersectsBlast(target.getBoundingBox()));
        if (this.owner == null || this.sourceAbility == null) return;
        Vector3d launchDirection = blastHitbox().getForward();
        for (LivingEntity target : this.targetsInBlast) {
            // Record the first contact for this cast, including resisted hits.
            if (!this.hitTargets.add(target.getUUID())) continue;
            if (this.sourceAbility.hitThirdBlast(this.owner, target)) {
                AbilityHelper.setDeltaMovement(target,
                        launchDirection.x * LAUNCH_FORWARD_SPEED, LAUNCH_UPWARD_SPEED,
                        launchDirection.z * LAUNCH_FORWARD_SPEED);
            }
        }
    }

    public List<LivingEntity> getTargetsInBlast() {
        return Collections.unmodifiableList(this.targetsInBlast);
    }

    private void refreshBounds() {
        this.setBoundingBox(isReleased() ? getBlastBounds()
                : new AxisAlignedBB(getX() - 4, getY(), getZ() - 4,
                        getX() + 4, getY() + 16, getZ() + 4));
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(RELEASED, false);
        this.entityData.define(PHASE_START, 0);
        this.entityData.define(CHARGE_PROGRESS, 0.0F);
        this.entityData.define(RELEASE_TRANSFORM, new CompoundNBT());
    }

    @Override
    public void onSyncedDataUpdated(DataParameter<?> parameter) {
        super.onSyncedDataUpdated(parameter);
        if (RELEASE_TRANSFORM.equals(parameter)) this.releasedHitbox = null;
        if (RELEASE_TRANSFORM.equals(parameter) || RELEASED.equals(parameter)) refreshBounds();
    }

    @Override
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport) {
        if (isReleased() && !this.entityData.get(RELEASE_TRANSFORM).isEmpty()) {
            EnhancementBlastHitbox blast = blastHitbox();
            Vector3d origin = blast.getOrigin();
            super.lerpTo(origin.x, origin.y, origin.z, blast.getYaw(), 0, steps, teleport);
        } else {
            super.lerpTo(x, y, z, yaw, pitch, steps, teleport);
        }
        // Base interpolation resets the box to the registered 1x1 entity size.
        refreshBounds();
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT tag) { }

    @Override
    protected void addAdditionalSaveData(CompoundNBT tag) { }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean hurt(DamageSource source, float amount) { return false; }

    @Override
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public boolean shouldRenderAtSqrDistance(double distance) { return distance < 192.0D * 192.0D; }

    @Override
    public IPacket<?> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
