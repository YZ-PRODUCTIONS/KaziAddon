package net.kazi.kazimod.init;

import net.kazi.kazimod.api.KaziRegistry;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;

public class KaziSounds {

    public static final RegistryObject<SoundEvent> DISMANTLE_SFX = KaziRegistry.registerSound("Dismantle");
    public static final RegistryObject<SoundEvent> CLEAVE_START_SFX = KaziRegistry.registerSound("Cleave Start");
    public static final RegistryObject<SoundEvent> CLEAVE_HIT_SFX = KaziRegistry.registerSound("Cleave Hit");
    public static final RegistryObject<SoundEvent> SHRINE_START_SFX = KaziRegistry.registerSound("Shrine Start");
    public static final RegistryObject<SoundEvent> SHRINE_MUSIC_SFX = KaziRegistry.registerSound("Shrine Music");
    public static final RegistryObject<SoundEvent> FUGA_SFX = KaziRegistry.registerSound("Fuga");
    public static final RegistryObject<SoundEvent> FUGA_HIT_SFX = KaziRegistry.registerSound("Fuga Hit");
    public static final RegistryObject<SoundEvent> TIMETHEFT_SFX = KaziRegistry.registerSound("Timetheft");
    public static final RegistryObject<SoundEvent> TIMEACCELERATION_SFX = KaziRegistry.registerSound("Timeacceleration");
    public static final RegistryObject<SoundEvent> CHRONOSTASIS_SFX = KaziRegistry.registerSound("Chronostasis");

    public static void register(IEventBus eventBus) {
        KaziRegistry.SOUNDS.register(eventBus);
    }
}