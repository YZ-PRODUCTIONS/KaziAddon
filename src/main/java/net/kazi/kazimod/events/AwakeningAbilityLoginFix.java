package net.kazi.kazimod.events;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import xyz.pixelatedw.mineminenomi.abilities.gomu.GearFifthAbility;
import xyz.pixelatedw.mineminenomi.abilities.gomu.GomuGomuNoDawnWhipAbility;
import xyz.pixelatedw.mineminenomi.abilities.gomu.GomuGomuNoGigantAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.events.ability.UnlockAbilityEvent;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit;
import xyz.pixelatedw.mineminenomi.items.AkumaNoMiItem;

import java.util.*;

@Mod.EventBusSubscriber
public class AwakeningAbilityLoginFix {

    // Vanilla abilities that have been replaced by KaziMod reworks.
    // These should never be unlocked even if they pass the fruit check,
    // because they no longer exist on any fruit after injection.
    private static final Set<AbilityCore<?>> REPLACED_ABILITIES = new HashSet<>(java.util.Arrays.asList(
            GearFifthAbility.INSTANCE,
            GomuGomuNoDawnWhipAbility.INSTANCE,
            GomuGomuNoGigantAbility.INSTANCE
    ));

    // Maps each AbilityCore to which fruit(s) it belongs to
    private static Map<AbilityCore<?>, Set<AkumaNoMiItem>> abilityToFruits = null;

    /**
     * Call this after FruitAbilityInjector has finished modifying fruit ability lists
     * so the map gets rebuilt with the correct injected abilities.
     */
    public static void invalidateCache() {
        abilityToFruits = null;
    }

    // Players currently in the middle of an intentional awakening grant —
    // unlock events for these players are fully allowed through
    private static final Set<UUID> awakeningInProgress = new HashSet<>();

    public static void beginAwakening(UUID playerUUID) {
        awakeningInProgress.add(playerUUID);
    }

    public static void endAwakening(UUID playerUUID) {
        awakeningInProgress.remove(playerUUID);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onUnlockAbility(UnlockAbilityEvent event) {
        if (event.getResult() == Event.Result.DENY) return;

        LivingEntity entity = event.getEntityLiving();
        AbilityCore<?> core = event.getAbilityCore();
        if (entity == null || core == null) return;

        // If this is an intentional awakening grant, allow everything through
        if (awakeningInProgress.contains(entity.getUUID())) return;

        IDevilFruit devilFruit = DevilFruitCapability.get(entity);
        if (devilFruit == null || !devilFruit.hasAwakenedFruit()) return;

        // Always deny explicitly replaced vanilla abilities
        if (REPLACED_ABILITIES.contains(core)) {
            event.setResult(Event.Result.DENY);
            return;
        }

        if (abilityToFruits == null) buildAbilityToFruitsMap();

        // If this ability doesn't appear in ANY fruit, it's race/style/haki — allow it
        Set<AkumaNoMiItem> fruitsWithThisAbility = abilityToFruits.get(core);
        if (fruitsWithThisAbility == null || fruitsWithThisAbility.isEmpty()) return;

        // Get the player's current fruit
        AkumaNoMiItem playerFruitItem = null;
        try {
            Item item = devilFruit.getDevilFruitItem();
            if (item instanceof AkumaNoMiItem) playerFruitItem = (AkumaNoMiItem) item;
        } catch (Exception e) { return; }
        if (playerFruitItem == null) return;

        // If the ability belongs to the player's fruit, allow it
        if (fruitsWithThisAbility.contains(playerFruitItem)) return;

        // Belongs to a different fruit — deny
        event.setResult(Event.Result.DENY);
    }

    private static void buildAbilityToFruitsMap() {
        abilityToFruits = new HashMap<>();
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            if (!(item instanceof AkumaNoMiItem)) continue;
            AkumaNoMiItem fruit = (AkumaNoMiItem) item;
            AbilityCore<?>[] abilities = fruit.getAbilities();
            if (abilities == null) continue;
            for (AbilityCore<?> ability : abilities) {
                abilityToFruits.computeIfAbsent(ability, k -> new HashSet<>()).add(fruit);
            }
        }
    }
}