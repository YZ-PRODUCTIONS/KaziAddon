package net.kazi.kazimod.particles;

import net.kazi.kazimod.init.KaziParticleTypes;
import net.minecraft.entity.Entity;
import net.minecraft.particles.ParticleType;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.math.EasingDirection;
import xyz.pixelatedw.mineminenomi.api.math.EasingFunction;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

public class GojoPurpleParticleEffect extends ParticleEffect<ParticleEffect.NoDetails> {

    public GojoPurpleParticleEffect() {
    }

    @Override
    public void spawn(Entity entity, World world, double posX, double posY, double posZ,
                      ParticleEffect.NoDetails details) {

        SimpleParticleData orb = new SimpleParticleData((ParticleType) KaziParticleTypes.GOJO_PURPLE.get());
        orb.setFunction(EasingFunction.SINE_IN_OUT);
        orb.setEaseDirection(EasingDirection.POSITIVE);
        orb.setLookVec(entity.xRot, entity.yRot);
        orb.setLife(2);
        orb.setSize(8.0F);
        orb.setMotion(0.0, 0.0, 0.0);
        world.addParticle(orb, true, posX, posY + 0.5, posZ, 0.0, 0.0, 0.0);
    }
}