/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityType
 *  net.minecraft.entity.LivingEntity
 *  net.minecraft.potion.Effect
 *  net.minecraft.potion.EffectInstance
 *  net.minecraft.potion.Effects
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.vector.Vector3d
 *  net.minecraft.world.IWorld
 *  net.minecraft.world.World
 *  xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper
 *  xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity
 *  xyz.pixelatedw.mineminenomi.entities.projectiles.extra.EntityCloud
 *  xyz.pixelatedw.mineminenomi.init.ModEffects
 *  xyz.pixelatedw.mineminenomi.init.ModEntityPredicates
 *  xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect
 *  xyz.pixelatedw.mineminenomi.wypi.WyHelper
 */
package net.kazi.kazimod.entities.projectiles;

import java.util.function.Predicate;
import net.MrMagicalCart.cartaddon.abilities.susu.GokuroAbility;
import net.MrMagicalCart.cartaddon.entities.projectiles.susu.SusuProjectiles;
import net.MrMagicalCart.cartaddon.init.CartEffects;
import net.MrMagicalCart.cartaddon.init.CartParticleEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.extra.EntityCloud;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class GokuroProjectileRework
extends AbilityProjectileEntity {
    public GokuroProjectileRework(EntityType type, World world) {
        super(type, world);
    }

    public GokuroProjectileRework(World world, LivingEntity player) {
        super((EntityType)SusuProjectiles.GOKURO.get(), world, player, GokuroAbility.INSTANCE);
        this.setDamage(10.0f);
        this.onBlockImpactEvent = this::onBlockImpactEvent;
    }

    private void onBlockImpactEvent(BlockPos hit) {
        SootCloudEntity sootCloudEntity = new SootCloudEntity(this.level);
        sootCloudEntity.setLife(100);
        sootCloudEntity.moveTo(this.getX(), this.getY() + 1.0, this.getZ(), 0.0f, 0.0f);
        AbilityHelper.setDeltaMovement((Entity)sootCloudEntity, (double)0.0, (double)0.0, (double)0.0);
        sootCloudEntity.setThrower(this.getThrower());
        this.level.addFreshEntity((Entity)sootCloudEntity);
    }

    public static class SootCloudEntity
    extends EntityCloud {
        public SootCloudEntity(World world) {
            super(world);
        }

        public void tick() {
            super.tick();
            if (!this.level.isClientSide) {
                for (LivingEntity target : WyHelper.getNearbyLiving(this.position(), this.level, 6.0,
                        ModEntityPredicates.getEnemyFactions(this.getThrower()))) {
                    if (this.getThrower() == target || target.hasEffect(Effects.POISON)) continue;
                    target.addEffect(new EffectInstance(Effects.POISON, 100, 2));
                    target.addEffect(new EffectInstance(Effects.BLINDNESS, 80, 2));
                    target.addEffect(new EffectInstance((Effect)CartEffects.SOOT.get(), 100, 2));
                }
                if (this.tickCount % 4 == 0) {
                    WyHelper.spawnParticleEffect((ParticleEffect)((ParticleEffect)CartParticleEffects.GOKURO.get()), (Entity)this, (double)this.getX(), (double)this.getY(), (double)this.getZ());
                }
            }
        }
    }
}



