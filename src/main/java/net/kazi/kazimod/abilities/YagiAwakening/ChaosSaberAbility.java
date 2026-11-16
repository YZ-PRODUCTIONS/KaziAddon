package net.kazi.kazimod.abilities.YagiAwakening;

import net.kazi.kazimod.entities.asriel.AsrielChaosFieldEntity;
import net.kazi.kazimod.entities.asriel.AsrielChaosProjectile;
import net.kazi.kazimod.init.KaziAnimations;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.util.text.TranslationTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;

public final class ChaosSaberAbility extends Ability {
    public static final float COOLDOWN = 280.0F;
    public static final AbilityCore<ChaosSaberAbility> INSTANCE = new AbilityCore.Builder<>(
            "Chaos Saber", AbilityCategory.DEVIL_FRUITS, ChaosSaberAbility::new)
            .setUnlockCheck(YagiAwakeningSupport::canUnlock)
            .setIcon(new ResourceLocation("kazimod", "textures/abilities/chaos_saber.png"))
            .addDescriptionLine(new TranslationTextComponent("ability.kazimod.chaos_saber.description.0"))
            .addAdvancedDescriptionLine(CooldownComponent.getTooltip(COOLDOWN))
            .setSourceElement(SourceElement.LIGHT).setSourceType(SourceType.PROJECTILE)
            .setSourceHakiNature(SourceHakiNature.SPECIAL).build();
    private final ContinuousComponent cast = new ContinuousComponent(this, true)
            .addTickEvent(this::duringCast).addEndEvent(this::finish);
    private final AnimationComponent animation = new AnimationComponent(this);
    private boolean enhancedCast;
    private int shots;
    private int angelFlashes;
    private final Vector3d[] angelDirections = new Vector3d[AngelOfDeathPattern.flashCount()];
    private final Vector3d[] angelAimPoints = new Vector3d[AngelOfDeathPattern.flashCount()];
    private AsrielChaosFieldEntity saberField;

    public ChaosSaberAbility(AbilityCore<ChaosSaberAbility> core) {
        super(core);
        isNew = true;
        addComponents(cast, animation);
        addUseEvent(this::use);
        addEquipEvent((user, ability) -> updateName(user));
        addTickEvent((user, ability) -> {
            if (!user.level.isClientSide && cast.isContinuous()
                    && (isDisabled() || isCastPaused())) cast.stopContinuity(user);
            if (!cast.isContinuous()) updateName(user);
        });
        addRemoveEvent((user, ability) -> {
            if (cast.isContinuous()) cast.stopContinuity(user);
            if (enhancedCast) AngelOfDeathSounds.stop(user);
            clearSwords();
            animation.stop(user);
        });
    }
    private void updateName(LivingEntity user) {
        String id = YagiAwakeningSupport.enhanced(user) ? "chaos_bullets" : "chaos_saber";
        setDisplayName(new TranslationTextComponent("ability.kazimod." + id));
        setDisplayIcon(new ResourceLocation("kazimod", "textures/abilities/" + id + ".png"));
    }
    private void use(LivingEntity user, IAbility ability) {
        if (user.level.isClientSide || cast.isContinuous()) return;
        enhancedCast = YagiAwakeningSupport.enhanced(user);
        updateName(user);
        shots = 0;
        angelFlashes = 0;
        java.util.Arrays.fill(angelDirections, null);
        java.util.Arrays.fill(angelAimPoints, null);
        clearSwords();
        cast.startContinuity(user, enhancedCast ? AngelOfDeathPattern.DURATION : ChaosSaberPattern.DURATION);
        if (enhancedCast) {
            animation.start(user, ModAnimations.POINT_ARMS, AngelOfDeathPattern.DURATION);
            AngelOfDeathSounds.cast(user);
        }
        else animation.start(user, KaziAnimations.CHAOS_SABER, ChaosSaberPattern.DURATION);
        if (!enhancedCast) {
            Vector3d forward = user.getLookAngle().multiply(1, 0, 1).normalize();
            if (forward.lengthSqr() < 0.001D) {
                double yaw = Math.toRadians(user.yRot);
                forward = new Vector3d(-Math.sin(yaw), 0, Math.cos(yaw));
            }
            saberField = AsrielChaosFieldEntity.spawn(user, AsrielChaosFieldEntity.SABER, false,
                    user.position().add(0, 1, 0).add(forward.scale(1)), forward, ChaosSaberPattern.DURATION);
            if (saberField == null || saberField.removed) cast.stopContinuity(user);
            else ChaosSaberSounds.appear(user, saberField.position());
        }
    }
    private void duringCast(LivingEntity user, IAbility ability) {
        if (user.level.isClientSide || !enhancedCast) return;
        int age = (int) cast.getContinueTime();
        while (angelFlashes < AngelOfDeathPattern.flashCount() && age >= AngelOfDeathPattern.flashTick(angelFlashes)) {
            Vector3d forward = user.getLookAngle().normalize();
            angelDirections[angelFlashes] = forward;
            angelAimPoints[angelFlashes] = YagiAwakeningSupport.aimPoint(user, 48.0D);
            int hand = AngelOfDeathPattern.flashSide(angelFlashes);
            if (hand <= 0) flashAngelHand(user, angelHand(user, forward, -1));
            if (hand >= 0) flashAngelHand(user, angelHand(user, forward, 1));
            AngelOfDeathSounds.fire(user, hand == 0 ? user.getEyePosition(1) : angelHand(user, forward, hand));
            angelFlashes++;
        }
        while (shots < AngelOfDeathPattern.SHOT_COUNT && age >= AngelOfDeathPattern.shotTick(shots)) {
            int volley = AngelOfDeathPattern.volleyIndex(shots);
            Vector3d forward = angelDirections[volley];
            if (forward == null) forward = user.getLookAngle().normalize();
            Vector3d right = angelRight(user, forward), up = forward.cross(right).normalize();
            double[] local = AngelOfDeathPattern.initialDirection(shots);
            Vector3d direction = right.scale(local[0]).add(up.scale(local[1])).add(forward.scale(local[2])).normalize();
            Vector3d origin = angelHand(user, forward, AngelOfDeathPattern.side(shots));
            Vector3d target = angelAimPoints[volley] == null ? origin.add(forward.scale(48)) : angelAimPoints[volley];
            AsrielChaosProjectile shot = new AsrielChaosProjectile(user, origin, direction,
                    AsrielChaosProjectile.BULLET, shots);
            shot.setAngelAim(forward, target);
            user.level.addFreshEntity(shot);
            shots++;
        }
    }

