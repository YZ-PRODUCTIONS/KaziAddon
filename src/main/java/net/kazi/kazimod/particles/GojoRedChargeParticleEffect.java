package net.kazi.kazimod.particles;

import net.kazi.kazimod.init.KaziParticleTypes;
import net.minecraft.entity.Entity;
import net.minecraft.particles.ParticleType;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.math.EasingDirection;
import xyz.pixelatedw.mineminenomi.api.math.EasingFunction;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

public class GojoRedChargeParticleEffect extends ParticleEffect<ParticleEffect.NoDetails> {

    public GojoRedChargeParticleEffect() {
    }

    @Override
    public void spawn(Entity entity, World world, double posX, double posY, double posZ, ParticleEffect.NoDetails details) {

        // Tendrils — 50% of normal size (8.0 -> 4.0)
        SimpleParticleData tendrils = new SimpleParticleData((ParticleType) KaziParticleTypes.GOJORED.get());
        tendrils.setFunction(EasingFunction.SINE_IN_OUT);
        tendrils.setEaseDirection(EasingDirection.POSITIVE);
        tendrils.setLookVec(entity.xRot, entity.yRot);
        tendrils.setColor(0.9F, 0.05F, 0.05F, 1.0F);
        tendrils.setLife(2);
        tendrils.setSize(0.5F);
        tendrils.setMotion(0.0, 0.0, 0.0);
        world.addParticle(tendrils, true, posX, posY + 0.5, posZ, 0.0, 0.0, 0.0);

        // Central orb — 50% of normal size (7.0 -> 3.5)
        SimpleParticleData centralOrb = new SimpleParticleData((ParticleType) KaziParticleTypes.GOJORED.get());
        centralOrb.setFunction(EasingFunction.ELASTIC_OUT);
        centralOrb.setEaseDirection(EasingDirection.NEGATIVE);
        centralOrb.setLookVec(entity.xRot, entity.yRot);
        centralOrb.setColor(1.0F, 0.0F, 0.0F, 0.85F);
        centralOrb.setLife(2);
        centralOrb.setSize(0.4375F);
        centralOrb.setMotion(0.0, 0.0, 0.0);
        world.addParticle(centralOrb, true, posX, posY + 0.5, posZ, 0.0, 0.0, 0.0);
    }
}