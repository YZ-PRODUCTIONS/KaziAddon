package net.kazi.kazimod.entities.asriel;

import net.kazi.kazimod.abilities.NagiAwakening.ImagineBreakerNegation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.kazi.kazimod.abilities.YagiAwakening.ChaosBusterAbility;
import net.kazi.kazimod.abilities.YagiAwakening.ChaosBusterPattern;
import net.kazi.kazimod.abilities.YagiAwakening.ChaosBusterSounds;
import net.kazi.kazimod.abilities.YagiAwakening.ChaosSaberAbility;
import net.kazi.kazimod.abilities.YagiAwakening.ChaosSaberPattern;
import net.kazi.kazimod.abilities.YagiAwakening.ChaosSaberSounds;
import net.kazi.kazimod.abilities.YagiAwakening.YagiAwakeningSupport;
import net.kazi.kazimod.abilities.YagiAwakening.YagiAwakeningMath;
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
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkHooks;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;

/** The synchronized swords, charge aura and beam share finite, server-owned lifetimes. */
public final class AsrielChaosFieldEntity extends Entity {
    public static final int SABER = 0;
    public static final int BEAM = 1;
    public static final int CHARGE = 2;
    private static final DataParameter<CompoundNBT> STATE = EntityDataManager.defineId(AsrielChaosFieldEntity.class, DataSerializers.COMPOUND_TAG);
    private static final DataParameter<Boolean> DARK_PALETTE = EntityDataManager.defineId(AsrielChaosFieldEntity.class, DataSerializers.BOOLEAN);
    // A superseded field must never stop a new cast's sound on the same player.
    private static final Map<LivingEntity, Integer> SOUND_FIELDS = new WeakHashMap<>();
    private static final Map<LivingEntity, Integer> SABER_SOUND_FIELDS = new WeakHashMap<>();
    private final Set<UUID> beamHits = new HashSet<>();
    private LivingEntity owner;
    private boolean expiredAfterLoad;

    public AsrielChaosFieldEntity(EntityType<? extends AsrielChaosFieldEntity> type, World world) {
        super(type, world);
        noPhysics = true;
        noCulling = true;
        setNoGravity(true);
    }

    public static AsrielChaosFieldEntity spawn(LivingEntity user, int kind, boolean enhanced,
                                               Vector3d origin, Vector3d direction, int duration) {
        if (!(user.level instanceof ServerWorld)) return null;
        AsrielChaosFieldEntity field = new AsrielChaosFieldEntity(KaziEntities.ASRIEL_CHAOS_FIELD.get(), user.level);
        field.owner = user;
        field.entityData.set(DARK_PALETTE, YagiAwakeningSupport.enhanced(user));
        field.setPos(origin.x, origin.y, origin.z);
        Vector3d forward = direction.lengthSqr() > 0.0001D ? direction.normalize() : new Vector3d(0, 0, 1);
        double length = kind == SABER ? ChaosSaberPattern.LENGTH
                : kind == BEAM ? ChaosBusterPattern.BEAM_RANGE : 48.0D;
        if (kind == BEAM) {
            RayTraceResult hit = user.level.clip(new RayTraceContext(origin, origin.add(forward.scale(length)),
                    RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, user));
            if (hit.getType() != RayTraceResult.Type.MISS) length = origin.distanceTo(hit.getLocation());
        }
        CompoundNBT state = new CompoundNBT();
        state.putInt("kind", kind);
        state.putInt("owner", user.getId());
        state.putLong("start", user.level.getGameTime());
        state.putInt("duration", duration);
        state.putBoolean("enhanced", enhanced);
        state.putDouble("dx", forward.x); state.putDouble("dy", forward.y); state.putDouble("dz", forward.z);
        state.putDouble("length", length);
        double radius = kind == SABER ? ChaosSaberPattern.SPARK_RADIUS
                : enhanced ? 3.2D : ChaosBusterPattern.BEAM_RADIUS;
        if (kind == BEAM && enhanced) radius = ChaosBusterPattern.BLASTER_BEAM_RADIUS;
        state.putDouble("radius", radius);
        field.entityData.set(STATE, state);
        if (user.level.addFreshEntity(field)) {
            if (kind == CHARGE || kind == BEAM) SOUND_FIELDS.put(user, field.getId());
            else if (kind == SABER) SABER_SOUND_FIELDS.put(user, field.getId());
        } else field.remove();
        return field;
    }

