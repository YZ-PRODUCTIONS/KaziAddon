package net.kazi.kazimod.blood;

import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;

/** Server-owned receipts: each committed attack can pay out exactly once. */
public final class BloodLedger {
    private static CompoundNBT casts(LivingEntity p) {
        CompoundNBT state = BloodFeatures.state(p);
        if (!state.contains("Casts", 10)) state.put("Casts", new CompoundNBT());
        return state.getCompound("Casts");
    }
    public static UUID begin(LivingEntity p, int life, float amount) {
        UUID id = UUID.randomUUID();
        CompoundNBT n = new CompoundNBT();
        n.putLong("Due", p.level.getGameTime() + life);
        n.putFloat("Amount", amount);
        casts(p).put(id.toString(), n);
        return id;
    }
    public static boolean pending(LivingEntity p, UUID id) { return id != null && casts(p).contains(id.toString(), 10); }
    public static void hit(LivingEntity p, UUID id) { settle(p, id, true, false); }
    public static void neutral(LivingEntity p, UUID id) { settle(p, id, false, true); }
    public static void miss(LivingEntity p, UUID id) { settle(p, id, false, false); }
    private static void settle(LivingEntity p, UUID id, boolean hit, boolean neutral) {
        if (p.level.isClientSide || !pending(p, id)) return;
        CompoundNBT all = casts(p), n = all.getCompound(id.toString());
        all.remove(id.toString());
        if (!p.isAlive() || neutral) return;
        float amount = Math.max(0, Math.min(4, n.getFloat("Amount")));
        if (hit && BloodFeatures.usable(p)) p.heal(amount);
        else if (!hit) p.setHealth(Math.max(1, p.getHealth() - amount));
    }
    public static void tick(LivingEntity p) {
        for (String key : new ArrayList<>(casts(p).getAllKeys()))
            if (p.level.getGameTime() >= casts(p).getCompound(key).getLong("Due")) miss(p, UUID.fromString(key));
    }
    public static void clear(LivingEntity p) {
        for (String key : new ArrayList<>(casts(p).getAllKeys())) miss(p, UUID.fromString(key));
    }
    private BloodLedger() {}
}
