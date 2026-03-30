package net.kazi.kazimod.setup;

import net.MrMagicalCart.cartaddon.abilities.chiyuextra.ReworkedChiyupopoAbility;
import net.MrMagicalCart.cartaddon.abilities.deka.ReworkedDekaDekaAbility;
import net.MrMagicalCart.cartaddon.abilities.dokuextra.NewVenomRoadAbility;
import net.MrMagicalCart.cartaddon.abilities.gasuextra.ReworkedGastilleAbility;
import net.MrMagicalCart.cartaddon.abilities.gasuextra.ReworkedKarakuniAbility;
import net.MrMagicalCart.cartaddon.abilities.goroextra.ReworkedElThorAbility;
import net.MrMagicalCart.cartaddon.abilities.goroextra.ReworkedSangoAbility;
import net.MrMagicalCart.cartaddon.abilities.hieextra.IceAge2Ability;
import net.MrMagicalCart.cartaddon.abilities.hitodaibutsuextra.ReworkedHitoDaibutsuPointAbility;
import net.MrMagicalCart.cartaddon.abilities.inukitsune.FoxAssaultAbility;
import net.MrMagicalCart.cartaddon.abilities.inukitsune.FoxfireBallAbility;
import net.MrMagicalCart.cartaddon.abilities.inukitsune.FoxfireExplosionAbility;
import net.MrMagicalCart.cartaddon.abilities.inukitsune.InuKitsuneWalkPointAbility;
import net.MrMagicalCart.cartaddon.abilities.itoextra.GodThreadAbility;
import net.MrMagicalCart.cartaddon.abilities.jikiextra.ReworkedDamnedPunkAbility;
import net.MrMagicalCart.cartaddon.abilities.kageextra.IslandShatteringPunchAbility;
import net.MrMagicalCart.cartaddon.abilities.kama.KamaKamaCrossSlashAbility;
import net.MrMagicalCart.cartaddon.abilities.kama.KamaKamaSliceAbility;
import net.MrMagicalCart.cartaddon.abilities.nikyuextra.ReworkedUrsusShockAbility;
import net.MrMagicalCart.cartaddon.abilities.opeextra.*;
import net.MrMagicalCart.cartaddon.abilities.pikaextra.MaxAccelerationAbility;
import net.MrMagicalCart.cartaddon.abilities.ryukirin.KirinHeavyPointAbility;
import net.MrMagicalCart.cartaddon.abilities.ryukirin.KnockoutBeamAbility;
import net.MrMagicalCart.cartaddon.abilities.soru.SoulRecoveryAbility;
import net.MrMagicalCart.cartaddon.abilities.torinue.FlameBlessingAbility;
import net.MrMagicalCart.cartaddon.abilities.torinue.ImperialFlameRingCommandmentAbility;
import net.MrMagicalCart.cartaddon.abilities.uoseriyu.SeiryuHeavyPointAbility;
import net.MrMagicalCart.cartaddon.abilities.yamiextra.ReworkedBlackHoleAbility;
import net.MrMagicalCart.cartaddon.abilities.yamiextra.YamiAbsorptionPassive;
import net.MrMagicalCart.cartaddon.init.CartAbilities;
import net.kazi.kazimod.KaziMod;
import net.kazi.kazimod.abilities.AwaRework.GoldenHourRework;
import net.kazi.kazimod.abilities.BaneRework.SpringSnipeRework;
import net.kazi.kazimod.abilities.BaraRework.KuchuKirimomiDaiCircusRework;
import net.kazi.kazimod.abilities.BariRework.BarrierGuardAbility;
import net.kazi.kazimod.abilities.BomuRework.*;
import net.kazi.kazimod.abilities.BuddhaRework.HitoDaibutsuPointRework;
import net.kazi.kazimod.abilities.ChiyuRework.ChiyupopoRework;
import net.kazi.kazimod.abilities.DekaRework.DekaDekaRework;
import net.kazi.kazimod.abilities.DokuRework.VenomRoadRework;
import net.kazi.kazimod.abilities.GasuRework.GastilleRework;
import net.kazi.kazimod.abilities.GasuRework.KarakuniRework;
import net.kazi.kazimod.abilities.GomuRework.*;
import net.kazi.kazimod.abilities.GoroRework.ElThorRework;
import net.kazi.kazimod.abilities.GoroRework.SangoRework;
import net.kazi.kazimod.abilities.HieRework.IceAgeRework;
import net.kazi.kazimod.abilities.HoroRework.MiniHollowRework;
import net.kazi.kazimod.abilities.HoroRework.NegativeHollowRework;
import net.kazi.kazimod.abilities.HoroRework.TokuHollowRework;
import net.kazi.kazimod.abilities.ItoRework.GodThreadRework;
import net.kazi.kazimod.abilities.JikiRework.DamnedPunkRework;
import net.kazi.kazimod.abilities.JikiRework.GenocideRaidRework;
import net.kazi.kazimod.abilities.JikiRework.PunkCrossRework;
import net.kazi.kazimod.abilities.KachiRework.CruelSunAbility;
import net.kazi.kazimod.abilities.KachiRework.SunshineAbility;
import net.kazi.kazimod.abilities.KageRework.DoppelmanRework;
import net.kazi.kazimod.abilities.KamaRework.*;
import net.kazi.kazimod.abilities.KaruRework.ExplodingKarmaAbility;
import net.kazi.kazimod.abilities.KaruRework.IngaZarashiRework;
import net.kazi.kazimod.abilities.KaruRework.RageRushAbility;
import net.kazi.kazimod.abilities.KirinRework.DreamwavePulseAbility;
import net.kazi.kazimod.abilities.KirinRework.KirinHeavyPointRework;
import net.kazi.kazimod.abilities.KirinRework.SlumberFieldAbility;
import net.kazi.kazimod.abilities.KitsuneRework.FoxAssaultRework;
import net.kazi.kazimod.abilities.KitsuneRework.FoxfireBallRework;
import net.kazi.kazimod.abilities.KitsuneRework.FoxfireExplosionRework;
import net.kazi.kazimod.abilities.KitsuneRework.InuKitsuneWalkPointRework;
import net.kazi.kazimod.abilities.KobuRework.ShoureiRework;
import net.kazi.kazimod.abilities.NikyuRework.PadHoRework;
import net.kazi.kazimod.abilities.NikyuRework.TsuppariPadHoRework;
import net.kazi.kazimod.abilities.NikyuRework.UrsusShockRework;
import net.kazi.kazimod.abilities.NoroRework.NoroNoroBeamRework;
import net.kazi.kazimod.abilities.NoroRework.NoroNoroBeamSwordRework;
import net.kazi.kazimod.abilities.OpeRework.*;
import net.kazi.kazimod.abilities.PikaRework.MaxAccelerationRework;
import net.kazi.kazimod.abilities.SoruRework.SoulRecoveryRework;
import net.kazi.kazimod.abilities.ToriNueRework.FlameBlessingRework;
import net.kazi.kazimod.abilities.ToriNueRework.ImperialFlameRIngCommandmentRework;
import net.kazi.kazimod.abilities.UoSeiryuRework.SeiryuHeavyPointRework;
import net.kazi.kazimod.abilities.YamiRework.BlackHoleRework;
import net.kazi.kazimod.config.KaziConfig;
import net.kazi.kazimod.events.AwakeningAbilityLoginFix;
import net.kazi.kazimod.util.FruitAbilityInjector;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.pixelatedw.mineminenomi.abilities.awa.GoldenHourAbility;
import xyz.pixelatedw.mineminenomi.abilities.bane.SpringSnipeAbility;
import xyz.pixelatedw.mineminenomi.abilities.bara.KuchuKirimomiDaiCircusAbility;
import xyz.pixelatedw.mineminenomi.abilities.beta.BetaLauncherAbility;
import xyz.pixelatedw.mineminenomi.abilities.bomu.ExplosivePunchAbility;
import xyz.pixelatedw.mineminenomi.abilities.bomu.KickBombAbility;
import xyz.pixelatedw.mineminenomi.abilities.gomu.*;
import xyz.pixelatedw.mineminenomi.abilities.horo.MiniHollowAbility;
import xyz.pixelatedw.mineminenomi.abilities.horo.NegativeHollowAbility;
import xyz.pixelatedw.mineminenomi.abilities.horo.TokuHollowAbility;
import xyz.pixelatedw.mineminenomi.abilities.jiki.GenocideRaidAbility;
import xyz.pixelatedw.mineminenomi.abilities.jiki.PunkCrossAbility;
import xyz.pixelatedw.mineminenomi.abilities.kage.DoppelmanAbility;
import xyz.pixelatedw.mineminenomi.abilities.karu.IngaZarashiAbility;
import xyz.pixelatedw.mineminenomi.abilities.kobu.ShoureiAbility;
import xyz.pixelatedw.mineminenomi.abilities.nikyu.PadHoAbility;
import xyz.pixelatedw.mineminenomi.abilities.nikyu.TsuppariPadHoAbility;
import xyz.pixelatedw.mineminenomi.abilities.noro.NoroNoroBeamAbility;
import xyz.pixelatedw.mineminenomi.abilities.noro.NoroNoroBeamSwordAbility;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;

