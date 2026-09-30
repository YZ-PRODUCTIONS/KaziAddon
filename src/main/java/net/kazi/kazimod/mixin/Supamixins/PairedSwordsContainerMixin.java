package net.kazi.kazimod.mixin.Supamixins;

import net.kazi.kazimod.items.PairedSwordsItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep projected blades in the player's inventory on both sides of the connection. */
@Mixin(Container.class)
public abstract class PairedSwordsContainerMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void kazi$keepProjectedSwords(int slotId, int button, ClickType type,
                                          PlayerEntity player, CallbackInfoReturnable<ItemStack> cir) {
        Container container = (Container) (Object) this;
        // Drag completion can write several slots, so reject the whole drag gesture.
        if (type == ClickType.QUICK_CRAFT && PairedSwordsItem.isPair(player.inventory.getCarried())) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        if (slotId < 0 || slotId >= container.slots.size()) {
            return;
        }
        Slot slot = container.slots.get(slotId);
        if (type == ClickType.QUICK_MOVE && PairedSwordsItem.isPair(slot.getItem())) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        if (slot.container == player.inventory) {
            return;
        }
        if (type == ClickType.PICKUP && PairedSwordsItem.isPair(player.inventory.getCarried())) {
            cir.setReturnValue(ItemStack.EMPTY);
        } else if (type == ClickType.SWAP && (button >= 0 && button < 9 || button == 40)
                && PairedSwordsItem.isPair(player.inventory.getItem(button))) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
