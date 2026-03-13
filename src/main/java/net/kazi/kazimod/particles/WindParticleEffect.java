package net.kazi.kazimod.particles;

import net.kazi.kazimod.init.KaziParticleTypes;
import net.minecraft.entity.Entity;
import net.minecraft.particles.ParticleType;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.math.EasingDirection;
import xyz.pixelatedw.mineminenomi.api.math.EasingFunction;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

public class WindParticleEffect extends ParticleEffect<ParticleEffect.NoDetails> {

    public WindParticleEffect() {
    }

    @Override
    public void spawn(Entity entity, World world, double posX, double posY, double posZ, ParticleEffect.NoDetails details) {

        // Main wind swoosh — large, fast-expanding, slightly transparent
        SimpleParticleData main = new SimpleParticleData((ParticleType) KaziParticleTypes.WIND.get());
        main.setFunction(EasingFunction.ELASTIC_OUT);
        main.setEaseDirection(EasingDirection.POSITIVE);
        main.setLookVec(entity.xRot, entity.yRot);
        main.setColor(1.0F, 1.0F, 1.0F, 0.85F);
        main.setLife(18);
        main.setSize(14.0F);
        main.setMotion(0.0, 0.0, 0.0);
        world.addParticle(main, true, posX, posY, posZ, 0.0, 0.0, 0.0);

        // Secondary wind layer — larger and lingers longer for a trailing afterimage
        SimpleParticleData secondary = new SimpleParticleData((ParticleType) KaziParticleTypes.WIND.get());
        secondary.setFunction(EasingFunction.ELASTIC_OUT);
        secondary.setEaseDirection(EasingDirection.POSITIVE);
        secondary.setLookVec(entity.xRot, entity.yRot);
        secondary.setColor(1.0F, 1.0F, 1.0F, 0.6F);
        secondary.setLife(28);
        secondary.setSize(17.0F);
        secondary.setMotion(0.0, 0.0, 0.0);
        world.addParticle(secondary, true, posX, posY, posZ, 0.0, 0.0, 0.0);
    }
}