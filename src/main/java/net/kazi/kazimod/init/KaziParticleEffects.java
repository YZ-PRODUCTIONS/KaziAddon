package net.kazi.kazimod.init;

import net.kazi.kazimod.api.KaziRegistry;
import net.minecraftforge.eventbus.api.IEventBus;

public class KaziParticleEffects
{

    public static void register(IEventBus eventBus)
    {
        KaziRegistry.PARTICLE_EFFECTS.register(eventBus);
    }
}
