package net.kazi.kazimod.rules;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;

/** Hardcoded deny list used by the enchantment-blocking mixins. */
public final class DisabledEnchantments {

    private static final Set<ResourceLocation> DISABLED = new HashSet<>();

    static {
        // Add one registry ID per line. Examples:
        // disable("minecraft:mending");
        // disable("minecraft:vanishing_curse");
        // disable("modid:enchantment_name");
    }

    private DisabledEnchantments() {
    }

    private static void disable(String registryId) {
        DISABLED.add(new ResourceLocation(registryId));
    }

    public static boolean isDisabled(@Nullable Enchantment enchantment) {
        return enchantment != null && isDisabled(Registry.ENCHANTMENT.getKey(enchantment));
    }

    public static boolean isDisabled(@Nullable EnchantmentData enchantment) {
        return enchantment != null && isDisabled(enchantment.enchantment);
    }

    public static boolean isDisabled(@Nullable ResourceLocation registryId) {
        return registryId != null && DISABLED.contains(registryId);
    }

    public static <M extends Map<Enchantment, Integer>> Map<Enchantment, Integer> filteredCopy(M enchantments) {
        Map<Enchantment, Integer> filtered = new LinkedHashMap<>();
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            if (!isDisabled(entry.getKey())) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }

    public static void sanitizeTag(@Nullable CompoundNBT tag) {
        if (tag == null || DISABLED.isEmpty()) {
            return;
        }

        sanitizeList(tag, "Enchantments");
        sanitizeList(tag, "StoredEnchantments");
    }

    public static void sanitizeList(ListNBT enchantments) {
        if (DISABLED.isEmpty()) {
            return;
        }

        for (int index = enchantments.size() - 1; index >= 0; --index) {
            CompoundNBT enchantmentTag = enchantments.getCompound(index);
            ResourceLocation id = ResourceLocation.tryParse(enchantmentTag.getString("id"));
            if (isDisabled(id)) {
                enchantments.remove(index);
            }
        }
    }

    private static void sanitizeList(CompoundNBT tag, String key) {
        if (!tag.contains(key, 9)) {
            return;
        }

        ListNBT enchantments = tag.getList(key, 10);
        sanitizeList(enchantments);
        if (enchantments.isEmpty()) {
            tag.remove(key);
        }
    }
}
