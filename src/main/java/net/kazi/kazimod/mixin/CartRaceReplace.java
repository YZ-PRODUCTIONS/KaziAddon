
package net.kazi.kazimod.mixin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Supplier;

import net.MrMagicalCart.cartaddon.abilities.cyborgextra.*;
import net.MrMagicalCart.cartaddon.abilities.electroextra.*;
import net.MrMagicalCart.cartaddon.abilities.fishmankarateextra.*;
import net.MrMagicalCart.cartaddon.abilities.modifiedhuman.GeneticAwakeningAbility;
import net.MrMagicalCart.cartaddon.abilities.oni.*;
import net.MrMagicalCart.cartaddon.abilities.rokushikiextra.*;
import net.MrMagicalCart.cartaddon.abilities.saber.NewDivineDepartureAbility;
import net.MrMagicalCart.cartaddon.init.CartResources;
import net.kazi.kazimod.abilities.HumanRework.KamieRework;
import net.kazi.kazimod.abilities.HumanRework.SoruRework;
import net.kazi.kazimod.abilities.HumanRework.TekkaiRework;
import net.kazi.kazimod.abilities.MinkRework.ElectricalBurstRework;
import net.kazi.kazimod.abilities.MinkRework.ElectricalMissileRework;
import net.kazi.kazimod.abilities.MinkRework.ElectricalShowerRework;
import net.kazi.kazimod.abilities.MinkRework.ElectricalTempestaRework;
import net.kazi.kazimod.abilities.onirework.SkullBasherRework;
import net.kazi.kazimod.abilities.onirework.ViciousRoarRework;
import net.minecraftforge.fml.RegistryObject;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCoreUnlockWrapper;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUnlock;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.charactercreator.CharacterCreatorSelectionMap;
import xyz.pixelatedw.mineminenomi.api.charactercreator.RaceId;
import xyz.pixelatedw.mineminenomi.abilities.cyborg.RadicalBeamAbility;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataBase;
import xyz.pixelatedw.mineminenomi.init.ModResources;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

// Cart's race replacement also cancels registerRace at HEAD. A lower mixin
// priority makes this callback run first, so Kazi's replacement is the one
// that supplies the final race ability list.
@Mixin(value = {WyRegistry.class, AbilityDataBase.class}, remap = false, priority = 900)
public abstract class CartRaceReplace {
    public CartRaceReplace() {
    }

    @Inject(
            method = {"registerRace"},
            at = {@At("HEAD")},
            remap = false,
            cancellable = true,
            require = 0
    )
    private static <I extends RaceId> void registerEditedRace(String localizedName, Supplier<I> race, CallbackInfoReturnable<RegistryObject<I>> cir) {
        String resourceName = WyHelper.getResourceName(localizedName);
        WyRegistry.getLangMap().put("race.mineminenomi." + resourceName, localizedName);
        Supplier<I> modifiedRace = race;
        if (resourceName.equalsIgnoreCase("cyborg")) {
            modifiedRace = () -> {
                CharacterCreatorSelectionMap.SelectionInfo info =
                        new CharacterCreatorSelectionMap.SelectionInfo(ModResources.CYBORG);

                info.addTopAbilities(new AbilityCore[]{
                        CartStrongRightAbility.INSTANCE,
                        ReworkedCoupDeVentAbility.INSTANCE,
                        ReworkedFreshFireAbility.INSTANCE,
                        CartCoupDeBooAbility.INSTANCE,
                        CartSouthlandSuplexAbility.INSTANCE,
                        IronBoxingAbility.INSTANCE,
                        ReworkedColaOverdriveAbility.INSTANCE,
                        RadicalBeamAbility.INSTANCE
                });

                info.addBottomAbilities(new AbilityCore[]{
                        GeneralFrankyAbility.INSTANCE
                });

                return (I) new RaceId(info, true, 3);
            };
        }
        if (resourceName.equalsIgnoreCase("fishman")) {
            modifiedRace = () -> {
                CharacterCreatorSelectionMap.SelectionInfo info =
                        new CharacterCreatorSelectionMap.SelectionInfo(ModResources.FISHMAN);

                info.addTopAbilities(new AbilityCore[]{
                        ReworkedKachiageHaisokuAbility.INSTANCE,
                        ReworkedKarakusagawaraSeikenAbility.INSTANCE,
                        ReworkedMizuOsuAbility.INSTANCE,
                        ReworkedMizuShuryudanAbility.INSTANCE,
                        ReworkedMurasameAbility.INSTANCE,
                        ReworkedSamehadaShoteiAbility.INSTANCE,
                        ReworkedTwoFishEngineAbility.INSTANCE,
                        SosharkAbility.INSTANCE,
                        ReworkedUchimizuAbility.INSTANCE,
                        ReworkedMizuTaihoAbility.INSTANCE,
                        ReworkedYarinamiAbility.INSTANCE,
                        CharacterCreatorSelectionMap.FISHMAN_SWIM_SPEED_PERK,
                        CharacterCreatorSelectionMap.FISHMAN_DAMAGE_PERK
                });

                return (I) new RaceId(info, true, 2);
            };
        }
        if (resourceName.equalsIgnoreCase("oni")) {
            modifiedRace = () -> {
                CharacterCreatorSelectionMap.SelectionInfo info =
                        new CharacterCreatorSelectionMap.SelectionInfo(CartResources.ONI);

                info.addTopAbilities(new AbilityCore[]{
                        DrunkOniPassive.INSTANCE,
                        HardenedGutsAbility.INSTANCE,
                        OniAwakeningAbility.INSTANCE,
                        OniDebuffPassiveAbility.INSTANCE,
                        OniPassiveBonusesAbility.INSTANCE,
                        PerceptionBlitzAbility.INSTANCE,
                        SkullBasherRework.INSTANCE,
                        SmashingFistAbility.INSTANCE,
                        ThunderousLeapAbility.INSTANCE,
                        ViciousRoarRework.INSTANCE
                });

                return (I) new RaceId(info, true, 6);
            };
        }
        if (resourceName.equalsIgnoreCase("mink")) {
            modifiedRace = () -> {
                CharacterCreatorSelectionMap.SelectionInfo info =
                        new CharacterCreatorSelectionMap.SelectionInfo(ModResources.MINK1);

                info.addTopAbilities(new AbilityCore[]{
                        CartEleclawAbility.INSTANCE,
                        CartElectricalLunaAbility.INSTANCE,
                        MinkSizePasssiveAbility.INSTANCE,
                        CartSulongAbility.INSTANCE,
                        ElectricalMissileRework.INSTANCE,
                        ElectricalShowerRework.INSTANCE,
                        ElectricalBurstRework.INSTANCE,
                        ElectricalTempestaRework.INSTANCE
                });

                RaceId id = new RaceId(info, true, 4);
                // Character creation resolves its selected index through this list.
                // Keep MMNM/Cart's order; an empty list drops the selection and renders as Dog.
                id.setSubRaces(new ArrayList<>(Arrays.asList("mink_dog", "mink_lion", "mink_bunny")));
                return (I) id;
            };
        }
        if (resourceName.equalsIgnoreCase("human")) {
            modifiedRace = () -> {
                CharacterCreatorSelectionMap.SelectionInfo info =
                        new CharacterCreatorSelectionMap.SelectionInfo(ModResources.HUMAN);

                info.addTopAbilities(new AbilityCore[]{
                        KamieRework.INSTANCE,
                        ReworkedGeppoAbility.INSTANCE,
                        ReworkedRankyakuAbility.INSTANCE,
                        ReworkedRokuoganAbility.INSTANCE,
                        ReworkedShiganAbility.INSTANCE,
                        SoruRework.INSTANCE,
                        TekkaiRework.INSTANCE
                });

                return (I) new RaceId(info, true, 1);
            };
        }
        RegistryObject<I> reg = WyRegistry.RACES.register(resourceName, modifiedRace);
        cir.setReturnValue(reg);
    }

