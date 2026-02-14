package net.kazi.kazimod.init;



import net.kazi.kazimod.abilities.AwaRework.GoldenHourRework;
import net.kazi.kazimod.abilities.AxeStyleRework.MountainEaterRework;
import net.kazi.kazimod.abilities.MinkRework.SulongRework;
import net.kazi.kazimod.abilities.NitoryuRework.NitoryuIaiRashomonRework;
import net.kazi.kazimod.abilities.onirework.SkullBasherRework;
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
            //SkySplitterRework.INSTANCE,





    };

    public static void register(IEventBus eventBus) {
        KaziRegistry.ABILITIES.register(eventBus);


        Arrays.stream(KAZIABILITY).forEach(KaziRegistry::registerAbility);

        AbilityCommandGroup.create("KAZI", () -> KAZIABILITY);
    }
}