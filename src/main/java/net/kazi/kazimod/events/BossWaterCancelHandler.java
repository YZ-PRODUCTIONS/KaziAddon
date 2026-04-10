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
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.entities.SphereEntity;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;

import java.util.UUID;

/**
 * Cancels the awakening trial if the boss enters water.
 *
 * Listens on LivingUpdateEvent (fires every entity tick server-side).
 * If the boss has a SUMMONER_TAG and is touching water, the trial is
 * cleaned up, the boss removed, and the summoner notified.
 * The item is NOT consumed — the player keeps it and can retry.
 */
public class BossWaterCancelHandler {

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        LivingEntity entity = event.getEntityLiving();

        // Only run server-side
        if (entity.level.isClientSide) return;

        // Must have a summoner tag
        String summonerStr = entity.getPersistentData().getString(AwakeningEssenceItem.SUMMONER_TAG);
        if (summonerStr == null || summonerStr.isEmpty()) return;

        // Must be in water (isInWater covers feet-in-water AND swimming)
        if (!entity.isInWater()) return;

        UUID summonerUUID;
        try { summonerUUID = UUID.fromString(summonerStr); }
        catch (IllegalArgumentException e) { return; }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        ServerPlayerEntity player = server.getPlayerList().getPlayer(summonerUUID);

        // Stop any active domain first so cleanup() runs and removes sphere/barrier
        IAbilityData abilityData = AbilityDataCapability.get(entity);
        if (abilityData != null) {
            stopDomainIfActive(abilityData, entity);
        }

        // Remove arena entities
        cleanupArena(entity, server);

        // Remove the boss
        entity.remove();
        AwakeningEssenceDeathHandler.unregisterTrialBoss(entity.getUUID());
        if (player != null) {
            player.getPersistentData().remove(AwakeningEssenceItem.ACTIVE_TRIAL_BOSS_TAG);
        }

        // Notify the summoner — item is kept
        if (player != null) {
            player.sendMessage(new StringTextComponent(
                            "\u00a7cThe trial has been cancelled \u2014 the boss entered water."),
                    player.getUUID());
        }
    }

    // ── Helpers (mirrors AwakeningEssenceDeathHandler) ────────────────────────

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

    private void cleanupArena(LivingEntity boss, MinecraftServer server) {
        removeEntityByUUID(boss.getPersistentData()
                .getString(AwakeningEssenceItem.BARRIER_TAG), InfiniteVoidBarrierEntity.class, server);
        removeEntityByUUID(boss.getPersistentData()
                .getString(AwakeningEssenceItem.SPHERE_TAG), SphereEntity.class, server);
    }

    private void removeEntityByUUID(String uuidStr, Class<? extends Entity> type,
                                    MinecraftServer server) {
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
