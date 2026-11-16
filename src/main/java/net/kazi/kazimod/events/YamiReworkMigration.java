package net.kazi.kazimod.events;

import net.kazi.kazimod.abilities.YamiRework.KurozuRework;
import net.kazi.kazimod.abilities.YamiRework.DarkMatterRework;
import net.kazi.kazimod.abilities.YamiRework.LiberationRework;
import net.kazi.kazimod.abilities.YamiRework.DarknessStoryAbility;
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
        boolean changed=replace(player,data,new AbilityCore<?>[]{
                xyz.pixelatedw.mineminenomi.abilities.yami.DarkMatterAbility.INSTANCE,
                net.MrMagicalCart.cartaddon.abilities.yamiextra.ReworkedDarkMatterAbility.INSTANCE
        },DarkMatterRework.INSTANCE);
        AbilityCore<?>[] originals = {
                xyz.pixelatedw.mineminenomi.abilities.yami.KurouzuAbility.INSTANCE,
                net.MrMagicalCart.cartaddon.abilities.yamiextra.ReworkedKurozuAbility.INSTANCE
        };
        changed|=replace(player,data,originals,KurozuRework.INSTANCE);
        changed|=replace(player,data,new AbilityCore<?>[]{
                xyz.pixelatedw.mineminenomi.abilities.yami.LiberationAbility.INSTANCE,
                net.MrMagicalCart.cartaddon.abilities.yamiextra.ReworkedLiberationAbility.INSTANCE
        },LiberationRework.INSTANCE);
        changed|=mergeStorm(player,data);
        if (changed) WyNetwork.sendTo(new SSyncAbilityDataPacket(player.getId(), data), player);
    }

    private static boolean mergeStorm(PlayerEntity player,IAbilityData data) {
        boolean changed=data.hasUnlockedAbility(DarknessStoryAbility.INSTANCE);
        LiberationRework liberation=data.getEquippedAbility(LiberationRework.INSTANCE);
        java.util.List<IAbility> equipped=new java.util.ArrayList<>(data.getRawEquippedAbilities());
        for(int slot=0;slot<equipped.size();slot++) {
            IAbility old=equipped.get(slot);
            if(old==null||old.getCore()!=DarknessStoryAbility.INSTANCE)continue;
            AbilityHelper.emergencyStopAbility(player,old);
            if(liberation==null) {
                liberation=LiberationRework.INSTANCE.createAbility();
                liberation.getComponent(xyz.pixelatedw.mineminenomi.init.ModAbilityKeys.ALT_MODE)
                        .ifPresent(c->c.setMode(player,LiberationRework.Mode.STORM));
                data.addUnlockedAbility(LiberationRework.INSTANCE,AbilityUnlock.PROGRESSION);
                data.setEquippedAbility(slot,liberation);
            } else data.removeEquippedAbility(old);
            // Merging the slots must not clear an existing Storm cooldown.
            xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent source=
                    old.getComponent(xyz.pixelatedw.mineminenomi.init.ModAbilityKeys.COOLDOWN).orElse(null);
            xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent destination=
                    liberation.getComponent(xyz.pixelatedw.mineminenomi.init.ModAbilityKeys.COOLDOWN).orElse(null);
            if(source!=null&&destination!=null&&source.getCooldown()>destination.getCooldown())destination.load(source.save());
            changed=true;
        }
        if(changed) {
            data.removeUnlockedAbility(DarknessStoryAbility.INSTANCE);
            data.addUnlockedAbility(LiberationRework.INSTANCE,AbilityUnlock.PROGRESSION);
        }
        return changed;
    }

    private static boolean replace(PlayerEntity player,IAbilityData data,AbilityCore<?>[] originals,AbilityCore<?> replacement) {
        boolean changed=false;
        for (AbilityCore<?> original : originals) {
            boolean hadOriginal = data.hasUnlockedAbility(original);
            java.util.List<IAbility> equipped = data.getRawEquippedAbilities();
            for (int slot = 0; slot < equipped.size(); slot++) {
                IAbility ability = equipped.get(slot);
                if (ability != null && ability.getCore() == original) {
                    AbilityHelper.emergencyStopAbility(player, ability);
                    data.addUnlockedAbility(replacement, AbilityUnlock.PROGRESSION);
                    IAbility next=replacement.createAbility();
                    if(replacement==LiberationRework.INSTANCE&&ability.getComponent(xyz.pixelatedw.mineminenomi.init.ModAbilityKeys.ALT_MODE)
                            .map(c->c.getCurrentMode().name().equals("RELEASE")).orElse(false)) {
                        next.getComponent(xyz.pixelatedw.mineminenomi.init.ModAbilityKeys.ALT_MODE)
                                .ifPresent(c->c.setMode(player,LiberationRework.Mode.RELEASE));
                    }
                    data.setEquippedAbility(slot,next);
                    changed = true;
                }
            }
            if (hadOriginal) {
                data.removeUnlockedAbility(original);
                data.addUnlockedAbility(replacement, AbilityUnlock.PROGRESSION);
                changed = true;
            }
        }
        return changed;
    }
}
