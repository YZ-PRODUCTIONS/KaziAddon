package net.kazi.kazimod.mixin.client;
import net.minecraft.client.resources.ClientLanguageMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Preserve the saved race ID while avoiding CartAddon's later language override. */
@Mixin(ClientLanguageMap.class)
public abstract class GhoulRaceNameMixin {
    @Inject(method="getOrDefault",at=@At("HEAD"),cancellable=true)
    private void ghoulName(String key,CallbackInfoReturnable<String> cir){
        if("race.mineminenomi.zombie".equals(key)||"race.cartaddon.zombie".equals(key))
            cir.setReturnValue(((ClientLanguageMap)(Object)this).getOrDefault("race.kazimod.ghoul"));
    }
}