@Mod.EventBusSubscriber(modid = KaziMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class KaziDickSetup {

    private static final Logger LOGGER = LogManager.getLogger("kazimod");

    @SubscribeEvent
    public static void enqueueIMC(InterModEnqueueEvent event) {
        LOGGER.info("piraterecruits IMC Enqueue");
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onStitch(TextureStitchEvent.Pre event) {
        LOGGER.info("piraterecruits Texture Stitch (Pre)");
    }

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("piraterecruits Common Setup Starting");
        event.enqueueWork(KaziDickSetup::setupFruitAbilities);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        LOGGER.info("piraterecruits Client Setup Starting");
    }

    private static void setupFruitAbilities() {
        LOGGER.info("Setting up custom fruit abilities");
        if (KaziConfig.INSTANCE.disableFruitChanges.get()) return;

        // ── AWA AWA NO MI ────────────────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.AWA_AWA_NO_MI, GoldenHourAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(ModAbilities.AWA_AWA_NO_MI, GoldenHourRework.INSTANCE);

        // ── GORO GORO NO MI ──────────────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.GORO_GORO_NO_MI,
                ReworkedSangoAbility.INSTANCE, ReworkedElThorAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(ModAbilities.GORO_GORO_NO_MI,
                SangoRework.INSTANCE, ElThorRework.INSTANCE);

        // ── TORI TORI NO MI MODEL NUE ────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(CartAbilities.TORI_TORI_NO_MI_MODEL_NUE,
                FlameBlessingAbility.INSTANCE, ImperialFlameRingCommandmentAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(CartAbilities.TORI_TORI_NO_MI_MODEL_NUE,
                FlameBlessingRework.INSTANCE, ImperialFlameRIngCommandmentRework.INSTANCE);

        // ── UO UO NO MI MODEL SEIRYU ─────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(CartAbilities.UO_UO_NO_MI_MODEL_SEIRYU,
                SeiryuHeavyPointAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(CartAbilities.UO_UO_NO_MI_MODEL_SEIRYU,
                SeiryuHeavyPointRework.INSTANCE);

        // ── INU INU NO MI MODEL KYUBI NO KITSUNE ────────────────────────────
        FruitAbilityInjector.removeAbilities(CartAbilities.INU_INU_NO_MI_MODEL_KYUBI_NO_KITSUNE,
                FoxfireExplosionAbility.INSTANCE, FoxAssaultAbility.INSTANCE,
                FoxfireBallAbility.INSTANCE, InuKitsuneWalkPointAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(CartAbilities.INU_INU_NO_MI_MODEL_KYUBI_NO_KITSUNE,
                FoxfireExplosionRework.INSTANCE, FoxAssaultRework.INSTANCE,
                FoxfireBallRework.INSTANCE, InuKitsuneWalkPointRework.INSTANCE);

        // ── NIKYU NIKYU NO MI ────────────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.NIKYU_NIKYU_NO_MI,
                ReworkedUrsusShockAbility.INSTANCE, PadHoAbility.INSTANCE, TsuppariPadHoAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(ModAbilities.NIKYU_NIKYU_NO_MI,
                UrsusShockRework.INSTANCE, PadHoRework.INSTANCE, TsuppariPadHoRework.INSTANCE);

        // ── GASU GASU NO MI ──────────────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.GASU_GASU_NO_MI,
                ReworkedKarakuniAbility.INSTANCE, ReworkedGastilleAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(ModAbilities.GASU_GASU_NO_MI,
                KarakuniRework.INSTANCE, GastilleRework.INSTANCE);

        // ── RYU RYU NO MI MODEL KIRIN ────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(CartAbilities.RYU_RYU_NO_MI_MODEL_KIRIN,
                KirinHeavyPointAbility.INSTANCE, KnockoutBeamAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(CartAbilities.RYU_RYU_NO_MI_MODEL_KIRIN,
                KirinHeavyPointRework.INSTANCE, DreamwavePulseAbility.INSTANCE, SlumberFieldAbility.INSTANCE);

        // ── KARU KARU NO MI ──────────────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.KARU_KARU_NO_MI, IngaZarashiAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(ModAbilities.KARU_KARU_NO_MI,
                IngaZarashiRework.INSTANCE, RageRushAbility.INSTANCE, ExplodingKarmaAbility.INSTANCE);

        // ── YAMI YAMI NO MI ──────────────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.YAMI_YAMI_NO_MI,
                ReworkedBlackHoleAbility.INSTANCE, YamiAbsorptionPassive.INSTANCE);
        FruitAbilityInjector.addAbilities(ModAbilities.YAMI_YAMI_NO_MI, BlackHoleRework.INSTANCE);

        // ── ITO ITO NO MI ────────────────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.ITO_ITO_NO_MI, GodThreadAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(ModAbilities.ITO_ITO_NO_MI, GodThreadRework.INSTANCE);

        // ── SUNA SUNA NO MI ──────────────────────────────────────────────────
        FruitAbilityInjector.addAbilities(ModAbilities.SUNA_SUNA_NO_MI
                );

        // ── GOMU GOMU NO MI ──────────────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.GOMU_GOMU_NO_MI,
                GomuGomuNoBazookaAbility.INSTANCE, GomuGomuNoPistolAbility.INSTANCE,
                GomuGomuNoGatlingAbility.INSTANCE, GomuGomuNoRocketAbility.INSTANCE,
                GearFifthAbility.INSTANCE, GomuGomuNoGigantAbility.INSTANCE,
                GomuGomuNoDawnWhipAbility.INSTANCE, GearSecondAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(ModAbilities.GOMU_GOMU_NO_MI,
                GomuGomuNoBazookaRework.INSTANCE, GomuGomuNoPistolRework.INSTANCE,
                GomuGomuNoGatlingRework.INSTANCE, GomuGomuNoRocketRework.INSTANCE,
                GomuGomuNoRedRocAbility.INSTANCE, GearFifthRework.INSTANCE,
                GomuGomuNoGigantRework.INSTANCE, GomuGomuNoDawnWhipRework.INSTANCE,
                GomuGomuNoKaminariAbility.INSTANCE, GearSecondRework.INSTANCE);

        // ── KAMA KAMA NO MI ──────────────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(CartAbilities.KAMA_KAMA_NO_MI,
                KamaKamaCrossSlashAbility.INSTANCE, KamaKamaSliceAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(CartAbilities.KAMA_KAMA_NO_MI,
                DismantleAbility.INSTANCE, CleaveAbility.INSTANCE, SpiderwebCleaveAbility.INSTANCE,
                DomainExpansionMalevolentShrine.INSTANCE, FugaAbility.INSTANCE);

        // ── NORO NORO NO MI ──────────────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.NORO_NORO_NO_MI,
                NoroNoroBeamAbility.INSTANCE, NoroNoroBeamSwordAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(ModAbilities.NORO_NORO_NO_MI,
                NoroNoroBeamRework.INSTANCE, NoroNoroBeamSwordRework.INSTANCE);

        // ── CHIYU CHIYU NO MI ────────────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.CHIYU_CHIYU_NO_MI,
                ReworkedChiyupopoAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(ModAbilities.CHIYU_CHIYU_NO_MI, ChiyupopoRework.INSTANCE);

        // ── BANE BANE NO MI ──────────────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.BANE_BANE_NO_MI, SpringSnipeAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(ModAbilities.BANE_BANE_NO_MI, SpringSnipeRework.INSTANCE);

        // ── DEKA DEKA NO MI ──────────────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.DEKA_DEKA_NO_MI, ReworkedDekaDekaAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(ModAbilities.DEKA_DEKA_NO_MI, DekaDekaRework.INSTANCE);

        // ── DOKU DOKU NO MI ──────────────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.DOKU_DOKU_NO_MI, NewVenomRoadAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(ModAbilities.DOKU_DOKU_NO_MI, VenomRoadRework.INSTANCE);

        // ── BOMU BOMU NO MI ──────────────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.BOMU_BOMU_NO_MI,
                ExplosivePunchAbility.INSTANCE, KickBombAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(ModAbilities.BOMU_BOMU_NO_MI,
                ExplosivePunchRework.INSTANCE, KickBombRework.INSTANCE,
                PropellingBlastsAbility.INSTANCE, ClusterBombAbility.INSTANCE,
                ExplosiveHoldAbility.INSTANCE, StunGrenadeAbility.INSTANCE);

        // ── HORO HORO NO MI ──────────────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.HORO_HORO_NO_MI,
                MiniHollowAbility.INSTANCE, NegativeHollowAbility.INSTANCE, TokuHollowAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(ModAbilities.HORO_HORO_NO_MI,
                MiniHollowRework.INSTANCE, NegativeHollowRework.INSTANCE, TokuHollowRework.INSTANCE);

        // ── BETA BETA NO MI ──────────────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.BETA_BETA_NO_MI, BetaLauncherAbility.INSTANCE);

        // ── SORU SORU NO MI ──────────────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(CartAbilities.SORU_SORU_NO_MI, SoulRecoveryAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(CartAbilities.SORU_SORU_NO_MI, SoulRecoveryRework.INSTANCE);

        // ── KOBU KOBU NO MI ──────────────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.KOBU_KOBU_NO_MI, ShoureiAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(ModAbilities.KOBU_KOBU_NO_MI, ShoureiRework.INSTANCE);

        // ── JIKI JIKI NO MI ──────────────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.JIKI_JIKI_NO_MI,
                ReworkedDamnedPunkAbility.INSTANCE, GenocideRaidAbility.INSTANCE, PunkCrossAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(ModAbilities.JIKI_JIKI_NO_MI,
                DamnedPunkRework.INSTANCE, GenocideRaidRework.INSTANCE, PunkCrossRework.INSTANCE);

        // ── KACHI KACHI NO MI ────────────────────────────────────────────────
        FruitAbilityInjector.addAbilities(ModAbilities.KACHI_KACHI_NO_MI,
                SunshineAbility.INSTANCE, CruelSunAbility.INSTANCE);

        // ── HITO HITO NO MI MODEL DAIBUTSU ───────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.HITO_HITO_NO_MI_DAIBUTSU,
                ReworkedHitoDaibutsuPointAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(ModAbilities.HITO_HITO_NO_MI_DAIBUTSU,
                HitoDaibutsuPointRework.INSTANCE);

        // ── KAGE KAGE NO MI ───────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.KAGE_KAGE_NO_MI,
                DoppelmanAbility.INSTANCE, IslandShatteringPunchAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(ModAbilities.KAGE_KAGE_NO_MI,
                DoppelmanRework.INSTANCE);

        // ── BARI BARI NO MI ───────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.BARI_BARI_NO_MI
                );
        FruitAbilityInjector.addAbilities(ModAbilities.BARI_BARI_NO_MI,
                BarrierGuardAbility.INSTANCE);

        // ── KIRA KIRA NO MI ───────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.KIRA_KIRA_NO_MI
        );
        FruitAbilityInjector.addAbilities(ModAbilities.KIRA_KIRA_NO_MI
                );

        // ── OPE OPE NO MI ───────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.OPE_OPE_NO_MI,
                KRoomAnesthesiaAbility.INSTANCE,
                ReworkedTaktAbility.INSTANCE,
                TaktEmergenceAbility.INSTANCE,
                TaktTossAbility.INSTANCE,
                PunctureWilleAbility.INSTANCE,
                ShockWilleAbility.INSTANCE,
                ReworkedShamblesAbility.INSTANCE,
                RadioKnifeAbility.INSTANCE
        );
        FruitAbilityInjector.addAbilities(ModAbilities.OPE_OPE_NO_MI,
                KRoomAnesthesiaRework.INSTANCE,
                TaktRework.INSTANCE,
                ShockWilleRework.INSTANCE,
                PunctureWilleRework.INSTANCE,
                ShamblesRework.INSTANCE,
                RadioKnifeRework.INSTANCE
        );

        // ── PIKA PIKA NO MI ───────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.PIKA_PIKA_NO_MI,
                MaxAccelerationAbility.INSTANCE
        );
        FruitAbilityInjector.addAbilities(ModAbilities.PIKA_PIKA_NO_MI,
                MaxAccelerationRework.INSTANCE
        );

        // ── HIE HIE NO MI ───────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.HIE_HIE_NO_MI,
                IceAge2Ability.INSTANCE
        );
        FruitAbilityInjector.addAbilities(ModAbilities.HIE_HIE_NO_MI,
                IceAgeRework.INSTANCE
        );

        // ── BARA BARA NO MI ───────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.BARA_BARA_NO_MI,
                KuchuKirimomiDaiCircusAbility.INSTANCE
        );
        FruitAbilityInjector.addAbilities(ModAbilities.BARA_BARA_NO_MI,
                KuchuKirimomiDaiCircusRework.INSTANCE
        );


        // Rebuild ability-to-fruit map after all injections are done
        AwakeningAbilityLoginFix.invalidateCache();
    }
}