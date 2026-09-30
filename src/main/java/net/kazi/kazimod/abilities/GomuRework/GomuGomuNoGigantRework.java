package net.kazi.kazimod.abilities.GomuRework;

import java.awt.Color;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.ForgeMod;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityAttributeModifier;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityOverlay;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityOverlay.RenderType;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUseResult;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.MorphAbility2;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChangeStatsComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.SkinOverlayComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.AttributeHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.init.ModMorphs;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class GomuGomuNoGigantRework extends MorphAbility2 {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "gomu_gomu_no_gigant", new Pair[]{ImmutablePair.of("Allows the user to increase their size to that of a giant.", (Object)null)});
    private static final int HOLD_TIME = 1200;
    private static final int MIN_COOLDOWN = 100;
    private static final int MAX_COOLDOWN = 1200;
    public static final AbilityCore<GomuGomuNoGigantRework> INSTANCE;
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

    private static final Color COLOR = WyHelper.hexToRGB("#FFFFFF30");
    private static final AbilityOverlay OVERLAY;
    private final SkinOverlayComponent skinOverlayComponent;

    public GomuGomuNoGigantRework(AbilityCore core) {
        super(core);
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

        this.skinOverlayComponent = new SkinOverlayComponent(this, OVERLAY, new AbilityOverlay[0]);
        this.addComponents(new AbilityComponent[]{this.skinOverlayComponent});

        this.addCanUseCheck(this::canUse);
        this.continuousComponent.addStartEvent(this::startContinuityEvent);
        this.continuousComponent.addEndEvent(100, this::endContinuityEvent);
    }

    /** Exposes the continuous component so GearFifthRework can cancel this ability on end. */
    public ContinuousComponent getContinuousComponent() {
        return this.continuousComponent;
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.skinOverlayComponent.showAll(entity);

        // Switch Bazooka into Dawn Cymbal mode while Gigant is active
        IAbilityData props = AbilityDataCapability.get(entity);
        GomuGomuNoBazookaRework bazooka = (GomuGomuNoBazookaRework) props.getEquippedAbility(GomuGomuNoBazookaRework.INSTANCE);
        if (bazooka != null) bazooka.switchDawnCymbal(entity);
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.skinOverlayComponent.hideAll(entity);

        // Revert Bazooka back to normal No Gear mode when Gigant ends
        IAbilityData props = AbilityDataCapability.get(entity);
        GomuGomuNoBazookaRework bazooka = (GomuGomuNoBazookaRework) props.getEquippedAbility(GomuGomuNoBazookaRework.INSTANCE);
        if (bazooka != null) bazooka.switchNoGear(entity);

        float cooldown = Math.max(100.0F, this.continuousComponent.getContinueTime());
        this.cooldownComponent.startCooldown(entity, cooldown);
    }

    public MorphInfo getTransformation() {
        return (MorphInfo) ModMorphs.GOMU_GIGANT.get();
    }

    public float getContinuityHoldTime() {
        return 1200.0F;
    }

    private AbilityUseResult canUse(LivingEntity entity, IAbility ability) {
        IAbilityData props = AbilityDataCapability.get(entity);
        GearFifthRework gearFifth = (GearFifthRework) props.getEquippedAbility(GearFifthRework.INSTANCE);
        return gearFifth != null && gearFifth.isContinuous() ? AbilityUseResult.success() : AbilityUseResult.fail((ITextComponent) null);
    }

    private static boolean canUnlock(LivingEntity user) {
        return DevilFruitCapability.get(user).hasDevilFruit(ModAbilities.GOMU_GOMU_NO_MI)
                && DevilFruitCapability.get(user).hasAwakenedFruit();
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Gomu Gomu no Gigant", AbilityCategory.DEVIL_FRUITS, GomuGomuNoGigantRework::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(100.0F, 1200.0F),
                        ContinuousComponent.getTooltip(1200.0F),
                        ChangeStatsComponent.getTooltip()
                })
                .setUnlockCheck(GomuGomuNoGigantRework::canUnlock)
                .build();
        OVERLAY = (new AbilityOverlay.Builder()).setColor(COLOR).setRenderType(RenderType.ENERGY).build();
        SPEED_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_MOVEMENT_SPEED_UUID, INSTANCE, "Gomu Gomu no Gigant Speed Modifier", (double) 1.02F, Operation.MULTIPLY_BASE);
        JUMP_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_JUMP_BOOST_UUID, INSTANCE, "Gomu Gomu no Gigant Jump Modifier", (double) 2.0F, Operation.ADDITION);
        ARMOR_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_ARMOR_UUID, INSTANCE, "Gomu Gomu no Gigant Armor Modifier", (double) 5.0F, Operation.ADDITION);
        STRENGTH_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_STRENGTH_UUID, INSTANCE, "Gomu Gomu no Gigant Strength Modifier", (double) 3.0F, Operation.ADDITION);
        REACH_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_ATTACK_REACH_UUID, INSTANCE, "Gomu Gomu no Gigant Reach Modifier", (double) 5.0F, Operation.ADDITION);
        STEP_HEIGHT = new AbilityAttributeModifier(AttributeHelper.MORPH_STEP_HEIGHT_UUID, INSTANCE, "Gomu Gomu no Gigant Step Height Modifier", (double) 1.5F, Operation.ADDITION);
        KNOCKBACK_RESISTANCE = new AbilityAttributeModifier(AttributeHelper.MORPH_KNOCKBACK_RESISTANCE_UUID, INSTANCE, "Gomu Gomu no Gigant Knockback Resistance Modifier", (double) 1.0F, Operation.ADDITION);
        FALL_RESISTANCE_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_FALL_RESISTANCE_UUID, INSTANCE, "Gomu Gomu no Gigant Fall Resistance Modifier", (double) 10.0F, Operation.ADDITION);
        TOUGHNESS_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_TOUGHNESS_UUID, INSTANCE, "Gomu Gomu no Gigant Toughness Modifier", (double) 0.0F, Operation.ADDITION);
        HEALTH_BOOST_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_HEALTH_UUID, INSTANCE, "Gomu Gomu no Gigant Health Modifier", (double) 150.0F, Operation.ADDITION);
    }
}
