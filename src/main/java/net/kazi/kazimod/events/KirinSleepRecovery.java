package net.kazi.kazimod.events;

import java.util.Map;
import java.util.WeakHashMap;
import net.MrMagicalCart.cartaddon.init.CartEffects;
import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Sleep recovery is tracked on the target, so it survives changing Kirin casters. */
@Mod.EventBusSubscriber(modid = "kazimod")
public final class KirinSleepRecovery {
    private static final String WAS_ASLEEP = "kazimodKirinWasAsleep";
    private static final String IMMUNE_UNTIL = "kazimodKirinWakeImmunityUntil";
    private static final Map<LivingEntity, EffectInstance> MOVEMENT = new WeakHashMap<>();
    private static final Map<LivingEntity, EffectInstance> DISABLED = new WeakHashMap<>();
    private KirinSleepRecovery() { }

    public static boolean isImmune(LivingEntity target) {
        if (!target.level.isClientSide && target.getPersistentData().getBoolean(WAS_ASLEEP)
                && !isAsleep(target)) wake(target);
        return target.getPersistentData().getLong(IMMUNE_UNTIL) > target.level.getGameTime();
    }

    public static void applySleep(LivingEntity target) {
        if (target.level.isClientSide || isImmune(target)) return;
        if (!target.addEffect(new EffectInstance(KaziEffects.KIRIN_SLEEP.get(), 60, 0))) return;
        EffectInstance prior = target.getEffect(CartEffects.DISABLED_ABILITIES.get());
        target.addEffect(new EffectInstance(CartEffects.DISABLED_ABILITIES.get(), 60, 0));
        if (prior == null || prior == DISABLED.get(target))
            DISABLED.put(target, target.getEffect(CartEffects.DISABLED_ABILITIES.get()));
    }

    public static void wake(LivingEntity target) {
        if (target.level.isClientSide) return;
        target.getPersistentData().remove(WAS_ASLEEP);
        target.getPersistentData().putLong(IMMUNE_UNTIL, target.level.getGameTime() + 600);
        target.removeEffect(KaziEffects.TIRED.get());
        EffectInstance owned = MOVEMENT.remove(target);
        if (owned != null && target.getEffect(KaziEffects.WEAKENED_MOVEMENT.get()) == owned)
            target.removeEffect(KaziEffects.WEAKENED_MOVEMENT.get());
        owned = DISABLED.remove(target);
        if (owned != null && target.getEffect(CartEffects.DISABLED_ABILITIES.get()) == owned)
            target.removeEffect(CartEffects.DISABLED_ABILITIES.get());
    }

    public static void applyMovementPenalty(LivingEntity target, int ticks) {
        if (target.level.isClientSide || isImmune(target)) return;
        EffectInstance previous = target.getEffect(KaziEffects.WEAKENED_MOVEMENT.get());
        if (previous != null && previous != MOVEMENT.get(target)) return;
        target.addEffect(new EffectInstance(KaziEffects.WEAKENED_MOVEMENT.get(), ticks, 0, false, false));
        MOVEMENT.put(target, target.getEffect(KaziEffects.WEAKENED_MOVEMENT.get()));
    }

    @SubscribeEvent
    public static void onApplicable(PotionEvent.PotionApplicableEvent event) {
        if (isImmune(event.getEntityLiving()) && (event.getPotionEffect().getEffect() == KaziEffects.TIRED.get()
                || isSleepEffect(event.getPotionEffect().getEffect()))) event.setResult(Event.Result.DENY);
    }

    @SubscribeEvent
    public static void onSleepAdded(PotionEvent.PotionAddedEvent event) {
        if (!event.getEntityLiving().level.isClientSide && isSleepEffect(event.getPotionEffect().getEffect()))
            event.getEntityLiving().getPersistentData().putBoolean(WAS_ASLEEP, true);
    }

    @SubscribeEvent
    public static void onTick(LivingEvent.LivingUpdateEvent event) {
        LivingEntity target = event.getEntityLiving();
        if (target.level.isClientSide) return;
        if (isAsleep(target)) {
            target.getPersistentData().putBoolean(WAS_ASLEEP, true);
        } else if (target.getPersistentData().getBoolean(WAS_ASLEEP)) {
            wake(target);
        }
    }

    private static boolean isSleepEffect(net.minecraft.potion.Effect effect) {
        return effect == KaziEffects.KIRIN_SLEEP.get() || effect == CartEffects.SLEEPY.get();
    }

    private static boolean isAsleep(LivingEntity target) {
        // Recognize existing Cart sleep too, including effects saved before this update.
        return target.hasEffect(KaziEffects.KIRIN_SLEEP.get()) || target.hasEffect(CartEffects.SLEEPY.get());
    }
}
