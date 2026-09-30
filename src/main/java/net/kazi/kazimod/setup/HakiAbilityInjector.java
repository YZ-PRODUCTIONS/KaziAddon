package net.kazi.kazimod.setup;

import net.kazi.kazimod.KaziMod;
import net.kazi.kazimod.abilities.HakiRework.BusoshokuHakiFullBodyHardeningRework;
import net.kazi.kazimod.abilities.HakiRework.HakiSenseAbility;
import net.kazi.kazimod.abilities.HakiRework.KenbunshokuHakiFutureSightRework;
import sun.misc.Unsafe;
import xyz.pixelatedw.mineminenomi.abilities.haki.BusoshokuHakiFullBodyHardeningAbility;
import xyz.pixelatedw.mineminenomi.abilities.haki.KenbunshokuHakiFutureSightAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class HakiAbilityInjector {

    private HakiAbilityInjector() {
    }

    public static void inject() {
        try {
            Field field = ModAbilities.class.getDeclaredField("HAKI_ABILITIES");
            field.setAccessible(true);

            AbilityCore<?>[] current = (AbilityCore<?>[]) field.get(null);
            List<AbilityCore<?>> updated = new ArrayList<>();

            if (current != null) {
                updated.addAll(Arrays.asList(current));
            }

            replace(updated, BusoshokuHakiFullBodyHardeningAbility.INSTANCE, BusoshokuHakiFullBodyHardeningRework.INSTANCE);
            replace(updated, KenbunshokuHakiFutureSightAbility.INSTANCE, KenbunshokuHakiFutureSightRework.INSTANCE);
            addIfMissing(updated, HakiSenseAbility.INSTANCE);

            setStaticFinal(field, updated.toArray(new AbilityCore[0]));
            KaziMod.LOGGER.info("[HakiAbilityInjector] Updated HAKI_ABILITIES ({} total abilities)", updated.size());
        } catch (Exception e) {
            KaziMod.LOGGER.error("[HakiAbilityInjector] Failed to inject haki abilities", e);
        }
    }

    private static void replace(List<AbilityCore<?>> abilities, AbilityCore<?> original, AbilityCore<?> replacement) {
        int index = abilities.indexOf(original);
        if (index >= 0) {
            abilities.set(index, replacement);
        } else {
            addIfMissing(abilities, replacement);
        }
    }

    private static void addIfMissing(List<AbilityCore<?>> abilities, AbilityCore<?> ability) {
        if (!abilities.contains(ability)) {
            abilities.add(ability);
        }
    }

    private static void setStaticFinal(Field field, Object value) throws Exception {
        try {
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            field.set(null, value);
        } catch (IllegalAccessException ignored) {
            // Java 8 may still reject Field#set for static finals after clearing
            // the modifier. Unsafe updates the field storage directly.
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) unsafeField.get(null);
            unsafe.putObjectVolatile(unsafe.staticFieldBase(field), unsafe.staticFieldOffset(field), value);
        }
    }
}
