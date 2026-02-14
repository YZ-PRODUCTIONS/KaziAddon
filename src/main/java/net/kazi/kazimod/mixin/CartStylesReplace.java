package net.kazi.kazimod.mixin;

import net.MrMagicalCart.cartaddon.abilities.nitoryu.*;
import net.MrMagicalCart.cartaddon.abilities.swordsmenextra.ReworkedOTatsumakiAbility;
import net.MrMagicalCart.cartaddon.abilities.swordsmenextra.ReworkedShiShishiSonsonAbility;
import net.MrMagicalCart.cartaddon.init.CartResources;
import net.kazi.kazimod.abilities.NitoryuRework.NitoryuIaiRashomonRework;
import net.kazi.kazimod.abilities.swordsmanrework.HiryuKaenRework;
import net.kazi.kazimod.abilities.swordsmanrework.RadiantSliceAbility;
import net.kazi.kazimod.abilities.swordsmanrework.SanbyakurokujoPoundHoRework;
import net.kazi.kazimod.abilities.swordsmanrework.YakkodoriRework;
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
import xyz.pixelatedw.mineminenomi.api.charactercreator.StyleId;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

import java.util.function.Supplier;

@Mixin(WyRegistry.class)
public abstract class CartStylesReplace {

    @Shadow
    @Final
    public static DeferredRegister<StyleId> STYLES;
    private static Object modifiedRace;

    @Inject(
            method = "registerStyle",

            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )

    private static <I extends StyleId> void registerEditedStyle(
            String localizedName,
            Supplier<I> style,
            CallbackInfoReturnable<RegistryObject<I>> cir
    ) {

        String resourceName = WyHelper.getResourceName(localizedName);

        // ===== SWORDSMAN REPLACEMENT =====
        if (resourceName.equalsIgnoreCase("swordsman")) {

            Supplier<I> modifiedStyle = () -> {
                CharacterCreatorSelectionMap.SelectionInfo info =
                        new CharacterCreatorSelectionMap.SelectionInfo(CartResources.ITTORYU);

                info.addTopAbilities(new AbilityCore[]{
                        ReworkedShiShishiSonsonAbility.INSTANCE,
                        YakkodoriRework.INSTANCE,
                        SanbyakurokujoPoundHoRework.INSTANCE,
                        ReworkedOTatsumakiAbility.INSTANCE,
                        HiryuKaenRework.INSTANCE,
                        RadiantSliceAbility.INSTANCE,
                });

                info.addBottomAbilities(new AbilityCore[]{
                        CharacterCreatorSelectionMap.SWORDSMAN_DAMAGE_PERK
                });

                return (I) new StyleId(info, true, 1);
            };

            RegistryObject<I> reg = STYLES.register(resourceName, modifiedStyle);
            cir.setReturnValue(reg);
            return; // VERY IMPORTANT
        }

        // ===== NITORYU REPLACEMENT =====
        if (resourceName.equalsIgnoreCase("nitoryu")) {

            Supplier<I> modifiedStyle = () -> {
                CharacterCreatorSelectionMap.SelectionInfo info =
                        new CharacterCreatorSelectionMap.SelectionInfo(CartResources.NITORYU);

                info.addTopAbilities(new AbilityCore[]{
                        NitoryuIaiRashomonRework.INSTANCE,
                        MagumaAbility.INSTANCE,
                        MixedBumakiAbility.INSTANCE,
                        NanahyakunijuPoundHoAbility.INSTANCE,
                        NitoryuCounterStrikeAbility.INSTANCE,
                        ParadiseTotsukaAbility.INSTANCE,
                        SaiKuruAbility.INSTANCE,
                        TakaNamiAbility.INSTANCE,


                });
                info.addBottomAbilities(new AbilityCore[]{});

                return (I) new StyleId(info, true, 8);
            };

            RegistryObject<I> reg = STYLES.register(resourceName, modifiedStyle);
            cir.setReturnValue(reg);
            return;
        }
    }}
