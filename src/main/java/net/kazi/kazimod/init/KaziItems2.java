package net.kazi.kazimod.init;

import net.kazi.kazimod.api.KaziRegistry;
import net.kazi.kazimod.items.SkillBookItem;
import net.minecraftforge.fml.RegistryObject;

/**
 * Secondary item registry for KaziAdditions items.
 *
 * Items registered here use the same KaziRegistry.ITEMS DeferredRegister
 * that KaziMod already calls register() on, so no extra wiring is needed
 * in KaziMod — just call KaziItems2.init() early (e.g. from NusuAbilities.register()
 * or from a static initialiser).
 */
public class KaziItems2 {

    public static RegistryObject<SkillBookItem> SKILL_BOOK;

    /** Call this before the deferred register fires (i.e. during mod construction). */
    public static void init() {
        SKILL_BOOK = KaziRegistry.registerItem("Skill Book", SkillBookItem::new);
    }
}