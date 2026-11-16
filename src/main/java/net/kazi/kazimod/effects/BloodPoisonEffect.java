package net.kazi.kazimod.effects;

import xyz.pixelatedw.mineminenomi.api.effects.DamageOverTimeEffect;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;

/** Doku's poison cadence and damage type, at half strength without contact transmission. */
public final class BloodPoisonEffect extends DamageOverTimeEffect {
    public BloodPoisonEffect() {
        super(ModDamageSource.POISON, 5F, 20);
        setDamageFunction(amplifier -> getBaseDamage() + amplifier * .5F);
    }
    @Override
    public void applyEffectTick(net.minecraft.entity.LivingEntity entity, int amplifier) {
        // Saved poison must not damage someone who has since acquired the blood fruit.
        if(!net.kazi.kazimod.blood.BloodFeatures.fruit(entity))super.applyEffectTick(entity,amplifier);
    }
}
