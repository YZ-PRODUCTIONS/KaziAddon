package net.kazi.kazimod.tusk;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;

public final class TuskState {
    public static final String TAG = "KaziTusk";
    public static final ResourceLocation FRUIT = new ResourceLocation("cartaddon", "guru_guru_no_mi");
    public static CompoundNBT get(LivingEntity user) {
        CompoundNBT root = user.getPersistentData();
        if (!root.contains(TAG, 10)) root.put(TAG, new CompoundNBT());
        return root.getCompound(TAG);
    }
    public static boolean hasFruit(LivingEntity user) {
        return DevilFruitCapability.get(user) != null
                && DevilFruitCapability.get(user).getDevilFruit().filter(FRUIT::equals).isPresent();
    }
    public static boolean awakened(LivingEntity user) {
        return hasFruit(user) && DevilFruitCapability.get(user).hasAwakenedFruit();
    }
    public static int tier(LivingEntity user) { return MathHelper.clamp(get(user).getInt("Tier"), 1, 4); }
    public static int act(LivingEntity user) { return MathHelper.clamp(get(user).getInt("Act"), 1, tier(user)); }
    public static float nails(LivingEntity user) { return finite(get(user).getFloat("Nails"), 10); }
    public static float spin(LivingEntity user) { return finite(get(user).getFloat("Spin"), 100); }
    private static float finite(float value, float max) { return Float.isFinite(value) ? MathHelper.clamp(value, 0, max) : 0; }
    public static boolean spend(LivingEntity user, int nails, float spin) {
        if (nails(user) + .0001F < nails || spin(user) + .0001F < spin) return false;
        get(user).putFloat("Nails", nails(user) - nails);
        get(user).putFloat("Spin", spin(user) - spin);
        return true;
    }
    public static void addSpin(LivingEntity user, float amount) { get(user).putFloat("Spin", finite(spin(user) + amount, 100)); }
    public static void initialize(LivingEntity user) {
        CompoundNBT n = get(user);
        if (!n.getBoolean("Initialized")) {
            n.putBoolean("Initialized", true); n.putInt("Tier", 1); n.putInt("Act", 1);
            n.putFloat("Nails", 10); n.putBoolean("Manifest", true);
        }
    }
    public static void pose(LivingEntity user, int action, int ticks) {
        CompoundNBT n = get(user);
        n.putInt("Action", action); n.putLong("ActionStart", user.level.getGameTime());
        n.putLong("ActionEnd", user.level.getGameTime() + ticks);
    }
    public static int action(LivingEntity user) { return get(user).getLong("ActionEnd") > user.level.getGameTime() ? get(user).getInt("Action") : 0; }
    public static void progress(LivingEntity user, String key) {
        if (!awakened(user)) return;
        CompoundNBT n = get(user); n.putInt(key, Math.min(100000, n.getInt(key) + 1));
        int tier = tier(user);
        if (tier == 1 && n.getInt("NailHits") >= 40 && n.getInt("LongShots") >= 10
                || tier == 2 && n.getInt("HoleHits") >= 24 && n.getInt("Bursts") >= 8
                || tier == 3 && n.getInt("WormholeHits") >= 20 && n.getInt("Dodges") >= 12) {
            n.putInt("Tier", tier + 1); n.putInt("Act", tier + 1); n.putBoolean("Manifest", true);
            n.putFloat("Nails", 10); pose(user, 1, 40);
            if (user instanceof net.minecraft.entity.player.PlayerEntity) {
                net.minecraft.entity.player.PlayerEntity p = (net.minecraft.entity.player.PlayerEntity) user;
                p.displayClientMessage(new net.minecraft.util.text.TranslationTextComponent("message.kazimod.tusk.evolved", tier + 1), false);
                TuskFeatures.reconcile(p); TuskFeatures.sync(p);
            }
        }
    }
    public static String mastery(LivingEntity user) {
        CompoundNBT n = get(user);
        switch (tier(user)) {
            case 1: return "Act 2: nail hits " + Math.min(40, n.getInt("NailHits")) + "/40; 8-block shots " + Math.min(10, n.getInt("LongShots")) + "/10";
            case 2: return "Act 3: pursuing-hole hits " + Math.min(24, n.getInt("HoleHits")) + "/24; burst hits " + Math.min(8, n.getInt("Bursts")) + "/8";
            case 3: return "Act 4: wormhole-shot hits " + Math.min(20, n.getInt("WormholeHits")) + "/20; attacks evaded " + Math.min(12, n.getInt("Dodges")) + "/12";
            default: return "All four Acts mastered";
        }
    }
    private TuskState() {}
}
