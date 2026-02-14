package net.kazi.kazimod;

import net.kazi.kazimod.init.KaziAbilities;
import net.kazi.kazimod.init.KaziParticleEffects;
import net.kazi.kazimod.init.KaziParticleTypes;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(KaziMod.MODID)
public class KaziMod {

    public static final String MODID = "kazimod";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public KaziMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        KaziAbilities.register(modBus);
        KaziParticleEffects.register(modBus);
        KaziParticleTypes.register(modBus);


        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("kazimod constructed");
    }
}
