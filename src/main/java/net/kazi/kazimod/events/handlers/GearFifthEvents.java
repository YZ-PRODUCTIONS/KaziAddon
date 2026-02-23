package net.kazi.kazimod.events.handlers;

import net.kazi.kazimod.abilities.GomuRework.GearFifthRework;
import net.minecraft.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;

@Mod.EventBusSubscriber(modid = "kazimod") // replace with your actual mod ID if different
public class GearFifthEvents {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntityLiving();

        IAbilityData abilityData = AbilityDataCapability.get(target);
        if (abilityData == null) return;

        GearFifthRework gearFifth = (GearFifthRework) abilityData.getEquippedAbility(GearFifthRework.INSTANCE);
        if (gearFifth == null || !gearFifth.getContinuousComponent().isContinuous()) return;

        if (event.getSource() instanceof AbilityDamageSource) {
            AbilityDamageSource abilitySource = (AbilityDamageSource) event.getSource();
            for (SourceType type : abilitySource.getSourceTypes()) {
                if (type == SourceType.FIST) {
                    event.setAmount(event.getAmount() * 0.5F);
                    return;
                }
            }
        }
    }
}