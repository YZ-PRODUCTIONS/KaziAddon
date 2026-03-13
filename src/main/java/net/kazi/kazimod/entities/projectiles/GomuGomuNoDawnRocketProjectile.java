package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.abilities.GomuRework.GomuGomuNoRocketRework;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

public class GomuGomuNoDawnRocketProjectile extends AbilityProjectileEntity {

    private static final float BLOCK_PULL = 3.5F;

    public GomuGomuNoDawnRocketProjectile(EntityType<?> type, World world) {
        super(type, world);
    }

    public GomuGomuNoDawnRocketProjectile(World world, LivingEntity shooter, GomuGomuNoRocketRework ability) {
        super((EntityType<?>) GomuReworkProjectiles.GOMU_GOMU_NO_DAWN_ROCKET.get(), world, shooter, GomuGomuNoRocketRework.INSTANCE);
        this.setDamage(0.0F); // damage handled by the ability's charge phase
        this.setMaxLife(200);
        super.setFist();
        this.setEntityCollisionSize(1.0F);
        this.setUnavoidable();
        this.setDamageSource(this.getDamageSource().setSourceElement(SourceElement.RUBBER));
        super.setGravity(0.0F);

        // Block hit — pull shooter toward the block exactly like regular rocket,
        // then signal the ability to start the grab+throw phase
        this.onBlockImpactEvent = (BlockPos blockPos) -> {
            if (world.isClientSide) return;
            Vector3d hitPos = new Vector3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
            Vector3d dir = hitPos.subtract(shooter.position()).normalize();
            AbilityHelper.setDeltaMovement(shooter, dir.scale(BLOCK_PULL));
            // Tell the ability to start the grab phase now that we've launched the shooter
            ability.startGrabPhase(shooter);
            this.remove();
        };

        // Entity hit — same pull toward the hit entity, then grab phase
        this.onEntityImpactEvent = (LivingEntity target) -> {
            if (world.isClientSide) return;
            Vector3d dir = target.position().subtract(shooter.position()).normalize();
            AbilityHelper.setDeltaMovement(shooter, dir.scale(BLOCK_PULL));
            ability.startGrabPhase(shooter);
            this.remove();
        };
    }
}