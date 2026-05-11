package net.kazi.kazimod.events.handlers;

import net.kazi.kazimod.abilities.ServerUtility.DevilFruitDamageMultiplierAbility;
import net.kazi.kazimod.abilities.ServerUtility.DevilFruitDamageMultiplier125Ability;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;

import java.lang.reflect.Method;

@Mod.EventBusSubscriber(modid = "kazimod")
public class DevilFruitDamageMultiplierHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource() instanceof AbilityDamageSource)) {
            return;
        }

        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof LivingEntity)) {
            return;
        }

        LivingEntity attacker = (LivingEntity) sourceEntity;
        IAbilityData abilityData = AbilityDataCapability.get(attacker);
        if (abilityData == null) {
            return;
        }

        float damageMultiplier = getActiveMultiplier(abilityData);
        if (damageMultiplier <= 1.0F) {
            return;
        }

        if (!isDevilFruitAbilitySource((AbilityDamageSource) event.getSource())) {
            return;
        }

        event.setAmount(event.getAmount() * damageMultiplier);
    }

    private static float getActiveMultiplier(IAbilityData abilityData) {
        DevilFruitDamageMultiplierAbility multiplier150 =
                (DevilFruitDamageMultiplierAbility) abilityData.getEquippedAbility(
                        DevilFruitDamageMultiplierAbility.INSTANCE
                );
        if (multiplier150 != null && multiplier150.getContinuousComponent().isContinuous()) {
            return DevilFruitDamageMultiplierAbility.DAMAGE_MULTIPLIER;
        }

        DevilFruitDamageMultiplier125Ability multiplier125 =
                (DevilFruitDamageMultiplier125Ability) abilityData.getEquippedAbility(
                        DevilFruitDamageMultiplier125Ability.INSTANCE
                );
        if (multiplier125 != null && multiplier125.getContinuousComponent().isContinuous()) {
            return DevilFruitDamageMultiplier125Ability.DAMAGE_MULTIPLIER;
        }

        return 1.0F;
    }

    private static boolean isDevilFruitAbilitySource(AbilityDamageSource source) {
        Object abilityRef = tryInvoke(source, "getAbility");
        if (abilityRef == null) {
            abilityRef = tryInvoke(source, "getAbilityCore");
        }
        if (abilityRef == null) {
            abilityRef = tryInvoke(source, "getSourceAbility");
        }

        if (abilityRef == null) {
            return false;
        }

        if (abilityRef instanceof AbilityCore) {
            return ((AbilityCore<?>) abilityRef).getCategory() == AbilityCategory.DEVIL_FRUITS;
        }

        Object core = tryInvoke(abilityRef, "getCore");
        if (core instanceof AbilityCore) {
            return ((AbilityCore<?>) core).getCategory() == AbilityCategory.DEVIL_FRUITS;
        }

        Object category = tryInvoke(abilityRef, "getCategory");
        return category == AbilityCategory.DEVIL_FRUITS;
    }

    private static Object tryInvoke(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
