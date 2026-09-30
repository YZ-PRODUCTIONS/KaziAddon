package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.init.KaziParticleEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HollowPurpleProjectile extends AbilityProjectileEntity {

    public static final Map<UUID, HollowPurpleProjectile> ACTIVE_PROJECTILES = new HashMap<>();

    private static final int   MIN_AIR_TICKS           = 10;   // 1.5 seconds
    private static final float EXPLOSION_VISUAL_RADIUS  = 20.0F;
    private static final float EXPLOSION_DAMAGE         = 85.0F;
    private static final float EXPLOSION_KNOCKBACK_MULT = 5.0F;

    private int     ticksInAir = 0;
    private boolean detonated  = false;

    public HollowPurpleProjectile(EntityType type, World world) {
        super(type, world);
    }

    public HollowPurpleProjectile(World world, LivingEntity player, Ability ability) {
        super((EntityType) GojoProjectiles.HOLLOW_PURPLE.get(), world, player, ability);
        super.setPassThroughEntities();
        this.setDamage(60.0F);
        this.setMaxLife(200);
        this.setHurtTime(5);
        this.setUnavoidable();
        this.setBlocksAffectedLimit(40960);
        this.setEntityCollisionSize(10.0F, 10.0F, 10.0F);
        this.onEntityImpactEvent = this::onEntityImpactEvent;
        this.onBlockImpactEvent  = this::onBlockImpactEvent;
        this.onTickEvent         = this::onTickEvent;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide) {
            if (getThrower() != null && this.isAlive())
                ACTIVE_PROJECTILES.put(getThrower().getUUID(), this);
            if (!detonated) ticksInAir++;
        }
    }

    @Override
    public void remove() {
        if (!this.level.isClientSide && getThrower() != null)
            ACTIVE_PROJECTILES.remove(getThrower().getUUID());
        super.remove();
    }

    public void detonate() {
        if (!detonated && ticksInAir >= MIN_AIR_TICKS)
            doExplosion(this.blockPosition());
    }

    private void onEntityImpactEvent(LivingEntity hitEntity) {
        Vector3d direction = this.getDeltaMovement().normalize();
        AbilityHelper.setDeltaMovement(hitEntity, direction.x * 5.0, 2.5, direction.z * 5.0);
        if (!detonated) doExplosion(this.blockPosition());
    }

    private void onBlockImpactEvent(BlockPos hit) {
        if (!detonated) doExplosion(hit);
    }

    private void doExplosion(BlockPos pos) {
        detonated = true;

        if (!this.level.isClientSide) {
            net.kazi.kazimod.entities.KokuVfxEntity.impact(this.level, Vector3d.atCenterOf(pos),
                    net.kazi.kazimod.entities.KokuVfxEntity.PURPLE_IMPACT, EXPLOSION_VISUAL_RADIUS);
            // Flashbang nearby players
            for (ServerPlayerEntity player : ((ServerWorld) this.level).players()) {
                if (player.distanceTo(this) <= 80.0F)
                    net.kazi.kazimod.init.KaziPacketHandler.sendFlashbang(player);
            }

            // Block destruction — carve a true sphere matching the visual radius
            AbilityHelper.createSphere(this.level, pos,
                    (int) EXPLOSION_VISUAL_RADIUS, (int) EXPLOSION_VISUAL_RADIUS,
                    false, net.minecraft.block.Blocks.AIR, 3,
                    new xyz.pixelatedw.mineminenomi.api.protection.BlockProtectionRule.Builder(
                            new xyz.pixelatedw.mineminenomi.api.protection.BlockProtectionRule[]{
                                    xyz.pixelatedw.mineminenomi.api.protection.block.AirBlockProtectionRule.INSTANCE,
                                    xyz.pixelatedw.mineminenomi.api.protection.block.LiquidBlockProtectionRule.INSTANCE,
                                    xyz.pixelatedw.mineminenomi.api.protection.block.CoreBlockProtectionRule.INSTANCE,
                                    xyz.pixelatedw.mineminenomi.api.protection.block.FoliageBlockProtectionRule.INSTANCE,
                                    xyz.pixelatedw.mineminenomi.api.protection.block.OreBlockProtectionRule.INSTANCE
                            }).build());

            // Manual entity damage in a sphere that exactly matches the visual radius
            Vector3d center = new Vector3d(pos.getX(), pos.getY(), pos.getZ());
            AxisAlignedBB box = new AxisAlignedBB(
                    center.x - EXPLOSION_VISUAL_RADIUS, center.y - EXPLOSION_VISUAL_RADIUS, center.z - EXPLOSION_VISUAL_RADIUS,
                    center.x + EXPLOSION_VISUAL_RADIUS, center.y + EXPLOSION_VISUAL_RADIUS, center.z + EXPLOSION_VISUAL_RADIUS);

            for (Entity e : this.level.getEntities(this, box, ent -> ent.isAlive() && ent instanceof LivingEntity)) {
                if (e.position().distanceTo(center) > EXPLOSION_VISUAL_RADIUS) continue;
                LivingEntity target = (LivingEntity) e;
                target.hurtTime = target.invulnerableTime = 0;
                target.hurt(DamageSource.MAGIC, EXPLOSION_DAMAGE);
                Vector3d dir = target.position().subtract(center).normalize();
                AbilityHelper.setDeltaMovement(target,
                        dir.x * EXPLOSION_KNOCKBACK_MULT, 2.5, dir.z * EXPLOSION_KNOCKBACK_MULT);
            }
        }

        if (getThrower() != null)
            net.kazi.kazimod.abilities.Koku.HollowPurpleAbility.triggerCooldownForEntity(getThrower());

        this.remove();
    }

    private void onTickEvent() {
        if (!this.level.isClientSide) {
            // Flight visuals are handled by KokuProjectileRenderer.
        }
    }
}
