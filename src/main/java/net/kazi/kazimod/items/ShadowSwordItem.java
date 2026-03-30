package net.kazi.kazimod.items;

import net.minecraft.item.Item;
import xyz.pixelatedw.mineminenomi.items.weapons.ModSwordItem;

public class ShadowSwordItem extends ModSwordItem {

    public ShadowSwordItem() {
        super(new Item.Properties(), 8, -2.1F);
        this.setExtraAttackRange(1.5D);
    }
}
