package net.kazi.kazimod.mixin;

import java.util.function.Supplier;

import net.MrMagicalCart.cartaddon.abilities.axestyle.*;
import net.MrMagicalCart.cartaddon.abilities.blacklegextra.*;
import net.MrMagicalCart.cartaddon.abilities.brawlerextra.*;
import net.MrMagicalCart.cartaddon.abilities.nitoryu.*;
import net.MrMagicalCart.cartaddon.abilities.swordsmenextra.ReworkedOTatsumakiAbility;
import net.MrMagicalCart.cartaddon.abilities.swordsmenextra.ReworkedShiShishiSonsonAbility;
import net.MrMagicalCart.cartaddon.init.CartResources;
import net.kazi.kazimod.abilities.AxeStyleRework.MountainEaterRework;
import net.kazi.kazimod.abilities.AxeStyleRework.ReversalRework;
import net.kazi.kazimod.abilities.AxeStyleRework.SkySplitterRework;
import net.kazi.kazimod.abilities.AxeStyleRework.TyrantCleaveRework;
import net.kazi.kazimod.abilities.BlacklegRework.BienCultGrillShotRework;
import net.kazi.kazimod.abilities.BlacklegRework.PartyTableKickCourseRework;
import net.kazi.kazimod.abilities.NitoryuRework.NitoryuIaiRashomonRework;
import net.kazi.kazimod.abilities.NitoryuRework.SaiKuruRework;
import net.kazi.kazimod.abilities.NitoryuRework.TakaNamiRework;
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
import xyz.pixelatedw.mineminenomi.init.ModResources;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

@Mixin({WyRegistry.class})
public abstract class CartStylesReplace {

    @Shadow
    @Final
    public static DeferredRegister<StyleId> STYLES;

    @Inject(
            method = {"registerStyle"},
            at = {@At("HEAD")},
            remap = false,
            cancellable = true
    )
    private static <I extends StyleId> void registerEditedStyle(String localizedName, Supplier<I> style, CallbackInfoReturnable<RegistryObject<I>> cir) {

        String resourceName = WyHelper.getResourceName(localizedName);
        WyRegistry.getLangMap().put("style.mineminenomi." + resourceName, localizedName);

        Supplier<I> modifiedStyle = style;

        if (resourceName.equalsIgnoreCase("swordsman")) {
            modifiedStyle = () -> {
                CharacterCreatorSelectionMap.SelectionInfo info =
                        new CharacterCreatorSelectionMap.SelectionInfo(CartResources.ITTORYU);

                info.addTopAbilities(new AbilityCore[]{
                        ReworkedShiShishiSonsonAbility.INSTANCE,
                        YakkodoriRework.INSTANCE,
                        SanbyakurokujoPoundHoRework.INSTANCE,
                        ReworkedOTatsumakiAbility.INSTANCE,
                        HiryuKaenRework.INSTANCE,
                        RadiantSliceAbility.INSTANCE
                });

                info.addBottomAbilities(new AbilityCore[]{
                        CharacterCreatorSelectionMap.SWORDSMAN_DAMAGE_PERK
                });

                I val = (I) new StyleId(info, true, 1);
                return val;
            };
        }

        if (resourceName.equalsIgnoreCase("nitoryu")) {
            modifiedStyle = () -> {
                CharacterCreatorSelectionMap.SelectionInfo info =
                        new CharacterCreatorSelectionMap.SelectionInfo(CartResources.NITORYU);

                info.addTopAbilities(new AbilityCore[]{
                        MagumaAbility.INSTANCE,
                        MixedBumakiAbility.INSTANCE,
                        NanahyakunijuPoundHoAbility.INSTANCE,
                        NitoryuCounterStrikeAbility.INSTANCE,
                        ParadiseTotsukaAbility.INSTANCE,
                        SaiKuruRework.INSTANCE,
                        TakaNamiRework.INSTANCE,
                        NitoryuIaiRashomonRework.INSTANCE
                });

                info.addBottomAbilities(new AbilityCore[0]);

                I val = (I) new StyleId(info, true, 8);
                return val;
            };
        }

        if (resourceName.equalsIgnoreCase("axestyle")) {
            modifiedStyle = () -> {
                CharacterCreatorSelectionMap.SelectionInfo info =
                        new CharacterCreatorSelectionMap.SelectionInfo(CartResources.DOUBLE_AXE);

                info.addTopAbilities(new AbilityCore[]{
                        MountainEaterRework.INSTANCE,
                        BerserkAbility.INSTANCE,
                        FutenrakuAbility.INSTANCE,
                        PredatorsThrowAbility.INSTANCE,
                        ReversalRework.INSTANCE,
                        SkySplitterRework.INSTANCE,
                        TyrantCleaveRework.INSTANCE,
                        YasotakeruAbility.INSTANCE,

                });

                info.addBottomAbilities(new AbilityCore[0]);

                I val = (I) new StyleId(info, true, 15);
                return val;
            };
        }

        if (resourceName.equalsIgnoreCase("brawler")) {
            modifiedStyle = () -> {
                CharacterCreatorSelectionMap.SelectionInfo info =
                        new CharacterCreatorSelectionMap.SelectionInfo(ModResources.BRAWLER);

                info.addTopAbilities(new AbilityCore[]{
                        BlueHoleAbility.INSTANCE,
                        FistsOfLoveBarrageAbility.INSTANCE,
                        GalaxyImpactAbility.INSTANCE,
                        QueenPunchAbility.INSTANCE,
                        ReworkedGenkotsuMeteorAbility.INSTANCE,
                        ReworkedHakaiHoAbility.INSTANCE,
                        ReworkedJishinHoAbility.INSTANCE,
                        ReworkedSpinningBrawlAbility.INSTANCE,
                        ReworkedSuplexAbility.INSTANCE

                });

                info.addBottomAbilities(new AbilityCore[0]);

                I val = (I) new StyleId(info, true, 5);
                return val;
            };
        }

        if (resourceName.equalsIgnoreCase("blackleg")) {
            modifiedStyle = () -> {
                CharacterCreatorSelectionMap.SelectionInfo info =
                        new CharacterCreatorSelectionMap.SelectionInfo(ModResources.BLACK_LEG);

                info.addTopAbilities(new AbilityCore[]{
                        BeoufBurstAbility.INSTANCE,
                        CartAntiMannerKickCourseAbility.INSTANCE,
                        BienCultGrillShotRework.INSTANCE,
                        CartConcasseAbility.INSTANCE,
                        CartDiableJambeAbility.INSTANCE,
                        CartExtraHachisAbility.INSTANCE,
                        PartyTableKickCourseRework.INSTANCE,
                        CartSkywalkAbility.INSTANCE

                });

                info.addBottomAbilities(new AbilityCore[0]);

                I val = (I) new StyleId(info, true, 6);
                return val;
            };
        }

        RegistryObject<I> reg = STYLES.register(resourceName, modifiedStyle);
        cir.setReturnValue(reg);
    }
}