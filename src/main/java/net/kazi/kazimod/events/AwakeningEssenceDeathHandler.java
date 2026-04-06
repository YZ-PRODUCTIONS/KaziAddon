package net.kazi.kazimod.events;

import net.kazi.kazimod.abilities.Koku.DomainExpansionInfiniteVoidAbility;
import net.kazi.kazimod.abilities.KamaRework.DomainExpansionMalevolentShrine;
import net.kazi.kazimod.entities.InfiniteVoidBarrierEntity;
import net.kazi.kazimod.items.AwakeningEssenceItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit;
import xyz.pixelatedw.mineminenomi.entities.SphereEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.events.abilities.AbilityValidationEvents;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.packets.server.SSyncDevilFruitPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

import java.util.UUID;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.Set;

public class AwakeningEssenceDeathHandler {
    private static final Set<UUID> PENDING_LOGOUT_BOSS_CLEANUP = new LinkedHashSet<>();
    private static final Set<UUID> ACTIVE_TRIAL_BOSSES = new LinkedHashSet<>();

    public static void registerTrialBoss(UUID bossUUID) {
        if (bossUUID != null) {
            ACTIVE_TRIAL_BOSSES.add(bossUUID);
        }
    }

    public static void unregisterTrialBoss(UUID bossUUID) {
        if (bossUUID != null) {
            ACTIVE_TRIAL_BOSSES.remove(bossUUID);
            PENDING_LOGOUT_BOSS_CLEANUP.remove(bossUUID);
        }
    }

    // ── Boss dies — player wins ────────────────────────────────────────────────

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        LivingEntity dying = event.getEntityLiving();

        String summonerStr = dying.getPersistentData().getString(AwakeningEssenceItem.SUMMONER_TAG);
        if (summonerStr == null || summonerStr.isEmpty()) return;

        UUID summonerUUID;
        try { summonerUUID = UUID.fromString(summonerStr); }
        catch (IllegalArgumentException e) { return; }

        LivingEntity killer = null;
        if (event.getSource().getEntity() instanceof LivingEntity)
            killer = (LivingEntity) event.getSource().getEntity();
        if (killer == null || !killer.getUUID().equals(summonerUUID)) return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerPlayerEntity player = server.getPlayerList().getPlayer(summonerUUID);
        if (player == null) return;

        cleanupArena(dying, server);
        unregisterTrialBoss(dying.getUUID());
        player.getPersistentData().remove(AwakeningEssenceItem.ACTIVE_TRIAL_BOSS_TAG);

