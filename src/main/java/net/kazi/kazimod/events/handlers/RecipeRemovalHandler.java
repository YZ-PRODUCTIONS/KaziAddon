package net.kazi.kazimod.events.handlers;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RecipeRemovalHandler {

    private static final Set<ResourceLocation> BLOCKED_ITEMS = new HashSet<>(Arrays.asList(
            new ResourceLocation("cartaddon", "poison_pink_canister"),
            new ResourceLocation("cartaddon", "electric_shock_blue_canister"),
            new ResourceLocation("cartaddon", "stealth_black_canister"),
            new ResourceLocation("cartaddon", "winch_green_canister"),
            new ResourceLocation("cartaddon", "sparkling_red_canister")
    ));

    @SubscribeEvent
    public void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack result = event.getCrafting();
        if (result.isEmpty()) return;

        ResourceLocation id = result.getItem().getRegistryName();
        if (id == null) return;

        if (BLOCKED_ITEMS.contains(id)) {
            // Clear the result slot so the player gets nothing
            result.setCount(0);
        }
    }
}