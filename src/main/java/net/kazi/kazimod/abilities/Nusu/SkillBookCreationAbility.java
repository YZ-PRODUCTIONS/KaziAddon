package net.kazi.kazimod.abilities.Nusu;

import net.kazi.kazimod.init.KaziItems2;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ItemSpawnComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Nusu Nusu no Mi - Skill Book Creation
 *
 * Activating summons the Skill Book item into the user's hand.
 * Activating again (toggle) despawns it. The book cannot be dropped
 * (handled in SkillBookItem.onEntityItemUpdate).
 *
 * The book must be in the user's hand/inventory for:
 *   - SkillHunterAbility (stealing process)
 *   - SkillRemoverAbility (returning abilities)
 *   - Using stolen abilities
 */
public class SkillBookCreationAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "skill_book_creation",
            new Pair[]{
                    ImmutablePair.of(
                            "Summons the Skill Book into your hand. Required for stealing, " +
                                    "returning, and using stolen abilities. Toggle to despawn.",
                            (Object) null)
            });

    public static final AbilityCore<SkillBookCreationAbility> INSTANCE;

    private final ContinuousComponent continuousComponent = new ContinuousComponent(this, true)
            .addStartEvent(this::onContinuityStart)
            .addEndEvent(this::onContinuityEnd);
    private final ItemSpawnComponent itemSpawnComponent = new ItemSpawnComponent(this);

    public SkillBookCreationAbility(AbilityCore<SkillBookCreationAbility> core) {
        super(core);
        super.isNew = true;
        super.addComponents(new AbilityComponent[]{
                this.continuousComponent, this.itemSpawnComponent
        });
        super.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity);
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        // Spawn the skill book item into the user's hand, same as Ama no Murakumo
        this.itemSpawnComponent.spawnItem(entity,
                new ItemStack((IItemProvider) KaziItems2.SKILL_BOOK.get()));
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        deactivateBookSpawnTracking();
        removeOnlySkillBooks(entity);
    }

    private void deactivateBookSpawnTracking() {
        try {
            Field activeField = ItemSpawnComponent.class.getDeclaredField("isActive");
            activeField.setAccessible(true);
            activeField.setBoolean(this.itemSpawnComponent, false);

            Field trackedStacksField = ItemSpawnComponent.class.getDeclaredField("trackedStacks");
            trackedStacksField.setAccessible(true);
            Object trackedStacks = trackedStacksField.get(this.itemSpawnComponent);
            if (trackedStacks instanceof List) {
                ((List<?>) trackedStacks).clear();
            }
        } catch (ReflectiveOperationException ignored) {
            // Book cleanup below still removes the summoned item even if MMNM internals change.
        }
    }

    private void removeOnlySkillBooks(LivingEntity entity) {
        if (entity.getMainHandItem().getItem() == KaziItems2.SKILL_BOOK.get()) {
            entity.setItemSlot(EquipmentSlotType.MAINHAND, ItemStack.EMPTY);
        }
        if (entity.getOffhandItem().getItem() == KaziItems2.SKILL_BOOK.get()) {
            entity.setItemSlot(EquipmentSlotType.OFFHAND, ItemStack.EMPTY);
        }
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            for (int slot = 0; slot < player.inventory.items.size(); slot++) {
                ItemStack stack = player.inventory.items.get(slot);
                if (!stack.isEmpty() && stack.getItem() == KaziItems2.SKILL_BOOK.get()) {
                    player.inventory.removeItemNoUpdate(slot);
                }
            }
            for (int slot = 0; slot < player.inventory.offhand.size(); slot++) {
                ItemStack stack = player.inventory.offhand.get(slot);
                if (!stack.isEmpty() && stack.getItem() == KaziItems2.SKILL_BOOK.get()) {
                    player.inventory.offhand.set(slot, ItemStack.EMPTY);
                }
            }
        }
    }

    static {
        INSTANCE = new AbilityCore.Builder<>(
                "Skill Book Creation", AbilityCategory.DEVIL_FRUITS,
                SkillBookCreationAbility::new)
                .addDescriptionLine(DESCRIPTION)
                .build();
    }
}
