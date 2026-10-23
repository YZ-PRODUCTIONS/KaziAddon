package net.kazi.kazimod.abilities.TripelT;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;

public class ArrowsOfLightAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "arrows_of_light",
            new Pair[]{ImmutablePair.of("Summon five overhead portals that fire heavenly arrows toward your aim.", null)});

    private static final float CHARGE_TIME = 10.0F;
    private static final float CONTINUOUS_TIME = 50.0F;
    private static final float COOLDOWN = 380.0F;
    private static final int ARROW_INTERVAL = 4;

    public static final AbilityCore<ArrowsOfLightAbility> INSTANCE;

    private final ChargeComponent chargeComponent =
            new ChargeComponent(this).addStartEvent(this::onChargeStart).addTickEvent(this::onChargeTick).addEndEvent(this::onChargeEnd);
    private final ContinuousComponent continuousComponent =
            new ContinuousComponent(this).addStartEvent(this::onContinuityStart).addTickEvent(this::onContinuityTick).addEndEvent(this::onContinuityEnd);
    private final AnimationComponent animationComponent = new AnimationComponent(this);

    private Vector3d targetPos;
    private List<net.kazi.kazimod.preserved.sahur.SahurLightEntity> portals = new ArrayList<>();
    private int tickCount = 0;

    public ArrowsOfLightAbility(AbilityCore<ArrowsOfLightAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.chargeComponent,
                this.continuousComponent,
                this.animationComponent
        });
        this.addCanUseCheck((entity, ability) -> TripelTHelper.canUseTripelTMove(entity));
        this.addUseEvent(this::onUse);
    }

    private void onUse(LivingEntity entity, IAbility ability) {
        if (!this.chargeComponent.isCharging() && !this.continuousComponent.isContinuous()) {
            TripelTHelper.playTungSound(entity, 2.2F, 1.12F);
            this.chargeComponent.startCharging(entity, CHARGE_TIME);
        }
    }

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        this.targetPos = TripelTHelper.getAimPoint(entity, 64.0D);
        this.animationComponent.start(entity, ModAnimations.RAISE_RIGHT_ARM);
    }

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        this.targetPos = TripelTHelper.getAimPoint(entity, 64.0D);
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);
        if (this.targetPos != null) {
            spawnPortals(entity);
        }
        this.continuousComponent.startContinuity(entity, CONTINUOUS_TIME);
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        this.tickCount = 0;
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        this.tickCount++;
        this.targetPos = TripelTHelper.getAimPoint(entity, 64.0D);
        if (!entity.level.isClientSide) {
            if (this.tickCount % ARROW_INTERVAL == 0) {
                spawnArrowBarrage(entity);
            }
        }
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        net.kazi.kazimod.preserved.sahur.HeavenlyArrows.close(this.portals);
        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    private void spawnPortals(LivingEntity entity) {
        if (entity.level.isClientSide) {
            return;
        }

        net.kazi.kazimod.preserved.sahur.HeavenlyArrows.close(this.portals);
        this.portals = net.kazi.kazimod.preserved.sahur.HeavenlyArrows.open(entity, (int)CONTINUOUS_TIME);

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 4.0F, 1.0F);
    }

    private void spawnArrowBarrage(LivingEntity entity) {
        net.kazi.kazimod.preserved.sahur.HeavenlyArrows.volley(entity, this.portals, 18.0F);
    }

    static {
        INSTANCE = new AbilityCore.Builder<>("Arrows of Light", AbilityCategory.DEVIL_FRUITS, ArrowsOfLightAbility::new)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        ContinuousComponent.getTooltip(CONTINUOUS_TIME),
                        CooldownComponent.getTooltip(COOLDOWN)
                })
                .setSourceHakiNature(SourceHakiNature.IMBUING)
                .setSourceType(new SourceType[]{SourceType.PROJECTILE})
                .build();
    }
}
