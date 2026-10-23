package net.kazi.kazimod.init;

import net.kazi.kazimod.api.KaziRegistry;
import net.kazi.kazimod.morphs.TripelTGodMorphInfo;
import net.kazi.kazimod.morphs.TripelTMorphInfo;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;

public class KaziMorphs {

    public static RegistryObject<TripelTMorphInfo> TRIPEL_T;
    public static RegistryObject<TripelTGodMorphInfo> TRIPEL_T_GOD;
    public static RegistryObject<net.kazi.kazimod.morphs.WorldTurtleMorphInfo> WORLD_TURTLE;

    public static void register(IEventBus eventBus) {
        TRIPEL_T = KaziRegistry.MORPHS.register("tripel_t", TripelTMorphInfo::new);
        TRIPEL_T_GOD = KaziRegistry.MORPHS.register("tripel_t_god", TripelTGodMorphInfo::new);
        WORLD_TURTLE = KaziRegistry.MORPHS.register("world_turtle", net.kazi.kazimod.morphs.WorldTurtleMorphInfo::new);
    }
}
