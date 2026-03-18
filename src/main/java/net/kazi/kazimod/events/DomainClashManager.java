package net.kazi.kazimod.abilities.Gojo;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DomainClashManager {

    // Maps challenger UUID -> defender UUID for active clashes
    private static final Map<UUID, UUID> activeClashes = new HashMap<>();
    // Maps entity UUID -> damage taken during clash
    private static final Map<UUID, Float> clashDamage = new HashMap<>();
    // Maps entity UUID -> their max HP threshold for losing
    private static final Map<UUID, Float> clashThresholds = new HashMap<>();

    public static boolean isInClash(LivingEntity entity) {
        UUID id = entity.getUUID();
        return activeClashes.containsKey(id) || activeClashes.containsValue(id);
    }

    public static void startClash(LivingEntity entity1, LivingEntity entity2) {
        activeClashes.put(entity1.getUUID(), entity2.getUUID());
        clashDamage.put(entity1.getUUID(), 0.0F);
        clashDamage.put(entity2.getUUID(), 0.0F);
        clashThresholds.put(entity1.getUUID(), entity1.getMaxHealth() * 0.20F);
        clashThresholds.put(entity2.getUUID(), entity2.getMaxHealth() * 0.20F);
    }

    public static void addDamage(UUID entityId, float damage) {
        clashDamage.merge(entityId, damage, Float::sum);
    }

    public static float getDamage(UUID entityId) {
        return clashDamage.getOrDefault(entityId, 0.0F);
    }

    public static float getThreshold(UUID entityId) {
        return clashThresholds.getOrDefault(entityId, Float.MAX_VALUE);
    }

    public static UUID getOpponent(UUID entityId) {
        if (activeClashes.containsKey(entityId)) return activeClashes.get(entityId);
        for (Map.Entry<UUID, UUID> entry : activeClashes.entrySet()) {
            if (entry.getValue().equals(entityId)) return entry.getKey();
        }
        return null;
    }

    public static void endClash(UUID entity1Id, UUID entity2Id) {
        activeClashes.remove(entity1Id);
        activeClashes.remove(entity2Id);
        clashDamage.remove(entity1Id);
        clashDamage.remove(entity2Id);
        clashThresholds.remove(entity1Id);
        clashThresholds.remove(entity2Id);
    }

    public static boolean hasLost(UUID entityId) {
        float damage = getDamage(entityId);
        float threshold = getThreshold(entityId);
        return damage >= threshold;
    }
}