package net.kazi.kazimod.abilities.GomuRework;

import java.awt.Color;
import java.util.UUID;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.abilities.gomu.GearFourthAbility;
import xyz.pixelatedw.mineminenomi.abilities.gomu.GomuHelper;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityAttributeModifier;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUseResult;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityOverlay;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityOverlay.OverlayPart;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChangeStatsComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.SkinOverlayComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class GearSecondRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("kazimod", "gear_second", new Pair[]{ImmutablePair.of("By speeding up their blood flow, the user gains strength, speed and mobility.", (Object)null)});
    private static final int   HOLD_TIME    = 400;
    private static final float MIN_COOLDOWN = 100.0F;  // 5 seconds (was 20 = 1s)
    private static final float MAX_COOLDOWN = 800.0F;  // 40 seconds (was 666.6667 = ~33s)

    public static final AbilityCore<GearSecondRework> INSTANCE;
    private static final AbilityOverlay OVERLAY;
    private static final AbilityAttributeModifier JUMP_HEIGHT;
    private static final AbilityAttributeModifier STRENGTH_MODIFIER;
    private static final AbilityAttributeModifier ATTACK_SPEED_MODIFIER;
    private static final AbilityAttributeModifier STEP_HEIGHT;

    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this, true))
            .addStartEvent(this::startContinuityEvent)
            .addTickEvent(this::duringContinuityEvent)
            .addEndEvent(this::endContinuityEvent);
    private final ChangeStatsComponent changeStatsComponent = new ChangeStatsComponent(this);
    private final SkinOverlayComponent skinOverlayComponent;
    private boolean prevSprintValue;

    public GearSecondRework(AbilityCore<GearSecondRework> core) {
        super(core);
        this.skinOverlayComponent = new SkinOverlayComponent(this, OVERLAY, new AbilityOverlay[0]);
        this.prevSprintValue = false;
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.changeStatsComponent, this.skinOverlayComponent});
        this.changeStatsComponent.addAttributeModifier((Attribute) ModAttributes.JUMP_HEIGHT.get(), JUMP_HEIGHT);
        this.changeStatsComponent.addAttributeModifier((Attribute) ModAttributes.STEP_HEIGHT.get(), STEP_HEIGHT);
        this.changeStatsComponent.addAttributeModifier(Attributes.ATTACK_SPEED, ATTACK_SPEED_MODIFIER);
        this.changeStatsComponent.addAttributeModifier(ModAttributes.PUNCH_DAMAGE, STRENGTH_MODIFIER);
        this.addCanUseCheck(GomuHelper.canUseGearCheck(INSTANCE));
        this.addCanUseCheck(this::canUseWithGearFifth);
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity, 400.0F);
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.changeStatsComponent.applyModifiers(entity);
        this.skinOverlayComponent.showAll(entity);

        IAbilityData props = AbilityDataCapability.get(entity);

        GomuGomuNoPistolRework pistol = (GomuGomuNoPistolRework) props.getEquippedAbility(GomuGomuNoPistolRework.INSTANCE);
        if (pistol != null) pistol.switchSecondGear(entity);

        GomuGomuNoGatlingRework gatling = (GomuGomuNoGatlingRework) props.getEquippedAbility(GomuGomuNoGatlingRework.INSTANCE);
        if (gatling != null) gatling.switchSecondGear(entity);

        GomuGomuNoBazookaRework bazooka = (GomuGomuNoBazookaRework) props.getEquippedAbility(GomuGomuNoBazookaRework.INSTANCE);
        if (bazooka != null) bazooka.switchSecondGear(entity);

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.GEAR_SECOND_SFX.get(), SoundCategory.PLAYERS, 2.0F, 1.0F);
        this.prevSprintValue = entity.isSprinting();
    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        IAbilityData props = AbilityDataCapability.get(entity);
        GearFourthAbility gearFourth = (GearFourthAbility) props.getEquippedAbility(GearFourthAbility.INSTANCE);
        if (gearFourth != null) {
            applyShortDisable(gearFourth, entity);
        }

        GearFifthRework gearFifth = (GearFifthRework) props.getEquippedAbility(GearFifthRework.INSTANCE);
        if (gearFifth != null) {
            applyShortDisable(gearFifth, entity);
        }

        if (this.continuousComponent.getContinueTime() % 10.0F == 0.0F) {
            WyHelper.spawnParticleEffect((ParticleEffect) ModParticleEffects.GEAR_SECOND.get(),
                    entity, entity.getX(), entity.getY(), entity.getZ());
        }

        if (AbilityHelper.canUseMomentumAbilities(entity)) {
            if (entity.isSprinting()) {
                if (!this.prevSprintValue) {
                    entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                            (SoundEvent) ModSounds.TELEPORT_SFX.get(), SoundCategory.PLAYERS, 2.0F, 1.0F);
                }

                // Speed nerfed by 40%: 1.75 * 0.6 = 1.05
                float maxSpeed = 1.2F;
                Vector3d vec = entity.getLookAngle();
                if (entity.isOnGround()) {
                    AbilityHelper.setDeltaMovement(entity,
                            vec.x * maxSpeed,
                            entity.getDeltaMovement().y,
                            vec.z * maxSpeed);
                } else {
                    AbilityHelper.setDeltaMovement(entity,
                            vec.x * maxSpeed * 0.5F,
                            entity.getDeltaMovement().y,
                            vec.z * maxSpeed * 0.5F);
                }

                this.prevSprintValue = entity.isSprinting();
            } else {
                this.prevSprintValue = false;
            }
        }
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.changeStatsComponent.removeModifiers(entity);
        this.skinOverlayComponent.hideAll(entity);

        IAbilityData props = AbilityDataCapability.get(entity);

        GomuGomuNoPistolRework pistol = (GomuGomuNoPistolRework) props.getEquippedAbility(GomuGomuNoPistolRework.INSTANCE);
        if (pistol != null) pistol.switchNoGear(entity);

        GomuGomuNoGatlingRework gatling = (GomuGomuNoGatlingRework) props.getEquippedAbility(GomuGomuNoGatlingRework.INSTANCE);
        if (gatling != null) gatling.switchNoGear(entity);

        GomuGomuNoBazookaRework bazooka = (GomuGomuNoBazookaRework) props.getEquippedAbility(GomuGomuNoBazookaRework.INSTANCE);
        if (bazooka != null) bazooka.switchNoGear(entity);

        // Cooldown = 2x the time held (400 ticks held → 800 ticks = 40s cooldown, minimum 5s)
        float cooldown = Math.max(MIN_COOLDOWN, this.continuousComponent.getContinueTime() * 2.0F);
        this.cooldownComponent.startCooldown(entity, cooldown);
    }

    private AbilityUseResult canUseWithGearFifth(LivingEntity entity, IAbility ability) {
        IAbilityData props = AbilityDataCapability.get(entity);
        GearFifthRework gearFifth = (GearFifthRework) props.getEquippedAbility(GearFifthRework.INSTANCE);
        if (gearFifth != null && gearFifth.getContinuousComponent() != null && gearFifth.getContinuousComponent().isContinuous()) {
            return AbilityUseResult.fail((ITextComponent) null);
        }
        return AbilityUseResult.success();
    }

    private static void applyShortDisable(Ability gearAbility, LivingEntity entity) {
        try {
            java.lang.reflect.Field disableField = Ability.class.getDeclaredField("disableComponent");
            disableField.setAccessible(true);
            Object disable = disableField.get(gearAbility);
            if (disable != null) {
                disable.getClass().getMethod("startDisable", LivingEntity.class, float.class).invoke(disable, entity, 10.0F);
            }
        } catch (Exception e) {
            // skip
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Gear Second", AbilityCategory.DEVIL_FRUITS, GearSecondRework::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(MIN_COOLDOWN, MAX_COOLDOWN),
                        ContinuousComponent.getTooltip(400.0F),
                        ChangeStatsComponent.getTooltip()
                })
                .build();
        OVERLAY = (new AbilityOverlay.Builder())
                .setOverlayPart(OverlayPart.BODY)
                .setColor(new Color(232, 54, 54, 74))
                .build();
        JUMP_HEIGHT           = new AbilityAttributeModifier(UUID.fromString("a44a9644-369a-4e18-88d9-323727d3d85b"), INSTANCE, "Gear Second Jump Modifier",         5.0D, Operation.ADDITION);
        STRENGTH_MODIFIER     = new AbilityAttributeModifier(UUID.fromString("a2337b58-7e6d-4361-a8ca-943feee4f906"), INSTANCE, "Gear Second Attack Damage Modifier", 4.0D, Operation.ADDITION);
        ATTACK_SPEED_MODIFIER = new AbilityAttributeModifier(UUID.fromString("c495cf01-f3ff-4933-9805-5bb1ed9d27b0"), INSTANCE, "Gear Second Attack Speed Modifier",  4.0D, Operation.ADDITION);
        STEP_HEIGHT           = new AbilityAttributeModifier(UUID.fromString("eab680cd-a6dc-438a-99d8-46f9eb53a950"), INSTANCE, "Gear Second Step Height Modifier",    1.0D, Operation.ADDITION);
    }
}
