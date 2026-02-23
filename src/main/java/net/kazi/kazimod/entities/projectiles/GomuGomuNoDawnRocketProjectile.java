package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.abilities.GomuRework.GomuGomuNoRocketRework;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.abilities.components.GrabEntityComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class GomuGomuNoDawnRocketProjectile extends AbilityProjectileEntity {

    // Knockback power applied to the grabbed target on release
    private static final float THROW_HORIZONTAL = 2.5F;
    private static final float THROW_VERTICAL    = 2.0F;
    private static final float DAMAGE            = 60.0F;

    public GomuGomuNoDawnRocketProjectile(EntityType<?> type, World world) {
        super(type, world);
    }

    public GomuGomuNoDawnRocketProjectile(World world, LivingEntity shooter) {
        super((EntityType<?>) GomuReworkProjectiles.GOMU_GOMU_NO_DAWN_ROCKET.get(), world, shooter, GomuGomuNoRocketRework.INSTANCE);
        this.setDamage(DAMAGE);
        this.setMaxLife(200);
        super.setFist();
        this.setEntityCollisionSize(1.0F);
        this.setUnavoidable();
        this.setDamageSource(this.getDamageSource().setSourceElement(SourceElement.RUBBER));
        super.setGravity(0.0F);
        // Override the default entity-hit behaviour so we can grab + throw
        super.onEntityImpactEvent = this::onEntityImpactEvent;
    }

    private void onEntityImpactEvent(LivingEntity target) {
        if (this.level.isClientSide) return;

        LivingEntity shooter = this.getOwner() instanceof LivingEntity ? (LivingEntity) this.getOwner() : null;

        // Deal damage
        if (shooter != null) {
            AbilityDamageSource source = (AbilityDamageSource) ModDamageSource
                    .causeAbilityDamage(shooter, GomuGomuNoRocketRework.INSTANCE)
                    .setFist();
            target.hurt(source, DAMAGE);
        } else {
            target.hurt(this.getDamageSource(), DAMAGE);
        }

        // Apply dizzy so the target is stunned briefly
        target.addEffect(new EffectInstance((Effect) ModEffects.DIZZY.get(), 40, 1));

        // Heavy knockback — launch the target away from the shooter (or from the
        // projectile's travel direction if the shooter is unavailable)
        Vector3d throwDir;
        if (shooter != null) {
            throwDir = target.position().subtract(shooter.position()).normalize();
        } else {
            throwDir = this.getDeltaMovement().normalize();
        }

        AbilityHelper.setDeltaMovement(
                target,
                throwDir.x * THROW_HORIZONTAL,
                THROW_VERTICAL,
                throwDir.z * THROW_HORIZONTAL
        );

        // Destroy the projectile after impact
        this.remove();
    }
}