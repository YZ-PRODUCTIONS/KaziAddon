package net.kazi.kazimod.moku;

import net.MrMagicalCart.cartaddon.particles.effects.mokuextra.WorldOfSmokeParticleEffect;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.particles.effects.moku.*;

public final class MokuVisuals {
    public static final int LOGIA=0,FLIGHT=1,LAUNCH=2,STRIKE=3,DEBUFF=4,WORLD=5,BURST=6;
    private MokuVisuals(){}
    public static int kind(ParticleEffect<?> effect) {
        if(effect==ModParticleEffects.MOKU_LOGIA.get())return LOGIA;
        if(effect==ModParticleEffects.MOKU_FLY.get())return FLIGHT;
        if(effect instanceof WhiteLauncherParticleEffect)return LAUNCH;
        if(effect instanceof WhiteStrikeParticleEffect)return STRIKE;
        if(effect instanceof SmokeDebuffParticleEffect)return DEBUFF;
        if(effect instanceof WorldOfSmokeParticleEffect)return WORLD;
        return -1;
    }
    public static float radius(int kind,ParticleEffect.Details details) {
        if(kind==WORLD) {
            float scale=details instanceof WorldOfSmokeParticleEffect.Details?((WorldOfSmokeParticleEffect.Details)details).getRadiusScale():1;
            return Float.isFinite(scale)?Math.max(2,Math.min(32,scale*4)):4;
        }
        return kind==STRIKE?3:kind==BURST?2:1;
    }
    public static boolean follows(int kind){return kind==LOGIA||kind==FLIGHT||kind==DEBUFF||kind==WORLD;}
}
