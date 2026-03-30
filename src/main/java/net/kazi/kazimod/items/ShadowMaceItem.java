package net.kazi.kazimod.items;

import net.minecraft.item.Item;
import xyz.pixelatedw.mineminenomi.items.weapons.ModSwordItem;

public class ShadowMaceItem extends ModSwordItem {

    public ShadowMaceItem() {
        super(new Item.Properties(), 9, -2.6F);
        this.setBlunt();
        this.setExtraAttackRange(1.0D);
    }
}
