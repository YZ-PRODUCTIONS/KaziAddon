package net.kazi.kazimod.setup;

import net.MrMagicalCart.cartaddon.abilities.gasuextra.ReworkedKarakuniAbility;
import net.MrMagicalCart.cartaddon.abilities.goroextra.ReworkedElThorAbility;
import net.MrMagicalCart.cartaddon.abilities.goroextra.ReworkedSangoAbility;
import net.MrMagicalCart.cartaddon.abilities.inukitsune.FoxAssaultAbility;
import net.MrMagicalCart.cartaddon.abilities.inukitsune.FoxfireBallAbility;
import net.MrMagicalCart.cartaddon.abilities.inukitsune.FoxfireExplosionAbility;
import net.MrMagicalCart.cartaddon.abilities.nikyuextra.ReworkedUrsusShockAbility;
import net.MrMagicalCart.cartaddon.abilities.ryukirin.KirinHeavyPointAbility;
import net.MrMagicalCart.cartaddon.abilities.ryukirin.KnockoutBeamAbility;
import net.MrMagicalCart.cartaddon.abilities.torinue.FlameBlessingAbility;
import net.MrMagicalCart.cartaddon.abilities.torinue.ImperialFlameRingCommandmentAbility;
import net.MrMagicalCart.cartaddon.abilities.uoseriyu.SeiryuHeavyPointAbility;
import net.MrMagicalCart.cartaddon.init.CartAbilities;
import net.kazi.kazimod.abilities.AwaRework.GoldenHourRework;
import net.kazi.kazimod.abilities.GasuRework.KarakuniRework;
import net.kazi.kazimod.abilities.GoroRework.ElThorRework;
import net.kazi.kazimod.abilities.GoroRework.SangoRework;
import net.kazi.kazimod.abilities.KaruRework.ExplodingKarmaAbility;
import net.kazi.kazimod.abilities.KaruRework.IngaZarashiRework;
import net.kazi.kazimod.abilities.KaruRework.RageRushAbility;
import net.kazi.kazimod.abilities.KirinRework.DreamwavePulseAbility;
import net.kazi.kazimod.abilities.KirinRework.KirinHeavyPointRework;
import net.kazi.kazimod.abilities.KirinRework.SlumberFieldAbility;
import net.kazi.kazimod.abilities.KitsuneRework.FoxAssaultRework;
import net.kazi.kazimod.abilities.KitsuneRework.FoxfireBallRework;
import net.kazi.kazimod.abilities.KitsuneRework.FoxfireExplosionRework;
import net.kazi.kazimod.abilities.NikyuRework.UrsusShockRework;
import net.kazi.kazimod.abilities.ToriNueRework.FlameBlessingRework;
import net.kazi.kazimod.abilities.ToriNueRework.ImperialFlameRIngCommandmentRework;
import net.kazi.kazimod.abilities.UoSeiryuRework.SeiryuHeavyPointRework;
import net.kazi.kazimod.KaziMod;
import net.kazi.kazimod.util.FruitAbilityInjector;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.pixelatedw.mineminenomi.abilities.awa.GoldenHourAbility;
import xyz.pixelatedw.mineminenomi.abilities.karu.IngaZarashiAbility;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;
import xyz.pixelatedw.mineminenomi.items.AkumaNoMiItem;

@Mod.EventBusSubscriber(modid = KaziMod.MODID, bus = Bus.MOD)
public class KaziDickSetup {

    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public static void enqueueIMC(InterModEnqueueEvent event) {
        LOGGER.info("piraterecruits IMC Enqueue");
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onStitch(TextureStitchEvent.Pre event) {
        LOGGER.info("piraterecruits Texture Stitch (Pre)");
    }

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("piraterecruits Common Setup Starting");

        event.enqueueWork(() -> {
            // Initialize fruit ability modifications here
            setupFruitAbilities();
        });
    }

