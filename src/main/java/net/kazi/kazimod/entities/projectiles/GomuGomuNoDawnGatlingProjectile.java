//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.entities.projectiles;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.particles.effects.gomu.GearSecondParticleEffect;

public class GomuGomuNoDawnGatlingProjectile extends AbilityProjectileEntity {
    private static final ParticleEffect PARTICLES = new GearSecondParticleEffect();

    public GomuGomuNoDawnGatlingProjectile(EntityType type, World world) {
        super(type, world);
    }

    public GomuGomuNoDawnGatlingProjectile(World world, LivingEntity player, Ability ability) {
        super((EntityType) GomuReworkProjectiles.GOMU_GOMU_DAWN_GATLING.get(), world, player, ability.getCore());
        this.setDamage(10.0F);
        this.setMaxLife(18);
        this.setFist();
        this.setHurtTime(10);
        this.setEntityCollisionSize((double)1.0F);
        this.setDamageSource(this.getDamageSource().setSourceElement(SourceElement.RUBBER));
        this.onTickEvent = this::onTickEvent;
    }

    private void onTickEvent() {
        if (this.tickCount % 2 == 0) {
            PARTICLES.spawn(this.level, this.getX(), this.getY(), this.getZ(), (double)0.0F, (double)0.0F, (double)0.0F);
        }



    }

    private void onEntityImpactEvent(LivingEntity entity) {
        entity.addEffect(new EffectInstance((Effect) ModEffects.DIZZY.get(), 20, 0));
}}