    private static Vector3d angelRight(LivingEntity user, Vector3d forward) {
        Vector3d right = new Vector3d(forward.z, 0, -forward.x);
        if (right.lengthSqr() < .0001D) {
            double yaw = Math.toRadians(user.yRot);
            return new Vector3d(Math.cos(yaw), 0, Math.sin(yaw));
        }
        return right.normalize();
    }

    private static Vector3d angelHand(LivingEntity user, Vector3d forward, int side) {
        Vector3d eye = user.getEyePosition(1);
        Vector3d desired = eye.add(forward.scale(.6D)).add(angelRight(user, forward).scale(side * 1.1D)).add(0, .4D, 0);
        RayTraceResult hit = user.level.clip(new RayTraceContext(eye, desired,
                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, user));
        return hit.getType() == RayTraceResult.Type.MISS ? desired
                : hit.getLocation().subtract(desired.subtract(eye).normalize().scale(.1D));
    }

    private static void flashAngelHand(LivingEntity user, Vector3d hand) {
        if (user.level instanceof ServerWorld)
            ((ServerWorld) user.level).sendParticles(ParticleTypes.FLASH, hand.x, hand.y, hand.z, 1, 0, 0, 0, 0);
    }
    private void finish(LivingEntity user, IAbility ability) {
        animation.stop(user);
        clearSwords();
        // Let the final one-shot cue finish naturally; interrupted volleys stop their own channels.
        if (enhancedCast && shots < AngelOfDeathPattern.SHOT_COUNT) AngelOfDeathSounds.stop(user);
        if (!user.level.isClientSide) cooldownComponent.startCooldown(user, COOLDOWN);
    }
    private boolean isCastPaused() {
        return getComponent(ModAbilityKeys.PAUSE_TICK).map(PauseTickComponent::isPaused).orElse(false);
    }
    private void clearSwords() {
        AsrielChaosFieldEntity previous = saberField;
        saberField = null;
        if (previous != null && !previous.level.isClientSide) previous.remove();
    }
    public boolean ownsSwords(AsrielChaosFieldEntity field) {
        return saberField == field && cast.isContinuous() && !enhancedCast && !isDisabled() && !isCastPaused();
    }
    public void onSwordsRemoved(AsrielChaosFieldEntity field, LivingEntity user) {
        if (saberField != field) return;
        saberField = null;
        if (cast.isContinuous()) cast.stopContinuity(user);
        animation.stop(user);
    }
}
