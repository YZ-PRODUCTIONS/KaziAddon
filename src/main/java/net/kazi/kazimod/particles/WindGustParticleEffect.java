package net.kazi.kazimod.particles;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.kazi.kazimod.init.KaziParticleTypes;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class WindGustParticleEffect extends ParticleEffect<ParticleEffect.NoDetails> {

    public WindGustParticleEffect() {
    }

    public void spawn(Entity entity, World world, double posX, double posY, double posZ, ParticleEffect.NoDetails details) {
        net.minecraft.util.math.vector.Vector3d look = entity.getLookAngle();

        // Up vector
        double upX = 0;
        double upY = 1;
        double upZ = 0;

        // Right vector (perpendicular to look on XZ plane)
        double rightX = -look.z;
        double rightZ = look.x;

        double t = 0.0;
        while (t < 3.0) {
            ++t;

            for (double theta = 0; theta <= Math.PI; theta += 0.19634954084936207) {
                // Spread left/right with cos, spread up with sin
                double right = t * Math.cos(theta);
                double up = t * Math.sin(theta);

                double worldX = look.x * t + rightX * right;
                double worldY = up;
                double worldZ = look.z * t + rightZ * right;

                SimpleParticleData data = new SimpleParticleData(KaziParticleTypes.GREEN_SWEEP.get())
                        .setSize(10)
                        .setLife(12)
                        .setColor(0.56f, 0.93f, 0.56f)
                        .setHasScaleDecay(true)
                        .setHasMotionDecay(false);

                world.addParticle(
                        data,
                        true,
                        posX + worldX * 1.85,
                        posY + worldY + WyHelper.randomDouble(),
                        posZ + worldZ * 1.85,
                        0.0, 0.0, 0.0
                );
            }
        }
    }
}