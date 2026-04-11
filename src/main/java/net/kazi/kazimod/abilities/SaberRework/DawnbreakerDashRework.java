//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package net.kazi.kazimod.abilities.SaberRework;

import java.util.List;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.DamageSource;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent.DamageState;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;

public class DawnbreakerDashRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "dawnbreaker_dash", new Pair[]{ImmutablePair.of("The user dashes forward with immense momentum, sending opponents flying. (Reduces dmg taken by 50% when in use)", (Object)null)});
    private static final float HOLD_TIME = 15.0F;
    private static final float COOLDOWN = 220.0F;
    private static final float DMG = 45.0F;
    private static final float RANGE = 3.0F;
    public static final AbilityCore<DawnbreakerDashRework> INSTANCE;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this, true)).addStartEvent(100, this::onStartContinuityEvent).addTickEvent(100, this::onTickContinuityEvent).addEndEvent(100, this::onEndContinuityEvent);
    private final DamageTakenComponent damageTakenComponent;
    private final RangeComponent rangeComponent;
    private final DealDamageComponent dealDamageComponent;
    private final HitTrackerComponent hitTrackerComponent;

    public DawnbreakerDashRework(AbilityCore<DawnbreakerDashRework> core) {
        super(core);
        this.damageTakenComponent = new DamageTakenComponent(this, this::onDamageTakenEvent, DamageState.ATTACK);
        this.rangeComponent = new RangeComponent(this);
        this.dealDamageComponent = new DealDamageComponent(this);
        this.hitTrackerComponent = new HitTrackerComponent(this);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.hitTrackerComponent, this.dealDamageComponent, this.rangeComponent, this.continuousComponent, this.damageTakenComponent});
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addCanUseCheck(AbilityHelper::canUseSwordsmanAbilities);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity, 15.0F);
    }

    private void onStartContinuityEvent(LivingEntity entity, IAbility ability) {
        float horizontalPropulsion = 4.5F;
        Vector3d speed = entity.getLookAngle().multiply((double)horizontalPropulsion, (double)0.0F, (double)horizontalPropulsion);
        AbilityHelper.setDeltaMovement(entity, speed.x, 0.3, speed.z);
    }

    private void onTickContinuityEvent(LivingEntity entity, IAbility ability) {
        if (entity.isAlive()) {
            List<LivingEntity> targets = this.rangeComponent.getTargetsInArea(entity, 3.0F);
            targets.remove(entity);
            float horizontalPropulsion = 3.0F;
            Vector3d pushSpeed = entity.getLookAngle().multiply((double)horizontalPropulsion, (double)0.0F, (double)horizontalPropulsion);

            for(LivingEntity target : targets) {
                if (this.hitTrackerComponent.canHit(target) && this.dealDamageComponent.hurtTarget(entity, target, 30.0F)) {
                    AbilityHelper.setDeltaMovement(target, pushSpeed.x, 0.2, pushSpeed.z);
                }
            }
        }

    }

    private void onEndContinuityEvent(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();
        this.cooldownComponent.startCooldown(entity, 220.0F);
    }

    private float onDamageTakenEvent(LivingEntity entity, IAbility ability, DamageSource source, float damage) {
        if (AbilityHelper.isDodging(entity)) {
            return damage;
        } else {
            return !this.continuousComponent.isContinuous() ? damage : damage * 0.5F;
        }
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.getFightingStyle().equals(CartValues.SABER) && questProps.hasFinishedQuest(CartQuests.SABER_TRIAL_06);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Dawnbreaker Dash", AbilityCategory.STYLE, DawnbreakerDashRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, DealDamageComponent.getTooltip(45.0F), CooldownComponent.getTooltip(220.0F), RangeComponent.getTooltip(3.0F, RangeType.AOE)}).setSourceType(new SourceType[]{SourceType.SLASH}).setSourceHakiNature(SourceHakiNature.IMBUING).setUnlockCheck(DawnbreakerDashRework::canUnlock).build();
    }
}
