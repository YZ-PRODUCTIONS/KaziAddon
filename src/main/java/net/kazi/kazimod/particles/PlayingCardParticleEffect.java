package net.kazi.kazimod.particles;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.kazi.kazimod.init.KaziParticleEffects;
import net.kazi.kazimod.init.KaziParticleTypes;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class PlayingCardParticleEffect extends ParticleEffect<ParticleEffect.NoDetails> {

    public PlayingCardParticleEffect() { super(); }

    @Override
    public void spawn(Entity entity, World world, double x, double y, double z, ParticleEffect.NoDetails details) {
        SimpleParticleData data = new SimpleParticleData(KaziParticleTypes.PLAYING_CARD.get());
        data.setLife(4);          // matches 4-tick spawn interval exactly — no gap between replacements
        data.setSize(40.0f);
        data.setMotion(0, 0, 0);
        data.setColor(1f, 1f, 1f, 1f);
        data.setHasScaleDecay(false);
        data.setHasMotionDecay(false);
        data.setRotationSpeed(0.0f); // fixed orientation — every replacement looks identical, no flicker
        world.addParticle(data, true, x, y, z, 0, 0, 0);
    }

    public static void spawnAt(Entity entity, World world, double x, double y, double z) {
        WyHelper.spawnParticleEffect(
                (ParticleEffect<?>) KaziParticleEffects.PLAYING_CARD.get(),
                entity, x, y, z);
    }
}