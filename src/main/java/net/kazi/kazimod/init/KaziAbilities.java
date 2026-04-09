package net.kazi.kazimod.init;


import net.kazi.kazimod.abilities.AwaRework.GoldenHourRework;
import net.kazi.kazimod.abilities.AxeStyleRework.*;
import net.kazi.kazimod.abilities.BaneRework.SpringSnipeRework;
import net.kazi.kazimod.abilities.BaraRework.KuchuKirimomiDaiCircusRework;
import net.kazi.kazimod.abilities.BariRework.BarrierGuardAbility;
import net.kazi.kazimod.abilities.BlacklegRework.AntiMatterKickCourseRework;
import net.kazi.kazimod.abilities.BlacklegRework.PartyTableKickCourseRework;
import net.kazi.kazimod.abilities.BomuRework.*;
import net.kazi.kazimod.abilities.BrawlerRework.*;
import net.kazi.kazimod.abilities.BuddhaRework.HitoDaibutsuPointRework;
import net.kazi.kazimod.abilities.ChiyuRework.ChiyupopoRework;
import net.kazi.kazimod.abilities.DekaRework.DekaDekaRework;
import net.kazi.kazimod.abilities.DoctorRework.*;
import net.kazi.kazimod.abilities.DokuRework.VenomRoadRework;
import net.kazi.kazimod.abilities.GasuRework.GastilleRework;
import net.kazi.kazimod.abilities.GasuRework.KarakuniRework;
import net.kazi.kazimod.abilities.GomuRework.*;
import net.kazi.kazimod.abilities.GoroRework.ElThorRework;
import net.kazi.kazimod.abilities.GoroRework.SangoRework;
import net.kazi.kazimod.abilities.HakiRework.BusoshokuHakiFullBodyHardeningRework;
import net.kazi.kazimod.abilities.HakiRework.HakiSenseAbility;
import net.kazi.kazimod.abilities.HakiRework.KenbunshokuHakiFutureSightRework;
import net.kazi.kazimod.abilities.HieRework.IceAgeRework;
import net.kazi.kazimod.abilities.HoroRework.MiniHollowRework;
import net.kazi.kazimod.abilities.HoroRework.NegativeHollowRework;
import net.kazi.kazimod.abilities.HoroRework.TokuHollowRework;
import net.kazi.kazimod.abilities.HumanRework.KamieRework;
import net.kazi.kazimod.abilities.HumanRework.SoruRework;
import net.kazi.kazimod.abilities.ItoRework.GodThreadRework;
import net.kazi.kazimod.abilities.JikiRework.DamnedPunkRework;
import net.kazi.kazimod.abilities.JikiRework.GenocideRaidRework;
import net.kazi.kazimod.abilities.JikiRework.PunkCrossRework;
import net.kazi.kazimod.abilities.KachiRework.CruelSunAbility;
import net.kazi.kazimod.abilities.KachiRework.SunshineAbility;
import net.kazi.kazimod.abilities.KageRework.DoppelmanRework;
import net.kazi.kazimod.abilities.Kake.CasinoRollAbility;
import net.kazi.kazimod.abilities.Kake.LuckySlotAbility;
import net.kazi.kazimod.abilities.Kake.SlotSpinAbility;
import net.kazi.kazimod.abilities.KamaRework.*;
import net.kazi.kazimod.abilities.AkumaRework.*;
import net.kazi.kazimod.abilities.NagiRework.*;
import net.kazi.kazimod.abilities.KendoStyle.SeveranceAbility;
import net.kazi.kazimod.abilities.KendoStyle.ZanshiAbility;
import net.kazi.kazimod.abilities.SakuRework.*;
import net.kazi.kazimod.abilities.KaruRework.ExplodingKarmaAbility;
import net.kazi.kazimod.abilities.KaruRework.IngaZarashiRework;
import net.kazi.kazimod.abilities.KaruRework.RageRushAbility;
import net.kazi.kazimod.abilities.KiraRework.DiamondAwaken;
import net.kazi.kazimod.abilities.KirinRework.DreamwavePulseAbility;
import net.kazi.kazimod.abilities.KirinRework.KirinHeavyPointRework;
import net.kazi.kazimod.abilities.KirinRework.SlumberFieldAbility;
import net.kazi.kazimod.abilities.KitsuneRework.FoxAssaultRework;
import net.kazi.kazimod.abilities.KitsuneRework.FoxfireBallRework;
import net.kazi.kazimod.abilities.KitsuneRework.FoxfireExplosionRework;
import net.kazi.kazimod.abilities.KitsuneRework.InuKitsuneWalkPointRework;
import net.kazi.kazimod.abilities.KobuRework.ShoureiRework;
import net.kazi.kazimod.abilities.Koku.*;
import net.kazi.kazimod.abilities.MinkRework.ElectricalBurstRework;
import net.kazi.kazimod.abilities.MinkRework.ElectricalMissileRework;
import net.kazi.kazimod.abilities.MinkRework.ElectricalShowerRework;
import net.kazi.kazimod.abilities.MinkRework.ElectricalTempestaRework;
import net.kazi.kazimod.abilities.NikyuRework.PadHoRework;
import net.kazi.kazimod.abilities.NikyuRework.TsuppariPadHoRework;
import net.kazi.kazimod.abilities.NikyuRework.UrsusShockRework;
import net.kazi.kazimod.abilities.NitoryuRework.NitoryuIaiRashomonRework;
import net.kazi.kazimod.abilities.NitoryuRework.SaiKuruRework;
import net.kazi.kazimod.abilities.NitoryuRework.TakaNamiRework;
import net.kazi.kazimod.abilities.NoroRework.NoroNoroBeamRework;
import net.kazi.kazimod.abilities.NoroRework.NoroNoroBeamSwordRework;
import net.kazi.kazimod.abilities.Nusu.SkillBookCreationAbility;
import net.kazi.kazimod.abilities.Nusu.SkillHunterAbility;
import net.kazi.kazimod.abilities.Nusu.SkillRemoverAbility;
import net.kazi.kazimod.abilities.OpeRework.*;
import net.kazi.kazimod.abilities.PikaRework.MaxAccelerationRework;
import net.kazi.kazimod.abilities.RyusokenRework.DragonWhirlwindAbility;
import net.kazi.kazimod.abilities.RyusokenRework.RyuNoIbukiRework;
import net.kazi.kazimod.abilities.RyusokenRework.RyuNoKagizumeRework;
import net.kazi.kazimod.abilities.RyusokenRework.TalonRushRework;
import net.kazi.kazimod.abilities.SoruRework.SoulRecoveryRework;
import net.kazi.kazimod.abilities.SpearRework.AbsolutePierceRework;
import net.kazi.kazimod.abilities.SpearRework.DrillJabRework;
import net.kazi.kazimod.abilities.SpearRework.SkySplitterDescentRework;
import net.kazi.kazimod.abilities.SpearRework.VaultRework;
import net.kazi.kazimod.abilities.Tenki.*;
import net.kazi.kazimod.abilities.Toki.*;
import net.kazi.kazimod.abilities.ToriNueRework.FlameBlessingRework;
import net.kazi.kazimod.abilities.ToriNueRework.ImperialFlameRIngCommandmentRework;
import net.kazi.kazimod.abilities.Toshi.*;
import net.kazi.kazimod.abilities.UoSeiryuRework.SeiryuHeavyPointRework;
import net.kazi.kazimod.abilities.YamiRework.BlackHoleRework;
import net.kazi.kazimod.abilities.boss.gojo.BossHollowPurpleAbility;
import net.kazi.kazimod.abilities.boss.gojo.BossLapseBlueAbility;
import net.kazi.kazimod.abilities.boss.gojo.BossMaxOutputLapseBlueAbility;
import net.kazi.kazimod.abilities.boss.gojo.BossRedAbility;
import net.kazi.kazimod.abilities.boss.sukuna.BossDismantleAbility;
import net.kazi.kazimod.abilities.boss.sukuna.BossFugaAbility;
import net.kazi.kazimod.abilities.boss.sukuna.BossMalevolentShrineAbility;
import net.kazi.kazimod.abilities.onirework.SkullBasherRework;
import net.kazi.kazimod.abilities.onirework.ViciousRoarRework;
import net.kazi.kazimod.abilities.swordsmanrework.*;
import net.kazi.kazimod.api.KaziRegistry;
import net.minecraftforge.eventbus.api.IEventBus;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.enums.AbilityCommandGroup;

