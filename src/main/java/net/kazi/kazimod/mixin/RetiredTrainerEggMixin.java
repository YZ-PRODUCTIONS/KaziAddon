package net.kazi.kazimod.mixin;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Item.class)
public abstract class RetiredTrainerEggMixin {
    @Inject(method = "fillItemCategory", at = @At("HEAD"), cancellable = true)
    private void kazi$hideRetiredEggs(ItemGroup group, NonNullList<ItemStack> items, CallbackInfo ci) {
        if (!((Object)this instanceof SpawnEggItem)) return;
        ResourceLocation id = ((Item)(Object)this).getRegistryName();
        if (id != null && id.getNamespace().equals("cartaddon")
                && (id.getPath().equals("hasshoken_trainer_spawn_egg") || id.getPath().equals("great_blade_trainer_spawn_egg"))) ci.cancel();
    }
}
