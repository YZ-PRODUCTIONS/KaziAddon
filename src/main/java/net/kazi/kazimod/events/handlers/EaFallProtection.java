package net.kazi.kazimod.events.handlers;

import net.minecraft.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Protects Ea's next landing even if its charge is canceled or the ability is unequipped. */
@Mod.EventBusSubscriber(modid = "kazimod")
public final class EaFallProtection {
    private static final String UNTIL_TAG = "kazimod_ea_fall_protection_until";
    private static final int PROTECTION_TICKS = 30 * 20;

    private EaFallProtection() {}

    public static void grant(LivingEntity user) {
        if (!user.level.isClientSide) {
            user.getPersistentData().putLong(UNTIL_TAG, user.level.getGameTime() + PROTECTION_TICKS);
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        LivingEntity user = event.getEntityLiving();
        if (user.level.isClientSide || !user.getPersistentData().contains(UNTIL_TAG)) return;
        long until = user.getPersistentData().getLong(UNTIL_TAG);
        user.getPersistentData().remove(UNTIL_TAG);
        if (user.level.getGameTime() >= until) return;
        event.setDamageMultiplier(0.0F);
        user.fallDistance = 0.0F;
    }
}
