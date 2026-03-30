package net.kazi.kazimod.items;

import net.minecraft.item.ItemGroup;
import net.minecraft.util.NonNullList;
import net.minecraft.item.ItemStack;
import xyz.pixelatedw.mineminenomi.items.weapons.ModSpearItem;

public class ShadowSpearItem extends ModSpearItem {

    public ShadowSpearItem() {
        super(8, -2.8F, 1000);
        this.setExtraAttackRange(2.5D);
    }

    @Override
    public void fillItemCategory(ItemGroup group, NonNullList<ItemStack> items) {
        // Intentionally hidden from creative tabs; only used internally by the Doppelman.
    }
}
