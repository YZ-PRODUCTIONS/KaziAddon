package net.kazi.kazimod.events;

import net.MrMagicalCart.cartaddon.abilities.battovampire.*;
import net.MrMagicalCart.cartaddon.abilities.goroextra.*;
import net.MrMagicalCart.cartaddon.abilities.gasuextra.ReworkedGastilleAbility;
import net.MrMagicalCart.cartaddon.abilities.gasuextra.ReworkedKarakuniAbility;
import net.MrMagicalCart.cartaddon.abilities.mochi2.KuriMochiNewAbility;
import net.MrMagicalCart.cartaddon.abilities.mochi2.MochiGinchakuNewAbility;
import net.MrMagicalCart.cartaddon.abilities.mochi2.ZanGiriMochiAbility;
import net.MrMagicalCart.cartaddon.init.CartAbilities;
import net.MrMagicalCart.cartaddon.abilities.meraextra.ReworkedFlameRushAbility;
import net.kazi.kazimod.abilities.MochiRework.KuriMochiClone;
import net.kazi.kazimod.abilities.MochiRework.MochiGinchakuClone;
import net.kazi.kazimod.abilities.MochiRework.ZanGiriMochiClone;
import net.kazi.kazimod.abilities.GoroRework.*;
import net.kazi.kazimod.abilities.GoruRework.SeriousnessAbility;
import net.kazi.kazimod.abilities.GasuRework.GastilleRework;
import net.kazi.kazimod.abilities.GasuRework.KarakuniRework;
import net.kazi.kazimod.abilities.KameRework.KameGuardPointRework;
import net.kazi.kazimod.abilities.MeraRework.DaiEnkaiRework;
import net.kazi.kazimod.abilities.ToriPhoenixRework.FlamesOfRegenerationRework;
import net.kazi.kazimod.abilities.ToriPhoenixRework.PhoenixAssaultPointRework;
import net.kazi.kazimod.abilities.ToriPhoenixRework.PhoenixFlyPointRework;
import net.kazi.kazimod.abilities.VampAwaken.AwakenedBloodStepAbility;
import net.kazi.kazimod.abilities.VampAwaken.AwakenedPhantomCloakAbility;
import net.kazi.kazimod.abilities.VampAwaken.AwakenedPhantomVeilAbility;
import net.kazi.kazimod.abilities.VampAwaken.AwakenedVampirePassiveAbility;
import net.kazi.kazimod.config.KaziConfig;
import net.kazi.kazimod.abilities.SupaRework.RealityMarbleAbility;
import net.kazi.kazimod.abilities.SupaRework.ProjectionAbility;
import net.kazi.kazimod.abilities.SupaRework.EnhancementAbility;
import net.kazi.kazimod.abilities.SupaRework.KanshouBakuyaAbility;
import net.minecraftforge.event.TickEvent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.registries.ForgeRegistries;
import xyz.pixelatedw.mineminenomi.abilities.gomu.GearFifthAbility;
import xyz.pixelatedw.mineminenomi.abilities.gomu.GomuGomuNoDawnWhipAbility;
import xyz.pixelatedw.mineminenomi.abilities.gomu.GomuGomuNoGigantAbility;
import xyz.pixelatedw.mineminenomi.abilities.goro.*;
import xyz.pixelatedw.mineminenomi.abilities.gasu.GastilleAbility;
import xyz.pixelatedw.mineminenomi.abilities.gasu.KarakuniAbility;
import xyz.pixelatedw.mineminenomi.abilities.kame.KameGuardPointAbility;
import xyz.pixelatedw.mineminenomi.abilities.mera.HeatDashAbility;
import xyz.pixelatedw.mineminenomi.abilities.mera.DaiEnkaiAbility;
import xyz.pixelatedw.mineminenomi.abilities.mera.HidarumaAbility;
import xyz.pixelatedw.mineminenomi.abilities.mera.HikenAbility;
import xyz.pixelatedw.mineminenomi.abilities.supa.AtomicRushAbility;
import xyz.pixelatedw.mineminenomi.abilities.supa.AtomicSpurtAbility;
import xyz.pixelatedw.mineminenomi.abilities.supa.SparClawAbility;
import xyz.pixelatedw.mineminenomi.abilities.supa.SparklingDaisyAbility;
import xyz.pixelatedw.mineminenomi.abilities.supa.SpiderAbility;
import xyz.pixelatedw.mineminenomi.abilities.supa.SpiralHollowAbility;
import xyz.pixelatedw.mineminenomi.abilities.toriphoenix.FlamesOfRegenerationAbility;
import xyz.pixelatedw.mineminenomi.abilities.toriphoenix.PhoenixAssaultPointAbility;
import xyz.pixelatedw.mineminenomi.abilities.toriphoenix.PhoenixFlyPointAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUnlock;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.events.ability.UnlockAbilityEvent;
import xyz.pixelatedw.mineminenomi.api.events.onefruit.EatDevilFruitEvent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit;
import xyz.pixelatedw.mineminenomi.events.abilities.AbilityValidationEvents;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;
import xyz.pixelatedw.mineminenomi.items.AkumaNoMiItem;
import xyz.pixelatedw.mineminenomi.packets.server.SSyncAbilityDataPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

