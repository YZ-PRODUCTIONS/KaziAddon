package net.kazi.kazimod.entities.projectiles;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.kazi.kazimod.abilities.Kake.CasinoRollAbility;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import net.kazi.kazimod.abilities.Kake.CasinoRollAbility;

public class DiceProjectile extends AbilityProjectileEntity {

    public static final float DEFAULT_DAMAGE = 14.0f;
    private int bounceCount;

    public DiceProjectile(EntityType<?> type, World world) {
        super(type, world);
        this.bounceCount = 0;
    }

    public DiceProjectile(World world, LivingEntity shooter) {
        super(CasinoProjectiles.DICE.get(), world, shooter, CasinoRollAbility.INSTANCE);
        this.bounceCount = 0;
        this.setDamage(DEFAULT_DAMAGE);
        this.setMaxLife(80);
        this.setGravity(0.06f);
        this.setEntityCollisionSize(2.4);

        this.onEntityImpactEvent = (target) -> explode();
        this.onBlockImpactEvent = (pos) -> {
            if (this.bounceCount < 2) {
                this.bounceCount++;
                Vector3d m = this.getDeltaMovement();
                this.setDeltaMovement(m.x, Math.abs(m.y) * 0.7, m.z);
            } else {
                explode();
            }
        };
    }

    private void explode() {
        if (!this.level.isClientSide) {
            this.level.explode(this, this.getX(), this.getY(), this.getZ(), 1.5f, false, Explosion.Mode.NONE);
            this.remove();
        }
    }
}