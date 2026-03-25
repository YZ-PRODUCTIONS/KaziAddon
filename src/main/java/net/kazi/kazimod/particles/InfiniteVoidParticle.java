package net.kazi.kazimod.particles;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteTexturedParticle;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;

@OnlyIn(Dist.CLIENT)
public class InfiniteVoidParticle extends SpriteTexturedParticle {

    private float rotation;
    private final float rotationSpeed;
    private int age;
    private static final float BASE_SIZE = 3.0f;

    protected InfiniteVoidParticle(ClientWorld world, double x, double y, double z,
                                   SimpleParticleData data, IAnimatedSprite sprite) {
        super(world, x, y, z);
        this.pickSprite(sprite);
        this.age = 0;
        // maxAge: use the life value from SimpleParticleData if available,
        // otherwise default to 320 to outlast the full domain duration
        this.lifetime = data.getLife() > 0 ? data.getLife() : 320;
        this.quadSize = BASE_SIZE;
        this.rCol = 1.0f;
        this.gCol = 1.0f;
        this.bCol = 1.0f;
        this.alpha = 0.85f;
        this.hasPhysics = false;
        this.rotation = 0.0f;
        this.rotationSpeed = 0.02f;
    }

    @Override
    public void tick() {
        super.tick();
        this.rotation += this.rotationSpeed;
        this.age++;
        this.quadSize = BASE_SIZE;
    }

    @Override
    public void render(IVertexBuilder buffer, ActiveRenderInfo renderInfo, float partialTicks) {
        float size = this.quadSize;

        Vector3f[] corners = new Vector3f[]{
                new Vector3f(-1.0f, -1.0f, 0.0f),
                new Vector3f(-1.0f,  1.0f, 0.0f),
                new Vector3f( 1.0f,  1.0f, 0.0f),
                new Vector3f( 1.0f, -1.0f, 0.0f)
        };

        Quaternion rotQuat = new Quaternion(
                new Vector3f(0.0f, 0.0f, 1.0f),
                this.rotation + this.rotationSpeed * partialTicks,
                false
        );
        Quaternion viewQuat = renderInfo.rotation();

        for (Vector3f corner : corners) {
            corner.transform(rotQuat);
            corner.transform(viewQuat);
            corner.mul(size);
        }

        double rx = this.x - renderInfo.getPosition().x;
        double ry = this.y - renderInfo.getPosition().y;
        double rz = this.z - renderInfo.getPosition().z;

        float u0 = this.getU0(), u1 = this.getU1();
        float v0 = this.getV0(), v1 = this.getV1();
        int light = this.getLightColor(partialTicks);

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
        public Factory(IAnimatedSprite sprite) { this.sprite = sprite; }

        @Override
        public Particle createParticle(SimpleParticleData data, ClientWorld world,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new InfiniteVoidParticle(world, x, y, z, data, sprite);
        }
    }
}