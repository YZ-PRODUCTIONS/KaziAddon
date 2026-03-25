package net.kazi.kazimod.init;

import net.kazi.kazimod.items.AwakeningEssenceItem;
import net.kazi.kazimod.items.ShadowMaceItem;
import net.kazi.kazimod.items.ShadowSpearItem;
import net.kazi.kazimod.items.ShadowSwordItem;
import net.kazi.kazimod.items.ShadowWeaponItem;
import net.kazi.kazimod.items.SkillBookItem;
import net.minecraft.item.Item;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class KaziItems2 {

    // Own DeferredRegister so it doesn't depend on KaziRegistry internals.
    // Register it to the mod event bus in KaziMod constructor:
    //   KaziItems2.ITEMS.register(modEventBus);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "kazimod");

    public static final RegistryObject<SkillBookItem> SKILL_BOOK =
            ITEMS.register("skill_book", SkillBookItem::new);

    public static final RegistryObject<AwakeningEssenceItem> AWAKENING_ESSENCE =
            ITEMS.register("awakening_essence", AwakeningEssenceItem::new);

    public static final RegistryObject<ShadowSwordItem> SHADOW_SWORD =
            ITEMS.register("shadow_sword", ShadowSwordItem::new);

    public static final RegistryObject<ShadowMaceItem> SHADOW_MACE =
            ITEMS.register("shadow_mace", ShadowMaceItem::new);

    public static final RegistryObject<ShadowWeaponItem> SHADOW_KNIFE =
            ITEMS.register("shadow_knife", ShadowWeaponItem::new);

    public static final RegistryObject<ShadowSpearItem> SHADOW_SPEAR =
            ITEMS.register("shadow_spear", ShadowSpearItem::new);

    public static final RegistryObject<ShadowWeaponItem> SHADOW_AXE =
            ITEMS.register("shadow_axe", ShadowWeaponItem::new);

    // Called from KaziMod constructor - kept for compatibility but registration
    // now happens via the DeferredRegister above.
    public static void init() {
        // no-op: items are registered via the static DeferredRegister fields above
    }
}
