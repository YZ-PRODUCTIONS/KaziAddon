//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.MinkRework;

import java.awt.Color;
import java.util.UUID;

import net.MrMagicalCart.cartaddon.abilities.electroextra.CartEleclawAbility;
import net.MrMagicalCart.cartaddon.abilities.electroextra.CartElectroHelper;
import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.entities.mobs.CartOfficerEntity;
import net.MrMagicalCart.cartaddon.init.CartEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityAttributeModifier;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityOverlay;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityOverlay.OverlayPart;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChangeStatsComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.SkinOverlayComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.StackComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent.DamageState;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class SulongRework extends Ability {
    private static final ITextComponent[] DESCRIPTION;
    private static final int MIN_COOLDOWN = 100;
    private static final int MAX_COOLDOWN = 1200;
    private static final Color COLOR;
    public static final AbilityCore<SulongRework> INSTANCE;
    private static final AbilityOverlay OVERLAY;
    private static final AbilityAttributeModifier SPEED_MODIFIER;
    private static final AbilityAttributeModifier STRENGTH_MODIFIER;
    private static final AbilityAttributeModifier JUMP_MODIFIER;
    private static final AbilityAttributeModifier STEP_HEIGHT;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this, true)).addStartEvent(this::startContinuityEvent).addTickEvent(this::tickContinuityEvent).addEndEvent(this::endContinuityEvent);
    private final ChangeStatsComponent statsComponent = new ChangeStatsComponent(this);
    private final DamageTakenComponent damageTakenComponent;
    private final SkinOverlayComponent skinOverlayComponent;
    private final StackComponent stackComponent;
    private boolean rumbleBall;

    public SulongRework(AbilityCore<SulongRework> core) {
        super(core);
        this.damageTakenComponent = new DamageTakenComponent(this, this::onDamageTaken, DamageState.ATTACK);
        this.stackComponent = new StackComponent(this, 0);
        this.rumbleBall = false;
        this.skinOverlayComponent = new SkinOverlayComponent(this, OVERLAY, new AbilityOverlay[0]);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.stackComponent, this.damageTakenComponent, this.continuousComponent, this.statsComponent, this.skinOverlayComponent});
        this.statsComponent.addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_MODIFIER);
        this.statsComponent.addAttributeModifier(Attributes.ATTACK_DAMAGE, STRENGTH_MODIFIER);
        this.statsComponent.addAttributeModifier((Attribute)ModAttributes.JUMP_HEIGHT.get(), JUMP_MODIFIER);
        this.statsComponent.addAttributeModifier((Attribute)ModAttributes.STEP_HEIGHT.get(), STEP_HEIGHT);
        this.addCanUseCheck(CartElectroHelper::canTransformInSulong);
        this.addUseEvent(this::useEvent);

    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        if (entity.hasEffect((Effect)CartEffects.RUMBLE.get())) {
            this.continuousComponent.triggerContinuity(entity, 3600.0F);
            this.rumbleBall = true;
        } else {
            this.continuousComponent.triggerContinuity(entity);
        }

    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.statsComponent.applyModifiers(entity);
        this.skinOverlayComponent.showAll(entity);
    }

    private void tickContinuityEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide && CartElectroHelper.canTransformInSulong(entity, ability).isFail()) {
            this.continuousComponent.stopContinuity(entity);
        }

        if (!entity.hasEffect(Effects.NIGHT_VISION) || entity.getEffect(Effects.NIGHT_VISION).getDuration() <= 250) {
            entity.addEffect(new EffectInstance(Effects.NIGHT_VISION, 500, 0, true, false));
        }

        if (this.rumbleBall && this.continuousComponent.getContinueTime() >= 1200.0F && this.continuousComponent.getContinueTime() % 600.0F == 0.0F) {
            this.stackComponent.addStacks(entity, this, 1);
        }

        if (this.stackComponent.getStacks() >= 5) {
            entity.addEffect(new EffectInstance((Effect)ModEffects.UNCONSCIOUS.get(), 200));
        }

        if (this.stackComponent.getStacks() >= 4) {
            entity.addEffect(new EffectInstance(Effects.BLINDNESS, 300, 0));
            entity.addEffect(new EffectInstance(Effects.WEAKNESS, 300, 0));
        }

        if (this.stackComponent.getStacks() >= 3) {
            entity.addEffect(new EffectInstance(Effects.HUNGER, 300, 2));
        }

        if (this.stackComponent.getStacks() >= 2) {
            entity.addEffect(new EffectInstance(Effects.DIG_SLOWDOWN, 300, 0));
        }

        if (this.stackComponent.getStacks() >= 1) {
            entity.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 300, 1));
        }

        if (entity instanceof CartOfficerEntity) {
            entity.addEffect(new EffectInstance(Effects.MOVEMENT_SPEED, 5, 1, false, false));
        }

    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.stackComponent.revertStacksToDefault(entity, ability);
        this.statsComponent.removeModifiers(entity);
        this.skinOverlayComponent.hideAll(entity);
        float holdTime = this.continuousComponent.getContinueTime() / 2.0F;
        if (holdTime >= 2400.0F) {
            holdTime = 2400.0F;
        }

        this.cooldownComponent.startCooldown(entity, 100.0F + holdTime);
        this.rumbleBall = false;
    }

    private float onDamageTaken(LivingEntity entity, IAbility ability, DamageSource damageSource, float damage) {
        return this.continuousComponent.isContinuous() && damageSource == DamageSource.FALL ? 0.0F : damage;
    }

    private static boolean canUnlock(LivingEntity user) {
        IEntityStats props = EntityStatsCapability.get(user);
        return props.isMink() && props.getDoriki() >= (double)1200.0F;
    }

    static {
        DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "sulong", new Pair[]{ImmutablePair.of("The user reveals their true power during the night, enhancing their physical and electrical power. While active %s stacks are not consumed. Can be used during all non new moon nights or while having a rumble ball eaten. Be careful holding this ability with rumble balls, since past one minute it starts to wear down on you.", new Object[]{AbilityHelper.mentionAbility(CartEleclawAbility.INSTANCE)})});
        COLOR = WyHelper.hexToRGB("#B0E9F255");
        INSTANCE = (new AbilityCore.Builder("Sulong", AbilityCategory.RACIAL, SulongRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(100.0F, 2500.0F), ContinuousComponent.getTooltip(3600.0F, 12000.0F), ChangeStatsComponent.getTooltip()}).setUnlockCheck(SulongRework::canUnlock).build();
        OVERLAY = (new AbilityOverlay.Builder()).setOverlayPart(OverlayPart.BODY).setColor(COLOR).build();
        SPEED_MODIFIER = new AbilityAttributeModifier(UUID.fromString("0b6d004a-d30e-4eec-9900-ac56f8d34c2c"), INSTANCE, "Sulong Speed Modifier", 0.075, Operation.ADDITION);
        STRENGTH_MODIFIER = new AbilityAttributeModifier(UUID.fromString("a0c74dd2-9e6e-450b-9e71-125aa4dca62d"), INSTANCE, "Sulong Attack Damage Modifier", (double)8.0F, Operation.ADDITION);
        JUMP_MODIFIER = new AbilityAttributeModifier(UUID.fromString("0e9a63d0-d466-457b-a473-69652b8dcbf5"), INSTANCE, "Sulong Jump Modifier", (double)8.0F, Operation.ADDITION);
        STEP_HEIGHT = new AbilityAttributeModifier(UUID.fromString("6ec90ae9-1125-4fad-ac11-15b6011d7cd0"), INSTANCE, "Sulong Step Height Modifier", (double)1.0F, Operation.ADDITION);
    }
}
