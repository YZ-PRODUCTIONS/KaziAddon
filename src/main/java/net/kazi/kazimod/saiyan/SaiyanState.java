package net.kazi.kazimod.saiyan;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;

public final class SaiyanState {
    public static final String TAG = "KaziSaiyan";
    public static final ResourceLocation FRUIT = new ResourceLocation("kazimod", "hito_hito_no_mi_model_saiyan");
    private SaiyanState() {}
    public static CompoundNBT get(LivingEntity user) {
        CompoundNBT root = user.getPersistentData();
        if (!root.contains(TAG, 10)) root.put(TAG, new CompoundNBT());
        return root.getCompound(TAG);
    }
    public static boolean hasFruit(LivingEntity user) {
        return DevilFruitCapability.get(user).getDevilFruit().filter(FRUIT::equals).isPresent();
    }
    public static int tier(LivingEntity user) { return SaiyanBalance.tier(get(user).getInt("Tier")); }
    public static int form(LivingEntity user) { return Math.min(tier(user), SaiyanBalance.tier(get(user).getInt("Form"))); }
    public static float ki(LivingEntity user) { return SaiyanBalance.clampKi(get(user).getFloat("Ki"), tier(user)); }
    public static int maxKi(LivingEntity user) { return SaiyanBalance.maxKi(tier(user)); }
    public static void addKi(LivingEntity user, float amount) { get(user).putFloat("Ki", SaiyanBalance.clampKi(ki(user) + amount, tier(user))); }
    public static boolean spend(LivingEntity user, float amount) {
        if (ki(user) + .0001F < amount) return false;
        addKi(user, -amount); return true;
    }
    public static float power(LivingEntity user) { return SaiyanBalance.POWER[form(user)]; }
    public static void pose(LivingEntity user, int action, int ticks) {
        CompoundNBT data = get(user);
        if(data.getInt("Action")!=action || data.getLong("ActionEnd")<=user.level.getGameTime())
            data.putLong("ActionStart", user.level.getGameTime());
        data.putInt("Action", action);
        data.putLong("ActionEnd", user.level.getGameTime() + ticks);
    }
    public static int action(LivingEntity user) { return get(user).getLong("ActionEnd") > user.level.getGameTime() ? get(user).getInt("Action") : 0; }
}
