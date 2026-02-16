package net.kazi.kazimod.effects;

import net.MrMagicalCart.cartaddon.init.CartEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;
import net.kazi.kazimod.init.KaziEffects;

public class TiredEffect extends Effect {

    public TiredEffect() {
        super(EffectType.HARMFUL, 0x2C2C2C); // Dark gray color
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (amplifier >= 6) {
            EffectInstance tired = entity.getEffect((Effect)KaziEffects.TIRED.get());
            if (tired != null) {
                int duration = tired.getDuration();
                entity.addEffect(new EffectInstance((Effect)CartEffects.SLEEPY.get(), 120, 0));
            }

            entity.removeEffect((Effect)KaziEffects.TIRED.get());
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // This determines when applyEffectTick is called
        // We return true every tick to keep tracking
        return true;
    }
}