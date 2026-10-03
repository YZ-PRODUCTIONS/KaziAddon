package net.kazi.kazimod.init;

import net.kazi.kazimod.abilities.TripelT.*;

import net.kazi.kazimod.abilities.Koku.*;
import net.kazi.kazimod.abilities.Kake.CasinoRollAbility;
import net.kazi.kazimod.abilities.Kake.LuckySlotAbility;
import net.kazi.kazimod.abilities.Kake.SlotSpinAbility;
import net.kazi.kazimod.abilities.AkumaRework.*;
import net.kazi.kazimod.abilities.Kyoka.IllusionCloneBarrageAbility;
import net.kazi.kazimod.abilities.Kyoka.IllusionCounterAbility;
import net.kazi.kazimod.abilities.Kyoka.InvisibleExecutionAbility;
import net.kazi.kazimod.abilities.Kyoka.KanzenSaiminAbility;
import net.kazi.kazimod.abilities.Kyoka.GoryutenmetsuAbility;
import net.kazi.kazimod.abilities.Kyoka.KurohitsugiAbility;
import net.kazi.kazimod.abilities.Nusu.SkillBookCreationAbility;
import net.kazi.kazimod.abilities.Nusu.SkillHunterAbility;
import net.kazi.kazimod.abilities.Nusu.SkillHunterEXAbility;
import net.kazi.kazimod.abilities.Nusu.SkillRemoverAbility;
import net.kazi.kazimod.abilities.SakuRework.*;
import net.kazi.kazimod.abilities.Tenki.*;
import net.kazi.kazimod.abilities.Toki.*;
import net.kazi.kazimod.abilities.Toshi.*;
import net.kazi.kazimod.api.KaziRegistry;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.loading.FMLPaths;
import xyz.pixelatedw.mineminenomi.api.enums.FruitType;
import xyz.pixelatedw.mineminenomi.items.AkumaNoMiItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class KaziItems {
    public static RegistryObject<AkumaNoMiItem> KI_KI_NO_MI_MODEL_TUNG_TUNG_TUNG_SAHUR;

    public static RegistryObject<AkumaNoMiItem> TOSHI_TOSHI_NO_MI;
    public static RegistryObject<AkumaNoMiItem> TOKI_TOKI_NO_MI;
    public static RegistryObject<AkumaNoMiItem> TENKI_TENKI_NO_MI;
    public static RegistryObject<AkumaNoMiItem> KOKU_KOKU_NO_MI;
    public static RegistryObject<AkumaNoMiItem> KAKE_KAKE_NO_MI;
    public static RegistryObject<AkumaNoMiItem> NUSU_NUSU_NO_MI;
    public static RegistryObject<AkumaNoMiItem> SAKU_SAKU_NO_MI;
    public static RegistryObject<AkumaNoMiItem> AKUMA_AKUMA_NO_MI;
    public static RegistryObject<AkumaNoMiItem> KYOKA_KYOKA_NO_MI;

    /**
     * Reads the kazimod.toml config file directly from disk before Forge's
     * config system is available, so we can conditionally skip item registration.
     * Falls back to false (items enabled) if the file doesn't exist yet.
     */
    private static boolean isFruitChangesDisabled() {
        try {
            Path configPath = FMLPaths.CONFIGDIR.get().resolve("kazimod.toml");
            if (!Files.exists(configPath)) return false;
            for (String line : Files.readAllLines(configPath)) {
                String trimmed = line.trim();
                if (trimmed.startsWith("disableFruitChanges")) {
                    return trimmed.contains("true");
                }
            }
        } catch (IOException e) {
            // If we can't read the file, default to enabled
        }
        return false;
    }

    public static void register() {
        if (isFruitChangesDisabled()) return;

        TOSHI_TOSHI_NO_MI = KaziRegistry.registerItem(
                "toshi_toshi_no_mi",
                () -> new AkumaNoMiItem(
                        "Toshi Toshi no Mi", 2, FruitType.PARAMECIA,
                        DeAgedAbility.INSTANCE,
                        FutureOfFreedomAbility.INSTANCE,
                        AgeAccelerationAbility.INSTANCE,
                        GiantFutureAbility.INSTANCE,
                        GiantPunchAbility.INSTANCE
                )
        );

        TOKI_TOKI_NO_MI = KaziRegistry.registerItem(
                "toki_toki_no_mi",
                () -> new AkumaNoMiItem(
                        "Toki Toki no Mi", 1, FruitType.PARAMECIA,
                        TimeTheftAbility.INSTANCE,
                        TimeReversalAbility.INSTANCE,
                        TimeAccelerationAbility.INSTANCE,
                        ChronostasisAbility.INSTANCE,
                        ChronostasisGrigoraAbility.INSTANCE,
                        TimeBarAbility.INSTANCE
                )
        );

        TENKI_TENKI_NO_MI = KaziRegistry.registerItem(
                "tenki_tenki_no_mi",
                () -> new AkumaNoMiItem(
                        "Tenki Tenki no Mi", 1, FruitType.PARAMECIA,
                        WindGustAbility.INSTANCE,
                        TornadoWrathAbility.INSTANCE,
                        LightningJabAbility.INSTANCE,
                        CloudyDayAbility.INSTANCE,
                        ThunderstormAbility.INSTANCE,
                        GaleStormAbility.INSTANCE
                )
        );

        KOKU_KOKU_NO_MI = KaziRegistry.registerItem(
                "koku_koku_no_mi",
                () -> new AkumaNoMiItem(
                        "Koku Koku no Mi", 1, FruitType.PARAMECIA,
                        RedAbility.INSTANCE,
                        LapseBlueAbility.INSTANCE,
                        MaxOutputLapseBlueAbility.INSTANCE,
                        HollowPurpleAbility.INSTANCE,
                        InfinityAbility.INSTANCE,
                        DomainExpansionInfiniteVoidAbility.INSTANCE
                )
        );

        KAKE_KAKE_NO_MI = KaziRegistry.registerItem(
                "kake_kake_no_mi",
                () -> new AkumaNoMiItem(
                        "Kake Kake no Mi", 2, FruitType.PARAMECIA,
                        SlotSpinAbility.INSTANCE,
                        LuckySlotAbility.INSTANCE,
                        CasinoRollAbility.INSTANCE
                )
        );

        NUSU_NUSU_NO_MI = KaziRegistry.registerItem(
                "nusu_nusu_no_mi",
                () -> new AkumaNoMiItem(
                        "Nusu Nusu no Mi", 1, FruitType.PARAMECIA,
                        SkillHunterAbility.INSTANCE,
                        SkillHunterEXAbility.INSTANCE,
                        SkillRemoverAbility.INSTANCE,
                        SkillBookCreationAbility.INSTANCE
                )
        );





        KYOKA_KYOKA_NO_MI = KaziRegistry.registerItem(
                "kyoka_kyoka_no_mi",
                () -> new AkumaNoMiItem(
                        "Kyoka Kyoka no Mi", 1, FruitType.PARAMECIA,
                        KanzenSaiminAbility.INSTANCE,
                        KurohitsugiAbility.INSTANCE,
                        GoryutenmetsuAbility.INSTANCE,
                        IllusionCloneBarrageAbility.INSTANCE,
                        InvisibleExecutionAbility.INSTANCE,
                        IllusionCounterAbility.INSTANCE
                )
        );
    
        KI_KI_NO_MI_MODEL_TUNG_TUNG_TUNG_SAHUR = KaziRegistry.registerItem(
                "ki_ki_no_mi_model_tung_tung_tung_sahur",
                () -> new AkumaNoMiItem(
                        "Ki Ki no mi Model: Tung Tung Tung Sahur", 3, FruitType.MYTHICAL_ZOAN,
                        TripelTFormAbility.INSTANCE,
                        TripelTGodFormAbility.INSTANCE,
                        TripelTFlightAbility.INSTANCE,
                        HomeRunSwingAbility.INSTANCE,
                        TungTungTungBarrageAbility.INSTANCE,
                        SahurYellAbility.INSTANCE,
                        SwingingCounterAbility.INSTANCE
                )
        );
    }
}
