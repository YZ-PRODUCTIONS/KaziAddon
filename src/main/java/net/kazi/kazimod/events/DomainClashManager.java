package net.kazi.kazimod.events;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "kazimod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DomainClashManager {

    public static class ClashEntry {
        public final LivingEntity entity;
        public final LivingEntity opponent;
        public float damageTaken = 0.0F;
        public final float threshold;
        public boolean lost = false;

        public ClashEntry(LivingEntity entity, LivingEntity opponent) {
            this.entity    = entity;
            this.opponent  = opponent;
            this.threshold = entity.getMaxHealth() * 0.20F;
        }
    }

    private static final Map<UUID, ClashEntry> clashes = new ConcurrentHashMap<>();

    public static boolean isInClash(UUID id) {
        ClashEntry entry = clashes.get(id);
        if (entry == null) return false;
        // Auto-clear stale entries where either side is dead or gone
        if (!entry.entity.isAlive() || !entry.opponent.isAlive()) {
            clashes.remove(entry.entity.getUUID());
            clashes.remove(entry.opponent.getUUID());
            return false;
        }
        return true;
    }

    public static Collection<ClashEntry> getClashEntries() {
        return new ArrayList<>(clashes.values());
    }

    public static void startClash(LivingEntity e1, LivingEntity e2) {
        UUID id1 = e1.getUUID();
        UUID id2 = e2.getUUID();
        clashes.put(id1, new ClashEntry(e1, e2));
        clashes.put(id2, new ClashEntry(e2, e1));
        sendMessage(e1, TextFormatting.YELLOW + "Domain Clash Initiated!");
        sendMessage(e2, TextFormatting.YELLOW + "Domain Clash Initiated!");
    }

    public static ClashEntry getEntry(UUID id) {
        return clashes.get(id);
    }

    public static boolean hasLost(UUID id) {
        ClashEntry entry = clashes.get(id);
        return entry != null && entry.lost;
    }

    public static void endClash(UUID id1, UUID id2) {
        clashes.remove(id1);
        clashes.remove(id2);
    }

    // Force-clears a single UUID from the clash map — used as a safety escape hatch
    public static void forceRemove(UUID id) {
        ClashEntry entry = clashes.remove(id);
        if (entry != null) {
            // Also remove the opponent so they aren't stuck waiting
            clashes.remove(entry.opponent.getUUID());
        }
    }

    private static void sendMessage(LivingEntity entity, String text) {
        if (entity instanceof ServerPlayerEntity) {
            ((ServerPlayerEntity) entity).sendMessage(
                    new StringTextComponent(text), entity.getUUID());
        }
    }

    @SubscribeEvent
    public static void onEntityHurt(LivingHurtEvent event) {
        UUID id = event.getEntityLiving().getUUID();
        ClashEntry entry = clashes.get(id);
        if (entry == null) return;
        entry.damageTaken += event.getAmount();
        if (entry.damageTaken >= entry.threshold) {
            entry.lost = true;
        }
    }

    // Clean up clash entries when a player dies so they aren't permanently locked
    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        UUID id = event.getEntityLiving().getUUID();
        if (clashes.containsKey(id)) {
            forceRemove(id);
        }
    }

    // Clean up clash entries when a player logs off
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getPlayer().getUUID();
        if (clashes.containsKey(id)) {
            forceRemove(id);
        }
    }
}