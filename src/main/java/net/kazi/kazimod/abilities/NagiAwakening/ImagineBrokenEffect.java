package net.kazi.kazimod.abilities.NagiAwakening;

import java.util.ArrayList;
import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.effects.ModEffect;
import xyz.pixelatedw.mineminenomi.api.events.ability.AbilityUseEvent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;

/** Timed suppression follows the victim, so rebinding cannot bypass it. */
@Mod.EventBusSubscriber(modid = "kazimod")
public class ImagineBrokenEffect extends ModEffect {
    public ImagineBrokenEffect() { super(EffectType.HARMFUL, 0xA5E8FF); }

    public static void apply(LivingEntity target, int ticks) {
        if (target.level.isClientSide || !target.isAlive()) return;
        target.addEffect(new EffectInstance(KaziEffects.IMAGINE_BROKEN.get(), ticks, 0, false, false));
        IAbilityData data = AbilityDataCapability.get(target);
        if (data == null) return;
        for (IAbility ability : new ArrayList<>(data.getEquippedAndPassiveAbilities())) {
            if (isFruitMove(ability)) AbilityHelper.emergencyStopAbility(target, ability);
        }
    }

    private static boolean isFruitMove(IAbility ability) {
        return ability != null && ability.getCore().getCategory() == AbilityCategory.DEVIL_FRUITS;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforeUse(AbilityUseEvent.Pre event) {
        LivingEntity user = event.getEntityLiving();
        if (user != null && user.hasEffect(KaziEffects.IMAGINE_BROKEN.get()) && isFruitMove(event.getAbility())) {
            event.setCanceled(true);
            NagiAwakeningSupport.playNullificationSound(user, user.position());
        }
    }

    // No custom status icon is shipped; the ability description explains the lock.
    @Override public boolean shouldRender(EffectInstance effect) { return false; }
    @Override public boolean shouldRenderHUD(EffectInstance effect) { return false; }
}
