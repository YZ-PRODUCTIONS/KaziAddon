package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.abilities.GoruRework.EnkiduAbility;
import net.kazi.kazimod.abilities.GoruRework.GateOfBabylonAbility;
import net.kazi.kazimod.entities.BabylonImpactEntity;
import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.datasync.*;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

/** Treasure weapons pierce entities using the normal damage/dodge pipeline. */
public final class BabylonWeaponEntity extends AbilityProjectileEntity {
    public static final int LIFETIME_TICKS = 30 * 20;
    private static final DataParameter<Integer> VARIANT = EntityDataManager.defineId(BabylonWeaponEntity.class, DataSerializers.INT);
    private int flightTicks;
    private int lastEntityImpactTick = -2;
    public BabylonWeaponEntity(EntityType<? extends BabylonWeaponEntity> type, World world) {
        super(type, world);
        configure();
    }
    public BabylonWeaponEntity(LivingEntity owner, Vector3d origin, Vector3d aim, int variant) {
        super(KaziEntities.BABYLON_WEAPON.get(), owner.level, owner, GateOfBabylonAbility.INSTANCE,
                GateOfBabylonAbility.INSTANCE.getSourceElement(), GateOfBabylonAbility.INSTANCE.getSourceHakiNature(),
                GateOfBabylonAbility.INSTANCE.getSourceTypes());
        configure();
        entityData.set(VARIANT, variant);
        setPos(origin.x, origin.y, origin.z);
        Vector3d direction = aim.subtract(origin).normalize();
        if (direction.lengthSqr() < .001) direction = owner.getLookAngle();
        shoot(direction.x, direction.y, direction.z, 3.5F, 0);
    }
    private void configure() {
        setDamage(GateOfBabylonAbility.DAMAGE);
        setGravity(0); setNoGravity(true);
        setMaxLife(LIFETIME_TICKS);
        setPassThroughEntities();
        setEntityCollisionSize(1.1); setBlockCollisionSize(.5);
        setHurtTime(0);
    }
    @Override public void defineSynchedData() { super.defineSynchedData(); entityData.define(VARIANT, 0); }
    public int getVariant() { return entityData.get(VARIANT); }
    @Override public void tick() {
        if (!level.isClientSide) {
            LivingEntity owner = getThrower();
            if (owner == null || !owner.isAlive() || owner.level != level || ++flightTicks > LIFETIME_TICKS) { remove(); return; }
        }
        super.tick();
    }
    @Override public void onModHit(RayTraceResult hit) {
        if (level.isClientSide || removed || hit.getType() == RayTraceResult.Type.MISS) return;
        if (hit instanceof EntityRayTraceResult) {
            Entity target = ((EntityRayTraceResult) hit).getEntity();
            LivingEntity owner = getThrower();
            if (target == owner || owner == null || owner.isAlliedTo(target) || target.isAlliedTo(owner)) return;
            if (target instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) target;
                if (!EnkiduAbility.isEnemy(owner, living)) return;
                if (AbilityHelper.isDodging(living)) return;
            }
        }
        super.onModHit(hit);
        boolean blockImpact = hit.getType() == RayTraceResult.Type.BLOCK;
        // Dense piercing contacts share one flash every two ticks. Damage and
        // hit sounds still run for every accepted contact; the final wall flash
        // always appears, even directly after an entity contact in the same tick.
        if (blockImpact || tickCount - lastEntityImpactTick >= 2) {
            Vector3d outward = getDeltaMovement().scale(-1);
            if (hit instanceof BlockRayTraceResult) {
                net.minecraft.util.Direction face = ((BlockRayTraceResult) hit).getDirection();
                outward = new Vector3d(face.getStepX(), face.getStepY(), face.getStepZ());
            }
            BabylonImpactEntity.spawn(level, hit.getLocation(), outward,
                    blockImpact, getId() * 31 + tickCount);
            if (!blockImpact) lastEntityImpactTick = tickCount;
        }
        level.playSound(null, blockPosition(), SoundEvents.TRIDENT_HIT, SoundCategory.PLAYERS, .45F, 1.2F);
        if (hit.getType() == RayTraceResult.Type.BLOCK) remove();
    }
}
