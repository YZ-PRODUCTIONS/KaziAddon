package net.kazi.kazimod.alchemy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.MrMagicalCart.cartaddon.abilities.ishi.CharlestoneAbility;
import net.MrMagicalCart.cartaddon.abilities.ishi.IshiKatachiAbility;
import net.MrMagicalCart.cartaddon.abilities.ishi.PetrificationAbility;
import net.MrMagicalCart.cartaddon.abilities.ishi.StoneItemsAbility;
import net.MrMagicalCart.cartaddon.abilities.ishi.StoneTossAbility;
import net.MrMagicalCart.cartaddon.abilities.ishi.StoneTravelAbility;
import net.MrMagicalCart.cartaddon.init.CartAbilities;
import net.kazi.kazimod.api.KaziRegistry;
import net.kazi.kazimod.events.AwakeningAbilityLoginFix;
import net.kazi.kazimod.items.AwakeningEssenceItem;
import net.kazi.kazimod.util.FruitAbilityInjector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCoreUnlockWrapper;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUnlock;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.enums.AbilityCommandGroup;
import xyz.pixelatedw.mineminenomi.api.events.ability.AbilityUseEvent;
import xyz.pixelatedw.mineminenomi.api.events.ability.UnlockAbilityEvent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.config.CommonConfig;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.packets.server.SSyncAbilityDataPacket;
import xyz.pixelatedw.mineminenomi.packets.server.SSyncDevilFruitPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

