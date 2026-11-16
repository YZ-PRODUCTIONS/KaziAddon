package net.kazi.kazimod.abilities.YagiAwakening;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.events.WyLivingDamageEvent;

/** Cap only the beam's final enemy damage, after Haki and other damage modifiers. */
@Mod.EventBusSubscriber(modid = "kazimod")
public final class ChaosBeamDamage {
    private static final String SOURCE_ID = "kazimod.chaos_beam_nonlethal";
    private ChaosBeamDamage() { }

    public static boolean hurt(LivingEntity caster, LivingEntity target) {
        if (caster == null || target == null || caster.level.isClientSide || caster.level != target.level
                || target == caster || !caster.isAlive() || !target.isAlive() || target.getHealth() <= 1F) return false;
        return target.hurt(new Source(caster), ChaosBeamPattern.DAMAGE);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void nativeDamage(WyLivingDamageEvent event) {
        if (isBeam(event.getSource()))
            event.setAmount(cap(event.getEntityLiving(), event.getAmount()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damage(LivingDamageEvent event) {
        if (isBeam(event.getSource()))
            event.setAmount(cap(event.getEntityLiving(), event.getAmount()));
    }

    /** Final health-write floor, even if another damage callback runs after our event caps. */
    public static float clampHealth(DamageSource source, float proposedHealth) {
        return isBeam(source) && !(proposedHealth >= 1F) ? 1F : proposedHealth;
    }

    private static boolean isBeam(DamageSource source) {
        // MMNM's compatibility path wraps ability sources but preserves their message ID.
        return source instanceof Source || source != null && SOURCE_ID.equals(source.getMsgId());
    }

    private static float cap(LivingEntity target, float amount) {
        float health = target.getHealth();
        if (!Float.isFinite(health) || Float.isNaN(amount)) return 0F;
        return Math.max(0F, Math.min(amount, health - 1F));
    }

    /** A distinct source keeps ordinary Shocker Breaker and caster recoil lethal. */
    private static final class Source extends AbilityDamageSource {
        Source(LivingEntity caster) {
            super(SOURCE_ID, caster, ShockerBreakerAbility.INSTANCE);
            setSourceElement(SourceElement.LIGHT);
            setProjectile();
        }
    }
}
