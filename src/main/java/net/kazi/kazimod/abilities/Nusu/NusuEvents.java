package net.kazi.kazimod.abilities.Nusu;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUnlock;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.BonusOperation;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.events.ability.UnlockAbilityEvent;
import xyz.pixelatedw.mineminenomi.api.events.onefruit.LostDevilFruitEvent;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.items.AkumaNoMiItem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NusuEvents {

    private static final UUID NUSU_PENALTY_UUID =
            UUID.nameUUIDFromBytes("nusu_stolen_penalty".getBytes());

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new NusuEvents());
        MinecraftForge.EVENT_BUS.register(NusuRemoveCommand.class);
        MinecraftForge.EVENT_BUS.register(NusuStealCommand.class);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntityLiving() instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) event.getEntityLiving();
        if (player.level.isClientSide) return;

        // Strip stolen abilities from victims every tick (still needed for
        // online victims so the ability doesn't briefly re-appear on them)
        if (player.getPersistentData().contains(NusuStolenData.VICTIM_KEY)) {
            stripStolen(player);
        }

        // Apply morph bypass and cooldown penalty to held stolen abilities
        if (player.getPersistentData().contains(NusuStolenData.HELD_KEY)) {
            IAbilityData data = AbilityDataCapability.get(player);
            if (data != null) {
                for (AbilityCore<?> core : NusuStolenData.getHeld(player)) {
                    applyNusuModifiers(player, core, data);
                }
            }
        }
    }

    /**
     * Block stolen ability grant for ANYONE — not just the original victim.
     * This is the critical fix: we check the global world data, so even a brand
     * new player who eats the respawned fruit cannot receive the stolen ability.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onUnlockAbility(UnlockAbilityEvent event) {
        LivingEntity entity = event.getEntityLiving();
        AbilityCore<?> core = event.getAbilityCore();
        if (core == null) return;

        // Check per-victim NBT (original victim still alive)
        if (NusuStolenData.isStolen(entity, core)) {
            event.setResult(Event.Result.DENY);
            return;
        }

// Only relevant on one-fruit-per-world servers — on multi-fruit servers the
// same ability can legitimately exist on multiple players, so we don't block it.
        if (entity.level instanceof ServerWorld
                && xyz.pixelatedw.mineminenomi.config.CommonConfig.INSTANCE.hasOneFruitPerWorldSimpleLogic()) {
            NusuWorldData worldData = NusuWorldData.get((ServerWorld) entity.level);
            if (worldData.isStolen(core)) {
                event.setResult(Event.Result.DENY);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (!(event.getEntity() instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) event.getEntity();
        if (player.level.isClientSide) return;
        if (!player.getPersistentData().contains(NusuStolenData.VICTIM_KEY)) return;

        List<AbilityCore<?>> stolen = NusuStolenData.getStolenFrom(player);
        for (AbilityCore<?> core : stolen) {
            boolean stillHeld = false;
            for (PlayerEntity online : player.level.players()) {
                if (online != player && NusuStolenData.isHeld(online, core)) {
                    stillHeld = true;
                    break;
                }
            }
            if (!stillHeld) {
                returnToVictim(player, core);
                sendMsg(player, "\u00a7a" + core.getLocalizedName().getString()
                        + " has been returned to you (the Nusu user died)");
            }
        }
        stripStolen(player);
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntityLiving();
        if (NusuStolenData.heldCount(dead) == 0) return;
        if (dead.level == null || dead.level.isClientSide) return;

        List<AbilityCore<?>> held = new ArrayList<>(NusuStolenData.getHeld(dead));
        for (AbilityCore<?> core : held) {
            IAbilityData nusuData = AbilityDataCapability.get(dead);
            if (nusuData != null) nusuData.removeUnlockedAbility(core);

            // Try to return to victim if they're online
            boolean returned = false;
            for (PlayerEntity p : dead.level.players()) {
                if (NusuStolenData.isStolen(p, core)) {
                    returnToVictim(p, core);
                    sendMsg(p, "\u00a7a" + core.getLocalizedName().getString()
                            + " has been returned to you");
                    returned = true;
                    break;
                }
            }
            // Whether or not victim was online, unmark from global world data
            // so the fruit can be re-eaten freely again
            if (dead.level instanceof ServerWorld) {
                NusuWorldData.get((ServerWorld) dead.level).unmarkStolen(core);
            }
        }
        NusuStolenData.clearHeld(dead);
    }

    // ── Static API ────────────────────────────────────────────────────────────

    public static int stealAbility(LivingEntity nusuUser, LivingEntity victim, AbilityCore<?> core) {
        if (NusuStolenData.heldCount(nusuUser) >= NusuStolenData.MAX_SLOTS) return -1;

        IAbilityData victimData = AbilityDataCapability.get(victim);
        if (victimData != null) victimData.removeUnlockedAbility(core);

        // Mark on victim entity NBT (strips ability every tick while they're online)
        NusuStolenData.markStolen(victim, core);

        // Mark globally in world data (blocks any new player from getting this ability)
        if (nusuUser.level instanceof ServerWorld) {
            NusuWorldData.get((ServerWorld) nusuUser.level).markStolen(core);
        }

        IAbilityData nusuData = AbilityDataCapability.get(nusuUser);
        if (nusuData != null) {
            nusuData.addUnlockedAbility(core, AbilityUnlock.COMMAND);
            applyNusuModifiers(nusuUser, core, nusuData);
        }

        return NusuStolenData.addHeld(nusuUser, core);
    }

    private static void applyNusuModifiers(LivingEntity nusuUser, AbilityCore<?> core, IAbilityData data) {
        IAbility inst = data.getEquippedAbility(core);
        if (inst == null) inst = data.getPassiveAbility(core);
        if (inst == null) return;

        inst.getComponent(ModAbilityKeys.REQUIRE_MORPH)
                .ifPresent(c -> ((AbilityComponent<?>) c).setDisabled(true));
        inst.getComponent(ModAbilityKeys.REQUIRE_ABILITY)
                .ifPresent(c -> ((AbilityComponent<?>) c).setDisabled(true));

        inst.getComponent(ModAbilityKeys.COOLDOWN).ifPresent(c -> {
            if (c instanceof CooldownComponent) {
                ((CooldownComponent) c).getBonusManager().addBonus(
                        NUSU_PENALTY_UUID,
                        "nusu_stolen_penalty",
                        BonusOperation.MUL,
                        1.5f);
            }
        });
    }

    public static void returnToVictim(LivingEntity victim, AbilityCore<?> core) {
        NusuStolenData.unmarkStolen(victim, core);
        // Also clear global lock so the ability is fully free again
        if (victim.level instanceof ServerWorld) {
            NusuWorldData.get((ServerWorld) victim.level).unmarkStolen(core);
        }
        IAbilityData victimData = AbilityDataCapability.get(victim);
        if (victimData != null) victimData.addUnlockedAbility(core, AbilityUnlock.PROGRESSION);
    }

    public static void discardAbility(LivingEntity nusuUser, AbilityCore<?> core) {
        IAbilityData nusuData = AbilityDataCapability.get(nusuUser);
        if (nusuData != null) nusuData.removeUnlockedAbility(core);
        NusuStolenData.removeHeld(nusuUser, core);
        // Also clear global lock when manually discarding
        if (nusuUser.level instanceof ServerWorld) {
            NusuWorldData.get((ServerWorld) nusuUser.level).unmarkStolen(core);
        }
    }

    private static void stripStolen(PlayerEntity player) {
        IAbilityData data = AbilityDataCapability.get(player);
        if (data == null) return;
        List<AbilityCore<?>> stolen = NusuStolenData.getStolenFrom(player);
        if (stolen.isEmpty()) return;
        for (AbilityCore<?> core : stolen) {
            if (data.hasUnlockedAbility(core)) {
                data.removeUnlockedAbility(core);
            }
        }
    }

    static void sendMsg(LivingEntity entity, String text) {
        if (entity instanceof PlayerEntity) {
            ((PlayerEntity) entity).sendMessage(
                    new StringTextComponent(text), entity.getUUID());
        }
    }

    // ── Add this handler inside NusuEvents ───────────────────────────────────────

    /**
     * If the Nusu user loses their devil fruit (sea prism, admin remove, curse death
     * from eating another fruit, inactivity wipe, etc.), return all stolen abilities
     * immediately. The entity on LostDevilFruitEvent is the one who lost the fruit,
     * so we check if THEY are a Nusu user holding stolen abilities.
     */
    @SubscribeEvent
    public void onLostDevilFruit(LostDevilFruitEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity entity = (LivingEntity) event.getEntity();
        if (entity == null) return;
        if (entity.level == null || entity.level.isClientSide) return;
        if (NusuStolenData.heldCount(entity) == 0) return;

        // Check if the lost fruit IS the Nusu fruit specifically.
        // If an admin wipes someone else's fruit this event fires for that other
        // player — we only care when it fires for a Nusu user losing Nusu no Mi.
        Item lostItem = event.getItem();
        if (!(lostItem instanceof AkumaNoMiItem)) return;

        boolean isNusuFruit = false;
        for (AbilityCore<?> fruitCore : ((AkumaNoMiItem) lostItem).getAbilities()) {
            if (fruitCore == SkillHunterAbility.INSTANCE) {
                isNusuFruit = true;
                break;
            }
        }
        if (!isNusuFruit) return;

        // Return every stolen ability
        List<AbilityCore<?>> held = new ArrayList<>(NusuStolenData.getHeld(entity));
        for (AbilityCore<?> core : held) {
            IAbilityData nusuData = AbilityDataCapability.get(entity);
            if (nusuData != null) nusuData.removeUnlockedAbility(core);

            // Return to victim if online
            for (PlayerEntity p : entity.level.players()) {
                if (NusuStolenData.isStolen(p, core)) {
                    returnToVictim(p, core);
                    sendMsg(p, "\u00a7a" + core.getLocalizedName().getString()
                            + " has been returned to you (Nusu user lost their fruit)");
                    break;
                }
            }

            // Clear global lock regardless of whether victim was online
            if (entity.level instanceof ServerWorld) {
                NusuWorldData.get((ServerWorld) entity.level).unmarkStolen(core);
            }
        }

        NusuStolenData.clearHeld(entity);
        sendMsg(entity, "\u00a7cYou lost your devil fruit — all stolen abilities have been returned");
    }
}