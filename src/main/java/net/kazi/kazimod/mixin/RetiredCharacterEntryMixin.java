package net.kazi.kazimod.mixin;

import net.kazi.kazimod.events.RetiredCharacterOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.api.charactercreator.ICharacterCreatorEntry;
import xyz.pixelatedw.mineminenomi.api.charactercreator.RaceId;
import xyz.pixelatedw.mineminenomi.api.charactercreator.StyleId;

@Mixin(value = {RaceId.class, StyleId.class}, remap = false)
public abstract class RetiredCharacterEntryMixin {
    @Inject(method = "isInBook", at = @At("HEAD"), cancellable = true)
    private void kazi$hideRetiredEntry(CallbackInfoReturnable<Boolean> cir) {
        if (RetiredCharacterOptions.retired(((ICharacterCreatorEntry)this).getRegistryName())) cir.setReturnValue(false);
    }
}
