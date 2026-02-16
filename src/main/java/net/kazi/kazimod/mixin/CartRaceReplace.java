
package net.kazi.kazimod.mixin;

import java.util.function.Supplier;

import net.MrMagicalCart.cartaddon.abilities.electroextra.*;
import net.MrMagicalCart.cartaddon.abilities.oni.*;
import net.MrMagicalCart.cartaddon.init.CartResources;
import net.kazi.kazimod.abilities.MinkRework.SulongRework;
import net.kazi.kazimod.abilities.onirework.SkullBasherRework;
import net.kazi.kazimod.abilities.onirework.ViciousRoarRework;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.charactercreator.CharacterCreatorSelectionMap;
import xyz.pixelatedw.mineminenomi.api.charactercreator.RaceId;
import xyz.pixelatedw.mineminenomi.init.ModResources;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

@Mixin({WyRegistry.class})
public abstract class CartRaceReplace {
    @Shadow
    @Final
    public static DeferredRegister<RaceId> RACES;

    public CartRaceReplace() {
    }

    @Inject(
            method = {"registerRace"},
            at = {@At("HEAD")},
            remap = false,
            cancellable = true
    )
    private static <I extends RaceId> void registerEditedRace(String localizedName, Supplier<I> race, CallbackInfoReturnable<RegistryObject<I>> cir) {
        String resourceName = WyHelper.getResourceName(localizedName);
        WyRegistry.getLangMap().put("race.cartaddon." + resourceName, localizedName);
        final Supplier<I>[] modifiedRace = new Supplier[]{race};
        if (resourceName.equalsIgnoreCase("oni")) {
            modifiedRace[0] = () -> {
                if (resourceName.equalsIgnoreCase("oni")) {

                    modifiedRace[0] = () -> {
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
                return null;
            };
        }
        if (resourceName.equalsIgnoreCase("mink")) {
            modifiedRace[0] = () -> {
                if (resourceName.equalsIgnoreCase("mink")) {

                    modifiedRace[0] = () -> {
                        CharacterCreatorSelectionMap.SelectionInfo info =
                                new CharacterCreatorSelectionMap.SelectionInfo(ModResources.MINK1);

                        info.addTopAbilities(new AbilityCore[]{
                                CartEleclawAbility.INSTANCE,
                                CartElectricalShowerAbility.INSTANCE,
                                CartElectricalLunaAbility.INSTANCE,
                                CartElectricalMissileAbility.INSTANCE,
                                CartElectricalTempestaAbility.INSTANCE,
                                SulongRework.INSTANCE,
                                ElectricalBurstAbility.INSTANCE,
                                MinkSizePasssiveAbility.INSTANCE,
                        });

                        return (I) new RaceId(info, true, 4);
                    };
                }
                return null;
            };
        }
    }}