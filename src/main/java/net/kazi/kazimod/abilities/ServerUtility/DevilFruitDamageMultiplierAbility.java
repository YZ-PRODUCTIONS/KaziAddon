package net.kazi.kazimod.abilities.ServerUtility;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityAttributeModifier;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChangeStatsComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.AttributeHelper;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;

public class DevilFruitDamageMultiplierAbility extends Ability {

    public static final float DAMAGE_MULTIPLIER = 1.5F;
    private static final float HOLD_TIME = 800.0F;
    private static final float MIN_COOLDOWN = 100.0F;
    private static final float MAX_COOLDOWN = 1200.0F;
    private static final ResourceLocation DEFAULT_ICON =
            new ResourceLocation("kazimod", "textures/abilities/logia_awakening.png");
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "logia_awakening",
            new Pair[]{ImmutablePair.of(
                    "The user awakens their Logia Devil Fruit, drawing out its true power and increasing Devil Fruit ability damage while active.",
                    null
            )}
    );

    public static final AbilityCore<DevilFruitDamageMultiplierAbility> INSTANCE =
            new AbilityCore.Builder<>(
                    "Logia Awakening",
                    AbilityCategory.DEVIL_FRUITS,
                    DevilFruitDamageMultiplierAbility::new
            )
                    .addDescriptionLine(DESCRIPTION)
                    .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                            AbilityDescriptionLine.NEW_LINE,
                            ContinuousComponent.getTooltip(HOLD_TIME),
                            ChangeStatsComponent.getTooltip(),
                            CooldownComponent.getTooltip(MIN_COOLDOWN, MAX_COOLDOWN)
                    })
                    .setIcon(DEFAULT_ICON)
                    .build();

    private static final AbilityAttributeModifier ARMOR_MODIFIER =
            new AbilityAttributeModifier(AttributeHelper.MORPH_ARMOR_UUID, INSTANCE, "Logia Awakening Armor Modifier", 3.0D, Operation.ADDITION);
    private static final AbilityAttributeModifier TOUGHNESS_MODIFIER =
            new AbilityAttributeModifier(AttributeHelper.MORPH_TOUGHNESS_UUID, INSTANCE, "Logia Awakening Toughness Modifier", 1.0D, Operation.ADDITION);

    private final ContinuousComponent continuousComponent = new ContinuousComponent(this, true)
            .addStartEvent(this::onContinuityStart)
            .addEndEvent(this::onContinuityEnd);
    private final ChangeStatsComponent changeStatsComponent = new ChangeStatsComponent(this);

    public DevilFruitDamageMultiplierAbility(AbilityCore<DevilFruitDamageMultiplierAbility> core) {
        super(core);
        this.isNew = true;
        this.setDisplayIcon(DEFAULT_ICON);
        this.changeStatsComponent.addAttributeModifier(Attributes.ARMOR, ARMOR_MODIFIER);
        this.changeStatsComponent.addAttributeModifier(ModAttributes.TOUGHNESS, TOUGHNESS_MODIFIER);
        this.addComponents(new AbilityComponent[]{this.cooldownComponent, this.continuousComponent, this.changeStatsComponent});
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.continuousComponent.isContinuous()) {
            this.continuousComponent.stopContinuity(entity);
        } else {
            this.continuousComponent.startContinuity(entity, HOLD_TIME);
        }
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        this.setDisplayIcon(DEFAULT_ICON);
        this.changeStatsComponent.applyModifiers(entity);
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        this.changeStatsComponent.removeModifiers(entity);
        this.cooldownComponent.startCooldown(entity, this.getScaledCooldown());
    }

    private float getScaledCooldown() {
        float holdFraction = Math.min(1.0F, this.continuousComponent.getContinueTime() / HOLD_TIME);
        return MIN_COOLDOWN + ((MAX_COOLDOWN - MIN_COOLDOWN) * holdFraction);
    }

    public boolean isActive() {
        return this.continuousComponent.isContinuous();
    }

    public ContinuousComponent getContinuousComponent() {
        return this.continuousComponent;
    }
}
