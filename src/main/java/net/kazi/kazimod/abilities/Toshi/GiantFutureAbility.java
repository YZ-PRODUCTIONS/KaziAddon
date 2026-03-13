package net.kazi.kazimod.abilities.Toshi;

import net.MrMagicalCart.cartaddon.init.CartMorphs;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.ForgeMod;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityAttributeModifier;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.MorphAbility2;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChangeStatsComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.AttributeHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;

public class GiantFutureAbility extends MorphAbility2 {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("kazimod", "giant_future", new Pair[]{ImmutablePair.of("Turns you into an alternate timeline version of yourself where you are a giant", (Object) null)});

    // FIX: typed to GiantFutureAbility so getEquippedAbility() returns the correct type
    public static final AbilityCore<GiantFutureAbility> INSTANCE;

    private static final AbilityAttributeModifier SPEED_MODIFIER;
    private static final AbilityAttributeModifier JUMP_MODIFIER;
    private static final AbilityAttributeModifier ARMOR_MODIFIER;
    private static final AbilityAttributeModifier STRENGTH_MODIFIER;
    private static final AbilityAttributeModifier REACH_MODIFIER;
    private static final AbilityAttributeModifier STEP_HEIGHT;
    private static final AbilityAttributeModifier KNOCKBACK_RESISTANCE;
    private static final AbilityAttributeModifier FALL_RESISTANCE_MODIFIER;
    private static final AbilityAttributeModifier TOUGHNESS_MODIFIER;
    private static final AbilityAttributeModifier HEALTH_BOOST_MODIFIER;

    // 10 second cooldown = 200 ticks
    private static final float COOLDOWN_TICKS = 200.0F;

    private final DamageTakenComponent damageTakenComponent;

    // FIX: constructor parameter type matches the corrected INSTANCE generic
    public GiantFutureAbility(AbilityCore<GiantFutureAbility> core) {
        super(core);
        this.damageTakenComponent = new DamageTakenComponent(this, this::damageTakenEvent, DamageTakenComponent.DamageState.HURT);
        this.addComponents(new AbilityComponent[]{this.damageTakenComponent});
        this.statsComponent.addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_MODIFIER);
        this.statsComponent.addAttributeModifier(ModAttributes.JUMP_HEIGHT, JUMP_MODIFIER);
        this.statsComponent.addAttributeModifier(Attributes.ARMOR, ARMOR_MODIFIER);
        this.statsComponent.addAttributeModifier(ModAttributes.PUNCH_DAMAGE, STRENGTH_MODIFIER);
        this.statsComponent.addAttributeModifier(ForgeMod.REACH_DISTANCE, REACH_MODIFIER);
        this.statsComponent.addAttributeModifier(ModAttributes.ATTACK_RANGE, REACH_MODIFIER);
        this.statsComponent.addAttributeModifier(ModAttributes.STEP_HEIGHT, STEP_HEIGHT);
        this.statsComponent.addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE);
        this.statsComponent.addAttributeModifier(ModAttributes.FALL_RESISTANCE, FALL_RESISTANCE_MODIFIER);
        this.statsComponent.addAttributeModifier(ModAttributes.TOUGHNESS, TOUGHNESS_MODIFIER);
        this.statsComponent.addAttributeModifier(Attributes.MAX_HEALTH, HEALTH_BOOST_MODIFIER);
    }

    /**
     * Exposes the inherited continuousComponent publicly so other abilities
     * (e.g. GiantPunchAbility) can check whether Giant Future is active.
     * Mirrors the same pattern used in GearFifthRework.
     */
    public ContinuousComponent getContinuousComponent() {
        return this.continuousComponent;
    }

    @Override
    public float getCooldownTicks() {
        return COOLDOWN_TICKS;
    }

    private float damageTakenEvent(LivingEntity entity, IAbility ability, DamageSource damageSource, float damage) {
        if (AbilityHelper.isDodging(entity)) {
            return damage;
        } else {
            return this.continuousComponent.isContinuous() ? damage * 0.9F : damage;
        }
    }

    public MorphInfo getTransformation() {
        return (MorphInfo) CartMorphs.DEKA.get();
    }

    static {
        // FIX: builder now correctly references GiantFutureAbility::new (was wrongly GiantPunchAbility::new)
        // FIX: INSTANCE generic is GiantFutureAbility, tooltip updated to reflect 10s cooldown
        INSTANCE = (new AbilityCore.Builder<>("Giant Future", AbilityCategory.DEVIL_FRUITS, GiantFutureAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN_TICKS / 20.0F),
                        ContinuousComponent.getTooltip(),
                        ChangeStatsComponent.getTooltip()
                })
                .build();
        SPEED_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_MOVEMENT_SPEED_UUID, INSTANCE, "Mega Mega Speed Modifier", (double) 1.02F, Operation.MULTIPLY_BASE);
        JUMP_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_JUMP_BOOST_UUID, INSTANCE, "Mega Mega Jump Modifier", (double) 2.0F, Operation.ADDITION);
        ARMOR_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_ARMOR_UUID, INSTANCE, "Mega Mega Armor Modifier", (double) 5.0F, Operation.ADDITION);
        STRENGTH_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_STRENGTH_UUID, INSTANCE, "Mega Mega Strength Modifier", (double) 3.0F, Operation.ADDITION);
        REACH_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_ATTACK_REACH_UUID, INSTANCE, "Mega Mega Reach Modifier", (double) 5.0F, Operation.ADDITION);
        STEP_HEIGHT = new AbilityAttributeModifier(AttributeHelper.MORPH_STEP_HEIGHT_UUID, INSTANCE, "Mega Mega Step Height Modifier", (double) 1.5F, Operation.ADDITION);
        KNOCKBACK_RESISTANCE = new AbilityAttributeModifier(AttributeHelper.MORPH_KNOCKBACK_RESISTANCE_UUID, INSTANCE, "Mega Mega Knockback Resistance Modifier", (double) 1.0F, Operation.ADDITION);
        FALL_RESISTANCE_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_FALL_RESISTANCE_UUID, INSTANCE, "Mega Mega Fall Resistance Modifier", (double) 10.0F, Operation.ADDITION);
        TOUGHNESS_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_TOUGHNESS_UUID, INSTANCE, "Mega Mega Toughness Modifier", (double) 4.0F, Operation.ADDITION);
        HEALTH_BOOST_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_HEALTH_UUID, INSTANCE, "Mega Mega Health Modifier", (double) 100.0F, Operation.ADDITION);
    }
}
