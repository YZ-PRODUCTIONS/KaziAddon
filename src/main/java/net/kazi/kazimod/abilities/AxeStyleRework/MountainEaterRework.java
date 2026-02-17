//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.AxeStyleRework;

import java.util.List;
import java.util.UUID;

import net.MrMagicalCart.cartaddon.abilities.axestyle.AxeHelper;
import net.MrMagicalCart.cartaddon.abilities.axestyle.BerserkAbility;
import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.init.CartAnimations;
import net.MrMagicalCart.cartaddon.init.CartParticleEffects;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3d;
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
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class MountainEaterRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "mountain_eater", new Pair[]{ImmutablePair.of("", (Object)null)});
    private static final float HOLD_TIME = 30.0F;
    private static final int COOLDOWN = 300;
    private static final float RANGE = 2.0F;
    private static final float DAMAGE = 20.0F;
    public static final AbilityCore<MountainEaterRework> INSTANCE;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this, true)).addStartEvent(this::startContinuityEvent).addTickEvent(this::duringContinuityEvent).addEndEvent(this::endContinuityEvent);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);
    private final ChangeStatsComponent changeStatsComponent = new ChangeStatsComponent(this);
    private static Interval damageTick = new Interval(8);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final HealComponent healComponent = new HealComponent(this);
    private static final AbilityAttributeModifier STEP_HEIGHT_MODIFIER;
    private final ChargeComponent chargeComponent =
            new ChargeComponent(this)
                    .addStartEvent(this::startChargeEvent)
                    .addTickEvent(this::tickChargeEvent)
                    .addEndEvent(this::endChargeEvent);


    public MountainEaterRework(AbilityCore<MountainEaterRework> core) {
        super(core);
        this.isNew = true;

        this.addComponents(new AbilityComponent[]{
                this.animationComponent,
                this.healComponent,
                this.changeStatsComponent,
                this.chargeComponent,      // ← must be INSIDE array with comma
                this.continuousComponent,
                this.rangeComponent,
                this.dealDamageComponent,
                this.hitTrackerComponent
        });

        this.changeStatsComponent.addAttributeModifier(ModAttributes.STEP_HEIGHT, STEP_HEIGHT_MODIFIER);
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addCanUseCheck(AbilityLimits::requiresAxe);
        this.addCanUseCheck(AbilityLimits::requirestwoAxe);
        this.addUseEvent(this::useEvent);
    }


    private void useEvent(LivingEntity entity, IAbility ability) {
        if (!this.chargeComponent.isCharging() && !this.continuousComponent.isContinuous()) {
            this.chargeComponent.startCharging(entity, 15.0F);
        }

        if (this.continuousComponent.isContinuous()) {
            this.continuousComponent.stopContinuity(entity);
        }

    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, CartAnimations.MOUNTAIN_EATER);
    }

    private void tickChargeEvent(LivingEntity entity, IAbility ability) {
        entity.addEffect(new EffectInstance((Effect) ModEffects.MOVEMENT_BLOCKED.get(), 2, 0, false, false));
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.startContinuity(entity, 25.0F);
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {

        this.changeStatsComponent.applyModifiers(entity);
        this.hitTrackerComponent.clearHits();
    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        if (entity.isAlive()) {
            if (entity instanceof PlayerEntity) {
                Vector3d look = entity.getLookAngle();
                Vector3d speed = look.multiply(2.1, (double)0.0F, 2.1);
                entity.move(MoverType.SELF, speed);
            }

            List<LivingEntity> list = this.rangeComponent.getTargetsInArea(entity, 4.0F);
            ItemStack mainHand = entity.getItemInHand(Hand.MAIN_HAND);
            float weaponDamage = (float)mainHand.getItem().getDamage(mainHand);
            BerserkAbility Berserk = (BerserkAbility)AbilityDataCapability.get(entity).getEquippedAbility(BerserkAbility.INSTANCE);
            boolean isBerserk = Berserk != null && Berserk.isContinuous();
            if (isBerserk) {
                list = this.rangeComponent.getTargetsInArea(entity, 6.0F);
            }

            for(LivingEntity target : list) {
                float damage = DAMAGE + weaponDamage;

                if (this.hitTrackerComponent.canHit(target)) {
                    this.dealDamageComponent.hurtTarget(entity, target, damage);
                    this.dealDamageComponent.getBonusManager().removeBonus(AxeHelper.AXE_DAMAGE_BONUS);
                    if (isBerserk) {
                        this.dealDamageComponent.getBonusManager().addBonus(AxeHelper.AXE_DAMAGE_BONUS, "Axe Damage Bonus", BonusOperation.MUL, 1.5F);
                    }
                }

                this.hitTrackerComponent.clearHits();
            }
        }

    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.changeStatsComponent.removeModifiers(entity);
        this.hitTrackerComponent.clearHits();
        this.cooldownComponent.startCooldown(entity, 180.0F);
        this.animationComponent.stop(entity);
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.getFightingStyle().equals(CartValues.DOUBLE_AXE) && questProps.hasFinishedQuest(CartQuests.AXE_TRIAL_06);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Mountain Eater", AbilityCategory.STYLE, MountainEaterRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, ContinuousComponent.getTooltip(80.0F), CooldownComponent.getTooltip(250.0F), RangeComponent.getTooltip(4.0F, RangeType.AOE)}).setSourceHakiNature(SourceHakiNature.IMBUING).setUnlockCheck(MountainEaterRework::canUnlock).build();
        STEP_HEIGHT_MODIFIER = new AbilityAttributeModifier(UUID.fromString("27db8674-ba38-463d-a84e-21ae454cc1de"), INSTANCE, "Mountain Eater Step Height Modifier", (double)1.5F, Operation.ADDITION);
    }
}