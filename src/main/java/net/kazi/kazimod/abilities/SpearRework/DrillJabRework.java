//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.SpearRework;

import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.init.CartAbilityPools;
import net.MrMagicalCart.cartaddon.init.CartAnimations;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityPool2;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

public class DrillJabRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "drill_jab", new Pair[]{ImmutablePair.of("The user inflates his forearm until they release it. On release, the pressure explodes and fires a flamming fist that deals great knockback. (When charging this move, the damage and range of the move increases)", (Object)null)});
    private static final int HOLD_TIME = 10;
    private static final int COOLDOWN = 300;
    private static final float RANGE = 3.0F;
    private static final float DAMAGE = 30.0F;
    public static final AbilityCore<DrillJabRework> INSTANCE;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this, true)).addStartEvent(this::startContinuityEvent).addTickEvent(this::duringContinuityEvent).addEndEvent(this::endContinuityEvent);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);
    private final PoolComponent poolComponent;

    public DrillJabRework(AbilityCore<DrillJabRework> core) {
        super(core);
        this.poolComponent = new PoolComponent(this, CartAbilityPools.MOCHI_ABILITY, new AbilityPool2[0]);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.poolComponent, this.continuousComponent, this.rangeComponent, this.dealDamageComponent, this.animationComponent, this.hitTrackerComponent});
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addCanUseCheck(AbilityLimits::requiresSpear);
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity, 10.0F);
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();
        this.animationComponent.start(entity, CartAnimations.DRILL_JAB);
    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        if (entity.isAlive()) {
            Vector3d look = entity.getLookAngle();
            Vector3d speed = look.multiply(1.4, (double)0.0F, 1.4);
            entity.move(MoverType.SELF, speed);

            for(LivingEntity target : this.rangeComponent.getTargetsInArea(entity, 3.0F)) {
                if (this.hitTrackerComponent.canHit(target)) {
                    this.dealDamageComponent.hurtTarget(entity, target, 30.0F);
                    target.addEffect(new EffectInstance((Effect)ModEffects.BLEEDING.get(), 300, 0, false, false));
                }
            }

            for(Entity target : this.hitTrackerComponent.getHits()) {
                target.teleportTo(entity.getX() + look.x, entity.getY(), entity.getZ() + look.z);
            }
        }

    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);
        this.cooldownComponent.startCooldown(entity, 300.0F);
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.getFightingStyle().equals(CartValues.SPEAR) && questProps.hasFinishedQuest(CartQuests.SPEAR_02);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Drill Jab", AbilityCategory.STYLE, DrillJabRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, DealDamageComponent.getTooltip(30.0F), RangeComponent.getTooltip(3.0F, RangeType.LINE), CooldownComponent.getTooltip(300.0F)}).setSourceType(new SourceType[]{SourceType.SLASH}).setSourceHakiNature(SourceHakiNature.IMBUING).setUnlockCheck(DrillJabRework::canUnlock).build();
    }
}