public final class AlchemyFeatures {
    public static final String TAG = "KaziAlchemy";
    public static final int AUTOMAIL_BLADE = 0;
    public static final int EARTH_FANG = 1;
    public static final int ALCHEMISTS_BULWARK = 2;
    public static final int STONE_PRISON = 3;
    public static final int FULLMETAL_COMBINATION = 4;
    public static final int GRAND_TRANSMUTATION = 5;
    private static final String FIELD_OWNER_TAG = "KaziAlchemyOwner";
    private static final Set<UUID> USABLE_PLAYERS = new HashSet<>();
    private static final Set<AbilityCore<?>> ORIGINALS = new LinkedHashSet<>(Arrays.asList(
            IshiKatachiAbility.INSTANCE, StoneTossAbility.INSTANCE, CharlestoneAbility.INSTANCE,
            PetrificationAbility.INSTANCE, StoneTravelAbility.INSTANCE, StoneItemsAbility.INSTANCE));
    private static final DeferredRegister<EntityType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITIES, "kazimod");
    public static final RegistryObject<EntityType<AlchemyField>> FIELD = TYPES.register("alchemy_field",
            () -> EntityType.Builder.<AlchemyField>of(AlchemyField::new, EntityClassification.MISC)
                    .sized(.1F, .1F).clientTrackingRange(12).updateInterval(2).noSave().build("alchemy_field"));
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("kazimod", "alchemy"), () -> "1", "1"::equals, "1"::equals);

    public static void init(IEventBus bus) {
        TYPES.register(bus);
        for (AbilityCore<?> core : AlchemyAbility.CORES) KaziRegistry.registerAbility(core);
        AbilityCommandGroup.create("ISHI_AWAKENING", () -> AlchemyAbility.CORES);
        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            for (AbilityCore<?> core : CartAbilities.ISHI_ISHI_NO_MI.getAbilities()) {
                if (!ours(core)) ORIGINALS.add(core);
            }
            FruitAbilityInjector.addAbilities(CartAbilities.ISHI_ISHI_NO_MI, AlchemyAbility.CORES);
            AwakeningAbilityLoginFix.invalidateCache();
        }));
        CHANNEL.registerMessage(0, AlchemyPacket.class, AlchemyPacket::encode, AlchemyPacket::decode,
                AlchemyPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        MinecraftForge.EVENT_BUS.addListener(AlchemyFeatures::tick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, AlchemyFeatures::use);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, (UnlockAbilityEvent event) -> {
            if (blocked(event.getEntityLiving(), event.getAbilityCore())) event.setResult(Event.Result.DENY);
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            clear(event.getPlayer());
            reconcile(event.getPlayer());
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
            clear(event.getPlayer());
            reconcile(event.getPlayer());
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> clear(event.getPlayer()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> clear(event.getPlayer()));
        MinecraftForge.EVENT_BUS.addListener((LivingDeathEvent event) -> {
            if (event.getEntityLiving() instanceof PlayerEntity) clear((PlayerEntity) event.getEntityLiving());
        });
        MinecraftForge.EVENT_BUS.addListener((EntityTravelToDimensionEvent event) -> {
            if (event.getEntity() instanceof PlayerEntity) clear((PlayerEntity) event.getEntity());
        });
        MinecraftForge.EVENT_BUS.addListener((EntityJoinWorldEvent event) -> {
            if (!event.getWorld().isClientSide && event.getEntity() instanceof AlchemyField) {
                AlchemyField field = (AlchemyField) event.getEntity();
                LivingEntity owner = field.owner();
                if (owner != null) field.getPersistentData().putUUID(FIELD_OWNER_TAG, owner.getUUID());
            }
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.StartTracking event) -> {
            if (event.getPlayer() instanceof ServerPlayerEntity && event.getTarget() instanceof PlayerEntity) {
                LivingEntity target = (LivingEntity) event.getTarget();
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) event.getPlayer()),
                        new AlchemyPacket(target.getId(), state(target)));
            }
        });
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> AlchemyClient.init(bus));
    }

    public static boolean fruit(LivingEntity user) {
        if (user == null) return false;
        IDevilFruit fruit = DevilFruitCapability.get(user);
        return fruit != null && fruit.hasDevilFruit(CartAbilities.ISHI_ISHI_NO_MI);
    }

    public static boolean awakened(LivingEntity user) {
        return fruit(user) && DevilFruitCapability.get(user).hasAwakenedFruit();
    }

    public static boolean usable(LivingEntity user) {
        return awakened(user) && user.isAlive() && !user.isSpectator()
                && !AbilityHelper.isWeakenedByKairosekiOrWater(user);
    }

    public static boolean ours(AbilityCore<?> core) {
        return core != null && Arrays.asList(AlchemyAbility.CORES).contains(core);
    }

    public static boolean original(AbilityCore<?> core) {
        return core != null && !ours(core) && (ORIGINALS.contains(core)
                || Arrays.asList(CartAbilities.ISHI_ISHI_NO_MI.getAbilities()).contains(core));
    }

    public static boolean blocked(LivingEntity user, AbilityCore<?> core) {
        return ours(core) ? !awakened(user) : awakened(user) && original(core);
    }

    public static CompoundNBT state(LivingEntity user) {
        if (!user.getPersistentData().contains(TAG, 10)) {
            CompoundNBT state = new CompoundNBT();
            state.putInt("Action", -1);
            user.getPersistentData().put(TAG, state);
        }
        return user.getPersistentData().getCompound(TAG);
    }

    public static int action(LivingEntity user) {
        CompoundNBT state = user.getPersistentData().getCompound(TAG);
        int action = state.contains("Action", 3) ? state.getInt("Action") : -1;
        return action >= 0 && action < AlchemyAbility.CORES.length
                && state.getLong("End") > user.level.getGameTime() ? action : -1;
    }

    public static boolean blade(LivingEntity user) {
        return user.getPersistentData().getCompound(TAG).getBoolean("Blade");
    }

    public static void blade(LivingEntity user, boolean active) {
        if (user.level.isClientSide) return;
        state(user).putBoolean("Blade", active);
        sync(user);
    }

    public static void pose(LivingEntity user, int action, int life) {
        if (user.level.isClientSide) return;
        // Toggling the blade cannot replace another technique's animation.
        if (action == AUTOMAIL_BLADE && action(user) > AUTOMAIL_BLADE) {
            sync(user);
            return;
        }
        boolean active = action >= 0 && action < AlchemyAbility.CORES.length && life > 0;
        CompoundNBT state = state(user);
        state.putInt("Action", active ? action : -1);
        state.putLong("Start", user.level.getGameTime());
        state.putLong("End", user.level.getGameTime() + (active ? life : 0));
        sync(user);
    }

    public static void sync(LivingEntity user) {
        if (user instanceof ServerPlayerEntity) {
            CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> user),
                    new AlchemyPacket(user.getId(), state(user)));
        }
    }

    public static boolean awaken(PlayerEntity user, ItemStack essence) {
        if (user.level.isClientSide || !CommonConfig.INSTANCE.hasAwakeningsEnabled()
                || !fruit(user) || awakened(user) || !user.isAlive() || user.isSpectator()
                || essence.isEmpty() || !(essence.getItem() instanceof AwakeningEssenceItem)
                || AbilityDataCapability.get(user) == null) return false;
        DevilFruitCapability.get(user).setAwakenedFruit(true);
        if (!user.isCreative()) essence.shrink(1);
        clear(user);
        reconcile(user);
        WyNetwork.sendTo(new SSyncDevilFruitPacket(user.getId(), DevilFruitCapability.get(user)), user);
        user.displayClientMessage(new TranslationTextComponent("message.kazimod.alchemy.awakened"), false);
        return true;
    }

    public static void reconcile(PlayerEntity user) {
        if (user == null || user.level.isClientSide) return;
        IAbilityData data = AbilityDataCapability.get(user);
        if (data == null) return;
        boolean awakened = awakened(user);
        if (!awakened && hasAlchemy(data)) clear(user);
        Set<AbilityCore<?>> removals = new LinkedHashSet<>();
        for (AbilityCoreUnlockWrapper<?> unlocked : data.getUnlockedAbilities()) {
            AbilityCore<?> core = unlocked.getAbilityCore();
            if (awakened ? original(core) : ours(core)) removals.add(core);
        }
        for (IAbility ability : data.getEquippedAndPassiveAbilities()) {
            if (awakened ? original(ability.getCore()) : ours(ability.getCore())) removals.add(ability.getCore());
        }

        boolean changed = false;
        List<Integer> freedSlots = new ArrayList<>();
        List<IAbility> equipped = data.getRawEquippedAbilities();
        for (int slot = 0; slot < equipped.size(); slot++) {
            IAbility ability = equipped.get(slot);
            if (ability == null || !removals.contains(ability.getCore())) continue;
            AbilityHelper.emergencyStopAbility(user, ability);
            if (data.getEquippedAbility(slot) == ability) data.setEquippedAbility(slot, null);
            freedSlots.add(slot);
            changed = true;
        }
        for (IAbility ability : new ArrayList<IAbility>(data.getPassiveAbilities())) {
            if (!removals.contains(ability.getCore())) continue;
            AbilityHelper.emergencyStopAbility(user, ability);
            data.removePassiveAbility(ability.getCore());
            changed = true;
        }
        for (AbilityCore<?> core : removals) changed |= data.removeUnlockedAbility(core);

        if (awakened) {
            int nextSlot = 0;
            for (AbilityCore<?> core : AlchemyAbility.CORES) {
                if (!data.hasUnlockedAbility(core)) changed |= data.addUnlockedAbility(core, AbilityUnlock.PROGRESSION);
                if (data.getEquippedAbility(core) != null) continue;
                // Only slots vacated by original Ishi moves are eligible for migration.
                while (nextSlot < freedSlots.size()) {
                    int slot = freedSlots.get(nextSlot++);
                    if (data.getEquippedAbility(slot) != null) continue;
                    changed |= data.setEquippedAbility(slot, core.createAbility());
                    break;
                }
            }
        }
        if (changed) WyNetwork.sendTo(new SSyncAbilityDataPacket(user.getId(), data), user);
    }

    public static void clear(PlayerEntity user) {
        if (user == null || user.level.isClientSide) return;
        USABLE_PLAYERS.remove(user.getUUID());
        IAbilityData data = AbilityDataCapability.get(user);
        if (data != null) {
            for (IAbility ability : new ArrayList<IAbility>(data.getEquippedAndPassiveAbilities())) {
                if (ours(ability.getCore())) AbilityHelper.emergencyStopAbility(user, ability);
            }
        }
        // Persisted bonuses can outlive the equipped ability in older saves.
        removeModifier(user, Attributes.ATTACK_DAMAGE, AlchemyAbility.BLADE_DAMAGE);
        removeModifier(user, ForgeMod.REACH_DISTANCE.get(), AlchemyAbility.BLADE_REACH);
        removeModifier(user, ModAttributes.ATTACK_RANGE.get(), AlchemyAbility.BLADE_REACH);
        state(user).putBoolean("Blade", false);
        state(user).putBoolean("Interrupted", false);
        state(user).remove("ComboContact");
        pose(user, -1, 0);
        if (user.getServer() != null) {
            for (ServerWorld world : user.getServer().getAllLevels()) {
                for (Entity entity : world.getEntities(FIELD.get(), field -> ownedBy((AlchemyField) field, user))) {
                    entity.remove();
                }
            }
        }
    }

    private static boolean ownedBy(AlchemyField field, PlayerEntity user) {
        LivingEntity owner = field.owner();
        return owner != null && user.getUUID().equals(owner.getUUID())
                || field.getPersistentData().hasUUID(FIELD_OWNER_TAG)
                && user.getUUID().equals(field.getPersistentData().getUUID(FIELD_OWNER_TAG));
    }

    private static void removeModifier(LivingEntity user, Attribute attribute, UUID id) {
        ModifiableAttributeInstance instance = user.getAttribute(attribute);
        if (instance != null && instance.getModifier(id) != null) instance.removeModifier(id);
    }

    private static boolean hasAlchemy(IAbilityData data) {
        for (AbilityCore<?> core : AlchemyAbility.CORES) {
            if (data.hasUnlockedAbility(core) || data.getEquippedAbility(core) != null
                    || data.getPassiveAbility(core) != null) return true;
        }
        return false;
    }

    private static void use(AbilityUseEvent.Pre event) {
        LivingEntity user = event.getEntityLiving();
        AbilityCore<?> core = event.getAbility().getCore();
        int action = action(user);
        if (blocked(user, core) || ours(core) && !usable(user)
                || awakened(user) && action >= AUTOMAIL_BLADE && core != AlchemyAbility.CORES[action]
                && core != AlchemyAbility.CORES[AUTOMAIL_BLADE]) event.setCanceled(true);
    }

    private static void tick(TickEvent.PlayerTickEvent event) {
        PlayerEntity user = event.player;
        if (event.phase != TickEvent.Phase.END || user.level.isClientSide) return;
        IAbilityData data = AbilityDataCapability.get(user);
        if (data == null) return;
        boolean eligible = awakened(user);
        boolean usable = usable(user);
        if (usable) USABLE_PLAYERS.add(user.getUUID());
        else if (USABLE_PLAYERS.remove(user.getUUID()) || blade(user) || action(user) >= 0) clear(user);
        if (!eligible && hasAlchemy(data)) reconcile(user);
        else if (fruit(user) && user.tickCount % 20 == 0) reconcile(user);
    }

    private AlchemyFeatures() {}
}