import java.util.*;

@Mod.EventBusSubscriber
public class AwakeningAbilityLoginFix {

    // Vanilla abilities that have been replaced by KaziMod reworks.
    // These should never be unlocked even if they pass the fruit check,
    // because they no longer exist on any fruit after injection.
    private static final Set<AbilityCore<?>> REPLACED_ABILITIES = new HashSet<>(java.util.Arrays.asList(
            GearFifthAbility.INSTANCE,
            GomuGomuNoDawnWhipAbility.INSTANCE,
            GomuGomuNoGigantAbility.INSTANCE,
            HeatDashAbility.INSTANCE,
            HidarumaAbility.INSTANCE,
            HikenAbility.INSTANCE
    ));

    private static final Map<AbilityCore<?>, AbilityCore<?>> MERA_REPLACEMENTS = new LinkedHashMap<>();
    private static final Map<AbilityCore<?>, AbilityCore<?>> MOCHI_REPLACEMENTS = new LinkedHashMap<>();
    private static final Map<AbilityCore<?>, AbilityCore<?>> GORO_REPLACEMENTS = new LinkedHashMap<>();
    private static final Map<AbilityCore<?>, AbilityCore<?>> GASU_REPLACEMENTS = new LinkedHashMap<>();
    private static final Map<AbilityCore<?>, AbilityCore<?>> KAME_REPLACEMENTS = new LinkedHashMap<>();
    private static final Map<AbilityCore<?>, AbilityCore<?>> TORI_PHOENIX_REPLACEMENTS = new LinkedHashMap<>();

    private static final Set<AbilityCore<?>> RETIRED_GORO_ABILITIES = new HashSet<>(Arrays.asList(
            VoltageUpAbility.INSTANCE,
            ShinzoMassagePassiveAbility.INSTANCE
    ));

    // The Goro fruit owns these cores outright. Do not make their grant depend on
    // a player having an older Cart/base counterpart: players can receive a fruit
    // through commands and other paths that never create those legacy unlocks.
    private static final List<AbilityCore<?>> KAZI_GORO_ABILITIES = Arrays.asList(
            ElThorRework.INSTANCE,
            VariRework.INSTANCE,
            KariRework.INSTANCE,
            SangoRework.INSTANCE,
            RaigoRework.INSTANCE,
            VoltAmaruRework.INSTANCE,
            VoltAmaruFlightRework.INSTANCE
    );

    private static final List<AbilityCore<?>> AWAKENED_GORU_ABILITIES = Arrays.asList(
            net.kazi.kazimod.abilities.GoruRework.ShaNaqbaImuruAbility.INSTANCE,
            SeriousnessAbility.INSTANCE,
            net.kazi.kazimod.abilities.GoruRework.EaAbility.INSTANCE,
            net.kazi.kazimod.abilities.GoruRework.EnkiduAbility.INSTANCE,
            net.kazi.kazimod.abilities.GoruRework.GateOfBabylonAbility.INSTANCE
    );

    // Keep this explicit: the fruit's ability list also contains our awakening moves after injection.
    private static final Set<AbilityCore<?>> AWAKENED_GORU_REMOVALS = new HashSet<>(Arrays.asList(
            net.MrMagicalCart.cartaddon.abilities.goru.GonBombaAbility.INSTANCE,
            net.MrMagicalCart.cartaddon.abilities.goru.GoldenTouchAbility.INSTANCE,
            net.MrMagicalCart.cartaddon.abilities.goru.GoldenAgeAbility.INSTANCE,
            net.MrMagicalCart.cartaddon.abilities.goru.GoldenBodyAbility.INSTANCE,
            net.MrMagicalCart.cartaddon.abilities.goru.GoldenTesoroAbility.INSTANCE,
            net.MrMagicalCart.cartaddon.abilities.goru.GoruGoruAxeAbility.INSTANCE,
            net.MrMagicalCart.cartaddon.abilities.goru.GoldenGuardAbility.INSTANCE,
            net.MrMagicalCart.cartaddon.abilities.goru.GonFuocoDiDioAbility.INSTANCE
    ));

    private static final List<AbilityCore<?>> AWAKENED_SUPA_ABILITIES = Arrays.asList(
            RealityMarbleAbility.INSTANCE,
            ProjectionAbility.INSTANCE,
            EnhancementAbility.INSTANCE,
            KanshouBakuyaAbility.INSTANCE
    );

    // These base Supa moves are retired when the fruit awakens. Keep this list
    // explicit like Vampire's cleanup so it cannot accidentally include Kazi's
    // awakened Supa moves after fruit injection mutates the fruit ability list.
    private static final Set<AbilityCore<?>> AWAKENED_SUPA_REMOVALS = new HashSet<>(Arrays.asList(
            SparklingDaisyAbility.INSTANCE,
            SpiderAbility.INSTANCE,
            net.kazi.kazimod.abilities.SupaRework.SpiderRework.INSTANCE,
            net.kazi.kazimod.abilities.SupaRework.SparklingDaisyRework.INSTANCE,
            SparClawAbility.INSTANCE,
            AtomicSpurtAbility.INSTANCE,
            AtomicRushAbility.INSTANCE,
            SpiralHollowAbility.INSTANCE
    ));

