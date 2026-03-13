package net.kazi.kazimod.init;

import net.kazi.kazimod.abilities.Tenki.*;
import net.kazi.kazimod.abilities.Toki.*;
import net.kazi.kazimod.abilities.Toshi.*;
import net.kazi.kazimod.api.KaziRegistry;
import net.minecraftforge.fml.RegistryObject;
import xyz.pixelatedw.mineminenomi.api.enums.FruitType;
import xyz.pixelatedw.mineminenomi.items.AkumaNoMiItem;

public class KaziItems {

    public static RegistryObject<AkumaNoMiItem> TOSHI_TOSHI_NO_MI;
    public static RegistryObject<AkumaNoMiItem> TOKI_TOKI_NO_MI;
    public static RegistryObject<AkumaNoMiItem> TENKI_TENKI_NO_MI;

    public static void register() {
        TOSHI_TOSHI_NO_MI = KaziRegistry.registerItem(
                "toshi_toshi_no_mi",
                () -> new AkumaNoMiItem(
                        "Toshi Toshi no Mi",
                        2,
                        FruitType.PARAMECIA,
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
                        "Toki Toki no Mi",
                        1,
                        FruitType.PARAMECIA,
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
                        "Tenki Tenki no Mi",
                        1,
                        FruitType.PARAMECIA,
                        WindGustAbility.INSTANCE,
                        TornadoWrathAbility.INSTANCE,
                        LightningJabAbility.INSTANCE,
                        CloudyDayAbility.INSTANCE,
                        ThunderstormAbility.INSTANCE,
                        GaleStormAbility.INSTANCE



                )
        );
    }
}