    private static void setupFruitAbilities() {
        LOGGER.info("Setting up custom fruit abilities");

        // Example usage - uncomment and modify as needed:
        // Add abilities to a fruit
        FruitAbilityInjector.addAbilities(
                (AkumaNoMiItem) ModAbilities.AWA_AWA_NO_MI,
                GoldenHourRework.INSTANCE
        );

        FruitAbilityInjector.addAbilities(
                (AkumaNoMiItem) ModAbilities.GORO_GORO_NO_MI,
                SangoRework.INSTANCE,
                ElThorRework.INSTANCE

        );

        FruitAbilityInjector.addAbilities(
                (AkumaNoMiItem) CartAbilities.TORI_TORI_NO_MI_MODEL_NUE,
                FlameBlessingRework.INSTANCE,
                ImperialFlameRIngCommandmentRework.INSTANCE
        );

        FruitAbilityInjector.addAbilities(
                (AkumaNoMiItem) CartAbilities.UO_UO_NO_MI_MODEL_SEIRYU,
                SeiryuHeavyPointRework.INSTANCE
        );

        FruitAbilityInjector.addAbilities(
                (AkumaNoMiItem) CartAbilities.INU_INU_NO_MI_MODEL_KYUBI_NO_KITSUNE,
                FoxfireExplosionRework.INSTANCE,
                FoxAssaultRework.INSTANCE,
                FoxfireBallRework.INSTANCE
        );

        FruitAbilityInjector.addAbilities(
                (AkumaNoMiItem) ModAbilities.NIKYU_NIKYU_NO_MI,
                UrsusShockRework.INSTANCE
        );

        FruitAbilityInjector.addAbilities(
                (AkumaNoMiItem) ModAbilities.GASU_GASU_NO_MI,
                KarakuniRework.INSTANCE
        );

        FruitAbilityInjector.addAbilities(
                (AkumaNoMiItem) CartAbilities.RYU_RYU_NO_MI_MODEL_KIRIN,
                KirinHeavyPointRework.INSTANCE,
                DreamwavePulseAbility.INSTANCE,
                SlumberFieldAbility.INSTANCE
        );

        FruitAbilityInjector.addAbilities(
                (AkumaNoMiItem) ModAbilities.KARU_KARU_NO_MI,
                IngaZarashiRework.INSTANCE,
                RageRushAbility.INSTANCE,
                ExplodingKarmaAbility.INSTANCE
        );



        // Replace all abilities on a fruit
        //FruitAbilityInjector.replaceAbilities(
                //(AkumaNoMiItem) ModAbilities.GORO_GORO_NO_MI,
               // SangoRework.INSTANCE
               // YourCustomAbilities.ABILITY_2
        //);

        // Remove specific abilities from a fruit
        FruitAbilityInjector.removeAbilities(
                (AkumaNoMiItem) ModAbilities.GORO_GORO_NO_MI,
                ReworkedSangoAbility.INSTANCE,
                ReworkedElThorAbility.INSTANCE
        );

        FruitAbilityInjector.removeAbilities(
                (AkumaNoMiItem) ModAbilities.AWA_AWA_NO_MI,
                GoldenHourAbility.INSTANCE
        );

        FruitAbilityInjector.removeAbilities(
                (AkumaNoMiItem) CartAbilities.TORI_TORI_NO_MI_MODEL_NUE,
                FlameBlessingAbility.INSTANCE,
                ImperialFlameRingCommandmentAbility.INSTANCE
        );

        FruitAbilityInjector.removeAbilities(
                (AkumaNoMiItem) CartAbilities.UO_UO_NO_MI_MODEL_SEIRYU,
                SeiryuHeavyPointAbility.INSTANCE
        );

        FruitAbilityInjector.removeAbilities(
                (AkumaNoMiItem) CartAbilities.INU_INU_NO_MI_MODEL_KYUBI_NO_KITSUNE,
                FoxfireExplosionAbility.INSTANCE,
                FoxAssaultAbility.INSTANCE,
                FoxfireBallAbility.INSTANCE

        );

        FruitAbilityInjector.removeAbilities(
                (AkumaNoMiItem) ModAbilities.NIKYU_NIKYU_NO_MI,
                ReworkedUrsusShockAbility.INSTANCE
        );

        FruitAbilityInjector.removeAbilities(
                (AkumaNoMiItem) ModAbilities.GASU_GASU_NO_MI,
                ReworkedKarakuniAbility.INSTANCE
        );

        FruitAbilityInjector.removeAbilities(
                (AkumaNoMiItem) CartAbilities.RYU_RYU_NO_MI_MODEL_KIRIN,
                KirinHeavyPointAbility.INSTANCE,
                KnockoutBeamAbility.INSTANCE
        );

        FruitAbilityInjector.removeAbilities(
                (AkumaNoMiItem) ModAbilities.KARU_KARU_NO_MI,
                IngaZarashiAbility.INSTANCE
        );

    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        LOGGER.info("piraterecruits Client Setup Starting");
        event.enqueueWork(() -> {});
    }
}