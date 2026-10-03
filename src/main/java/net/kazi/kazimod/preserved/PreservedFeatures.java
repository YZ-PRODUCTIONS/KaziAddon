package net.kazi.kazimod.preserved;

import net.kazi.kazimod.abilities.TripelT.*;
import net.kazi.kazimod.api.KaziRegistry;
import net.kazi.kazimod.init.KaziMorphs;
import net.kazi.kazimod.init.KaziEntities;
import net.kazi.kazimod.entities.boss.rebelus.TrueFormRebelusBossEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.enums.AbilityCommandGroup;

/** Registers the imported Gura, Cerberus, Triple T and Tenki features. */
public final class PreservedFeatures {
    public static void init() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        net.kazi.kazimod.preserved.cerberus.CerberusFeatures.init(bus);
        TripelTFormWeapon.registerEvents();
        net.kazi.kazimod.preserved.sahur.SahurEffects.init(bus);
        net.kazi.kazimod.preserved.tenki.TenkiEffects.init(bus);
        net.kazi.kazimod.worldturtle.WorldTurtleEffects.init(bus);
        net.kazi.kazimod.kake.KakeVisuals.init(bus);
        AbilityCore<?>[] abilities = {TripelTFormAbility.INSTANCE, TripelTGodFormAbility.INSTANCE,
                TripelTArmoryAbility.INSTANCE, TripelTFlightAbility.INSTANCE, HomeRunSwingAbility.INSTANCE,
                TungTungTungBarrageAbility.INSTANCE, SahurYellAbility.INSTANCE, SwingingCounterAbility.INSTANCE,
                ArrowsOfLightAbility.INSTANCE};
        // Keep the old registry entry readable for save migration, but do not grant it.
        KaziRegistry.registerAbility(JudgementationAbility.INSTANCE);
        for (AbilityCore<?> ability : abilities) KaziRegistry.registerAbility(ability);
        AbilityCommandGroup.create("TRIPLE_T", () -> abilities);
        KaziMorphs.register(bus);
        KaziRegistry.MORPHS.register(bus);
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event) ->
                event.put(KaziEntities.TRUE_FORM_REBELUS_BOSS.get(),
                        TrueFormRebelusBossEntity.createAttributes().build()));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PreservedClient.init(bus));
    }
}
