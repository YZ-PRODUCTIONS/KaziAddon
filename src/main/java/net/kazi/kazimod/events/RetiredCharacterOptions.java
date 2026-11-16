package net.kazi.kazimod.events;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.MrMagicalCart.cartaddon.abilities.greatblade.*;
import net.MrMagicalCart.cartaddon.abilities.hasshoken.*;
import net.MrMagicalCart.cartaddon.abilities.tontatta.*;
import net.MrMagicalCart.cartaddon.api.helpers.CartFightingStyleHelper;
import net.MrMagicalCart.cartaddon.entities.mobs.quests.givers.GreatBladeTrainerEntity;
import net.MrMagicalCart.cartaddon.entities.mobs.quests.givers.HasshokenTrainerEntity;
import net.MrMagicalCart.cartaddon.init.CartAttributes;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.events.ability.AbilityUseEvent;
import xyz.pixelatedw.mineminenomi.api.events.ability.UnlockAbilityEvent;
import xyz.pixelatedw.mineminenomi.api.quests.QuestId;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.init.ModValues;
import xyz.pixelatedw.mineminenomi.events.abilities.AbilityProgressionEvents;
import xyz.pixelatedw.mineminenomi.packets.server.SSyncQuestDataPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

@Mod.EventBusSubscriber(modid = "kazimod")
public final class RetiredCharacterOptions {
    public static boolean retired(ResourceLocation id) {
        return CartValues.TONTATTA.equals(id) || CartValues.HASSHOKEN.equals(id) || CartValues.GREAT_BLADE.equals(id);
    }

    public static boolean retired(AbilityCore<?> core) {
        // Keep these cores registered for old saves and NPCs that use the moves.
        return Moves.ALL.contains(core);
    }

    public static boolean retiredTrial(QuestId<?> quest) {
        return quest != null && (CartQuests.HASSHOKEN_TRIALS.contains(quest) || CartQuests.GREAT_BLADE_TRIALS.contains(quest));
    }