    @Dynamic("Injected only into the AbilityDataBase target of this combined mixin")
    @Inject(
            method = "addUnlockedAbility(Lxyz/pixelatedw/mineminenomi/api/abilities/AbilityCore;Lxyz/pixelatedw/mineminenomi/api/abilities/AbilityUnlock;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void kazi$blockRetiredAddonUnlock(AbilityCore<?> core, AbilityUnlock unlockType,
                                                   CallbackInfoReturnable<Boolean> cir) {
        if (kazi$isRetiredAddonCore(core)) {
            cir.setReturnValue(false);
        }
    }

    @Dynamic("Injected only into the AbilityDataBase target of this combined mixin")
    @Inject(
            method = "addUnlockedAbility(Lxyz/pixelatedw/mineminenomi/api/abilities/AbilityCoreUnlockWrapper;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void kazi$blockSavedRetiredAddonUnlock(AbilityCoreUnlockWrapper<?> wrapper,
                                                        CallbackInfoReturnable<Boolean> cir) {
        if (wrapper != null && kazi$isRetiredAddonCore(wrapper.getAbilityCore())) {
            cir.setReturnValue(false);
        }
    }

    @Dynamic("Injected only into the AbilityDataBase target of this combined mixin")
    @Inject(
            method = "addPassiveAbility(Lxyz/pixelatedw/mineminenomi/api/abilities/IAbility;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void kazi$blockRetiredAddonPassive(IAbility ability,
                                                    CallbackInfoReturnable<Boolean> cir) {
        if (kazi$isRetiredAddonAbility(ability)) {
            cir.setReturnValue(false);
        }
    }

    @Dynamic("Injected only into the AbilityDataBase target of this combined mixin")
    @Inject(
            method = "setEquippedAbility(ILxyz/pixelatedw/mineminenomi/api/abilities/IAbility;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void kazi$blockRetiredAddonEquip(int slot, IAbility ability,
                                                  CallbackInfoReturnable<Boolean> cir) {
        if (kazi$isRetiredAddonAbility(ability)) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private static boolean kazi$isRetiredAddonAbility(IAbility ability) {
        return ability != null && kazi$isRetiredAddonCore(ability.getCore());
    }

    @Unique
    private static boolean kazi$isRetiredAddonCore(AbilityCore<?> core) {
        // Also filter direct quest grants and saved unlocks/equipment, which bypass canUnlock.
        // Match Cart's core only: Saber still uses Kazi's DivineDepartureClone.
        return core == GeneticAwakeningAbility.INSTANCE
                || core == NewDivineDepartureAbility.INSTANCE;
    }
}
