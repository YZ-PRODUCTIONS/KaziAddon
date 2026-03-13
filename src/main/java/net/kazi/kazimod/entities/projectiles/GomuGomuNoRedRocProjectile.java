package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.abilities.GomuRework.GomuGomuNoRedRocAbility;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.particles.effects.CommonExplosionParticleEffect;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class GomuGomuNoRedRocProjectile extends AbilityProjectileEntity {

    // Must match BASE_FORWARD_OFFSET and STRETCH_OFFSET_MULTIPLIER in RedRocProjectileRenderer,
    // and stretchScaleZ set in GomuReworkProjectiles (3.1F).
    // Tweak these alongside the renderer constants to keep hitbox and visual in sync.
    private static final float HITBOX_BASE_OFFSET = 1.5F;
    private static final float HITBOX_STRETCH_MULTIPLIER = 0.35F;
    private static final float STRETCH_SCALE_Z = 3.1F;

    public GomuGomuNoRedRocProjectile(EntityType type, World world) {
        super(type, world);
    }

    public GomuGomuNoRedRocProjectile(World world, LivingEntity player) {
        super((EntityType) BigGomuReworkProjectiles.GOMU_GOMU_NO_RED_ROC.get(), world, player, GomuGomuNoRedRocAbility.INSTANCE);
        this.setDamage(110.0F);
        this.setMaxLife(30);
        this.setBlocksAffectedLimit(100000);
        this.setPassThroughEntities();
        super.setFist();
        this.setEntityCollisionSize((double) 5.0F);
        this.setDamageSource(this.getDamageSource().setSourceElement(SourceElement.RUBBER));
        this.onBlockImpactEvent = this::onBlockImpact;
        this.setUnavoidable();
        this.onEntityImpactEvent = this::onEntityHit;
    }

    private void onBlockImpact(BlockPos hit) {
        ExplosionAbility explosion = super.createExplosion(
                this.getThrower(),
                this.level,
                (double) hit.getX(),
                (double) hit.getY(),
                (double) hit.getZ(),
                3.0F
        );
        explosion.setStaticDamage(30.0F);
        explosion.setSmokeParticles(new CommonExplosionParticleEffect(3));
        explosion.doExplosion();
    }

    private void onEntityHit(LivingEntity target) {
        // passthrough — damage is handled by AbilityProjectileEntity base class
    }

    @Override
    public void tick() {
        // Push the hitbox forward to match the visual fist position before collision checks run.
        // The offset formula mirrors RedRocProjectileRenderer so both stay in sync.
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

        if (!this.level.isClientSide) {
            int particleCount = 8;
            double radius = 1.5;
            double angleOffset = (this.tickCount * 30.0) * (Math.PI / 180.0);

            Vector3d motion = this.getDeltaMovement().normalize();
            Vector3d up = new Vector3d(0, 1, 0);
            if (Math.abs(motion.dot(up)) > 0.99) {
                up = new Vector3d(1, 0, 0);
            }

            Vector3d axisA = motion.cross(up).normalize();
            Vector3d axisB = motion.cross(axisA).normalize();

            // Front ring
            for (int i = 0; i < particleCount; i++) {
                double angle = angleOffset + (i * (2 * Math.PI / particleCount));
                double offsetX = (Math.cos(angle) * axisA.x + Math.sin(angle) * axisB.x) * radius;
                double offsetY = (Math.cos(angle) * axisA.y + Math.sin(angle) * axisB.y) * radius;
                double offsetZ = (Math.cos(angle) * axisA.z + Math.sin(angle) * axisB.z) * radius;

                WyHelper.spawnParticleEffect(
                        (ParticleEffect) ModParticleEffects.HEAT_DASH.get(),
                        this,
                        this.getX() + offsetX,
                        this.getY() + offsetY,
                        this.getZ() + offsetZ
                );
            }

            // Back ring offset by half step for spiral look
            Vector3d behind = this.getDeltaMovement().normalize().scale(-1.2);
            for (int i = 0; i < particleCount; i++) {
                double angle = angleOffset + (i * (2 * Math.PI / particleCount)) + Math.PI / particleCount;
                double offsetX = (Math.cos(angle) * axisA.x + Math.sin(angle) * axisB.x) * radius;
                double offsetY = (Math.cos(angle) * axisA.y + Math.sin(angle) * axisB.y) * radius;
                double offsetZ = (Math.cos(angle) * axisA.z + Math.sin(angle) * axisB.z) * radius;

                WyHelper.spawnParticleEffect(
                        (ParticleEffect) ModParticleEffects.HEAT_DASH.get(),
                        this,
                        this.getX() + offsetX + behind.x,
                        this.getY() + offsetY + behind.y,
                        this.getZ() + offsetZ + behind.z
                );
            }
        }
    }
}