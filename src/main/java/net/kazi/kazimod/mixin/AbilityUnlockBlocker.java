package net.kazi.kazimod.mixin;

import net.MrMagicalCart.cartaddon.abilities.axestyle.*;
import net.MrMagicalCart.cartaddon.abilities.blacklegextra.CartAntiMannerKickCourseAbility;
import net.MrMagicalCart.cartaddon.abilities.blacklegextra.CartPartyTableKickCourseAbility;
import net.MrMagicalCart.cartaddon.abilities.brawlerextra.*;
import net.MrMagicalCart.cartaddon.abilities.electroextra.CartElectricalMissileAbility;
import net.MrMagicalCart.cartaddon.abilities.electroextra.CartElectricalShowerAbility;
import net.MrMagicalCart.cartaddon.abilities.electroextra.CartElectricalTempestaAbility;
import net.MrMagicalCart.cartaddon.abilities.electroextra.ElectricalBurstAbility;
import net.MrMagicalCart.cartaddon.abilities.modifiedhuman.BootBoostAbility;
import net.MrMagicalCart.cartaddon.abilities.modifiedhuman.CapeGuardAbility;
import net.MrMagicalCart.cartaddon.abilities.modifiedhuman.ExoRepairAbility;
import net.MrMagicalCart.cartaddon.abilities.modifiedhuman.GeneticAwakeningAbility;
import net.MrMagicalCart.cartaddon.abilities.modifiedhuman.HoverBootsAbility;
import net.MrMagicalCart.cartaddon.abilities.modifiedhuman.ModifiedHumanPassiveBonusesAbility;
import net.MrMagicalCart.cartaddon.abilities.modifiedhuman.ModifiedHumanPenalty;
import net.MrMagicalCart.cartaddon.abilities.nitoryu.NitoryuIaiRashomonAbility;
import net.MrMagicalCart.cartaddon.abilities.nitoryu.SaiKuruAbility;
import net.MrMagicalCart.cartaddon.abilities.nitoryu.TakaNamiAbility;
import net.MrMagicalCart.cartaddon.abilities.oni.SkullBasherAbility;
import net.MrMagicalCart.cartaddon.abilities.oni.ViciousRoarAbility;
import net.MrMagicalCart.cartaddon.abilities.rokushikiextra.ReworkedKamieAbility;
import net.MrMagicalCart.cartaddon.abilities.rokushikiextra.ReworkedSoruAbility;
import net.MrMagicalCart.cartaddon.abilities.ryusoken.*;
import net.MrMagicalCart.cartaddon.abilities.swordsmenextra.*;

import net.MrMagicalCart.cartaddon.abilities.trident.AbsolutePierceAbility;
import net.MrMagicalCart.cartaddon.abilities.trident.DrillJabAbility;
import net.MrMagicalCart.cartaddon.abilities.trident.SkySplitterDescentAbility;
import net.MrMagicalCart.cartaddon.abilities.trident.VaultAbility;
import net.minecraft.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import xyz.pixelatedw.mineminenomi.abilities.doctor.*;
import xyz.pixelatedw.mineminenomi.abilities.haki.BusoshokuHakiHardeningAbility;
import xyz.pixelatedw.mineminenomi.abilities.haki.BusoshokuHakiImbuingAbility;
import xyz.pixelatedw.mineminenomi.abilities.haki.BusoshokuHakiFullBodyHardeningAbility;
import xyz.pixelatedw.mineminenomi.abilities.haki.HaoshokuHakiInfusionAbility;
import xyz.pixelatedw.mineminenomi.abilities.haki.KenbunshokuHakiFutureSightAbility;
import xyz.pixelatedw.mineminenomi.abilities.swordsman.YakkodoriAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;

@Mixin(value = AbilityCore.class, remap = false)
public abstract class AbilityUnlockBlocker {

    @Inject(
            method = "canUnlock",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void zaza$blockSelectedAbilities(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {

        AbilityCore self = (AbilityCore)(Object) this;

        if (self == ReworkedHiryuKaenAbility.INSTANCE
                || self == ReworkedSanbyakurokujuPoundHoAbility.INSTANCE
                || self == ReworkedYakkodoriAbility.INSTANCE
                || self == NitoryuIaiRashomonAbility.INSTANCE
                || self == TakaNamiAbility.INSTANCE
                || self == ViciousRoarAbility.INSTANCE
                || self == SkullBasherAbility.INSTANCE
                || self == MountainEaterAbility.INSTANCE
                || self == SaiKuruAbility.INSTANCE
                || self == SkySplitterAbility.INSTANCE
                || self == ReversalAbility.INSTANCE
                || self == CartPartyTableKickCourseAbility.INSTANCE
                || self == TyrantCleaveAbility.INSTANCE
                || self == BerserkAbility.INSTANCE
                || self == FutenrakuAbility.INSTANCE
                || self == PredatorsThrowAbility.INSTANCE
                || self == YasotakeruAbility.INSTANCE
                || self == GalaxyImpactAbility.INSTANCE
                || self == ReworkedOTatsumakiAbility.INSTANCE
                || self == ReworkedShiShishiSonsonAbility.INSTANCE
                || self == RyuNoKagizumeAbility.INSTANCE
                || self == DancingDragonSlamAbility.INSTANCE
                || self == RyuNoIbukiAbility.INSTANCE
                || self == TalonRushAbility.INSTANCE
                || self == FistsOfLoveBarrageAbility.INSTANCE
                || self == YakkodoriAbility.INSTANCE
                || self == AntidoteShotAbility.INSTANCE
                || self == DopingAbility.INSTANCE
                || self == FailedExperimentAbility.INSTANCE
                || self == FirstAidAbility.INSTANCE
                || self == MedicBagExplosionAbility.INSTANCE
                || self == VirusZoneAbility.INSTANCE
                || self == CartElectricalShowerAbility.INSTANCE
                || self == CartElectricalMissileAbility.INSTANCE
                || self == CartElectricalTempestaAbility.INSTANCE
                || self == ElectricalBurstAbility.INSTANCE
                || self == AbsolutePierceAbility.INSTANCE
                || self == DrillJabAbility.INSTANCE
                || self == SkySplitterDescentAbility.INSTANCE
                || self == VaultAbility.INSTANCE
                || self == ReworkedSpinningBrawlAbility.INSTANCE
                || self == ReworkedKamieAbility.INSTANCE
                || self == ReworkedSoruAbility.INSTANCE
                || self == ReworkedJishinHoAbility.INSTANCE
                || self == CartAntiMannerKickCourseAbility.INSTANCE
                || self == BusoshokuHakiFullBodyHardeningAbility.INSTANCE
                || self == KenbunshokuHakiFutureSightAbility.INSTANCE
                || self == ModifiedHumanPassiveBonusesAbility.INSTANCE)

                {
            cir.setReturnValue(false);
        }
    }
}
