package net.kazi.kazimod.particles;

import net.kazi.kazimod.init.KaziParticleTypes;
import net.minecraft.entity.Entity;
import net.minecraft.particles.ParticleType;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.math.EasingDirection;
import xyz.pixelatedw.mineminenomi.api.math.EasingFunction;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

public class DismantleParticleEffect extends ParticleEffect<ParticleEffect.NoDetails> {

    public DismantleParticleEffect() {
    }

    @Override
    public void spawn(Entity entity, World world, double posX, double posY, double posZ, ParticleEffect.NoDetails details) {

        float randomYaw = (float)(Math.random() * 360.0);

        // Large central cleave flash
        SimpleParticleData main = new SimpleParticleData((ParticleType) KaziParticleTypes.DISMANTLE.get());
        main.setFunction(EasingFunction.ELASTIC_OUT);
        main.setEaseDirection(EasingDirection.POSITIVE);
        main.setLookVec((float)(Math.random() * 360.0), entity.yRot);
        main.setColor(1.0F, 1.0F, 1.0F, 0.9F);
        main.setLife(18);
        main.setSize(14.0F);
        main.setMotion(0.0, 0.0, 0.0);
        world.addParticle(main, true, posX, posY, posZ, 0.0, 0.0, 0.0);

        // Second layer — slightly offset rotation for variety
        float randomYaw2 = (float)(Math.random() * 360.0);

    }
}