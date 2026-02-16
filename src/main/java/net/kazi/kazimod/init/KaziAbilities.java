package net.kazi.kazimod.init;



import net.kazi.kazimod.abilities.AwaRework.GoldenHourRework;
import net.kazi.kazimod.abilities.AxeStyleRework.MountainEaterRework;
import net.kazi.kazimod.abilities.AxeStyleRework.ReversalRework;
import net.kazi.kazimod.abilities.AxeStyleRework.SkySplitterRework;
import net.kazi.kazimod.abilities.GasuRework.KarakuniRework;
import net.kazi.kazimod.abilities.GoroRework.ElThorRework;
import net.kazi.kazimod.abilities.GoroRework.SangoRework;
import net.kazi.kazimod.abilities.KirinRework.KirinHeavyPointRework;
import net.kazi.kazimod.abilities.KirinRework.DreamwavePulseAbility;
import net.kazi.kazimod.abilities.KirinRework.SlumberFieldAbility;
import net.kazi.kazimod.abilities.KitsuneRework.FoxAssaultRework;
import net.kazi.kazimod.abilities.KitsuneRework.FoxfireBallRework;
import net.kazi.kazimod.abilities.KitsuneRework.FoxfireExplosionRework;
import net.kazi.kazimod.abilities.MinkRework.SulongRework;
import net.kazi.kazimod.abilities.NikyuRework.UrsusShockRework;
import net.kazi.kazimod.abilities.NitoryuRework.NitoryuIaiRashomonRework;
import net.kazi.kazimod.abilities.NitoryuRework.SaiKuruRework;
import net.kazi.kazimod.abilities.NitoryuRework.TakaNamiRework;
import net.kazi.kazimod.abilities.ToriNueRework.FlameBlessingRework;
import net.kazi.kazimod.abilities.ToriNueRework.ImperialFlameRIngCommandmentRework;
import net.kazi.kazimod.abilities.UoSeiryuRework.SeiryuHeavyPointRework;
import net.kazi.kazimod.abilities.onirework.SkullBasherRework;
import net.kazi.kazimod.abilities.onirework.ViciousRoarRework;
import net.kazi.kazimod.abilities.swordsmanrework.HiryuKaenRework;
import net.kazi.kazimod.abilities.swordsmanrework.RadiantSliceAbility;
import net.kazi.kazimod.abilities.swordsmanrework.SanbyakurokujoPoundHoRework;
import net.kazi.kazimod.abilities.swordsmanrework.YakkodoriRework;
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
            SulongRework.INSTANCE,
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
            FoxfireBallRework.INSTANCE






    };

    public static void register(IEventBus eventBus) {
        KaziRegistry.ABILITIES.register(eventBus);


        Arrays.stream(KAZIABILITY).forEach(KaziRegistry::registerAbility);

        AbilityCommandGroup.create("KAZI", () -> KAZIABILITY);
    }
}