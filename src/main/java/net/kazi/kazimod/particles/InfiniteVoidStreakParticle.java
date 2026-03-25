package net.kazi.kazimod.particles;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;

@OnlyIn(Dist.CLIENT)
public class InfiniteVoidStreakParticle extends SpriteTexturedParticle {

    private float rotation;
    private int age;
    private final float initialAlpha;

    protected InfiniteVoidStreakParticle(final ClientWorld world,
                                         final double x, final double y, final double z,
                                         final double dx, final double dy, final double dz,
                                         final SimpleParticleData data,
                                         final IAnimatedSprite sprite) {
        super(world, x, y, z);
        this.pickSprite(sprite);
        this.age          = 0;
        this.lifetime     = (data.getLife() > 0) ? data.getLife() : 20;
        this.quadSize     = (data.getSize() > 0) ? data.getSize() : 1.5f;
        this.rCol         = data.getRed();
        this.gCol         = data.getGreen();
        this.bCol         = data.getBlue();
        this.alpha        = data.getAlpha();
        this.initialAlpha = data.getAlpha();
        this.hasPhysics   = false;
        // Set velocity so the particle moves across the domain
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        // Orient the slash along the direction of travel
        this.rotation = (float) Math.atan2(dx, dz);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;
        ++this.age;
        if (this.age >= this.lifetime) this.remove();
        // Fade out in last third
        if (this.age > this.lifetime * 0.66f) {
            this.alpha = this.initialAlpha * (1.0f - ((float)(this.age - this.lifetime * 0.66f) / (this.lifetime * 0.34f)));
        }
    }

    @Override
    public void render(final IVertexBuilder buffer, final ActiveRenderInfo renderInfo, final float partialTicks) {
        final float size = this.quadSize;
        final Vector3f[] corners = {
                new Vector3f(-1.0f, -1.0f, 0.0f),
                new Vector3f(-1.0f,  1.0f, 0.0f),
                new Vector3f( 1.0f,  1.0f, 0.0f),
                new Vector3f( 1.0f, -1.0f, 0.0f)
        };
        final Quaternion rotQuat  = new Quaternion(new Vector3f(0.0f, 0.0f, 1.0f), this.rotation, false);
        final Quaternion viewQuat = renderInfo.rotation();
        for (final Vector3f corner : corners) {
            corner.transform(rotQuat);
            corner.transform(viewQuat);
            corner.mul(size);
        }
        final double rx = (this.xo + (this.x - this.xo) * partialTicks) - renderInfo.getPosition().x;
        final double ry = (this.yo + (this.y - this.yo) * partialTicks) - renderInfo.getPosition().y;
        final double rz = (this.zo + (this.z - this.zo) * partialTicks) - renderInfo.getPosition().z;
        final float u0 = this.getU0(), u1 = this.getU1();
        final float v0 = this.getV0(), v1 = this.getV1();
        final int light = this.getLightColor(partialTicks);
        buffer.vertex(rx + corners[0].x(), ry + corners[0].y(), rz + corners[0].z()).uv(u1, v1).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
        buffer.vertex(rx + corners[1].x(), ry + corners[1].y(), rz + corners[1].z()).uv(u1, v0).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
        buffer.vertex(rx + corners[2].x(), ry + corners[2].y(), rz + corners[2].z()).uv(u0, v0).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
        buffer.vertex(rx + corners[3].x(), ry + corners[3].y(), rz + corners[3].z()).uv(u0, v1).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
    }

    @Override
    public IParticleRenderType getRenderType() {
        return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Factory implements IParticleFactory<SimpleParticleData> {
        private final IAnimatedSprite sprite;

        public Factory(final IAnimatedSprite sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(final SimpleParticleData data, final ClientWorld world,
                                       final double x, final double y, final double z,
                                       final double dx, final double dy, final double dz) {
            return new InfiniteVoidStreakParticle(world, x, y, z, dx, dy, dz, data, this.sprite);
        }
    }
}