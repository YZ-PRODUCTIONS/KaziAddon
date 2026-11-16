package net.kazi.kazimod.mixin;

import net.kazi.kazimod.yami.DarknessLaunch;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.events.BlocksEvents;

@Mixin(value = BlocksEvents.class, remap = false)
public abstract class YamiDarknessReleaseMixin {
    @Inject(method = "onEntityUpdate", at = @At("HEAD"), cancellable = true)
    private static void kazi$leaveDarkness(LivingUpdateEvent event, CallbackInfo ci) {
        LivingEntity entity = event.getEntityLiving();
        if (DarknessLaunch.bypasses(entity) && DarknessLaunch.isDarkness(entity.level.getBlockState(
                new BlockPos(entity.getX(), entity.getY() + .15, entity.getZ())).getBlock())) ci.cancel();
    }
}
