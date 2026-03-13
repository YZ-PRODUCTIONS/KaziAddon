package net.kazi.kazimod.abilities.Toshi;

import java.awt.Color;
import java.util.List;

import net.kazi.kazimod.effects.BouncyEffect;
import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
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
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityOverlay.RenderType;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChangeStatsComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.SkinOverlayComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.AttributeHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.packets.server.ability.SToggleDrumsOfLiberationSoundPacket;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;
import net.minecraftforge.common.ForgeMod;

public class FutureOfFreedomAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod",
            "future_of_freedom",
            new Pair[]{ImmutablePair.of("Turn into the most free version of yourself", (Object) null)}
    );

    public static final int HOLD_TIME = 600;
    private static final int MIN_COOLDOWN = 200;
    private static final float MAX_COOLDOWN = 600.0F;
    private static final Color COLOR = WyHelper.hexToRGB("#FFFFFF30");

    public static final AbilityCore<FutureOfFreedomAbility> INSTANCE;
    private static final AbilityOverlay OVERLAY;
    private static final AbilityAttributeModifier GRAVITY_MODIFIER;
    private static final AbilityAttributeModifier JUMP_BOOST_MODIFIER;

    // Debuff duration scaling constants (in ticks)
    private static final int DEBUFF_MIN_TICKS = 50;  // 5 seconds
    private static final int DEBUFF_MAX_TICKS = 300; // 30 seconds

    private static final double BOUNCE_RADIUS = 50.0;
    private static final int EFFECT_DURATION = 10;
    private static final int CHARGE_TIME = 20;

    private int particleTick = 0;

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

    public FutureOfFreedomAbility(AbilityCore core) {
        super(core);
        this.skinOverlayComponent = new SkinOverlayComponent(this, OVERLAY, new AbilityOverlay[0]);
        this.isNew = true;

        this.addComponents(new AbilityComponent[]{
                this.chargeComponent,
                this.continuousComponent,
                this.changeStatsComponent,
                this.skinOverlayComponent
        });

        this.changeStatsComponent.addAttributeModifier((Attribute) ForgeMod.ENTITY_GRAVITY.get(), GRAVITY_MODIFIER);
        this.changeStatsComponent.addAttributeModifier(ModAttributes.JUMP_HEIGHT, JUMP_BOOST_MODIFIER);
        this.addUseEvent(this::useEvent);
    }

    public ContinuousComponent getContinuousComponent() {
        return this.continuousComponent;
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        if (!this.chargeComponent.isCharging()) {
            this.chargeComponent.startCharging(entity, CHARGE_TIME);
        }
    }

    // =========================================================
    // CHARGE EVENTS
    // =========================================================

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
    }

    private void duringChargeEvent(LivingEntity entity, IAbility ability) {
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity, 600.0F);
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
        this.particleTick = 0;

        // Switch Giant Punch → Nika Punch while Future of Freedom is active
        IAbilityData props = AbilityDataCapability.get(entity);
        GiantPunchAbility giantPunch = (GiantPunchAbility) props.getEquippedAbility(GiantPunchAbility.INSTANCE);
        if (giantPunch != null) giantPunch.switchNikaPunch(entity);
    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        if (entity.isOnGround() && !this.playJumpSound) {
            this.playJumpSound = true;
        } else if (!entity.isOnGround() && this.playJumpSound) {
            SoundEvent sfx = (SoundEvent) ModSounds.BOUNCE_1.get();
            if (entity.getRandom().nextBoolean()) sfx = (SoundEvent) ModSounds.BOUNCE_2.get();
            entity.level.playSound(
                    (PlayerEntity) null,
                    entity.blockPosition(),
                    sfx,
                    SoundCategory.PLAYERS,
                    2.0F,
                    0.75F + entity.getRandom().nextFloat() / 2.0F
            );
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

        particleTick++;
        if (particleTick >= 10) {
            particleTick = 0;
            spawnGroundParticles(entity);
        }
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.changeStatsComponent.removeModifiers(entity);
        this.skinOverlayComponent.hideAll(entity);

        if (!entity.level.isClientSide) {
            WyNetwork.sendToAllTrackingAndSelf(new SToggleDrumsOfLiberationSoundPacket(entity, false), entity);
        }

        // Remove bouncy effect from all nearby entities
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

        // Revert Nika Punch → Giant Punch when Future of Freedom ends
        IAbilityData props = AbilityDataCapability.get(entity);
        GiantPunchAbility giantPunch = (GiantPunchAbility) props.getEquippedAbility(GiantPunchAbility.INSTANCE);
        if (giantPunch != null) giantPunch.switchGiantPunch(entity);

        // Scale debuff duration based on how long the ability was active.
        float activeTime = this.continuousComponent.getContinueTime();
        float ratio = Math.min(1.0F, activeTime / 1200.0F);
        int debuffDuration = (int) (DEBUFF_MIN_TICKS + ratio * (DEBUFF_MAX_TICKS - DEBUFF_MIN_TICKS));

        entity.addEffect(new EffectInstance(net.minecraft.potion.Effects.MOVEMENT_SLOWDOWN, debuffDuration, 1, false, true, true));
        entity.addEffect(new EffectInstance(net.minecraft.potion.Effects.WEAKNESS, debuffDuration, 1, false, true, true));
        entity.addEffect(new EffectInstance(KaziEffects.WEAKENED_MOVEMENT.get(), debuffDuration, 1, false, true, true));

        float cooldown = Math.min(600.0F, Math.max(200.0F, activeTime));
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

    private void spawnGroundParticles(LivingEntity user) {
        int radius = (int) BOUNCE_RADIUS;
        int step = 4;
        for (int dx = -radius; dx <= radius; dx += step) {
            for (int dz = -radius; dz <= radius; dz += step) {
                if (dx * dx + dz * dz > radius * radius) continue;
                int baseX = (int) user.getX() + dx;
                int baseZ = (int) user.getZ() + dz;
                int startY = (int) user.getY();
                BlockPos groundPos = null;
                for (int dy = 0; dy >= -10; dy--) {
                    BlockPos check = new BlockPos(baseX, startY + dy, baseZ);
                    BlockState state = user.level.getBlockState(check);
                    if (state.isSolidRender(user.level, check)) {
                        groundPos = check;
                        break;
                    }
                }
                if (groundPos != null) {
                    WyHelper.spawnParticleEffect(
                            (ParticleEffect) ModParticleEffects.GEAR_SECOND.get(),
                            user, baseX + 0.5, groundPos.getY() + 0.05, baseZ + 0.5
                    );
                }
            }
        }
    }

    // =========================================================
    // STATIC INIT
    // =========================================================

    static {
        INSTANCE = (new AbilityCore.Builder("Future of Freedom", AbilityCategory.DEVIL_FRUITS, FutureOfFreedomAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(200.0F, 600.0F),
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        ContinuousComponent.getTooltip(1200.0F),
                        ChangeStatsComponent.getTooltip()
                })
                .build();
        OVERLAY = (new AbilityOverlay.Builder()).setColor(COLOR).setRenderType(RenderType.ENERGY).build();
        GRAVITY_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_GRAVITY_UUID, INSTANCE, "Freedom Jump Modifier", (double) -0.02F, Operation.ADDITION);
        JUMP_BOOST_MODIFIER = new AbilityAttributeModifier(AttributeHelper.MORPH_JUMP_BOOST_UUID, INSTANCE, "Freedom Jump Boost Modifier", (double) 6.0F, Operation.ADDITION);
    }
}