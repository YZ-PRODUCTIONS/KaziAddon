package net.kazi.kazimod.abilities.SupaRework;

import java.util.Locale;
import java.util.UUID;
import net.kazi.kazimod.entities.EnhancementLightEntity;
import net.kazi.kazimod.entities.GaeBolgVfxEntity;
import net.kazi.kazimod.entities.projectiles.CaladbolgProjectile;
import net.kazi.kazimod.init.KaziEntities;
import net.kazi.kazimod.init.KaziSounds;
import net.kazi.kazimod.items.PairedSwordsItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.server.ServerWorld;
import net.kazi.kazimod.init.KaziPacketHandler;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.StringTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityAttributeModifier;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChangeStatsComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

/**
 * Awakened Supa framework. Each mode has its own entry point so its mechanics
 * can be added independently without coupling future enhancements together.
 */
public class EnhancementAbility extends Ability {
    public static final double FIRST_DAMAGE_BONUS = 2.0D;
    private static final UUID FIRST_DAMAGE_ID = UUID.fromString("e9fe4a61-cb5e-416f-8828-a611d218c256");
    private static final double FIRST_SPEED_BONUS = 0.03D;
    private static final double FIRST_TOUGHNESS_BONUS = 6.0D;
    private static final UUID FIRST_SPEED_ID = UUID.fromString("8f9cd8d7-2c4c-4260-b12c-c12fc793e65c");
    private static final UUID FIRST_TOUGHNESS_ID = UUID.fromString("49166fdf-6ed4-4c78-88ed-03ddc46cd5c3");
    private static final float FIRST_HOLD_TICKS = 20.0F * 20.0F;
    private static final float FIRST_COOLDOWN_TICKS = 20.0F * 20.0F;
    private static final float THIRD_COOLDOWN_TICKS = 70.0F * 20.0F;
    private static final double THIRD_RELEASE_SHAKE_RADIUS = 100.0D;
    private static final int THIRD_RELEASE_SHAKE_TICKS = 40;
    private static final float THIRD_RELEASE_SHAKE_INTENSITY = 6.0F;
    private static final float FOURTH_CHARGE_TICKS = 100.0F;
    private static final float FOURTH_COOLDOWN_TICKS = 70.0F * 20.0F;
    private static final float FOURTH_PROJECTILE_SPEED = 5.0F;
    public static final float THIRD_BASE_DAMAGE = 70.0F;
    public enum Mode {
        FIRST("Overedge"), SECOND("Second"),
        THIRD("Noble Phantasm Reconstruction: Caliburn"), FOURTH("Noble Phantasm Trace: Gae Bolg");

        private final String displayName;

