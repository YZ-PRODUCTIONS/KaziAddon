package net.kazi.kazimod.mixin;

import java.util.function.Supplier;

import net.MrMagicalCart.cartaddon.abilities.blacklegextra.*;
import net.MrMagicalCart.cartaddon.abilities.brawlerextra.*;
import net.MrMagicalCart.cartaddon.abilities.nitoryu.*;
import net.MrMagicalCart.cartaddon.abilities.ryusoken.*;
import net.MrMagicalCart.cartaddon.abilities.saber.CircleParryAbility;
import net.MrMagicalCart.cartaddon.abilities.saber.DawnbreakerDashAbility;
import net.MrMagicalCart.cartaddon.abilities.saber.FinalResortAbility;
import net.MrMagicalCart.cartaddon.abilities.saber.HiNoKagutsuchiNoEisuAbility;
import net.MrMagicalCart.cartaddon.abilities.saber.NewDivineDepartureAbility;
import net.MrMagicalCart.cartaddon.abilities.saber.SpiderLilySliceAbility;
import net.MrMagicalCart.cartaddon.abilities.saber.WildFuryAbility;
import net.MrMagicalCart.cartaddon.abilities.saber.WildGambitAbility;
import net.MrMagicalCart.cartaddon.abilities.trident.RapidRushAbility;
import net.MrMagicalCart.cartaddon.abilities.trident.SpinStanceAbility;
import net.MrMagicalCart.cartaddon.abilities.trident.WideSlashAbility;
import net.MrMagicalCart.cartaddon.init.CartFightingStyles;
import net.MrMagicalCart.cartaddon.init.CartResources;
import net.kazi.kazimod.abilities.AxeStyleRework.*;
import net.kazi.kazimod.abilities.BlacklegRework.AntiMatterKickCourseRework;
import net.kazi.kazimod.abilities.BlacklegRework.PartyTableKickCourseRework;
import net.kazi.kazimod.abilities.BrawlerRework.*;
import net.kazi.kazimod.abilities.DoctorRework.*;
import net.kazi.kazimod.abilities.NitoryuRework.NitoryuIaiRashomonRework;
import net.kazi.kazimod.abilities.NitoryuRework.SaiKuruRework;
import net.kazi.kazimod.abilities.NitoryuRework.TakaNamiRework;
import net.kazi.kazimod.abilities.RyusokenRework.DragonWhirlwindAbility;
import net.kazi.kazimod.abilities.RyusokenRework.RyuNoIbukiRework;
import net.kazi.kazimod.abilities.RyusokenRework.RyuNoKagizumeRework;
import net.kazi.kazimod.abilities.RyusokenRework.TalonRushRework;
import net.kazi.kazimod.abilities.SaberRework.CircleParryRework;
import net.kazi.kazimod.abilities.SaberRework.DawnbreakerDashRework;
import net.kazi.kazimod.abilities.SaberRework.HeavenSlash;
import net.kazi.kazimod.abilities.SpearRework.AbsolutePierceRework;
import net.kazi.kazimod.abilities.SpearRework.DrillJabRework;
import net.kazi.kazimod.abilities.SpearRework.SkySplitterDescentRework;
import net.kazi.kazimod.abilities.SpearRework.VaultRework;
import net.kazi.kazimod.abilities.swordsmanrework.*;

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

import java.lang.reflect.Field;

