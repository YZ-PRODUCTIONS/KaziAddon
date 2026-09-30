package net.kazi.kazimod.events;

import net.kazi.kazimod.abilities.YamiRework.KurozuRework;
import net.kazi.kazimod.config.KaziConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.*;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;
import xyz.pixelatedw.mineminenomi.packets.server.SSyncAbilityDataPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

@Mod.EventBusSubscriber(modid = "kazimod")
public final class YamiReworkMigration {
    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) { migrate(event.getPlayer()); }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) { migrate(event.getPlayer()); }

    private static void migrate(PlayerEntity player) {
        if (player.level.isClientSide || KaziConfig.INSTANCE.disableFruitChanges.get()
                || !DevilFruitCapability.get(player).hasDevilFruit(ModAbilities.YAMI_YAMI_NO_MI)) return;
        IAbilityData data = AbilityDataCapability.get(player);
        AbilityCore<?>[] originals = {
                xyz.pixelatedw.mineminenomi.abilities.yami.KurouzuAbility.INSTANCE,
                net.MrMagicalCart.cartaddon.abilities.yamiextra.ReworkedKurozuAbility.INSTANCE
        };
        boolean changed = false;
        for (AbilityCore<?> original : originals) {
            boolean hadOriginal = data.hasUnlockedAbility(original);
            java.util.List<IAbility> equipped = data.getRawEquippedAbilities();
            for (int slot = 0; slot < equipped.size(); slot++) {
                IAbility ability = equipped.get(slot);
                if (ability != null && ability.getCore() == original) {
                    AbilityHelper.emergencyStopAbility(player, ability);
                    data.addUnlockedAbility(KurozuRework.INSTANCE, AbilityUnlock.PROGRESSION);
                    data.setEquippedAbility(slot, KurozuRework.INSTANCE.createAbility());
                    changed = true;
                }
            }
            if (hadOriginal) {
                data.removeUnlockedAbility(original);
                data.addUnlockedAbility(KurozuRework.INSTANCE, AbilityUnlock.PROGRESSION);
                changed = true;
            }
        }
        if (changed) WyNetwork.sendTo(new SSyncAbilityDataPacket(player.getId(), data), player);
    }
}
