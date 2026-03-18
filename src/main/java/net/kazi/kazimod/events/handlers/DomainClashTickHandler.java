package net.kazi.kazimod.events.handlers;

import net.kazi.kazimod.abilities.Gojo.DomainExpansionInfiniteVoidAbility;
import net.kazi.kazimod.abilities.KamaRework.DomainExpansionMalevolentShrine;
import net.kazi.kazimod.events.DomainClashManager;
import net.kazi.kazimod.events.DomainClashManager.ClashEntry;
import net.minecraft.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "kazimod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DomainClashTickHandler {

    private static int ticker = 0;
    private static final int CHECK_RATE = 5;
    // Safety timeout: if a clash lasts longer than 30 seconds, force-end it
    private static final int MAX_CLASH_TICKS = 20 * 30;
    private static final java.util.Map<UUID, Integer> clashAge = new java.util.concurrent.ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        ticker++;
        if (ticker < CHECK_RATE) return;
        ticker = 0;

        Collection<ClashEntry> entries = DomainClashManager.getClashEntries();
        if (entries.isEmpty()) {
            clashAge.clear();
            return;
        }

        Set<UUID> processed = new HashSet<>();

        for (ClashEntry entry : entries) {
            UUID idA = entry.entity.getUUID();
            UUID idB = entry.opponent.getUUID();

            if (processed.contains(idA) || processed.contains(idB)) continue;

            // Age tracking — force-end stale clashes
            int age = clashAge.merge(idA, CHECK_RATE, Integer::sum);
            if (age >= MAX_CLASH_TICKS) {
                // Timed out — treat as a draw, clear everything
                DomainClashManager.endClash(idA, idB);
                clashAge.remove(idA);
                clashAge.remove(idB);
                processed.add(idA);
                processed.add(idB);
                resolveForEntity(entry.entity,   true);
                resolveForEntity(entry.opponent, true);
                continue;
            }

            // One or both entities are dead/gone — force cleanup
            if (!entry.entity.isAlive() || !entry.opponent.isAlive()) {
                DomainClashManager.endClash(idA, idB);
                clashAge.remove(idA);
                clashAge.remove(idB);
                processed.add(idA);
                processed.add(idB);
                continue;
            }

            boolean aLost = DomainClashManager.hasLost(idA);
            boolean bLost = DomainClashManager.hasLost(idB);

            if (!aLost && !bLost) continue; // still fighting

            processed.add(idA);
            processed.add(idB);

            DomainClashManager.endClash(idA, idB);
            clashAge.remove(idA);
            clashAge.remove(idB);

            if (aLost && bLost) {
                // Draw
                resolveForEntity(entry.entity,   true);
                resolveForEntity(entry.opponent, true);
            } else if (bLost) {
                resolveForEntity(entry.entity,   true);
                resolveForEntity(entry.opponent, false);
            } else {
                resolveForEntity(entry.entity,   false);
                resolveForEntity(entry.opponent, true);
            }
        }
    }

    private static void resolveForEntity(LivingEntity entity, boolean won) {
        IAbilityData data = AbilityDataCapability.get(entity);
        if (data == null) return;

        DomainExpansionInfiniteVoidAbility voidAbility = (DomainExpansionInfiniteVoidAbility)
                data.getEquippedAbility(DomainExpansionInfiniteVoidAbility.INSTANCE);
        if (voidAbility != null) {
            if (won) voidAbility.resolveClashWin(entity);
            else     voidAbility.resolveClashLoss(entity);
            return;
        }

        DomainExpansionMalevolentShrine shrine = (DomainExpansionMalevolentShrine)
                data.getEquippedAbility(DomainExpansionMalevolentShrine.INSTANCE);
        if (shrine != null) {
            if (won) shrine.resolveClashWin(entity);
            else     shrine.resolveClashLoss(entity);
        }
    }
}