    private static final class Moves {
        static final Set<AbilityCore<?>> ALL = new HashSet<>(Arrays.asList(
                SkeedaddleAbility.INSTANCE, TailHammerAbility.INSTANCE, TailWhipAbility.INSTANCE,
                TontattaLeapingAbility.INSTANCE, TontattaPassiveAbility.INSTANCE, TontattaSwingAbility.INSTANCE,
                BujaogenAbility.INSTANCE, ButoAbility.INSTANCE, ButoKaitenAbility.INSTANCE, ButoKaitenRanbuAbility.INSTANCE,
                ElbowGuardAbility.INSTANCE, HasshokenBonusesAbility.INSTANCE, KiryuKirikugiAbility.INSTANCE,
                MukiryuMukirikugiAbility.INSTANCE, OgiFocusAbility.INSTANCE, BlackBladeCrescentMoonAbility.INSTANCE,
                HeavySwingAbility.INSTANCE, HollowRequitalAbility.INSTANCE, ThousandSlicesAbility.INSTANCE,
                VoidAscenderAbility.INSTANCE, VoidShatteringSlashAbility.INSTANCE));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void preventUnlock(UnlockAbilityEvent event) {
        if (event.getEntityLiving() instanceof PlayerEntity && retired(event.getAbilityCore())) event.setResult(Event.Result.DENY);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventUse(AbilityUseEvent.Pre event) {
        if (event.getEntityLiving() instanceof PlayerEntity && retired(event.getAbility().getCore())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onJoin(EntityJoinWorldEvent event) {
        if (event.getWorld().isClientSide) return;
        if (event.getEntity() instanceof GreatBladeTrainerEntity || event.getEntity() instanceof HasshokenTrainerEntity) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof ServerPlayerEntity) migrate((ServerPlayerEntity)event.getEntity());
    }

    static void migrate(ServerPlayerEntity player) {
        IEntityStats stats = EntityStatsCapability.get(player);
        boolean oldRace = CartValues.TONTATTA.equals(stats.getRace());
        ResourceLocation style = stats.getFightingStyle();
        boolean hasshoken = CartValues.HASSHOKEN.equals(style);
        boolean greatBlade = CartValues.GREAT_BLADE.equals(style);
        if (oldRace) {
            stats.setRace(ModValues.HUMAN);
            stats.setSubRace("");
        }
        if (hasshoken || greatBlade) stats.setFightingStyle(hasshoken ? ModValues.BRAWLER : ModValues.SWORDSMAN);

        IAbilityData data = AbilityDataCapability.get(player);
        for (AbilityCore<?> core : Moves.ALL) {
            data.removeUnlockedAbility(core);
            data.removePassiveAbility(core);
            data.removeEquippedAbility(core);
        }

        boolean resized = removeModifier(player, CartAttributes.SIZE, "2a972463-95f1-41c1-a00c-74d11f5ea4a9");
        removeModifier(player, ModAttributes.PUNCH_DAMAGE.get(), "a46633c5-63d9-4d39-894b-725984cdc35c");
        removeModifier(player, ModAttributes.FALL_RESISTANCE.get(), "427c05a3-b5a6-44bc-8597-79deeed7d6f3");
        removeModifier(player, ModAttributes.JUMP_HEIGHT.get(), "5ca1330c-3eef-4a9f-9828-1258fcb987b6");
        removeModifier(player, ModAttributes.PUNCH_DAMAGE.get(), "6e133caf-b2ee-4959-b57e-8e4be10a3012");
        removeModifier(player, Attributes.ATTACK_SPEED, "9e34e54d-984e-430e-a3cf-c6bcf0ecb7dc");
        removeModifier(player, ModAttributes.STEP_HEIGHT.get(), "4f75838d-0055-42a4-ac2c-ad847c5413e9");
        removeModifier(player, ModAttributes.STEP_HEIGHT.get(), "3a5fdc2c-1a8f-4f84-ba2d-80744a3c6a28");
        removeModifier(player, ModAttributes.PUNCH_DAMAGE.get(), "1c0e6b2b-6495-41b1-aed7-de1ce6fcbb72");
        removeModifier(player, ModAttributes.JUMP_HEIGHT.get(), "f5f3d581-31c4-41b1-87f3-5f405be8753b");
        removeModifier(player, Attributes.ATTACK_DAMAGE, CartFightingStyleHelper.HASSHOKEN_ATTACK_BONUS_ID);
        removeModifier(player, Attributes.ATTACK_DAMAGE, CartFightingStyleHelper.GREAT_BLADE_ATTACK_BONUS_ID);
        removeModifier(player, Attributes.ATTACK_SPEED, CartFightingStyleHelper.GREAT_BLADE_SPEED_BONUS_ID);
        if (resized || oldRace) player.refreshDimensions();

        IQuestData quests = QuestDataCapability.get(player);
        boolean questsChanged = clearTrials(quests, CartQuests.HASSHOKEN_TRIALS) | clearTrials(quests, CartQuests.GREAT_BLADE_TRIALS);
        if (questsChanged) WyNetwork.sendTo(new SSyncQuestDataPacket(player.getId(), quests), player);
        if (oldRace || hasshoken || greatBlade) {
            if (oldRace) AbilityProgressionEvents.checkForRacialUnlocks(player);
            if (hasshoken || greatBlade) AbilityProgressionEvents.checkForStyleUnlocks(player);
            String text = (oldRace ? "Tontatta was retired. Your race is now Human. " : "")
                    + (hasshoken ? "Hasshoken was retired. Your style is now Brawler. "
                    : greatBlade ? "Great Blade was retired. Your style is now Swordsman. " : "");
            player.sendMessage(new StringTextComponent(text + "Use a Character Creator book to change your selection."), player.getUUID());
        }
    }

    private static boolean clearTrials(IQuestData quests, List<? extends QuestId> trials) {
        boolean changed = false;
        for (QuestId<?> trial : trials) {
            changed |= quests.removeInProgressQuest(trial);
            changed |= quests.removeFinishedQuest(trial);
        }
        return changed;
    }

    private static boolean removeModifier(LivingEntity player, Attribute attribute, String uuid) {
        return removeModifier(player, attribute, UUID.fromString(uuid));
    }

    private static boolean removeModifier(LivingEntity player, Attribute attribute, UUID id) {
        ModifiableAttributeInstance instance = player.getAttribute(attribute);
        if (instance == null || instance.getModifier(id) == null) return false;
        instance.removeModifier(id);
        return true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void removeItemBonuses(ItemAttributeModifierEvent event) {
        // Old weapons may still carry Cart's style-specific bonus tags.
        for (Map.Entry<Attribute, AttributeModifier> entry : new ArrayList<>(event.getModifiers().entries())) {
            UUID id = entry.getValue().getId();
            if (id.equals(CartFightingStyleHelper.HASSHOKEN_ATTACK_BONUS_ID)
                    || id.equals(CartFightingStyleHelper.GREAT_BLADE_ATTACK_BONUS_ID)
                    || id.equals(CartFightingStyleHelper.GREAT_BLADE_SPEED_BONUS_ID))
                event.removeModifier(entry.getKey(), entry.getValue());
        }
    }

    private RetiredCharacterOptions() {}
}
