package net.kazi.kazimod.abilities.Nusu;

import net.kazi.kazimod.init.KaziItems2;
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
        this.itemSpawnComponent.despawnItems(entity);
    }

    static {
        INSTANCE = new AbilityCore.Builder<>(
                "Skill Book Creation", AbilityCategory.DEVIL_FRUITS,
                SkillBookCreationAbility::new)
                .addDescriptionLine(DESCRIPTION)
                .build();
    }
}