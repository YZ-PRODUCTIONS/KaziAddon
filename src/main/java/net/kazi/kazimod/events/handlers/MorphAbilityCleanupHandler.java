package net.kazi.kazimod.events.handlers;

import net.kazi.kazimod.mixin.MorphAbility2Accessor;
import net.minecraft.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.MorphAbility2;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;

@Mod.EventBusSubscriber(modid = "kazimod")
public class MorphAbilityCleanupHandler {

    private static void cleanupMorphAbilities(LivingEntity entity) {
        IAbilityData abilityData = AbilityDataCapability.get(entity);
        if (abilityData == null) {
            return;
        }

        for (IAbility ability : abilityData.getEquippedAbilities()) {
            if (!(ability instanceof MorphAbility2)) {
                continue;
            }

            MorphAbility2Accessor accessor = (MorphAbility2Accessor) ability;
            accessor.kazi$getContinuousComponent().stopContinuity(entity);
            accessor.kazi$getStatsComponent().removeModifiers(entity);
            accessor.kazi$getMorphComponent().stopMorph(entity);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        cleanupMorphAbilities(event.getEntityLiving());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        cleanupMorphAbilities(event.getPlayer());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        cleanupMorphAbilities(event.getPlayer());
    }
}