    // These base Vampire moves are retired when the fruit awakens. Assault
    // Point's stats are replaced by Kazi's awakening-only passive.
    private static final Set<AbilityCore<?>> AWAKENED_VAMPIRE_REMOVALS = new HashSet<>(Arrays.asList(
            VampireAssaultPointAbility.INSTANCE,
            VampireFlyPointAbility.INSTANCE,
            VampireFlightAbility.INSTANCE,
            BloodBurstAbility.INSTANCE,
            LifeStealBiteAbility.INSTANCE,
            PhantomVeilAbility.INSTANCE,
            AwakenedPhantomVeilAbility.INSTANCE
    ));

    private static final Map<AbilityCore<?>, AbilityCore<?>> VAMPIRE_AWAKENING_REPLACEMENTS =
            new LinkedHashMap<>();

    // Maps each AbilityCore to which fruit(s) it belongs to
    private static Map<AbilityCore<?>, Set<AkumaNoMiItem>> abilityToFruits = null;

    /**
     * Call this after FruitAbilityInjector has finished modifying fruit ability lists
     * so the map gets rebuilt with the correct injected abilities.
     */
    public static void invalidateCache() {
        abilityToFruits = null;
    }

    // Players currently in the middle of an intentional awakening grant —
    // unlock events for these players are fully allowed through
    private static final Set<UUID> awakeningInProgress = new HashSet<>();

    static {
        MERA_REPLACEMENTS.put(HeatDashAbility.INSTANCE, ReworkedFlameRushAbility.INSTANCE);
        MERA_REPLACEMENTS.put(DaiEnkaiAbility.INSTANCE, DaiEnkaiRework.INSTANCE);
        KAME_REPLACEMENTS.put(KameGuardPointAbility.INSTANCE, KameGuardPointRework.INSTANCE);
        TORI_PHOENIX_REPLACEMENTS.put(PhoenixFlyPointAbility.INSTANCE, PhoenixFlyPointRework.INSTANCE);
        TORI_PHOENIX_REPLACEMENTS.put(PhoenixAssaultPointAbility.INSTANCE, PhoenixAssaultPointRework.INSTANCE);
        TORI_PHOENIX_REPLACEMENTS.put(FlamesOfRegenerationAbility.INSTANCE, FlamesOfRegenerationRework.INSTANCE);
        GORO_REPLACEMENTS.put(ReworkedElThorAbility.INSTANCE, ElThorRework.INSTANCE);
        GORO_REPLACEMENTS.put(ReworkedVariAbility.INSTANCE, VariRework.INSTANCE);
        GORO_REPLACEMENTS.put(ReworkedSangoAbility.INSTANCE, SangoRework.INSTANCE);
        GORO_REPLACEMENTS.put(ReworkedRaigoAbility.INSTANCE, RaigoRework.INSTANCE);
        GORO_REPLACEMENTS.put(ReworkedVoltAmaruAbility.INSTANCE, VoltAmaruRework.INSTANCE);
        GORO_REPLACEMENTS.put(
                ReworkedVoltAmaruFlightAbility.INSTANCE, VoltAmaruFlightRework.INSTANCE);
        GORO_REPLACEMENTS.put(ElThorAbility.INSTANCE, ElThorRework.INSTANCE);
        GORO_REPLACEMENTS.put(VariAbility.INSTANCE, VariRework.INSTANCE);
        GORO_REPLACEMENTS.put(KariAbility.INSTANCE, KariRework.INSTANCE);
        GORO_REPLACEMENTS.put(SangoAbility.INSTANCE, SangoRework.INSTANCE);
        GORO_REPLACEMENTS.put(RaigoAbility.INSTANCE, RaigoRework.INSTANCE);
        GORO_REPLACEMENTS.put(VoltAmaruAbility.INSTANCE, VoltAmaruRework.INSTANCE);
        GORO_REPLACEMENTS.put(
                VoltAmaruFlightAbility.INSTANCE, VoltAmaruFlightRework.INSTANCE);

        GASU_REPLACEMENTS.put(GastilleAbility.INSTANCE, GastilleRework.INSTANCE);
        GASU_REPLACEMENTS.put(ReworkedGastilleAbility.INSTANCE, GastilleRework.INSTANCE);
        GASU_REPLACEMENTS.put(KarakuniAbility.INSTANCE, KarakuniRework.INSTANCE);
        GASU_REPLACEMENTS.put(ReworkedKarakuniAbility.INSTANCE, KarakuniRework.INSTANCE);

        MOCHI_REPLACEMENTS.put(ZanGiriMochiAbility.INSTANCE, ZanGiriMochiClone.INSTANCE);
        MOCHI_REPLACEMENTS.put(KuriMochiNewAbility.INSTANCE, KuriMochiClone.INSTANCE);
        MOCHI_REPLACEMENTS.put(MochiGinchakuNewAbility.INSTANCE, MochiGinchakuClone.INSTANCE);

        VAMPIRE_AWAKENING_REPLACEMENTS.put(
                BloodStepAbility.INSTANCE, AwakenedBloodStepAbility.INSTANCE);
        VAMPIRE_AWAKENING_REPLACEMENTS.put(
                PhantomCloakAbility.INSTANCE, AwakenedPhantomCloakAbility.INSTANCE);
        VAMPIRE_AWAKENING_REPLACEMENTS.put(
                VampirePassiveAbility.INSTANCE, AwakenedVampirePassiveAbility.INSTANCE);
    }

