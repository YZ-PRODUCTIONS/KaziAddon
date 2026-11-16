package net.kazi.kazimod.entities;

import net.kazi.kazimod.abilities.NagiAwakening.DragonKind;
import net.kazi.kazimod.abilities.NagiAwakening.DragonVfxPhase;
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
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

/** One bounded visual event per attack. Animated geometry is generated entirely on the client. */
public final class DragonAttackVfxEntity extends Entity {
    private static final DataParameter<CompoundNBT> STATE =
            EntityDataManager.defineId(DragonAttackVfxEntity.class, DataSerializers.COMPOUND_TAG);
    private DragonStrikeEntity dragon;
    private LivingEntity followTarget;

    public DragonAttackVfxEntity(EntityType<? extends DragonAttackVfxEntity> type, World world) {
        super(type, world);
        noPhysics = true;
        setNoGravity(true);
        setInvulnerable(true);
    }

    public static DragonAttackVfxEntity spawn(DragonStrikeEntity dragon, DragonVfxPhase phase,
                                              Vector3d origin, Vector3d end) {
        return spawn(dragon, phase, origin, end, phase.lifetime, null);
    }

    public static DragonAttackVfxEntity spawn(DragonStrikeEntity dragon, DragonVfxPhase phase,
                                              Vector3d origin, Vector3d end, int lifetime,
                                              LivingEntity followTarget) {
        if (dragon.level.isClientSide || !finite(origin) || !finite(end)) return null;
        DragonAttackVfxEntity visual = new DragonAttackVfxEntity(KaziEntities.DRAGON_ATTACK_VFX.get(), dragon.level);
        visual.dragon = dragon;
        visual.followTarget = followTarget;
        visual.setPos(origin.x, origin.y, origin.z);
        Vector3d delta = end.subtract(origin);
        float length = (float) Math.min(64, delta.length());
        Vector3d direction = delta.lengthSqr() > .000001 ? delta.normalize() : dragon.getLookAngle();
        // The crystal cage, binding ribbons and ground impact use their own upright frame.
        if (phase == DragonVfxPhase.BIND || phase == DragonVfxPhase.SLAM
                || phase == DragonVfxPhase.SPECIAL && dragon.getKind() == DragonKind.ICE_CRYSTAL) {
            direction = new Vector3d(0, 1, 0);
            length = 0;
        }
        CompoundNBT state = new CompoundNBT();
        state.putLong("start", dragon.level.getGameTime());
        state.putInt("kind", dragon.getKind().ordinal());
        state.putInt("phase", phase.ordinal());
        state.putInt("life", Math.max(1, Math.min(60, lifetime)));
        state.putInt("seed", dragon.getId() * 31 + dragon.tickCount);
        state.putInt("dragon", dragon.getId());
        state.putInt("follow", followTarget == null ? -1 : followTarget.getId());
        state.putFloat("length", length);
        state.putFloat("x", (float) direction.x);
        state.putFloat("y", (float) direction.y);
        state.putFloat("z", (float) direction.z);
        visual.entityData.set(STATE, state);
        return dragon.level.addFreshEntity(visual) ? visual : null;
    }

    private static boolean finite(Vector3d point) {
        return Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }

    public boolean isReady() { return entityData.get(STATE).contains("life"); }
    public DragonKind getKind() { return DragonKind.byId(entityData.get(STATE).getInt("kind")); }
    public DragonVfxPhase getPhase() { return DragonVfxPhase.byId(entityData.get(STATE).getInt("phase")); }
    public int getLifetime() { return entityData.get(STATE).getInt("life"); }
    public float getLength() { return entityData.get(STATE).getFloat("length"); }
    public int getSeed() { return entityData.get(STATE).getInt("seed"); }
    public float getVisualAge(float partial) {
        return isReady() ? Math.max(0, level.getGameTime() - entityData.get(STATE).getLong("start") + partial) : 0;
    }
    public Vector3d getVisualDirection(float partial) {
        CompoundNBT state = entityData.get(STATE);
        if (level.isClientSide && getPhase() == DragonVfxPhase.CHARGE) {
            Entity source = level.getEntity(state.getInt("dragon"));
            if (source instanceof DragonStrikeEntity) return source.getViewVector(partial);
        }
        return new Vector3d(state.getFloat("x"), state.getFloat("y"), state.getFloat("z"));
    }

    @Override public void tick() {
        super.tick();
        if (level.isClientSide) {
            // Follow the already-synchronized actors locally for smooth bindings without extra packets.
            if (isReady()) {
                boolean charge = getPhase() == DragonVfxPhase.CHARGE;
                Entity anchor = level.getEntity(entityData.get(STATE).getInt(charge ? "dragon" : "follow"));
                if (anchor instanceof LivingEntity && !anchor.removed) {
                    Vector3d point = charge ? anchor.getEyePosition(1) : anchor.getBoundingBox().getCenter();
                    setPos(point.x, point.y, point.z);
                }
            }
            return;
        }
        if (dragon == null || dragon.removed || !dragon.isAlive() || getVisualAge(0) >= getLifetime()) {
            remove();
            return;
        }
        if (getPhase() == DragonVfxPhase.CHARGE) {
            Vector3d mouth = dragon.getEyePosition(1);
            setPos(mouth.x, mouth.y, mouth.z);
        } else if (followTarget != null && (getPhase() == DragonVfxPhase.BIND
                || getPhase() == DragonVfxPhase.SPECIAL && getKind() == DragonKind.ICE_CRYSTAL)) {
            if (!followTarget.isAlive() || followTarget.removed || followTarget.level != level) { remove(); return; }
            if (getKind() == DragonKind.ICE_CRYSTAL && !followTarget.hasEffect(ModEffects.FROZEN.get())) { remove(); return; }
            Vector3d center = followTarget.getBoundingBox().getCenter();
            setPos(center.x, center.y, center.z);
        }
    }

    @Override protected void defineSynchedData() { entityData.define(STATE, new CompoundNBT()); }
    @Override protected void readAdditionalSaveData(CompoundNBT tag) { remove(); }
    @Override protected void addAdditionalSaveData(CompoundNBT tag) { }
    @Override public boolean isPickable() { return false; }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 160 * 160; }
    @Override public IPacket<?> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
