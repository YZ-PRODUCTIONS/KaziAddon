package net.kazi.kazimod.particles;

import net.kazi.kazimod.init.KaziParticleTypes;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

public class InfiniteVoidStreakParticleEffect extends ParticleEffect<ParticleEffect.NoDetails> {

    private static final double RADIUS = 24.0;

    @Override
    public void spawn(Entity entity, World world, double cx, double cy, double cz, ParticleEffect.NoDetails details) {
        // One shared direction for all streaks this call — like rain going sideways
        double yaw = world.random.nextDouble() * Math.PI * 2.0;
        double velDirX = Math.cos(yaw);
        double velDirZ = Math.sin(yaw);

        // Perpendicular horizontal axis for spreading spawn points
        double perpX = -velDirZ;
        double perpZ =  velDirX;

        double speed = (2.0 * RADIUS) / 3.0;
        double velX = velDirX * speed;
        double velZ = velDirZ * speed;

        // Many parallel streaks filling a cylinder — dense grid of spawn points
        // across the perpendicular axis and vertical axis (upper half only)
        int streakCount = 20 + world.random.nextInt(10); // 20-30 streaks per call

        for (int s = 0; s < streakCount; s++) {
            // Cylindrical distribution: random point inside a circle
            // on the plane perpendicular to travel direction
            double r = RADIUS * Math.sqrt(world.random.nextDouble()); // uniform disk distribution
            double angle = world.random.nextDouble() * Math.PI * 2.0;

            // Spread on perp axis (horizontal) and vertical axis
            double perpOffset   = r * Math.cos(angle);
            double heightOffset = r * Math.sin(angle);

            // Upper half only: flip negative heights to positive
            if (heightOffset < 0) heightOffset = -heightOffset * 0.3; // squash bottom to near-center

            // Start on the entry wall of the domain
            double startX = cx - velDirX * RADIUS + perpX * perpOffset;
            double startY = cy + heightOffset;
            double startZ = cz - velDirZ * RADIUS + perpZ * perpOffset;

            boolean isPink = world.random.nextBoolean();

            // Dense particle cluster per streak
            int count = 30 + world.random.nextInt(15); // 30-45 per streak
            for (int i = 0; i < count; i++) {
                double t = (i / (double)(count - 1)) * RADIUS * 0.12;
                double posX = startX + velDirX * t;
                double posY = startY;
                double posZ = startZ + velDirZ * t;

                SimpleParticleData data = new SimpleParticleData(
                        isPink
                                ? (net.minecraft.particles.ParticleType<?>) KaziParticleTypes.INFINITE_VOID_STREAK_PINK.get()
                                : (net.minecraft.particles.ParticleType<?>) KaziParticleTypes.INFINITE_VOID_STREAK.get()
                );
                if (isPink) {
                    data.setColor(1.0f, 0.3f, 0.7f, 1.0f);
                } else {
                    data.setColor(1.0f, 0.02f, 0.02f, 1.0f);
                }
                data.setLife(20);
                data.setSize(0.8f);

                world.addParticle(data, true, posX, posY, posZ, velX, 0.0, velZ);
            }
        }
    }
}