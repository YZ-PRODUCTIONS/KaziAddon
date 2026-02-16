package net.kazi.kazimod.entities.projectiles;

import net.MrMagicalCart.cartaddon.entities.projectiles.ryukirin.KirinProjectiles;
import net.kazi.kazimod.api.helpers.KaziAbilityHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
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
        super.setEntityCollisionSize((double)1.5F);
        super.setPassThroughEntities();
        super.setPassThroughBlocks();
        super.onEntityImpactEvent = this::onEntityImpactEvent;
    }

    private void onEntityImpactEvent(LivingEntity hitEntity) {
        // Add 5 stacks of tired (matching the ice block pheasant adding 6 stacks of frostbite)
        KaziAbilityHelper.addTiredStacks(hitEntity, super.getThrower(), 3);
    }
}