package net.kazi.kazimod.abilities.Nusu;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.ResourceLocation;
import xyz.pixelatedw.mineminenomi.api.ModRegistries;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NusuStolenData {

    public static final String HELD_KEY   = "nusu_held";
    public static final String VICTIM_KEY = "nusu_stolen_from";
    public static final int    MAX_SLOTS  = 2; // reduced from 4 to 2

    // ── victim ────────────────────────────────────────────────────────────────

    public static void markStolen(LivingEntity victim, AbilityCore<?> core) {
        ResourceLocation rl = ModRegistries.ABILITIES.getKey(core);
        if (rl == null) return;
        CompoundNBT nbt = victim.getPersistentData();
        ListNBT list = readList(nbt, VICTIM_KEY);
        String s = rl.toString();
        if (!listContains(list, s)) list.add(StringNBT.valueOf(s));
        nbt.put(VICTIM_KEY, list);
    }

    public static boolean isStolen(LivingEntity victim, AbilityCore<?> core) {
        ResourceLocation rl = ModRegistries.ABILITIES.getKey(core);
        if (rl == null) return false;
        return listContains(readList(victim.getPersistentData(), VICTIM_KEY), rl.toString());
    }

    public static void unmarkStolen(LivingEntity victim, AbilityCore<?> core) {
        ResourceLocation rl = ModRegistries.ABILITIES.getKey(core);
        if (rl == null) return;
        CompoundNBT nbt = victim.getPersistentData();
        if (!nbt.contains(VICTIM_KEY)) return;
        ListNBT list = nbt.getList(VICTIM_KEY, 8);
        String s = rl.toString();
        for (int i = 0; i < list.size(); i++) {
            if (list.getString(i).equals(s)) { list.remove(i); break; }
        }
        nbt.put(VICTIM_KEY, list);
    }

    public static List<AbilityCore<?>> getStolenFrom(LivingEntity victim) {
        return parseList(readList(victim.getPersistentData(), VICTIM_KEY));
    }

    // ── nusu user ─────────────────────────────────────────────────────────────

    public static int heldCount(LivingEntity user) {
        return readList(user.getPersistentData(), HELD_KEY).size();
    }

    public static int addHeld(LivingEntity user, AbilityCore<?> core) {
        ResourceLocation rl = ModRegistries.ABILITIES.getKey(core);
        if (rl == null) return -1;
        CompoundNBT nbt = user.getPersistentData();
        ListNBT list = readList(nbt, HELD_KEY);
        if (list.size() >= MAX_SLOTS) return -1;
        int slot = list.size();
        list.add(StringNBT.valueOf(rl.toString()));
        nbt.put(HELD_KEY, list);
        return slot;
    }

    public static List<AbilityCore<?>> getHeld(LivingEntity user) {
        return parseList(readList(user.getPersistentData(), HELD_KEY));
    }

    public static boolean isHeld(LivingEntity user, AbilityCore<?> core) {
        ResourceLocation rl = ModRegistries.ABILITIES.getKey(core);
        if (rl == null) return false;
        return listContains(readList(user.getPersistentData(), HELD_KEY), rl.toString());
    }

    public static void removeHeld(LivingEntity user, AbilityCore<?> core) {
        ResourceLocation rl = ModRegistries.ABILITIES.getKey(core);
        if (rl == null) return;
        CompoundNBT nbt = user.getPersistentData();
        if (!nbt.contains(HELD_KEY)) return;
        ListNBT list = nbt.getList(HELD_KEY, 8);
        String s = rl.toString();
        for (int i = 0; i < list.size(); i++) {
            if (list.getString(i).equals(s)) { list.remove(i); break; }
        }
        nbt.put(HELD_KEY, list);
    }

    public static void clearHeld(LivingEntity user) {
        user.getPersistentData().put(HELD_KEY, new ListNBT());
    }

    // ── pending steal ─────────────────────────────────────────────────────────

    public static final String PENDING_TARGET_KEY  = "nusu_pending_target";
    public static final String PENDING_CHOICES_KEY = "nusu_pending_choices";

    public static void setPendingStealTarget(LivingEntity user, LivingEntity target) {
        user.getPersistentData().putUUID(PENDING_TARGET_KEY, target.getUUID());
    }

    public static void setPendingChoices(LivingEntity user, List<AbilityCore<?>> choices) {
        ListNBT list = new ListNBT();
        for (AbilityCore<?> core : choices) {
            ResourceLocation rl = ModRegistries.ABILITIES.getKey(core);
            if (rl != null) list.add(StringNBT.valueOf(rl.toString()));
        }
        user.getPersistentData().put(PENDING_CHOICES_KEY, list);
    }

    public static List<AbilityCore<?>> getPendingChoices(LivingEntity user) {
        return parseList(readList(user.getPersistentData(), PENDING_CHOICES_KEY));
    }

    public static UUID getPendingStealTargetUUID(LivingEntity user) {
        CompoundNBT nbt = user.getPersistentData();
        if (!nbt.contains(PENDING_TARGET_KEY)) return null;
        try { return nbt.getUUID(PENDING_TARGET_KEY); } catch (Exception e) { return null; }
    }

    public static void clearPending(LivingEntity user) {
        CompoundNBT nbt = user.getPersistentData();
        nbt.remove(PENDING_TARGET_KEY);
        nbt.put(PENDING_CHOICES_KEY, new ListNBT());
    }

    // ── internal helpers ──────────────────────────────────────────────────────

    private static ListNBT readList(CompoundNBT nbt, String key) {
        return nbt.contains(key) ? nbt.getList(key, 8) : new ListNBT();
    }

    private static boolean listContains(ListNBT list, String value) {
        for (int i = 0; i < list.size(); i++) {
            if (list.getString(i).equals(value)) return true;
        }
        return false;
    }

    private static List<AbilityCore<?>> parseList(ListNBT list) {
        List<AbilityCore<?>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            try {
                ResourceLocation rl = new ResourceLocation(list.getString(i));
                AbilityCore<?> core = ModRegistries.ABILITIES.getValue(rl);
                if (core != null) result.add(core);
            } catch (Exception ignored) {}
        }
        return result;
    }
}