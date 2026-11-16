package net.kazi.kazimod.entities.asriel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kazi.kazimod.abilities.YagiAwakening.ShockerBreakerAbility;
import net.kazi.kazimod.abilities.YagiAwakening.ShockerBreakerDimensions;
import net.kazi.kazimod.abilities.YagiAwakening.ShockerBreakerExSequence;
import net.kazi.kazimod.abilities.YagiAwakening.ShockerBreakerSounds;
import net.kazi.kazimod.abilities.YagiAwakening.ShockerBreakerVisualTiming;
import net.kazi.kazimod.abilities.YagiAwakening.YagiAwakeningSupport;
import net.kazi.kazimod.init.KaziEntities;
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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkHooks;

/** Each marker owns its fixed warning and strike; the charge effect schedules five fresh marking rounds. */
public final class AsrielLightningEntity extends Entity {
    public static final int CHARGE_TICKS = ShockerBreakerVisualTiming.CHARGE_TICKS;
    public static final int LIFETIME = ShockerBreakerVisualTiming.LIFETIME;
    private static final DataParameter<Integer> START_TIME = EntityDataManager.defineId(AsrielLightningEntity.class, DataSerializers.INT);
    private static final DataParameter<Boolean> ENHANCED = EntityDataManager.defineId(AsrielLightningEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> STRUCK = EntityDataManager.defineId(AsrielLightningEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> CHARGE_AURA = EntityDataManager.defineId(AsrielLightningEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> CASTER = EntityDataManager.defineId(AsrielLightningEntity.class, DataSerializers.INT);
    private static final DataParameter<Boolean> DARK_PALETTE = EntityDataManager.defineId(AsrielLightningEntity.class, DataSerializers.BOOLEAN);
    private UUID ownerId;
    private boolean armed;
    // Every pillar in either variant shares its cast's round clock, cancellation and hit budget.
    private ShockerBreakerExSequence sequence;
    private final List<AsrielLightningEntity> pendingMarkers = new ArrayList<>();

    public AsrielLightningEntity(EntityType<? extends AsrielLightningEntity> type, World world) {
        super(type, world);
        noPhysics = true;
        noCulling = true;
        setNoGravity(true);
        if (!world.isClientSide) entityData.set(START_TIME, (int) world.getGameTime());
    }

    public AsrielLightningEntity(World world, LivingEntity owner, Vector3d point, boolean enhanced) {
        this(KaziEntities.ASRIEL_LIGHTNING.get(), world);
        ownerId = owner.getUUID();
        entityData.set(CASTER, owner.getId());
        entityData.set(DARK_PALETTE, YagiAwakeningSupport.enhanced(owner));
        entityData.set(ENHANCED, enhanced);
        setPos(point.x, point.y + 0.04D, point.z);
    }

    /** One attached charge effect per cast, shared by the base and multi-target EX move. */
    public static AsrielLightningEntity chargeAura(World world, LivingEntity owner) {
        AsrielLightningEntity aura = new AsrielLightningEntity(world, owner, owner.position(), false);
        aura.entityData.set(CHARGE_AURA, true);
        return aura;
    }

    public void arm() {
        if (level.isClientSide) return;
        armed = true;
        for (AsrielLightningEntity marker : pendingMarkers) marker.arm();
        pendingMarkers.clear();
    }

    /** The charge aura owns the automatic sequence after the ability's initial charge completes. */
    public void startRegularSequence(LivingEntity owner) {
        startSequence(owner, false);
    }

    public void startEnhancedSequence(LivingEntity owner) {
        startSequence(owner, true);
    }

    private void startSequence(LivingEntity owner, boolean enhanced) {
        if (level.isClientSide || !isChargeAura() || sequence != null) return;
        entityData.set(ENHANCED, enhanced);
        sequence = new ShockerBreakerExSequence();
        markRound(owner, 0);
        // Preserve EX's original aimed fallback when no hostile enemy is available at cast time.
        if (enhanced && pendingMarkers.isEmpty()) {
            spawnMarker(owner, YagiAwakeningSupport.groundPoint(owner,
                    YagiAwakeningSupport.aimPoint(owner, 48.0D)));
        }
    }

    private void markRound(LivingEntity owner, int age) {
        if (!sequence.pollRound(age)) return;
        if (!isEnhanced()) {
            // Sample current crosshair aim for each pillar; existing warnings stay fixed.
            spawnMarker(owner, YagiAwakeningSupport.groundPoint(owner,
                    YagiAwakeningSupport.aimPoint(owner, 48.0D)));
            return;
        }
        int marked = 0;
        for (LivingEntity target : YagiAwakeningSupport.nearestTargets(owner, 48.0D, Integer.MAX_VALUE)) {
            if (!sequence.canMark(target.getUUID())) continue;
            // Snapshot each enemy here, on this round's tick, never from an older pillar's position.
            if (spawnMarker(owner, YagiAwakeningSupport.groundPoint(owner, target.position()))) {
                sequence.recordMark(target.getUUID());
                if (++marked >= ShockerBreakerExSequence.MAX_TARGETS) break;
            }
        }
    }

    private boolean spawnMarker(LivingEntity owner, Vector3d point) {
        AsrielLightningEntity marker = new AsrielLightningEntity(level, owner, point, isEnhanced());
        marker.sequence = sequence;
        if (!level.addFreshEntity(marker)) return false;
        if (armed) marker.arm();
        else pendingMarkers.add(marker);
        return true;
    }

    public void cancelCast() {
        if (!level.isClientSide && isChargeAura()) {
            if (sequence != null) sequence.cancel();
            for (AsrielLightningEntity marker : pendingMarkers) marker.remove();
            pendingMarkers.clear();
        }
        remove();
    }

    public boolean isChargeAura() { return entityData.get(CHARGE_AURA); }
    public boolean isEnhanced() { return entityData.get(ENHANCED); }
    public boolean hasDarkPalette() { return entityData.get(DARK_PALETTE); }
    public boolean hasStruck() { return entityData.get(STRUCK); }
    public float getRadius() { return ShockerBreakerDimensions.damageRadius(isEnhanced()); }
    public float getVisualAge(float partialTick) {
        return Math.max(0, (int) level.getGameTime() - entityData.get(START_TIME)) + partialTick;
    }
    public float getChargeFraction(float partialTick) {
        return MathHelper.clamp(getVisualAge(partialTick) / CHARGE_TICKS, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (isChargeAura() && level.isClientSide) {
            Entity caster = level.getEntity(entityData.get(CASTER));
            if (caster != null) setPos(caster.getX(), caster.getY(), caster.getZ());
        }
        if (!(level instanceof ServerWorld)) return;
        Entity ownerEntity = ownerId == null ? null : ((ServerWorld) level).getEntity(ownerId);
        if (!(ownerEntity instanceof LivingEntity) || !ownerEntity.isAlive() || ownerEntity.removed) {
            cancelCast();
            return;
        }
        int age = (int) getVisualAge(0.0F);
        entityData.set(DARK_PALETTE, YagiAwakeningSupport.enhanced((LivingEntity) ownerEntity));
        if (isChargeAura()) {
            setPos(ownerEntity.getX(), ownerEntity.getY(), ownerEntity.getZ());
            if (sequence == null) {
                if (age >= CHARGE_TICKS) remove();
            } else if (age > CHARGE_TICKS + 3 && !armed) {
                cancelCast();
            } else {
                markRound((LivingEntity) ownerEntity, age);
                // Rendering already hides the aura after two seconds; keep only its scheduler until round five.
                if (armed && sequence.isComplete()) remove();
            }
            return;
        }
        if (sequence != null && sequence.isCancelled()) { remove(); return; }
        if (age >= ShockerBreakerVisualTiming.lifetime(isEnhanced()) || age > CHARGE_TICKS + 3 && !armed) { remove(); return; }
        if (armed && age >= CHARGE_TICKS && !hasStruck()) strike((LivingEntity) ownerEntity);
    }

    private void strike(LivingEntity owner) {
        entityData.set(STRUCK, true);
        double radius = getRadius();
        float damage = isEnhanced() ? 30.0F : 24.0F;
        AxisAlignedBB area = new AxisAlignedBB(getX() - radius, getY() - ShockerBreakerDimensions.DAMAGE_BELOW, getZ() - radius,
                getX() + radius, getY() + ShockerBreakerDimensions.DAMAGE_HEIGHT, getZ() + radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area)) {
            double dx = target.getX() - getX(), dz = target.getZ() - getZ();
            if (dx * dx + dz * dz <= radius * radius && YagiAwakeningSupport.isEnemy(owner, target)
                    && YagiAwakeningSupport.clearPath(owner, position().add(0.0D, 0.35D, 0.0D), target.getBoundingBox().getCenter())) {
                if (sequence != null && sequence.isCancelled()) break;
                YagiAwakeningSupport.hurt(owner, target, ShockerBreakerAbility.INSTANCE, damage);
            }
        }
        // Each pillar plays once at impact; simultaneous pillars keep their own overlapping sound.
        ShockerBreakerSounds.strike(this, 0);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(START_TIME, 0);
        entityData.define(ENHANCED, false);
        entityData.define(STRUCK, false);
        entityData.define(CHARGE_AURA, false);
        entityData.define(CASTER, -1);
        entityData.define(DARK_PALETTE, false);
    }
    @Override protected void readAdditionalSaveData(CompoundNBT tag) { remove(); }
    @Override protected void addAdditionalSaveData(CompoundNBT tag) { }
    @Override public boolean isPickable() { return false; }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    // Keep the enlarged bolt visible when the camera is beside its top, far from the ground anchor.
    @Override public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < ShockerBreakerDimensions.RENDER_DISTANCE * ShockerBreakerDimensions.RENDER_DISTANCE;
    }
    @Override public IPacket<?> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
