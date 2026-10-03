package net.kazi.kazimod.preserved.cerberus;

import net.minecraft.potion.*;
import xyz.pixelatedw.mineminenomi.api.abilities.*;

public final class CerberusHuntPassive extends PassiveAbility2 {
    public CerberusHuntPassive(AbilityCore<CerberusHuntPassive> core){
        super(core);
        addDuringPassiveEvent(entity->{
            if(entity.level.isClientSide || !CerberusCombat.active(entity))return;
            EffectInstance vision=entity.getEffect(Effects.NIGHT_VISION);
            if(vision==null || vision.getDuration()<250)entity.addEffect(new EffectInstance(Effects.NIGHT_VISION,500,0,true,false));
        });
    }
}
