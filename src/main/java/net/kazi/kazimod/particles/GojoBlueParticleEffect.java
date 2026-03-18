package net.kazi.kazimod.particles;

import net.kazi.kazimod.init.KaziParticleTypes;
import net.minecraft.entity.Entity;
import net.minecraft.particles.ParticleType;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.math.EasingDirection;
import xyz.pixelatedw.mineminenomi.api.math.EasingFunction;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

public class GojoBlueParticleEffect extends ParticleEffect<ParticleEffect.NoDetails> {

    public GojoBlueParticleEffect() {
    }

    @Override
    public void spawn(Entity entity, World world, double posX, double posY, double posZ,
                      ParticleEffect.NoDetails details) {

        // Inner core — 50% of original (6.0 -> 3.0)
        SimpleParticleData core = new SimpleParticleData((ParticleType) KaziParticleTypes.GOJO_BLUE.get());
        core.setFunction(EasingFunction.SINE_IN_OUT);
        core.setEaseDirection(EasingDirection.POSITIVE);
        core.setLookVec(entity.xRot, entity.yRot);
        core.setLife(2);
        core.setSize(1.5F);
        core.setMotion(0.0, 0.0, 0.0);
        world.addParticle(core, true, posX, posY + 0.5, posZ, 0.0, 0.0, 0.0);

        // Outer glow — 50% of original (10.0 -> 5.0)
        SimpleParticleData glow = new SimpleParticleData((ParticleType) KaziParticleTypes.GOJO_BLUE.get());
        glow.setFunction(EasingFunction.SINE_IN_OUT);
        glow.setEaseDirection(EasingDirection.POSITIVE);
        glow.setLookVec(entity.xRot, entity.yRot);
        glow.setLife(2);
        glow.setSize(2.5F);
        glow.setMotion(0.0, 0.0, 0.0);
        world.addParticle(glow, true, posX, posY + 0.5, posZ, 0.0, 0.0, 0.0);
    }
}