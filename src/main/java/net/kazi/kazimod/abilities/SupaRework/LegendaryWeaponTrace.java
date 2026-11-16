package net.kazi.kazimod.abilities.SupaRework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.MrMagicalCart.cartaddon.abilities.dagger.FinalExecutionAbility;
import net.MrMagicalCart.cartaddon.abilities.greatblade.VoidShatteringSlashAbility;
import net.MrMagicalCart.cartaddon.abilities.nitoryu.MixedBumakiAbility;
import net.MrMagicalCart.cartaddon.abilities.nitoryu.ParadiseTotsukaAbility;
import net.MrMagicalCart.cartaddon.abilities.ryusoken.DragonsWrathAbility;
import net.kazi.kazimod.abilities.BludgeonRework.DestroyerOfDeathThunderBaguaRework;
import net.kazi.kazimod.abilities.BludgeonRework.ShinsokuHakujakuRework;
import net.kazi.kazimod.abilities.BrawlerRework.GalaxyImpactRework;
import net.kazi.kazimod.abilities.Nusu.SkillHunterEXAbility;
import net.kazi.kazimod.abilities.SaberRework.DivineDepartureClone;
import net.kazi.kazimod.abilities.SpearRework.AbsolutePierceRework;
import net.kazi.kazimod.abilities.SpearRework.SkySplitterDescentRework;
import net.kazi.kazimod.init.KaziPacketHandler;
import net.kazi.kazimod.network.LegendaryWeaponTracePacket;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;
import xyz.pixelatedw.mineminenomi.api.ModRegistries;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUnlock;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.events.ability.AbilityUseEvent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.config.CommonConfig;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.packets.server.SSyncAbilityDataPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

/** A temporary, single-use slot replacement following Skill Hunter EX's controls. */
@Mod.EventBusSubscriber(modid = "kazimod")
public final class LegendaryWeaponTrace {
    public static final String TAG = "kazimodLegendaryWeaponTrace";
    public static final String CYCLE_TAG = "kazimodLegendaryWeaponTraceCycle";
    public static final long LIFETIME_TICKS = SkillHunterEXAbility.ROLLED_LIFETIME_TICKS;
    public static final float UNUSED_COOLDOWN = SkillHunterEXAbility.COOLDOWN;

    private LegendaryWeaponTrace() {}

    // Deferred initialization avoids referencing ability cores during their registration.
    private static final class Pool {
        private static final List<AbilityCore<?>> MOVES = Arrays.asList(
                GalaxyImpactRework.INSTANCE, DestroyerOfDeathThunderBaguaRework.INSTANCE,
                ShinsokuHakujakuRework.INSTANCE, ParadiseTotsukaAbility.INSTANCE,
                MixedBumakiAbility.INSTANCE, DragonsWrathAbility.INSTANCE,
                FinalExecutionAbility.INSTANCE, DivineDepartureClone.INSTANCE,
                VoidShatteringSlashAbility.INSTANCE, AbsolutePierceRework.INSTANCE,
                SkySplitterDescentRework.INSTANCE);
    }

    public static boolean isMarbleActive(LivingEntity user) {
        if (user == null || !user.isAlive() || user.isSpectator()) return false;
        IAbilityData data = AbilityDataCapability.get(user);
        if (data == null) return false;
        // Match MMNM's command-unlock rules for Projection, including while its
        // slot temporarily holds a traced move instead of Projection itself.
        if (data.getUnlockTypeForAbility(ProjectionAbility.INSTANCE) != AbilityUnlock.COMMAND) {
            IDevilFruit fruit = DevilFruitCapability.get(user);
            if (fruit == null || !fruit.hasAwakenedFruit() || !fruit.hasDevilFruit(ModAbilities.SUPA_SUPA_NO_MI)) return false;
        }
        RealityMarbleAbility marble = data.getEquippedAbility(RealityMarbleAbility.INSTANCE);
        return marble != null && (marble.isModeActive(RealityMarbleAbility.Mode.MUGEN_NO_KENSEI)
                || marble.isModeActive(RealityMarbleAbility.Mode.INFINITE_CREATION_OF_SWORDS));
    }

