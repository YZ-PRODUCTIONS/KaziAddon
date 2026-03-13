package net.kazi.kazimod.events;

import net.kazi.kazimod.abilities.HakiRework.FlowHakiAbility;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.abilities.haki.HaoshokuHakiInfusionAbility;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.events.WyLivingHurtEvent;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;

@Mod.EventBusSubscriber(modid = "kazimod")
public class FlowHakiDamageEvents {

    private static final ThreadLocal<Boolean> PROCESSING = ThreadLocal.withInitial(() -> false);

    @SubscribeEvent
    public static void onLivingHurt(WyLivingHurtEvent event) {
        if (PROCESSING.get()) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof LivingEntity)) return;
        LivingEntity attacker = (LivingEntity) sourceEntity;

        if (!AbilityDataCapability.get(attacker).hasEquippedAbility(FlowHakiAbility.INSTANCE)) return;

        FlowHakiAbility ability = (FlowHakiAbility) AbilityDataCapability.get(attacker)
                .getEquippedAbilities(a -> a.getCore() == FlowHakiAbility.INSTANCE)
                .stream()
                .findFirst()
                .orElse(null);
        if (ability == null || !ability.isActive()) return;

        // Only apply our boost to SPECIAL or UNKNOWN natured sources.
        // Hardening/imbuing/internal destruction use HARDENING/IMBUING natures
        // and fire separately — we do NOT touch those, they add their own damage.
        // We ONLY add half of the Haoshoku-specific boost (not busoshoku boosts)
        // so the final total stays below Haoshoku Infusion.
        if (event.getSource() instanceof ModDamageSource) {
            SourceHakiNature nature = ((ModDamageSource) event.getSource()).getHakiNature();
            if (nature == SourceHakiNature.HARDENING || nature == SourceHakiNature.IMBUING) return;
        }

        PROCESSING.set(true);
        try {
            float current = event.getAmount();

            // ONLY add half of the Haoshoku-specific formula.
            // Busoshoku bonuses are handled by hardening/imbuing events separately.
            float haoshokuBoost = (float) HaoshokuHakiInfusionAbility.getDamageBoost(attacker, current);
            event.setAmount(current + haoshokuBoost * 0.5f);
        } finally {
            PROCESSING.set(false);
        }
    }
}