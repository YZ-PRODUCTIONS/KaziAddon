package net.kazi.kazimod.particles;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleType;
import net.minecraft.world.World;
import net.kazi.kazimod.init.KaziParticleTypes;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class GreenTornadoParticleEffect extends ParticleEffect<GreenTornadoParticleEffect.Details> {

    public GreenTornadoParticleEffect() {
        super(Details::new);
    }

    @Override
    public void spawn(Entity entity, World world, double posX, double posY, double posZ, Details details) {
        int angle = 0;
        int maxHeight = (int)(details.size * 1.25F);
        double minRadius = (double)(details.size / 7.0F);
        double maxRadius = (double)(details.size * 3.0F);
        int lines = 6;
        double heightIncrease = 0.15;
        double radiusIncrement = maxRadius / (double)maxHeight / 4.0F;




        angle += 2;
    }

    public static class Details extends ParticleEffect.Details {
        float size;

        public Details() {}

        @Override
        public void save(CompoundNBT nbt) {
            nbt.putFloat("size", this.size);
        }

        @Override
        public void load(CompoundNBT nbt) {
            this.size = nbt.getFloat("size");
        }

        public float getSize() { return this.size; }
        public void setSize(float size) { this.size = size; }
    }
}