package net.kazi.kazimod.particles;

import net.kazi.kazimod.init.KaziParticleTypes;
import net.minecraft.entity.Entity;
import net.minecraft.particles.ParticleType;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.math.EasingDirection;
import xyz.pixelatedw.mineminenomi.api.math.EasingFunction;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

public class InfiniteVoidParticleEffect extends ParticleEffect<ParticleEffect.NoDetails> {

    @Override
    public void spawn(Entity entity, World world, double x, double y, double z, ParticleEffect.NoDetails details) {
        SimpleParticleData data = new SimpleParticleData(
                (ParticleType) KaziParticleTypes.INFINITE_VOID.get());
        data.setFunction(EasingFunction.SINE_IN_OUT);
        data.setEaseDirection(EasingDirection.POSITIVE);
        data.setLookVec(entity.xRot, entity.yRot);
        data.setLife(2); // short life — ability respawns this every tick for seamless loop
        data.setSize(3.0f);
        data.setMotion(0, 0, 0);
        world.addParticle(data, true, x, y, z, 0, 0, 0);
    }
}