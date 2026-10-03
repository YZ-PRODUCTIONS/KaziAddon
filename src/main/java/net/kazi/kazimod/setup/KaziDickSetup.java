package net.kazi.kazimod.setup;

import net.MrMagicalCart.cartaddon.abilities.chiyuextra.ReworkedChiyupopoAbility;
import net.MrMagicalCart.cartaddon.abilities.deka.ReworkedDekaDekaAbility;
import net.MrMagicalCart.cartaddon.abilities.dokuextra.NewVenomRoadAbility;
import net.MrMagicalCart.cartaddon.abilities.gasuextra.ReworkedGastilleAbility;
import net.MrMagicalCart.cartaddon.abilities.gasuextra.ReworkedKarakuniAbility;
import net.MrMagicalCart.cartaddon.abilities.goroextra.*;
import net.MrMagicalCart.cartaddon.abilities.fuwa.ItemKaitenAbility;
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
import net.MrMagicalCart.cartaddon.abilities.mochi2.ZanGiriMochiAbility;
import net.MrMagicalCart.cartaddon.abilities.mochi2.MochiGinchakuNewAbility;
import net.MrMagicalCart.cartaddon.abilities.mochi2.KuriMochiNewAbility;
import net.MrMagicalCart.cartaddon.abilities.nikyuextra.ReworkedUrsusShockAbility;
import net.MrMagicalCart.cartaddon.abilities.nikyuextra.ReworkedPainRepelAbility;
import net.MrMagicalCart.cartaddon.abilities.opeextra.*;
import net.MrMagicalCart.cartaddon.abilities.pikaextra.MaxAccelerationAbility;
import net.MrMagicalCart.cartaddon.abilities.ryukirin.KirinHeavyPointAbility;
import net.MrMagicalCart.cartaddon.abilities.ryukirin.KnockoutBeamAbility;
import net.MrMagicalCart.cartaddon.abilities.soru.SoulRecoveryAbility;
import net.MrMagicalCart.cartaddon.abilities.susu.*;
import net.MrMagicalCart.cartaddon.abilities.torinue.FlameBlessingAbility;
import net.MrMagicalCart.cartaddon.abilities.torinue.ImperialFlameRingCommandmentAbility;
import net.MrMagicalCart.cartaddon.abilities.uoseriyu.SeiryuHeavyPointAbility;
import net.MrMagicalCart.cartaddon.abilities.uoseriyu.BoloBreathAbility;
import net.MrMagicalCart.cartaddon.abilities.uoseriyu.TatsumakiAbility;
import net.MrMagicalCart.cartaddon.init.CartAbilities;
import net.kazi.kazimod.KaziMod;
import net.kazi.kazimod.abilities.NagiRework.*;
import net.kazi.kazimod.abilities.AwaRework.GoldenHourRework;
import net.kazi.kazimod.abilities.BaneRework.SpringSnipeRework;
import net.kazi.kazimod.abilities.BaraRework.KuchuKirimomiDaiCircusRework;
import net.kazi.kazimod.abilities.BariRework.BarrierGuardAbility;
import net.kazi.kazimod.abilities.BomuRework.*;
import net.kazi.kazimod.abilities.BuddhaRework.HitoDaibutsuPointRework;
import net.kazi.kazimod.abilities.BludgeonRework.*;
import net.kazi.kazimod.abilities.ChiyuRework.ChiyupopoRework;
import net.kazi.kazimod.abilities.DekaRework.DekaDekaRework;
import net.kazi.kazimod.abilities.DokuRework.*;
import net.kazi.kazimod.abilities.GasuRework.GastilleRework;
import net.kazi.kazimod.abilities.GasuRework.KarakuniRework;
import net.kazi.kazimod.abilities.GomuRework.*;
import net.kazi.kazimod.abilities.GoroRework.*;
import net.kazi.kazimod.abilities.GoruRework.SeriousnessAbility;
import net.kazi.kazimod.abilities.FuwaRework.ItemKaitenReworked;
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
import net.kazi.kazimod.abilities.KameRework.KameGuardPointRework;
import net.MrMagicalCart.cartaddon.abilities.meraextra.ReworkedFlameRushAbility;
import net.kazi.kazimod.abilities.MeraRework.HibashiraRework;
import net.kazi.kazimod.abilities.MeraRework.DaiEnkaiRework;
import net.kazi.kazimod.abilities.KamaRework.*;
import net.kazi.kazimod.abilities.KaruRework.ExplodingKarmaAbility;
import net.kazi.kazimod.abilities.KaruRework.IngaZarashiRework;
import net.kazi.kazimod.abilities.KaruRework.RageRushAbility;
import net.kazi.kazimod.abilities.KirinRework.DreamwavePulseAbility;
import net.kazi.kazimod.abilities.KirinRework.KirinHeavyPointRework;
import net.kazi.kazimod.abilities.KirinRework.SlumberFieldAbility;
import net.kazi.kazimod.abilities.KiraRework.DiamondAwaken;
import net.kazi.kazimod.abilities.KitsuneRework.FoxAssaultRework;
import net.kazi.kazimod.abilities.KitsuneRework.FoxfireBallRework;
import net.kazi.kazimod.abilities.KitsuneRework.FoxfireExplosionRework;
import net.kazi.kazimod.abilities.KitsuneRework.InuKitsuneWalkPointRework;
import net.kazi.kazimod.abilities.NetsuRework.HellfireBirdAbility;
import net.kazi.kazimod.abilities.NetsuRework.InfernalColumnAbility;
import net.kazi.kazimod.abilities.NetsuRework.PhoenixDiveAbility;
import net.kazi.kazimod.abilities.MochiRework.ZanGiriMochiClone;
import net.kazi.kazimod.abilities.MochiRework.MochiGinchakuClone;
import net.kazi.kazimod.abilities.MochiRework.KuriMochiClone;
import net.kazi.kazimod.abilities.KobuRework.ShoureiRework;
import net.kazi.kazimod.abilities.NikyuRework.PadHoRework;
import net.kazi.kazimod.abilities.NikyuRework.PainRepelRework;
import net.kazi.kazimod.abilities.NikyuRework.TsuppariPadHoRework;
import net.kazi.kazimod.abilities.NikyuRework.UrsusShockRework;
import net.kazi.kazimod.abilities.NoroRework.NoroNoroBeamRework;
import net.kazi.kazimod.abilities.NoroRework.NoroNoroBeamSwordRework;
import net.kazi.kazimod.abilities.OpeRework.*;
import net.kazi.kazimod.abilities.PikaRework.MaxAccelerationRework;
import net.kazi.kazimod.abilities.SoruRework.SoulRecoveryRework;
import net.kazi.kazimod.abilities.SupaRework.RealityMarbleAbility;
import net.kazi.kazimod.abilities.SupaRework.ProjectionAbility;
import net.kazi.kazimod.abilities.SupaRework.EnhancementAbility;
import net.kazi.kazimod.abilities.SupaRework.KanshouBakuyaAbility;
import net.kazi.kazimod.abilities.SusuRework.*;
import net.kazi.kazimod.abilities.ToriNueRework.FlameBlessingRework;
import net.kazi.kazimod.abilities.ToriNueRework.ImperialFlameRIngCommandmentRework;
import net.kazi.kazimod.abilities.ToriPhoenixRework.FlamesOfRegenerationRework;
import net.kazi.kazimod.abilities.ToriPhoenixRework.PhoenixAssaultPointRework;
import net.kazi.kazimod.abilities.ToriPhoenixRework.PhoenixFlyPointRework;
import net.kazi.kazimod.abilities.UoSeiryuRework.SeiryuHeavyPointRework;
import net.kazi.kazimod.abilities.UoSeiryuRework.BoloBreathRework;
import net.kazi.kazimod.abilities.UoSeiryuRework.TatsumakiRework;
import net.kazi.kazimod.abilities.VampAwaken.BloodRiver;
import net.kazi.kazimod.abilities.VampAwaken.Feast;
import net.kazi.kazimod.abilities.VampAwaken.SoulsPassive;
import net.kazi.kazimod.abilities.VampAwaken.VampireAwakeningPassive;
import net.kazi.kazimod.abilities.VampAwaken.AwakenedBloodStepAbility;
import net.kazi.kazimod.abilities.VampAwaken.AwakenedPhantomCloakAbility;
import net.kazi.kazimod.abilities.VampAwaken.AwakenedVampirePassiveAbility;
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
import xyz.pixelatedw.mineminenomi.abilities.goro.*;
import xyz.pixelatedw.mineminenomi.abilities.gasu.GastilleAbility;
import xyz.pixelatedw.mineminenomi.abilities.gasu.KarakuniAbility;
import xyz.pixelatedw.mineminenomi.abilities.PoisonImmunityAbility;
import xyz.pixelatedw.mineminenomi.abilities.doku.*;
import xyz.pixelatedw.mineminenomi.abilities.horo.MiniHollowAbility;
import xyz.pixelatedw.mineminenomi.abilities.horo.NegativeHollowAbility;
import xyz.pixelatedw.mineminenomi.abilities.horo.TokuHollowAbility;
import xyz.pixelatedw.mineminenomi.abilities.jiki.GenocideRaidAbility;
import xyz.pixelatedw.mineminenomi.abilities.jiki.PunkCrossAbility;
import xyz.pixelatedw.mineminenomi.abilities.kage.DoppelmanAbility;
import xyz.pixelatedw.mineminenomi.abilities.kame.KameGuardPointAbility;
import xyz.pixelatedw.mineminenomi.abilities.karu.IngaZarashiAbility;
import xyz.pixelatedw.mineminenomi.abilities.kobu.ShoureiAbility;
import xyz.pixelatedw.mineminenomi.abilities.mera.HeatDashAbility;
import xyz.pixelatedw.mineminenomi.abilities.mera.HibashiraAbility;
import xyz.pixelatedw.mineminenomi.abilities.mera.DaiEnkaiAbility;
import xyz.pixelatedw.mineminenomi.abilities.mera.HidarumaAbility;
import xyz.pixelatedw.mineminenomi.abilities.mera.HikenAbility;
import xyz.pixelatedw.mineminenomi.abilities.nikyu.PadHoAbility;
import xyz.pixelatedw.mineminenomi.abilities.nikyu.PainRepelAbility;
import xyz.pixelatedw.mineminenomi.abilities.nikyu.TsuppariPadHoAbility;
import xyz.pixelatedw.mineminenomi.abilities.noro.NoroNoroBeamAbility;
import xyz.pixelatedw.mineminenomi.abilities.noro.NoroNoroBeamSwordAbility;
import xyz.pixelatedw.mineminenomi.abilities.toriphoenix.FlamesOfRegenerationAbility;
import xyz.pixelatedw.mineminenomi.abilities.toriphoenix.PhoenixAssaultPointAbility;
import xyz.pixelatedw.mineminenomi.abilities.toriphoenix.PhoenixFlyPointAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;
import xyz.pixelatedw.mineminenomi.init.ModValues;

