package net.kazi.kazimod.abilities.YagiAwakening;

import net.kazi.kazimod.entities.asriel.AsrielChaosFieldEntity;
import net.kazi.kazimod.entities.asriel.AsrielChaosProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;

public final class ChaosBusterAbility extends Ability {
    public static final float COOLDOWN = 480.0F;
    public static final AbilityCore<ChaosBusterAbility> INSTANCE = new AbilityCore.Builder<>(
            "Chaos Buster", AbilityCategory.DEVIL_FRUITS, ChaosBusterAbility::new)
            .setUnlockCheck(YagiAwakeningSupport::canUnlock)
            .setIcon(new ResourceLocation("kazimod", "textures/abilities/chaos_buster.png"))
            .addDescriptionLine(new TranslationTextComponent("ability.kazimod.chaos_buster.description.0"))
            .addAdvancedDescriptionLine(CooldownComponent.getTooltip(COOLDOWN),
                    ChargeComponent.getTooltip(20.0F, ChaosBusterPattern.BEAM_START_TICK))
            .setSourceElement(SourceElement.LIGHT).setSourceType(SourceType.PROJECTILE)
            .setSourceHakiNature(SourceHakiNature.SPECIAL).build();
    private final ChargeComponent charge = new ChargeComponent(this, false)
            .addStartEvent(this::start).addTickEvent(this::duringCharge).addEndEvent(this::release);
    private final AnimationComponent animation = new AnimationComponent(this);
    private AsrielChaosFieldEntity chargeField;
    private AsrielChaosFieldEntity beamField;
    private boolean enhancedCast;
    private boolean chargeSoundPlayed;
    private boolean cancelled;
    private final boolean[] warned = new boolean[ChaosBusterPattern.groupCount()];
    private final int[] volleys = new int[ChaosBusterPattern.groupCount()];
    private int shots;

    public ChaosBusterAbility(AbilityCore<ChaosBusterAbility> core) {
        super(core);
        isNew = true;
        addComponents(charge, animation);
        addUseEvent(this::use);
        addEquipEvent((user, ability) -> updateName(user));
        addTickEvent((user, ability) -> {
            if (!user.level.isClientSide && chargeField != null
                    && (!charge.isCharging() || !canContinue(user))) cancel(user);
            if (!charge.isCharging()) updateName(user);
        });
        addRemoveEvent((user, ability) -> {
            cancel(user);
            if (beamField != null) beamField.remove();
            beamField = null;
        });
    }
    private void updateName(LivingEntity user) {
        String id = YagiAwakeningSupport.enhanced(user) ? "chaos_blaster" : "chaos_buster";
        setDisplayName(new TranslationTextComponent("ability.kazimod." + id));
        setDisplayIcon(new ResourceLocation("kazimod", "textures/abilities/" + id + ".png"));
    }
    private void use(LivingEntity user, IAbility ability) {
        if (user.level.isClientSide || charge.isCharging()) return;
        if (beamField != null) beamField.remove();
        beamField = null;
        enhancedCast = YagiAwakeningSupport.enhanced(user);
        updateName(user);
        cancelled = chargeSoundPlayed = false;
        java.util.Arrays.fill(warned, false);
        java.util.Arrays.fill(volleys, 0);
        shots = 0;
        charge.startCharging(user, enhancedCast ? 20.0F : ChaosBusterPattern.BEAM_START_TICK);
        cooldownComponent.startCooldown(user, COOLDOWN);
    }
    private void start(LivingEntity user, IAbility ability) {
        if (user.level.isClientSide) return;
        int duration = enhancedCast ? 20 : ChaosBusterPattern.BEAM_START_TICK;
        animation.start(user, ModAnimations.POINT_ARMS, duration);
        chargeField = AsrielChaosFieldEntity.spawn(user, AsrielChaosFieldEntity.CHARGE, enhancedCast,
                AsrielChaosFieldEntity.muzzlePosition(user, user.getLookAngle()), user.getLookAngle(), duration + 2);
        if (enhancedCast && chargeField != null && !chargeField.removed) {
            ChaosBusterSounds.charge(user, chargeField.position());
            chargeSoundPlayed = true;
        }
    }
    private void duringCharge(LivingEntity user, IAbility ability) {
        if (user.level.isClientSide) return;
        if (!canContinue(user)) { cancel(user); return; }
        int age = (int) charge.getChargeTime();
        chargeField.updateChargePose(age);
        if (enhancedCast) return;
        for (int i = 0; i < ChaosBusterPattern.groupCount(); i++) {
            ChaosBusterPattern.Group group = ChaosBusterPattern.group(i);
            if (!warned[i] && age >= group.warningTick) {
                warned[i] = true;
                ChaosBusterSounds.target(user, chargeField.position());
            }
            while (volleys[i] < group.volleyCount && age >= group.volleyTick(volleys[i])) {
                Vector3d origin = chargeField.position();
                Vector3d direction = chargeField.getAttackDirection();
                for (double angle : group.shotAnglesRadians()) {
                    user.level.addFreshEntity(new AsrielChaosProjectile(user, origin, direction,
                            AsrielChaosProjectile.BUSTER, shots++, angle));
                }
                ChaosBusterSounds.fire(user, origin);
                volleys[i]++;
            }
        }
        // The last volley shares this beat; start charging after it replaces the firing cue.
        if (!chargeSoundPlayed && age >= ChaosBusterPattern.CHARGE_START_TICK) {
            ChaosBusterSounds.charge(user, chargeField.position());
            chargeSoundPlayed = true;
        }
    }
    private void release(LivingEntity user, IAbility ability) {
        if (user.level.isClientSide) return;
        // An interrupted charge never fires the finishing beam.
        boolean complete = !cancelled && charge.getChargePercentage() >= 0.99F && canContinue(user);
        Vector3d origin = chargeField == null ? user.position() : chargeField.position();
        Vector3d direction = chargeField == null ? user.getLookAngle() : chargeField.getAttackDirection();
        cleanupCharge(user);
        if (!complete) return;
        beamField = AsrielChaosFieldEntity.spawn(user, AsrielChaosFieldEntity.BEAM, enhancedCast,
                origin, direction, ChaosBusterPattern.BEAM_LIFETIME);
        if (beamField != null && !beamField.removed) ChaosBusterSounds.beam(user, origin);
    }
    private boolean canContinue(LivingEntity user) {
        return user.isAlive() && !user.removed && !isDisabled() && chargeField != null
                && !chargeField.removed && chargeField.level == user.level
                && !getComponent(ModAbilityKeys.PAUSE_TICK).map(PauseTickComponent::isPaused).orElse(false);
    }
    private void cancel(LivingEntity user) {
        cancelled = true;
        if (charge.isCharging()) charge.forceStopCharging(user);
        cleanupCharge(user);
    }
    private void cleanupCharge(LivingEntity user) {
        animation.stop(user);
        AsrielChaosFieldEntity previous = chargeField;
        chargeField = null;
        if (previous != null && !previous.level.isClientSide) previous.remove();
    }
    public void onFieldRemoved(AsrielChaosFieldEntity field, LivingEntity user) {
        if (chargeField == field) {
            chargeField = null;
            cancelled = true;
            if (charge.isCharging()) charge.forceStopCharging(user);
            animation.stop(user);
        }
        if (beamField == field) beamField = null;
    }
    public boolean ownsCharge(AsrielChaosFieldEntity field) {
        return chargeField == field && charge.isCharging() && !isDisabled()
                && !getComponent(ModAbilityKeys.PAUSE_TICK).map(PauseTickComponent::isPaused).orElse(false);
    }
}