    public static boolean hasActiveRoll(LivingEntity user) {
        return user != null && user.getPersistentData().contains(TAG, 10);
    }

    /** Matches the actual equipped loan, including when it is moved to another slot. */
    public static boolean isRolledMove(LivingEntity user, IAbility ability) {
        if (!hasActiveRoll(user) || ability == null) return false;
        CompoundNBT state = user.getPersistentData().getCompound(TAG);
        if (state.getBoolean("returning") || !matches(state, ability)) return false;
        IAbilityData data = AbilityDataCapability.get(user);
        return data != null && data.getEquippedAbility(ability.getCore()) == ability;
    }

    public static boolean canUseRolledMove(LivingEntity user, IAbility ability) {
        if (!isRolledMove(user, ability) || !isMarbleActive(user)) return false;
        CompoundNBT state = user.getPersistentData().getCompound(TAG);
        if (!sameDimension(user, state)) return false;
        // A use-state packet can precede the native client charge/use packet.
        // The server alone enforces single-use; clients must still start its visuals.
        return state.getBoolean("used") ? user.level.isClientSide || isRunning(ability)
                : user.level.getGameTime() < state.getLong("expiry");
    }

    /** End callbacks still clean up, but must not fire a discarded attack. */
    public static boolean isReturningMove(LivingEntity user, IAbility ability) {
        if (!hasActiveRoll(user) || ability == null) return false;
        CompoundNBT state = user.getPersistentData().getCompound(TAG);
        IAbilityData data = AbilityDataCapability.get(user);
        return state.getBoolean("returning") && matches(state, ability)
                && data != null && data.getEquippedAbility(ability.getCore()) == ability;
    }

    public static void roll(PlayerEntity user, ProjectionAbility projection) {
        if (user.level.isClientSide || !isMarbleActive(user)) return;
        if (hasActiveRoll(user)) {
            message(user, "ability.kazimod.legendary_weapon_trace.already_rolled");
            return;
        }
        IAbilityData data = AbilityDataCapability.get(user);
        if (data == null) return;
        int slot = data.getEquippedAbilitySlot(projection);
        if (slot < 0 || data.getEquippedAbility(slot) != projection) return;
        List<AbilityCore<?>> choices = new ArrayList<>();
        for (AbilityCore<?> core : Pool.MOVES) {
            if (CommonConfig.INSTANCE.isAbilityFraudChecksEnabled() && AbilityHelper.verifyIfAbilityIsBanned(core)) continue;
            // MMNM cannot equip two instances of one core. Never replace an existing move.
            if (ModRegistries.ABILITIES.getKey(core) != null
                    && !data.hasEquippedAbility(core) && !data.hasPassiveAbility(core)) choices.add(core);
        }
        if (choices.isEmpty()) {
            message(user, "ability.kazimod.legendary_weapon_trace.no_moves");
            return;
        }
        List<AbilityCore<?>> remaining = remainingCycle(user, choices);
        AbilityCore<?> chosen = remaining.remove(user.getRandom().nextInt(remaining.size()));
        CompoundNBT state = new CompoundNBT();
        state.putString("selected", ModRegistries.ABILITIES.getKey(chosen).toString());
        state.putInt("slot", slot);
        state.put("projection", saveProjection(projection));
        state.putBoolean("hadUnlock", data.hasUnlockedAbility(chosen));
        state.putLong("expiry", user.level.getGameTime() + LIFETIME_TICKS);
        state.putString("dimension", user.level.dimension().location().toString());
        user.getPersistentData().put(TAG, state);
        if (!state.getBoolean("hadUnlock")) data.addUnlockedAbility(chosen, AbilityUnlock.COMMAND);
        IAbility rolled = chosen.createAbility();
        rolled.getComponent(ModAbilityKeys.COOLDOWN).ifPresent(component ->
                component.addStartEvent(LegendaryWeaponTrace::rememberCooldown));
        if (!data.hasUnlockedAbility(chosen) || !data.setEquippedAbility(slot, rolled)) {
            if (!state.getBoolean("hadUnlock")) data.removeUnlockedAbility(chosen);
            user.getPersistentData().remove(TAG);
            message(user, "ability.kazimod.legendary_weapon_trace.failed");
            return;
        }
        // Only a successful grant consumes its place in the cycle.
        saveCycle(user, remaining);
        syncState(user);
        syncAbilities(user, data);
        message(user, "ability.kazimod.legendary_weapon_trace.rolled", chosen.getLocalizedName(), LIFETIME_TICKS / 20);
    }

