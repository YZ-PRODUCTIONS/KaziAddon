package net.kazi.kazimod.mixin;

import net.kazi.kazimod.items.AwakeningEssenceItem;
import net.kazi.kazimod.preserved.TripleTTrial;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AwakeningEssenceItem.class)
public abstract class TripleTAwakeningMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void preserved$tripleT(World world, PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult<ItemStack>> cir) {
        ActionResult<ItemStack> result = TripleTTrial.use(world, player, hand);
        if (result != null) cir.setReturnValue(result);
    }
}
