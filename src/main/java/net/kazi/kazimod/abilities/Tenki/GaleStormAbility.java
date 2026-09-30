package net.kazi.kazimod.abilities.Tenki;

import net.MrMagicalCart.cartaddon.abilities.aowrework.entities.AOWReworkEntities;
import net.MrMagicalCart.cartaddon.abilities.aowrework.entities.projectiles.WeatherCloudReworkEntity;
import net.MrMagicalCart.cartaddon.abilities.aowrework.entities.projectiles.WeatherCloudReworkEntity.CloudForm;
import net.kazi.kazimod.init.KaziParticleEffects;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
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
import java.util.function.Predicate;

public class GaleStormAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "gale_storm",
            new Pair[]{
                    ImmutablePair.of("Summons a Windy Cloud that creates a wind barrier around the user, trapping enemies within its range. Requires Cloudy Day to be active.", (Object) null)
            }
    );

    private static final float  MAX_HOLD_TICKS      = 400.0F;
    private static final int    MIN_COOLDOWN        = 100;
    private static final int    MAX_COOLDOWN        = 900;
    private static final int    EXIT_COOLDOWN       = 400;
    private static final int    CLOUD_HEIGHT_OFFSET = 50;
    private static final float  CLOUD_RADIUS        = 75.0F;
    public  static final double BARRIER_RADIUS      = 83.0;
    private static final double BARRIER_RADIUS_SQ   = BARRIER_RADIUS * BARRIER_RADIUS; // avoid sqrt in exit check
    private static final double BARRIER_HEIGHT      = 4.0;
    private static final int    PARTICLE_COUNT      = 16;
    private static final int    EXIT_CHECK_GRACE    = 20;

    // Pre-baked sin/cos for particle ring — computed once at class-load, never again
    private static final double[] PARTICLE_COS = new double[PARTICLE_COUNT];
    private static final double[] PARTICLE_SIN = new double[PARTICLE_COUNT];
    static {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            double angle = (2 * Math.PI / PARTICLE_COUNT) * i;
            PARTICLE_COS[i] = Math.cos(angle);
            PARTICLE_SIN[i] = Math.sin(angle);
        }
    }

    // How often (in ticks) to run the slow/expensive checks
    private static final int CLOUDY_DAY_CHECK_INTERVAL = 20; // once per second
    private static final int CLOUD_ALIVE_CHECK_INTERVAL = 20;
    private static final int BARRIER_PUSH_INTERVAL      = 2;  // every other tick — still very responsive

    public static final AbilityCore<GaleStormAbility> INSTANCE;

    private final ContinuousComponent continuousComponent =
            (new ContinuousComponent(this, true))
                    .addStartEvent(this::onStart)
                    .addTickEvent(this::onTick)
                    .addEndEvent(this::onEnd);

    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);

    // Reused scratch list — avoids allocating a new List every push tick
    private final List<LivingEntity>             targetScratch = new ArrayList<>(16);
    private final List<WeatherCloudReworkEntity> activeClouds  = new ArrayList<>(1);

    private Vector3d spawnPosition   = null;
    private int      particleTick    = 0;
    private int      cloudCheckTick  = 0;
    private int      clouddayCheckTick = 0;
    private int      pushTick        = 0;
    private int      activeTicks     = 0;
    private boolean  exitedBarrier   = false;

    // Cached once per activation — enemy predicate and AABB don't change while active
    private Predicate<LivingEntity> cachedEnemyPredicate = null;
    private AxisAlignedBB           cachedBarrierBox     = null;

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

        if (cloudyDay == null)
            return AbilityUseResult.fail(new StringTextComponent("Cloudy Day must be active first."));

        boolean isActive = cloudyDay.getComponent(ModAbilityKeys.CONTINUOUS)
                .map(ContinuousComponent::isContinuous).orElse(false);

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
        EntityType<WeatherCloudReworkEntity> type = AOWReworkEntities.WEATHER_CLOUD_REWORK.get();
        WeatherCloudReworkEntity cloud = new WeatherCloudReworkEntity(
                type, entity.level);

        if (entity instanceof PlayerEntity) cloud.setOwner((PlayerEntity) entity);

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

        WindGustAbility windGust = (WindGustAbility) props.getEquippedAbility(WindGustAbility.INSTANCE);
        if (windGust != null) windGust.switchWindFury(entity);

        TornadoWrathAbility tornadoWrath = (TornadoWrathAbility) props.getEquippedAbility(TornadoWrathAbility.INSTANCE);
        if (tornadoWrath != null) tornadoWrath.switchGaleWrath(entity);

        activeClouds.clear();
        hitTrackerComponent.clearHits();
        particleTick       = 0;
        cloudCheckTick     = 0;
        clouddayCheckTick  = 0;
        pushTick           = 0;
        activeTicks        = 0;
        exitedBarrier      = false;

        IAbility cloudyDayAbility = props.getEquippedOrPassiveAbility(CloudyDayAbility.INSTANCE);
        Vector3d cloudyDayPos = (cloudyDayAbility instanceof CloudyDayAbility)
                ? ((CloudyDayAbility) cloudyDayAbility).getCastPosition() : null;

        double centerX = cloudyDayPos != null ? cloudyDayPos.x : entity.getX();
        double centerY = (cloudyDayPos != null ? cloudyDayPos.y : entity.getY()) + CLOUD_HEIGHT_OFFSET;
        double centerZ = cloudyDayPos != null ? cloudyDayPos.z : entity.getZ();

        spawnPosition = new Vector3d(centerX, centerY, centerZ);

        // Build the AABB and predicate once — they don't change while active
        cachedEnemyPredicate = buildEnemyPredicate(entity);
        cachedBarrierBox = new AxisAlignedBB(
                centerX - BARRIER_RADIUS, entity.getY() - BARRIER_HEIGHT,
                centerZ - BARRIER_RADIUS,
                centerX + BARRIER_RADIUS, entity.getY() + BARRIER_HEIGHT + 32,
                centerZ + BARRIER_RADIUS
        );

        WeatherCloudReworkEntity cloud = spawnCloud(entity, spawnPosition.x, spawnPosition.y, spawnPosition.z);
        if (cloud != null) activeClouds.add(cloud);
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    private void onTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        if (spawnPosition == null) {
            this.continuousComponent.stopContinuity(entity);
            return;
        }

        // ── Barrier exit check (squared distance — no sqrt needed) ────────────
        if (activeTicks >= EXIT_CHECK_GRACE) {
            double dx = entity.getX() - spawnPosition.x;
            double dz = entity.getZ() - spawnPosition.z;
            if (dx * dx + dz * dz > BARRIER_RADIUS_SQ) {
                exitedBarrier = true;
                if (entity instanceof PlayerEntity) {
                    entity.sendMessage(
                            new StringTextComponent("You left the barrier! Gale Storm has ended."),
                            entity.getUUID());
                }
                this.continuousComponent.stopContinuity(entity);
                return;
            }
        }

        // ── Cloudy Day still active? (throttled to once per second) ───────────
        if (++clouddayCheckTick >= CLOUDY_DAY_CHECK_INTERVAL) {
            clouddayCheckTick = 0;
            IAbilityData props = AbilityDataCapability.get(entity);
            IAbility cloudyDay = props.getEquippedOrPassiveAbility(CloudyDayAbility.INSTANCE);
            boolean active = cloudyDay != null && cloudyDay.getComponent(ModAbilityKeys.CONTINUOUS)
                    .map(ContinuousComponent::isContinuous).orElse(false);
            if (!active) {
                this.continuousComponent.stopContinuity(entity);
                return;
            }

            // Also prune dead clouds on the same cadence
            activeClouds.removeIf(c -> c == null || !c.isAlive());
            if (activeClouds.isEmpty()) {
                this.continuousComponent.stopContinuity(entity);
                return;
            }
        }

        activeTicks++;

        // ── Wind barrier push (every BARRIER_PUSH_INTERVAL ticks) ─────────────
        if (++pushTick >= BARRIER_PUSH_INTERVAL) {
            pushTick = 0;
            doPush(entity);
        }

        hitTrackerComponent.clearHits();

        // ── Particle ring (every 4 ticks, using pre-baked sin/cos) ─────────────
        if (particleTick++ % 4 == 0) {
            double baseY   = spawnPosition.y - CLOUD_HEIGHT_OFFSET;
            double cx      = spawnPosition.x;
            double cz      = spawnPosition.z;
            double[] heights = {1.0, 16.0, 33.0, 50.0};
            ParticleEffect windGustEffect = (ParticleEffect) KaziParticleEffects.WIND_GUST.get();
            for (double height : heights) {
                double py = baseY + height;
                for (int i = 0; i < PARTICLE_COUNT; i++) {
                    WyHelper.spawnParticleEffect(
                            windGustEffect, entity,
                            cx + BARRIER_RADIUS * PARTICLE_COS[i],
                            py,
                            cz + BARRIER_RADIUS * PARTICLE_SIN[i]
                    );
                }
            }
        }
    }

    // ── Barrier push — isolated for clarity and JIT friendliness ─────────────

    private void doPush(LivingEntity caster) {
        targetScratch.clear();

        List<LivingEntity> nearby = caster.level.getEntitiesOfClass(
                LivingEntity.class, cachedBarrierBox, cachedEnemyPredicate);

        double cx = spawnPosition.x;
        double cz = spawnPosition.z;

        for (LivingEntity target : nearby) {
            if (target == caster) continue;
            // Only push entities that are actually outside the radius
            double dx = target.getX() - cx;
            double dz = target.getZ() - cz;
            if (dx * dx + dz * dz < BARRIER_RADIUS_SQ) continue;

            Vector3d center = new Vector3d(cx, target.getY(), cz);
            Vector3d rejectionVec = center.subtract(target.position()).normalize();
            AbilityHelper.setDeltaMovement(target, rejectionVec);
        }
    }

    // ── End ───────────────────────────────────────────────────────────────────

    private void onEnd(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        IAbilityData props = AbilityDataCapability.get(entity);

        WindGustAbility windGust = (WindGustAbility) props.getEquippedAbility(WindGustAbility.INSTANCE);
        if (windGust != null) windGust.switchNormal(entity);

        TornadoWrathAbility tornadoWrath = (TornadoWrathAbility) props.getEquippedAbility(TornadoWrathAbility.INSTANCE);
        if (tornadoWrath != null) tornadoWrath.switchNormal(entity);

        for (WeatherCloudReworkEntity cloud : activeClouds) {
            if (cloud != null && cloud.isAlive()) cloud.remove();
        }
        activeClouds.clear();
        hitTrackerComponent.clearHits();
        targetScratch.clear();
        particleTick       = 0;
        cloudCheckTick     = 0;
        clouddayCheckTick  = 0;
        pushTick           = 0;

        int cooldown;
        if (exitedBarrier) {
            cooldown = EXIT_COOLDOWN;
        } else {
            float holdFraction = Math.min(1.0f, (float) activeTicks / MAX_HOLD_TICKS);
            cooldown = MIN_COOLDOWN + (int)((MAX_COOLDOWN - MIN_COOLDOWN) * holdFraction);
        }

        this.cooldownComponent.startCooldown(entity, (float) cooldown);

        activeTicks          = 0;
        exitedBarrier        = false;
        spawnPosition        = null;
        cachedEnemyPredicate = null;
        cachedBarrierBox     = null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Predicate<LivingEntity> buildEnemyPredicate(LivingEntity caster) {
        Object raw = ModEntityPredicates.getEnemyFactions(caster);
        if (raw instanceof Predicate) return (Predicate<LivingEntity>) raw;
        return e -> true; // safe fallback
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
