package net.kazi.kazimod.yami;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.particles.effects.yami.*;

/** Common-side classification; these visuals must not pass through particle settings. */
public final class YamiVisuals {
    private YamiVisuals(){}
    public static int kind(ParticleEffect<?> effect,Entity source) {
        if(effect instanceof BlackHoleParticleEffect)return 0;
        if(effect instanceof DarkMatterChargingParticleEffect)return source instanceof LivingEntity?1:0;
        // Kurouzu deliberately keeps MMNM's original smoke and hand-spiral particles.
        if(effect instanceof DarkMatterParticleEffect)return 4;
        if(effect instanceof BlackWorldParticleEffect)return 5;
        return -1;
    }
}
