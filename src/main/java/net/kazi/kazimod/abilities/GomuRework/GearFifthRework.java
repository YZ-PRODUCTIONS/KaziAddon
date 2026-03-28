package net.kazi.kazimod.abilities.GomuRework;

import java.awt.Color;
import java.util.List;

import net.kazi.kazimod.config.KaziConfig;
import net.kazi.kazimod.effects.BouncyEffect;
import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.abilities.gomu.GearFourthAbility;
import xyz.pixelatedw.mineminenomi.abilities.gomu.GearSecondAbility;
import xyz.pixelatedw.mineminenomi.abilities.gomu.GearThirdAbility;
import xyz.pixelatedw.mineminenomi.abilities.gomu.GomuGomuNoPistolAbility;
import xyz.pixelatedw.mineminenomi.abilities.gomu.GomuHelper;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityOverlay.RenderType;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChangeStatsComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.SkinOverlayComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.AttributeHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.HakiHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.haki.HakiDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.haki.IHakiData;
import xyz.pixelatedw.mineminenomi.entities.LightningDischargeEntity;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.packets.server.ability.SToggleDrumsOfLiberationSoundPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;
import net.minecraftforge.common.ForgeMod;

public class GearFifthRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "gear_fifth", new Pair[]{ImmutablePair.of("The absolute peak bringing joy and freedom to those around them.", (Object)null)});
    public static final int HOLD_TIME = 1200;
    private static final int MIN_COOLDOWN = 200;
    private static final float MAX_COOLDOWN = 800.0F;
    private static final Color COLOR = WyHelper.hexToRGB("#FFFFFF30");

    // Debuff duration scaling constants (in ticks)
    private static final int DEBUFF_MIN_TICKS = 100;  // 5 seconds
    private static final int DEBUFF_MAX_TICKS = 600;  // 30 seconds
    private static final int UNCONSCIOUS_MIN_TICKS = 10;   // 1 second
    private static final int UNCONSCIOUS_MAX_TICKS = 80;  // 8 seconds

    public static final AbilityCore<GearFifthRework> INSTANCE;
    private static final AbilityOverlay OVERLAY;
    private static final AbilityAttributeModifier GRAVITY_MODIFIER;
    private static final AbilityAttributeModifier JUMP_BOOST_MODIFIER;

    private static final double BOUNCE_RADIUS = 50.0;
    private static final int EFFECT_DURATION = 10;
    private static final int CHARGE_TIME = 20;

    private final ChargeComponent chargeComponent = (new ChargeComponent(this))
            .addStartEvent(this::startChargeEvent)
            .addTickEvent(this::duringChargeEvent)
            .addEndEvent(this::endChargeEvent);

    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this, true))
            .addStartEvent(this::startContinuityEvent)
            .addTickEvent(this::duringContinuityEvent)
            .addEndEvent(this::endContinuityEvent);
    private final ChangeStatsComponent changeStatsComponent = new ChangeStatsComponent(this);
    private final SkinOverlayComponent skinOverlayComponent;
    private boolean playJumpSound;

    private LightningDischargeEntity discharge;
    private Color hakiColor;
    private int radius;
    private int haoMastery;

    public GearFifthRework(AbilityCore core) {
        super(core);
        this.skinOverlayComponent = new SkinOverlayComponent(this, OVERLAY, new AbilityOverlay[0]);
        this.isNew = true;
        this.hakiColor = new Color(16711680);
        this.radius = 0;
        this.haoMastery = 0;

        this.addComponents(new AbilityComponent[]{
                this.chargeComponent,
                this.continuousComponent,
                this.changeStatsComponent,
                this.skinOverlayComponent
        });
        this.changeStatsComponent.addAttributeModifier((Attribute) ForgeMod.ENTITY_GRAVITY.get(), GRAVITY_MODIFIER);
        this.changeStatsComponent.addAttributeModifier(ModAttributes.JUMP_HEIGHT, JUMP_BOOST_MODIFIER);
        this.addCanUseCheck(GomuHelper.canUseGearCheck(INSTANCE));
        this.addCanUseCheck(this::canUseWithGearSecond);
        this.addUseEvent(this::useEvent);
    }

    public ContinuousComponent getContinuousComponent() {
        return this.continuousComponent;
    }

    private static int getConfiguredHoldTime() {
        return KaziConfig.INSTANCE.gearFifthMaxHoldTime.get();
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        IAbilityData props = AbilityDataCapability.get(entity);
        GomuGomuNoRedRocAbility redRoc = (GomuGomuNoRedRocAbility) props.getEquippedAbility(GomuGomuNoRedRocAbility.INSTANCE);
        if (redRoc != null && redRoc.isBusy()) {
            if (entity instanceof PlayerEntity) {
                entity.sendMessage(new net.minecraft.util.text.StringTextComponent("Gear Fifth cannot be activated while Red Roc is being used!"), entity.getUUID());
            }
            return;
        }
        if (!this.chargeComponent.isCharging()) {
            this.chargeComponent.startCharging(entity, CHARGE_TIME);
        }
    }

    private AbilityUseResult canUseWithGearSecond(LivingEntity entity, IAbility ability) {
        IAbilityData props = AbilityDataCapability.get(entity);
        GearSecondRework gearSecondRework = (GearSecondRework) props.getEquippedAbility(GearSecondRework.INSTANCE);
        if (gearSecondRework != null && gearSecondRework.isContinuous()) {
            return AbilityUseResult.fail((ITextComponent) null);
        }
        return AbilityUseResult.success();
    }

    // =========================================================
    // CHARGE EVENTS
    // =========================================================

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        IHakiData hakiProps = HakiDataCapability.get(entity);
        float haoLevel = hakiProps.getTotalHakiExp() / 100.0F;
        if (haoLevel <= 1.0F) {
            this.radius = 10;
            this.haoMastery = 0;
        } else if (haoLevel > 1.0F && haoLevel <= 1.75F) {
            this.radius = 25;
            this.haoMastery = 1;
        } else {
            this.radius = 40;
            this.haoMastery = 2;
        }

        if (entity instanceof PlayerEntity) {
            this.hakiColor = new Color(HakiHelper.getHaoshokuColour(entity));
        }

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.HAKI_RELEASE_SFX.get(),
                SoundCategory.PLAYERS, 3.0F, 0.5F + entity.getRandom().nextFloat());

        this.discharge = new LightningDischargeEntity(entity,
                entity.getX(), entity.getY() + 1.5F, entity.getZ(),
                entity.yRot, entity.xRot);
        this.discharge.setAliveTicks(-1);
        this.discharge.setUpdateRate(8);
        this.discharge.setLightningLength((float) (this.radius * 2));
        this.discharge.setColor(new Color(0, 0, 0, 100));
        this.discharge.setOutlineColor(this.hakiColor);
        this.discharge.setRenderTransparent();
        this.discharge.setDetails(16);
        int density = this.haoMastery == 2 ? 32 : 16;
        this.discharge.setDensity(density);
        this.discharge.setSize(1.0F);
        this.discharge.setSkipSegments(1);
        if (this.haoMastery == 0) {
            this.discharge.setSplit();
        }

        if (entity instanceof PlayerEntity) {
            entity.level.addFreshEntity(this.discharge);
            if (this.discharge != null) {
                this.discharge.setAliveTicks(CHARGE_TIME + 20);
            }
        }
    }

    private void duringChargeEvent(LivingEntity entity, IAbility ability) {
        if (this.chargeComponent.getChargeTime() % 10.0F == 0.0F) {
            entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                    (SoundEvent) ModSounds.HAKI_RELEASE_SFX.get(),
                    SoundCategory.PLAYERS, 3.0F, 0.5F + entity.getRandom().nextFloat());
        }

        if (this.chargeComponent.getChargeTime() % 5.0F == 0.0F) {
            if (this.discharge != null) {
                this.discharge.setPos(entity.getX(), entity.getY() + 1.0F, entity.getZ());
            }
        }

        if (this.discharge != null && !entity.isAlive()) {
            this.discharge.setAliveTicks(0);
        }
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.HAKI_RELEASE_SFX.get(),
                SoundCategory.PLAYERS, 1.0F, 1.0F);

        if (this.discharge != null) {
            this.discharge.setAliveTicks(30);
        }

        this.continuousComponent.triggerContinuity(entity, (float) getConfiguredHoldTime());
    }

    // =========================================================
    // CONTINUITY EVENTS
    // =========================================================

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.changeStatsComponent.applyModifiers(entity);
        this.skinOverlayComponent.showAll(entity);
        if (!entity.level.isClientSide) {
            WyNetwork.sendToAllTrackingAndSelf(new SToggleDrumsOfLiberationSoundPacket(entity, true), entity);
        }
        this.playJumpSound = false;

        IAbilityData props = AbilityDataCapability.get(entity);

        GearSecondAbility gearSecond = (GearSecondAbility) props.getEquippedAbility(GearSecondAbility.INSTANCE);
        if (gearSecond != null) stopGear(gearSecond, entity);

        GearThirdAbility gearThird = (GearThirdAbility) props.getEquippedAbility(GearThirdAbility.INSTANCE);
        if (gearThird != null) stopGear(gearThird, entity);

        GearFourthAbility gearFourth = (GearFourthAbility) props.getEquippedAbility(GearFourthAbility.INSTANCE);
        if (gearFourth != null) stopGear(gearFourth, entity);

        // Rework pistol — switch to Star Gun
        GomuGomuNoPistolRework pistolRework = (GomuGomuNoPistolRework) props.getEquippedAbility(GomuGomuNoPistolRework.INSTANCE);
        if (pistolRework != null) pistolRework.switchFifthGear(entity);

        // Rework gatling — switch to Dawn Gatling
        GomuGomuNoGatlingRework gatlingRework = (GomuGomuNoGatlingRework) props.getEquippedAbility(GomuGomuNoGatlingRework.INSTANCE);
        if (gatlingRework != null) gatlingRework.switchFifthGear(entity);

        // Red Roc — switch to Bajrang Gun
        GomuGomuNoRedRocAbility redRoc = (GomuGomuNoRedRocAbility) props.getEquippedAbility(GomuGomuNoRedRocAbility.INSTANCE);
        if (redRoc != null) redRoc.switchBajrangGun(entity);

        // Rocket — switch to Dawn Rocket
        GomuGomuNoRocketRework rocket = (GomuGomuNoRocketRework) props.getEquippedAbility(GomuGomuNoRocketRework.INSTANCE);
        if (rocket != null) rocket.switchDawnRocket(entity);
    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        IAbilityData props = AbilityDataCapability.get(entity);

        GearSecondAbility gearSecond = (GearSecondAbility) props.getEquippedAbility(GearSecondAbility.INSTANCE);
        if (gearSecond != null) applyShortDisable(gearSecond, entity);

        GearThirdAbility gearThird = (GearThirdAbility) props.getEquippedAbility(GearThirdAbility.INSTANCE);
        if (gearThird != null) applyShortDisable(gearThird, entity);

        GearFourthAbility gearFourth = (GearFourthAbility) props.getEquippedAbility(GearFourthAbility.INSTANCE);
        if (gearFourth != null) applyShortDisable(gearFourth, entity);

        if (entity.isOnGround() && !this.playJumpSound) {
            this.playJumpSound = true;
        } else if (!entity.isOnGround() && this.playJumpSound) {
            SoundEvent sfx = (SoundEvent) ModSounds.BOUNCE_1.get();
            if (entity.getRandom().nextBoolean()) sfx = (SoundEvent) ModSounds.BOUNCE_2.get();
            entity.level.playSound((PlayerEntity) null, entity.blockPosition(), sfx, SoundCategory.PLAYERS, 2.0F, 0.75F + entity.getRandom().nextFloat() / 2.0F);
            this.playJumpSound = false;
        }

        if (AbilityHelper.canUseMomentumAbilities(entity) && entity.isSprinting()) {
            float maxSpeed = 1.0F;
            net.minecraft.util.math.vector.Vector3d vec = entity.getLookAngle();
            if (entity.isOnGround()) {
                AbilityHelper.setDeltaMovement(entity, vec.x * maxSpeed, entity.getDeltaMovement().y, vec.z * maxSpeed);
            } else {
                AbilityHelper.setDeltaMovement(entity, vec.x * maxSpeed * 0.5F, entity.getDeltaMovement().y, vec.z * maxSpeed * 0.5F);
            }
        }

        applyBouncyEffectToNearby(entity);
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.changeStatsComponent.removeModifiers(entity);
        this.skinOverlayComponent.hideAll(entity);
        if (!entity.level.isClientSide) {
            WyNetwork.sendToAllTrackingAndSelf(new SToggleDrumsOfLiberationSoundPacket(entity, false), entity);
        }

        AxisAlignedBB box = entity.getBoundingBox().inflate(BOUNCE_RADIUS);
        List<LivingEntity> nearby = entity.level.getEntitiesOfClass(
                LivingEntity.class, box,
                e -> e != entity && e.distanceTo(entity) <= BOUNCE_RADIUS
        );
        for (LivingEntity target : nearby) {
            if (target.hasEffect(KaziEffects.BOUNCY.get())) {
                ((BouncyEffect) KaziEffects.BOUNCY.get()).onEffectRemoved(target);
                target.removeEffect(KaziEffects.BOUNCY.get());
            }
        }

        IAbilityData props = AbilityDataCapability.get(entity);

        // Vanilla pistol
        GomuGomuNoPistolAbility pistol = (GomuGomuNoPistolAbility) props.getEquippedAbility(GomuGomuNoPistolAbility.INSTANCE);
        if (pistol != null) pistol.switchNoGear(entity);

        // Rework pistol — revert to No Gear
        GomuGomuNoPistolRework pistolRework = (GomuGomuNoPistolRework) props.getEquippedAbility(GomuGomuNoPistolRework.INSTANCE);
        if (pistolRework != null) pistolRework.switchNoGear(entity);

        // Rework gatling — revert to No Gear
        GomuGomuNoGatlingRework gatlingRework = (GomuGomuNoGatlingRework) props.getEquippedAbility(GomuGomuNoGatlingRework.INSTANCE);
        if (gatlingRework != null) gatlingRework.switchNoGear(entity);

        // Red Roc — revert to Red Roc
        GomuGomuNoRedRocAbility redRoc = (GomuGomuNoRedRocAbility) props.getEquippedAbility(GomuGomuNoRedRocAbility.INSTANCE);
        if (redRoc != null) redRoc.switchRedRoc(entity);

        // Rocket — revert to Normal Rocket
        GomuGomuNoRocketRework rocket = (GomuGomuNoRocketRework) props.getEquippedAbility(GomuGomuNoRocketRework.INSTANCE);
        if (rocket != null) rocket.switchNoGear(entity);

        // Cancel Dawn Whip if active — uses the same reflection helper as vanilla gears
        Ability dawnWhip = (Ability) props.getEquippedAbility(GomuGomuNoDawnWhipRework.INSTANCE);
        if (dawnWhip != null) stopGear(dawnWhip, entity);

        // Cancel Gigant if active
        Ability gigant = (Ability) props.getEquippedAbility(GomuGomuNoGigantRework.INSTANCE);
        if (gigant != null) stopGear(gigant, entity);

        // Cancel Kaminari if charging OR firing — stop charge first, then continuity
        Ability kaminari = (Ability) props.getEquippedAbility(GomuGomuNoKaminariAbility.INSTANCE);
        if (kaminari != null) {
            stopCharge(kaminari, entity);
            stopGear(kaminari, entity);
        }

        // Scale debuff duration based on how long the ability was active.
        // continueTime ranges from 0 to the configured max hold time; debuff clamps between 5s (100t) and 30s (600t).
        float activeTime = this.continuousComponent.getContinueTime();
        float ratio = Math.min(1.0F, activeTime / (float) getConfiguredHoldTime());
        int debuffDuration = (int) (DEBUFF_MIN_TICKS + ratio * (DEBUFF_MAX_TICKS - DEBUFF_MIN_TICKS));
        int unconsciousDuration = (int) (UNCONSCIOUS_MIN_TICKS + ratio * (UNCONSCIOUS_MAX_TICKS - UNCONSCIOUS_MIN_TICKS));

        entity.addEffect(new EffectInstance(net.minecraft.potion.Effects.MOVEMENT_SLOWDOWN, debuffDuration, 1, false, true, true));
        entity.addEffect(new EffectInstance(net.minecraft.potion.Effects.WEAKNESS, debuffDuration, 1, false, true, true));
        entity.addEffect(new EffectInstance(KaziEffects.WEAKENED_MOVEMENT.get(), debuffDuration, 1, false, true, true));
        entity.addEffect(new EffectInstance((net.minecraft.potion.Effect) ModEffects.UNCONSCIOUS.get(), unconsciousDuration, 0, false, true, true));

        float cooldown = Math.max(200.0F, activeTime);
        this.cooldownComponent.startCooldown(entity, cooldown);
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private void applyBouncyEffectToNearby(LivingEntity user) {
        AxisAlignedBB box = user.getBoundingBox().inflate(BOUNCE_RADIUS);
        List<LivingEntity> nearby = user.level.getEntitiesOfClass(
                LivingEntity.class, box,
                e -> e != user && e.distanceTo(user) <= BOUNCE_RADIUS
        );
        for (LivingEntity target : nearby) {
            target.addEffect(new EffectInstance(
                    KaziEffects.BOUNCY.get(), EFFECT_DURATION, 0, false, false, false
            ));
        }
    }

    /**
     * Stops the continuousComponent of any Ability via reflection.
     * Walks the full class hierarchy so it works whether the field is declared
     * on the concrete class (e.g. GomuGomuNoDawnWhipRework) or on a parent class
     * (e.g. MorphAbility2, which is the superclass of GomuGomuNoGigantRework).
     * Silently skips if no matching field is found anywhere in the hierarchy.
     */
    private static void stopGear(Ability gearAbility, LivingEntity entity) {
        java.lang.reflect.Field contField = findFieldInHierarchy(gearAbility.getClass(), "continuousComponent");
        if (contField == null) return;
        try {
            contField.setAccessible(true);
            ContinuousComponent cont = (ContinuousComponent) contField.get(gearAbility);
            if (cont != null && cont.isContinuous()) {
                cont.stopContinuity(entity);
            }
        } catch (Exception e) {
            // skip any reflection error
        }
    }

    /**
     * Stops the chargeComponent of any Ability via reflection.
     * Used for Kaminari which has a charge phase before the beam fires.
     * Silently skips if the field does not exist anywhere in the hierarchy.
     */
    private static void stopCharge(Ability gearAbility, LivingEntity entity) {
        java.lang.reflect.Field chargeField = findFieldInHierarchy(gearAbility.getClass(), "chargeComponent");
        if (chargeField == null) return;
        try {
            chargeField.setAccessible(true);
            ChargeComponent charge = (ChargeComponent) chargeField.get(gearAbility);
            if (charge != null && charge.isCharging()) {
                charge.stopCharging(entity);
            }
        } catch (Exception e) {
            // skip any reflection error
        }
    }

    /**
     * Walks the class hierarchy from {@code clazz} up to (but not including) Object,
     * returning the first declared field with the given name, or null if not found.
     */
    private static java.lang.reflect.Field findFieldInHierarchy(Class<?> clazz, String fieldName) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
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

    private static boolean canUnlock(LivingEntity user) {
        return DevilFruitCapability.get(user).hasAwakenedFruit();
    }

    // =========================================================
    // STATIC INIT
    // =========================================================

    static {
        INSTANCE = (new AbilityCore.Builder("Gear Fifth", AbilityCategory.DEVIL_FRUITS, GearFifthRework::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(200.0F, 800.0F),
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        ContinuousComponent.getTooltip((float) KaziConfig.INSTANCE.gearFifthMaxHoldTime.get()),
                        ChangeStatsComponent.getTooltip()
                })
                .setUnlockCheck(GearFifthRework::canUnlock)
                .build();
        OVERLAY = (new AbilityOverlay.Builder()).setColor(COLOR).setRenderType(RenderType.ENERGY).build();
        GRAVITY_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_GRAVITY_UUID, INSTANCE, "Nika Jump Modifier", (double) -0.02F, Operation.ADDITION);
        JUMP_BOOST_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_JUMP_BOOST_UUID, INSTANCE, "Nika Jump Boost Modifier", (double) 6.0F, Operation.ADDITION);
    }
}
