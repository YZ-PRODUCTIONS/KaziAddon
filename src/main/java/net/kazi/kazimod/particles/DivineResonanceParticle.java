package net.kazi.kazimod.particles;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteTexturedParticle;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;

@OnlyIn(Dist.CLIENT)
public class DivineResonanceParticle extends SpriteTexturedParticle {

    protected DivineResonanceParticle(ClientWorld world, double x, double y, double z, SimpleParticleData data, IAnimatedSprite sprite) {
        super(world, x, y, z);
        this.pickSprite(sprite);
        this.lifetime = data.getLife() > 0 ? data.getLife() : 100;
        this.quadSize = data.getSize() > 0.0F ? data.getSize() : 230.0F;
        this.alpha = 0.92F;
        this.rCol = 1.0F;
        this.gCol = 0.98F;
        this.bCol = 0.85F;
        this.hasPhysics = false;
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
        this.alpha = 0.92F * (1.0F - ((float) this.age / (float) this.lifetime) * 0.35F);
    }

    @Override
    public void render(IVertexBuilder buffer, ActiveRenderInfo renderInfo, float partialTicks) {
        net.minecraft.util.math.vector.Vector3d camPos = renderInfo.getPosition();
        float cx = (float) (this.xo + (this.x - this.xo) * partialTicks - camPos.x);
        float cy = (float) (this.yo + (this.y - this.yo) * partialTicks - camPos.y);
        float cz = (float) (this.zo + (this.z - this.zo) * partialTicks - camPos.z);
        float size = this.quadSize * 0.5F;

        Vector3f[] corners = new Vector3f[]{
                new Vector3f(-1.0F, 0.0F, -1.0F),
                new Vector3f(-1.0F, 0.0F, 1.0F),
                new Vector3f(1.0F, 0.0F, 1.0F),
                new Vector3f(1.0F, 0.0F, -1.0F)
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
            return new DivineResonanceParticle(world, x, y, z, data, this.sprite);
        }
    }
}
