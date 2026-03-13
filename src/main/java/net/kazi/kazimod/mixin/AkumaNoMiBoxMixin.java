package net.kazi.kazimod.mixin;

import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.pixelatedw.mineminenomi.items.AkumaNoMiBoxItem;

@Mixin(value = AkumaNoMiBoxItem.class, remap = false)
public class AkumaNoMiBoxMixin {

    @Shadow
    private static Pair<Integer, ResourceLocation> TIER_1_FRUITS;
    @Shadow
    private static Pair<Integer, ResourceLocation> TIER_2_FRUITS;
    @Shadow
    private static Pair<Integer, ResourceLocation> TIER_3_FRUITS;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void overrideBoxTables(CallbackInfo ci) {
        TIER_1_FRUITS = ImmutablePair.of(1, new ResourceLocation("kazimod", "dfboxes/wooden_box"));
        TIER_2_FRUITS = ImmutablePair.of(2, new ResourceLocation("kazimod", "dfboxes/iron_box"));
        TIER_3_FRUITS = ImmutablePair.of(3, new ResourceLocation("kazimod", "dfboxes/golden_box"));
    }
}