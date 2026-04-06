//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.BaneRework;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.abilities.bane.SpringHopperAbility;
import xyz.pixelatedw.mineminenomi.abilities.bane.SpringSnipeAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;

public class SpringSnipeRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("kazimod", "spring_snipe", new Pair[]{ImmutablePair.of("Turning the user's forelegs into springs, they can launch themselves directly at the opponent", (Object)null)});
    private static final int CHARGE_TIME = 10;
    private static final float MIN_COOLDOWN = 100.0F;  // 4 seconds (20 ticks/sec)
    private static final float MAX_COOLDOWN = 160.0F; // 12 seconds
    private static final float RANGE = 1.6F;
    private static final float DAMAGE = 5.0F;
    public static final AbilityCore<SpringSnipeRework> INSTANCE;
    private final ChargeComponent chargeComponent = (new ChargeComponent(this)).addStartEvent(this::startChargeEvent).addTickEvent(this::duringChargeEvent).addEndEvent(this::endChargeEvent);
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this, true)).addStartEvent(this::startContinuousEvent).addTickEvent(this::duringContinuityEvent).addEndEvent(this::endContinuityEvent);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);

    public SpringSnipeRework(AbilityCore<SpringSnipeRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.chargeComponent, this.rangeComponent, this.dealDamageComponent, this.animationComponent, this.hitTrackerComponent, this.continuousComponent});
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addCanUseCheck((entity, ability) -> {
            for (int i = 1; i <= 18; i++) {
                if (!entity.level.getBlockState(entity.blockPosition().below(i)).isAir()) {
                    return AbilityUseResult.success();
                }
            }
            return AbilityUseResult.fail(new net.minecraft.util.text.StringTextComponent("Too high above the ground"));
        });

        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        this.chargeComponent.startCharging(entity, 10.0F);
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();
        IAbilityData abilityDataProps = AbilityDataCapability.get(entity);
        SpringHopperAbility springHopper = (SpringHopperAbility)abilityDataProps.getEquippedAbility(SpringHopperAbility.INSTANCE);
        if (springHopper != null && !springHopper.canUse(entity).isFail()) {
            springHopper.getComponent(ModAbilityKeys.CONTINUOUS).ifPresent((comp) -> {
                if (!comp.isContinuous()) {
                    comp.startContinuity(entity);
                }

            });
            this.hitTrackerComponent.clearHits();
        }
    }

    private void duringChargeEvent(LivingEntity entity, IAbility ability) {
        AbilityHelper.setDeltaMovement(entity, (double)0.0F, (double)0.0F, (double)0.0F);
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.startContinuity(entity, 10.0F);
        this.animationComponent.start(entity, ModAnimations.SHOOT_SELF_FORWARD);
    }

    private void startContinuousEvent(LivingEntity entity, IAbility ability) {
        Vector3d speed = entity.getLookAngle().multiply((double)6.0F, (double)6.0F, (double)6.0F);
        AbilityHelper.setDeltaMovement(entity, speed.x, speed.y, speed.z);
    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        for(LivingEntity target : this.rangeComponent.getTargetsInLine(entity, 1.6F, 1.25F)) {
            if (this.hitTrackerComponent.canHit(target)) {
                this.dealDamageComponent.hurtTarget(entity, target, 5.0F);
            }
        }

    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);

        // Scale cooldown based on missing HP: lower HP = longer cooldown
        // At full HP (100%) -> MIN_COOLDOWN (80 ticks / 4s)
        // At 0% HP          -> MAX_COOLDOWN (240 ticks / 12s)
        float percentHp = entity.getHealth() / entity.getMaxHealth(); // 1.0 = full, 0.0 = empty
        float scaledCooldown = MIN_COOLDOWN + (MAX_COOLDOWN - MIN_COOLDOWN) * (1.0F - percentHp);
        this.cooldownComponent.startCooldown(entity, scaledCooldown);
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Spring Snipe", AbilityCategory.DEVIL_FRUITS, SpringSnipeRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(60.0F, 200.0F), ChargeComponent.getTooltip(10.0F), RangeComponent.getTooltip(1.6F, RangeType.LINE), DealDamageComponent.getTooltip(5.0F)}).setSourceType(new SourceType[]{SourceType.PHYSICAL}).setSourceHakiNature(SourceHakiNature.HARDENING).build();
    }
}