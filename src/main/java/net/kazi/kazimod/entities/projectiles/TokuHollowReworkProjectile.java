package net.kazi.kazimod.entities.projectiles;

import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.api.damagesource.ModIndirectEntityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.horo.HoroProjectiles;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.CommonExplosionParticleEffect;
import net.minecraft.potion.Effect; // ADD THIS IMPORT


public class TokuHollowReworkProjectile extends AbilityProjectileEntity {
    public TokuHollowReworkProjectile(EntityType type, World world) {
        super(type, world);
    }

    public TokuHollowReworkProjectile(World world, LivingEntity player, Ability ability) {
        super((EntityType) HoroReworkProjectiles.TOKU_HOLLOW_REWORK.get(), world, player, ability);
        this.setDamage(25.0F);
        this.withEffects = () -> new EffectInstance[]{
                new EffectInstance(Effects.CONFUSION, 350, 1),
                new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 350, 1),
                new EffectInstance((Effect) ModEffects.NO_HANDS.get(), 100, 0)  // ADD THIS
        };        this.onBlockImpactEvent = this::onBlockImpactEvent;
        this.onEntityImpactEvent = this::onEntityImpactEvent;
    }

    private void onEntityImpactEvent(LivingEntity target) {
        ModDamageSource source = (ModDamageSource) (new ModIndirectEntityDamageSource(this.getDamageSource().msgId, this, this.getThrower()))
                .setSourceElement(SourceElement.SHOCKWAVE)
                .setHakiNature(SourceHakiNature.IMBUING)
                .setSourceTypes(new ArrayList(Arrays.asList(SourceType.INTERNAL)))
                .setUnavoidable();
        target.hurtTime = target.invulnerableTime = 0;
        target.hurt(source, this.getDamage());
    }

    private void onBlockImpactEvent(BlockPos pos) {
        ExplosionAbility explosion = super.createExplosion(this.getThrower(), this.level, (double) pos.getX(), (double) pos.getY(), (double) pos.getZ(), 7.0F);
        explosion.setStaticDamage(35.0F);
        explosion.setExplosionSound(true);
        explosion.setDamageOwner(false);
        explosion.setDestroyBlocks(true);
        explosion.setFireAfterExplosion(false);
        explosion.setSmokeParticles(new CommonExplosionParticleEffect(7));
        explosion.setDamageEntities(true);
        explosion.doExplosion();
    }
}