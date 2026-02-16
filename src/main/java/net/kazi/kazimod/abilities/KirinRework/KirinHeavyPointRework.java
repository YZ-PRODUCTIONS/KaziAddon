//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.KirinRework;

import java.util.function.Predicate;

import net.MrMagicalCart.cartaddon.abilities.ryukirin.KirinFlightAbility;
import net.MrMagicalCart.cartaddon.cartapi.CartHelper;
import net.MrMagicalCart.cartaddon.init.CartMorphs;
import net.MrMagicalCart.cartaddon.init.CartParticleEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.ForgeMod;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.abilities.PropelledFlightAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityAttributeModifier;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.MorphAbility2;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChangeStatsComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent.DamageState;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.AttributeHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.DevilFruitHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class KirinHeavyPointRework extends MorphAbility2 {
    private static final ResourceLocation DEFAULT_ICON = new ResourceLocation("cartaddon", "textures/abilities/kirin_heavy_point.png");
    private static final ResourceLocation ALT_ICON = new ResourceLocation("cartaddon", "textures/abilities/alts/kirin_heavy_point_alt.png");
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "kirin_heavy_point", new Pair[]{ImmutablePair.of("Transforms the user into a half-mythical Kirin.", (Object)null)});
    public static final AbilityCore<KirinHeavyPointRework> INSTANCE;
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private static final AbilityAttributeModifier SPEED_MODIFIER;
    private static final AbilityAttributeModifier HEALTH_MODIFIER;
    private static final AbilityAttributeModifier ARMOR_MODIFIER;
    private static final AbilityAttributeModifier STRENGTH_MODIFIER;
    private static final AbilityAttributeModifier ATTACK_SPEED_MODIFIER;
    private static final AbilityAttributeModifier JUMP_BOOST_MODIFIER;
    private static final AbilityAttributeModifier REACH_MODIFIER;
    private static final AbilityAttributeModifier ARMOR_TOUGHNESS_MODIFIER;
    private static final AbilityAttributeModifier TOUGHNESS_MODIFIER;
    private final DamageTakenComponent damageTakenComponent;
    private final AltModeComponent altModeComponent;

    public KirinHeavyPointRework(AbilityCore<KirinHeavyPointRework> core) {
        super(core);
        this.damageTakenComponent = new DamageTakenComponent(this, this::onDamageTaken, DamageState.ATTACK);
        this.altModeComponent = (new AltModeComponent(this, Mode.class, KirinHeavyPointRework.Mode.BASE)).addChangeModeEvent(this::onAltModeChange);
        super.setDisplayIcon(DEFAULT_ICON);
        if (CartHelper.isApril()) {
            super.setDisplayIcon(ALT_ICON);
        }

        Predicate<LivingEntity> isMorphed = (entity) -> this.morphComponent.isMorphed();
        this.statsComponent.addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_MODIFIER, isMorphed);
        this.statsComponent.addAttributeModifier(Attributes.MAX_HEALTH, HEALTH_MODIFIER, isMorphed);
        this.statsComponent.addAttributeModifier(Attributes.ARMOR, ARMOR_MODIFIER, isMorphed);
        this.statsComponent.addAttributeModifier(ModAttributes.PUNCH_DAMAGE, STRENGTH_MODIFIER, isMorphed);
        this.statsComponent.addAttributeModifier(Attributes.ATTACK_SPEED, ATTACK_SPEED_MODIFIER, isMorphed);
        this.statsComponent.addAttributeModifier(ModAttributes.JUMP_HEIGHT, JUMP_BOOST_MODIFIER, isMorphed);
        this.statsComponent.addAttributeModifier(ForgeMod.REACH_DISTANCE, REACH_MODIFIER, isMorphed);
        this.statsComponent.addAttributeModifier(ModAttributes.ATTACK_RANGE, REACH_MODIFIER, isMorphed);
        this.statsComponent.addAttributeModifier(Attributes.ARMOR_TOUGHNESS, ARMOR_TOUGHNESS_MODIFIER, isMorphed);
        this.statsComponent.addAttributeModifier(ModAttributes.TOUGHNESS, TOUGHNESS_MODIFIER, isMorphed);
        super.addComponents(new AbilityComponent[]{this.animationComponent, this.damageTakenComponent, this.altModeComponent});
        this.addEquipEvent(this::equipEvent);
        super.continuousComponent.addStartEvent(this::onContinuityStart).addTickEvent(this::duringContinuityEvent).addTickEvent(100, this::onContinuityTick).addEndEvent(this::onContinuityEnd);
    }

    private void onAltModeChange(LivingEntity livingEntity, IAbility iAbility, Enum anEnum) {
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            IAbilityData abilityDataProps = AbilityDataCapability.get(entity);
            if (abilityDataProps != null) {
                PropelledFlightAbility flightAbility = (PropelledFlightAbility)abilityDataProps.getPassiveAbility(KirinFlightAbility.INSTANCE);
                if (flightAbility != null && !flightAbility.isPaused()) {
                    PropelledFlightAbility.enableFlight((PlayerEntity)entity);
                }
            }
        }

    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        if (!entity.isOnGround()) {
            WyHelper.spawnParticleEffect((ParticleEffect)CartParticleEffects.KIRIN_PUFF.get(), entity, entity.getX(), entity.getY(), entity.getZ());
        }

    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        boolean isFlying = !entity.isOnGround() && DevilFruitHelper.getDifferenceToFloor(entity) > (double)1.0F;
        if (entity instanceof PlayerEntity) {
            boolean var10000 = isFlying | ((PlayerEntity)entity).abilities.flying;
        }

    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            this.animationComponent.stop(entity);
            IAbilityData abilityDataProps = AbilityDataCapability.get(entity);
            if (abilityDataProps != null) {
                PropelledFlightAbility flightAbility = (PropelledFlightAbility)abilityDataProps.getPassiveAbility(KirinFlightAbility.INSTANCE);
                if (flightAbility != null) {
                    PropelledFlightAbility.disableFlight((PlayerEntity)entity);
                }
            }
        }

    }

    public void equipEvent(LivingEntity entity, Ability ability) {
        this.setDisplayIcon(DEFAULT_ICON);
        if (CartHelper.isApril()) {
            this.setDisplayIcon(ALT_ICON);
        }

    }

    public MorphInfo getTransformation() {
        return this.altModeComponent.getCurrentMode() == KirinHeavyPointRework.Mode.ALTERNATIVE ? (MorphInfo)CartMorphs.KIRIN_HEAVY.get() : (MorphInfo)CartMorphs.KIRIN_HEAVY_ALT.get();
    }

    private float onDamageTaken(LivingEntity entity, IAbility ability, DamageSource damageSource, float damage) {
        if (AbilityHelper.isDodging(entity)) {
            return damage;
        } else if (this.continuousComponent.isContinuous()) {
            return damageSource == DamageSource.FALL ? 0.0F : damage * 0.9F;
        } else {
            return damage;
        }
    }

    private void onAltModeChange(LivingEntity entity, IAbility ability, Mode mode) {
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Kirin Heavy Point", AbilityCategory.DEVIL_FRUITS, KirinHeavyPointRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, ChangeStatsComponent.getTooltip()}).build();
        SPEED_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_MOVEMENT_SPEED_UUID, INSTANCE, "Kirin Heavy Point Movement Modifier", 0.065, Operation.ADDITION);
        HEALTH_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_HEALTH_UUID, INSTANCE, "Kirin Heavy Point Health Modifier", (double)20.0F, Operation.ADDITION);
        ARMOR_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_ARMOR_UUID, INSTANCE, "Kirin Heavy Point Armor Modifier", (double)10.0F, Operation.ADDITION);
        STRENGTH_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_STRENGTH_UUID, INSTANCE, "Kirin Heavy Point Strength Modifier", (double)8.0F, Operation.ADDITION);
        ATTACK_SPEED_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_ATTACK_SPEED_UUID, INSTANCE, "Kirin Heavy Point Attack Speed Modifier", (double)0.5F, Operation.ADDITION);
        JUMP_BOOST_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_JUMP_BOOST_UUID, INSTANCE, "Kirin Heavy Point Jump Modifier", (double)3.0F, Operation.ADDITION);
        REACH_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_ATTACK_REACH_UUID, INSTANCE, "Kirin Heavy Point Reach Modifier", (double)2.0F, Operation.ADDITION);
        ARMOR_TOUGHNESS_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_ARMOR_TOUGHNESS_UUID, INSTANCE, "Kirin Heavy Point Armor Toughness Modifier", (double)2.0F, Operation.ADDITION);
        TOUGHNESS_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_TOUGHNESS_UUID, INSTANCE, "Kirin Heavy Point Toughness Modifier", (double)5.0F, Operation.ADDITION);
    }

    public static enum Mode {
        BASE,
        ALTERNATIVE;

        private Mode() {
        }
    }
}
