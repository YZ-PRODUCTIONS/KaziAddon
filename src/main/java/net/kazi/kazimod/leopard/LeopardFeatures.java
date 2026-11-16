package net.kazi.kazimod.leopard;

import net.kazi.kazimod.api.KaziRegistry;
import net.kazi.kazimod.util.FruitAbilityInjector;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUnlock;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.abilities.nekoleopard.ClawStrikeAbility;
import xyz.pixelatedw.mineminenomi.abilities.nekoleopard.FerociousLeapAbility;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.MorphHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;
import xyz.pixelatedw.mineminenomi.packets.server.SSyncAbilityDataPacket;
import xyz.pixelatedw.mineminenomi.packets.server.SSyncDevilFruitPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

public final class LeopardFeatures {
    public static final RegistryObject<LeopardAwakenedMorph> MORPH = KaziRegistry.registerMorph("leopard_awakened", LeopardAwakenedMorph::new);
    public static void init(IEventBus bus) {
        KaziRegistry.registerAbility(LeopardAwakeningAbility.INSTANCE);
        KaziRegistry.registerAbility(ShuganBarrageAbility.INSTANCE);
        KaziRegistry.registerAbility(TailGrabAbility.INSTANCE);
        KaziRegistry.registerAbility(PredatorHuntAbility.INSTANCE);
        KaziRegistry.registerAbility(KamisoriAbility.INSTANCE);
        bus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(() -> {
            FruitAbilityInjector.addAbilities(ModAbilities.NEKO_NEKO_NO_MI_LEOPARD, LeopardAwakeningAbility.INSTANCE, TailGrabAbility.INSTANCE, KamisoriAbility.INSTANCE);
            net.kazi.kazimod.events.AwakeningAbilityLoginFix.invalidateCache();
        }));
        MinecraftForge.EVENT_BUS.addListener(LeopardFeatures::tick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, LeopardFeatures::hurt);
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> reconcile(e.getPlayer()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> reconcile(e.getPlayer()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> reconcile(e.getPlayer()));
    }
    public static boolean fruit(LivingEntity user) { return DevilFruitCapability.get(user).hasDevilFruit(ModAbilities.NEKO_NEKO_NO_MI_LEOPARD); }
    public static boolean awakened(LivingEntity user) { return fruit(user) && DevilFruitCapability.get(user).hasAwakenedFruit(); }
    public static boolean active(LivingEntity user) { return MorphHelper.getZoanInfo(user) == MORPH.get(); }
    public static boolean empowered(LivingEntity user) { return active(user) && awakened(user) && !AbilityHelper.isWeakenedByKairosekiOrWater(user); }
    public static boolean awaken(PlayerEntity user, ItemStack essence) {
        if (user.level.isClientSide || !xyz.pixelatedw.mineminenomi.config.CommonConfig.INSTANCE.hasAwakeningsEnabled()
                || !fruit(user) || awakened(user) || essence.isEmpty()
                || !(essence.getItem() instanceof net.kazi.kazimod.items.AwakeningEssenceItem)) return false;
        DevilFruitCapability.get(user).setAwakenedFruit(true);
        if (!user.isCreative()) essence.shrink(1);
        reconcile(user);
        WyNetwork.sendTo(new SSyncDevilFruitPacket(user.getId(), DevilFruitCapability.get(user)), user);
        user.displayClientMessage(new TranslationTextComponent("message.kazimod.leopard.awakened"), false);
        user.level.playSound(null, user.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundCategory.PLAYERS, .8F, .7F);
        return true;
    }
    public static void reconcile(PlayerEntity user) {
        if (user.level.isClientSide) return;
        IAbilityData data = AbilityDataCapability.get(user);
        boolean changed = false;
        changed |= migrateAlt(user, data, ShuganBarrageAbility.INSTANCE, ClawStrikeAbility.INSTANCE);
        changed |= migrateAlt(user, data, PredatorHuntAbility.INSTANCE, FerociousLeapAbility.INSTANCE);
        for(xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<?> core : new xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore<?>[]{TailGrabAbility.INSTANCE,KamisoriAbility.INSTANCE}) {
            if(awakened(user)) {
                if(!data.hasUnlockedAbility(core)){data.addUnlockedAbility(core,AbilityUnlock.PROGRESSION);changed=true;}
            } else {
                IAbility equipped=data.getEquippedAbility(core),passive=data.getPassiveAbility(core);
                if(equipped!=null)AbilityHelper.emergencyStopAbility(user,equipped);
                if(passive!=null)AbilityHelper.emergencyStopAbility(user,passive);
                changed|=equipped!=null||passive!=null||data.hasUnlockedAbility(core);
                data.removeEquippedAbility(core);data.removePassiveAbility(core);data.removeUnlockedAbility(core);
            }
        }
        if (awakened(user)) {
            if (!data.hasUnlockedAbility(LeopardAwakeningAbility.INSTANCE)) {
                data.addUnlockedAbility(LeopardAwakeningAbility.INSTANCE, AbilityUnlock.PROGRESSION);
                changed = true;
            }
        } else {
            IAbility equipped = data.getEquippedAbility(LeopardAwakeningAbility.INSTANCE);
            IAbility passive = data.getPassiveAbility(LeopardAwakeningAbility.INSTANCE);
            if (equipped != null) AbilityHelper.emergencyStopAbility(user, equipped);
            if (passive != null) AbilityHelper.emergencyStopAbility(user, passive);
            changed |= equipped != null || passive != null || data.hasUnlockedAbility(LeopardAwakeningAbility.INSTANCE);
            data.removeEquippedAbility(LeopardAwakeningAbility.INSTANCE);
            data.removePassiveAbility(LeopardAwakeningAbility.INSTANCE);
            data.removeUnlockedAbility(LeopardAwakeningAbility.INSTANCE);
        }
        if (changed) WyNetwork.sendTo(new SSyncAbilityDataPacket(user.getId(), data), user);
    }
    private static boolean migrateAlt(PlayerEntity user, IAbilityData data, AbilityCore<?> oldCore, AbilityCore<?> baseCore) {
        IAbility old = data.getEquippedAbility(oldCore), passive = data.getPassiveAbility(oldCore);
        boolean changed = old != null || passive != null || data.hasUnlockedAbility(oldCore);
        int slot = old == null ? -1 : data.getEquippedAbilitySlot(oldCore);
        if (old != null) AbilityHelper.emergencyStopAbility(user, old);
        if (passive != null) AbilityHelper.emergencyStopAbility(user, passive);
        data.removeEquippedAbility(oldCore); data.removePassiveAbility(oldCore); data.removeUnlockedAbility(oldCore);
        if (awakened(user)) {
            if (!data.hasUnlockedAbility(baseCore)) { data.addUnlockedAbility(baseCore, AbilityUnlock.PROGRESSION); changed = true; }
            IAbility base = data.getEquippedAbility(baseCore);
            if (slot >= 0 && base == null) {
                base = baseCore.createAbility();
                data.setEquippedAbility(slot, base);
                LeopardAltMode.get(base).setMode(user, LeopardAltMode.Mode.AWAKENED);
            }
            if (base != null && old != null) {
                CooldownComponent from = cooldown(old), to = cooldown(base);
                if (from != null && to != null && from.getCooldown() > to.getCooldown()) to.load(from.save());
            }
        }
        return changed;
    }
    private static CooldownComponent cooldown(IAbility ability) {
        return ability.getComponents().values().stream().filter(CooldownComponent.class::isInstance).map(CooldownComponent.class::cast).findFirst().orElse(null);
    }
    private static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level.isClientSide || event.player.tickCount % 20 != 0) return;
        PlayerEntity user = event.player;
        if (fruit(user) || AbilityDataCapability.get(user).hasUnlockedAbility(LeopardAwakeningAbility.INSTANCE)) reconcile(user);
    }
    private static void hurt(LivingHurtEvent event) {
        if (!event.getEntityLiving().level.isClientSide && empowered(event.getEntityLiving()) && !event.getSource().isBypassInvul())
            event.setAmount(event.getAmount() * .75F);
    }
    private LeopardFeatures() {}
}
