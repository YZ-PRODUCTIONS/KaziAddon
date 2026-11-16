package net.kazi.kazimod.events;

import net.kazi.kazimod.KaziMod;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;

/** Retire removed Nagi awakening entries safely when existing worlds are loaded. */
@Mod.EventBusSubscriber(modid = KaziMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RetiredNagiAbilities {
    private RetiredNagiAbilities() {}

    public static boolean isRetired(ResourceLocation id) {
        if (id == null || !KaziMod.MODID.equals(id.getNamespace())) return false;
        switch (id.getPath()) {
            case "imagine_breaker":
            case "silent_stride":
            case "silent_slice":
            case "silent_step":
            case "silent_box":
            case "silent_death":
                return true;
            default:
                return false;
        }
    }

    @SubscribeEvent
    public static void onMissingMappings(RegistryEvent.MissingMappings<AbilityCore<?>> event) {
        for (RegistryEvent.MissingMappings.Mapping<AbilityCore<?>> mapping : event.getAllMappings()) {
            // Do not remap to the boss copies: that would restore them to player saves.
            // MMNM safely skips unknown unlocked, equipped and passive ability IDs.
            if (isRetired(mapping.key)) mapping.ignore();
        }
    }
}