    public static void beginAwakening(UUID playerUUID) {
        awakeningInProgress.add(playerUUID);
    }

    public static void endAwakening(UUID playerUUID) {
        awakeningInProgress.remove(playerUUID);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        restoreAwakenedFruitAbilities(event.getPlayer());
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        restoreAwakenedFruitAbilities(event.getPlayer());
    }

    @SubscribeEvent
    public void onSeriousnessEligibilityTick(TickEvent.PlayerTickEvent event) {
        PlayerEntity player = event.player;
        if (event.phase != TickEvent.Phase.END || player.level.isClientSide
                || SeriousnessAbility.isEligible(player)) return;
        IAbilityData data = AbilityDataCapability.get(player);
        if (data != null && removeSeriousnessPassive(player, data)) {
            WyNetwork.sendTo(new SSyncAbilityDataPacket(player.getId(), data), player);
        }
    }

    @SubscribeEvent
    public void onDevilFruitEaten(EatDevilFruitEvent.Post event) {
        syncGoruKaziAbilities(event.getPlayer());
        syncGoroKaziAbilities(event.getPlayer());
        syncGasuReworks(event.getPlayer());
        syncKameRework(event.getPlayer());
        syncToriPhoenixReworks(event.getPlayer());
    }

    public static void syncPlayerAwakeningReplacements(PlayerEntity player) {
        syncGoruKaziAbilities(player);
        syncAwakenedSupaAbilities(player);
        syncGoroKaziAbilities(player);
        syncGasuReworks(player);
        syncKameRework(player);
        syncToriPhoenixReworks(player);
        syncMeraReworks(player);
        syncMochiReworks(player);
        syncAwakenedVampireAbilities(player);
    }