    public static Vector3d muzzlePosition(LivingEntity user, Vector3d direction) {
        Vector3d forward = direction.normalize();
        Vector3d side = new Vector3d(forward.z, 0.0D, -forward.x);
        if (side.lengthSqr() < 0.0001D) {
            double yaw = Math.toRadians(user.yRot);
            side = new Vector3d(Math.cos(yaw), 0.0D, Math.sin(yaw));
        } else side = side.normalize();
        Vector3d eye = user.getEyePosition(1);
        Vector3d desired = eye.add(forward.scale(2.5D)).add(side).add(0.0D, -0.5D, 0.0D);
        RayTraceResult hit = user.level.clip(new RayTraceContext(eye, desired,
                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, user));
        return hit.getType() == RayTraceResult.Type.MISS ? desired
                : hit.getLocation().subtract(desired.subtract(eye).normalize().scale(0.15D));
    }

    public void updateChargePose(int age) {
        if (level.isClientSide || owner == null || getKind() != CHARGE) return;
        if (!isEnhanced() && age >= ChaosBusterPattern.AIM_LOCK_TICK) return;
        Vector3d forward = owner.getLookAngle().normalize();
        Vector3d at = muzzlePosition(owner, forward);
        setPos(at.x, at.y, at.z);
        if (getAttackDirection().distanceToSqr(forward) > 0.00000001D) {
            CompoundNBT state = entityData.get(STATE).copy();
            state.putDouble("dx", forward.x); state.putDouble("dy", forward.y); state.putDouble("dz", forward.z);
            entityData.set(STATE, state);
        }
    }

    @Override public void tick() {
        super.tick();
        if (level.isClientSide) return;
        if (expiredAfterLoad || owner == null || !owner.isAlive() || owner.removed || owner.level != level
                || getVisualAge(0) >= getDuration()) { remove(); return; }
        entityData.set(DARK_PALETTE, YagiAwakeningSupport.enhanced(owner));
        int age = (int) getVisualAge(0);
        if (getKind() == CHARGE) {
            IAbilityData data = AbilityDataCapability.get(owner);
            ChaosBusterAbility ability = data == null ? null : data.getEquippedAbility(ChaosBusterAbility.INSTANCE);
            if (ability == null || !ability.ownsCharge(this)) { remove(); return; }
            updateChargePose(age);
            return;
        }
        if (getKind() == BEAM) {
            if (ImagineBreakerNegation.negateBeam(this, owner, ChaosBusterAbility.INSTANCE,
                    position(), position().add(getAttackDirection().scale(getLength())), getCurrentRadius(0))) return;
            if (age % 2 == 0) damageLine(getAttackDirection(), getCurrentRadius(0), isEnhanced() ? 60.0F : 40.0F, beamHits, true);
        } else if (getKind() == SABER) tickSwords(age);
    }

    private void tickSwords(int age) {
        IAbilityData data = AbilityDataCapability.get(owner);
        ChaosSaberAbility ability = data == null ? null : data.getEquippedAbility(ChaosSaberAbility.INSTANCE);
        if (ability == null || !ability.ownsSwords(this)) { remove(); return; }
        if (age == ChaosSaberPattern.GRAB_TICK) ChaosSaberSounds.grab(owner, position());
        for (int i = 0; i < ChaosSaberPattern.SINGLE_SLASHES; i++) {
            if (age == ChaosSaberPattern.windupTick(i)) ChaosSaberSounds.pullback(owner, position());
            if (age == ChaosSaberPattern.slashTick(i)) {
                ChaosSaberSounds.slash(owner, position(), false);
            }
            if (age == ChaosSaberPattern.hitTick(i)) {
                // rotateYaw's sign is opposite the renderer's local +X yaw convention.
                damageLine(rotateYaw(getAttackDirection(), -ChaosSaberPattern.side(i) * 0.24D),
                        ChaosSaberPattern.SLASH_RADIUS, 24.0F, new HashSet<>(), false);
            }
        }
        if (age == ChaosSaberPattern.FINAL_CHARGE_TICK) ChaosSaberSounds.charge(owner, position());
        if (age == ChaosSaberPattern.FINAL_SLASH_TICK) {
            ChaosSaberSounds.slash(owner, position(), true);
        }
        if (age == ChaosSaberPattern.FINAL_HIT_TICK) {
            // One finishing strike per target, even where the two blades cross.
            Set<UUID> struck = new HashSet<>();
            damageLine(rotateYaw(getAttackDirection(), -0.24D), ChaosSaberPattern.SLASH_RADIUS, 24.0F, struck, false);
            damageLine(rotateYaw(getAttackDirection(), 0.24D), ChaosSaberPattern.SLASH_RADIUS, 24.0F, struck, false);
        }
        if (age == ChaosSaberPattern.SPARKLE_TICK) ChaosSaberSounds.sparkles(owner, position());
        if (age == ChaosSaberPattern.sparkDamageTick(0) || age == ChaosSaberPattern.sparkDamageTick(1)) {
            damageLine(getAttackDirection(), getRadius(), 6.0F, new HashSet<>(), false);
        }
    }

