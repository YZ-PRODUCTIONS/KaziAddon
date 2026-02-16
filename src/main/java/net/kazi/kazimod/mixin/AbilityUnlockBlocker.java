package net.kazi.kazimod.mixin;

import net.MrMagicalCart.cartaddon.abilities.axestyle.MountainEaterAbility;
import net.MrMagicalCart.cartaddon.abilities.axestyle.ReversalAbility;
import net.MrMagicalCart.cartaddon.abilities.axestyle.SkySplitterAbility;
import net.MrMagicalCart.cartaddon.abilities.brawlerextra.ReworkedSuplexAbility;
import net.MrMagicalCart.cartaddon.abilities.electroextra.CartSulongAbility;
import net.MrMagicalCart.cartaddon.abilities.nitoryu.NitoryuIaiRashomonAbility;
import net.MrMagicalCart.cartaddon.abilities.nitoryu.SaiKuruAbility;
import net.MrMagicalCart.cartaddon.abilities.nitoryu.TakaNamiAbility;
import net.MrMagicalCart.cartaddon.abilities.oni.SkullBasherAbility;
import net.MrMagicalCart.cartaddon.abilities.oni.ViciousRoarAbility;
import net.MrMagicalCart.cartaddon.abilities.swordsmenextra.ReworkedHiryuKaenAbility;
import net.MrMagicalCart.cartaddon.abilities.swordsmenextra.ReworkedSanbyakurokujuPoundHoAbility;
import net.MrMagicalCart.cartaddon.abilities.swordsmenextra.ReworkedYakkodoriAbility;
import net.kazi.kazimod.abilities.AxeStyleRework.SkySplitterRework;
import net.kazi.kazimod.abilities.NitoryuRework.SaiKuruRework;
import net.kazi.kazimod.abilities.NitoryuRework.TakaNamiRework;
import net.kazi.kazimod.abilities.onirework.SkullBasherRework;
import net.kazi.kazimod.abilities.swordsmanrework.YakkodoriRework;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;

// import the abilities you want to block
import xyz.pixelatedw.mineminenomi.abilities.rokushiki.RankyakuAbility;
import net.MrMagicalCart.cartaddon.abilities.swordsmenextra.ReworkedOTatsumakiAbility;
import net.MrMagicalCart.cartaddon.abilities.brawlerextra.ReworkedHakaiHoAbility;

@Mixin(value = AbilityCore.class, remap = false)
public abstract class AbilityUnlockBlocker {

    @Inject(
            method = "canUnlock",
            at = @At("HEAD"),
            cancellable = true
    )
    private void zaza$blockSelectedAbilities(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {

        AbilityCore<?> self = (AbilityCore<?>) (Object) this;

        if (self == ReworkedHiryuKaenAbility.INSTANCE
                || self == ReworkedSanbyakurokujuPoundHoAbility.INSTANCE
                || self == ReworkedYakkodoriAbility.INSTANCE
                || self == NitoryuIaiRashomonAbility.INSTANCE
                || self == TakaNamiAbility.INSTANCE
                || self == ViciousRoarAbility.INSTANCE
                || self == SkullBasherAbility.INSTANCE
                || self == CartSulongAbility.INSTANCE
                || self == MountainEaterAbility.INSTANCE
                || self == SaiKuruAbility.INSTANCE
                || self == ReworkedSuplexAbility.INSTANCE
                || self == SkySplitterAbility.INSTANCE
                || self == ReversalAbility.INSTANCE)


        {
            cir.setReturnValue(false);
        }
    }
}