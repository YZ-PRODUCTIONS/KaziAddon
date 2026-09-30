package net.kazi.kazimod.abilities.DokuRework;

import java.util.function.Supplier;
import net.kazi.kazimod.abilities.DokuRework.NewChloroBallRework;
import net.kazi.kazimod.abilities.DokuRework.NewDokuGumoRework;
import net.kazi.kazimod.abilities.DokuRework.NewHydraRework;
import net.kazi.kazimod.abilities.DokuRework.NewVenomRoadRework;
import net.MrMagicalCart.cartaddon.api.helpers.HakiUsedChecker;
import net.MrMagicalCart.cartaddon.init.CartMorphs;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.ForgeMod;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityAttributeModifier;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.MorphAbility2;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChangeStatsComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTriggerComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.AttributeHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.api.protection.BlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.protection.DefaultProtectionRules;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.events.FactionEvents;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.init.ModBlocks;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class NewVenomDemonRework
extends MorphAbility2 {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText((String)"kazimod", (String)"venom_demon", (Pair[])new Pair[]{ImmutablePair.of((Object)"The user coats themselves in layers of strong corrosive venom, becoming a Venom Demon and leaving a highly poisonous trail. Also enhances all Poison abilities.", null)});
    private static final int COOLDOWN = 1600;
    private static final int HOLD_TIME = 1200;
    public static final AbilityCore<NewVenomDemonRework> INSTANCE = new AbilityCore.Builder<NewVenomDemonRework>("Venom Demon", AbilityCategory.DEVIL_FRUITS, NewVenomDemonRework::new).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, DealDamageComponent.getTooltip((float)5.0f), ContinuousComponent.getTooltip((float)1200.0f), CooldownComponent.getTooltip((float)400.0f, (float)1600.0f), ChangeStatsComponent.getTooltip()}).setSourceHakiNature(SourceHakiNature.SPECIAL).build();
    private static final AbilityAttributeModifier ATTACK_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_STRENGTH_UUID, INSTANCE, "Venom Demon Attack Modifier", 8.0, AttributeModifier.Operation.ADDITION);
    private static final AbilityAttributeModifier REACH_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_ATTACK_REACH_UUID, INSTANCE, "Venom Demon Reach Modifier", 3.0, AttributeModifier.Operation.ADDITION);
    private static final AbilityAttributeModifier SPEED_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_MOVEMENT_SPEED_UUID, INSTANCE, "Venom Demon Speed Modifier", 0.02, AttributeModifier.Operation.ADDITION);
    private static final AbilityAttributeModifier ATTACK_SPEED = new AbilityAttributeModifier(AttributeHelper.MORPH_ATTACK_SPEED_UUID, INSTANCE, "Venom Demon Attack Speed Modifier", 0.15, AttributeModifier.Operation.ADDITION);
    private static final AbilityAttributeModifier STEP_ASSIST = new AbilityAttributeModifier(AttributeHelper.MORPH_STEP_HEIGHT_UUID, INSTANCE, "Venom Demon Step assist Modifier", 2.5, AttributeModifier.Operation.ADDITION);
    private static final AbilityAttributeModifier JUMP_HEIGHT = new AbilityAttributeModifier(AttributeHelper.MORPH_JUMP_BOOST_UUID, INSTANCE, "Venom Demon Jump Height Modifier", -1.0, AttributeModifier.Operation.ADDITION);
    private static final AbilityAttributeModifier KNOCKBACK_RESISTANCE = new AbilityAttributeModifier(AttributeHelper.MORPH_KNOCKBACK_RESISTANCE_UUID, INSTANCE, "Venom Demon Knockback Resistance Modifier", 1.0, AttributeModifier.Operation.ADDITION);
    private static final AbilityAttributeModifier ARMOR = new AbilityAttributeModifier(AttributeHelper.MORPH_ARMOR_UUID, INSTANCE, "Venom Demon Armor Modifier", 4.0, AttributeModifier.Operation.ADDITION);
    private static final AbilityAttributeModifier TOUGHNESS;
    private static final AbilityAttributeModifier ARMOR_TOUGHNESS;
    private final HitTriggerComponent hitTriggerComponent = new HitTriggerComponent((IAbility)this).addOnHitEvent(100, this::hitTriggerEvent);
    private final DamageTakenComponent damageTakenComponent = new DamageTakenComponent((IAbility)this, this::damageTakenEvent, DamageTakenComponent.DamageState.HURT);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent((IAbility)this);

    public NewVenomDemonRework(AbilityCore<NewVenomDemonRework> core) {
        super(core);
        this.addComponents(new AbilityComponent[]{this.hitTriggerComponent, this.dealDamageComponent, this.damageTakenComponent});
        this.statsComponent.addAttributeModifier((Supplier)ForgeMod.REACH_DISTANCE, (AttributeModifier)REACH_MODIFIER);
        this.statsComponent.addAttributeModifier((Supplier)ModAttributes.ATTACK_RANGE, (AttributeModifier)REACH_MODIFIER);
        this.statsComponent.addAttributeModifier((Supplier)ModAttributes.PUNCH_DAMAGE, (AttributeModifier)ATTACK_MODIFIER);
        this.statsComponent.addAttributeModifier(Attributes.MOVEMENT_SPEED, (AttributeModifier)SPEED_MODIFIER);
        this.statsComponent.addAttributeModifier(Attributes.ATTACK_SPEED, (AttributeModifier)ATTACK_SPEED);
        this.statsComponent.addAttributeModifier((Supplier)ModAttributes.STEP_HEIGHT, (AttributeModifier)STEP_ASSIST);
        this.statsComponent.addAttributeModifier((Supplier)ModAttributes.FALL_RESISTANCE, (AttributeModifier)STEP_ASSIST);
        this.statsComponent.addAttributeModifier((Supplier)ModAttributes.JUMP_HEIGHT, (AttributeModifier)JUMP_HEIGHT);
        this.statsComponent.addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, (AttributeModifier)KNOCKBACK_RESISTANCE);
        this.statsComponent.addAttributeModifier(Attributes.ARMOR, (AttributeModifier)ARMOR);
        this.statsComponent.addAttributeModifier(Attributes.ARMOR_TOUGHNESS, (AttributeModifier)TOUGHNESS);
        this.statsComponent.addAttributeModifier((Supplier)ModAttributes.TOUGHNESS, (AttributeModifier)ARMOR_TOUGHNESS);
        this.continuousComponent.addStartEvent(this::startContinuityEvent);
        this.continuousComponent.addTickEvent(this::duringContinuityEvent);
        this.continuousComponent.addEndEvent(80, this::earlyEndContinuityEvent);
        this.continuousComponent.addEndEvent(this::endContinuityEvent);
    }

    public void startContinuityEvent(LivingEntity entity, IAbility ability) {
        NewVenomRoadRework venomRoad;
        NewDokuGumoRework dokuGumo;
        NewChloroBallRework chloroBall;
        IAbilityData props = AbilityDataCapability.get((LivingEntity)entity);
        NewHydraRework hydra = (NewHydraRework)props.getEquippedAbility(NewHydraRework.INSTANCE);
        if (hydra != null) {
            hydra.setVenomMode(entity);
        }
        if ((chloroBall = (NewChloroBallRework)props.getEquippedAbility(NewChloroBallRework.INSTANCE)) != null) {
            chloroBall.setVenomMode(entity);
        }
        if ((dokuGumo = (NewDokuGumoRework)props.getEquippedAbility(NewDokuGumoRework.INSTANCE)) != null) {
            dokuGumo.setVenomMode(entity);
        }
        if ((venomRoad = (NewVenomRoadRework)props.getEquippedAbility(NewVenomRoadRework.INSTANCE)) != null) {
            venomRoad.setVenomMode(entity);
        }
    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            entity.addEffect(new EffectInstance((Effect)ModEffects.PHYSICAL_MOVING_GUARD.get(), 2, 3, false, false));
            if (!AbilityHelper.isWeakenedByKairosekiOrWater((LivingEntity)entity)) {
                BlockPos.Mutable mutpos = new BlockPos.Mutable();
                for (int x = -1; x < 1; ++x) {
                    for (int z = -1; z < 1; ++z) {
                        mutpos.set(entity.getX() + (double)x, entity.getY(), entity.getZ() + (double)z);
                        if (!entity.level.getBlockState(mutpos.below()).getMaterial().isSolid()) continue;
                        AbilityHelper.placeBlockIfAllowed((LivingEntity)entity, (BlockPos)mutpos, (BlockState)((Block)ModBlocks.DEMON_POISON.get()).defaultBlockState(), (BlockProtectionRule)DefaultProtectionRules.AIR_FOLIAGE);
                    }
                }
            }
            if (this.continuousComponent.getContinueTime() % 2.0f == 0.0f) {
                WyHelper.spawnParticleEffect((ParticleEffect)((ParticleEffect)ModParticleEffects.VENOM_DEMON.get()), (Entity)entity, (double)entity.getX(), (double)entity.getY(), (double)entity.getZ());
            }
        }
    }

    private void earlyEndContinuityEvent(LivingEntity entity, IAbility ability) {
        this.cooldownComponent.startCooldown(entity, 400.0f + this.continuousComponent.getContinueTime());
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        NewVenomRoadRework venomRoad;
        NewDokuGumoRework dokuGumo;
        NewChloroBallRework chloroBall;
        IAbilityData props = AbilityDataCapability.get((LivingEntity)entity);
        NewHydraRework hydra = (NewHydraRework)props.getEquippedAbility(NewHydraRework.INSTANCE);
        if (hydra != null) {
            hydra.setNormalMode(entity);
        }
        if ((chloroBall = (NewChloroBallRework)props.getEquippedAbility(NewChloroBallRework.INSTANCE)) != null) {
            chloroBall.setNormalMode(entity);
        }
        if ((dokuGumo = (NewDokuGumoRework)props.getEquippedAbility(NewDokuGumoRework.INSTANCE)) != null) {
            dokuGumo.setNormalMode(entity);
        }
        if ((venomRoad = (NewVenomRoadRework)props.getEquippedAbility(NewVenomRoadRework.INSTANCE)) != null) {
            venomRoad.setNormalMode(entity);
        }
    }

    private float damageTakenEvent(LivingEntity entity, IAbility ability, DamageSource damageSource, float damage) {
        Entity sourceEntity;
        if (AbilityHelper.isDodging((LivingEntity)entity)) {
            return damage;
        }
        if (this.continuousComponent.isContinuous() && (sourceEntity = damageSource.getEntity()) != null && sourceEntity instanceof LivingEntity && FactionEvents.isDirectHit((DamageSource)damageSource)) {
            if (HakiUsedChecker.hasHasoInfusionOn((LivingEntity)sourceEntity)) {
                return damage;
            }
            if (HakiUsedChecker.hasInternalOn((LivingEntity)sourceEntity)) {
                return damage;
            }
            if (HakiUsedChecker.hasEmissionOn((LivingEntity)sourceEntity)) {
                return damage;
            }
            this.dealDamageComponent.hurtTarget(entity, (LivingEntity)sourceEntity, 5.0f, ModDamageSource.POISON);
            ((LivingEntity)sourceEntity).addEffect(new EffectInstance((Effect)ModEffects.DOKU_POISON.get(), 100, 1));
        }
        return damage;
    }

    public MorphInfo getTransformation() {
        return (MorphInfo)CartMorphs.VENOM_DEMON2.get();
    }

    public float getContinuityHoldTime() {
        return 1200.0f;
    }

    public boolean hitTriggerEvent(LivingEntity entity, LivingEntity target, ModDamageSource source, IAbility ability) {
        if (this.continuousComponent.isContinuous() && entity.getMainHandItem().isEmpty()) {
            source.setFistDamage().setSourceElement(SourceElement.POISON);
            target.addEffect(new EffectInstance((Effect)ModEffects.DOKU_POISON.get(), 100, 1));
        }
        return true;
    }

    static {
        ARMOR_TOUGHNESS = new AbilityAttributeModifier(AttributeHelper.MORPH_ARMOR_TOUGHNESS_UUID, INSTANCE, "Venom Demon Armor Toughness Modifier", 2.0, AttributeModifier.Operation.ADDITION);
        TOUGHNESS = new AbilityAttributeModifier(AttributeHelper.MORPH_TOUGHNESS_UUID, INSTANCE, "Venom Demon Toughness Modifier", 2.0, AttributeModifier.Operation.ADDITION);
    }
}