    private static void restoreAwakenedFruitAbilities(PlayerEntity player) {
        if (player == null || player.level.isClientSide) return;
        syncGoruKaziAbilities(player);

        IDevilFruit devilFruit = DevilFruitCapability.get(player);
        boolean isAwakenedSupa = devilFruit != null
                && devilFruit.hasAwakenedFruit()
                && devilFruit.hasDevilFruit(ModAbilities.SUPA_SUPA_NO_MI);
        syncAwakenedSupaAbilities(player);
        syncGoroKaziAbilities(player);
        syncGasuReworks(player);
        syncKameRework(player);
        syncToriPhoenixReworks(player);
        syncMeraReworks(player);
        syncAwakenedVampireAbilities(player);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void syncGoroKaziAbilities(PlayerEntity player) {
        if (player == null || player.level.isClientSide
                || KaziConfig.INSTANCE.disableFruitChanges.get()) {
            return;
        }

        IDevilFruit devilFruit = DevilFruitCapability.get(player);
        if (devilFruit == null || !devilFruit.hasDevilFruit(ModAbilities.GORO_GORO_NO_MI)) return;

        IAbilityData abilityData = AbilityDataCapability.get(player);
        if (abilityData == null) return;

        // Repair Ea grants from the old shared Goro/Goru synchronization path.
        if (!devilFruit.hasDevilFruit(CartAbilities.GORU_GORU_NO_MI)) {
            for (AbilityCore<?> core : AWAKENED_GORU_ABILITIES) {
                removeProgressionAbility(player, abilityData, core);
            }
        }

        for (AbilityCore<?> retiredCore : RETIRED_GORO_ABILITIES) {
            abilityData.removeEquippedAbility(retiredCore);
            abilityData.removeUnlockedAbility(retiredCore);
        }

        List<IAbility> equipped = abilityData.getRawEquippedAbilities();
        for (Map.Entry<AbilityCore<?>, AbilityCore<?>> entry : GORO_REPLACEMENTS.entrySet()) {
            AbilityCore oldCore = entry.getKey();
            AbilityCore replacementCore = entry.getValue();
            boolean hadOld = abilityData.hasUnlockedAbility(oldCore);
            List<Integer> slotsToReplace = new ArrayList<>();

            for (int slot = 0; slot < equipped.size(); slot++) {
                IAbility equippedAbility = equipped.get(slot);
                if (equippedAbility != null && oldCore.equals(equippedAbility.getCore())) {
                    slotsToReplace.add(slot);
                }
            }

            abilityData.removeEquippedAbility(oldCore);
            abilityData.removeUnlockedAbility(oldCore);

            if (hadOld || !slotsToReplace.isEmpty()) {
                if (!abilityData.hasUnlockedAbility(replacementCore)) {
                    abilityData.addUnlockedAbility(replacementCore, AbilityUnlock.PROGRESSION);
                }
                for (int slot : slotsToReplace) {
                    abilityData.setEquippedAbility(slot, replacementCore.createAbility());
                }
            }
        }

        // Grant the full Kazi Goro set even if this player was given Goro by a
        // command or another path that skipped Cart/base ability progression.
        for (AbilityCore<?> core : KAZI_GORO_ABILITIES) {
            if (!abilityData.hasUnlockedAbility(core)) {
                abilityData.addUnlockedAbility(core, AbilityUnlock.PROGRESSION);
            }
        }

        // Re-evaluate the corrected fruit list so existing Goro users also
        // receive any base moves for which they already meet the requirements.
        AbilityValidationEvents.checkForPossibleFruitAbilities(player);
        WyNetwork.sendTo(new SSyncAbilityDataPacket(player.getId(), abilityData), player);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void syncGasuReworks(PlayerEntity player) {
        if (player == null || player.level.isClientSide
                || KaziConfig.INSTANCE.disableFruitChanges.get()) return;

        IDevilFruit devilFruit = DevilFruitCapability.get(player);
        if (devilFruit == null || !devilFruit.hasDevilFruit(ModAbilities.GASU_GASU_NO_MI)) return;

        IAbilityData abilityData = AbilityDataCapability.get(player);
        if (abilityData == null) return;

        List<IAbility> equipped = abilityData.getRawEquippedAbilities();
        for (Map.Entry<AbilityCore<?>, AbilityCore<?>> entry : GASU_REPLACEMENTS.entrySet()) {
            AbilityCore oldCore = entry.getKey();
            AbilityCore replacementCore = entry.getValue();
            boolean hadOld = abilityData.hasUnlockedAbility(oldCore);
            List<Integer> slotsToReplace = new ArrayList<>();

            for (int slot = 0; slot < equipped.size(); slot++) {
                IAbility equippedAbility = equipped.get(slot);
                if (equippedAbility != null && oldCore.equals(equippedAbility.getCore())) {
                    slotsToReplace.add(slot);
                }
            }

            abilityData.removeEquippedAbility(oldCore);
            abilityData.removeUnlockedAbility(oldCore);

            if (hadOld || !slotsToReplace.isEmpty()) {
                if (!abilityData.hasUnlockedAbility(replacementCore)) {
                    abilityData.addUnlockedAbility(replacementCore, AbilityUnlock.PROGRESSION);
                }
                for (int slot : slotsToReplace) {
                    abilityData.setEquippedAbility(slot, replacementCore.createAbility());
                }
            }
        }

        // Existing saves do not automatically re-evaluate abilities added to a
        // fruit after it was eaten. This grants Gastille/Karakuni as soon as
        // their normal Gasu progression requirements are satisfied.
        AbilityValidationEvents.checkForPossibleFruitAbilities(player);
        WyNetwork.sendTo(new SSyncAbilityDataPacket(player.getId(), abilityData), player);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void syncKameRework(PlayerEntity player) {
        if (player == null || player.level.isClientSide
                || KaziConfig.INSTANCE.disableFruitChanges.get()) return;

        IDevilFruit devilFruit = DevilFruitCapability.get(player);
        if (devilFruit == null || !devilFruit.hasDevilFruit(ModAbilities.KAME_KAME_NO_MI)) return;

        IAbilityData abilityData = AbilityDataCapability.get(player);
        if (abilityData == null) return;

        List<IAbility> equipped = abilityData.getRawEquippedAbilities();
        for (Map.Entry<AbilityCore<?>, AbilityCore<?>> entry : KAME_REPLACEMENTS.entrySet()) {
            AbilityCore oldCore = entry.getKey();
            AbilityCore replacementCore = entry.getValue();
            boolean hadOld = abilityData.hasUnlockedAbility(oldCore);
            List<Integer> slotsToReplace = new ArrayList<>();

            for (int slot = 0; slot < equipped.size(); slot++) {
                IAbility equippedAbility = equipped.get(slot);
                if (equippedAbility != null && oldCore.equals(equippedAbility.getCore())) {
                    slotsToReplace.add(slot);
                }
            }

            abilityData.removeEquippedAbility(oldCore);
            abilityData.removeUnlockedAbility(oldCore);
            if (hadOld || !slotsToReplace.isEmpty()) {
                if (!abilityData.hasUnlockedAbility(replacementCore)) {
                    abilityData.addUnlockedAbility(replacementCore, AbilityUnlock.PROGRESSION);
                }
                for (int slot : slotsToReplace) {
                    abilityData.setEquippedAbility(slot, replacementCore.createAbility());
                }
            }
        }

        AbilityValidationEvents.checkForPossibleFruitAbilities(player);
        WyNetwork.sendTo(new SSyncAbilityDataPacket(player.getId(), abilityData), player);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void syncToriPhoenixReworks(PlayerEntity player) {
        if (player == null || player.level.isClientSide
                || KaziConfig.INSTANCE.disableFruitChanges.get()) return;

        IDevilFruit devilFruit = DevilFruitCapability.get(player);
        if (devilFruit == null || !devilFruit.hasDevilFruit(ModAbilities.TORI_TORI_NO_MI_PHOENIX)) return;

        IAbilityData abilityData = AbilityDataCapability.get(player);
        if (abilityData == null) return;

        List<IAbility> equipped = abilityData.getRawEquippedAbilities();
        for (Map.Entry<AbilityCore<?>, AbilityCore<?>> entry : TORI_PHOENIX_REPLACEMENTS.entrySet()) {
            AbilityCore oldCore = entry.getKey();
            AbilityCore replacementCore = entry.getValue();
            boolean hadOldUnlocked = abilityData.hasUnlockedAbility(oldCore);
            boolean hadOldPassive = abilityData.hasPassiveAbility(oldCore);
            List<Integer> slotsToReplace = new ArrayList<>();

            for (int slot = 0; slot < equipped.size(); slot++) {
                IAbility equippedAbility = equipped.get(slot);
                if (equippedAbility != null && oldCore.equals(equippedAbility.getCore())) {
                    slotsToReplace.add(slot);
                }
            }

            abilityData.removeEquippedAbility(oldCore);
            abilityData.removeUnlockedAbility(oldCore);
            abilityData.removePassiveAbility(oldCore);

            if (hadOldUnlocked || !slotsToReplace.isEmpty()) {
                if (!abilityData.hasUnlockedAbility(replacementCore)) {
                    abilityData.addUnlockedAbility(replacementCore, AbilityUnlock.PROGRESSION);
                }
                for (int slot : slotsToReplace) {
                    abilityData.setEquippedAbility(slot, replacementCore.createAbility());
                }
            }
            if (hadOldPassive && !abilityData.hasPassiveAbility(replacementCore)) {
                abilityData.addPassiveAbility(replacementCore.createAbility());
            }
        }

        AbilityValidationEvents.checkForPossibleFruitAbilities(player);
        WyNetwork.sendTo(new SSyncAbilityDataPacket(player.getId(), abilityData), player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onUnlockAbility(UnlockAbilityEvent event) {
        if (event.getResult() == Event.Result.DENY) return;

        LivingEntity entity = event.getEntityLiving();
        AbilityCore<?> core = event.getAbilityCore();
        if (entity == null || core == null) return;

        IDevilFruit devilFruit = DevilFruitCapability.get(entity);
        if (devilFruit == null || !devilFruit.hasAwakenedFruit()) return;

        if (devilFruit.hasDevilFruit(CartAbilities.GORU_GORU_NO_MI)
                && AWAKENED_GORU_REMOVALS.contains(core)) {
            event.setResult(Event.Result.DENY);
            return;
        }

        if (devilFruit.hasDevilFruit(ModAbilities.SUPA_SUPA_NO_MI)
                && AWAKENED_SUPA_REMOVALS.contains(core)) {
            event.setResult(Event.Result.DENY);
            return;
        }

        // Awakening validation normally rechecks every ability on the fruit.
        // Keep removed Vampire moves and the originals of awakening-only
        // replacements from returning during that validation.
        if (isAwakenedVampire(devilFruit)
                && (AWAKENED_VAMPIRE_REMOVALS.contains(core)
                || VAMPIRE_AWAKENING_REPLACEMENTS.containsKey(core))) {
            event.setResult(Event.Result.DENY);
            return;
        }

        if (entity instanceof PlayerEntity && devilFruit.hasDevilFruit(ModAbilities.MERA_MERA_NO_MI)
                && !MERA_REPLACEMENTS.containsValue(core)) {
            syncMeraReworks((PlayerEntity) entity);
        }

        // Always deny explicitly replaced vanilla abilities
        if (REPLACED_ABILITIES.contains(core)) {
            event.setResult(Event.Result.DENY);
            return;
        }

        if (abilityToFruits == null) buildAbilityToFruitsMap();

        // If this ability doesn't appear in ANY fruit, it's race/style/haki — allow it
        Set<AkumaNoMiItem> fruitsWithThisAbility = abilityToFruits.get(core);
        if (fruitsWithThisAbility == null || fruitsWithThisAbility.isEmpty()) return;

        // Get the player's current fruit
        AkumaNoMiItem playerFruitItem = null;
        try {
            Item item = devilFruit.getDevilFruitItem();
            if (item instanceof AkumaNoMiItem) playerFruitItem = (AkumaNoMiItem) item;
        } catch (Exception e) { return; }
        if (playerFruitItem == null) return;

        // If the ability belongs to the player's fruit, allow it
        if (fruitsWithThisAbility.contains(playerFruitItem)) return;

        // Belongs to a different fruit — deny
        event.setResult(Event.Result.DENY);
    }

    private static void buildAbilityToFruitsMap() {
        abilityToFruits = new HashMap<>();
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            if (!(item instanceof AkumaNoMiItem)) continue;
            AkumaNoMiItem fruit = (AkumaNoMiItem) item;
            AbilityCore<?>[] abilities = fruit.getAbilities();
            if (abilities == null) continue;
            for (AbilityCore<?> ability : abilities) {
                abilityToFruits.computeIfAbsent(ability, k -> new HashSet<>()).add(fruit);
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void syncMeraReworks(PlayerEntity player) {
        if (player == null || player.level.isClientSide
                || KaziConfig.INSTANCE.disableFruitChanges.get()) return;

        IDevilFruit devilFruit = DevilFruitCapability.get(player);
        if (devilFruit == null || !devilFruit.hasDevilFruit(ModAbilities.MERA_MERA_NO_MI)) return;

        IAbilityData abilityData = AbilityDataCapability.get(player);
        if (abilityData == null) return;

        List<IAbility> equipped = abilityData.getRawEquippedAbilities();
        for (Map.Entry<AbilityCore<?>, AbilityCore<?>> entry : MERA_REPLACEMENTS.entrySet()) {
            AbilityCore oldCore = entry.getKey();
            AbilityCore replacementCore = entry.getValue();
            boolean hadUnlockedOld = abilityData.hasUnlockedAbility(oldCore);
            boolean hadUnlockedReplacement = abilityData.hasUnlockedAbility(replacementCore);
            List<Integer> slotsToReplace = new ArrayList<>();

            for (int slot = 0; slot < equipped.size(); slot++) {
                IAbility equippedAbility = equipped.get(slot);
                if (equippedAbility != null && oldCore.equals(equippedAbility.getCore())) {
                    slotsToReplace.add(slot);
                }
            }

            if (!hadUnlockedOld && !hadUnlockedReplacement && slotsToReplace.isEmpty()) {
                abilityData.addUnlockedAbility(replacementCore, AbilityUnlock.PROGRESSION);
                continue;
            }

            abilityData.removeUnlockedAbility(oldCore);
            if (!abilityData.hasUnlockedAbility(replacementCore)) {
                abilityData.addUnlockedAbility(replacementCore, AbilityUnlock.PROGRESSION);
            }

            for (int slot : slotsToReplace) {
                abilityData.setEquippedAbility(slot, replacementCore.createAbility());
            }
        }

        WyNetwork.sendTo(new SSyncAbilityDataPacket(player.getId(), abilityData), player);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void syncMochiReworks(PlayerEntity player) {
        if (player == null || player.level.isClientSide) return;

        IDevilFruit devilFruit = DevilFruitCapability.get(player);
        if (devilFruit == null || !devilFruit.hasDevilFruit(CartAbilities.MOCHI_MOCHI_NO_MI)) return;

        IAbilityData abilityData = AbilityDataCapability.get(player);
        if (abilityData == null) return;

        List<IAbility> equipped = abilityData.getRawEquippedAbilities();
        for (Map.Entry<AbilityCore<?>, AbilityCore<?>> entry : MOCHI_REPLACEMENTS.entrySet()) {
            AbilityCore oldCore = entry.getKey();
            AbilityCore replacementCore = entry.getValue();
            boolean hadOld = abilityData.hasUnlockedAbility(oldCore);
            List<Integer> slotsToReplace = new ArrayList<>();

            for (int slot = 0; slot < equipped.size(); slot++) {
                IAbility equippedAbility = equipped.get(slot);
                if (equippedAbility != null && oldCore.equals(equippedAbility.getCore())) {
                    slotsToReplace.add(slot);
                }
            }

            if (!hadOld && slotsToReplace.isEmpty()) continue;
            abilityData.removeUnlockedAbility(oldCore);
            if (!abilityData.hasUnlockedAbility(replacementCore)) {
                abilityData.addUnlockedAbility(replacementCore, AbilityUnlock.PROGRESSION);
            }
            for (int slot : slotsToReplace) {
                abilityData.setEquippedAbility(slot, replacementCore.createAbility());
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void syncAwakenedVampireAbilities(PlayerEntity player) {
        if (player == null || player.level.isClientSide) return;

        IDevilFruit devilFruit = DevilFruitCapability.get(player);
        if (!isAwakenedVampire(devilFruit)) return;

        IAbilityData abilityData = AbilityDataCapability.get(player);
        if (abilityData == null) return;

        for (AbilityCore<?> core : AWAKENED_VAMPIRE_REMOVALS) {
            abilityData.removeEquippedAbility(core);
            abilityData.removeUnlockedAbility(core);
        }

        List<IAbility> equipped = abilityData.getRawEquippedAbilities();
        for (Map.Entry<AbilityCore<?>, AbilityCore<?>> entry
                : VAMPIRE_AWAKENING_REPLACEMENTS.entrySet()) {
            AbilityCore originalCore = entry.getKey();
            AbilityCore replacementCore = entry.getValue();
            List<Integer> slotsToReplace = new ArrayList<>();

            for (int slot = 0; slot < equipped.size(); slot++) {
                IAbility equippedAbility = equipped.get(slot);
                if (equippedAbility != null && originalCore.equals(equippedAbility.getCore())) {
                    slotsToReplace.add(slot);
                }
            }

            abilityData.removeEquippedAbility(originalCore);
            abilityData.removeUnlockedAbility(originalCore);
            if (!abilityData.hasUnlockedAbility(replacementCore)) {
                abilityData.addUnlockedAbility(replacementCore, AbilityUnlock.PROGRESSION);
            }

            for (int slot : slotsToReplace) {
                abilityData.setEquippedAbility(slot, replacementCore.createAbility());
            }
        }
    }

    private static void syncGoruKaziAbilities(PlayerEntity player) {
        if (player == null || player.level.isClientSide || KaziConfig.INSTANCE.disableFruitChanges.get()) return;
        IDevilFruit fruit = DevilFruitCapability.get(player);
        IAbilityData data = AbilityDataCapability.get(player);
        if (data == null) return;
        if (fruit == null || !fruit.hasDevilFruit(CartAbilities.GORU_GORU_NO_MI)) {
            if (removeSeriousnessPassive(player, data)) {
                WyNetwork.sendTo(new SSyncAbilityDataPacket(player.getId(), data), player);
            }
            return;
        }

        // The former shared grant path gave gold users the entire lightning set.
        // Keep legitimate Goro ownership and explicit command/other unlocks intact.
        if (!fruit.hasDevilFruit(ModAbilities.GORO_GORO_NO_MI)) {
            for (AbilityCore<?> core : KAZI_GORO_ABILITIES) {
                removeProgressionAbility(player, data, core);
            }
        }

        if (fruit.hasAwakenedFruit()) {
            for (IAbility ability : new ArrayList<>(data.getEquippedAndPassiveAbilities())) {
                if (AWAKENED_GORU_REMOVALS.contains(ability.getCore())) {
                    AbilityHelper.emergencyStopAbility(player, ability);
                }
            }
            for (AbilityCore<?> core : AWAKENED_GORU_REMOVALS) {
                data.removeEquippedAbility(core);
                data.removePassiveAbility(core);
                data.removeUnlockedAbility(core);
            }
        }

        for (AbilityCore<?> core : AWAKENED_GORU_ABILITIES) {
            if (fruit.hasAwakenedFruit()) {
                if (!data.hasUnlockedAbility(core)) {
                    data.addUnlockedAbility(core, AbilityUnlock.PROGRESSION);
                }
            } else {
                removeProgressionAbility(player, data, core);
            }
        }

        AbilityCore<?> observation = net.kazi.kazimod.abilities.GoruRework.ShaNaqbaImuruAbility.INSTANCE;
        if (fruit.hasAwakenedFruit()) {
            if (data.getPassiveAbility(observation) == null) data.addPassiveAbility(observation.createAbility());
        } else data.removePassiveAbility(observation);

        if (fruit.hasAwakenedFruit()) {
            if (data.getPassiveAbility(SeriousnessAbility.INSTANCE) == null) {
                data.addPassiveAbility(SeriousnessAbility.INSTANCE.createAbility());
            }
        } else {
            removeSeriousnessPassive(player, data);
        }

        AbilityValidationEvents.checkForPossibleFruitAbilities(player);
        WyNetwork.sendTo(new SSyncAbilityDataPacket(player.getId(), data), player);
    }

    private static boolean removeSeriousnessPassive(PlayerEntity player, IAbilityData data) {
        IAbility passive = data.getPassiveAbility(SeriousnessAbility.INSTANCE);
        boolean progressionUnlock = data.getUnlockTypeForAbility(SeriousnessAbility.INSTANCE)
                == AbilityUnlock.PROGRESSION;
        if (passive == null && !progressionUnlock) return false;
        if (passive != null) {
            AbilityHelper.emergencyStopAbility(player, passive);
            data.removePassiveAbility(SeriousnessAbility.INSTANCE);
        }
        // Explicit command unlocks retain their provenance, but cannot stay active without awakened Goru.
        removeProgressionAbility(player, data, SeriousnessAbility.INSTANCE);
        return true;
    }

    private static void removeProgressionAbility(PlayerEntity player, IAbilityData data, AbilityCore<?> core) {
        if (data.getUnlockTypeForAbility(core) != AbilityUnlock.PROGRESSION) return;
        for (IAbility ability : data.getEquippedAndPassiveAbilities()) {
            if (core.equals(ability.getCore())) {
                AbilityHelper.emergencyStopAbility(player, ability);
            }
        }
        data.removeEquippedAbility(core);
        data.removeUnlockedAbility(core);
    }

    private static void syncAwakenedSupaAbilities(PlayerEntity player) {
        if (player == null || player.level.isClientSide) return;
        IDevilFruit fruit = DevilFruitCapability.get(player);
        if (fruit == null || !fruit.hasAwakenedFruit()
                || !fruit.hasDevilFruit(ModAbilities.SUPA_SUPA_NO_MI)) return;
        IAbilityData data = AbilityDataCapability.get(player);
        if (data == null) return;
        for (AbilityCore<?> core : AWAKENED_SUPA_REMOVALS) {
            data.removeEquippedAbility(core);
            data.removeUnlockedAbility(core);
        }
        for (AbilityCore<?> core : AWAKENED_SUPA_ABILITIES) {
            if (!data.hasUnlockedAbility(core)) {
                data.addUnlockedAbility(core, AbilityUnlock.PROGRESSION);
            }
        }
        if (data.getPassiveAbility(KanshouBakuyaAbility.INSTANCE) == null) {
            data.addPassiveAbility(KanshouBakuyaAbility.INSTANCE.createAbility());
        }
    }

    private static boolean isAwakenedVampire(IDevilFruit devilFruit) {
        return devilFruit != null
                && devilFruit.hasAwakenedFruit()
                && devilFruit.hasDevilFruit(CartAbilities.BATTO_BATTO_NO_MI_MODEL_VAMPIRE);
    }
}