@Mixin(value = WyRegistry.class, remap = false)
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
                        ShiShishiSonsonRework.INSTANCE,
                        YakkodoriRework.INSTANCE,
                        SanbyakurokujoPoundHoRework.INSTANCE,
                        OTatsumakiRework.INSTANCE,
                        HiryuKaenRework.INSTANCE,
                        RadiantSliceAbility.INSTANCE,
                        FoxfireStyleAbility.INSTANCE
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
                        ReversalRework.INSTANCE,
                        SkySplitterRework.INSTANCE,
                        TyrantCleaveRework.INSTANCE,
                        BerserkRework.INSTANCE,
                        FutenrakuRework.INSTANCE,
                        PredatorsThrowRework.INSTANCE,
                        YasotakeruRework.INSTANCE
                });

                info.addBottomAbilities(new AbilityCore[0]);

                I val = (I) new StyleId(info, true, 14);
                return val;
            };
        }

        if (resourceName.equalsIgnoreCase("brawler")) {
            modifiedStyle = () -> {
                CharacterCreatorSelectionMap.SelectionInfo info =
                        new CharacterCreatorSelectionMap.SelectionInfo(ModResources.BRAWLER);

                info.addTopAbilities(new AbilityCore[]{
                        BlueHoleAbility.INSTANCE,
                        FistsOfLoveBarrageRework.INSTANCE,
                        GalaxyImpactRework.INSTANCE,
                        QueenPunchAbility.INSTANCE,
                        ReworkedGenkotsuMeteorAbility.INSTANCE,
                        ReworkedHakaiHoAbility.INSTANCE,
                        JinshinHoRework.INSTANCE,
                        SpinningBrawlRework.INSTANCE,
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
                        AntiMatterKickCourseRework.INSTANCE,
                        CartBienCuitGrillShotAbility.INSTANCE,
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

        if (resourceName.equalsIgnoreCase("ryusoken")) {
            modifiedStyle = () -> {
                CharacterCreatorSelectionMap.SelectionInfo info =
                        new CharacterCreatorSelectionMap.SelectionInfo(CartResources.RYUSOKEN);

                info.addTopAbilities(new AbilityCore[]{
                        RyuNoKagizumeRework.INSTANCE,
                        RyuNoIbukiRework.INSTANCE,
                        DragonsLawnMowerAbility.INSTANCE,
                        PenetratingClawsAbility.INSTANCE,
                        PreciseTalonStrikesAbility.INSTANCE,
                        TalonRushRework.INSTANCE,
                        DragonWhirlwindAbility.INSTANCE
                });

                info.addBottomAbilities(new AbilityCore[0]);

                I val = (I) new StyleId(info, true, 8);
                return val;
            };
        }

        if (resourceName.equalsIgnoreCase("doctor")) {
            modifiedStyle = () -> {
                CharacterCreatorSelectionMap.SelectionInfo info =
                        new CharacterCreatorSelectionMap.SelectionInfo(ModResources.DOCTOR);

                info.addTopAbilities(new AbilityCore[]{
                        AntidoteShotRework.INSTANCE,
                        DopingRework.INSTANCE,
                        FailedExperimentRework.INSTANCE,
                        FirstAidRework.INSTANCE,
                        MedicBagExplosionRework.INSTANCE,
                        VirusZoneRework.INSTANCE

                });



                I val = (I) new StyleId(info, true, 3);
                return val;
            };
        }

        if (resourceName.equalsIgnoreCase("saber")) {
            modifiedStyle = () -> {
                CharacterCreatorSelectionMap.SelectionInfo info =
                        new CharacterCreatorSelectionMap.SelectionInfo(CartResources.SABER);

                info.addTopAbilities(new AbilityCore[]{
                        CircleParryRework.INSTANCE,
                        DawnbreakerDashRework.INSTANCE,
                        FinalResortAbility.INSTANCE,
                        HeavenSlash.INSTANCE,
                        HiNoKagutsuchiNoEisuAbility.INSTANCE,
                        NewDivineDepartureAbility.INSTANCE,
                        SpiderLilySliceAbility.INSTANCE,
                        WildFuryAbility.INSTANCE,
                        WildGambitAbility.INSTANCE
                });

                AbilityCore<?> perk = getPrivateAbilityCore(CartFightingStyles.class, "SABER_ATTACK_PERK");
                info.addBottomAbilities(perk == null ? new AbilityCore[0] : new AbilityCore[]{perk});

                return (I) new StyleId(info, true, 15);
            };
        }

        if (resourceName.equalsIgnoreCase("trident")) {
            modifiedStyle = () -> {
                CharacterCreatorSelectionMap.SelectionInfo info =
                        new CharacterCreatorSelectionMap.SelectionInfo(CartResources.SPEAR);

                info.addTopAbilities(new AbilityCore[]{
                        AbsolutePierceRework.INSTANCE,
                        DrillJabRework.INSTANCE,
                        SkySplitterDescentRework.INSTANCE,
                        VaultRework.INSTANCE,
                        RapidRushAbility.INSTANCE,
                        SpinStanceAbility.INSTANCE,
                        WideSlashAbility.INSTANCE

                });

                info.addBottomAbilities(new AbilityCore[0]);

                I val = (I) new StyleId(info, true, 17);
                return val;
            };
        }

        RegistryObject<I> reg = STYLES.register(resourceName, modifiedStyle);
        cir.setReturnValue(reg);
    }

    private static AbilityCore<?> getPrivateAbilityCore(Class<?> owner, String fieldName) {
        try {
            Field field = owner.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(null);
            return value instanceof AbilityCore ? (AbilityCore<?>) value : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
