package net.kazi.kazimod.particles;

import net.kazi.kazimod.init.KaziParticleTypes;
import net.minecraft.entity.Entity;
import net.minecraft.particles.ParticleType;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.math.EasingDirection;
import xyz.pixelatedw.mineminenomi.api.math.EasingFunction;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

public class FugaParticleEffect extends ParticleEffect<ParticleEffect.NoDetails> {

    public FugaParticleEffect() {
    }

    @Override
    public void spawn(Entity entity, World world, double posX, double posY, double posZ, ParticleEffect.NoDetails details) {

        double yawRad = Math.toRadians(entity.yRot);

        // Forward = direction entity faces
        double forwardX = -Math.sin(yawRad);
        double forwardZ =  Math.cos(yawRad);

        // Right = 90 degrees clockwise from forward
        double rightX =  Math.cos(yawRad);
        double rightZ =  Math.sin(yawRad);

        // Place at fist: 0.7 forward, 0.6 to the right, at arm height
        // Place at fist: push further forward and more to the right
        double spawnX = posX + (forwardX * 1.2) - (rightX * 1.1);
        double spawnY = posY + 1.1;
        double spawnZ = posZ + (forwardZ * 1.2) - (rightZ * 1.1);



        SimpleParticleData core = new SimpleParticleData((ParticleType) KaziParticleTypes.FUGA.get());
        core.setFunction(EasingFunction.ELASTIC_OUT);
        core.setEaseDirection(EasingDirection.POSITIVE);
        core.setLookVec(entity.xRot - 60, entity.yRot + 90);
        core.setColor(1.0F, 0.5F, 0.0F, 1.0F);
        core.setLife(10);
        core.setSize(7.0F);
        core.setMotion(0.0, 0.0, 0.0);
        world.addParticle(core, true, spawnX, spawnY, spawnZ, 0.0, 0.0, 0.0);

        SimpleParticleData ring = new SimpleParticleData((ParticleType) KaziParticleTypes.FUGA.get());
        ring.setFunction(EasingFunction.ELASTIC_OUT);
        ring.setEaseDirection(EasingDirection.POSITIVE);
        ring.setLookVec(entity.xRot - 60, entity.yRot + 90);
        ring.setColor(1.0F, 0.75F, 0.0F, 0.6F);
        ring.setLife(18);
        ring.setSize(10.0F);
        ring.setMotion(0.0, 0.0, 0.0);
        world.addParticle(ring, true, spawnX, spawnY, spawnZ, 0.0, 0.0, 0.0);

        SimpleParticleData tail = new SimpleParticleData((ParticleType) KaziParticleTypes.FUGA.get());
        tail.setFunction(EasingFunction.ELASTIC_OUT);
        tail.setEaseDirection(EasingDirection.NEGATIVE);
        tail.setLookVec(entity.xRot - 60, entity.yRot + 90);
        tail.setColor(1.0F, 0.3F, 0.0F, 0.4F);
        tail.setLife(28);
        tail.setSize(5.0F);
        tail.setMotion(0.0, 0.01, 0.0);
        world.addParticle(tail, true, spawnX, spawnY, spawnZ, 0.0, 0.0, 0.0);
    }
}