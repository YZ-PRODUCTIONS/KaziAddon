package net.kazi.kazimod.init;

import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xyz.pixelatedw.mineminenomi.init.ModValues;
import xyz.pixelatedw.mineminenomi.items.AkumaNoMiItem;

public class KaziItemModelProps {

    // Mirrors CartAddon's CartItemModelProps.DEVIL_FRUITS_RANDOMIZER.
    // Reads the "type" int from the item NBT (set by Mine Mine no Mi's randomizer),
    // applies randomness if not yet assigned, and returns it as a float so the
    // model overrides in the item JSON (generic_fruit_1 through 10) resolve correctly.
    @OnlyIn(Dist.CLIENT)
    public static final IItemPropertyGetter DEVIL_FRUITS_RANDOMIZER = (stack, world, entity) -> {
        CompoundNBT nbt = stack.getTag();

        if (nbt != null && nbt.contains("type")) {
            return nbt.getInt("type");
        }

        // Not yet randomized - let Mine Mine no Mi apply randomness
        if (world != null) {
            ((AkumaNoMiItem) stack.getItem()).applyRandomness(world, stack);
        }

        nbt = stack.getTag();
        if (nbt != null && nbt.contains("type")) {
            return nbt.getInt("type");
        }

        return 0;
    };

    public static void register() {
        ItemModelsProperties.register(
                KaziItems.TOSHI_TOSHI_NO_MI.get(),
                new ResourceLocation("type"),
                DEVIL_FRUITS_RANDOMIZER
        );
    }

}

