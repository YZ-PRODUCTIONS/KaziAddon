package net.kazi.kazimod.entities.projectiles;

import java.awt.Color;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.particles.effects.CommonExplosionParticleEffect;

public class TimeTheftProjectile extends AbilityProjectileEntity {

    // The same light-blue used by TimeTheftAbility's AOE sphere: (180, 230, 255)
    public static final Color TIME_THEFT_COLOR = new Color(180, 230, 255, 200);

    public TimeTheftProjectile(EntityType type, World world) {
        super(type, world);
    }

    public TimeTheftProjectile(World world, LivingEntity player) {
        super(
                (EntityType) TokiProjectiles.TIME_THEFT_BEAM.get(),
                world, player,
                net.kazi.kazimod.abilities.Toki.TimeTheftAbility.INSTANCE
        );
        // Half of Gastille's 50 damage = 25
        this.setDamage(25.0F);
        this.setPassThroughEntities();
        this.setArmorPiercing(0.5F);
        // Range limit: 20 blocks. At default gravity the projectile travels
        // roughly 1 block/tick, so 20 ticks ~ 20 blocks.
        this.setMaxLife(20);
        this.onBlockImpactEvent = this::onBlockImpactEvent;
        this.onTickEvent        = this::onTickEvent;
    }

    private void onBlockImpactEvent(BlockPos hit) {
        ExplosionAbility explosion = super.createExplosion(
                this.getThrower(), this.level,
                (double) hit.getX(), (double) hit.getY(), (double) hit.getZ(),
                2.5F   // half of Gastille's 5.0 explosion size
        );
        explosion.setStaticDamage(14.0F);   // half of Gastille's 28.0
        explosion.setExplosionSound(true);
        explosion.setDamageOwner(false);
        explosion.setDestroyBlocks(true);
        explosion.setFireAfterExplosion(false);
        explosion.setSmokeParticles(new CommonExplosionParticleEffect(3));
        explosion.setDamageEntities(true);
        explosion.doExplosion();
    }

    private void onTickEvent() {
        // Tint trail particles to the time-theft colour.
        // Reuse the generic Gastille particle shape but override the colour by
        // spawning coloured dust particles instead.
        if (!this.level.isClientSide) {
            this.level.addParticle(
                    new net.minecraft.particles.RedstoneParticleData(
                            TIME_THEFT_COLOR.getRed()   / 255.0F,
                            TIME_THEFT_COLOR.getGreen() / 255.0F,
                            TIME_THEFT_COLOR.getBlue()  / 255.0F,
                            1.5F
                    ),
                    this.getX(), this.getY(), this.getZ(),
                    0.0, 0.0, 0.0
            );
        }
    }
}