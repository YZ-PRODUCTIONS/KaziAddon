package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.abilities.GoruRework.EnkiduAbility;
import net.kazi.kazimod.abilities.GoruRework.GateOfBabylonAbility;
import net.kazi.kazimod.entities.BabylonImpactEntity;
import net.kazi.kazimod.init.KaziEntities;
import net.kazi.kazimod.seiryu.SeiryuPartEntity;
import net.kazi.kazimod.worldturtle.WorldTurtlePartEntity;
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
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.api.events.ProjectileHitEvent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

/** Treasure weapons pierce entities using the normal damage/dodge pipeline. */
@Mod.EventBusSubscriber(modid = "kazimod")
public final class BabylonWeaponEntity extends AbilityProjectileEntity {
    public static final int LIFETIME_TICKS = 30 * 20;
    private static final BabylonHitCooldown<LivingEntity> SWORD_HITS = new BabylonHitCooldown<>();
    private static final DataParameter<Integer> VARIANT = EntityDataManager.defineId(BabylonWeaponEntity.class, DataSerializers.INT);
    private int flightTicks;
    private int lastEntityImpactTick = -2;
    private boolean hitLanded;
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
        onEntityImpactEvent = this::onSuccessfulHit;
    }

    // MMNM posts this before applying damage. Let defenses such as Imagine Breaker
    // intercept the sword first, even while the target is on cooldown.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSwordContact(ProjectileHitEvent event) {
        if (!(event.getProjectile() instanceof BabylonWeaponEntity)
                || !(event.getHit() instanceof EntityRayTraceResult)) return;
        BabylonWeaponEntity sword = (BabylonWeaponEntity) event.getProjectile();
        if (sword.level.isClientSide) return;
        LivingEntity enemy = damageRecipient(((EntityRayTraceResult) event.getHit()).getEntity());
        if (enemy != null && SWORD_HITS.blocks(enemy, sword.getId(), sword.level.getGameTime())) {
            event.setCanceled(true);
        }
    }

    private void onSuccessfulHit(LivingEntity target) {
        hitLanded = true;
        LivingEntity enemy = damageRecipient(target);
        if (enemy != null) SWORD_HITS.record(enemy, getId(), level.getGameTime());
    }

    // Different body parts of a single enemy share its timer.
    private static LivingEntity damageRecipient(Entity target) {
        if (target instanceof PartEntity<?>) target = ((PartEntity<?>) target).getParent();
        if (target instanceof SeiryuPartEntity) return ((SeiryuPartEntity) target).owner();
        if (target instanceof WorldTurtlePartEntity) return ((WorldTurtlePartEntity) target).owner();
        return target instanceof LivingEntity ? (LivingEntity) target : null;
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
        hitLanded = false;
        super.onModHit(hit);
        if (!hitLanded && hit instanceof EntityRayTraceResult
                && damageRecipient(((EntityRayTraceResult) hit).getEntity()) != null) return;
        boolean blockImpact = hit.getType() == RayTraceResult.Type.BLOCK;
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
        if (blockImpact) remove();
    }
}
