package net.kazi.kazimod.entities.projectiles;

import java.util.List;

import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.api.damagesource.ModIndirectEntityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

/**
 * Kazi-owned copy of Cart Addon's Item Kaiten projectile.
 *
 * The defaults intentionally match CartAddon 0.7.5. Keeping them as named
 * constants makes the projectile's stats directly editable in this project.
 */
public class ItemKaitenReworkedProjectile extends AbilityProjectileEntity {

    public static final float DAMAGE = 50.0F;
    public static final int MAX_LIFE = 1200;
    public static final float ENTITY_WIDTH = 6.0F;
    public static final float ENTITY_HEIGHT = 6.0F;
    public static final double RENDER_SCALE = 24.0D;
    public static final double ORBIT_RADIUS = 8.0D;
    public static final double ORBIT_HEIGHT = 12.0D;
    public static final float ORBIT_SPEED = 5.0F;
    public static final float TOSSED_GRAVITY = 0.025F;
    public static final float BLOCK_EXPLOSION_SIZE = 12.0F;
    public static final double DAMAGE_RADIUS = 14.0D;
    public static final double KNOCKBACK_RADIUS = 17.0D;
    public static final int DIZZY_DURATION = 60;
    public static final float IMPACT_DAMAGE = 50.0F;
    public static final double KNOCKBACK_Y = 0.25D;

    private boolean tossed;
    private float orbitAngle;
    private float orbitOffset;

    public ItemKaitenReworkedProjectile(EntityType<?> type, World world) {
        super((EntityType) type, world);
    }

    public ItemKaitenReworkedProjectile(World world, LivingEntity thrower) {
        super((EntityType) KaziEntities.ITEM_KAITEN.get(), world, thrower);
        this.setDamage(DAMAGE);
        this.setMaxLife(MAX_LIFE);
        this.setCollideWithEntities(false);
        this.setCollideWithBlocks(false);
        this.setNoVelocityRotation();
        this.setNoGravity(true);
        this.setAffectedByHardening();
        this.setArmorPiercing(1.0F);
        this.setUnavoidable();
        this.onEntityImpactEvent = this::onEntityImpactEvent;
        this.onBlockImpactEvent = this::onHitBlock;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level.isClientSide && !this.tossed && this.getThrower() != null) {
            this.orbitAngle += ORBIT_SPEED;
            double angle = Math.toRadians(this.orbitAngle + this.orbitOffset);
            double x = this.getThrower().getX() + ORBIT_RADIUS * Math.cos(angle);
            double y = this.getThrower().getY() + ORBIT_HEIGHT;
            double z = this.getThrower().getZ() + ORBIT_RADIUS * Math.sin(angle);
            this.setPos(x, y, z);
            this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        } else if (this.tossed) {
            this.setNoGravity(false);
            this.setGravity(TOSSED_GRAVITY);
        }

    }

    private void onEntityImpactEvent(LivingEntity target) {
        if (this.tossed) {
            target.addEffect(new EffectInstance(ModEffects.DIZZY.get(), DIZZY_DURATION, 0));
        }
    }

    private void onHitBlock(BlockPos hit) {
        if (!this.tossed) {
            return;
        }

        ExplosionAbility explosion = this.createExplosion(
                this.getThrower(), this.level,
                hit.getX(), hit.getY(), hit.getZ(), BLOCK_EXPLOSION_SIZE);
        explosion.setStaticDamage(0.0F);
        explosion.doExplosion();

        List<LivingEntity> damageTargets = WyHelper.getNearbyLiving(
                this.position(), this.level, DAMAGE_RADIUS,
                ModEntityPredicates.getEnemyFactions(this.getThrower()));
        List<LivingEntity> knockbackTargets = WyHelper.getNearbyLiving(
                this.position(), this.level, KNOCKBACK_RADIUS,
                ModEntityPredicates.getEnemyFactions(this.getThrower()));

        ModDamageSource damageSource = (ModDamageSource) (new ModIndirectEntityDamageSource(
                this.getDamageSource().msgId, this, this.getThrower()))
                .setHakiNature(SourceHakiNature.HARDENING)
                .setUnavoidable()
                .setPiercing(1.0F);

        for (LivingEntity target : damageTargets) {
            target.hurtTime = target.invulnerableTime = 0;
            target.hurt(damageSource, IMPACT_DAMAGE);
            target.addEffect(new EffectInstance(ModEffects.DIZZY.get(), DIZZY_DURATION, 0));
        }

        for (LivingEntity target : knockbackTargets) {
            Vector3d speed = target.getLookAngle()
                    .multiply(-1.0D, -1.0D, -1.0D)
                    .multiply(1.0D, 0.0D, 1.0D);
            AbilityHelper.setDeltaMovement(target, speed.x, KNOCKBACK_Y, speed.z);
        }

        this.remove();
    }

    public void setTossed(boolean tossed) {
        this.tossed = tossed;
        if (tossed) {
            this.setCollideWithBlocks(true);
        }
    }

    public void setOrbitOffset(float orbitOffset) {
        this.orbitOffset = orbitOffset;
    }
}
