package net.kazi.kazimod.abilities.GoroRework;

import java.awt.Color;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.abilities.PropelledFlightAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityAttributeModifier;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityOverlay;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChangeStatsComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTriggerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.MorphComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.SkinOverlayComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.AttributeHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.config.ClientConfig;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.entities.LightningDischargeEntity;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModMorphs;
import xyz.pixelatedw.mineminenomi.init.ModTags;
import xyz.pixelatedw.mineminenomi.packets.server.ability.SRecalculateEyeHeightPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

/** Kazi-owned copy of Mine Mine no Mi 0.10.11's Volt Amaru. */
public class VoltAmaruRework extends Ability {
    private static final ResourceLocation DEFAULT_ICON =
            new ResourceLocation("mineminenomi", "textures/abilities/volt_amaru.png");
    private static final ResourceLocation ALT_ICON =
            new ResourceLocation("mineminenomi", "textures/abilities/alts/volt_amaru.png");
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "volt_amaru",
            new Pair[]{ImmutablePair.of(
                    "Transforms the user into a powerful, lightning giant massively boosting physical attributes and lightning attacks",
                    null)});

    public static final AbilityCore<VoltAmaruRework> INSTANCE =
            new AbilityCore.Builder<VoltAmaruRework>(
                    "Volt Amaru", AbilityCategory.DEVIL_FRUITS, VoltAmaruRework::new)
                    .addDescriptionLine(DESCRIPTION)
                    .addAdvancedDescriptionLine(
                            AbilityDescriptionLine.NEW_LINE,
                            CooldownComponent.getTooltip(1000.0F),
                            ChargeComponent.getTooltip(20.0F),
                            ContinuousComponent.getTooltip(400.0F),
                            ChangeStatsComponent.getTooltip())
                    .setIcon(DEFAULT_ICON)
                    .build();

    private static final Color COLOR = WyHelper.hexToRGB("#F0EC7155");
    private static final AbilityOverlay OVERLAY = new AbilityOverlay.Builder()
            .setColor(COLOR)
            .setRenderType(AbilityOverlay.RenderType.ENERGY)
            .build();
    private static final AbilityOverlay OVERLAY_ALT = new AbilityOverlay.Builder()
            .setColor(ElThorRework.BLUE_THUNDER)
            .setRenderType(AbilityOverlay.RenderType.ENERGY)
            .build();

    private static final AbilityAttributeModifier REACH_MODIFIER =
            new AbilityAttributeModifier(
                    AttributeHelper.MORPH_ATTACK_REACH_UUID, INSTANCE,
                    "Volt Amaru Reach Modifier", 4.8D, AttributeModifier.Operation.ADDITION);
    private static final AbilityAttributeModifier KNOCKBACK_RESISTANCE =
            new AbilityAttributeModifier(
                    AttributeHelper.MORPH_KNOCKBACK_RESISTANCE_UUID, INSTANCE,
                    "Volt Amaru Knockback Resistance Modifier", 2.0D,
                    AttributeModifier.Operation.ADDITION);
    private static final AbilityAttributeModifier TOUGHNESS_MODIFIER =
            new AbilityAttributeModifier(
                    AttributeHelper.MORPH_TOUGHNESS_UUID, INSTANCE,
                    "Volt Amaru Toughness Modifier", 2.0D,
                    AttributeModifier.Operation.ADDITION);
    private static final AbilityAttributeModifier HEALTH_BOOST =
            new AbilityAttributeModifier(
                    AttributeHelper.MORPH_HEALTH_UUID, INSTANCE,
                    "Volt Amaru Health Modifier", 20.0D,
                    AttributeModifier.Operation.ADDITION);
    private static final AbilityAttributeModifier STRENGTH_MODIFIER =
            new AbilityAttributeModifier(
                    AttributeHelper.MORPH_STRENGTH_UUID, INSTANCE,
                    "Volt Amaru Strength Modifier", 12.0D,
                    AttributeModifier.Operation.ADDITION);
    private static final AbilityAttributeModifier ATTACK_SPEED_MODIFIER =
            new AbilityAttributeModifier(
                    AttributeHelper.MORPH_ATTACK_SPEED_UUID, INSTANCE,
                    "Volt Amaru Attack Speed Modifier", 1.0D,
                    AttributeModifier.Operation.MULTIPLY_BASE);
    private static final AbilityAttributeModifier JUMP_BOOST =
            new AbilityAttributeModifier(
                    AttributeHelper.MORPH_JUMP_BOOST_UUID, INSTANCE,
                    "Volt Amaru Jump Modifier", 5.0D,
                    AttributeModifier.Operation.MULTIPLY_BASE);

    private final ChargeComponent chargeComponent =
            new ChargeComponent(this)
                    .addStartEvent(this::onChargeStart)
                    .addEndEvent(this::onChargeEnd);
    private final ContinuousComponent continuousComponent =
            new ContinuousComponent(this, true)
                    .addStartEvent(this::onContinuityStart)
                    .addEndEvent(this::onContinuityEnd);
    private final ChangeStatsComponent changeStatsComponent = new ChangeStatsComponent(this);
    private final MorphComponent morphComponent = new MorphComponent(this);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final HitTriggerComponent hitTriggerComponent =
            new HitTriggerComponent(this)
                    .addOnHitEvent(this::onHitEvent)
                    .addTryHitEvent(this::tryHitEvent);
    private final SkinOverlayComponent skinOverlayComponent =
            new SkinOverlayComponent(this, OVERLAY);

    public VoltAmaruRework(AbilityCore<VoltAmaruRework> core) {
        super(core);
        this.isNew = true;
        Predicate<LivingEntity> isMorphActive = entity -> morphComponent.isMorphed();
        changeStatsComponent.addAttributeModifier(
                (Supplier<Attribute>) ForgeMod.REACH_DISTANCE,
                REACH_MODIFIER, isMorphActive);
        changeStatsComponent.addAttributeModifier(
                (Supplier<Attribute>) ModAttributes.TOUGHNESS,
                TOUGHNESS_MODIFIER, isMorphActive);
        changeStatsComponent.addAttributeModifier(
                Attributes.MAX_HEALTH, HEALTH_BOOST, isMorphActive);
        changeStatsComponent.addAttributeModifier(
                (Supplier<Attribute>) ModAttributes.PUNCH_DAMAGE,
                STRENGTH_MODIFIER, isMorphActive);
        changeStatsComponent.addAttributeModifier(
                Attributes.ATTACK_SPEED, ATTACK_SPEED_MODIFIER, isMorphActive);
        changeStatsComponent.addAttributeModifier(
                (Supplier<Attribute>) ModAttributes.JUMP_HEIGHT,
                JUMP_BOOST, isMorphActive);
        changeStatsComponent.addAttributeModifier(
                (Supplier<Attribute>) ModAttributes.ATTACK_RANGE,
                REACH_MODIFIER, isMorphActive);
        changeStatsComponent.addAttributeModifier(
                Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE, isMorphActive);

        this.addComponents(
                chargeComponent, continuousComponent, changeStatsComponent,
                morphComponent, dealDamageComponent, hitTriggerComponent,
                skinOverlayComponent);
        this.addUseEvent(this::onUseEvent);
        this.addEquipEvent(this::onEquip);
    }

    private void onEquip(LivingEntity entity, Ability ability) {
        this.setDisplayIcon(DEFAULT_ICON);
        if (ClientConfig.INSTANCE.isGoroBlue()) {
            this.setDisplayIcon(ALT_ICON);
            skinOverlayComponent.removeOverlay(OVERLAY);
            skinOverlayComponent.addOverlay(OVERLAY_ALT);
        }
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!continuousComponent.isContinuous()) {
            chargeComponent.startCharging(entity, 20.0F);
        } else {
            continuousComponent.stopContinuity(entity);
        }
    }

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        skinOverlayComponent.showAll(entity);
        LightningDischargeEntity ball = new LightningDischargeEntity(
                entity, entity.getX(), entity.getY(), entity.getZ(), entity.yRot, entity.xRot);
        ball.setSize(4.0F);
        ball.setLightningLength(10.0F);
        ball.setAliveTicks(20);
        ball.setColor(
                ClientConfig.INSTANCE.isGoroBlue()
                        ? ElThorRework.BLUE_THUNDER : ElThorRework.YELLOW_THUNDER);
        entity.level.addFreshEntity(ball);
        entity.addEffect(new EffectInstance(
                ModEffects.MOVEMENT_BLOCKED.get(), 20, 1, false, false));
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        continuousComponent.startContinuity(entity, 400.0F);
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        morphComponent.startMorph(entity, (MorphInfo) ModMorphs.VOLT_AMARU.get());
        changeStatsComponent.applyModifiers(entity);
        MinecraftForge.EVENT_BUS.post(new EntityEvent.Size(
                entity, entity.getPose(), entity.getDimensions(entity.getPose()), entity.getBbHeight()));
        entity.refreshDimensions();
        if (!entity.level.isClientSide) {
            WyNetwork.sendToAllTrackingAndSelf(
                    new SRecalculateEyeHeightPacket(entity.getId()), entity);
        }

        IAbilityData abilityData = AbilityDataCapability.get(entity);
        if (abilityData == null) return;
        PropelledFlightAbility flight =
                abilityData.getPassiveAbility(VoltAmaruFlightRework.INSTANCE);
        if (flight != null && !flight.isPaused() && entity instanceof PlayerEntity) {
            ((PlayerEntity) entity).abilities.flying = true;
            PropelledFlightAbility.enableFlight((PlayerEntity) entity);
        }
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        skinOverlayComponent.hideAll(entity);
        morphComponent.stopMorph(entity);
        changeStatsComponent.removeModifiers(entity);
        cooldownComponent.startCooldown(entity, 1000.0F);

        IAbilityData abilityData = AbilityDataCapability.get(entity);
        if (abilityData == null) return;
        PropelledFlightAbility flight =
                abilityData.getPassiveAbility(VoltAmaruFlightRework.INSTANCE);
        if (flight != null && entity instanceof PlayerEntity) {
            PropelledFlightAbility.disableFlight((PlayerEntity) entity);
        }
    }

    private HitTriggerComponent.HitResult tryHitEvent(
            LivingEntity entity, LivingEntity target, ModDamageSource source, IAbility ability) {
        return morphComponent.isMorphed()
                ? HitTriggerComponent.HitResult.HIT
                : HitTriggerComponent.HitResult.PASS;
    }

    private boolean onHitEvent(
            LivingEntity entity, LivingEntity target, ModDamageSource source, IAbility ability) {
        float conductivity = ModTags.Items.CONDUCTIVE.getValue(
                entity.getMainHandItem().getItem());
        if (conductivity > 0.5F) {
            AbilityHelper.setSecondsOnFireBy(target, 5, entity);
            ModDamageSource newSource = (ModDamageSource) dealDamageComponent.getDamageSource(entity);
            newSource.ignore();
            dealDamageComponent.hurtTarget(entity, target, conductivity * 3.0F, newSource);
        }
        return true;
    }
}
