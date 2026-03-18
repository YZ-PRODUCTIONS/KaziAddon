//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.BuddhaRework;

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
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent.DamageState;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.AttributeHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;

public class HitoDaibutsuPointRework extends MorphAbility2 {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "daibutsu_point", new Pair[]{ImmutablePair.of("Allows the user to transforms into a Golden Buddha statue, which focuses on strength and defense.", (Object)null)});
    public static final AbilityCore<HitoDaibutsuPointRework> INSTANCE;
    private final DamageTakenComponent damageTakenComponent;
    private static final AbilityAttributeModifier SPEED_MODIFIER;
    private static final AbilityAttributeModifier JUMP_BOOST_MODIFIER;
    private static final AbilityAttributeModifier STRENGTH_MODIFIER;
    private static final AbilityAttributeModifier ATTACK_SPEED_MODIFIER;
    private static final AbilityAttributeModifier ARMOR_MODIFIER;
    private static final AbilityAttributeModifier ARMOR_THOUGNESS_MODIFIER;
    private static final AbilityAttributeModifier KNOCKBACK_RESISTANCE_MODIFIER;
    private static final AbilityAttributeModifier REACH_MODIFIER;
    private static final AbilityAttributeModifier HEALTH_BOOST_MODIFIER;
    private static final AbilityAttributeModifier STEP_HEIGHT_MODIFIER;
    private static final AbilityAttributeModifier TOUGHNESS_MODIFIER;

    public HitoDaibutsuPointRework(AbilityCore<HitoDaibutsuPointRework> core) {
        super(core);
        this.damageTakenComponent = new DamageTakenComponent(this, this::onDamageTaken, DamageState.ATTACK);
        this.statsComponent.addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_MODIFIER);
        this.statsComponent.addAttributeModifier(ModAttributes.PUNCH_DAMAGE, STRENGTH_MODIFIER);
        this.statsComponent.addAttributeModifier(Attributes.ATTACK_SPEED, ATTACK_SPEED_MODIFIER);
        this.statsComponent.addAttributeModifier(Attributes.ARMOR, ARMOR_MODIFIER);
        this.statsComponent.addAttributeModifier(Attributes.ARMOR_TOUGHNESS, ARMOR_THOUGNESS_MODIFIER);
        this.statsComponent.addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE_MODIFIER);
        this.statsComponent.addAttributeModifier(ModAttributes.JUMP_HEIGHT, JUMP_BOOST_MODIFIER);
        this.statsComponent.addAttributeModifier(ForgeMod.REACH_DISTANCE, REACH_MODIFIER);
        this.statsComponent.addAttributeModifier(ModAttributes.ATTACK_RANGE, REACH_MODIFIER);
        this.statsComponent.addAttributeModifier(Attributes.MAX_HEALTH, HEALTH_BOOST_MODIFIER);
        this.statsComponent.addAttributeModifier(ModAttributes.STEP_HEIGHT, STEP_HEIGHT_MODIFIER);
        this.statsComponent.addAttributeModifier(ModAttributes.TOUGHNESS, TOUGHNESS_MODIFIER);
        super.addComponents(new AbilityComponent[]{this.damageTakenComponent});
        this.continuousComponent.addStartEvent(100, this::startContinuityEvent).addEndEvent(100, this::endContinuityEvent);
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
    }

    private float onDamageTaken(LivingEntity entity, IAbility ability, DamageSource damageSource, float damage) {
        if (AbilityHelper.isDodging(entity)) {
            return damage;
        } else if (this.continuousComponent.isContinuous()) {
            return damageSource == DamageSource.FALL ? 0.0F : damage * 0.95F;
        } else {
            return damage;
        }
    }

    public MorphInfo getTransformation() {
        return (MorphInfo)CartMorphs.HITO_DAIBUTSU_NEW.get();
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Daibutsu Point", AbilityCategory.DEVIL_FRUITS, HitoDaibutsuPointRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(10.0F), ContinuousComponent.getTooltip(), ChangeStatsComponent.getTooltip()}).build();
        SPEED_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_MOVEMENT_SPEED_UUID, INSTANCE, "Daibutsu Point Speed Modifier", (double)0.5F, Operation.MULTIPLY_BASE);
        JUMP_BOOST_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_JUMP_BOOST_UUID, INSTANCE, "Daibutsu Point Jump Modifier", (double)5.0F, Operation.MULTIPLY_BASE);
        STRENGTH_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_STRENGTH_UUID, INSTANCE, "Daibutsu Point Strength Modifier", (double)10.0F, Operation.ADDITION);
        ATTACK_SPEED_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_ATTACK_SPEED_UUID, INSTANCE, "Daibutsu Point Attack Speed Modifier", (double)-0.5F, Operation.MULTIPLY_BASE);
        ARMOR_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_ARMOR_UUID, INSTANCE, "Daibutsu Point Armor Modifier", (double)8.0F, Operation.ADDITION);
        ARMOR_THOUGNESS_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_ARMOR_TOUGHNESS_UUID, INSTANCE, "Daibutsu Point Armor Toughness Modifier", (double)6.0F, Operation.ADDITION);
        KNOCKBACK_RESISTANCE_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_KNOCKBACK_RESISTANCE_UUID, INSTANCE, "Daibutsu Point Knockback Resistance Modifier", (double)5.0F, Operation.ADDITION);
        REACH_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_ATTACK_REACH_UUID, INSTANCE, "Daibutsu Point Reach Modifier", (double)12.0F, Operation.ADDITION);
        HEALTH_BOOST_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_HEALTH_UUID, INSTANCE, "Daibutsu Point Health Modifier", (double)100.0F, Operation.ADDITION);
        STEP_HEIGHT_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_STEP_HEIGHT_UUID, INSTANCE, "Daibutsu Point Step Modifier", (double)3.0F, Operation.ADDITION);
        TOUGHNESS_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_TOUGHNESS_UUID, INSTANCE, "Daibutsu Point Toughness Modifier", (double)2.0F, Operation.ADDITION);
    }
}
