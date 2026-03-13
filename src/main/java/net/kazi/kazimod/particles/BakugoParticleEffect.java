package net.kazi.kazimod.particles;

import net.kazi.kazimod.init.KaziParticleTypes;
import net.minecraft.entity.Entity;
import net.minecraft.particles.ParticleType;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.math.EasingDirection;
import xyz.pixelatedw.mineminenomi.api.math.EasingFunction;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

public class BakugoParticleEffect extends ParticleEffect<ParticleEffect.NoDetails> {

    public BakugoParticleEffect() {
    }

    @Override
    public void spawn(Entity entity, World world, double posX, double posY, double posZ, ParticleEffect.NoDetails details) {

        // Core explosion burst — sharp, bright, instant pop
        SimpleParticleData core = new SimpleParticleData((ParticleType) KaziParticleTypes.BAKUGO.get());
        core.setFunction(EasingFunction.ELASTIC_OUT);
        core.setEaseDirection(EasingDirection.POSITIVE);
        core.setLookVec(entity.xRot, entity.yRot);
        core.setColor(1.0F, 0.6F, 0.0F, 1.0F); // fiery orange
        core.setLife(12);
        core.setSize(8.0F);
        core.setMotion(0.0, 0.0, 0.0);
        world.addParticle(core, true, posX, posY, posZ, 0.0, 0.0, 0.0);

        // Outer shockwave layer — larger, fades out quickly
        SimpleParticleData shockwave = new SimpleParticleData((ParticleType) KaziParticleTypes.BAKUGO.get());
        shockwave.setFunction(EasingFunction.ELASTIC_OUT);
        shockwave.setEaseDirection(EasingDirection.POSITIVE);
        shockwave.setLookVec(entity.xRot, entity.yRot);
        shockwave.setColor(1.0F, 0.85F, 0.1F, 0.75F); // bright yellow-orange
        shockwave.setLife(20);
        shockwave.setSize(11.0F);
        shockwave.setMotion(0.0, 0.0, 0.0);
        world.addParticle(shockwave, true, posX, posY, posZ, 0.0, 0.0, 0.0);

        // Afterburn — lingering ember glow
        SimpleParticleData afterburn = new SimpleParticleData((ParticleType) KaziParticleTypes.BAKUGO.get());
        afterburn.setFunction(EasingFunction.ELASTIC_OUT);
        afterburn.setEaseDirection(EasingDirection.NEGATIVE);
        afterburn.setLookVec(entity.xRot, entity.yRot);
        afterburn.setColor(1.0F, 0.3F, 0.0F, 0.5F); // deep orange-red fade
        afterburn.setLife(30);
        afterburn.setSize(6.0F);
        afterburn.setMotion(0.0, 0.02, 0.0);
        world.addParticle(afterburn, true, posX, posY, posZ, 0.0, 0.0, 0.0);
    }
}