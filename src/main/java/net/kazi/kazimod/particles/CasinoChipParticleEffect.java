package net.kazi.kazimod.particles;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.kazi.kazimod.init.KaziParticleEffects;
import net.kazi.kazimod.init.KaziParticleTypes;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class CasinoChipParticleEffect extends ParticleEffect<ParticleEffect.NoDetails> {
    public CasinoChipParticleEffect() { super(); }

    @Override
    public void spawn(Entity entity, World world, double x, double y, double z, ParticleEffect.NoDetails details) {
        SimpleParticleData data = new SimpleParticleData(KaziParticleTypes.CASINO_CHIP.get());
        data.setLife(1);
        data.setSize(14.0f);
        data.setMotion(0, 0, 0);
        data.setColor(1f, 1f, 1f, 1f);
        data.setHasScaleDecay(false);
        data.setHasMotionDecay(false);
        data.setRotationSpeed(18.0f);
        world.addParticle(data, true, x, y, z, 0, 0, 0);
    }

    /** Use the registered KaziParticleEffects.CASINO_CHIP instance — never "new CasinoChipParticleEffect()". */
    public static void spawnAt(Entity entity, World world, double x, double y, double z) {
        WyHelper.spawnParticleEffect((ParticleEffect<?>) KaziParticleEffects.CASINO_CHIP.get(), entity, x, y, z);
    }

    public static void spawnBarrier(Entity entity, World world, double x, double y, double z) {
        spawnAt(entity, world, x, y, z);
    }
}