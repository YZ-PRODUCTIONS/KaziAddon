package net.kazi.kazimod.particles;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.BasicParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CleaveParticle extends SpriteTexturedParticle {

    private final IAnimatedSprite spriteSet;

    protected CleaveParticle(ClientWorld world, double x, double y, double z,
                             double motionX, double motionY, double motionZ,
                             IAnimatedSprite spriteSet) {
        super(world, x, y, z, motionX, motionY, motionZ);
        this.spriteSet = spriteSet;

        // Appearance
        this.lifetime = 12;
        this.quadSize = 0.6f + world.random.nextFloat() * 0.4f;
        this.alpha = 1.0f;

        // Color: sharp steel-blue with a red tinge for a cleave effect
        this.rCol = 0.85f;
        this.gCol = 0.15f;
        this.bCol = 0.2f;

        // Motion: spread outward in an arc
        this.xd = motionX + (world.random.nextDouble() - 0.5) * 0.3;
        this.yd = motionY + 0.05 + world.random.nextDouble() * 0.1;
        this.zd = motionZ + (world.random.nextDouble() - 0.5) * 0.3;

        this.gravity = 0.02f;
        this.hasPhysics = false;

        this.setSpriteFromAge(spriteSet);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        // Fade out over lifetime
        this.alpha = 1.0f - ((float) this.age / (float) this.lifetime);

        // Expand the particle slightly as it ages for a sweeping feel
        this.quadSize += 0.02f;

        this.setSpriteFromAge(spriteSet);

        this.xd *= 0.92;
        this.yd -= this.gravity;
        this.zd *= 0.92;

        this.move(this.xd, this.yd, this.zd);
    }

    @Override
    public IParticleRenderType getRenderType() {
        return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    @OnlyIn(Dist.CLIENT)
    public static class Factory implements IParticleFactory<BasicParticleType> {

        private final IAnimatedSprite spriteSet;

        public Factory(IAnimatedSprite spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(BasicParticleType type, ClientWorld world,
                                       double x, double y, double z,
                                       double mx, double my, double mz) {
            return new CleaveParticle(world, x, y, z, mx, my, mz, spriteSet);
        }
    }
}