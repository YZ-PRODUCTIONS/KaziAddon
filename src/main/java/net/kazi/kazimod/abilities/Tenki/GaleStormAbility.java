package net.kazi.kazimod.abilities.Tenki;

import net.MrMagicalCart.cartaddon.abilities.aowrework.entities.projectiles.WeatherCloudReworkEntity;
import net.MrMagicalCart.cartaddon.abilities.aowrework.entities.projectiles.WeatherCloudReworkEntity.CloudForm;
import net.kazi.kazimod.init.KaziParticleEffects;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUseResult;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

import java.util.ArrayList;
import java.util.List;

public class GaleStormAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "gale_storm",
            new Pair[]{
                    ImmutablePair.of("Summons a Windy Cloud that creates a wind barrier around the user, trapping enemies within its range. Requires Cloudy Day to be active.", (Object) null)
            }
    );

    private static final float  MAX_HOLD_TICKS      = 600.0F;
    private static final int    MIN_COOLDOWN        = 100;
    private static final int    MAX_COOLDOWN        = 900;
    private static final int    EXIT_COOLDOWN       = 400;  // penalty for leaving the barrier
    private static final int    CLOUD_HEIGHT_OFFSET = 50;
    private static final float  CLOUD_RADIUS        = 75.0F;
    public  static final double BARRIER_RADIUS      = 83.0;
    private static final double BARRIER_HEIGHT      = 4.0;
    private static final int    PARTICLE_COUNT      = 16;

    // Grace period before the exit check activates (in ticks).
    // Prevents false-positive cancellation on the very first tick when the
    // player is still standing at their cast position.
    private static final int    EXIT_CHECK_GRACE    = 20;

    public static final AbilityCore<GaleStormAbility> INSTANCE;

    private final ContinuousComponent continuousComponent =
            (new ContinuousComponent(this, true))
                    .addStartEvent(this::onStart)
                    .addTickEvent(this::onTick)
                    .addEndEvent(this::onEnd);

    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);
    private final List<WeatherCloudReworkEntity> activeClouds = new ArrayList<>();

    private Vector3d spawnPosition   = null;   // null until onStart fires — guards against ZERO false-positive
    private int      particleTick    = 0;
    private int      cloudRefreshTick = 0;
    private int      activeTicks     = 0;
    private boolean  exitedBarrier   = false;

    public GaleStormAbility(AbilityCore<GaleStormAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.hitTrackerComponent});
        this.addCanUseCheck(this::requiresCloudyDay);
        this.addUseEvent(this::onUse);
    }

    // ── Can-use check ─────────────────────────────────────────────────────────
    private AbilityUseResult requiresCloudyDay(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return AbilityUseResult.success();

        IAbilityData props = AbilityDataCapability.get(entity);
        IAbility cloudyDay = props.getEquippedOrPassiveAbility(CloudyDayAbility.INSTANCE);

        if (cloudyDay == null) {
            return AbilityUseResult.fail(new StringTextComponent("Cloudy Day must be active first."));
        }

        boolean isActive = cloudyDay.getComponent(ModAbilityKeys.CONTINUOUS)
                .map(ContinuousComponent::isContinuous)
                .orElse(false);

        return isActive
                ? AbilityUseResult.success()
                : AbilityUseResult.fail(new StringTextComponent("Cloudy Day must be active first."));
    }

    // ── Use event ─────────────────────────────────────────────────────────────
    private void onUse(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        if (this.cooldownComponent.isOnCooldown()) return;

        if (this.continuousComponent.isContinuous()) {
            this.continuousComponent.stopContinuity(entity);
        } else {
            exitedBarrier = false;

            // Cancel Thunderstorm if currently active
            IAbilityData props = AbilityDataCapability.get(entity);
            IAbility thunderstorm = props.getEquippedOrPassiveAbility(ThunderstormAbility.INSTANCE);
            if (thunderstorm != null) {
                thunderstorm.getComponent(ModAbilityKeys.CONTINUOUS).ifPresent(cc -> {
                    if (cc.isContinuous()) cc.stopContinuity(entity);
                });
            }
            this.continuousComponent.startContinuity(entity, MAX_HOLD_TICKS);
        }
    }

    // ── Cloud spawning helper ─────────────────────────────────────────────────
    private WeatherCloudReworkEntity spawnCloud(LivingEntity entity, double x, double y, double z) {
        EntityType<?> type = ForgeRegistries.ENTITIES.getValue(
                new ResourceLocation("cartaddon", "weather_cloud_rework")
        );
        if (type == null) return null;

        WeatherCloudReworkEntity cloud = new WeatherCloudReworkEntity(
                (EntityType<WeatherCloudReworkEntity>) type,
                entity.level
        );

        if (entity instanceof PlayerEntity) {
            cloud.setOwner((PlayerEntity) entity);
        }

        cloud.setForm(CloudForm.WINDY, false);
        cloud.setRadius(CLOUD_RADIUS);
        cloud.setLife((int) MAX_HOLD_TICKS);
        cloud.setPos(x, y, z);
        cloud.setDeltaMovement(0.0, 0.0, 0.0);
        entity.level.addFreshEntity(cloud);
        return cloud;
    }

    // ── Start ─────────────────────────────────────────────────────────────────
    private void onStart(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        if (!activeClouds.isEmpty()) return;

        IAbilityData props = AbilityDataCapability.get(entity);

        // Switch Wind Gust → Wind Fury
        WindGustAbility windGust = (WindGustAbility) props.getEquippedAbility(WindGustAbility.INSTANCE);
        if (windGust != null) windGust.switchWindFury(entity);

        // Switch Tornado Wrath → Gale Wrath
        TornadoWrathAbility tornadoWrath = (TornadoWrathAbility) props.getEquippedAbility(TornadoWrathAbility.INSTANCE);
        if (tornadoWrath != null) tornadoWrath.switchGaleWrath(entity);

        activeClouds.clear();
        hitTrackerComponent.clearHits();
        particleTick     = 0;
        cloudRefreshTick = 0;
        activeTicks      = 0;
        exitedBarrier    = false;

        IAbility cloudyDayAbility = props.getEquippedOrPassiveAbility(CloudyDayAbility.INSTANCE);
        Vector3d cloudyDayPos = (cloudyDayAbility instanceof CloudyDayAbility)
                ? ((CloudyDayAbility) cloudyDayAbility).getCastPosition()
                : null;

        double centerX = cloudyDayPos != null ? cloudyDayPos.x : entity.getX();
        double centerY = (cloudyDayPos != null ? cloudyDayPos.y : entity.getY()) + CLOUD_HEIGHT_OFFSET;
        double centerZ = cloudyDayPos != null ? cloudyDayPos.z : entity.getZ();

        // Set spawnPosition only here — null guard in onTick prevents false positives
        spawnPosition = new Vector3d(centerX, centerY, centerZ);

        WeatherCloudReworkEntity cloud = spawnCloud(entity, spawnPosition.x, spawnPosition.y, spawnPosition.z);
        if (cloud != null) activeClouds.add(cloud);
    }

    // ── Tick ──────────────────────────────────────────────────────────────────
    private void onTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        // Safety guard — spawnPosition should always be set by onStart, but
        // if somehow it isn't, bail out rather than comparing against ZERO
        if (spawnPosition == null) {
            this.continuousComponent.stopContinuity(entity);
            return;
        }

        // ── Barrier exit check (skipped during grace period) ──────────────────
        if (activeTicks >= EXIT_CHECK_GRACE) {
            double dx   = entity.getX() - spawnPosition.x;
            double dz   = entity.getZ() - spawnPosition.z;
            double dist = Math.sqrt(dx * dx + dz * dz);

            if (dist > BARRIER_RADIUS) {
                exitedBarrier = true;
                if (entity instanceof PlayerEntity) {
                    entity.sendMessage(
                            new StringTextComponent("You left the barrier! Gale Storm has ended."),
                            entity.getUUID()
                    );
                }
                this.continuousComponent.stopContinuity(entity);
                return;
            }
        }

        // ── Cloudy Day still active? ───────────────────────────────────────────
        IAbilityData props = AbilityDataCapability.get(entity);
        IAbility cloudyDay = props.getEquippedOrPassiveAbility(CloudyDayAbility.INSTANCE);
        boolean cloudyDayActive = cloudyDay != null && cloudyDay.getComponent(ModAbilityKeys.CONTINUOUS)
                .map(ContinuousComponent::isContinuous)
                .orElse(false);
        if (!cloudyDayActive) {
            this.continuousComponent.stopContinuity(entity);
            return;
        }

        activeTicks++;

        activeClouds.removeIf(c -> c == null || !c.isAlive());
        if (activeClouds.isEmpty()) {
            this.continuousComponent.stopContinuity(entity);
            return;
        }

        if (cloudRefreshTick++ % 4 == 0) {
            WeatherCloudReworkEntity cloud = activeClouds.get(0);
            cloud.setPos(spawnPosition.x, spawnPosition.y, spawnPosition.z);
            cloud.setDeltaMovement(0.0, 0.0, 0.0);
            cloud.setRadius(CLOUD_RADIUS);
            cloud.setLife((int) MAX_HOLD_TICKS);
        }

        // ── Wind barrier — push enemies back toward cast position ──────────────
        List<?> rawTargets = WyHelper.getEntitiesAroundCircle(
                new Vector3d(spawnPosition.x, entity.getY(), spawnPosition.z),
                entity.level,
                BARRIER_RADIUS, BARRIER_HEIGHT,
                ModEntityPredicates.getEnemyFactions(entity),
                new Class[]{LivingEntity.class}
        );

        for (Object obj : rawTargets) {
            if (!(obj instanceof LivingEntity)) continue;
            LivingEntity target = (LivingEntity) obj;
            Vector3d center = new Vector3d(spawnPosition.x, target.getY(), spawnPosition.z);
            Vector3d rejectionVec = center.subtract(target.position()).normalize();
            AbilityHelper.setDeltaMovement(target, rejectionVec);
        }

        hitTrackerComponent.clearHits();

        // ── Wind gust particles around barrier perimeter ───────────────────────
        if (particleTick++ % 4 == 0) {
            double baseY = spawnPosition.y - CLOUD_HEIGHT_OFFSET;
            double[] heights = {1.0, 16.0, 33.0, 50.0};
            for (double height : heights) {
                for (int i = 0; i < PARTICLE_COUNT; i++) {
                    double angle = (2 * Math.PI / PARTICLE_COUNT) * i;
                    double px = spawnPosition.x + BARRIER_RADIUS * Math.cos(angle);
                    double py = baseY + height;
                    double pz = spawnPosition.z + BARRIER_RADIUS * Math.sin(angle);
                    WyHelper.spawnParticleEffect(
                            (ParticleEffect) KaziParticleEffects.WIND_GUST.get(),
                            entity, px, py, pz
                    );
                }
            }
        }
    }

    // ── End ───────────────────────────────────────────────────────────────────
    private void onEnd(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        IAbilityData props = AbilityDataCapability.get(entity);

        // Revert Wind Gust → Normal
        WindGustAbility windGust = (WindGustAbility) props.getEquippedAbility(WindGustAbility.INSTANCE);
        if (windGust != null) windGust.switchNormal(entity);

        // Revert Tornado Wrath → Normal
        TornadoWrathAbility tornadoWrath = (TornadoWrathAbility) props.getEquippedAbility(TornadoWrathAbility.INSTANCE);
        if (tornadoWrath != null) tornadoWrath.switchNormal(entity);

        for (WeatherCloudReworkEntity cloud : activeClouds) {
            if (cloud != null && cloud.isAlive()) cloud.remove();
        }
        activeClouds.clear();
        hitTrackerComponent.clearHits();
        particleTick     = 0;
        cloudRefreshTick = 0;

        // ── Cooldown selection ─────────────────────────────────────────────────
        // Exiting the barrier applies a fixed heavy cooldown as a penalty.
        // Normal expiry scales between MIN and MAX based on how long it was held.
        int cooldown;
        if (exitedBarrier) {
            cooldown = EXIT_COOLDOWN;
        } else {
            float holdFraction = Math.min(1.0f, (float) activeTicks / MAX_HOLD_TICKS);
            cooldown = MIN_COOLDOWN + (int)((MAX_COOLDOWN - MIN_COOLDOWN) * holdFraction);
        }

        this.cooldownComponent.startCooldown(entity, (float) cooldown);

        activeTicks   = 0;
        exitedBarrier = false;
        spawnPosition = null;  // reset so next onStart sets it cleanly
    }

    // ── Static init ───────────────────────────────────────────────────────────
    static {
        INSTANCE = (new AbilityCore.Builder<>("Gale Storm", AbilityCategory.DEVIL_FRUITS, GaleStormAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        ContinuousComponent.getTooltip(MAX_HOLD_TICKS),
                        CooldownComponent.getTooltip((float) MAX_COOLDOWN)
                })
                .build();
    }
}