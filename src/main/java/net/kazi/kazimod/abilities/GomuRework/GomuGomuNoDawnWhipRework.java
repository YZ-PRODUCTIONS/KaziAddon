//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.GomuRework;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.abilities.gomu.GearFifthAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUseResult;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
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
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;

public class GomuGomuNoDawnWhipRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "gomu_gomu_no_dawn_whip", new Pair[]{ImmutablePair.of("Launches the user forward hitting everybody in their path.", (Object)null)});
    private static final int COOLDOWN = 140;
    private static final int HOLD_TIME = 100;
    private static final float RANGE = 2.5F;
    private static final int DAMAGE = 30;
    public static final AbilityCore<GomuGomuNoDawnWhipRework> INSTANCE;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this)).addStartEvent(this::startContinuityEvent).addTickEvent(this::duringContinuityEvent).addEndEvent(this::endContinuityEvent);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);

    public GomuGomuNoDawnWhipRework(AbilityCore core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.hitTrackerComponent, this.animationComponent, this.rangeComponent, this.dealDamageComponent});
        this.addCanUseCheck(this::canUse);
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity, 100.0F);
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();
        this.animationComponent.start(entity, ModAnimations.GOMU_DAWN_WHIP);
    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        if (this.continuousComponent.getContinueTime() % 20.0F == 0.0F) {
            this.hitTrackerComponent.clearHits();
        }

        Vector3d lookAngle = entity.getLookAngle();
        Vector3d speed = lookAngle.multiply(1.35, 1.35, 1.35);
        AbilityHelper.setDeltaMovement(entity, speed.x, speed.y, speed.z);
        entity.fallDistance = 0.0F;

        for (LivingEntity target : this.rangeComponent.getTargetsInArea(entity, 2.5F)) {
            if (this.hitTrackerComponent.canHit(target)) {
                this.dealDamageComponent.hurtTarget(entity, target, 30.0F);
            }
        }
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        entity.fallDistance = 0.0F;
        this.animationComponent.stop(entity);
        this.cooldownComponent.startCooldown(entity, 140.0F);
    }

    private AbilityUseResult canUse(LivingEntity entity, IAbility ability) {
        IAbilityData props = AbilityDataCapability.get(entity);
        GearFifthRework gearFifth = (GearFifthRework)props.getEquippedAbility(GearFifthRework.INSTANCE);
        return gearFifth != null && gearFifth.isContinuous() ? AbilityUseResult.success() : AbilityUseResult.fail((ITextComponent)null);
    }

    private static boolean canUnlock(LivingEntity user) {
        return DevilFruitCapability.get(user).hasAwakenedFruit();
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Gomu Gomu no Dawn Whip", AbilityCategory.DEVIL_FRUITS, GomuGomuNoDawnWhipRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(140.0F), ContinuousComponent.getTooltip(100.0F), RangeComponent.getTooltip(2.5F, RangeType.AOE), DealDamageComponent.getTooltip(30.0F)}).setSourceHakiNature(SourceHakiNature.HARDENING).setSourceType(new SourceType[]{SourceType.FIST}).setUnlockCheck(GomuGomuNoDawnWhipRework::canUnlock).build();
    }
}
