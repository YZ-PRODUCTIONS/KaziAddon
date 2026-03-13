package net.kazi.kazimod.entities.projectiles;

import net.MrMagicalCart.cartaddon.entities.projectiles.ryukirin.KirinProjectiles;
import net.kazi.kazimod.api.helpers.KaziAbilityHelper;
import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

public class KnockoutBeamReworkProjectile extends AbilityProjectileEntity {
    public KnockoutBeamReworkProjectile(EntityType<Entity> type, World world) {
        super(type, world);
    }

    public KnockoutBeamReworkProjectile(World world, LivingEntity player, Ability ability) {
        super((EntityType) KirinProjectiles.KNOCKOUT_BEAM.get(), world, player, ability.getCore());
        super.setMaxLife(25);
        super.setEntityCollisionSize((double)4.0F);
        super.setPassThroughEntities();
        super.setPassThroughBlocks();
        this.setUnavoidable();
        super.onEntityImpactEvent = this::onEntityImpactEvent;
    }

    private void onEntityImpactEvent(LivingEntity hitEntity) {
        // Add 3 stacks of tired
        KaziAbilityHelper.addTiredStacks(hitEntity, super.getThrower(), 3);

        // Apply weakened movement effect for 20 seconds (400 ticks)
        hitEntity.addEffect(new EffectInstance(KaziEffects.WEAKENED_MOVEMENT.get(), 400, 0, false, false));
    }
}