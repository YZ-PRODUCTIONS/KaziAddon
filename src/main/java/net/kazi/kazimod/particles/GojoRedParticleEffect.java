package net.kazi.kazimod.particles;

import net.kazi.kazimod.init.KaziParticleTypes;
import net.minecraft.entity.Entity;
import net.minecraft.particles.ParticleType;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.math.EasingDirection;
import xyz.pixelatedw.mineminenomi.api.math.EasingFunction;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

public class GojoRedParticleEffect extends ParticleEffect<ParticleEffect.NoDetails> {

    public GojoRedParticleEffect() {
    }

    @Override
    public void spawn(Entity entity, World world, double posX, double posY, double posZ, ParticleEffect.NoDetails details) {

        // Tendrils — short life so it dies before next tick's spawn
        SimpleParticleData tendrils = new SimpleParticleData((ParticleType) KaziParticleTypes.GOJORED.get());
        tendrils.setFunction(EasingFunction.SINE_IN_OUT);
        tendrils.setEaseDirection(EasingDirection.POSITIVE);
        tendrils.setLookVec(entity.xRot, entity.yRot);
        tendrils.setLife(2);
        tendrils.setSize(8.0F);
        tendrils.setMotion(0.0, 0.0, 0.0);
        world.addParticle(tendrils, true, posX, posY + 0.5, posZ, 0.0, 0.0, 0.0);

        // Central orb — short life for same reason
        SimpleParticleData centralOrb = new SimpleParticleData((ParticleType) KaziParticleTypes.GOJORED.get());
        centralOrb.setFunction(EasingFunction.ELASTIC_OUT);
        centralOrb.setEaseDirection(EasingDirection.NEGATIVE);
        centralOrb.setLookVec(entity.xRot, entity.yRot);
        centralOrb.setLife(2);
        centralOrb.setSize(7.0F);
        centralOrb.setMotion(0.0, 0.0, 0.0);
        world.addParticle(centralOrb, true, posX, posY + 0.5, posZ, 0.0, 0.0, 0.0);
    }
}