    /** Drawing without replacement produces a shuffled cycle, including across relogs. */
    private static List<AbilityCore<?>> remainingCycle(PlayerEntity user, List<AbilityCore<?>> choices) {
        CompoundNBT saved = user.getPersistentData().getCompound(CYCLE_TAG);
        List<AbilityCore<?>> remaining = new ArrayList<>();
        if (poolSignature().equals(saved.getString("pool"))) {
            for (String id : saved.getString("remaining").split(",")) {
                for (AbilityCore<?> core : choices) {
                    if (id.equals(ModRegistries.ABILITIES.getKey(core).toString()) && !remaining.contains(core)) {
                        remaining.add(core);
                        break;
                    }
                }
            }
        }
        // Banned or already equipped moves cannot stall the cycle. Newly available
        // moves join the next cycle once every remaining eligible move has appeared.
        return remaining.isEmpty() ? new ArrayList<>(choices) : remaining;
    }

    private static void saveCycle(PlayerEntity user, List<AbilityCore<?>> remaining) {
        CompoundNBT saved = new CompoundNBT();
        saved.putString("pool", poolSignature());
        saved.putString("remaining", registryIds(remaining));
        user.getPersistentData().put(CYCLE_TAG, saved);
    }

    private static String poolSignature() {
        return registryIds(Pool.MOVES);
    }

