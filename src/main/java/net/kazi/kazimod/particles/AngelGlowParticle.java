package net.kazi.kazimod.particles;

import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteTexturedParticle;
import net.minecraft.client.world.ClientWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;

@OnlyIn(Dist.CLIENT)
public class AngelGlowParticle extends SpriteTexturedParticle {

    protected AngelGlowParticle(ClientWorld world, double x, double y, double z, SimpleParticleData data, IAnimatedSprite sprite) {
        super(world, x, y, z);
        this.pickSprite(sprite);
        this.lifetime = data.getLife() > 0 ? data.getLife() : 10;
        this.quadSize = data.getSize() > 0.0F ? data.getSize() : 4.5F;
        this.rCol = data.getRed() > 0.0F ? data.getRed() : 1.0F;
        this.gCol = data.getGreen() > 0.0F ? data.getGreen() : 1.0F;
        this.bCol = data.getBlue() > 0.0F ? data.getBlue() : 1.0F;
        this.alpha = data.getAlpha() > 0.0F ? data.getAlpha() : 0.95F;
        this.hasPhysics = false;
    }

    @Override
    public void tick() {
        super.tick();
        this.alpha *= 0.96F;
    }

    @Override
    public int getLightColor(float partialTicks) {
        return 15728880;
    }

    @Override
    public IParticleRenderType getRenderType() {
        return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Factory implements IParticleFactory<SimpleParticleData> {
        private final IAnimatedSprite sprite;

        public Factory(IAnimatedSprite sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleData data, ClientWorld world, double x, double y, double z, double dx, double dy, double dz) {
            return new AngelGlowParticle(world, x, y, z, data, this.sprite);
        }
    }
}
