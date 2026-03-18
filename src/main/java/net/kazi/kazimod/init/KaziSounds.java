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
    public static final RegistryObject<SoundEvent> RED_CHARGE_SFX = KaziRegistry.registerSound("redcharge");
    public static final RegistryObject<SoundEvent> RED_FIRE_SFX = KaziRegistry.registerSound("redfire");
    public static final RegistryObject<SoundEvent> BLACK_FLASH_HIT_SFX = KaziRegistry.registerSound("blackflashhit");
    public static final RegistryObject<SoundEvent> INFINITE_VOID_SFX = KaziRegistry.registerSound("infinitevoid");
    public static final RegistryObject<SoundEvent> INFINITE_VOID_MUSIC_SFX = KaziRegistry.registerSound("infinitevoidmusic");
    public static final RegistryObject<SoundEvent> PURPLE_CHANT_SFX = KaziRegistry.registerSound("purplechant");
    public static final RegistryObject<SoundEvent> HOLLOW_NUKE_MUSIC_SFX = KaziRegistry.registerSound("hollownukemusic");

    public static void register(IEventBus eventBus) {
        KaziRegistry.SOUNDS.register(eventBus);
    }
}