    private void damageLine(Vector3d direction, double radius, float damage, Set<UUID> alreadyHit, boolean beam) {
        Vector3d end = position().add(direction.scale(getLength()));
        AxisAlignedBB bounds = new AxisAlignedBB(position(), end).inflate(radius + 1.0D);
        int checked = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, bounds)) {
            if (++checked > 128) break;
            if (!YagiAwakeningSupport.isEnemy(owner, target) || alreadyHit.contains(target.getUUID())) continue;
            Vector3d center = target.getBoundingBox().getCenter();
            Vector3d relative = center.subtract(position());
            if (!YagiAwakeningMath.insideBeam(relative.x, relative.y, relative.z,
                    direction.x, direction.y, direction.z, getLength(), radius, target.getBbWidth() * 0.5D)
                    || !YagiAwakeningSupport.clearPath(owner, position(), center)) continue;
            alreadyHit.add(target.getUUID());
            YagiAwakeningSupport.hurt(owner, target, beam ? ChaosBusterAbility.INSTANCE : ChaosSaberAbility.INSTANCE, damage);
        }
    }

    private static Vector3d rotateYaw(Vector3d v, double angle) {
        return new Vector3d(v.x * Math.cos(angle) - v.z * Math.sin(angle), v.y,
                v.x * Math.sin(angle) + v.z * Math.cos(angle)).normalize();
    }

    public int getKind() { return entityData.get(STATE).getInt("kind"); }
    public int getDuration() { return entityData.get(STATE).getInt("duration"); }
    public boolean isEnhanced() { return entityData.get(STATE).getBoolean("enhanced"); }
    public boolean hasDarkPalette() { return entityData.get(DARK_PALETTE); }
    public double getLength() { return entityData.get(STATE).getDouble("length"); }
    public double getRadius() { return entityData.get(STATE).getDouble("radius"); }
    public double getCurrentRadius(float partial) {
        if (getKind() != BEAM) return getRadius();
        return getRadius() * ChaosBusterPattern.beamRadiusScale(getVisualAge(partial), getDuration());
    }
    public Vector3d getAttackDirection() {
        CompoundNBT data = entityData.get(STATE);
        return new Vector3d(data.getDouble("dx"), data.getDouble("dy"), data.getDouble("dz"));
    }
    public float getVisualAge(float partial) {
        return Math.max(0, level.getGameTime() - entityData.get(STATE).getLong("start") + partial);
    }
    public LivingEntity getCaster() {
        Entity caster = level.getEntity(entityData.get(STATE).getInt("owner"));
        return caster instanceof LivingEntity ? (LivingEntity) caster : null;
    }
    @Override public void remove() {
        if (!removed && !level.isClientSide && owner != null && getKind() == SABER) {
            if (Integer.valueOf(getId()).equals(SABER_SOUND_FIELDS.get(owner))) {
                SABER_SOUND_FIELDS.remove(owner);
                ChaosSaberSounds.stop(owner);
            }
            IAbilityData data = AbilityDataCapability.get(owner);
            ChaosSaberAbility ability = data == null ? null : data.getEquippedAbility(ChaosSaberAbility.INSTANCE);
            if (ability != null) ability.onSwordsRemoved(this, owner);
        }
        if (!removed && !level.isClientSide && owner != null && (getKind() == CHARGE || getKind() == BEAM)) {
            if (Integer.valueOf(getId()).equals(SOUND_FIELDS.get(owner))) {
                SOUND_FIELDS.remove(owner);
                ChaosBusterSounds.stop(owner);
            }
            IAbilityData data = AbilityDataCapability.get(owner);
            ChaosBusterAbility ability = data == null ? null : data.getEquippedAbility(ChaosBusterAbility.INSTANCE);
            if (ability != null) ability.onFieldRemoved(this, owner);
        }
        super.remove();
    }
    @Override protected void defineSynchedData() {
        entityData.define(STATE, new CompoundNBT());
        entityData.define(DARK_PALETTE, false);
    }
    @Override protected void readAdditionalSaveData(CompoundNBT tag) { expiredAfterLoad = true; }
    @Override protected void addAdditionalSaveData(CompoundNBT tag) { }
    @Override public boolean isPickable() { return false; }
    @Override public boolean hurt(DamageSource source, float damage) { return false; }
    @Override public IPacket<?> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 160.0D * 160.0D; }
}
