package net.kazi.kazimod.particles;

import net.kazi.kazimod.init.KaziParticleTypes;
import net.minecraft.entity.Entity;
import net.minecraft.particles.ParticleType;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.math.EasingDirection;
import xyz.pixelatedw.mineminenomi.api.math.EasingFunction;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

public class CleaveParticleEffect extends ParticleEffect<ParticleEffect.NoDetails> {

    public CleaveParticleEffect() {
    }

    @Override
    public void spawn(Entity entity, World world, double posX, double posY, double posZ, ParticleEffect.NoDetails details) {

        // Large central cleave flash — sharp red X burst facing the attacker's look direction
        SimpleParticleData main = new SimpleParticleData((ParticleType) KaziParticleTypes.DISMANTLE.get());
        main.setFunction(EasingFunction.ELASTIC_OUT);
        main.setEaseDirection(EasingDirection.POSITIVE);
        main.setLookVec(entity.xRot, entity.yRot);
        main.setColor(1.0F, 0.05F, 0.05F, 0.9F);
        main.setLife(18);
        main.setSize(14.0F);
        main.setMotion(0.0, 0.0, 0.0);
        world.addParticle(main, true, posX, posY, posZ, 0.0, 0.0, 0.0);

        // Second layer — slightly darker and larger, fades slower for a lingering afterimage
        SimpleParticleData secondary = new SimpleParticleData((ParticleType) KaziParticleTypes.DISMANTLE.get());
        secondary.setFunction(EasingFunction.ELASTIC_OUT);
        secondary.setEaseDirection(EasingDirection.POSITIVE);
        secondary.setLookVec(entity.xRot, entity.yRot);
        secondary.setColor(0.8F, 0.0F, 0.0F, 0.5F);
        secondary.setLife(25);
        secondary.setSize(16.0F);
        secondary.setMotion(0.0, 0.0, 0.0);
        world.addParticle(secondary, true, posX, posY, posZ, 0.0, 0.0, 0.0);
    }
}