package net.kazi.kazimod.abilities.Nusu;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.events.ability.AbilityUseEvent;
import xyz.pixelatedw.mineminenomi.api.events.ability.UnlockAbilityEvent;
import xyz.pixelatedw.mineminenomi.api.events.onefruit.LostDevilFruitEvent;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.items.AkumaNoMiItem;
import xyz.pixelatedw.mineminenomi.api.ModRegistries;
import xyz.pixelatedw.mineminenomi.packets.server.SSyncAbilityDataPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

import java.util.ArrayList;
import java.util.Arrays;
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

        finishUsedSkillHunterExRoll(player);

        if (player.getPersistentData().contains(NusuStolenData.VICTIM_KEY)) {
            stripStolen(player);
        }

        if (player.getPersistentData().contains(NusuStolenData.HELD_KEY)) {
            IAbilityData data = AbilityDataCapability.get(player);
            if (data != null) {
                for (AbilityCore<?> core : NusuStolenData.getHeld(player)) {
                    applyNusuModifiers(player, core, data);
                }
            }

            if (!hasSkillBookInInventory(player)) {
                IAbilityData data2 = AbilityDataCapability.get(player);
                if (data2 != null) {
                    for (AbilityCore<?> core : NusuStolenData.getHeld(player)) {
                        IAbility inst = data2.getEquippedAbility(core);
                        if (inst == null) inst = data2.getPassiveAbility(core);
                        if (inst == null) continue;
                        inst.getComponent(ModAbilityKeys.DISABLE).ifPresent(c ->
                                ((AbilityComponent<?>) c).setDisabled(true));
                    }
                }
            } else {
                IAbilityData data2 = AbilityDataCapability.get(player);
                if (data2 != null) {
                    for (AbilityCore<?> core : NusuStolenData.getHeld(player)) {
                        IAbility inst = data2.getEquippedAbility(core);
                        if (inst == null) inst = data2.getPassiveAbility(core);
                        if (inst == null) continue;
                        inst.getComponent(ModAbilityKeys.DISABLE).ifPresent(c ->
                                ((AbilityComponent<?>) c).setDisabled(false));
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onUnlockAbility(UnlockAbilityEvent event) {
        LivingEntity entity = event.getEntityLiving();
        AbilityCore<?> core = event.getAbilityCore();
        if (core == null) return;

        if (NusuStolenData.isStolen(entity, core)) {
            event.setResult(Event.Result.DENY);
            return;
        }

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
        if (dead.level == null || dead.level.isClientSide) return;

        tryOpenKillStealSelection(event);

        if (NusuStolenData.heldCount(dead) > 0) {
            List<AbilityCore<?>> held = new ArrayList<>(NusuStolenData.getHeld(dead));
            for (AbilityCore<?> core : held) {
                IAbilityData nusuData = AbilityDataCapability.get(dead);
                if (nusuData != null) nusuData.removeUnlockedAbility(core);

                for (PlayerEntity p : dead.level.players()) {
                    if (NusuStolenData.isStolen(p, core)) {
                        returnToVictim(p, core);
                        sendMsg(p, "\u00a7a" + core.getLocalizedName().getString()
                                + " has been returned to you");
                        break;
                    }
                }
                if (dead.level instanceof ServerWorld) {
                    NusuWorldData.get((ServerWorld) dead.level).unmarkStolen(core);
                }
            }
            NusuStolenData.clearHeld(dead);
        }
    }

    // ── Static API ────────────────────────────────────────────────────────────

    public static int stealAbility(LivingEntity nusuUser, LivingEntity victim, AbilityCore<?> core) {
        if (NusuStolenData.heldCount(nusuUser) >= NusuStolenData.MAX_SLOTS) return -1;

        IAbilityData victimData = AbilityDataCapability.get(victim);
        if (victimData != null) victimData.removeUnlockedAbility(core);

        NusuStolenData.markStolen(victim, core);

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

    @SubscribeEvent
    public void onAbilityUse(AbilityUseEvent.Post event) {
        LivingEntity entity = event.getEntityLiving();
        if (entity.level == null || entity.level.isClientSide) return;
        String rolledKey = entity.getPersistentData().getString(
                SkillHunterEXAbility.ROLLED_ABILITY_TAG);
        ResourceLocation usedKey = ModRegistries.ABILITIES.getKey(event.getAbility().getCore());
        if (usedKey != null && usedKey.toString().equals(rolledKey)) {
            entity.getPersistentData().putBoolean(SkillHunterEXAbility.ROLLED_USED_TAG, true);
        }
    }

    /** Sneak-use discards the temporary EX roll instead of firing it. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSkillHunterExDiscard(AbilityUseEvent.Pre event) {
        LivingEntity entity = event.getEntityLiving();
        if (!(entity instanceof PlayerEntity) || entity.level.isClientSide || !entity.isShiftKeyDown()) return;

        String rolledKey = entity.getPersistentData().getString(SkillHunterEXAbility.ROLLED_ABILITY_TAG);
        ResourceLocation usedKey = ModRegistries.ABILITIES.getKey(event.getAbility().getCore());
        if (rolledKey.isEmpty() || usedKey == null || !rolledKey.equals(usedKey.toString())) return;

        event.getAbility().getComponent(ModAbilityKeys.CHARGE).ifPresent(component -> {
            ChargeComponent charge = (ChargeComponent) component;
            if (charge.isCharging()) charge.stopCharging(entity);
        });
        event.getAbility().getComponent(ModAbilityKeys.CONTINUOUS).ifPresent(component -> {
            ContinuousComponent continuous = (ContinuousComponent) component;
            if (continuous.isContinuous()) continuous.stopContinuity(entity);
        });
        entity.getPersistentData().putBoolean(SkillHunterEXAbility.ROLLED_USED_TAG, true);
        event.setCanceled(true);
    }

    private static void finishUsedSkillHunterExRoll(PlayerEntity player) {
        if (!player.getPersistentData().getBoolean(SkillHunterEXAbility.ROLLED_USED_TAG)) return;

        IAbilityData data = AbilityDataCapability.get(player);
        if (data == null) return;
        ResourceLocation rolledKey;
        try {
            rolledKey = new ResourceLocation(player.getPersistentData().getString(
                    SkillHunterEXAbility.ROLLED_ABILITY_TAG));
        } catch (Exception ignored) {
            clearSkillHunterExRoll(player);
            return;
        }

        AbilityCore<?> rolledCore = ModRegistries.ABILITIES.getValue(rolledKey);
        int slot = player.getPersistentData().getInt(SkillHunterEXAbility.ROLLED_SLOT_TAG);
        List<IAbility> equipped = data.getRawEquippedAbilities();
        if (rolledCore == null || slot < 0 || slot >= equipped.size()) {
            clearSkillHunterExRoll(player);
            return;
        }

        IAbility rolledAbility = equipped.get(slot);
        if (rolledAbility == null || rolledAbility.getCore() != rolledCore) {
            clearSkillHunterExRoll(player);
            return;
        }
        if (isStillRunning(rolledAbility)) return;

        data.removeEquippedAbility(rolledCore);
        data.removeUnlockedAbility(rolledCore);
        SkillHunterEXAbility restored = SkillHunterEXAbility.INSTANCE.createAbility();
        restored.startReturnCooldown(player);
        data.setEquippedAbility(slot, restored);
        clearSkillHunterExRoll(player);
        syncAbilityData(player, data);
    }

    private static boolean isStillRunning(IAbility ability) {
        boolean charging = ability.getComponent(ModAbilityKeys.CHARGE)
                .map(component -> ((ChargeComponent) component).isCharging()).orElse(false);
        boolean continuous = ability.getComponent(ModAbilityKeys.CONTINUOUS)
                .map(component -> ((ContinuousComponent) component).isContinuous()).orElse(false);
        return charging || continuous;
    }

    private static void clearSkillHunterExRoll(LivingEntity entity) {
        SkillHunterEXAbility.restoreWeatherTool(entity);
        entity.getPersistentData().remove(SkillHunterEXAbility.ROLLED_ABILITY_TAG);
        entity.getPersistentData().remove(SkillHunterEXAbility.ROLLED_SLOT_TAG);
        entity.getPersistentData().remove(SkillHunterEXAbility.ROLLED_USED_TAG);
    }

    public static void syncAbilityData(PlayerEntity player, IAbilityData data) {
        if (player instanceof net.minecraft.entity.player.ServerPlayerEntity) {
            WyNetwork.sendTo(new SSyncAbilityDataPacket(player.getId(), data),
                    (net.minecraft.entity.player.ServerPlayerEntity) player);
        }
    }

    private static void applyNusuModifiers(LivingEntity nusuUser, AbilityCore<?> core, IAbilityData data) {
        IAbility inst = data.getEquippedAbility(core);
        if (inst == null) inst = data.getPassiveAbility(core);
        if (inst == null) return;

        // The RequireMorphComponent.postInit() adds a canUse lambda directly onto
        // the ability via addCanUseCheck(). setDisabled() has no effect because
        // the lambda never reads isDisabled. The only fix is to remove that lambda
        // from the ability's onCanUseEvents list using reflection.
        if (inst instanceof xyz.pixelatedw.mineminenomi.api.abilities.Ability) {
            try {
                java.lang.reflect.Field eventsField =
                        xyz.pixelatedw.mineminenomi.api.abilities.Ability.class
                                .getDeclaredField("onCanUseEvents");
                eventsField.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.List<Object> events = (java.util.List<Object>) eventsField.get(inst);
                // Remove any canUse check whose class name contains RequireMorphComponent
                // (these are lambdas captured by RequireMorphComponent.postInit)
                events.removeIf(e -> e.getClass().getName().contains("RequireMorphComponent"));
            } catch (Exception ignored) {}
        }

        // Disable pool component — this one does respect setDisabled
        inst.getComponent(ModAbilityKeys.POOL).ifPresent(c ->
                ((AbilityComponent<?>) c).setDisabled(true));

        // Cooldown penalty
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
        if (victim.level instanceof ServerWorld) {
            NusuWorldData.get((ServerWorld) victim.level).unmarkStolen(core);
        }

        if (victimStillHasFruitForAbility(victim, core)) {
            IAbilityData victimData = AbilityDataCapability.get(victim);
            if (victimData != null) victimData.addUnlockedAbility(core, AbilityUnlock.PROGRESSION);
        }
    }

    private static boolean victimStillHasFruitForAbility(LivingEntity victim, AbilityCore<?> core) {
        try {
            IDevilFruit devilFruit = DevilFruitCapability.get(victim);
            if (devilFruit == null) return false;
            Item item = devilFruit.getDevilFruitItem();
            if (!(item instanceof AkumaNoMiItem)) return false;
            AbilityCore<?>[] abilities = ((AkumaNoMiItem) item).getAbilities();
            if (abilities == null) return false;
            return Arrays.asList(abilities).contains(core);
        } catch (Exception e) {
            return false;
        }
    }

    public static void discardAbility(LivingEntity nusuUser, AbilityCore<?> core) {
        IAbilityData nusuData = AbilityDataCapability.get(nusuUser);
        if (nusuData != null) nusuData.removeUnlockedAbility(core);
        NusuStolenData.removeHeld(nusuUser, core);
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

    public static boolean hasSkillBookInInventory(PlayerEntity player) {
        for (ItemStack stack : player.inventory.items) {
            if (stack.getItem() instanceof net.kazi.kazimod.items.SkillBookItem) return true;
        }
        for (ItemStack stack : player.inventory.offhand) {
            if (stack.getItem() instanceof net.kazi.kazimod.items.SkillBookItem) return true;
        }
        return false;
    }

    static void sendMsg(LivingEntity entity, String text) {
        if (entity instanceof PlayerEntity) {
            ((PlayerEntity) entity).sendMessage(
                    new StringTextComponent(text), entity.getUUID());
        }
    }

    private void tryOpenKillStealSelection(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof PlayerEntity)) return;
        if (!(event.getSource().getEntity() instanceof PlayerEntity)) return;

        PlayerEntity victim = (PlayerEntity) event.getEntityLiving();
        PlayerEntity killer = (PlayerEntity) event.getSource().getEntity();
        if (killer == victim) return;
        if (!isNusuUser(killer)) return;
        if (NusuStolenData.heldCount(killer) >= NusuStolenData.MAX_SLOTS) {
            sendMsg(killer, "\u00a7cAll " + NusuStolenData.MAX_SLOTS + " stolen ability slots are full");
            return;
        }

        List<AbilityCore<?>> choices = SkillHunterAbility.getEligibleStealChoices(killer, victim);
        if (choices.isEmpty()) return;

        SkillHunterAbility.openStealSelection(
                killer,
                victim,
                choices,
                "Nusu Kill - Choose ability to steal"
        );
        sendMsg(killer, "\u00a76You killed " + victim.getName().getString() + " - choose a fruit ability to steal");
    }

    public static boolean isNusuUser(LivingEntity entity) {
        try {
            IDevilFruit devilFruit = DevilFruitCapability.get(entity);
            if (devilFruit == null) return false;
            return devilFruit.getDevilFruitItem() == net.kazi.kazimod.init.KaziItems.NUSU_NUSU_NO_MI.get();
        } catch (Exception e) {
            return false;
        }
    }

    @SubscribeEvent
    public void onLostDevilFruit(LostDevilFruitEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity entity = (LivingEntity) event.getEntity();
        if (entity == null) return;
        if (entity.level == null || entity.level.isClientSide) return;
        if (NusuStolenData.heldCount(entity) == 0) return;

        Item lostItem = event.getItem();
        if (!(lostItem instanceof AkumaNoMiItem)) return;

        if (lostItem != net.kazi.kazimod.init.KaziItems.NUSU_NUSU_NO_MI.get()) return;

        List<AbilityCore<?>> held = new ArrayList<>(NusuStolenData.getHeld(entity));
        for (AbilityCore<?> core : held) {
            IAbilityData nusuData = AbilityDataCapability.get(entity);
            if (nusuData != null) nusuData.removeUnlockedAbility(core);

            for (PlayerEntity p : entity.level.players()) {
                if (NusuStolenData.isStolen(p, core)) {
                    returnToVictim(p, core);
                    sendMsg(p, "\u00a7a" + core.getLocalizedName().getString()
                            + " has been returned to you (Nusu user lost their fruit)");
                    break;
                }
            }

            if (entity.level instanceof ServerWorld) {
                NusuWorldData.get((ServerWorld) entity.level).unmarkStolen(core);
            }
        }

        NusuStolenData.clearHeld(entity);
        sendMsg(entity, "\u00a7cYou lost your devil fruit — all stolen abilities have been returned");
    }
}
