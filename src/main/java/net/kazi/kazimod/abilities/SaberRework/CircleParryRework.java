//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package net.kazi.kazimod.abilities.SaberRework;

import net.MrMagicalCart.cartaddon.init.CartAbilityPools;
import net.MrMagicalCart.cartaddon.init.CartAnimations;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.HandSide;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.StackComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent.DamageState;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

public class CircleParryRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("kazimod", "circle_parry", new Pair[]{ImmutablePair.of("For the first few moments the user perfectly blocks incoming damage, however the longer the ability is active, the worse their guard gets.", (Object)null)});
    private static final float HOLD_TIME = 60.0F;
    private static final float COOLDOWN = 180.0F;
    public static final AbilityCore<CircleParryRework> INSTANCE;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this, true)).addStartEvent(100, this::onStartContinuityEvent).addTickEvent(100, this::onTickContinuityEvent).addEndEvent(100, this::onEndContinuityEvent);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final PoolComponent poolComponent;
    private final DamageTakenComponent damageTakenComponent;
    private final StackComponent stackComponent = new StackComponent(this, 5);

    public CircleParryRework(AbilityCore<CircleParryRework> core) {
        super(core);
        this.poolComponent = new PoolComponent(this, CartAbilityPools.PARRY_COUNTER, new AbilityPool2[]{ModAbilityPools.GRAB_ABILITY});
        this.damageTakenComponent = new DamageTakenComponent(this, this::onDamageTakenEvent, DamageState.ATTACK);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.stackComponent, this.continuousComponent, this.animationComponent, this.poolComponent, this.damageTakenComponent});
        this.addCanUseCheck(AbilityHelper::canUseSwordsmanAbilities);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity, 60.0F);
    }

    private void onStartContinuityEvent(LivingEntity entity, IAbility ability) {
        if (entity.getMainArm() == HandSide.RIGHT) {
            this.animationComponent.start(entity, CartAnimations.STEEL_RESOLVE_RIGHT);
        } else if (entity.getMainArm() == HandSide.LEFT) {
            this.animationComponent.start(entity, CartAnimations.STEEL_RESOLVE_LEFT);
        }

    }

    private void onTickContinuityEvent(LivingEntity entity, IAbility ability) {
        if (this.stackComponent.getStacks() <= 0) {
            this.continuousComponent.stopContinuity(entity);
        }

        if (this.continuousComponent.getContinueTime() >= 20.0F && this.continuousComponent.getContinueTime() % 10.0F == 0.0F) {
            this.stackComponent.addStacks(entity, this, -1);
        }

        entity.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 5, 2, false, false));
    }

    private void onEndContinuityEvent(LivingEntity entity, IAbility ability) {
        this.stackComponent.revertStacksToDefault(entity, this);
        this.animationComponent.stop(entity);
        this.cooldownComponent.startCooldown(entity, 180.0F);
    }

    private float onDamageTakenEvent(LivingEntity entity, IAbility ability, DamageSource source, float damage) {
        if (AbilityHelper.isDodging(entity)) {
            return damage;
        } else if (!this.continuousComponent.isContinuous()) {
            return damage;
        } else {
            ItemStack stack = entity.getMainHandItem();
            stack.hurtAndBreak(1, entity, (user) -> user.broadcastBreakEvent(EquipmentSlotType.MAINHAND));
            if (this.stackComponent.getStacks() == 3) {
                entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.GUARD.get(), SoundCategory.PLAYERS, 3.0F, 1.0F);
                return 0.5F;
            } else if (this.stackComponent.getStacks() == 2) {
                entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.GUARD.get(), SoundCategory.PLAYERS, 3.0F, 1.6F);
                return damage * 0.5F;
            } else if (this.stackComponent.getStacks() == 1) {
                entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.GUARD.get(), SoundCategory.PLAYERS, 3.0F, 1.8F);
                return damage * 0.5F;
            } else {
                return damage;
            }
        }
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.getFightingStyle().equals(CartValues.SABER) && questProps.hasFinishedQuest(CartQuests.SABER_TRIAL_04);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Circle Parry", AbilityCategory.STYLE, CircleParryRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, ContinuousComponent.getTooltip(60.0F), CooldownComponent.getTooltip(180.0F)}).setUnlockCheck(CircleParryRework::canUnlock).build();
    }
}
