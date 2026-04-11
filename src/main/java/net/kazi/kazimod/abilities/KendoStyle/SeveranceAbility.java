package net.kazi.kazimod.abilities.KendoStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.vector.Vector3d;
import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityAttributeModifier;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

public class SeveranceAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION =
            AbilityHelper.registerDescriptionText("kazimod", "severance",
                    new Pair[]{ImmutablePair.of(
                            "Strike your opponent from a distance, linking your spirits and severing their ability to heal for a short duration. Weapon attacks apply anti-heal and reduce healing by 50%.", null)});

    private static final float CHARGE_TIME = 50.0F; // 2.5 seconds (20 ticks/sec)
    private static final float CONTINUOUS_DURATION = 120.0F; // 6 seconds
    private static final float COOLDOWN = 600.0F; // 30 seconds
    private static final float RANGE = 15.0F;
    private static final double BIND_DISTANCE = 10.0;

    public static final AbilityCore<SeveranceAbility> INSTANCE;

    private final ChargeComponent chargeComponent =
            new ChargeComponent(this)
                    .addStartEvent(this::startChargeEvent)
                    .addTickEvent(this::duringChargeEvent)
                    .addEndEvent(this::endChargeEvent);

    private final ContinuousComponent continuousComponent =
            new ContinuousComponent(this, true)
                    .addStartEvent(this::startContinuityEvent)
                    .addTickEvent(this::duringContinuityEvent)
                    .addEndEvent(this::endContinuityEvent);

    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final ChangeStatsComponent changeStatsComponent = new ChangeStatsComponent(this);

    private static final AbilityAttributeModifier DAMAGE_REDUCTION_MODIFIER;

    private final List<LivingEntity> boundTargets = new ArrayList<>();

    public SeveranceAbility(AbilityCore<SeveranceAbility> core) {
        super(core);
        this.isNew = true;

        this.addComponents(new AbilityComponent[]{
                this.chargeComponent,
                this.continuousComponent,
                this.rangeComponent,
                this.changeStatsComponent
        });

        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!this.chargeComponent.isCharging() && !this.continuousComponent.isContinuous()) {
            this.chargeComponent.startCharging(entity, CHARGE_TIME);
        }
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        entity.addEffect(new EffectInstance((Effect) ModEffects.MOVEMENT_BLOCKED.get(), 2, 0, false, false));
    }

    private void duringChargeEvent(LivingEntity entity, IAbility ability) {
        entity.addEffect(new EffectInstance((Effect) ModEffects.MOVEMENT_BLOCKED.get(), 2, 0, false, false));
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        this.boundTargets.clear();

        List<LivingEntity> targets = this.rangeComponent.getTargetsInArea(entity, RANGE);
        this.boundTargets.addAll(targets);

        this.continuousComponent.startContinuity(entity, CONTINUOUS_DURATION);
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        // Apply 50% damage reduction to the user
        this.changeStatsComponent.addAttributeModifier(Attributes.ARMOR_TOUGHNESS, DAMAGE_REDUCTION_MODIFIER);
        this.changeStatsComponent.applyModifiers(entity);
    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        for (LivingEntity target : this.boundTargets) {
            if (!target.isAlive()) continue;

            double distance = target.distanceTo(entity);

            // If the target tries to move beyond 10 blocks, pull them back
            if (distance > BIND_DISTANCE) {
                Vector3d playerPos = entity.position();
                Vector3d targetPos = target.position();
                Vector3d direction = playerPos.subtract(targetPos).normalize();

                // Pull the target back to the edge of the bind radius
                double pullStrength = distance - BIND_DISTANCE;
                target.teleportToWithTicket(
                        targetPos.x + direction.x * pullStrength,
                        targetPos.y,
                        targetPos.z + direction.z * pullStrength
                );
            }
        }
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.changeStatsComponent.removeModifiers(entity);
        this.boundTargets.clear();
        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    public boolean isContinuous() {
        return this.continuousComponent.isContinuous();
    }

    public List<LivingEntity> getBoundTargets() {
        return this.boundTargets;
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Severance", AbilityCategory.STYLE, SeveranceAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        ContinuousComponent.getTooltip(CONTINUOUS_DURATION),
                        CooldownComponent.getTooltip(COOLDOWN),
                        RangeComponent.getTooltip(RANGE, RangeType.AOE)
                })
                .setSourceType(new SourceType[]{SourceType.SLASH})
                .build();

        DAMAGE_REDUCTION_MODIFIER = new AbilityAttributeModifier(
                UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"),
                INSTANCE,
                "Severance Damage Reduction",
                0.5,
                Operation.MULTIPLY_TOTAL
        );
    }
}
