package net.kazi.kazimod.abilities.ToriPhoenixRework;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.abilities.PropelledFlightAbility;
import xyz.pixelatedw.mineminenomi.abilities.toriphoenix.PhoenixFlightAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChangeStatsComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.AttributeHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.init.ModMorphs;

import java.util.function.Predicate;

public class PhoenixFlyPointRework extends MorphAbility2 {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("kazimod", "phoenix_fly_point", new Pair[]{
            ImmutablePair.of("Transforms the user into a phoenix, which focuses on speed and healing.", null)
    });
    public static final AbilityCore<PhoenixFlyPointRework> INSTANCE;
    private static final AbilityAttributeModifier REGEN_RATE_MODIFIER;
    private static final AbilityAttributeModifier FALL_DAMAGE_MODIFIER;
    private static final AbilityAttributeModifier TOUGHNESS_MODIFIER;

    public PhoenixFlyPointRework(AbilityCore<PhoenixFlyPointRework> core) {
        super(core);
        Predicate<LivingEntity> isMorphed = entity -> super.morphComponent.isMorphed();
        super.statsComponent.addAttributeModifier(ModAttributes.REGEN_RATE, REGEN_RATE_MODIFIER, isMorphed);
        super.statsComponent.addAttributeModifier(ModAttributes.FALL_RESISTANCE, FALL_DAMAGE_MODIFIER, isMorphed);
        super.statsComponent.addAttributeModifier(ModAttributes.TOUGHNESS, TOUGHNESS_MODIFIER, isMorphed);
        super.continuousComponent.addStartEvent(this::onContinuityStart).addEndEvent(this::onContinuityEnd);
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

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
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
        return ModMorphs.PHOENIX_FLY.get();
    }

    static {
        INSTANCE = new AbilityCore.Builder<PhoenixFlyPointRework>("Phoenix Fly Point", AbilityCategory.DEVIL_FRUITS, PhoenixFlyPointRework::new)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(10.0F), ContinuousComponent.getTooltip(), ChangeStatsComponent.getTooltip())
                .setIcon(new ResourceLocation("mineminenomi", "textures/abilities/phoenix_fly_point.png"))
                .build();
        REGEN_RATE_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_REGEN_RATE_UUID, INSTANCE, "Phoenix Fly Point Health Regeneration Speed Modifier", 1.0D, Operation.ADDITION);
        FALL_DAMAGE_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_FALL_RESISTANCE_UUID, INSTANCE, "Phoenix Fly Point Fall Damage Modifier", 500.0D, Operation.ADDITION);
        TOUGHNESS_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_TOUGHNESS_UUID, INSTANCE, "Phoenix Fly Point Toughness Modifier", 1.0D, Operation.ADDITION);
    }
}
