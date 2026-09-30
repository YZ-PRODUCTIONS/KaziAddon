package net.kazi.kazimod.mixin;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.pixelatedw.mineminenomi.commands.CheckFruitsCommand;
import xyz.pixelatedw.mineminenomi.init.ModValues;
import xyz.pixelatedw.mineminenomi.items.AkumaNoMiItem;

import java.util.ArrayList;

/**
 * Filters the stale, unregistered fruit instances left in ModValues by Cart
 * Addon 0.7.5 when Mine Mine no Mi's /check_fruits list command is displayed.
 * The backing list and every registered replacement fruit remain untouched.
 */
@Mixin(value = CheckFruitsCommand.class, remap = false)
public abstract class CheckFruitsCommandMixin {

    @Redirect(
            method = "checkFruitsInWorld",
            at = @At(
                    value = "FIELD",
                    target = "Lxyz/pixelatedw/mineminenomi/init/ModValues;DEVIL_FRUITS:Ljava/util/ArrayList;"
            ),
            require = 1,
            remap = false
    )
    private static ArrayList<AkumaNoMiItem> kazi$onlyListRegisteredFruits() {
        ArrayList<AkumaNoMiItem> registeredFruits = new ArrayList<>();

        for (AkumaNoMiItem fruit : ModValues.DEVIL_FRUITS) {
            ResourceLocation registryName = fruit.getRegistryName();
            if (registryName != null && ForgeRegistries.ITEMS.getValue(registryName) == fruit) {
                registeredFruits.add(fruit);
            }
        }

        return registeredFruits;
    }
}
