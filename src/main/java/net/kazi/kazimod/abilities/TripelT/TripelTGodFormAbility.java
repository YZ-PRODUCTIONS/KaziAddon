package net.kazi.kazimod.abilities.TripelT;

import net.kazi.kazimod.init.KaziMorphs;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.ForgeMod;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.abilities.PropelledFlightAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityAttributeModifier;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.MorphAbility2;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChangeStatsComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.AttributeHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;

public class TripelTGodFormAbility extends MorphAbility2 {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod",
            "triple_t_god_form",
            new Pair[]{ImmutablePair.of("Transform into Triple T God, automatically equipping the Triple T Staff, gaining heavy-point stats and unlocking falcon-style flight.", null)}
    );

    public static final AbilityCore<TripelTGodFormAbility> INSTANCE;

    private static final AbilityAttributeModifier SPEED_MODIFIER;
    private static final AbilityAttributeModifier STRENGTH_MODIFIER;
    private static final AbilityAttributeModifier ARMOR_MODIFIER;
    private static final AbilityAttributeModifier REACH_MODIFIER;
    private static final AbilityAttributeModifier STEP_HEIGHT_MODIFIER;
    private static final AbilityAttributeModifier JUMP_BOOST_MODIFIER;
    private static final AbilityAttributeModifier TOUGHNESS_MODIFIER;
    private static final AbilityAttributeModifier FALL_RESISTANCE_MODIFIER;

    public TripelTGodFormAbility(AbilityCore<TripelTGodFormAbility> core) {
        super(core);
        this.addCanUseCheck((entity, ability) -> TripelTFormWeapon.canStart(entity, this.continuousComponent.isContinuous()));
        this.continuousComponent.addStartEvent((entity, ability) -> TripelTFormWeapon.grant(entity, true))
                .addEndEvent((entity, ability) -> TripelTFormWeapon.remove(entity, true));
        this.addCanUseCheck((entity, ability) -> canUnlock(entity)
                ? xyz.pixelatedw.mineminenomi.api.abilities.AbilityUseResult.success()
                : xyz.pixelatedw.mineminenomi.api.abilities.AbilityUseResult.fail(null));
        this.addUseEvent((entity, ability) -> TripelTHelper.playTungSound(entity, 2.1F, 1.1F));
        this.statsComponent.addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_MODIFIER);
        this.statsComponent.addAttributeModifier(ModAttributes.PUNCH_DAMAGE, STRENGTH_MODIFIER);
        this.statsComponent.addAttributeModifier(Attributes.ARMOR, ARMOR_MODIFIER);
        this.statsComponent.addAttributeModifier(ForgeMod.REACH_DISTANCE, REACH_MODIFIER);
        this.statsComponent.addAttributeModifier(ModAttributes.ATTACK_RANGE, REACH_MODIFIER);
        this.statsComponent.addAttributeModifier(ModAttributes.STEP_HEIGHT, STEP_HEIGHT_MODIFIER);
        this.statsComponent.addAttributeModifier(ModAttributes.JUMP_HEIGHT, JUMP_BOOST_MODIFIER);
        this.statsComponent.addAttributeModifier(ModAttributes.TOUGHNESS, TOUGHNESS_MODIFIER);
        this.statsComponent.addAttributeModifier(ModAttributes.FALL_RESISTANCE, FALL_RESISTANCE_MODIFIER);
        this.continuousComponent.addStartEvent(this::onContinuityStart).addEndEvent(this::onContinuityEnd);
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide || !(entity instanceof PlayerEntity)) {
            return;
        }

        IAbilityData data = AbilityDataCapability.get(entity);
        if (data == null) {
            return;
        }

        IAbility flightAbility = data.getPassiveAbility(TripelTFlightAbility.INSTANCE);
        if (flightAbility instanceof PropelledFlightAbility && !((PropelledFlightAbility) flightAbility).isPaused()) {
            PropelledFlightAbility.enableFlight((PlayerEntity) entity);
        }
        this.syncTripelTAltModes(entity, data, true);
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide || !(entity instanceof PlayerEntity)) {
            return;
        }
        PropelledFlightAbility.disableFlight((PlayerEntity) entity);
        IAbilityData data = AbilityDataCapability.get(entity);
        if (data != null) {
            this.syncTripelTAltModes(entity, data, false);
        }
    }

    private void syncTripelTAltModes(LivingEntity entity, IAbilityData data, boolean godActive) {
        HomeRunSwingAbility homeRunSwing = (HomeRunSwingAbility) data.getEquippedAbility(HomeRunSwingAbility.INSTANCE);
        if (homeRunSwing != null) {
            if (godActive) {
                homeRunSwing.switchToAlt(entity);
            } else {
                homeRunSwing.switchToBase(entity);
            }
        }

        SwingingCounterAbility swingingCounter = (SwingingCounterAbility) data.getEquippedAbility(SwingingCounterAbility.INSTANCE);
        if (swingingCounter != null) {
            if (godActive) {
                swingingCounter.switchToJudgementation(entity);
            } else {
                swingingCounter.switchToBase(entity);
            }
        }

        TungTungTungBarrageAbility barrage = (TungTungTungBarrageAbility) data.getEquippedAbility(TungTungTungBarrageAbility.INSTANCE);
        if (barrage != null) {
            if (godActive) {
                barrage.switchToAlt(entity);
            } else {
                barrage.switchToBase(entity);
            }
        }

        SahurYellAbility sahurYell = (SahurYellAbility) data.getEquippedAbility(SahurYellAbility.INSTANCE);
        if (sahurYell != null) {
            if (godActive) {
                sahurYell.switchToAlt(entity);
            } else {
                sahurYell.switchToBase(entity);
            }
        }
    }

    @Override
    public MorphInfo getTransformation() {
        return KaziMorphs.TRIPEL_T_GOD.get();
    }

    private static boolean canUnlock(LivingEntity entity) {
        return DevilFruitCapability.get(entity).hasAwakenedFruit();
    }

    static {
        INSTANCE = new AbilityCore.Builder<>("Tripel T God Form", AbilityCategory.DEVIL_FRUITS, TripelTGodFormAbility::new)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(10.0F),
                        ContinuousComponent.getTooltip(),
                        ChangeStatsComponent.getTooltip())
                .setUnlockCheck(TripelTGodFormAbility::canUnlock)
                .build();

        SPEED_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_MOVEMENT_SPEED_UUID, INSTANCE, "Triple T God Speed Modifier", 0.19D, Operation.MULTIPLY_BASE);
        STRENGTH_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_STRENGTH_UUID, INSTANCE, "Triple T God Strength Modifier", 9.0D, Operation.ADDITION);
        ARMOR_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_ARMOR_UUID, INSTANCE, "Triple T God Armor Modifier", 15.0D, Operation.ADDITION);
        REACH_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_ATTACK_REACH_UUID, INSTANCE, "Triple T God Reach Modifier", 0.2D, Operation.ADDITION);
        STEP_HEIGHT_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_STEP_HEIGHT_UUID, INSTANCE, "Triple T God Step Height Modifier", 1.0D, Operation.ADDITION);
        JUMP_BOOST_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_JUMP_BOOST_UUID, INSTANCE, "Triple T God Jump Modifier", 1.4D, Operation.ADDITION);
        TOUGHNESS_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_TOUGHNESS_UUID, INSTANCE, "Triple T God Toughness Modifier", 8.0D, Operation.ADDITION);
        FALL_RESISTANCE_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_FALL_RESISTANCE_UUID, INSTANCE, "Triple T God Fall Resistance Modifier", 1.75D, Operation.ADDITION);
    }
}
