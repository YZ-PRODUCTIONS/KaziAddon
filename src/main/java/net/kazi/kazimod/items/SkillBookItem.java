package net.kazi.kazimod.items;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ItemSpawnComponent;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;

import java.util.Optional;

/**
 * The Skill Book — spawned by SkillBookCreationAbility.
 *
 * Behaviour mirrors AbilitySwordItem:
 *   - Every inventory tick, if the ItemSpawnComponent is no longer active
 *     (i.e. the ability was despawned), remove the item from the player's inventory.
 *   - onEntityItemUpdate: if this item is dropped on the ground, immediately delete it.
 *
 * The item uses the custom chrollobook 3D model (skill_book.json).
 */
public class SkillBookItem extends Item {

    public static final String REGISTRY_NAME = "skill_book";

    public SkillBookItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC));
    }

    /** Every tick while in a player's inventory: delete if the spawn component is gone. */
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClientSide) return;
        if (!(entity instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) entity;

        IAbilityData data = AbilityDataCapability.get(player);
        if (data == null) return;

        // Find any equipped ability that has an active ItemSpawnComponent
        // tracking this stack. If none found, remove this item.
        boolean foundActive = false;
        for (IAbility ability : data.getEquippedAndPassiveAbilities()) {
            Optional<?> comp = ability.getComponent(ModAbilityKeys.ITEM_SPAWN);
            if (comp.isPresent() && comp.get() instanceof ItemSpawnComponent) {
                if (((ItemSpawnComponent) comp.get()).isActive()) {
                    foundActive = true;
                    break;
                }
            }
        }

        if (!foundActive) {
            player.inventory.removeItemNoUpdate(slot);
        }
    }

    /** When dropped on the ground, immediately delete. */
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (entity.isAlive()) {
            entity.kill();
        }
        return true;
    }
}