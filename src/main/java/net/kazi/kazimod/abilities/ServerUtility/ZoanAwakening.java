package net.kazi.kazimod.abilities.ServerUtility;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.ForgeMod;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent.DamageState;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.AttributeHelper;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;

public class ZoanAwakening extends Ability {

    private static final float HOLD_TIME = 800.0F;
    private static final float MIN_COOLDOWN = 100.0F;
    private static final float MAX_COOLDOWN = 1200.0F;
    private static final ResourceLocation DEFAULT_ICON = new ResourceLocation("kazimod", "textures/abilities/zoanawakening.png");
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "zoan_awakening",
            new Pair[]{ImmutablePair.of(
                    "The user awakens their Zoan Devil Fruit, drawing out its true power and enhancing their physical abilities while active.",
                    null
            )}
    );

    public static final AbilityCore<ZoanAwakening> INSTANCE;

    private static final AbilityAttributeModifier SPEED_MODIFIER;
    private static final AbilityAttributeModifier JUMP_BOOST_MODIFIER;
    private static final AbilityAttributeModifier STRENGTH_MODIFIER;
    private static final AbilityAttributeModifier ARMOR_MODIFIER;
    private static final AbilityAttributeModifier ARMOR_THOUGNESS_MODIFIER;
    private static final AbilityAttributeModifier KNOCKBACK_RESISTANCE_MODIFIER;
    private static final AbilityAttributeModifier REACH_MODIFIER;
    private static final AbilityAttributeModifier STEP_HEIGHT_MODIFIER;
    private static final AbilityAttributeModifier TOUGHNESS_MODIFIER;

    private final DamageTakenComponent damageTakenComponent;
    private final ContinuousComponent continuousComponent = new ContinuousComponent(this, true)
            .addStartEvent(this::onContinuityStart)
            .addEndEvent(this::onContinuityEnd);
    private final ChangeStatsComponent changeStatsComponent = new ChangeStatsComponent(this);

    public ZoanAwakening(AbilityCore<ZoanAwakening> core) {
        super(core);
        this.isNew = true;
        this.damageTakenComponent = new DamageTakenComponent(this, this::onDamageTaken, DamageState.ATTACK);
        this.setDisplayIcon(DEFAULT_ICON);
        this.changeStatsComponent.addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_MODIFIER);
        this.changeStatsComponent.addAttributeModifier(ModAttributes.PUNCH_DAMAGE, STRENGTH_MODIFIER);
        this.changeStatsComponent.addAttributeModifier(Attributes.ARMOR, ARMOR_MODIFIER);
        this.changeStatsComponent.addAttributeModifier(Attributes.ARMOR_TOUGHNESS, ARMOR_THOUGNESS_MODIFIER);
        this.changeStatsComponent.addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE_MODIFIER);
        this.changeStatsComponent.addAttributeModifier(ModAttributes.JUMP_HEIGHT, JUMP_BOOST_MODIFIER);
        this.changeStatsComponent.addAttributeModifier(ForgeMod.REACH_DISTANCE, REACH_MODIFIER);
        this.changeStatsComponent.addAttributeModifier(ModAttributes.ATTACK_RANGE, REACH_MODIFIER);
        this.changeStatsComponent.addAttributeModifier(ModAttributes.STEP_HEIGHT, STEP_HEIGHT_MODIFIER);
        this.changeStatsComponent.addAttributeModifier(ModAttributes.TOUGHNESS, TOUGHNESS_MODIFIER);
        this.addComponents(new AbilityComponent[]{this.cooldownComponent, this.continuousComponent, this.changeStatsComponent, this.damageTakenComponent});
        this.addEquipEvent(this::equipEvent);
        this.addUseEvent(this::onUseEvent);
    }

    public void equipEvent(LivingEntity entity, Ability ability) {
        this.setDisplayIcon(DEFAULT_ICON);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.continuousComponent.isContinuous()) {
            this.continuousComponent.stopContinuity(entity);
        } else {
            this.continuousComponent.startContinuity(entity, HOLD_TIME);
        }
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
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

    private float onDamageTaken(LivingEntity entity, IAbility ability, DamageSource damageSource, float damage) {
        if (AbilityHelper.isDodging(entity)) {
            return damage;
        }


        return damage;
    }

    static {
        INSTANCE = new AbilityCore.Builder<>("Zoan Awakening", AbilityCategory.DEVIL_FRUITS, ZoanAwakening::new)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(MIN_COOLDOWN, MAX_COOLDOWN),
                        ContinuousComponent.getTooltip(HOLD_TIME),
                        ChangeStatsComponent.getTooltip()
                })
                .setIcon(DEFAULT_ICON)
                .build();
        SPEED_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_MOVEMENT_SPEED_UUID, INSTANCE, "Zoan Awakening Speed Modifier", 0.0D, Operation.ADDITION);
        JUMP_BOOST_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_JUMP_BOOST_UUID, INSTANCE, "Zoan Awakening Jump Modifier", 1.0D, Operation.ADDITION);
        STRENGTH_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_STRENGTH_UUID, INSTANCE, "Zoan Awakening Strength Modifier", 3.0D, Operation.ADDITION);
        ARMOR_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_ARMOR_UUID, INSTANCE, "Zoan Awakening Armor Modifier", 0.0D, Operation.ADDITION);
        ARMOR_THOUGNESS_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_ARMOR_TOUGHNESS_UUID, INSTANCE, "Zoan Awakening Armor Toughness Modifier", 0.0D, Operation.ADDITION);
        KNOCKBACK_RESISTANCE_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_KNOCKBACK_RESISTANCE_UUID, INSTANCE, "Zoan Awakening Knockback Resistance Modifier", 0.0D, Operation.ADDITION);
        REACH_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_ATTACK_REACH_UUID, INSTANCE, "Zoan Awakening Reach Modifier", 0.0D, Operation.ADDITION);
        STEP_HEIGHT_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_STEP_HEIGHT_UUID, INSTANCE, "Zoan Awakening Step Modifier", 0.0D, Operation.ADDITION);
        TOUGHNESS_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_TOUGHNESS_UUID, INSTANCE, "Zoan Awakening Toughness Modifier", 3.0D, Operation.ADDITION);
    }
}
