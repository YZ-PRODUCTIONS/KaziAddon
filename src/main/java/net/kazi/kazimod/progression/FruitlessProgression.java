package net.kazi.kazimod.progression;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.MrMagicalCart.cartaddon.abilities.aowrework.abilities.zeus.*;
import net.MrMagicalCart.cartaddon.abilities.axestyle.*;
import net.MrMagicalCart.cartaddon.abilities.blacklegextra.BeoufBurstAbility;
import net.MrMagicalCart.cartaddon.abilities.bludgeon.*;
import net.MrMagicalCart.cartaddon.abilities.brawlerextra.*;
import net.MrMagicalCart.cartaddon.abilities.cyborgextra.GeneralFrankyAbility;
import net.MrMagicalCart.cartaddon.abilities.dagger.*;
import net.MrMagicalCart.cartaddon.abilities.electroextra.CartSulongAbility;
import net.MrMagicalCart.cartaddon.abilities.greatblade.VoidShatteringSlashAbility;
import net.MrMagicalCart.cartaddon.abilities.hasshoken.*;
import net.MrMagicalCart.cartaddon.abilities.mihawk.*;
import net.MrMagicalCart.cartaddon.abilities.modifiedhuman.RaidSuitAbility;
import net.MrMagicalCart.cartaddon.abilities.nitoryu.*;
import net.MrMagicalCart.cartaddon.abilities.ryusoken.*;
import net.MrMagicalCart.cartaddon.abilities.saber.*;
import net.MrMagicalCart.cartaddon.abilities.santoryu.*;
import net.MrMagicalCart.cartaddon.abilities.sniperextra.*;
import net.MrMagicalCart.cartaddon.abilities.swordsmenextra.HiryuKaenInfernoAbility;
import net.MrMagicalCart.cartaddon.abilities.trident.*;
import net.MrMagicalCart.cartaddon.abilities.twinbarrel.*;
import net.kazi.kazimod.abilities.AxeStyleRework.*;
import net.kazi.kazimod.abilities.BludgeonRework.*;
import net.kazi.kazimod.abilities.BrawlerRework.GalaxyImpactRework;
import net.kazi.kazimod.abilities.Nusu.SkillHunterEXAbility;
import net.kazi.kazimod.abilities.SaberRework.DivineDepartureClone;
import net.kazi.kazimod.abilities.SpearRework.*;
import net.kazi.kazimod.abilities.swordsmanrework.*;
import net.kazi.kazimod.init.KaziPacketHandler;
import net.kazi.kazimod.items.AwakeningEssenceItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.events.ability.AbilityUseEvent;
import xyz.pixelatedw.mineminenomi.api.events.ability.UnlockAbilityEvent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit;
import xyz.pixelatedw.mineminenomi.packets.server.SSyncAbilityDataPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

@Mod.EventBusSubscriber(modid = "kazimod")
public final class FruitlessProgression {
    public static final String UNLOCK_TAG = "KaziFruitlessAwakened";

    private FruitlessProgression() {}

    // Include replaced Cart cores so old saves cannot bypass the new requirement.
    private static final class Moves {
        static final Set<AbilityCore<?>> CORES = Collections.unmodifiableSet(new HashSet<>(Arrays.<AbilityCore<?>>asList(
                GrowUpAbility.INSTANCE, LightningBlastAbility.INSTANCE, ThunderSharkTempoAbility.INSTANCE,
                FutenrakuAbility.INSTANCE, YasotakeruAbility.INSTANCE, BeoufBurstAbility.INSTANCE,
                DestroyerOfDeathThunderBaguaAbility.INSTANCE, ShinsokuHakujakuAbility.INSTANCE,
                BlueHoleAbility.INSTANCE, GalaxyImpactAbility.INSTANCE, GeneralFrankyAbility.INSTANCE,
                FinalExecutionAbility.INSTANCE, ShadowStalkAbility.INSTANCE, CartSulongAbility.INSTANCE,
                VoidShatteringSlashAbility.INSTANCE, ButoKaitenRanbuAbility.INSTANCE, KiryuKirikugiAbility.INSTANCE,
                IntenseBlackBladeFallenAbility.INSTANCE, TripleVoidShatteringSlashAbility.INSTANCE,
                RaidSuitAbility.INSTANCE, MixedBumakiAbility.INSTANCE, ParadiseTotsukaAbility.INSTANCE,
                DragonsLawnMowerAbility.INSTANCE, DragonsWrathAbility.INSTANCE,
                HiNoKagutsuchiNoEisuAbility.INSTANCE, NewDivineDepartureAbility.INSTANCE,
                AshuraBakkeiMojanoTawamureAbility.INSTANCE, KingOfHellAbility.INSTANCE,
                MidoriBoshiTakeJavreinAbility.INSTANCE, TenryuBoshiAbility.INSTANCE, HiryuKaenInfernoAbility.INSTANCE,
                AbsolutePierceAbility.INSTANCE, SkySplitterDescentAbility.INSTANCE,
                BulletRushAbility.INSTANCE, PointBlankAbility.INSTANCE,
                FutenrakuRework.INSTANCE, YasotakeruRework.INSTANCE,
                DestroyerOfDeathThunderBaguaRework.INSTANCE, ShinsokuHakujakuRework.INSTANCE,
                GalaxyImpactRework.INSTANCE, DivineDepartureClone.INSTANCE,
                AbsolutePierceRework.INSTANCE, SkySplitterDescentRework.INSTANCE,
                FoxfireStyleAbility.INSTANCE, RadiantSliceAbility.INSTANCE
        )));
    }

