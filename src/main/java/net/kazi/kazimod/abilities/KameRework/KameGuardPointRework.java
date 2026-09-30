package net.kazi.kazimod.abilities.KameRework;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.block.material.Material;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.ForgeMod;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.AttributeHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModMorphs;

/** Standalone copy of Kame Guard Point, with the additional jump restriction. */
public class KameGuardPointRework extends MorphAbility2 {
    public static final AbilityCore<KameGuardPointRework> INSTANCE =
            new AbilityCore.Builder<>("Kame Guard Point", AbilityCategory.DEVIL_FRUITS,
                    KameGuardPointRework::new)
                    .setIcon(new ResourceLocation("mineminenomi", "textures/abilities/kame_guard_point.png"))
                    .addDescriptionLine(
                            new StringTextComponent("Transforms the user into a turtle, which focuses on defense."),
                            new StringTextComponent("Sneaking allows you to retract into your shell."))
                    .addAdvancedDescriptionLine(AbilityDescriptionLine.NEW_LINE,
                            CooldownComponent.getTooltip(10.0F), ContinuousComponent.getTooltip(),
                            ChangeStatsComponent.getTooltip()).build();

    private static final AbilityAttributeModifier HEALTH_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_HEALTH_UUID, INSTANCE, "Kame Guard Point Modifier", 10.0, AttributeModifier.Operation.ADDITION);
    private static final AbilityAttributeModifier ARMOR_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_ARMOR_UUID, INSTANCE, "Kame Guard Point Modifier", 25.0, AttributeModifier.Operation.ADDITION);
    private static final AbilityAttributeModifier ATTACK_SPEED_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_ATTACK_SPEED_UUID, INSTANCE, "Kame Guard Point Modifier", (double)-0.15F, AttributeModifier.Operation.ADDITION);
    private static final AbilityAttributeModifier KNOCKBACK_RESISTANCE_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_KNOCKBACK_RESISTANCE_UUID, INSTANCE, "Kame Guard Point Knockback Resistance Modifier", 2.0, AttributeModifier.Operation.ADDITION);
    private static final AbilityAttributeModifier JUMP_BOOST_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_JUMP_BOOST_UUID, INSTANCE, "Kame Guard Point Jump Modifier", -10.0, AttributeModifier.Operation.ADDITION);
    private static final AbilityAttributeModifier SPEED_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_MOVEMENT_SPEED_UUID, INSTANCE, "Kame Guard Point Modifier", -0.85, AttributeModifier.Operation.MULTIPLY_BASE);
    private static final AbilityAttributeModifier SWIM_SPEED_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_SWIM_SPEED_UUID, INSTANCE, "Kame Guard Point Water Speed Modifier", 1.0, AttributeModifier.Operation.ADDITION);
    private static final AbilityAttributeModifier TOUGHNESS_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_TOUGHNESS_UUID, INSTANCE, "Kame Guard Point Toughness Modifier", 10.0, AttributeModifier.Operation.ADDITION);

    public KameGuardPointRework(AbilityCore<KameGuardPointRework> core) {
        super(core);
        this.statsComponent.addAttributeModifier(Attributes.ARMOR, ARMOR_MODIFIER);
        this.statsComponent.addAttributeModifier(Attributes.MAX_HEALTH, HEALTH_MODIFIER);
        this.statsComponent.addAttributeModifier(Attributes.ATTACK_SPEED, ATTACK_SPEED_MODIFIER);
        this.statsComponent.addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE_MODIFIER);
        this.statsComponent.addAttributeModifier(ModAttributes.JUMP_HEIGHT, JUMP_BOOST_MODIFIER);
        this.statsComponent.addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_MODIFIER);
        this.statsComponent.addAttributeModifier(ForgeMod.SWIM_SPEED, SWIM_SPEED_MODIFIER);
        this.statsComponent.addAttributeModifier(ModAttributes.TOUGHNESS, TOUGHNESS_MODIFIER);
        this.continuousComponent.addTickEvent(100, this::tickContinuityEvent);
        this.continuousComponent.addTickEvent(1, this::preventJumping);
    }

    public void tickContinuityEvent(LivingEntity entity, IAbility ability) {
        if (entity.isCrouching()) {
            if (!entity.level.isClientSide) {
                entity.addEffect(new EffectInstance(ModEffects.GUARDING.get(), 2, 3, false, false));
            }
        } else if (entity.isOnGround()
                && (entity.level.getBlockState(entity.blockPosition().below()).getMaterial() == Material.ICE
                || entity.level.getBlockState(entity.blockPosition().below()).getMaterial() == Material.ICE_SOLID)
                && (Math.abs(entity.getDeltaMovement().x) < 0.2 || Math.abs(entity.getDeltaMovement().z) < 0.2)) {
            AbilityHelper.setDeltaMovement(entity, entity.getDeltaMovement().x * 1.12,
                    entity.getDeltaMovement().y, entity.getDeltaMovement().z * 1.12);
        }
    }

    @Override
    public MorphInfo getTransformation() {
        return ModMorphs.KAME_GUARD.get();
    }

    private void preventJumping(LivingEntity user, IAbility ability) {
        if (!this.isContinuous()) return;

        user.setJumping(false);
        Vector3d movement = user.getDeltaMovement();
        if (movement.y > 0.0D) {
            user.setDeltaMovement(movement.x, 0.0D, movement.z);
        }
    }
}
