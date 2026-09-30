package net.kazi.kazimod.util;

import net.kazi.kazimod.KaziMod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.items.AkumaNoMiItem;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FruitAbilityInjector {

    private static final Logger LOGGER = LogManager.getLogger(KaziMod.MODID);
    private static Field abilitiesField;

    static {
        try {
            abilitiesField = AkumaNoMiItem.class.getDeclaredField("abilities");
            abilitiesField.setAccessible(true);
        } catch (Exception e) {
            LOGGER.error("[FruitAbilityInjector] Failed to access abilities field", e);
        }
    }

    public static void updateFruitAbilities(AkumaNoMiItem fruit, List<AbilityCore<?>> toRemove, List<AbilityCore<?>> toAdd, boolean replaceAll) {
        if (fruit == null || abilitiesField == null) return;

        try {
            AbilityCore<?>[] current = fruit.getAbilities();
            List<AbilityCore<?>> updated = new ArrayList<>();

            if (!replaceAll && current != null) {
                for (AbilityCore<?> ability : current) {
                    if (toRemove == null || !toRemove.contains(ability)) {
                        updated.add(ability);
                    }
                }
            }

            if (toAdd != null && !toAdd.isEmpty()) {
                updated.addAll(toAdd);
            }

            abilitiesField.set(fruit, updated.toArray(new AbilityCore[0]));
            LOGGER.info("[FruitAbilityInjector] Updated {} ({} total abilities)", fruit.getDevilFruitName(), updated.size());

        } catch (Exception e) {
            LOGGER.error("[FruitAbilityInjector] Failed to modify {}", fruit.getDevilFruitName(), e);
        }
    }

    public static void addAbilities(AkumaNoMiItem fruit, AbilityCore<?>... toAdd) {
        updateFruitAbilities(fruit, Collections.<AbilityCore<?>>emptyList(), Arrays.asList(toAdd), false);
    }

    public static void replaceAbilities(AkumaNoMiItem fruit, AbilityCore<?>... toAdd) {
        updateFruitAbilities(fruit, Collections.<AbilityCore<?>>emptyList(), Arrays.asList(toAdd), true);
    }

    public static void removeAbilities(AkumaNoMiItem fruit, AbilityCore<?>... toRemove) {
        updateFruitAbilities(fruit, Arrays.asList(toRemove), Collections.<AbilityCore<?>>emptyList(), false);
    }

    public static void replaceAbility(AkumaNoMiItem fruit, AbilityCore<?> original, AbilityCore<?> replacement) {
        if (fruit == null || abilitiesField == null || original == null || replacement == null) return;
        try {
            AbilityCore<?>[] current = fruit.getAbilities();
            if (current == null) return;
            AbilityCore<?>[] updated = Arrays.copyOf(current, current.length);
            for (int i = 0; i < updated.length; i++) {
                if (original.equals(updated[i])) {
                    updated[i] = replacement;
                }
            }
            abilitiesField.set(fruit, updated);
            LOGGER.info("[FruitAbilityInjector] Replaced an ability in place on {}", fruit.getDevilFruitName());
        } catch (Exception e) {
            LOGGER.error("[FruitAbilityInjector] Failed to replace ability on {}", fruit.getDevilFruitName(), e);
        }
    }
}