        IDevilFruit devilFruit = DevilFruitCapability.get(player);
        if (!devilFruit.hasAwakenedFruit()) {
            devilFruit.setAwakenedFruit(true);

            AwakeningAbilityLoginFix.beginAwakening(player.getUUID());
            try {
                AbilityValidationEvents.checkForPossibleFruitAbilities(player);
            } finally {
                AwakeningAbilityLoginFix.endAwakening(player.getUUID());
            }

            WyNetwork.sendTo(new SSyncDevilFruitPacket(player.getId(), devilFruit), player);
            player.sendMessage(new StringTextComponent(
                            "\u00a76\u00a7l\u2605 \u00a7eYour fruit has been awakened! \u00a76\u00a7l\u2605"),
                    player.getUUID());

            // ── Server-wide awakening announcement ────────────────────────────
            java.util.Optional<?> fruit = devilFruit.getDevilFruit();
            String fruitId = fruit.isPresent() ? fruit.get().toString() : "";
            String announcement;
            if (fruitId.contains("koku_koku_no_mi")) {
                announcement = "\u00a7d\u00a7l\u2605 \u00a7fThe Honored One has awakened. \u00a7d\u00a7l\u2605";
            } else if (fruitId.contains("kama_kama_no_mi")) {
                announcement = "\u00a74\u00a7l\u2605 \u00a7cThe King of Curses has Awakened. \u00a74\u00a7l\u2605";
            } else if (fruitId.contains("gomu_gomu_no_mi")) {
                announcement = "\u00a7e\u00a7l\u2605 \u00a7fJoyboy returns after 800 years. \u00a7e\u00a7l\u2605";
            } else if (fruitId.contains("ope_ope_no_mi")) {
                announcement = "\u00a7b\u00a7l\u2605 \u00a7fThe Surgeon of Death has awakened. \u00a7b\u00a7l\u2605";
            } else if (fruitId.contains("bomu_bomu_no_mi")) {
                announcement = "\u00a7c\u00a7l\u2605 \u00a76A devastating awakening ignites the battlefield. \u00a7c\u00a7l\u2605";
            } else {
                announcement = "\u00a76\u00a7l\u2605 \u00a7e" + player.getName().getString()
                        + " has awakened their Devil Fruit! \u00a76\u00a7l\u2605";
            }
            final String msg = announcement;
            server.getPlayerList().getPlayers().forEach(p ->
                    p.sendMessage(new StringTextComponent(msg), p.getUUID()));

            // ── Consume item only on success ──────────────────────────────────
            AwakeningEssenceItem.consumeFromInventory(player);
        }
    }

    // ── Player dies — trial failed ─────────────────────────────────────────────
    // Item is NOT consumed. Player keeps it and can try again.

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity player = (ServerPlayerEntity) event.getEntityLiving();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        UUID playerUUID = player.getUUID();

        for (ServerWorld world : server.getAllLevels()) {
            for (LivingEntity boss : world.getEntitiesOfClass(
                    LivingEntity.class, player.getBoundingBox().inflate(200))) {

                String tag = boss.getPersistentData().getString(AwakeningEssenceItem.SUMMONER_TAG);
                if (tag == null || tag.isEmpty()) continue;
                try { if (!playerUUID.equals(UUID.fromString(tag))) continue; }
                catch (IllegalArgumentException e) { continue; }

                IAbilityData abilityData = AbilityDataCapability.get(boss);
                if (abilityData != null) stopDomainIfActive(abilityData, boss);

                cleanupArena(boss, server);
                boss.remove();

                // Item kept — player can retry
                player.sendMessage(new StringTextComponent(
                                "\u00a7cThe trial has ended. You were not strong enough..."),
                        player.getUUID());
                return;
            }
        }
    }

    // ── Fatal hit protection — boss direct hit / projectile ───────────────────
    // Cancels the killing blow, cleans up, removes boss. Item NOT consumed.

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntityLiving();
        if (!(victim instanceof ServerPlayerEntity)) return;
        if (victim.getHealth() - event.getAmount() > 0) return;

        ServerPlayerEntity player = (ServerPlayerEntity) victim;
        Entity sourceEntity = event.getSource().getEntity();

        LivingEntity boss = null;
        if (isBossWith(sourceEntity, player.getUUID())) {
            boss = (LivingEntity) sourceEntity;
        } else if (sourceEntity instanceof AbilityProjectileEntity) {
            LivingEntity thrower = ((AbilityProjectileEntity) sourceEntity).getThrower();
            if (isBossWith(thrower, player.getUUID())) boss = thrower;
        }
        if (boss == null) return;

        event.setCanceled(true);
        player.setHealth(player.getMaxHealth());

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        cleanupArena(boss, server);
        boss.remove();
        unregisterTrialBoss(boss.getUUID());
        player.getPersistentData().remove(AwakeningEssenceItem.ACTIVE_TRIAL_BOSS_TAG);

        // Item kept — player can retry
        player.sendMessage(new StringTextComponent(
                        "\u00a7cThe trial has ended. You were not strong enough..."),
                player.getUUID());
    }

    // ── Player disconnects mid-fight ──────────────────────────────────────────
    // Item NOT consumed — player keeps it.

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();

        String bossUuidStr = player.getPersistentData().getString(AwakeningEssenceItem.ACTIVE_TRIAL_BOSS_TAG);
        if (bossUuidStr == null || bossUuidStr.isEmpty()) {
            return;
        }

        UUID bossUUID;
        try {
            bossUUID = UUID.fromString(bossUuidStr);
        } catch (IllegalArgumentException e) {
            player.getPersistentData().remove(AwakeningEssenceItem.ACTIVE_TRIAL_BOSS_TAG);
            return;
        }

        player.getPersistentData().remove(AwakeningEssenceItem.ACTIVE_TRIAL_BOSS_TAG);
        PENDING_LOGOUT_BOSS_CLEANUP.add(bossUUID);
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isClientSide) {
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || PENDING_LOGOUT_BOSS_CLEANUP.isEmpty()) {
            return;
        }

        java.util.List<UUID> pendingBosses = new java.util.ArrayList<>(PENDING_LOGOUT_BOSS_CLEANUP);
        PENDING_LOGOUT_BOSS_CLEANUP.clear();
        for (UUID bossUUID : pendingBosses) {

            for (ServerWorld world : server.getAllLevels()) {
                Entity entity = world.getEntity(bossUUID);
                if (!(entity instanceof LivingEntity)) {
                    continue;
                }

                LivingEntity boss = (LivingEntity) entity;
                cleanupArena(boss, server);
                boss.remove();
                break;
            }
            ACTIVE_TRIAL_BOSSES.remove(bossUUID);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isBossWith(Entity entity, UUID playerUUID) {
        if (!(entity instanceof LivingEntity)) return false;
        String tag = entity.getPersistentData().getString(AwakeningEssenceItem.SUMMONER_TAG);
        if (tag == null || tag.isEmpty()) return false;
        try { return playerUUID.equals(UUID.fromString(tag)); }
        catch (IllegalArgumentException e) { return false; }
    }

    private void stopDomainIfActive(IAbilityData abilityData, LivingEntity boss) {
        DomainExpansionInfiniteVoidAbility gojoDomain =
                (DomainExpansionInfiniteVoidAbility) abilityData.getEquippedAbility(
                        DomainExpansionInfiniteVoidAbility.INSTANCE);
        if (gojoDomain != null && gojoDomain.isDomainActive()) {
            gojoDomain.getComponent(ModAbilityKeys.CONTINUOUS)
                    .filter(c -> c instanceof ContinuousComponent)
                    .ifPresent(c -> ((ContinuousComponent) c).stopContinuity(boss));
        }

        DomainExpansionMalevolentShrine sukunaDomain =
                (DomainExpansionMalevolentShrine) abilityData.getEquippedAbility(
                        DomainExpansionMalevolentShrine.INSTANCE);
        if (sukunaDomain != null && sukunaDomain.isDomainActive()) {
            sukunaDomain.getComponent(ModAbilityKeys.CONTINUOUS)
                    .filter(c -> c instanceof ContinuousComponent)
                    .ifPresent(c -> ((ContinuousComponent) c).stopContinuity(boss));
        }
    }

    private static void cleanupArena(LivingEntity boss, MinecraftServer server) {
        if (boss == null || server == null) return;
        removeEntityByUUID(boss.getPersistentData()
                .getString(AwakeningEssenceItem.BARRIER_TAG), InfiniteVoidBarrierEntity.class, server);
        removeEntityByUUID(boss.getPersistentData()
                .getString(AwakeningEssenceItem.SPHERE_TAG), SphereEntity.class, server);
    }

    public static void cleanupAllAwakeningTrials(MinecraftServer server) {
        if (server == null) return;

        java.util.List<UUID> bossIds = new java.util.ArrayList<>(ACTIVE_TRIAL_BOSSES);
        for (UUID bossUUID : bossIds) {
            for (ServerWorld world : server.getAllLevels()) {
                Entity entity = world.getEntity(bossUUID);
                if (!(entity instanceof LivingEntity)) {
                    continue;
                }
                LivingEntity boss = (LivingEntity) entity;
                cleanupArena(boss, server);
                boss.remove();
                break;
            }
            unregisterTrialBoss(bossUUID);
        }
    }

    private static void removeEntityByUUID(String uuidStr, Class<? extends Entity> type,
                                    MinecraftServer server) {
        if (server == null) return;
        if (uuidStr == null || uuidStr.isEmpty()) return;
        UUID uuid;
        try { uuid = UUID.fromString(uuidStr); }
        catch (IllegalArgumentException e) { return; }
        for (ServerWorld world : server.getAllLevels()) {
            Entity entity = world.getEntity(uuid);
            if (entity != null && type.isInstance(entity)) {
                entity.remove();
                return;
            }
        }
    }
}