@Mod.EventBusSubscriber(modid = KaziMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class KaziDickSetup {

    private static final Logger LOGGER = LogManager.getLogger("kazimod");

    public static AbilityCore<?>[] getBludgeonTopReworks() {
        return new AbilityCore[]{
                ThunderBaguaRework.INSTANCE,
                KundaliDragonSwarmRework.INSTANCE,
                WhirlingMaceRework.INSTANCE,
                StrikingSwingRework.INSTANCE,
                VajraArrowRework.INSTANCE,
                ConquerorOfThreeWorldsRagnarakuRework.INSTANCE
        };
    }

    public static AbilityCore<?>[] getBludgeonBottomReworks() {
        return new AbilityCore[]{
                DestroyerOfDeathThunderBaguaRework.INSTANCE,
                ShinsokuHakujakuRework.INSTANCE
        };
    }

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
    @OnlyIn(Dist.CLIENT)
    public static void clientSetup(FMLClientSetupEvent event) {
        LOGGER.info("piraterecruits Client Setup Starting");
    }

    private static void setupFruitAbilities() {
        LOGGER.info("Setting up custom fruit abilities");
        if (KaziConfig.INSTANCE.disableFruitChanges.get()) return;

        // ── AWA AWA NO MI ────────────────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.KAME_KAME_NO_MI, KameGuardPointAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(ModAbilities.KAME_KAME_NO_MI, KameGuardPointRework.INSTANCE,
                net.kazi.kazimod.abilities.KameRework.WorldTurtleFormAbility.INSTANCE,
                net.kazi.kazimod.abilities.KameRework.WorldTurtleFlightAbility.INSTANCE,
                net.kazi.kazimod.abilities.KameRework.DivineShieldAbility.INSTANCE,
                net.kazi.kazimod.abilities.KameRework.DestructionAbility.INSTANCE,
                net.kazi.kazimod.abilities.KameRework.SupernovaAbility.INSTANCE,
                net.kazi.kazimod.abilities.KameRework.WorldShakingSpinAbility.INSTANCE);

        FruitAbilityInjector.removeAbilities(ModAbilities.AWA_AWA_NO_MI, GoldenHourAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(ModAbilities.AWA_AWA_NO_MI, GoldenHourRework.INSTANCE);

        // ── GORO GORO NO MI ──────────────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.GORO_GORO_NO_MI,
                ReworkedElThorAbility.INSTANCE,
                ReworkedVariAbility.INSTANCE,
                ReworkedSangoAbility.INSTANCE,
                ReworkedRaigoAbility.INSTANCE,
                VoltageUpAbility.INSTANCE,
                ReworkedVoltAmaruAbility.INSTANCE,
                ReworkedVoltAmaruFlightAbility.INSTANCE,
                ShinzoMassagePassiveAbility.INSTANCE,
                ElThorAbility.INSTANCE,
                VariAbility.INSTANCE,
                KariAbility.INSTANCE,
                SangoAbility.INSTANCE,
                RaigoAbility.INSTANCE,
                VoltAmaruAbility.INSTANCE,
                VoltAmaruFlightAbility.INSTANCE,
                ElThorRework.INSTANCE,
                VariRework.INSTANCE,
                KariRework.INSTANCE,
                SangoRework.INSTANCE,
                RaigoRework.INSTANCE,
                VoltAmaruRework.INSTANCE,
                VoltAmaruFlightRework.INSTANCE);
        FruitAbilityInjector.addAbilities(ModAbilities.GORO_GORO_NO_MI,
                ElThorRework.INSTANCE,
                VariRework.INSTANCE,
                KariRework.INSTANCE,
                SangoRework.INSTANCE,
                RaigoRework.INSTANCE,
                VoltAmaruRework.INSTANCE,
                VoltAmaruFlightRework.INSTANCE);
  
        // ── MOCHI MOCHI NO MI ─────────────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(CartAbilities.MOCHI_MOCHI_NO_MI,
                ZanGiriMochiAbility.INSTANCE,
                KuriMochiNewAbility.INSTANCE,
                MochiGinchakuNewAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(CartAbilities.MOCHI_MOCHI_NO_MI,
                ZanGiriMochiClone.INSTANCE,
                KuriMochiClone.INSTANCE,
                MochiGinchakuClone.INSTANCE);

        // ── TORI TORI NO MI MODEL NUE ────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(CartAbilities.TORI_TORI_NO_MI_MODEL_NUE,
                FlameBlessingAbility.INSTANCE, ImperialFlameRingCommandmentAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(CartAbilities.TORI_TORI_NO_MI_MODEL_NUE,
                FlameBlessingRework.INSTANCE, ImperialFlameRIngCommandmentRework.INSTANCE);

        // ── TORI TORI NO MI MODEL PHOENIX ────────────────────────────────────
        FruitAbilityInjector.replaceAbility(ModAbilities.TORI_TORI_NO_MI_PHOENIX,
                PhoenixFlyPointAbility.INSTANCE, PhoenixFlyPointRework.INSTANCE);
        FruitAbilityInjector.replaceAbility(ModAbilities.TORI_TORI_NO_MI_PHOENIX,
                PhoenixAssaultPointAbility.INSTANCE, PhoenixAssaultPointRework.INSTANCE);
        FruitAbilityInjector.replaceAbility(ModAbilities.TORI_TORI_NO_MI_PHOENIX,
                FlamesOfRegenerationAbility.INSTANCE, FlamesOfRegenerationRework.INSTANCE);

        // ── UO UO NO MI MODEL SEIRYU ─────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(CartAbilities.UO_UO_NO_MI_MODEL_SEIRYU,
                SeiryuHeavyPointAbility.INSTANCE, BoloBreathAbility.INSTANCE, TatsumakiAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(CartAbilities.UO_UO_NO_MI_MODEL_SEIRYU,
                SeiryuHeavyPointRework.INSTANCE, BoloBreathRework.INSTANCE, TatsumakiRework.INSTANCE);

        // ── INU INU NO MI MODEL KYUBI NO KITSUNE ────────────────────────────
        FruitAbilityInjector.removeAbilities(CartAbilities.INU_INU_NO_MI_MODEL_KYUBI_NO_KITSUNE,
                FoxfireExplosionAbility.INSTANCE, FoxAssaultAbility.INSTANCE,
                FoxfireBallAbility.INSTANCE, InuKitsuneWalkPointAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(CartAbilities.INU_INU_NO_MI_MODEL_KYUBI_NO_KITSUNE,
                FoxfireExplosionRework.INSTANCE, FoxAssaultRework.INSTANCE,
                FoxfireBallRework.INSTANCE, InuKitsuneWalkPointRework.INSTANCE);

        // ── NIKYU NIKYU NO MI ────────────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.NIKYU_NIKYU_NO_MI,
                ReworkedUrsusShockAbility.INSTANCE, ReworkedPainRepelAbility.INSTANCE,
                PadHoAbility.INSTANCE, TsuppariPadHoAbility.INSTANCE, PainRepelAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(ModAbilities.NIKYU_NIKYU_NO_MI,
                UrsusShockRework.INSTANCE, PadHoRework.INSTANCE, TsuppariPadHoRework.INSTANCE,
                PainRepelRework.INSTANCE);

        // ── GASU GASU NO MI ──────────────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.GASU_GASU_NO_MI,
                KarakuniAbility.INSTANCE, GastilleAbility.INSTANCE,
                ReworkedKarakuniAbility.INSTANCE, ReworkedGastilleAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(ModAbilities.GASU_GASU_NO_MI,
                KarakuniRework.INSTANCE, GastilleRework.INSTANCE);

        // ── MERA MERA NO MI ──────────────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.MERA_MERA_NO_MI,
                HeatDashAbility.INSTANCE, ReworkedFlameRushAbility.INSTANCE, HibashiraAbility.INSTANCE,
                DaiEnkaiAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(ModAbilities.MERA_MERA_NO_MI,
                ReworkedFlameRushAbility.INSTANCE, HibashiraRework.INSTANCE, DaiEnkaiRework.INSTANCE);

        // ── RYU RYU NO MI MODEL KIRIN ────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(CartAbilities.RYU_RYU_NO_MI_MODEL_KIRIN,
                KirinHeavyPointAbility.INSTANCE, KnockoutBeamAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(CartAbilities.RYU_RYU_NO_MI_MODEL_KIRIN,
                KirinHeavyPointRework.INSTANCE, DreamwavePulseAbility.INSTANCE, SlumberFieldAbility.INSTANCE);

        // ── KARU KARU NO MI ──────────────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.KARU_KARU_NO_MI, IngaZarashiAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(ModAbilities.KARU_KARU_NO_MI,
                IngaZarashiRework.INSTANCE, RageRushAbility.INSTANCE, ExplodingKarmaAbility.INSTANCE);

        // ── FUWA FUWA NO MI ──────────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(CartAbilities.FUWA_FUWA_NO_MI,
                ItemKaitenAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(CartAbilities.FUWA_FUWA_NO_MI,
                ItemKaitenReworked.INSTANCE);

        // ── ITO ITO NO MI ────────────────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.ITO_ITO_NO_MI, GodThreadAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(ModAbilities.ITO_ITO_NO_MI, GodThreadRework.INSTANCE);

        // ── SUNA SUNA NO MI ──────────────────────────────────────────────────
        FruitAbilityInjector.addAbilities(ModAbilities.SUNA_SUNA_NO_MI
                );

        // ── GOMU GOMU NO MI ──────────────────────────────────────────────────
        // Temporarily keep the reworked moves and awakening out of the player fruit.
        // The standard Mine Mine no Mi Gomu move set remains, except for Gear Fifth.
        FruitAbilityInjector.removeAbilities(ModAbilities.GOMU_GOMU_NO_MI,
                GearFifthAbility.INSTANCE);

        // ── SUSU SUSU NO MI ──────────────────────────────────────────────────
        FruitAbilityInjector.removeAbilities(CartAbilities.SUSU_SUSU_NO_MI,
                ObelisusuAbility.INSTANCE, KarasusuAbility.INSTANCE,
                GokuroAbility.INSTANCE, ShokuroAbility.INSTANCE,
                HijonnaKukuuAbility.INSTANCE, RakuroAbility.INSTANCE,
                SusuLogiaAbility.INSTANCE, SusuFlyAbility.INSTANCE,
                SusuImmunityAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(CartAbilities.SUSU_SUSU_NO_MI,
                ObelisusuRework.INSTANCE, KarasusuRework.INSTANCE,
                GokuroRework.INSTANCE, ShokuroRework.INSTANCE,
                HijonnaKukuuRework.INSTANCE, RakuroRework.INSTANCE,
                SusuLogiaRework.INSTANCE, SusuFlyRework.INSTANCE,
                SusuImmunityRework.INSTANCE);


        //  NETSU NETSU NO MI
        FruitAbilityInjector.addAbilities(ModAbilities.NETSU_NETSU_NO_MI,
                HellfireBirdAbility.INSTANCE, InfernalColumnAbility.INSTANCE, PhoenixDiveAbility.INSTANCE);

        // ── BATTO BATTO NO MI MODEL VAMPIRE ─────────────────────────────
        FruitAbilityInjector.addAbilities(CartAbilities.BATTO_BATTO_NO_MI_MODEL_VAMPIRE,
                BloodRiver.INSTANCE, Feast.INSTANCE, SoulsPassive.INSTANCE,
                VampireAwakeningPassive.INSTANCE,
                AwakenedBloodStepAbility.INSTANCE,
                AwakenedPhantomCloakAbility.INSTANCE,
                AwakenedVampirePassiveAbility.INSTANCE);

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
        FruitAbilityInjector.removeAbilities(ModAbilities.DOKU_DOKU_NO_MI,
                ChloroBallAbility.INSTANCE, DokuFuguAbility.INSTANCE, DokuGumoAbility.INSTANCE,
                HydraAbility.INSTANCE, VenomDemonAbility.INSTANCE, VenomRoadAbility.INSTANCE,
                PoisonImmunityAbility.INSTANCE,
                net.MrMagicalCart.cartaddon.abilities.dokuextra.NewChloroBallAbility.INSTANCE,
                net.MrMagicalCart.cartaddon.abilities.dokuextra.NewDokuFuguAbility.INSTANCE,
                net.MrMagicalCart.cartaddon.abilities.dokuextra.NewDokuGumoAbility.INSTANCE,
                net.MrMagicalCart.cartaddon.abilities.dokuextra.NewHydraAbility.INSTANCE,
                net.MrMagicalCart.cartaddon.abilities.dokuextra.NewVenomDemonAbility.INSTANCE,
                net.MrMagicalCart.cartaddon.abilities.dokuextra.ReworkedPoisonImmunityAbility.INSTANCE,
                NewVenomRoadAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(ModAbilities.DOKU_DOKU_NO_MI,
                NewChloroBallRework.INSTANCE, NewDokuFuguRework.INSTANCE, NewDokuGumoRework.INSTANCE,
                NewHydraRework.INSTANCE, NewVenomDemonRework.INSTANCE, NewVenomRoadRework.INSTANCE,
                ReworkedPoisonImmunityRework.INSTANCE);

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
        FruitAbilityInjector.addAbilities(ModAbilities.KIRA_KIRA_NO_MI,
                DiamondAwaken.INSTANCE);

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


        // ── NAGI NAGI NO MI ──────────────────────────────────
        FruitAbilityInjector.addAbilities(ModAbilities.NAGI_NAGI_NO_MI,
                SilentSliceAbility.INSTANCE,
                SilentStepAbility.INSTANCE,
                SilentBoxAbility.INSTANCE,
                SilentDeathAbility.INSTANCE
        );

        // ── GORU GORU NO MI ──────────────────────────────────
        FruitAbilityInjector.addAbilities(CartAbilities.GORU_GORU_NO_MI,
                net.kazi.kazimod.abilities.GoruRework.ShaNaqbaImuruAbility.INSTANCE,
                SeriousnessAbility.INSTANCE,
                net.kazi.kazimod.abilities.GoruRework.EnkiduAbility.INSTANCE,
                net.kazi.kazimod.abilities.GoruRework.GateOfBabylonAbility.INSTANCE,
                net.kazi.kazimod.abilities.GoruRework.EaAbility.INSTANCE);

        // ── SUPA SUPA NO MI ──────────────────────────────────
        FruitAbilityInjector.removeAbilities(ModAbilities.SUPA_SUPA_NO_MI,
                xyz.pixelatedw.mineminenomi.abilities.supa.SpiderAbility.INSTANCE,
                xyz.pixelatedw.mineminenomi.abilities.supa.SparklingDaisyAbility.INSTANCE);
        FruitAbilityInjector.addAbilities(ModAbilities.SUPA_SUPA_NO_MI,
                net.kazi.kazimod.abilities.SupaRework.SpiderRework.INSTANCE,
                net.kazi.kazimod.abilities.SupaRework.SparklingDaisyRework.INSTANCE);
        FruitAbilityInjector.addAbilities(ModAbilities.SUPA_SUPA_NO_MI,
                RealityMarbleAbility.INSTANCE,
                ProjectionAbility.INSTANCE,
                EnhancementAbility.INSTANCE,
                KanshouBakuyaAbility.INSTANCE
        );

        // Rebuild ability-to-fruit map after all injections are done
        AwakeningAbilityLoginFix.invalidateCache();

        // Some addon/version combinations leave null placeholders in Mine Mine
        // no Mi's global fruit list. Its client color registration does not
        // null-check this list and crashes during startup when one is present.
        int fruitCountBeforeCleanup = ModValues.DEVIL_FRUITS.size();
        ModValues.DEVIL_FRUITS.removeIf(java.util.Objects::isNull);
        int removedNullFruits = fruitCountBeforeCleanup - ModValues.DEVIL_FRUITS.size();
        if (removedNullFruits > 0) {
            LOGGER.warn("Removed {} null devil-fruit registry entries before client setup", removedNullFruits);
        }
    }
}
