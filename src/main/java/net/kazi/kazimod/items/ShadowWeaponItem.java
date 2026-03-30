package net.kazi.kazimod.items;

import net.minecraft.item.IItemTier;
import net.minecraft.item.ItemTier;
import net.minecraft.item.Rarity;
import net.minecraft.item.SwordItem;
import xyz.pixelatedw.mineminenomi.init.ModCreativeTabs;

public class ShadowWeaponItem extends SwordItem {

    private static final IItemTier SHADOW_TIER = ItemTier.NETHERITE;

    public ShadowWeaponItem() {
        super(SHADOW_TIER, 6, -2.2F, new Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON));
    }
}
