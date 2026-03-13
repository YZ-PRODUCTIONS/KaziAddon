package net.kazi.kazimod.particles;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GreenSweepParticle extends SpriteTexturedParticle {

    protected GreenSweepParticle(ClientWorld world, double x, double y, double z) {
        super(world, x, y, z);
        this.rCol = 0.56f;
        this.gCol = 0.93f;
        this.bCol = 0.56f;
        this.alpha = 0.8f;
        this.lifetime = 8;
        this.hasPhysics = false;
        float scale = (1.0f + this.random.nextFloat() * 0.5f) * 100.0f;
        this.quadSize = scale;
    }

    @Override
    public IParticleRenderType getRenderType() {
        return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
        this.alpha = 0.8f * (1.0f - (float) this.age / (float) this.lifetime);
    }

    @Override
    public void render(IVertexBuilder buffer, ActiveRenderInfo renderInfo, float partialTicks) {
        net.minecraft.util.math.vector.Vector3d camPos = renderInfo.getPosition();

        float cx = (float)(this.xo + (this.x - this.xo) * partialTicks - camPos.x);
        float cy = (float)(this.yo + (this.y - this.yo) * partialTicks - camPos.y);
        float cz = (float)(this.zo + (this.z - this.zo) * partialTicks - camPos.z);

        float size = this.quadSize * 0.5f;

        Vector3f[] corners = new Vector3f[]{
                new Vector3f(-1.0f, 0.0f, -1.0f),
                new Vector3f(-1.0f, 0.0f,  1.0f),
                new Vector3f( 1.0f, 0.0f,  1.0f),
                new Vector3f( 1.0f, 0.0f, -1.0f)
        };

        for (Vector3f corner : corners) {
            corner.mul(size);
        }

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();

        int light = this.getLightColor(partialTicks);

        buffer.vertex(cx + corners[0].x(), cy + corners[0].y(), cz + corners[0].z()).uv(u1, v1).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex(cx + corners[1].x(), cy + corners[1].y(), cz + corners[1].z()).uv(u1, v0).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex(cx + corners[2].x(), cy + corners[2].y(), cz + corners[2].z()).uv(u0, v0).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        buffer.vertex(cx + corners[3].x(), cy + corners[3].y(), cz + corners[3].z()).uv(u0, v1).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
    }

    @OnlyIn(Dist.CLIENT)
    public static class Factory implements IParticleFactory<BasicParticleType> {
        private final IAnimatedSprite sprite;

        public Factory(IAnimatedSprite sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(BasicParticleType type, ClientWorld world,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            GreenSweepParticle particle = new GreenSweepParticle(world, x, y, z);
            particle.pickSprite(sprite);
            return particle;
        }
    }
}