package net.kazi.kazimod.events;

import net.kazi.kazimod.abilities.Nusu.SkillHunterAbility;
import net.kazi.kazimod.abilities.SupaRework.SpiderRework;
import net.kazi.kazimod.abilities.SupaRework.SparklingDaisyRework;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.abilities.supa.*;
import xyz.pixelatedw.mineminenomi.data.entity.ability.*;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;

@Mod.EventBusSubscriber(modid = "kazimod")
public final class SupaBalanceMigration {
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void login(PlayerEvent.PlayerLoggedInEvent event) { migrate(event.getPlayer()); }
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) { migrate(event.getPlayer()); }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void migrate(PlayerEntity player) {
        if (!player.level.isClientSide && DevilFruitCapability.get(player)
                .hasDevilFruit(net.kazi.kazimod.init.KaziItems.NUSU_NUSU_NO_MI.get())) {
            xyz.pixelatedw.mineminenomi.events.abilities.AbilityValidationEvents.checkForPossibleFruitAbilities(player);
            IAbilityData nusuData = AbilityDataCapability.get(player);
            if (nusuData != null && !nusuData.hasUnlockedAbility(SkillHunterAbility.INSTANCE)) {
                nusuData.addUnlockedAbility(SkillHunterAbility.INSTANCE, AbilityUnlock.PROGRESSION);
            }
            xyz.pixelatedw.mineminenomi.wypi.WyNetwork.sendTo(
                    new xyz.pixelatedw.mineminenomi.packets.server.SSyncAbilityDataPacket(player.getId(), nusuData), player);
        }
        if (player.level.isClientSide || net.kazi.kazimod.config.KaziConfig.INSTANCE.disableFruitChanges.get()
                || !DevilFruitCapability.get(player).hasDevilFruit(ModAbilities.SUPA_SUPA_NO_MI)) return;
        IAbilityData data = AbilityDataCapability.get(player);
        AbilityCore[] old = {SpiderAbility.INSTANCE, SparklingDaisyAbility.INSTANCE};
        AbilityCore[] next = {SpiderRework.INSTANCE, SparklingDaisyRework.INSTANCE};
        for (int i = 0; i < old.length; i++) {
            boolean unlocked = data.hasUnlockedAbility(old[i]);
            java.util.List<IAbility> equipped = data.getRawEquippedAbilities();
            for (int slot = 0; slot < equipped.size(); slot++) {
                IAbility ability = equipped.get(slot);
                if (ability != null && ability.getCore() == old[i]) {
                    xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper.emergencyStopAbility(player, ability);
                    if (!DevilFruitCapability.get(player).hasAwakenedFruit()) {
                        data.addUnlockedAbility(next[i], AbilityUnlock.PROGRESSION);
                        data.setEquippedAbility(slot, next[i].createAbility());
                    } else data.removeEquippedAbility(old[i]);
                }
            }
            data.removeUnlockedAbility(old[i]);
            if (unlocked && !DevilFruitCapability.get(player).hasAwakenedFruit()) data.addUnlockedAbility(next[i], AbilityUnlock.PROGRESSION);
        }
        xyz.pixelatedw.mineminenomi.wypi.WyNetwork.sendTo(
                new xyz.pixelatedw.mineminenomi.packets.server.SSyncAbilityDataPacket(player.getId(), data), player);
    }
}
