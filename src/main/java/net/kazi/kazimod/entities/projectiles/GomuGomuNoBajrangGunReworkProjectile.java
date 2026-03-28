package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.abilities.GomuRework.GomuGomuNoRedRocAbility;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.particles.effects.CommonExplosionParticleEffect;

public class GomuGomuNoBajrangGunReworkProjectile extends AbilityProjectileEntity {

    private static final float HITBOX_BASE_OFFSET        = 15.0F;
    private static final float HITBOX_STRETCH_MULTIPLIER = 3.5F;
    private static final float STRETCH_SCALE_Z           = 3.1F;

    // Base explosion radius — will be scaled by getSize() just like Dai Entei uses 0.6F * size
    private static final float EXPLOSION_RADIUS_MULTIPLIER = 0.3F;  // was 0.6F
    // Base static damage — also scaled by getSize()
    private static final float DAMAGE_MULTIPLIER           = 3.0F;

    public GomuGomuNoBajrangGunReworkProjectile(EntityType type, World world) {
        super(type, world);
    }

    public GomuGomuNoBajrangGunReworkProjectile(World world, LivingEntity player) {
        super((EntityType) BigGomuReworkProjectiles.GOMU_GOMU_NO_BAJRANG_GUN_REWORK.get(), world, player, GomuGomuNoRedRocAbility.INSTANCE);
        this.setDamage(170.0F);
        this.setMaxLife(30);
        // Raised from 100k to match Dai Entei's 42875 — but since Bajrang is bigger,
        // keep it high so the destruction isn't prematurely capped.
        this.setBlocksAffectedLimit(50000);  // was 200000
        this.setArmorPiercing(0.75F);
        this.setPassThroughEntities();
        this.setUnavoidable();
        super.setFist();
        this.setEntityCollisionSize(50.0F);
        this.setDamageSource(this.getDamageSource().setSourceElement(SourceElement.RUBBER));
        this.onBlockImpactEvent = this::onBlockImpact;
        this.onEntityImpactEvent = this::onEntityHit;
    }

    /**
     * Mirrors DaiEnkaiEntei's getSize() — bounding box size scaled up.
     * AbilityProjectileEntity stores the collision size in the bounding box,
     * so getBoundingBox().getSize() returns the diameter (≈50 at full size).
     * Multiply by a tuning factor to get a useful explosion scalar.
     */
    public float getSize() {
        return (float) super.getBoundingBox().getSize() * 4.0F;
    }

    private void onBlockImpact(BlockPos hit) {
        ExplosionAbility explosion = super.createExplosion(
                this.getThrower(),
                this.level,
                (double) hit.getX(),
                (double) hit.getY(),
                (double) hit.getZ(),
                EXPLOSION_RADIUS_MULTIPLIER * this.getSize()  // scales with fist size, same pattern as Dai Entei
        );
        explosion.setStaticDamage(DAMAGE_MULTIPLIER * this.getSize());
        // Lower resistance value = more blocks destroyed (0.0 destroys everything, 1.0 nothing)
        explosion.setStaticBlockResistance(0.15F);            // slightly more destructive than Dai Entei's 0.25F
        explosion.setFireAfterExplosion(false);               // Bajrang Gun is rubber, not fire — toggle true if you want Haki fire
        explosion.setSmokeParticles(new CommonExplosionParticleEffect((int)(EXPLOSION_RADIUS_MULTIPLIER * this.getSize())));
        explosion.doExplosion();
    }

    private void onEntityHit(LivingEntity target) {
        // passthrough — damage handled by base class
    }

    @Override
    public void tick() {
        if (!this.level.isClientSide) {
            Vector3d motion = this.getDeltaMovement();
            double speed = motion.length();
            if (speed > 0.001) {
                Vector3d dir = motion.normalize();
                float hitboxOffset = HITBOX_BASE_OFFSET + (STRETCH_SCALE_Z * (float) speed * HITBOX_STRETCH_MULTIPLIER);
                this.setPos(
                        this.getX() + dir.x * hitboxOffset,
                        this.getY() + dir.y * hitboxOffset,
                        this.getZ() + dir.z * hitboxOffset
                );
            }
        }

        super.tick();
    }
}