        Mode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return this.displayName;
        }
    }

    private static final ResourceLocation[] MODE_ICONS = new ResourceLocation[]{
            new ResourceLocation("kazimod", "textures/abilities/supa_enhancement_first.png"),
            new ResourceLocation("kazimod", "textures/abilities/projection_2.png"),
            new ResourceLocation("kazimod", "textures/abilities/supa_enhancement_third.png"),
            new ResourceLocation("kazimod", "textures/abilities/supa_gae_bolg.png")
    };
    private static final Pair<String, Object[]>[] DESCRIPTION = new Pair[]{
            ImmutablePair.<String, Object[]>of("Overedge: Enhances Kanshou and Bakuya while maintained.", null),
            ImmutablePair.<String, Object[]>of("Second: Reserved for a future enhancement.", null),
            ImmutablePair.<String, Object[]>of("Caliburn: Releases a large area attack after a heavy charge-up.", null),
            ImmutablePair.<String, Object[]>of("Gae Bolg: Throws a fast crimson spear that detonates violently on impact.", null)
    };

    public static final AbilityCore<EnhancementAbility> INSTANCE = new AbilityCore.Builder<>(
            "Enhancement", AbilityCategory.DEVIL_FRUITS, EnhancementAbility::new)
            .addDescriptionLine(AbilityHelper.registerDescriptionText("kazimod", "enhancement", DESCRIPTION))
            .setIcon(MODE_ICONS[Mode.FIRST.ordinal()])
            .setSourceHakiNature(SourceHakiNature.IMBUING)
            .setSourceType(new SourceType[]{SourceType.SLASH})
            .setUnlockCheck(user -> DevilFruitCapability.get(user).hasAwakenedFruit())
            .build();

    private final AltModeComponent<Mode> altModeComponent = new EnhancementAltModeComponent(this)
            .addChangeModeEvent(this::onModeChanged);
    private final ChargeComponent chargeComponent = new ChargeComponent(this)
            .addStartEvent(this::startThirdCharge)
            .addTickEvent(this::tickThirdCharge)
            .addEndEvent(this::releaseThird);
    private final ContinuousComponent continuousComponent = new ContinuousComponent(this,
            component -> this.altModeComponent.isMode(Mode.FIRST))
            .addStartEvent(this::startContinuous)
            .addTickEvent(this::tickContinuous)
            .addEndEvent(this::endContinuous);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final DamageTakenComponent fourthDamageTaken = new DamageTakenComponent(this)
            .addOnAttackEvent(this::onFourthDamageTaken);
    private boolean preventFourthFallDamage;
    private final ChangeStatsComponent firstStats = new ChangeStatsComponent(this) {
        @Override
        protected void doTick(LivingEntity entity) {
            if (!entity.level.isClientSide) super.doTick(entity);
        }
    }.addAttributeModifier(Attributes.ATTACK_DAMAGE,
            new AbilityAttributeModifier(FIRST_DAMAGE_ID, this, "Enhancement First: paired swords",
                    FIRST_DAMAGE_BONUS, AttributeModifier.Operation.ADDITION),
            this::isFirstSwordEnhancementActive)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED,
                    new AbilityAttributeModifier(FIRST_SPEED_ID, this, "Enhancement First: movement speed",
                            FIRST_SPEED_BONUS, AttributeModifier.Operation.ADDITION),
                    this::isFirstSwordEnhancementActive)
            .addAttributeModifier(ModAttributes.TOUGHNESS,
                    new AbilityAttributeModifier(FIRST_TOUGHNESS_ID, this, "Enhancement First: toughness",
                            FIRST_TOUGHNESS_BONUS, AttributeModifier.Operation.ADDITION),
                    this::isFirstSwordEnhancementActive);
    private EnhancementLightEntity thirdLight;
    private GaeBolgVfxEntity fourthVfx;

    public EnhancementAbility(AbilityCore<EnhancementAbility> core) {
        super(core);
        this.isNew = true;
        this.setDisplayIcon(MODE_ICONS[Mode.FIRST.ordinal()]);
        this.addComponents(new AbilityComponent[]{this.altModeComponent, this.chargeComponent,
                this.continuousComponent, this.animationComponent, this.dealDamageComponent, this.firstStats,
                this.fourthDamageTaken});
        this.addUseEvent(this::onUse);
        this.addTickEvent(this::onAbilityTick);
        this.addRemoveEvent((entity, ability) -> {
            cancelThirdCharge(entity);
            this.preventFourthFallDamage = false;
        });
        this.addEquipEvent((entity, ability) -> {
            this.setDisplayIcon(MODE_ICONS[this.altModeComponent.getCurrentMode().ordinal()]);
            this.firstStats.tick(entity);
        });
    }

    private void onModeChanged(LivingEntity entity, IAbility ability, Mode mode) {
        this.setDisplayIcon(MODE_ICONS[mode.ordinal()]);
        // Selecting First does not activate it. Clear its bonuses when switching modes.
        if (!entity.level.isClientSide) {
            if (mode != Mode.FIRST && this.altModeComponent.isMode(Mode.FIRST)) {
                this.continuousComponent.stopContinuity(entity);
            }
            this.firstStats.removeModifiers(entity);
        }
    }

    @Override
    public void load(CompoundNBT nbt) {
        super.load(nbt);
        this.setDisplayIcon(MODE_ICONS[this.altModeComponent.getCurrentMode().ordinal()]);
    }

    private void onUse(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        if (this.continuousComponent.isContinuous()) {
            if (this.altModeComponent.isMode(Mode.FIRST)) this.continuousComponent.stopContinuity(entity);
            return;
        }
        if (this.chargeComponent.isCharging()
                || (this.thirdLight != null && this.thirdLight.isAlive())) return;
        if (!this.altModeComponent.isMode(Mode.FOURTH)) this.preventFourthFallDamage = false;
        switch (this.altModeComponent.getCurrentMode()) {
            case FIRST:
                useFirstMode(entity, ability);
                break;
            case SECOND:
                // Reserved until this enhancement is available.
                break;
            case THIRD:
                useThirdMode(entity, ability);
                break;
            case FOURTH:
                useFourthMode(entity, ability);
                break;
            default:
                break;
        }
    }

    private void useFirstMode(LivingEntity entity, IAbility ability) {
        this.continuousComponent.startContinuity(entity, FIRST_HOLD_TICKS);
    }

    private boolean isFirstSwordEnhancementActive(LivingEntity entity) {
        return isFirstEnhancementActive() && entity.isAlive()
                && PairedSwordsItem.isPair(entity.getMainHandItem());
    }

    private boolean isFirstEnhancementActive() {
        return this.altModeComponent.isMode(Mode.FIRST) && this.continuousComponent.isContinuous()
                && !this.isDisabled() && !this.isOnCooldown() && !this.isCharging()
                && !this.getComponent(ModAbilityKeys.PAUSE_TICK).map(component -> component.isPaused()).orElse(false);
    }

    /** Read-only holder check shared by both client render paths, like Blue Sword's model predicate. */
    public static boolean hasFirstSwordEnhancement(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) return false;
        IAbilityData data = AbilityDataCapability.get(entity);
        if (data == null) return false;
        EnhancementAbility ability = data.getEquippedAbility(INSTANCE);
        return ability != null && ability.isFirstEnhancementActive();
    }

    private void startContinuous(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide && this.altModeComponent.isMode(Mode.FIRST)) {
            this.firstStats.tick(entity);
        }
    }

    private void tickContinuous(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        if (this.altModeComponent.isMode(Mode.FIRST)) {
            if (!entity.isAlive() || !isFirstEnhancementActive()) {
                this.continuousComponent.stopContinuity(entity);
            }
        } else if (this.altModeComponent.isMode(Mode.THIRD)) {
            tickThirdBlast(entity, ability);
        }
    }

    private void endContinuous(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        if (this.altModeComponent.isMode(Mode.FIRST)) {
            this.firstStats.removeModifiers(entity);
            // The end callback runs before ContinuousComponent resets the elapsed hold time.
            float heldTicks = Math.max(0.0F,
                    Math.min(FIRST_HOLD_TICKS, this.continuousComponent.getContinueTime()));
            this.cooldownComponent.startCooldown(entity,
                    FIRST_COOLDOWN_TICKS * (heldTicks / FIRST_HOLD_TICKS));
        } else if (this.altModeComponent.isMode(Mode.THIRD)) {
            this.cooldownComponent.startCooldown(entity, THIRD_COOLDOWN_TICKS);
        } else if (this.altModeComponent.isMode(Mode.FOURTH)) {
            this.cooldownComponent.startCooldown(entity, FOURTH_COOLDOWN_TICKS);
        }
    }

    private void useSecondMode(LivingEntity entity, IAbility ability) {
    }

    private void useThirdMode(LivingEntity entity, IAbility ability) {
        if (!isMugenActive(entity)) {
            entity.sendMessage(new StringTextComponent("Enhancement Third requires Mugen no Kensei."), entity.getUUID());
            return;
        }
        this.chargeComponent.startCharging(entity, EnhancementLightEntity.CHARGE_TICKS);
    }

    private static boolean isMugenActive(LivingEntity entity) {
        IAbilityData data = AbilityDataCapability.get(entity);
        if (data == null) return false;
        RealityMarbleAbility marble = data.getEquippedAbility(RealityMarbleAbility.INSTANCE);
        return marble != null && marble.isModeActive(RealityMarbleAbility.Mode.MUGEN_NO_KENSEI);
    }

    private void useFourthMode(LivingEntity entity, IAbility ability) {
        if (!isInfiniteActive(entity)) {
            entity.sendMessage(new StringTextComponent("Noble Phantasm Trace: Gae Bolg requires Infinite Creation of Swords."), entity.getUUID());
            return;
        }
        this.chargeComponent.startCharging(entity, FOURTH_CHARGE_TICKS);
    }

    private static boolean isInfiniteActive(LivingEntity entity) {
        IAbilityData data = AbilityDataCapability.get(entity);
        if (data == null) return false;
        RealityMarbleAbility marble = data.getEquippedAbility(RealityMarbleAbility.INSTANCE);
        return marble != null && marble.isModeActive(RealityMarbleAbility.Mode.INFINITE_CREATION_OF_SWORDS);
    }

    private void startFourthCharge(LivingEntity entity) {
        entity.level.playSound(null, entity, KaziSounds.ENHANCEMENT_FOURTH_CHARGE_SFX.get(),
                SoundCategory.PLAYERS, 1.0F, 1.0F);
        clearFourthVfx();
        this.fourthVfx = new GaeBolgVfxEntity(KaziEntities.GAE_BOLG_VFX.get(), entity.level);
        this.fourthVfx.beginCharge(entity);
        entity.level.addFreshEntity(this.fourthVfx);
        this.preventFourthFallDamage = true;
        AbilityHelper.setDeltaMovement(entity, entity.getDeltaMovement().x, 2.0D,
                entity.getDeltaMovement().z);
        entity.addEffect(new EffectInstance(ModEffects.DIZZY.get(), 30, 0, false, false));
    }

    private void releaseFourth(LivingEntity entity) {
        clearFourthVfx();
        if (!entity.isAlive()) return;
        // Preserve the shared combat mechanics; select FOURTH's independent crimson presentation.
        CaladbolgProjectile projectile = new CaladbolgProjectile(entity.level, entity, INSTANCE, false);
        projectile.setDamage(85.0F);
        projectile.setCrimsonVisuals();
        entity.level.addFreshEntity(projectile);
        projectile.shootFromRotation(entity, entity.xRot, entity.yRot,
                0.0F, FOURTH_PROJECTILE_SPEED, 0.0F);
        this.cooldownComponent.startCooldown(entity, FOURTH_COOLDOWN_TICKS);
    }

    private void clearFourthVfx() {
        if (this.fourthVfx != null) this.fourthVfx.remove();
        this.fourthVfx = null;
    }

    public boolean isFourthChargingVfx(GaeBolgVfxEntity vfx) {
        return vfx == this.fourthVfx && this.altModeComponent.isMode(Mode.FOURTH)
                && this.chargeComponent.isCharging();
    }

    private float onFourthDamageTaken(LivingEntity entity, IAbility ability, DamageSource source, float damage) {
        if (this.altModeComponent.isMode(Mode.FOURTH) && this.preventFourthFallDamage
                && source == DamageSource.FALL) {
            this.preventFourthFallDamage = false;
            return 0.0F;
        }
        return damage;
    }

    private void startThirdCharge(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        if (this.altModeComponent.isMode(Mode.FOURTH)) {
            startFourthCharge(entity);
            return;
        }
        applyThirdChargeStuck(entity);
        this.thirdLight = new EnhancementLightEntity(KaziEntities.ENHANCEMENT_LIGHT.get(), entity.level);
        this.thirdLight.beginCharge(entity, this);
        entity.level.addFreshEntity(this.thirdLight);
        // Play once from the caster on the server, including the caster as a listener.
        entity.level.playSound(null, entity, KaziSounds.ENHANCEMENT_THIRD_CHARGE_SFX.get(),
                SoundCategory.PLAYERS, 1.0F, 1.0F);
        this.animationComponent.start(entity, ModAnimations.RAISE_ARMS,
                EnhancementLightEntity.CHARGE_TICKS + 5,
                owner -> !owner.isAlive() || !isChargingLight(this.thirdLight));
    }

    private void tickThirdCharge(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        if (this.altModeComponent.isMode(Mode.FOURTH)) {
            if (!entity.isAlive()) this.chargeComponent.forceStopCharging(entity);
            else AbilityHelper.slowEntityFall(entity);
            return;
        }
        if (!entity.isAlive() || this.thirdLight == null || !this.thirdLight.isAlive()) {
            cancelThirdCharge(entity);
            return;
        }
        applyThirdChargeStuck(entity);
        this.thirdLight.setChargeProgress(this.chargeComponent.getChargePercentage());
    }

    private void applyThirdChargeStuck(LivingEntity entity) {
        // A short refresh expires on release or any interruption without clearing
        // movement restrictions that another ability may have applied.
        entity.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(), 2, 0, false, false));
    }

    private void releaseThird(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        if (this.altModeComponent.isMode(Mode.FOURTH)) {
            releaseFourth(entity);
            return;
        }
        this.animationComponent.stop(entity);
        if (!entity.isAlive() || this.thirdLight == null || !this.thirdLight.isAlive()) return;
        this.thirdLight.release(entity);
        // One strong, two-second release shake for everyone nearby, including the caster.
        if (entity.level instanceof ServerWorld) {
            for (ServerPlayerEntity player : ((ServerWorld) entity.level).players()) {
                if (entity.distanceToSqr(player) <= THIRD_RELEASE_SHAKE_RADIUS * THIRD_RELEASE_SHAKE_RADIUS) {
                    KaziPacketHandler.sendCameraShake(player, THIRD_RELEASE_SHAKE_TICKS,
                            THIRD_RELEASE_SHAKE_INTENSITY);
                }
            }
        }
        this.continuousComponent.startContinuity(entity, EnhancementLightEntity.BLAST_TICKS);
        // Caliburn's cooldown starts when its blast continuity ends.
    }

    public boolean hitThirdBlast(LivingEntity caster, LivingEntity target) {
        if (caster.level.isClientSide) return false;
        AbilityDamageSource source = (AbilityDamageSource) this.dealDamageComponent.getDamageSource(caster);
        source.setSlash();
        return this.dealDamageComponent.hurtTarget(caster, target, THIRD_BASE_DAMAGE, source);
    }

    private void tickThirdBlast(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        if (this.thirdLight == null || !this.thirdLight.isAlive()) {
            this.continuousComponent.stopContinuity(entity);
        }
    }

    private void onAbilityTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        if (!this.chargeComponent.isCharging()) clearFourthVfx();
        if (!this.chargeComponent.isCharging() && this.thirdLight != null && !this.thirdLight.isReleased()) {
            cancelThirdCharge(entity);
        }
        if (this.thirdLight != null && !this.thirdLight.isAlive()) this.thirdLight = null;
    }

    private void cancelThirdCharge(LivingEntity entity) {
        if (entity.level.isClientSide) return;
        clearFourthVfx();
        if (this.chargeComponent.isCharging()) this.chargeComponent.forceStopCharging(entity);
        this.animationComponent.stop(entity);
        if (this.thirdLight != null && !this.thirdLight.isReleased()) {
            this.thirdLight.remove();
            this.thirdLight = null;
        }
    }

    /** Allows the overhead effect to clean itself up if its caster stops charging. */
    public boolean isChargingLight(EnhancementLightEntity light) {
        return light != null && light == this.thirdLight && this.chargeComponent.isCharging();
    }

    private static final class EnhancementAltModeComponent extends AltModeComponent<Mode> {
        private final EnhancementAbility enhancement;

        private EnhancementAltModeComponent(EnhancementAbility ability) {
            super(ability, Mode.class, Mode.FIRST);
            this.enhancement = ability;
        }

        @Override
        public void setNextInCycle(LivingEntity entity) {
            if (this.enhancement.chargeComponent.isCharging()
                    || this.enhancement.continuousComponent.isContinuous()) return;
            this.setMode(entity, findNextAvailableMode(entity, getCurrentMode()));
        }

        private static Mode findNextAvailableMode(LivingEntity entity, Mode from) {
            Mode[] modes = Mode.values();
            for (int offset = 1; offset < modes.length; offset++) {
                Mode candidate = modes[(from.ordinal() + offset) % modes.length];
                if (isModeAvailable(entity, candidate)) return candidate;
            }
            return isModeAvailable(entity, from) ? from : Mode.FIRST;
        }

        private static boolean isModeAvailable(LivingEntity entity, Mode mode) {
            switch (mode) {
                case FIRST:
                    return true;
                case THIRD:
                    return isMugenActive(entity);
                case FOURTH:
                    return isInfiniteActive(entity);
                case SECOND:
                default:
                    return false;
            }
        }

        @Override
        public CompoundNBT save() {
            CompoundNBT nbt = super.save();
            nbt.putString("currentMode", getCurrentMode().name());
            return nbt;
        }

        @Override
        public void load(CompoundNBT nbt) {
            // Accept the title-case names written by the initial framework.
            String mode = nbt.getString("currentMode").toUpperCase(Locale.ROOT);
            try {
                Mode restored = Mode.valueOf(mode);
                nbt.putString("currentMode", (restored == Mode.SECOND ? Mode.FIRST : restored).name());
            } catch (IllegalArgumentException ignored) {
                nbt.putString("currentMode", Mode.FIRST.name());
            }
            super.load(nbt);
            this.enhancement.setDisplayIcon(MODE_ICONS[getCurrentMode().ordinal()]);
        }
    }
}
