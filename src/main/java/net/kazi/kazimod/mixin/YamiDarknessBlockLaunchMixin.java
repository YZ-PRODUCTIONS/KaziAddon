package net.kazi.kazimod.mixin;

import net.MrMagicalCart.cartaddon.blocks.ReworkedYamiBlock;
import net.kazi.kazimod.yami.DarknessLaunch;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.blocks.traps.TrapAbilityBlock;

@Mixin(value = {TrapAbilityBlock.class, ReworkedYamiBlock.class}, remap = false)
public abstract class YamiDarknessBlockLaunchMixin {
    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true, remap = true)
    private void kazi$launchAfterContact(BlockState state, World world, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (DarknessLaunch.contact(state.getBlock(), entity)) ci.cancel();
    }
}
