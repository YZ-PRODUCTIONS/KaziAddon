package net.kazi.kazimod.abilities.Nusu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.ModRegistries;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityType;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.MorphAbility2;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUnlock;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModWeapons;

/** Self-roll that grants an eligible base Mine Mine no Mi Devil Fruit move. */
public class SkillHunterEXAbility extends Ability {
    public static final String ROLLED_ABILITY_TAG = "kazimodSkillHunterExRolledAbility";
    public static final String ROLLED_SLOT_TAG = "kazimodSkillHunterExRolledSlot";
    public static final String ROLLED_USED_TAG = "kazimodSkillHunterExRolledUsed";
    private static final String WEATHER_TOOL_TAG = "kazimodSkillHunterExWeatherTool";
    private static final String WEATHER_TOOL_MARKER = "kazimodSkillHunterExVirtualClimaTact";
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "skill_hunter_ex",
            new Pair[]{ImmutablePair.of(
                    "With the Skill Book in hand, randomly gain an eligible Mine Mine no Mi Devil Fruit move.",
                    null)});

    public static final float COOLDOWN = 200.0F;
    public static final AbilityCore<SkillHunterEXAbility> INSTANCE;

    public SkillHunterEXAbility(AbilityCore<SkillHunterEXAbility> core) {
        super(core);
        this.isNew = true;
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity user, IAbility ability) {
        if (!(user instanceof PlayerEntity)) return;
        if (!SkillHunterAbility.hasSkillBook(user)) {
            SkillHunterAbility.sendMsg(user, "\u00a7cYou need the Skill Book in your hand");
            return;
        }
        IAbilityData data = AbilityDataCapability.get(user);
        if (data == null) return;

        // A player can rebind a temporary move before rolling again. Remove every copy
        // by its stored core, not merely the slot that originally received the roll.
        removePreviousRoll(user, data);

        int slot = findOwnSlot(data);
        if (slot < 0) {
            SkillHunterAbility.sendMsg(user, "\u00a7cSkill Hunter EX must be equipped to roll a move");
            return;
        }

        List<AbilityCore<?>> choices = getEligibleMineMineNoMiAbilities(user, data);
        if (choices.isEmpty()) {
            SkillHunterAbility.sendMsg(user, "\u00a7cNo eligible Mine Mine no Mi abilities are available");
            return;
        }

        AbilityCore<?> chosen = choices.get(user.getRandom().nextInt(choices.size()));
        data.addUnlockedAbility(chosen, AbilityUnlock.COMMAND);
        IAbility rolledAbility = chosen.createAbility();
        data.setEquippedAbility(slot, rolledAbility);
        ResourceLocation chosenKey = ModRegistries.ABILITIES.getKey(chosen);
        user.getPersistentData().putString(ROLLED_ABILITY_TAG, chosenKey.toString());
        user.getPersistentData().putInt(ROLLED_SLOT_TAG, slot);
        user.getPersistentData().putBoolean(ROLLED_USED_TAG, false);
        NusuEvents.syncAbilityData((PlayerEntity) user, data);
        SkillHunterAbility.sendMsg(user, "\u00a7aSkill Hunter EX became "
                + chosen.getLocalizedName().getString() + " for one use!");
    }

    private static int findOwnSlot(IAbilityData data) {
        List<IAbility> equipped = data.getRawEquippedAbilities();
        for (int slot = 0; slot < equipped.size(); slot++) {
            IAbility ability = equipped.get(slot);
            if (ability != null && ability.getCore() == INSTANCE) return slot;
        }
        return -1;
    }

    private static void removePreviousRoll(LivingEntity user, IAbilityData data) {
        String storedKey = user.getPersistentData().getString(ROLLED_ABILITY_TAG);
        if (storedKey.isEmpty()) return;

        try {
            AbilityCore<?> previousCore = ModRegistries.ABILITIES.getValue(new ResourceLocation(storedKey));
            if (previousCore != null) {
                List<IAbility> equipped = data.getRawEquippedAbilities();
                for (int slot = 0; slot < equipped.size(); slot++) {
                    IAbility equippedAbility = equipped.get(slot);
                    if (equippedAbility != null && equippedAbility.getCore().equals(previousCore)) {
                        data.setEquippedAbility(slot, null);
                    }
                }
                data.removeUnlockedAbility(previousCore);
            }
        } catch (Exception ignored) {
            // A removed/renamed registry entry should not prevent a new roll.
        }

        restoreWeatherTool(user);
        user.getPersistentData().remove(ROLLED_ABILITY_TAG);
        user.getPersistentData().remove(ROLLED_SLOT_TAG);
        user.getPersistentData().remove(ROLLED_USED_TAG);
    }

    private static List<AbilityCore<?>> getEligibleMineMineNoMiAbilities(
            LivingEntity user, IAbilityData data) {
        List<AbilityCore<?>> choices = new ArrayList<>();
        for (AbilityCore<?> core : ModRegistries.ABILITIES.getValues()) {
            ResourceLocation key = ModRegistries.ABILITIES.getKey(core);
            if (key == null || (!"mineminenomi".equals(key.getNamespace())
                    && !"cartaddon".equals(key.getNamespace())
                    && !"kazimod".equals(key.getNamespace()))) continue;
            if (core.getCategory() != AbilityCategory.DEVIL_FRUITS) continue;
            if (core.getType() != AbilityType.ACTION || core.isHidden()) continue;
            if (data.hasUnlockedAbility(core)) continue;

            IAbility instance = core.createAbility();
            if (instance instanceof MorphAbility2) continue;
            if (!SkillHunterEXMoveAdapters.supports(instance, key)) continue;
            choices.add(core);
        }
        return choices;
    }

    public void startReturnCooldown(LivingEntity user) {
        this.cooldownComponent.startCooldown(user, COOLDOWN);
    }

    /** True only for the one temporary move created by the active EX roll. */
    public static boolean isRolledMove(LivingEntity entity, IAbility ability) {
        if (entity == null || ability == null) return false;
        String rolledKey = entity.getPersistentData().getString(ROLLED_ABILITY_TAG);
        if (rolledKey.isEmpty()) return false;
        ResourceLocation abilityKey = ModRegistries.ABILITIES.getKey(ability.getCore());
        return abilityKey != null && rolledKey.equals(abilityKey.toString());
    }

    /** Whether the entity currently has any temporary Skill Hunter EX roll. */
    public static boolean hasActiveRoll(LivingEntity entity) {
        return entity != null && !entity.getPersistentData().getString(ROLLED_ABILITY_TAG).isEmpty();
    }

    /** Temporarily supplies a Sorcery Clima Tact only while an EX weather roll is active. */
    public static void ensureWeatherTool(LivingEntity entity, IAbility ability) {
        if (!(entity instanceof PlayerEntity) || !isRolledMove(entity, ability)) return;
        ItemStack held = entity.getMainHandItem();
        if (held.getTag() != null && held.getTag().getBoolean(WEATHER_TOOL_MARKER)) return;
        if (held.getItem() instanceof xyz.pixelatedw.mineminenomi.items.weapons.ClimaTactItem) return;

        CompoundNBT saved = new CompoundNBT();
        held.save(saved);
        entity.getPersistentData().put(WEATHER_TOOL_TAG, saved);
        ItemStack virtualTool = new ItemStack(ModWeapons.SORCERY_CLIMA_TACT.get());
        virtualTool.getOrCreateTag().putBoolean(WEATHER_TOOL_MARKER, true);
        entity.setItemSlot(EquipmentSlotType.MAINHAND, virtualTool);
    }

    public static void restoreWeatherTool(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity) || !entity.getPersistentData().contains(WEATHER_TOOL_TAG)) return;
        ItemStack held = entity.getMainHandItem();
        if (held.getTag() != null && held.getTag().getBoolean(WEATHER_TOOL_MARKER)) {
            entity.setItemSlot(EquipmentSlotType.MAINHAND,
                    ItemStack.of(entity.getPersistentData().getCompound(WEATHER_TOOL_TAG)));
        }
        entity.getPersistentData().remove(WEATHER_TOOL_TAG);
    }

    static {
        INSTANCE = new AbilityCore.Builder<>(
                "Skill Hunter EX", AbilityCategory.DEVIL_FRUITS, SkillHunterEXAbility::new)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN))
                .build();
    }
}
