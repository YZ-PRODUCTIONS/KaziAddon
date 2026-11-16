package net.kazi.kazimod.abilities.YagiAwakening;

import java.util.Optional;
import net.kazi.kazimod.entities.asriel.AsrielStarEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

/** A bounded rain of owned star projectiles; the health variant is locked on cast. */
public final class StarBlazingAbility extends Ability {
    public static final float COOLDOWN_TICKS = 400.0F;
    private static final double LAUNCH_VELOCITY = 2.0D;
    private static final double AIM_RANGE = 48.0D;
    private static final ResourceLocation BASE_ICON = new ResourceLocation("kazimod", "textures/abilities/star_blazing.png");
    private static final ResourceLocation ENHANCED_ICON = new ResourceLocation("kazimod", "textures/abilities/galacta_blazing.png");
    public static final AbilityCore<StarBlazingAbility> INSTANCE = new AbilityCore.Builder<>(
            "Star Blazing", AbilityCategory.DEVIL_FRUITS, StarBlazingAbility::new)
            .setUnlockCheck(YagiAwakeningSupport::canUnlock)
            .setIcon(BASE_ICON)
            .addDescriptionLine(new TranslationTextComponent("ability.kazimod.star_blazing.description.0"))
            .addAdvancedDescriptionLine(AbilityDescriptionLine.NEW_LINE,
                    CooldownComponent.getTooltip(COOLDOWN_TICKS))
            .setSourceType(SourceType.PROJECTILE)
            .setSourceHakiNature(SourceHakiNature.SPECIAL)
            .build();

    private final ContinuousComponent rain = new ContinuousComponent(this, false)
            .addStartEvent((user, ability) -> {
                if (user.level.isClientSide) {
                    castEnhanced = YagiAwakeningSupport.enhanced(user);
                    initialized = true;
                }
                updateName(user);
            }).addTickEvent(this::tickRain).addEndEvent(this::endRain);
    private final DamageTakenComponent damageTaken = new DamageTakenComponent(this)
            .addOnDamageEvent(this::reduceIncomingDamage);
    private Vector3d center = Vector3d.ZERO;
    private Vector3d forward = new Vector3d(0.0D, 0.0D, 1.0D);
    private Vector3d right = new Vector3d(1.0D, 0.0D, 0.0D);
    private boolean castEnhanced;
    private boolean initialized;
    private boolean finaleLaunched;
    private boolean ownsAirborneEffect;
    private int spawned;
    private int elapsed;
    private int summonBeat;

    public StarBlazingAbility(AbilityCore<StarBlazingAbility> core) {
        super(core);
        this.isNew = true;
        addComponents(rain, damageTaken);
        addUseEvent(this::startRain);
        addEquipEvent((user, ability) -> updateName(user));
        addTickEvent((user, ability) -> {
            if (!user.level.isClientSide && rain.isContinuous()
                    && (isDisabled() || isCastPaused())) rain.stopContinuity(user);
            updateName(user);
        });
        addRemoveEvent((user, ability) -> {
            if (rain.isContinuous()) rain.stopContinuity(user);
            clearAirborneEffect(user);
            initialized = false;
        });
    }

    private void updateName(LivingEntity user) {
        boolean enhanced = rain.isContinuous() && initialized ? castEnhanced : YagiAwakeningSupport.enhanced(user);
        setDisplayName(new TranslationTextComponent(enhanced
                ? "ability.kazimod.galacta_blazing" : "ability.kazimod.star_blazing"));
        setDisplayIcon(enhanced ? ENHANCED_ICON : BASE_ICON);
    }

    private void startRain(LivingEntity user, IAbility ability) {
        if (!(user.level instanceof ServerWorld) || rain.isContinuous()) return;
        refreshAim(user);
        castEnhanced = YagiAwakeningSupport.enhanced(user);
        initialized = true;
        finaleLaunched = false;
        spawned = elapsed = summonBeat = 0;
        rain.startContinuity(user, StarBlazingPattern.duration());
        user.fallDistance = 0.0F;
        AbilityHelper.setDeltaMovement(user, user.getDeltaMovement().x, LAUNCH_VELOCITY,
                user.getDeltaMovement().z);
        keepAirborne(user);
        updateName(user);
        StarBlazingSounds.cast(user);
    }

    private void tickRain(LivingEntity user, IAbility ability) {
        if (!(user.level instanceof ServerWorld)) return;
        if (!initialized || !user.isAlive() || user.removed || isDisabled() || isCastPaused()) {
            rain.stopContinuity(user);
            return;
        }
        keepAirborne(user);
        // Each launch gets its own tick and fresh aim sample, including formerly simultaneous beats.
        if (spawned < StarBlazingPattern.count() && elapsed >= StarBlazingPattern.launch(spawned).tick) {
            StarBlazingPattern.Launch launch = StarBlazingPattern.launch(spawned++);
            if (launch.finisher) finaleLaunched = true;
            spawnStar(user, launch);
        }
        // The original summons every 16 frames at 30 fps: alternate 10/11 Minecraft tick gaps.
        if (!finaleLaunched && elapsed >= 8 + Math.round(summonBeat * (32.0F / 3.0F))) {
            StarBlazingSounds.summon(user, center);
            summonBeat++;
        }
        elapsed++;
    }