    public static boolean requiresEssence(AbilityCore<?> core) {
        return core != null && Moves.CORES.contains(core);
    }

    public static boolean unlocked(PlayerEntity player) {
        return player.getPersistentData().getBoolean(UNLOCK_TAG);
    }

    public static boolean fruitless(LivingEntity entity) {
        IDevilFruit fruit = DevilFruitCapability.get(entity);
        return fruit != null && !fruit.hasAnyDevilFruit();
    }

    public static boolean locked(LivingEntity entity, AbilityCore<?> core) {
        return entity instanceof PlayerEntity && requiresEssence(core)
                && (!unlocked((PlayerEntity) entity) || !fruitless(entity));
    }

    public static AbilityUseResult canUse(LivingEntity entity, IAbility ability) {
        if (!(entity instanceof PlayerEntity) || SkillHunterEXAbility.isRolledMove(entity, ability)) {
            return AbilityUseResult.success();
        }
        if (!fruitless(entity)) {
            return AbilityUseResult.fail(new TranslationTextComponent("message.kazimod.fruitless.requires_no_fruit"));
        }
        return unlocked((PlayerEntity) entity) ? AbilityUseResult.success()
                : AbilityUseResult.fail(new TranslationTextComponent("message.kazimod.fruitless.requires_essence"));
    }

    public static boolean unlock(PlayerEntity player, ItemStack essence) {
        if (player.level.isClientSide || !fruitless(player) || unlocked(player)
                || essence.isEmpty() || !(essence.getItem() instanceof AwakeningEssenceItem)) return false;
        player.getPersistentData().putBoolean(UNLOCK_TAG, true);
        if (!player.isCreative()) essence.shrink(1);
        reconcile(player);
        sync(player);
        player.sendMessage(new TranslationTextComponent("message.kazimod.fruitless.unlocked"), player.getUUID());
        return true;
    }

    public static boolean reconcile(PlayerEntity player) {
        if (player.level.isClientSide) return false;
        IAbilityData data = AbilityDataCapability.get(player);
        boolean changed = false;
        for (AbilityCore<?> core : Moves.CORES) {
            if (core.getRegistryName() == null) continue;
            if (locked(player, core)) {
                IAbility equipped = data.getEquippedAbility(core);
                if (equipped != null && SkillHunterEXAbility.isRolledMove(player, equipped)) continue;
                if (data.getUnlockTypeForAbility(core) != AbilityUnlock.PROGRESSION) continue;
                if (equipped != null) AbilityHelper.emergencyStopAbility(player, equipped);
                IAbility passive = data.getPassiveAbility(core);
                if (passive != null) AbilityHelper.emergencyStopAbility(player, passive);
                changed |= data.removeUnlockedAbility(core);
            } else if (!core.isHidden() && core.canUnlock(player)) {
                changed |= data.addUnlockedAbility(core, AbilityUnlock.PROGRESSION);
            }
        }
        if (changed && player instanceof ServerPlayerEntity) {
            WyNetwork.sendTo(new SSyncAbilityDataPacket(player.getId(), data), (ServerPlayerEntity) player);
        }
        return changed;
    }

    private static void sync(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity) {
            KaziPacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player),
                    new FruitlessProgressionPacket(player.getId(), unlocked(player)));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void unlockAbility(UnlockAbilityEvent event) {
        if (locked(event.getEntityLiving(), event.getAbilityCore())) event.setResult(Event.Result.DENY);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void useAbility(AbilityUseEvent.Pre event) {
        if (!requiresEssence(event.getAbility().getCore())) return;
        AbilityUseResult result = canUse(event.getEntityLiving(), event.getAbility());
        if (result.isFail()) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level.isClientSide
                && event.player.tickCount % 20 == 0) reconcile(event.player);
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        event.getPlayer().getPersistentData().putBoolean(UNLOCK_TAG, unlocked(event.getOriginal()));
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        reconcile(event.getPlayer());
        sync(event.getPlayer());
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        reconcile(event.getPlayer());
        sync(event.getPlayer());
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        sync(event.getPlayer());
    }
}
