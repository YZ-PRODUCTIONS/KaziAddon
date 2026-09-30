package net.kazi.kazimod.mixin;

import net.MrMagicalCart.cartaddon.abilities.yamiextra.BlackHandAbility;
import net.MrMagicalCart.cartaddon.init.ReworkAbilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.abilities.yami.AbsorbedBlocksAbility;
import xyz.pixelatedw.mineminenomi.abilities.yami.BlackHoleAbility;
import xyz.pixelatedw.mineminenomi.abilities.yami.BlackRoadAbility;
import xyz.pixelatedw.mineminenomi.abilities.yami.DarkMatterAbility;
import net.kazi.kazimod.abilities.YamiRework.KurozuRework;
import xyz.pixelatedw.mineminenomi.abilities.yami.LiberationAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;

@Mixin(value = ReworkAbilities.class, remap = false)
public abstract class CartYamiAbilitiesMixin {

    @Inject(method = "yamiAbilities", at = @At("RETURN"), cancellable = true, remap = false)
    private static void kazimod$useBaseYamiMoves(
            CallbackInfoReturnable<AbilityCore<?>[]> callback) {
        callback.setReturnValue(new AbilityCore[]{
                BlackHoleAbility.INSTANCE,
                LiberationAbility.INSTANCE,
                BlackRoadAbility.INSTANCE,
                DarkMatterAbility.INSTANCE,
                KurozuRework.INSTANCE,
                BlackHandAbility.INSTANCE,
                AbsorbedBlocksAbility.INSTANCE
        });
    }
}