    private static String registryIds(List<AbilityCore<?>> cores) {
        List<String> ids = new ArrayList<>();
        for (AbilityCore<?> core : cores) {
            ResourceLocation id = ModRegistries.ABILITIES.getKey(core);
            if (id != null) ids.add(id.toString());
        }
        return String.join(",", ids);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforeUse(AbilityUseEvent.Pre event) {
        LivingEntity entity = event.getEntityLiving();
        if (!(entity instanceof PlayerEntity) || entity.level.isClientSide || !hasActiveRoll(entity)) return;
        CompoundNBT state = entity.getPersistentData().getCompound(TAG);
        if (!matches(state, event.getAbility())) return;
        PlayerEntity user = (PlayerEntity) entity;
        if (!canUseRolledMove(user, event.getAbility()) || user.isShiftKeyDown()) {
            event.setCanceled(true);
            beginReturn(user);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void afterUse(AbilityUseEvent.Post event) {
        LivingEntity user = event.getEntityLiving();
        if (user.level.isClientSide || !isRolledMove(user, event.getAbility())) return;
        CompoundNBT state = user.getPersistentData().getCompound(TAG);
        state.putBoolean("used", true);
        state.putLong("castTick", user.level.getGameTime());
        rememberCooldown(user, event.getAbility());
        syncState(user);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        PlayerEntity user = event.player;
        if (event.phase != TickEvent.Phase.END || user.level.isClientSide || !hasActiveRoll(user)) return;
        CompoundNBT state = user.getPersistentData().getCompound(TAG);
        if (state.getBoolean("returning")) {
            restoreProjection(user, state);
            return;
        }
        IAbilityData data = AbilityDataCapability.get(user);
        IAbility rolled = findRolled(data, state);
        if (!isMarbleActive(user) || !sameDimension(user, state) || rolled == null) {
            beginReturn(user);
        } else if (!state.getBoolean("used") && user.level.getGameTime() >= state.getLong("expiry")) {
            message(user, "ability.kazimod.legendary_weapon_trace.expired");
            beginReturn(user);
        } else if (state.getBoolean("used") && user.level.getGameTime() > state.getLong("castTick")
                && !isRunning(rolled)) {
            beginReturn(user);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        if (event.getEntityLiving() instanceof PlayerEntity) beginReturn((PlayerEntity) event.getEntityLiving());
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { beginReturn(event.getPlayer()); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { beginReturn(event.getPlayer()); }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { beginReturn(event.getPlayer()); }

    @SubscribeEvent
    public static void tracking(PlayerEvent.StartTracking event) {
        if (!(event.getPlayer() instanceof ServerPlayerEntity) || !(event.getTarget() instanceof PlayerEntity)) return;
        PlayerEntity target = (PlayerEntity) event.getTarget();
        if (!hasActiveRoll(target)) return;
        ServerPlayerEntity viewer = (ServerPlayerEntity) event.getPlayer();
        KaziPacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer),
                new LegendaryWeaponTracePacket(target.getId(), clientState(target)));
        IAbilityData data = AbilityDataCapability.get(target);
        if (data != null) WyNetwork.sendTo(new SSyncAbilityDataPacket(target.getId(), data), viewer);
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        // Pending restoration survives a respawn even if every equipped slot was occupied.
        PlayerEntity old = event.getOriginal(), replacement = event.getPlayer();
        if (old.getPersistentData().contains(CYCLE_TAG, 10))
            replacement.getPersistentData().put(CYCLE_TAG, old.getPersistentData().getCompound(CYCLE_TAG).copy());
        if (hasActiveRoll(old) && !hasActiveRoll(replacement))
            replacement.getPersistentData().put(TAG, old.getPersistentData().getCompound(TAG).copy());
        beginReturn(replacement);
    }

    private static void beginReturn(PlayerEntity user) {
        if (user.level.isClientSide || !hasActiveRoll(user)) return;
        IAbilityData data = AbilityDataCapability.get(user);
        if (data == null) return;
        CompoundNBT state = user.getPersistentData().getCompound(TAG);
        if (!state.getBoolean("returning")) {
            IAbility rolled = findRolled(data, state);
            if (rolled != null && state.getBoolean("used")) rememberCooldown(user, rolled);
            // Mark before callbacks: emergency cleanup can recursively cause ability events.
            state.putBoolean("returning", true);
            // Clients run native end callbacks too. Deliver the cancellation marker first.
            syncState(user);
            AbilityCore<?> chosen = resolveCore(state);
            List<IAbility> equipped = data.getRawEquippedAbilities();
            for (int slot = 0; slot < equipped.size(); slot++) {
                IAbility ability = equipped.get(slot);
                if (!matches(state, ability)) continue;
                AbilityHelper.emergencyStopAbility(user, ability);
                data.setEquippedAbility(slot, null);
            }
            if (chosen != null && !state.getBoolean("hadUnlock")) data.removeUnlockedAbility(chosen);
        }
        restoreProjection(user, state);
    }

    private static void restoreProjection(PlayerEntity user, CompoundNBT state) {
        IAbilityData data = AbilityDataCapability.get(user);
        if (data == null) return;
        // Fruit loss must not re-grant a devil-fruit move that was legitimately removed.
        if (!data.hasUnlockedAbility(ProjectionAbility.INSTANCE)) {
            user.getPersistentData().remove(TAG);
            syncState(user);
            syncAbilities(user, data);
            return;
        }
        ProjectionAbility restored = data.getEquippedAbility(ProjectionAbility.INSTANCE);
        if (restored == null) {
            List<IAbility> equipped = data.getRawEquippedAbilities();
            int slot = state.getInt("slot");
            if (slot < 0 || slot >= equipped.size() || equipped.get(slot) != null) {
                slot = -1;
                for (int i = 0; i < equipped.size(); i++) if (equipped.get(i) == null) { slot = i; break; }
            }
            // Keep recovery state until a slot is free; never overwrite a different move.
            if (slot < 0) return;
            restored = ProjectionAbility.INSTANCE.createAbility();
            restored.restoreTraceState(state.getCompound("projection"));
            if (!data.setEquippedAbility(slot, restored)) return;
        }
        float cooldown = state.getBoolean("used") && state.contains("returnCooldownTicks")
                ? state.getFloat("returnCooldownTicks") : UNUSED_COOLDOWN;
        restored.startTraceReturnCooldown(user, cooldown);
        user.getPersistentData().remove(TAG);
        syncState(user);
        syncAbilities(user, data);
    }

    private static CompoundNBT saveProjection(ProjectionAbility ability) {
        CompoundNBT saved = ability.save(new CompoundNBT()), components = new CompoundNBT();
        ability.getComponents().forEach((key, component) -> {
            CompoundNBT value = component.save();
            if (value != null) components.put(key.getId().toString(), value);
        });
        saved.put("components", components);
        return saved;
    }

    private static IAbility findRolled(IAbilityData data, CompoundNBT state) {
        if (data == null) return null;
        for (IAbility ability : data.getRawEquippedAbilities()) if (matches(state, ability)) return ability;
        return null;
    }

    private static boolean matches(CompoundNBT state, IAbility ability) {
        if (ability == null) return false;
        ResourceLocation key = ModRegistries.ABILITIES.getKey(ability.getCore());
        return key != null && key.toString().equals(state.getString("selected"));
    }

    private static AbilityCore<?> resolveCore(CompoundNBT state) {
        try { return ModRegistries.ABILITIES.getValue(new ResourceLocation(state.getString("selected"))); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private static boolean sameDimension(LivingEntity user, CompoundNBT state) {
        return user.level.dimension().location().toString().equals(state.getString("dimension"));
    }

    private static boolean isRunning(IAbility ability) {
        return ability.getComponent(ModAbilityKeys.CHARGE).map(ChargeComponent::isCharging).orElse(false)
                || ability.getComponent(ModAbilityKeys.CONTINUOUS).map(ContinuousComponent::isContinuous).orElse(false);
    }

    private static void rememberCooldown(LivingEntity user, IAbility ability) {
        if (user.level.isClientSide || !isRolledMove(user, ability)) return;
        float original = ability.getComponent(ModAbilityKeys.COOLDOWN)
                .map(CooldownComponent::getStartCooldown).orElse(0.0F);
        // Use the full native cooldown, not its remaining time. Capture it before
        // emergency cleanup can replace it with the framework's interruption penalty.
        if (Float.isFinite(original) && original > 0.0F)
            user.getPersistentData().getCompound(TAG).putFloat("returnCooldownTicks", original * 0.5F);
    }

    private static CompoundNBT clientState(LivingEntity user) {
        CompoundNBT visible = new CompoundNBT();
        if (!hasActiveRoll(user)) return visible;
        CompoundNBT state = user.getPersistentData().getCompound(TAG);
        visible.putString("selected", state.getString("selected"));
        visible.putString("dimension", state.getString("dimension"));
        visible.putLong("expiry", state.getLong("expiry"));
        visible.putBoolean("used", state.getBoolean("used"));
        visible.putBoolean("returning", state.getBoolean("returning"));
        return visible;
    }

    private static void syncState(LivingEntity user) {
        KaziPacketHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> user),
                new LegendaryWeaponTracePacket(user.getId(), clientState(user)));
    }

    private static void syncAbilities(PlayerEntity user, IAbilityData data) {
        WyNetwork.sendToAllTrackingAndSelf(new SSyncAbilityDataPacket(user.getId(), data), user);
    }

    private static void message(PlayerEntity user, String key, Object... args) {
        user.sendMessage(new TranslationTextComponent(key, args), user.getUUID());
    }
}
