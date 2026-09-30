package net.kazi.kazimod.abilities.ToriPhoenixRework;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.abilities.PropelledFlightAbility;
import xyz.pixelatedw.mineminenomi.abilities.toriphoenix.BlueBirdAbility;
import xyz.pixelatedw.mineminenomi.abilities.toriphoenix.PhoenixFlightAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.AttributeHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.DevilFruitHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.*;

import java.util.function.Predicate;

public class PhoenixAssaultPointRework extends MorphAbility2 {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("kazimod", "phoenix_assault_point", new Pair[]{
            ImmutablePair.of("Transforms the user into a half-phoenix hybrid, which focuses on speed and healing.", null)
    });
    public static final AbilityCore<PhoenixAssaultPointRework> INSTANCE;
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private static final AbilityAttributeModifier REGEN_RATE_MODIFIER;
    private static final AbilityAttributeModifier STRENGTH_MODIFIER;
    private static final AbilityAttributeModifier TOUGHNESS_MODIFIER;

    public PhoenixAssaultPointRework(AbilityCore<PhoenixAssaultPointRework> core) {
        super(core);
        Predicate<LivingEntity> isMorphed = entity -> super.morphComponent.isMorphed();
        super.statsComponent.addAttributeModifier(ModAttributes.REGEN_RATE, REGEN_RATE_MODIFIER, isMorphed);
        super.statsComponent.addAttributeModifier(ModAttributes.PUNCH_DAMAGE, STRENGTH_MODIFIER, isMorphed);
        super.statsComponent.addAttributeModifier(ModAttributes.TOUGHNESS, TOUGHNESS_MODIFIER, isMorphed);
        super.continuousComponent.addStartEvent(this::onContinuityStart).addTickEvent(100, this::onContinuityTick).addEndEvent(this::onContinuityEnd);
        super.addComponents(this.animationComponent);
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            IAbilityData abilityDataProps = AbilityDataCapability.get(entity);
            if (abilityDataProps != null) {
                PropelledFlightAbility flightAbility = (PropelledFlightAbility) abilityDataProps.getPassiveAbility(PhoenixFlightAbility.INSTANCE);
                if (flightAbility != null && !flightAbility.isPaused()) {
                    PropelledFlightAbility.enableFlight((PlayerEntity) entity);
                }
            }
        }
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        boolean isFlying = !entity.isOnGround() && DevilFruitHelper.getDifferenceToFloor(entity) > 1.0D;
        if (entity instanceof PlayerEntity) {
            isFlying |= ((PlayerEntity) entity).abilities.flying;
        }
        if (isFlying) {
            if (this.animationComponent.isStopped()) {
                this.animationComponent.start(entity, ModAnimations.PHOENIX_ASSAULT_FLY, -1, e -> {
                    BlueBirdAbility blueBirdAbility = AbilityDataCapability.get(entity).getEquippedAbility(BlueBirdAbility.INSTANCE);
                    return blueBirdAbility != null && ((ContinuousComponent) blueBirdAbility.getComponent(ModAbilityKeys.CONTINUOUS).get()).isContinuous();
                });
            }
        } else {
            this.animationComponent.stop(entity);
        }
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            this.animationComponent.stop(entity);
            IAbilityData abilityDataProps = AbilityDataCapability.get(entity);
            if (abilityDataProps != null) {
                PropelledFlightAbility flightAbility = (PropelledFlightAbility) abilityDataProps.getPassiveAbility(PhoenixFlightAbility.INSTANCE);
                if (flightAbility != null) {
                    PropelledFlightAbility.disableFlight((PlayerEntity) entity);
                }
            }
        }
    }

    @Override
    public MorphInfo getTransformation() {
        return ModMorphs.PHOENIX_ASSAULT.get();
    }

    static {
        INSTANCE = new AbilityCore.Builder<PhoenixAssaultPointRework>("Phoenix Assault Point", AbilityCategory.DEVIL_FRUITS, PhoenixAssaultPointRework::new)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(10.0F), ContinuousComponent.getTooltip(), ChangeStatsComponent.getTooltip())
                .setIcon(new ResourceLocation("mineminenomi", "textures/abilities/phoenix_assault_point.png"))
                .build();
        REGEN_RATE_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_REGEN_RATE_UUID, INSTANCE, "Phoenix Assault Point Health Regeneration Speed Modifier", 0.6D, Operation.ADDITION);
        STRENGTH_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_STRENGTH_UUID, INSTANCE, "Phoenix Assault Point Strength Modifier", 3.0D, Operation.ADDITION);
        TOUGHNESS_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_TOUGHNESS_UUID, INSTANCE, "Phoenix Assault Point Toughness Modifier", 1.0D, Operation.ADDITION);
    }
}
