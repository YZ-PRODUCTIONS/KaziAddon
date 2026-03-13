//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.entities.projectiles;

import java.util.UUID;
import javax.annotation.Nullable;

import net.MrMagicalCart.cartaddon.entities.projectiles.itoextra.ItoAwakeningProjectiles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.CommonExplosionParticleEffect;

public class GodThreadProjectileRework extends AbilityProjectileEntity {
    @Nullable
    private UUID homingTarget;
    private static final double HOMING_STRENGTH = 0.18;

    public GodThreadProjectileRework(EntityType type, World world) {
        super(type, world);
    }

    public GodThreadProjectileRework(World world, LivingEntity player, Ability ability) {
        super((EntityType) ItoAwakeningProjectiles.FLAP_THREAD.get(), world, player, ability.getCore());
        this.setDamage(55.0F);
        this.setArmorPiercing(0.85F);
        this.setMaxLife(15);
        this.setPassThroughEntities();
        this.setHurtTime(10);
        this.onEntityImpactEvent = this::onEntityImpactEvent;
        this.onBlockImpactEvent = this::onBlockImpactEvent;
    }

    public void setHomingTarget(LivingEntity target) {
        if (target != null) {
            this.homingTarget = target.getUUID();
        }

    }

    public void tick() {
        if (!this.level.isClientSide && this.homingTarget != null && this.level instanceof ServerWorld) {
            Entity e = ((ServerWorld)this.level).getEntity(this.homingTarget);
            if (e instanceof LivingEntity && e.isAlive()) {
                LivingEntity target = (LivingEntity)e;
                Vector3d targetPos = target.position().add((double)0.0F, (double)target.getBbHeight() * 0.6, (double)0.0F);
                Vector3d to = targetPos.subtract(this.position());
                if (to.lengthSqr() > 1.0E-4) {
                    Vector3d vel = this.getDeltaMovement();
                    double speed = vel.length();
                    if (speed < 0.05) {
                        speed = 0.05;
                    }

                    Vector3d desired = to.normalize().scale(speed);
                    Vector3d newVel = vel.scale(0.8200000000000001).add(desired.scale(0.18));
                    this.setDeltaMovement(newVel);
                }
            }
        }

        super.tick();
    }

    private void onEntityImpactEvent(LivingEntity entity) {
        entity.addEffect(new EffectInstance((Effect)ModEffects.BLEEDING.get(), 200, 0));
        entity.addEffect(new EffectInstance((Effect)ModEffects.DIZZY.get(), 80, 0));
    }

    private void onBlockImpactEvent(BlockPos hit) {
        ExplosionAbility explosion = super.createExplosion(this.getThrower(), this.level, (double)hit.getX(), (double)hit.getY(), (double)hit.getZ(), 2.0F);
        explosion.setStaticDamage(0.0F);
        explosion.setFireAfterExplosion(false);
        explosion.setSmokeParticles(new CommonExplosionParticleEffect(5));
        explosion.doExplosion();
    }
}
