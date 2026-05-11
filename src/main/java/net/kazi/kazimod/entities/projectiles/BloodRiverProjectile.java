package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.abilities.VampAwaken.BloodRiver;
import net.kazi.kazimod.init.KaziEffects;
import net.kazi.kazimod.init.KaziEntities;
import net.MrMagicalCart.cartaddon.init.CartParticleTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.ParticleType;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effect;
import net.minecraft.potion.Effects;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class BloodRiverProjectile extends AbilityProjectileEntity {

    private static final float DAMAGE = 35.0F;
    private static final int MAX_LIFE = 32;
    private static final double HITBOX_SIZE = 3.0D;
    private static final float TRAIL_SIZE = 38.0F;
    private static final double TRAIL_SPREAD = 0.3D;

    public BloodRiverProjectile(EntityType<?> type, World world) {
        super(type, world);
    }

    public BloodRiverProjectile(World world, LivingEntity shooter) {
        super(KaziEntities.BLOOD_RIVER_PROJECTILE.get(), world, shooter, BloodRiver.INSTANCE);
        this.setDamage(DAMAGE);
        this.setArmorPiercing(1.0F);
        this.setPassThroughBlocks();
        this.setPassThroughEntities();
        this.setMaxLife(MAX_LIFE);
        this.setEntityCollisionSize(HITBOX_SIZE, HITBOX_SIZE, HITBOX_SIZE);
        this.setInvisible(true);
        this.onTickEvent = this::onTickEvent;
        this.onEntityImpactEvent = this::onEntityImpactEvent;
    }

    private void onTickEvent() {
        if (!this.level.isClientSide) {
            for (int i = 0; i < 3; ++i) {
                double offsetX = (WyHelper.randomDouble() - 0.5D) * TRAIL_SPREAD;
                double offsetY = (WyHelper.randomDouble() - 0.5D) * TRAIL_SPREAD;
                double offsetZ = (WyHelper.randomDouble() - 0.5D) * TRAIL_SPREAD;
                SimpleParticleData data = new SimpleParticleData((ParticleType<?>) CartParticleTypes.BATTO.get());
                data.setLife(32);
                data.setSize(TRAIL_SIZE);
                data.setColor(1.0F, 0.08F, 0.08F, 1.0F);
                if (this.tickCount % 2 == 0) {
                    WyHelper.spawnParticles(data, (ServerWorld) this.level,
                            this.getX() + offsetX, this.getY() - 0.4D + offsetY, this.getZ() + offsetZ);
                }
            }
        }
    }

    private void onEntityImpactEvent(LivingEntity target) {
        target.addEffect(new EffectInstance(Effects.CONFUSION, 200, 5, false, false));
        target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 200, 2, false, false));
        target.addEffect(new EffectInstance(Effects.DIG_SLOWDOWN, 200, 0, false, false));
        target.addEffect(new EffectInstance(KaziEffects.WEAKENED_MOVEMENT.get(), 200, 1, false, false));
        target.addEffect(new EffectInstance((Effect) ModEffects.PARALYSIS.get(), 10, 0, false, false));
    }

}
