package net.kazi.kazimod.particles;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;

@OnlyIn(Dist.CLIENT)
public class GojoPurpleParticle extends SpriteTexturedParticle {

    private float rotation;
    private final float rotationSpeed;
    private int age = 0;
    private static final float BASE_SIZE = 4.5F;
    private static final float PULSE_AMOUNT = 0.9F;
    private static final float PULSE_SPEED = 0.15F;

    protected GojoPurpleParticle(ClientWorld world, double x, double y, double z,
                                 SimpleParticleData data, IAnimatedSprite sprite) {
        super(world, x, y, z);
        this.pickSprite(sprite);

        this.lifetime = 2;
        this.quadSize = BASE_SIZE;
        this.rCol = 1.0F;
        this.gCol = 1.0F;
        this.bCol = 1.0F;
        this.alpha = 0.9F;
        this.hasPhysics = false;

        this.rotation = 0.0F;
        this.rotationSpeed = 0.1F;
    }

    @Override
    public void tick() {
        super.tick();
        rotation += rotationSpeed;
        age++;
        this.quadSize = BASE_SIZE + PULSE_AMOUNT * (float) Math.sin(age * PULSE_SPEED * Math.PI * 2);
    }

    @Override
    public void render(IVertexBuilder buffer, ActiveRenderInfo renderInfo, float partialTicks) {
        float prevSize = BASE_SIZE + PULSE_AMOUNT * (float) Math.sin((age - 1) * PULSE_SPEED * Math.PI * 2);
        float interpolatedSize = prevSize + (this.quadSize - prevSize) * partialTicks;

        net.minecraft.util.math.vector.Vector3f[] corners = new net.minecraft.util.math.vector.Vector3f[]{
                new net.minecraft.util.math.vector.Vector3f(-1.0F, -1.0F, 0.0F),
                new net.minecraft.util.math.vector.Vector3f(-1.0F,  1.0F, 0.0F),
                new net.minecraft.util.math.vector.Vector3f( 1.0F,  1.0F, 0.0F),
                new net.minecraft.util.math.vector.Vector3f( 1.0F, -1.0F, 0.0F)
        };

        net.minecraft.util.math.vector.Quaternion rot =
                new net.minecraft.util.math.vector.Quaternion(
                        new net.minecraft.util.math.vector.Vector3f(0, 0, 1),
                        rotation + rotationSpeed * partialTicks,
                        false
                );

        for (net.minecraft.util.math.vector.Vector3f corner : corners) {
            corner.transform(rot);
            corner.transform(renderInfo.rotation());
            corner.mul(interpolatedSize);
        }

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();

        double rx = this.x - renderInfo.getPosition().x;
        double ry = this.y - renderInfo.getPosition().y;
        double rz = this.z - renderInfo.getPosition().z;

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

        public Factory(IAnimatedSprite sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleData data, ClientWorld world,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new GojoPurpleParticle(world, x, y, z, data, sprite);
        }
    }
}