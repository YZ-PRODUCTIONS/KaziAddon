//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.KitsuneRework;

import java.util.function.Predicate;
import net.MrMagicalCart.cartaddon.abilities.inuokuchi.InuOkuchiHeavyPointAbility;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent.DamageState;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.AttributeHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;

public class InuKitsuneWalkPointRework extends MorphAbility2 {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "kitsune_walk_point", new Pair[]{ImmutablePair.of("Transforms the user into a mythical trickster fox.", (Object)null)});
    public static final AbilityCore<InuOkuchiHeavyPointAbility> INSTANCE;
    private static final AbilityAttributeModifier SPEED_MODIFIER;
    private static final AbilityAttributeModifier ARMOR_MODIFIER;
    private static final AbilityAttributeModifier STRENGTH_MODIFIER;
    private static final AbilityAttributeModifier ATTACK_SPEED_MODIFIER;
    private static final AbilityAttributeModifier JUMP_BOOST_MODIFIER;
    private static final AbilityAttributeModifier REACH_MODIFIER;
    private static final AbilityAttributeModifier STEP_HEIGHT;
    private static final AbilityAttributeModifier TOUGHNESS_MODIFIER;
    private final DamageTakenComponent damageTakenComponent;
    private boolean hasFallDamage;

    public InuKitsuneWalkPointRework(AbilityCore<InuKitsuneWalkPointRework> core) {
        super(core);
        Predicate<LivingEntity> isMorphed = (entity) -> this.morphComponent.isMorphed();
        this.statsComponent.addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_MODIFIER, isMorphed);
        this.statsComponent.addAttributeModifier(Attributes.ARMOR, ARMOR_MODIFIER, isMorphed);
        this.statsComponent.addAttributeModifier(ModAttributes.PUNCH_DAMAGE, STRENGTH_MODIFIER, isMorphed);
        this.statsComponent.addAttributeModifier(Attributes.ATTACK_SPEED, ATTACK_SPEED_MODIFIER, isMorphed);
        this.statsComponent.addAttributeModifier(ModAttributes.JUMP_HEIGHT, JUMP_BOOST_MODIFIER, isMorphed);
        this.statsComponent.addAttributeModifier(ForgeMod.REACH_DISTANCE, REACH_MODIFIER, isMorphed);
        this.statsComponent.addAttributeModifier(ModAttributes.ATTACK_RANGE, REACH_MODIFIER, isMorphed);
        this.statsComponent.addAttributeModifier(ModAttributes.STEP_HEIGHT, STEP_HEIGHT);
        this.statsComponent.addAttributeModifier(ModAttributes.TOUGHNESS, TOUGHNESS_MODIFIER, isMorphed);
        this.damageTakenComponent = new DamageTakenComponent(this, this::onDamageTaken, DamageState.ATTACK);
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.damageTakenComponent});
        this.hasFallDamage = true;
        this.continuousComponent.addEndEvent(this::stopContinuityEvent);
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        this.hasFallDamage = false;
    }

    public MorphInfo getTransformation() {
        return (MorphInfo)CartMorphs.KITSUNE_WALK.get();
    }

    private void stopContinuityEvent(LivingEntity entity, IAbility ability) {
        this.hasFallDamage = true;
        this.morphComponent.stopMorph(entity);
        this.statsComponent.removeModifiers(entity);
        this.cooldownComponent.startCooldown(entity, this.getCooldownTicks());
    }

    private float onDamageTaken(LivingEntity entity, IAbility ability, DamageSource damageSource, float damage) {
        return this.continuousComponent.isContinuous() && damageSource == DamageSource.FALL ? 0.0F : damage;
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Kitsune Walk Point", AbilityCategory.DEVIL_FRUITS, InuKitsuneWalkPointRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, ChangeStatsComponent.getTooltip()}).build();
        SPEED_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_MOVEMENT_SPEED_UUID, INSTANCE, "Kitsune Walk Point Movement Modifier", 0.2, Operation.ADDITION);
        ARMOR_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_ARMOR_UUID, INSTANCE, "Kitsune Walk Point Armor Modifier", (double)2.0F, Operation.ADDITION);
        STRENGTH_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_STRENGTH_UUID, INSTANCE, "Kitsune Walk Point Strength Modifier", (double)3.0F, Operation.ADDITION);
        ATTACK_SPEED_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_ATTACK_SPEED_UUID, INSTANCE, "Kitsune Walk Point Attack Movement Modifier", 0.2, Operation.ADDITION);
        JUMP_BOOST_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_JUMP_BOOST_UUID, INSTANCE, "Kitsune Walk Point Jump Modifier", (double)5.0F, Operation.ADDITION);
        REACH_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_ATTACK_REACH_UUID, INSTANCE, "Kitsune Walk Point Reach Modifier", (double)1.0F, Operation.ADDITION);
        STEP_HEIGHT = new AbilityAttributeModifier(AttributeHelper.MORPH_STEP_HEIGHT_UUID, INSTANCE, "Kitsune Walk Point Step Height Modifier", (double) 2.0F, Operation.ADDITION);
        TOUGHNESS_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_TOUGHNESS_UUID, INSTANCE, "Kitsune Walk Point Toughness Modifier", (double)3.0F, Operation.ADDITION);
    }
}