    private void spawnStar(LivingEntity user, StarBlazingPattern.Launch launch) {
        refreshAim(user);
        double angle = user.getRandom().nextDouble() * Math.PI * 2.0D;
        double radius = launch.finisher || spawned == 1 ? 0.0D
                : Math.sqrt(user.getRandom().nextDouble()) * (castEnhanced ? 18.0D : 15.0D);
        Vector3d offset = right.scale(Math.cos(angle) * radius).add(forward.scale(Math.sin(angle) * radius * 0.65D));
        Vector3d target = center;
        // Preserve the scattered, raised spawn positions; aim every star at the crosshair itself.
        Vector3d origin = target.add(offset).add(right.scale(launch.finisher ? 18.0D : 10.0D))
                .add(forward.scale(-4.0D)).add(0.0D, launch.finisher ? 32.0D : 24.0D, 0.0D);
        if (!user.level.hasChunkAt(new BlockPos(origin))) return;
        AsrielStarEntity star = new AsrielStarEntity(user.level, user, false, castEnhanced, launch.finisher);
        star.setPos(origin.x, origin.y, origin.z);
        star.setDeltaMovement(target.subtract(origin).scale(1.0D / launch.travelTicks));
        if (user.level.addFreshEntity(star) && launch.finisher) {
            StarBlazingSounds.finale(user, origin);
        }
    }

    private void refreshAim(LivingEntity user) {
        Vector3d eye = user.getEyePosition(1.0F);
        Vector3d blockAim = YagiAwakeningSupport.aimPoint(user, AIM_RANGE);
        center = blockAim;
        double nearest = eye.distanceToSqr(blockAim);
        // Offset projectiles must converge on the creature under the crosshair, not the wall behind it.
        for (LivingEntity candidate : user.level.getEntitiesOfClass(LivingEntity.class,
                new AxisAlignedBB(eye, blockAim).inflate(1.0D),
                entity -> YagiAwakeningSupport.isEnemy(user, entity))) {
            AxisAlignedBB bounds = candidate.getBoundingBox();
            Optional<Vector3d> hit = bounds.contains(eye) ? Optional.of(eye) : bounds.clip(eye, blockAim);
            if (hit.isPresent() && eye.distanceToSqr(hit.get()) < nearest) {
                center = hit.get();
                nearest = eye.distanceToSqr(center);
            }
        }
        double yaw = Math.toRadians(user.yRot);
        forward = new Vector3d(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        right = new Vector3d(forward.z, 0.0D, -forward.x);
    }

    private boolean isCastPaused() {
        return getComponent(ModAbilityKeys.PAUSE_TICK).map(component -> component.isPaused()).orElse(false);
    }

    private float reduceIncomingDamage(LivingEntity user, IAbility ability, DamageSource source, float damage) {
        return !user.level.isClientSide && user.isAlive() && !user.removed && initialized
                && rain.isContinuous() && !isDisabled() && !isCastPaused() ? damage * 0.5F : damage;
    }

    private void keepAirborne(LivingEntity user) {
        // MMNM's float effect preserves the upward launch, then slows descent for the whole volley.
        // Brief refreshes also expire safely if the caster disconnects or the ability stops ticking.
        if (!user.hasEffect(ModEffects.REDUCED_FALL.get())) ownsAirborneEffect = true;
        user.addEffect(new EffectInstance(ModEffects.REDUCED_FALL.get(), 2, 0, false, false));
        user.fallDistance = 0.0F;
    }

    private void clearAirborneEffect(LivingEntity user) {
        if (user.level.isClientSide) return;
        if (ownsAirborneEffect) {
            EffectInstance effect = user.getEffect(ModEffects.REDUCED_FALL.get());
            // Do not remove a longer or stronger float supplied by another ability.
            if (effect != null && effect.getDuration() <= 2 && effect.getAmplifier() == 0) {
                user.removeEffect(ModEffects.REDUCED_FALL.get());
            }
        }
        ownsAirborneEffect = false;
    }

    private void endRain(LivingEntity user, IAbility ability) {
        if (initialized && !user.level.isClientSide) StarBlazingLandingProtection.grant(user);
        initialized = false;
        clearAirborneEffect(user);
        if (!user.level.isClientSide) cooldownComponent.startCooldown(user, COOLDOWN_TICKS);
        updateName(user);
    }
}
