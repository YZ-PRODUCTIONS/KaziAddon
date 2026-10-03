package net.kazi.kazimod.init;

import net.kazi.kazimod.api.KaziRegistry;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;

public class KaziSounds {

    public static final RegistryObject<SoundEvent> EA_CAST_SFX = KaziRegistry.registerSound("Ea Cast");
    public static final RegistryObject<SoundEvent> WINDS_RAPTURE_CHARGE_SFX =
            KaziRegistry.registerSound("Winds Rapture Charge");
    public static final RegistryObject<SoundEvent> WINDS_RAPTURE_RELEASE_SFX =
            KaziRegistry.registerSound("Winds Rapture Release");

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
    public static final RegistryObject<SoundEvent> SSSTIK_SFX = KaziRegistry.registerSound("ssstik");
    public static final RegistryObject<SoundEvent> INFINITE_VOID_SFX = KaziRegistry.registerSound("infinitevoid");
    public static final RegistryObject<SoundEvent> INFINITE_VOID_MUSIC_SFX = KaziRegistry.registerSound("infinitevoidmusic");
    public static final RegistryObject<SoundEvent> PURPLE_CHANT_SFX = KaziRegistry.registerSound("purplechant");
    public static final RegistryObject<SoundEvent> HOLLOW_NUKE_MUSIC_SFX = KaziRegistry.registerSound("hollownukemusic");
    public static final RegistryObject<SoundEvent> REALITY_MARBLE_CHARGE_MUSIC_SFX =
            KaziRegistry.registerSound("Reality Marble Charge Music");
    public static final RegistryObject<SoundEvent> INFINITE_CREATION_CHARGE_SECONDARY_SFX =
            KaziRegistry.registerSound("Infinite Creation Charge Secondary");
    public static final RegistryObject<SoundEvent> MUGEN_CHARGE_SECONDARY_SFX =
            KaziRegistry.registerSound("Mugen Charge Secondary");
    public static final RegistryObject<SoundEvent> UNLIMITED_LOST_WORKS_HIT_SFX =
            KaziRegistry.registerSound("Unlimited Lost Works Hit");
    public static final RegistryObject<SoundEvent> CALADBOLG_CHARGE_SFX =
            KaziRegistry.registerSound("Caladbolg Charge");
    public static final RegistryObject<SoundEvent> ENHANCEMENT_THIRD_CHARGE_SFX =
            KaziRegistry.registerSound("Enhancement Third Charge");
    public static final RegistryObject<SoundEvent> ENHANCEMENT_FOURTH_CHARGE_SFX =
            KaziRegistry.registerSound("Enhancement Fourth Charge");
    public static final RegistryObject<SoundEvent> GOJO_BOSS_ENTRANCE_SFX   =
            KaziRegistry.registerSound("gojobossentrance");
    public static final RegistryObject<SoundEvent> SUKUNA_BOSS_ENTRANCE_SFX =
            KaziRegistry.registerSound("sukunabossentrance");
    public static final RegistryObject<SoundEvent> LUFFY_BOSS_ENTRANCE_SFX  =
            KaziRegistry.registerSound("luffybossentrance");


    public static void register(IEventBus eventBus) {
        KaziRegistry.SOUNDS.register(eventBus);
    }
}
