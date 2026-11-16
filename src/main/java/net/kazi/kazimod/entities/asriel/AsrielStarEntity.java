package net.kazi.kazimod.entities.asriel;

import net.kazi.kazimod.abilities.YagiAwakening.StarBlazingAbility;
import net.kazi.kazimod.abilities.YagiAwakening.StarBlazingSounds;
import net.kazi.kazimod.abilities.YagiAwakening.YagiAwakeningSupport;
import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

/** Giant falling stars burst into a small, fixed number of ordinary owned projectiles. */
public final class AsrielStarEntity extends AbilityProjectileEntity {
    private static final DataParameter<Boolean> SMALL = EntityDataManager.defineId(AsrielStarEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> ENHANCED = EntityDataManager.defineId(AsrielStarEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> FINISHER = EntityDataManager.defineId(AsrielStarEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> START_TIME = EntityDataManager.defineId(AsrielStarEntity.class, DataSerializers.INT);
    private static final DataParameter<Boolean> DARK_PALETTE = EntityDataManager.defineId(AsrielStarEntity.class, DataSerializers.BOOLEAN);
    private boolean impacted;

    public AsrielStarEntity(EntityType<? extends AsrielStarEntity> type, World world) {
        super(type, world);
        configure(false, false, false);
    }

    public AsrielStarEntity(World world, LivingEntity owner, boolean small, boolean enhanced) {
        this(world, owner, small, enhanced, false);
    }

    public AsrielStarEntity(LivingEntity owner, boolean small, boolean enhanced, boolean finisher) {
        this(owner.level, owner, small, enhanced, finisher);
    }

    public AsrielStarEntity(World world, LivingEntity owner, boolean small, boolean enhanced, boolean finisher) {
        super(KaziEntities.ASRIEL_STAR.get(), world, owner, StarBlazingAbility.INSTANCE);
        configure(small, enhanced, finisher);
        entityData.set(DARK_PALETTE, YagiAwakeningSupport.enhanced(owner));
        this.entityData.set(START_TIME, (int) world.getGameTime());
    }

    private void configure(boolean small, boolean enhanced, boolean finisher) {
        entityData.set(SMALL, small);
        entityData.set(ENHANCED, enhanced);
        entityData.set(FINISHER, !small && finisher);
        setDamage(small ? (enhanced ? 6.0F : 5.0F) : 0.0F);
        // MMNM's zero-damage fake projectile path dispatches a valid collision
        // without calling hurt(0). This keeps its cancel/block hooks intact and
        // lets the single explosion own all damage, even during target i-frames.
        if (!small) setFake();
        setMaxLife(small ? 40 : 80);
        // The main volley uses its scripted flight vector; gravity would desynchronize the cadence.
        setGravity(small ? 0.008F : 0.0F);
        setEntityCollisionSize(getVisualSize() * 0.5D);
        if (small) {
            setBlockCollisionSize(getVisualSize() * 0.28D);
        } else {
            // Keep the giant star broad against walls, but land close to the scripted ground impact time.
            double width = getVisualSize() * 0.5D;
            setBlockCollisionSize(width, 0.5D, width);
        }
        this.onEntityImpactEvent = target -> {
            if (!isSmallStar()) explode(position());
            else remove();
        };
        this.onBlockImpactEvent = block -> {
            if (!isSmallStar()) explode(new Vector3d(getX(), block.getY() + 1.05D, getZ()));
            else remove();
        };
    }

    @Override
    public void onModHit(RayTraceResult hit) {
        if (hit instanceof EntityRayTraceResult) {
            Entity target = ((EntityRayTraceResult) hit).getEntity();
            LivingEntity owner = getThrower();
            if (owner == null || !YagiAwakeningSupport.isEnemy(owner, target)) return;
        }
        // Preserve MMNM's collision events, damage source, dodge and nullification hooks.
        super.onModHit(hit);
    }

    private void explode(Vector3d point) {
        if (!(level instanceof ServerWorld) || impacted) return;
        impacted = true;
        LivingEntity owner = getThrower();
        if (owner == null || !owner.isAlive()) { remove(); return; }
        double radius = isFinisher() ? (isEnhanced() ? 15.0D : 12.0D) : (isEnhanced() ? 10.0D : 8.0D);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AxisAlignedBB(point, point).inflate(radius))) {
            Vector3d targetPoint = target.getBoundingBox().getCenter();
            if (YagiAwakeningSupport.isEnemy(owner, target) && targetPoint.distanceToSqr(point) <= radius * radius
                    && YagiAwakeningSupport.clearPath(owner, point, targetPoint)) {
                YagiAwakeningSupport.hurt(owner, target, StarBlazingAbility.INSTANCE, isEnhanced() ? 22.0F : 18.0F);
            }
        }
        ServerWorld world = (ServerWorld) level;
        AsrielStarBurstEntity.spawn(owner, point, isEnhanced(), isFinisher());
        world.sendParticles(ParticleTypes.FLASH, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        world.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 14, 0.6D, 0.5D, 0.6D, 0.12D);
        StarBlazingSounds.burst(owner, point, isFinisher());
        int count = isFinisher() ? (isEnhanced() ? 48 : 36) : (isEnhanced() ? 16 : 12);
        int rings = isFinisher() ? 2 : 1;
        int ringSize = count / rings;
        double phase = random.nextDouble() * Math.PI * 2.0D;
        for (int i = 0; i < count; i++) {
            int ring = i / ringSize;
            // Offset the second ring by half a spoke so fragments never share a flight path.
            double angle = phase + Math.PI * 2.0D * (i % ringSize) / ringSize + ring * Math.PI / ringSize;
            double speed = isFinisher() ? (ring == 0 ? 1.0D : 0.72D) : 0.85D;
            AsrielStarEntity fragment = new AsrielStarEntity(level, owner, true, isEnhanced());
            fragment.setPos(point.x, point.y + 0.15D, point.z);
            fragment.setDeltaMovement(Math.cos(angle) * speed, ring == 0 ? 0.12D : 0.18D,
                    Math.sin(angle) * speed);
            level.addFreshEntity(fragment);
        }
        remove();
    }

    @Override
    public void tick() {
        if (!level.isClientSide) {
            LivingEntity owner = getThrower();
            if (owner == null || !owner.isAlive() || owner.removed || owner.level != level
                    || !level.hasChunkAt(new BlockPos(position().add(getDeltaMovement())))) {
                remove();
                return;
            }
            entityData.set(DARK_PALETTE, YagiAwakeningSupport.enhanced(owner));
        }
        Vector3d flight = getDeltaMovement();
        super.tick();
        // ThrowableEntity applies drag even with zero gravity. Keep the main star's flight cadence
        // constant while retaining all normal collision hooks; fragments keep their natural drag.
        if (!isSmallStar() && !removed) setDeltaMovement(flight);
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(SMALL, false);
        entityData.define(ENHANCED, false);
        entityData.define(FINISHER, false);
        entityData.define(START_TIME, 0);
        entityData.define(DARK_PALETTE, false);
    }

    public boolean isSmallStar() { return entityData.get(SMALL); }
    public boolean isEnhanced() { return entityData.get(ENHANCED); }
    public boolean hasDarkPalette() { return entityData.get(DARK_PALETTE); }
    public boolean isFinisher() { return entityData.get(FINISHER); }
    public float getVisualSize() {
        if (isSmallStar()) return isEnhanced() ? 0.8F : 0.65F;
        return (isEnhanced() ? 8.0F : 6.4F) * (isFinisher() ? 2.0F : 1.0F);
    }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 160.0D * 160.0D; }
    public float getVisualAge(float partialTick) {
        return Math.max(0, (int) level.getGameTime() - entityData.get(START_TIME)) + partialTick;
    }

    @Override
    public void writeSpawnData(PacketBuffer buffer) {
        super.writeSpawnData(buffer);
        buffer.writeBoolean(isSmallStar());
        buffer.writeBoolean(isEnhanced());
        buffer.writeBoolean(isFinisher());
        buffer.writeInt(entityData.get(START_TIME));
        buffer.writeBoolean(hasDarkPalette());
    }

    @Override
    public void readSpawnData(PacketBuffer buffer) {
        super.readSpawnData(buffer);
        configure(buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean());
        entityData.set(START_TIME, buffer.readInt());
        entityData.set(DARK_PALETTE, buffer.readBoolean());
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT tag) {
        super.readAdditionalSaveData(tag);
        // Attacks are transient: a chunk reload must never recreate a rain or its damage.
        remove();
    }
}
