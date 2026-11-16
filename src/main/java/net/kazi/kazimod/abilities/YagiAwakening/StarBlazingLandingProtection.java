package net.kazi.kazimod.abilities.YagiAwakening;

import net.minecraft.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Protects the cast's landing even if Star Blazing is removed or disabled during descent. */
@Mod.EventBusSubscriber(modid = "kazimod")
public final class StarBlazingLandingProtection {
    private static final String UNTIL_TAG = "kazimod_star_blazing_landing_until";
    private static final int MAX_DESCENT_TICKS = 30 * 20;

    private StarBlazingLandingProtection() {}

    public static void grant(LivingEntity user) {
        if (user.level.isClientSide) return;
        user.fallDistance = 0.0F;
        if (hasLanded(user) || !user.isAlive() || user.removed) {
            user.getPersistentData().remove(UNTIL_TAG);
            return;
        }
        user.getPersistentData().putLong(UNTIL_TAG, user.level.getGameTime() + MAX_DESCENT_TICKS);
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        LivingEntity user = event.getEntityLiving();
        if (user.level.isClientSide || !user.getPersistentData().contains(UNTIL_TAG)) return;
        long until = user.getPersistentData().getLong(UNTIL_TAG);
        user.getPersistentData().remove(UNTIL_TAG);
        if (user.level.getGameTime() >= until) return;
        event.setDamageMultiplier(0.0F);
        user.fallDistance = 0.0F;
    }

    @SubscribeEvent
    public static void onLivingUpdate(LivingUpdateEvent event) {
        LivingEntity user = event.getEntityLiving();
        if (user.level.isClientSide || !user.getPersistentData().contains(UNTIL_TAG)) return;
        if (!user.isAlive() || user.removed || hasLanded(user)
                || user.level.getGameTime() >= user.getPersistentData().getLong(UNTIL_TAG)) {
            // Water and short, harmless landings must also consume the protection.
            user.getPersistentData().remove(UNTIL_TAG);
        }
    }

    private static boolean hasLanded(LivingEntity user) {
        return user.isOnGround() || user.isInWater() || user.isInLava() || user.isPassenger();
    }
}
