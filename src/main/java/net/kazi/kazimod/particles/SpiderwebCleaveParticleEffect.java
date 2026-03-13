package net.kazi.kazimod.particles;

import net.kazi.kazimod.init.KaziParticleTypes;
import net.minecraft.entity.Entity;
import net.minecraft.particles.ParticleType;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.math.EasingDirection;
import xyz.pixelatedw.mineminenomi.api.math.EasingFunction;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

public class SpiderwebCleaveParticleEffect extends ParticleEffect<ParticleEffect.NoDetails> {

    public SpiderwebCleaveParticleEffect() {
    }

    @Override
    public void spawn(Entity entity, World world, double posX, double posY, double posZ, ParticleEffect.NoDetails details) {

        // Main layer — sized to match 12 block radius AOE
        SimpleParticleData main = new SimpleParticleData((ParticleType) KaziParticleTypes.SPIDERWEB_CLEAVE.get());
        main.setFunction(EasingFunction.ELASTIC_OUT);
        main.setEaseDirection(EasingDirection.POSITIVE);
        main.setLookVec(-90.0F, 0.0F);
        main.setColor(1.0F, 1.0F, 1.0F, 0.95F);
        main.setLife(14);
        main.setSize(80.0F); // 12 blocks * 4 = 48
        main.setMotion(0.0, 0.0, 0.0);
        world.addParticle(main, true, posX, posY, posZ, 0.0, 0.0, 0.0);

        // Secondary lingering layer — slightly larger for soft edge falloff
        SimpleParticleData secondary = new SimpleParticleData((ParticleType) KaziParticleTypes.SPIDERWEB_CLEAVE.get());
        secondary.setFunction(EasingFunction.ELASTIC_OUT);
        secondary.setEaseDirection(EasingDirection.POSITIVE);
        secondary.setLookVec(-90.0F, 0.0F);
        secondary.setColor(0.85F, 0.85F, 0.85F, 0.6F);
        secondary.setLife(22);
        secondary.setSize(88.0F); // slightly bigger for soft edge
        secondary.setMotion(0.0, 0.0, 0.0);
        world.addParticle(secondary, true, posX, posY, posZ, 0.0, 0.0, 0.0);
    }
}