//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.api;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryManager;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;

@EventBusSubscriber(
        modid = "kazimod"
)
public class KaziRegistries {
    public static final IForgeRegistry<AbilityCore<?>> ABILITIES;

    public static <T extends IForgeRegistryEntry<T>> void make(ResourceLocation name, Class<T> type) {
        (new RegistryBuilder()).setName(name).setType(type).setMaxID(2147483646).create();
    }

    static {
        ABILITIES = RegistryManager.ACTIVE.getRegistry(AbilityCore.class);

    }

}
