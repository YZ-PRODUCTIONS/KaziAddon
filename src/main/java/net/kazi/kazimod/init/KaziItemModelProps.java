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

    @OnlyIn(Dist.CLIENT)
    public static final IItemPropertyGetter DEVIL_FRUITS_RANDOMIZER = (stack, world, entity) -> {
        CompoundNBT nbt = stack.getTag();

        if (nbt != null && nbt.contains("type")) {
            return nbt.getInt("type");
        }

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
        ResourceLocation type = new ResourceLocation("type");

        if (KaziItems.TOSHI_TOSHI_NO_MI != null && KaziItems.TOSHI_TOSHI_NO_MI.get() != null)
            ItemModelsProperties.register(KaziItems.TOSHI_TOSHI_NO_MI.get(), type, DEVIL_FRUITS_RANDOMIZER);

        if (KaziItems.TOKI_TOKI_NO_MI != null && KaziItems.TOKI_TOKI_NO_MI.get() != null)
            ItemModelsProperties.register(KaziItems.TOKI_TOKI_NO_MI.get(), type, DEVIL_FRUITS_RANDOMIZER);

        if (KaziItems.TENKI_TENKI_NO_MI != null && KaziItems.TENKI_TENKI_NO_MI.get() != null)
            ItemModelsProperties.register(KaziItems.TENKI_TENKI_NO_MI.get(), type, DEVIL_FRUITS_RANDOMIZER);

        if (KaziItems.KOKU_KOKU_NO_MI != null && KaziItems.KOKU_KOKU_NO_MI.get() != null)
            ItemModelsProperties.register(KaziItems.KOKU_KOKU_NO_MI.get(), type, DEVIL_FRUITS_RANDOMIZER);

        if (KaziItems.KAKE_KAKE_NO_MI != null && KaziItems.KAKE_KAKE_NO_MI.get() != null)
            ItemModelsProperties.register(KaziItems.KAKE_KAKE_NO_MI.get(), type, DEVIL_FRUITS_RANDOMIZER);

        if (KaziItems.NUSU_NUSU_NO_MI != null && KaziItems.NUSU_NUSU_NO_MI.get() != null)
            ItemModelsProperties.register(KaziItems.NUSU_NUSU_NO_MI.get(), type, DEVIL_FRUITS_RANDOMIZER);
    }
}