import java.util.Arrays;

public class KaziAbilities {

    public static final AbilityCore<?>[] KAZIABILITY = new AbilityCore[]{
            HiryuKaenRework.INSTANCE,
            SanbyakurokujoPoundHoRework.INSTANCE,
            YakkodoriRework.INSTANCE,
            SkullBasherRework.INSTANCE,
            NitoryuIaiRashomonRework.INSTANCE,
            GoldenHourRework.INSTANCE,
            MountainEaterRework.INSTANCE,
            RadiantSliceAbility.INSTANCE,
            ViciousRoarRework.INSTANCE,
            TakaNamiRework.INSTANCE,
            SangoRework.INSTANCE,
            FlameBlessingRework.INSTANCE,
            ImperialFlameRIngCommandmentRework.INSTANCE,
            SeiryuHeavyPointRework.INSTANCE,
            SaiKuruRework.INSTANCE,
            ElThorRework.INSTANCE,
            FoxfireExplosionRework.INSTANCE,
            FoxAssaultRework.INSTANCE,
            SkySplitterRework.INSTANCE,
            ReversalRework.INSTANCE,
            UrsusShockRework.INSTANCE,
            KarakuniRework.INSTANCE,
            KirinHeavyPointRework.INSTANCE,
            DreamwavePulseAbility.INSTANCE,
            SlumberFieldAbility.INSTANCE,
            FoxfireBallRework.INSTANCE,
            IngaZarashiRework.INSTANCE,
            RageRushAbility.INSTANCE,
            ExplodingKarmaAbility.INSTANCE,
            PartyTableKickCourseRework.INSTANCE,
            TyrantCleaveRework.INSTANCE,
            BerserkRework.INSTANCE,
            FutenrakuRework.INSTANCE,
            PredatorsThrowRework.INSTANCE,
            YasotakeruRework.INSTANCE,
            BlackHoleRework.INSTANCE,
            GodThreadRework.INSTANCE,
            GomuGomuNoRedRocAbility.INSTANCE,
            GomuGomuNoBazookaRework.INSTANCE,
            GomuGomuNoGatlingRework.INSTANCE,
            GomuGomuNoRocketRework.INSTANCE,
            GomuGomuNoPistolRework.INSTANCE,
            DismantleAbility.INSTANCE,
            NoroNoroBeamRework.INSTANCE,
            NoroNoroBeamSwordRework.INSTANCE,
            ChiyupopoRework.INSTANCE,
            SpringSnipeRework.INSTANCE,
            DekaDekaRework.INSTANCE,
            GearFifthRework.INSTANCE,
            GomuGomuNoGigantRework.INSTANCE,
            GomuGomuNoDawnWhipRework.INSTANCE,
            GomuGomuNoKaminariAbility.INSTANCE,
            OTatsumakiRework.INSTANCE,
            ShiShishiSonsonRework.INSTANCE,
            InuKitsuneWalkPointRework.INSTANCE,
            RyuNoKagizumeRework.INSTANCE,
            RyuNoIbukiRework.INSTANCE,
            TalonRushRework.INSTANCE,
            DragonWhirlwindAbility.INSTANCE,
            VenomRoadRework.INSTANCE,
            PadHoRework.INSTANCE,
            TsuppariPadHoRework.INSTANCE,
            ExplosivePunchRework.INSTANCE,
            KickBombRework.INSTANCE,
            GastilleRework.INSTANCE,
            CleaveAbility.INSTANCE,
            SpiderwebCleaveAbility.INSTANCE,
            FistsOfLoveBarrageRework.INSTANCE,
            GalaxyImpactRework.INSTANCE,
            DomainExpansionMalevolentShrine.INSTANCE,
            FugaAbility.INSTANCE,
            FoxfireStyleAbility.INSTANCE,
            PropellingBlastsAbility.INSTANCE,
            ClusterBombAbility.INSTANCE,
            ExplosiveHoldAbility.INSTANCE,
            StunGrenadeAbility.INSTANCE,
            // In KaziAbilities register() method, add alongside the other abilities:
            AntidoteShotRework.INSTANCE,
            DopingRework.INSTANCE,
            FailedExperimentRework.INSTANCE,
            FirstAidRework.INSTANCE,
            MedicBagExplosionRework.INSTANCE,
            VirusZoneRework.INSTANCE,
            DeAgedAbility.INSTANCE,
            FutureOfFreedomAbility.INSTANCE,
            AgeAccelerationAbility.INSTANCE,
            GiantFutureAbility.INSTANCE,
            GiantPunchAbility.INSTANCE,
            MiniHollowRework.INSTANCE,
            NegativeHollowRework.INSTANCE,
            TokuHollowRework.INSTANCE,
            ElectricalMissileRework.INSTANCE,
            ElectricalShowerRework.INSTANCE,
            ElectricalBurstRework.INSTANCE,
            ElectricalTempestaRework.INSTANCE,
            AbsolutePierceRework.INSTANCE,
            DrillJabRework.INSTANCE,
            SkySplitterDescentRework.INSTANCE,
            VaultRework.INSTANCE,
            SoulRecoveryRework.INSTANCE,
            TimeTheftAbility.INSTANCE,
            TimeReversalAbility.INSTANCE,
            TimeAccelerationAbility.INSTANCE,
            ChronostasisAbility.INSTANCE,
            ChronostasisGrigoraAbility.INSTANCE,
            TimeBarAbility.INSTANCE,
            ShoureiRework.INSTANCE,
            WindGustAbility.INSTANCE,
            TornadoWrathAbility.INSTANCE,
            LightningJabAbility.INSTANCE,
            CloudyDayAbility.INSTANCE,
            ThunderstormAbility.INSTANCE,
            GaleStormAbility.INSTANCE,
            DamnedPunkRework.INSTANCE,
            GenocideRaidRework.INSTANCE,
            PunkCrossRework.INSTANCE,
            RedAbility.INSTANCE,
            LapseBlueAbility.INSTANCE,
            MaxOutputLapseBlueAbility.INSTANCE,
            HollowPurpleAbility.INSTANCE,
            InfinityAbility.INSTANCE,
            DomainExpansionInfiniteVoidAbility.INSTANCE,
            SpinningBrawlRework.INSTANCE,
            SuplexRework.INSTANCE,
            SunshineAbility.INSTANCE,
            CruelSunAbility.INSTANCE,
            HitoDaibutsuPointRework.INSTANCE,
            LuckySlotAbility.INSTANCE,
            SlotSpinAbility.INSTANCE,
            CasinoRollAbility.INSTANCE,
            SkillHunterAbility.INSTANCE,
            SkillRemoverAbility.INSTANCE,
            BossDismantleAbility.INSTANCE,
            BossFugaAbility.INSTANCE,
            BossMalevolentShrineAbility.INSTANCE,
            BossHollowPurpleAbility.INSTANCE,
            BossLapseBlueAbility.INSTANCE,
            BossMaxOutputLapseBlueAbility.INSTANCE,
            BossRedAbility.INSTANCE,
            SkillBookCreationAbility.INSTANCE,
            GearSecondRework.INSTANCE,
            DoppelmanRework.INSTANCE,
            BarrierGuardAbility.INSTANCE,
            DiamondAwaken.INSTANCE,
            KRoomAnesthesiaRework.INSTANCE,
            TaktRework.INSTANCE,
            ShockWilleRework.INSTANCE,
            PunctureWilleRework.INSTANCE,
            ShamblesRework.INSTANCE,
            MaxAccelerationRework.INSTANCE,
            RadioKnifeRework.INSTANCE,
            KamieRework.INSTANCE,
            SoruRework.INSTANCE,
            IceAgeRework.INSTANCE,
            JinshinHoRework.INSTANCE,
            AntiMatterKickCourseRework.INSTANCE,
            KuchuKirimomiDaiCircusRework.INSTANCE,
            BusoshokuHakiFullBodyHardeningRework.INSTANCE,
            KenbunshokuHakiFutureSightRework.INSTANCE,
            HakiSenseAbility.INSTANCE,
            SeveranceAbility.INSTANCE,
            ZanshiAbility.INSTANCE,
            SenbonzakuraAbility.INSTANCE,
            SenbonzakuraKageyoshiAbility.INSTANCE,
            GokeiAbility.INSTANCE,
            SenkeiAbility.INSTANCE,
            ShukeiHakuteikenAbility.INSTANCE,
            FullCounterAbility.INSTANCE,
            RevengePassiveAbility.INSTANCE,
            RevengeCounterAbility.INSTANCE,
            HellblazeAbility.INSTANCE,
            KamiChigiriAbility.INSTANCE,
            DivineSlayerAbility.INSTANCE,
            TrillionDarkAbility.INSTANCE,
            DemonTransformationAbility.INSTANCE,
            SilentStrideAbility.INSTANCE,
            SilentSliceAbility.INSTANCE,
            SilentStepAbility.INSTANCE,
            SilentBoxAbility.INSTANCE,
            SilentDeathAbility.INSTANCE











    };

    public static void register(IEventBus eventBus) {
        KaziRegistry.ABILITIES.register(eventBus);


        Arrays.stream(KAZIABILITY).forEach(KaziRegistry::registerAbility);

        AbilityCommandGroup.create("KAZI", () -> KAZIABILITY);
    }
}
