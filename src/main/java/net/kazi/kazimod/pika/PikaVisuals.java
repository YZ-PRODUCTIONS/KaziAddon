package net.kazi.kazimod.pika;

import net.minecraft.entity.Entity;
import net.MrMagicalCart.cartaddon.abilities.pikaextra.*;
import net.MrMagicalCart.cartaddon.particles.effects.pikaextra.*;
import xyz.pixelatedw.mineminenomi.abilities.pika.*;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.particles.effects.pika.*;

/** Pika-only classification, shared by particle delivery, impacts and beam synchronization. */
public final class PikaVisuals {
    public static final int CHARGE=0, FLASH=1, TRAVEL=2, JEWEL=3, CUT_CHARGE=4, CUT_TRAIL=5, LOGIA=6, IMPACT=7;
    private PikaVisuals(){}
    public static int kind(ParticleEffect<?> effect) {
        if(effect instanceof ChargingPikaParticleEvent)return CHARGE;
        if(effect instanceof FlashParticleEffect)return FLASH;
        if(effect instanceof YataNoKagamiParticleEffect)return TRAVEL;
        if(effect instanceof JewelFlashParticleEffect)return JEWEL;
        if(effect instanceof DivineCutParticleEffect)return CUT_CHARGE;
        if(effect instanceof DivineCutProjectileParticleEffect)return CUT_TRAIL;
        if(effect==ModParticleEffects.PIKA_LOGIA.get())return LOGIA;
        return -1;
    }
    public static int beamKind(LightningEntity beam) {
        AbilityCore<?> core=beam.getParent();
        if(core==AmaterasuAbility.INSTANCE||core==ReworkedAmaterasuAbility.INSTANCE)return 1;
        if(core==YataNoKagamiAbility.INSTANCE||core==ReworkedYataNoKagamiAbility.INSTANCE)return 2;
        return 0;
    }
    public static void explode(ExplosionAbility explosion,Entity source) {
        ((PikaExplosionVisuals)explosion).kazi$pikaExplosion(source.getId());
        explosion.doExplosion();
    }
}
