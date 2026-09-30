/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityType
 *  net.minecraft.entity.LivingEntity
 *  net.minecraft.potion.Effect
 *  net.minecraft.potion.EffectInstance
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.vector.Vector3d
 *  net.minecraft.world.World
 *  xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility
 *  xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity
 *  xyz.pixelatedw.mineminenomi.init.ModEffects
 *  xyz.pixelatedw.mineminenomi.particles.effects.CommonExplosionParticleEffect
 *  xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect
 */
package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.abilities.SaberRework.DivineDepartureClone;
import net.MrMagicalCart.cartaddon.entities.projectiles.swordsmenextra.SwordsmenExtraProjectiles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.CommonExplosionParticleEffect;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

public class DivineDepartureProjectileClone
extends AbilityProjectileEntity {
    Vector3d look;

    public DivineDepartureProjectileClone(EntityType type, World world) {
        super(type, world);
        this.look = Vector3d.ZERO;
    }

    public DivineDepartureProjectileClone(World world, LivingEntity entity) {
        super((EntityType)SwordsmenExtraProjectiles.DIVINE_DEPARTURE.get(), world, entity, DivineDepartureClone.INSTANCE);
        this.setDamage(50.0f);
        this.setArmorPiercing(1.0f);
        this.setMaxLife(110);
        this.setBlocksAffectedLimit(100000);
        this.setUnavoidable();
        this.look = entity.getLookAngle();
        this.setPassThroughEntities();
        this.onBlockImpactEvent = this::onBlockImpactEvent;
        this.onEntityImpactEvent = this::onHit;
    }

    private void onHit(LivingEntity entity) {
        entity.addEffect(new EffectInstance((Effect)ModEffects.MOVEMENT_BLOCKED.get(), 40, 0, false, false));
        entity.addEffect(new EffectInstance((Effect)ModEffects.UNCONSCIOUS.get(), 40, 0, false, false));
        entity.hurtMarked = true;
    }

    private void onBlockImpactEvent(BlockPos hit) {
        ExplosionAbility explosion = super.createExplosion((Entity)this.getThrower(), this.level, (double)hit.getX(), (double)hit.getY(), (double)hit.getZ(), 3.0f);
        explosion.setStaticDamage(20.0f);
        explosion.setSmokeParticles((ParticleEffect)new CommonExplosionParticleEffect(3));
        explosion.doExplosion();
    }
}



