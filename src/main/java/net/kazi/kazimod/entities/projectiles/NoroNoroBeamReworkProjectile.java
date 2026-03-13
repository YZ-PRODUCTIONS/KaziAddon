//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.noro.NoroProjectiles;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.mixins.EffectInstanceMixin;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class NoroNoroBeamReworkProjectile extends AbilityProjectileEntity {
    public NoroNoroBeamReworkProjectile(EntityType<Entity> type, World world) {
        super(type, world);
    }

    public NoroNoroBeamReworkProjectile(World world, LivingEntity player, Ability ability) {
        super((EntityType) NoroProjectiles.NORO_NORO_BEAM.get(), world, player, ability.getCore());
        super.setMaxLife(10);
        super.setEntityCollisionSize((double)1.25F);
        super.setPassThroughEntities();
        super.setUnavoidable();
        super.setPassThroughBlocks();
        super.onEntityImpactEvent = this::onEntityImpactEvent;
    }

    private void onEntityImpactEvent(LivingEntity hitEntity) {
        EffectInstance instance = hitEntity.getEffect((Effect)ModEffects.NORO_SLOWNESS.get());
        if (instance == null) {
            hitEntity.addEffect(new EffectInstance((Effect)ModEffects.NORO_SLOWNESS.get(), 240, 0));
            hitEntity.addEffect(new EffectInstance((Effect) KaziEffects.WEAKENED_MOVEMENT.get(), 240, 0));
        } else {
            ((EffectInstanceMixin)instance).setDuration(instance.getDuration() + 240);
            WyHelper.sendApplyEffectToAllNearby(hitEntity, hitEntity.position(), 100, instance);
        }

    }
}
