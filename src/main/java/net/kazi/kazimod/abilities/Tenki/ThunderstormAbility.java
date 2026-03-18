package net.kazi.kazimod.abilities.Tenki;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.MrMagicalCart.cartaddon.abilities.goroextra.ReworkedElThorAbility;
import net.MrMagicalCart.cartaddon.abilities.aowrework.entities.projectiles.WeatherCloudReworkEntity;
import net.MrMagicalCart.cartaddon.abilities.aowrework.entities.projectiles.WeatherCloudReworkEntity.CloudForm;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
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
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.config.ClientConfig;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class ThunderstormAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "thunderstorm",
            new Pair[]{
                    ImmutablePair.of("Calls down lightning bolts onto all enemies within the cloud's range once every 5 seconds. Requires Cloudy Day to be active.", (Object) null)
            }
    );

    private static final float MAX_HOLD_TICKS      = 600.0F;
    private static final int   MIN_COOLDOWN        = 100;
    private static final int   MAX_COOLDOWN        = 900;
    private static final int   CLOUD_HEIGHT_OFFSET = 50;
    private static final float CLOUD_RADIUS        = 65.0F;
    private static final int   STRIKE_INTERVAL     = 100;
    // Cap bolts per strike to avoid spawning dozens of entities at once
    private static final int   MAX_TARGETS_PER_STRIKE = 8;

    private static final float BOLT_DAMAGE = 20.0F;
    private static final int   INNER_LIFE  = 24;
    private static final int   OUTER_LIFE  = 36;

    public static final AbilityCore<ThunderstormAbility> INSTANCE;

    private final ContinuousComponent continuousComponent =
            (new ContinuousComponent(this, true))
                    .addStartEvent(this::onStart)
                    .addTickEvent(this::onTick)
                    .addEndEvent(this::onEnd);

    // Reused every strike to avoid allocating a new list each time
    private final List<LivingEntity>             targetScratch  = new ArrayList<>(16);
    private final List<WeatherCloudReworkEntity> activeClouds   = new ArrayList<>(1);
    private final List<Vector3d>                 spawnPositions = new ArrayList<>(1);

    private int strikeTick        = 0;
    // Tracks whether the cloud entity is still alive; checked far less often than every tick
    private int cloudCheckTick    = 0;
    private int activeTicks       = 0;
    // Cache the enemy-faction predicate so it isn't rebuilt every strike
    private java.util.function.Predicate<LivingEntity> cachedEnemyPredicate = null;

    public ThunderstormAbility(AbilityCore<ThunderstormAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent});
        this.addCanUseCheck(this::requiresCloudyDay);
        this.addUseEvent(this::onUse);
    }

    // -------------------------------------------------------------------------
    // Can-use guard
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Use event
    // -------------------------------------------------------------------------

    private void onUse(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        if (this.cooldownComponent.isOnCooldown()) return;

        if (this.continuousComponent.isContinuous()) {
            this.continuousComponent.stopContinuity(entity);
        } else {
            IAbilityData props = AbilityDataCapability.get(entity);
            IAbility galeStorm = props.getEquippedOrPassiveAbility(GaleStormAbility.INSTANCE);
            if (galeStorm != null) {
                galeStorm.getComponent(ModAbilityKeys.CONTINUOUS).ifPresent(cc -> {
                    if (cc.isContinuous()) cc.stopContinuity(entity);
                });
            }
            this.continuousComponent.startContinuity(entity, MAX_HOLD_TICKS);
        }
    }

    // -------------------------------------------------------------------------
    // Cloud helpers
    // -------------------------------------------------------------------------

    private WeatherCloudReworkEntity spawnCloud(LivingEntity entity, double x, double y, double z) {
        EntityType<?> type = ForgeRegistries.ENTITIES.getValue(
                new ResourceLocation("cartaddon", "weather_cloud_rework")
        );
        if (type == null) return null;

        @SuppressWarnings("unchecked")
        WeatherCloudReworkEntity cloud = new WeatherCloudReworkEntity(
                (EntityType<WeatherCloudReworkEntity>) type,
                entity.level
        );

        if (entity instanceof PlayerEntity) {
            cloud.setOwner((PlayerEntity) entity);
        }

        cloud.setForm(CloudForm.THUNDER_STORM, false);
        cloud.setRadius(CLOUD_RADIUS);
        cloud.setLife((int) MAX_HOLD_TICKS);
        cloud.setPos(x, y, z);
        cloud.setDeltaMovement(0.0, 0.0, 0.0);
        entity.level.addFreshEntity(cloud);
        return cloud;
    }

    // -------------------------------------------------------------------------
    // onStart
    // -------------------------------------------------------------------------

    private void onStart(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        if (!activeClouds.isEmpty()) return;

        IAbilityData props = AbilityDataCapability.get(entity);

        // Switch Lightning Jab → Lightning Fury
        LightningJabAbility lightningJab = (LightningJabAbility) props.getEquippedAbility(LightningJabAbility.INSTANCE);
        if (lightningJab != null) lightningJab.switchFuryMode(entity);

        // Switch Tornado Wrath → Thunderous Wrath
        TornadoWrathAbility tornadoWrath = (TornadoWrathAbility) props.getEquippedAbility(TornadoWrathAbility.INSTANCE);
        if (tornadoWrath != null) tornadoWrath.switchThunderousWrath(entity);

        activeClouds.clear();
        spawnPositions.clear();
        strikeTick     = 0;
        cloudCheckTick = 0;
        activeTicks    = 0;

        // Cache the enemy predicate once per activation to avoid rebuilding it every strike
        cachedEnemyPredicate = null;

        IAbility cloudyDayAbility = props.getEquippedOrPassiveAbility(CloudyDayAbility.INSTANCE);
        Vector3d cloudyDayPos = (cloudyDayAbility instanceof CloudyDayAbility)
                ? ((CloudyDayAbility) cloudyDayAbility).getCastPosition()
                : null;

        double centerX = cloudyDayPos != null ? cloudyDayPos.x : entity.getX();
        double centerY = (cloudyDayPos != null ? cloudyDayPos.y : entity.getY()) + CLOUD_HEIGHT_OFFSET;
        double centerZ = cloudyDayPos != null ? cloudyDayPos.z : entity.getZ();

        WeatherCloudReworkEntity cloud = spawnCloud(entity, centerX, centerY, centerZ);
        if (cloud != null) {
            activeClouds.add(cloud);
            spawnPositions.add(new Vector3d(centerX, centerY, centerZ));
        }
    }

    // -------------------------------------------------------------------------
    // onTick  (runs every server tick while active)
    // -------------------------------------------------------------------------

    private void onTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        // --- Check Cloudy Day is still active (every 20 ticks is enough) ---
        // Avoids calling getEquippedOrPassiveAbility + Optional every single tick
        if (++cloudCheckTick >= 20) {
            cloudCheckTick = 0;
            IAbilityData props = AbilityDataCapability.get(entity);
            IAbility cloudyDay = props.getEquippedOrPassiveAbility(CloudyDayAbility.INSTANCE);
            boolean cloudyDayActive = cloudyDay != null && cloudyDay.getComponent(ModAbilityKeys.CONTINUOUS)
                    .map(ContinuousComponent::isContinuous)
                    .orElse(false);
            if (!cloudyDayActive) {
                this.continuousComponent.stopContinuity(entity);
                return;
            }

            // Also purge dead clouds on the same cadence — no need to do it every tick
            activeClouds.removeIf(c -> c == null || !c.isAlive());
        }

        activeTicks++;

        // Fire strikes on interval
        if (++strikeTick >= STRIKE_INTERVAL) {
            strikeTick = 0;
            fireStrikes(entity);
        }
    }

    // -------------------------------------------------------------------------
    // fireStrikes — the hottest path; keep allocations minimal
    // -------------------------------------------------------------------------

    private void fireStrikes(LivingEntity caster) {
        // Reuse scratch list instead of allocating a new one every call
        targetScratch.clear();

        // Use a direct AABB query instead of WyHelper which may do extra work.
        // We query a 60-block box, then filter by faction predicate ourselves.
        if (cachedEnemyPredicate == null) {
            // Build once; ModEntityPredicates.getEnemyFactions returns a Predicate
            cachedEnemyPredicate = buildEnemyPredicate(caster);
        }

        AxisAlignedBB searchBox = new AxisAlignedBB(
                caster.getX() - 60, caster.getY() - 20, caster.getZ() - 60,
                caster.getX() + 60, caster.getY() + 80, caster.getZ() + 60
        );

        List<LivingEntity> nearby = caster.level.getEntitiesOfClass(
                LivingEntity.class, searchBox, cachedEnemyPredicate
        );

        for (LivingEntity e : nearby) {
            if (e != caster) targetScratch.add(e);
        }

        if (targetScratch.isEmpty()) return;

        // Cap targets per strike to bound worst-case entity spam
        int count = Math.min(targetScratch.size(), MAX_TARGETS_PER_STRIKE);
        boolean blue = ClientConfig.INSTANCE.isGoroBlue();
        Color thunderColor = blue ? ReworkedElThorAbility.BLUE_THUNDER : ReworkedElThorAbility.YELLOW_THUNDER;

        for (int i = 0; i < count; i++) {
            LivingEntity t = targetScratch.get(i);
            spawnBoltPair(caster, t, thunderColor);
        }
    }

    /** Isolated so the JIT can inline/optimise the tight per-target loop. */
    private void spawnBoltPair(LivingEntity caster, LivingEntity t, Color thunderColor) {
        int worldTop = caster.level.getMaxBuildHeight() - 2;
        double startY = Math.min(worldTop, t.getY() + 96.0);
        double jitterX = (t.getRandom().nextDouble() - 0.5) * 1.5;
        double jitterZ = (t.getRandom().nextDouble() - 0.5) * 1.5;

        double sx = t.getX() + jitterX;
        double sz = t.getZ() + jitterZ;
        float travelLength = (float)(startY - (t.getY() + t.getBbHeight() * 0.5)) + 8.0F;

        LightningEntity inner = new LightningEntity(caster, sx, startY, sz, 0.0F, 90.0F, travelLength, 24.0F, this.getCore());
        LightningEntity outer = new LightningEntity(caster, sx, startY, sz, 0.0F, 90.0F, travelLength, 24.0F, this.getCore());

        setBoltProps(inner, 0.34F, travelLength, 0.0F,       INNER_LIFE, false, Color.WHITE,   1.0F);
        setBoltProps(outer, 0.4F,  travelLength, BOLT_DAMAGE, OUTER_LIFE, true,  thunderColor, 1.0F);
        // Share the same seed so both bolts trace identical paths
        outer.seed = inner.seed;

        caster.level.addFreshEntity(inner);
        caster.level.addFreshEntity(outer);
        caster.level.playSound(
                (PlayerEntity) null, t.blockPosition(),
                ModSounds.EL_THOR_SFX.get(), SoundCategory.WEATHER,
                2.0F, 1.0F
        );
    }

    // -------------------------------------------------------------------------
    // Bolt configuration
    // -------------------------------------------------------------------------

    private void setBoltProps(LightningEntity bolt, float size, double length, float damage,
                              int timeAlive, boolean explodes, @Nullable Color color, float multiplier) {
        int segments = (int)(length * 0.45F);
        bolt.setBlocksAffectedLimit(30000);
        bolt.setMaxLife(timeAlive);
        bolt.setDamage(damage * multiplier);
        if (explodes) {
            bolt.setExplosion((int)(4.0F * multiplier), true, 0.3F);
        } else {
            bolt.setExplosion(0, false);
        }
        bolt.setSize(size * multiplier);
        bolt.setBoxSizeDivision(0.2);
        bolt.setColor(color);
        bolt.setAngle(160);
        bolt.setTargetTimeToReset(9999);
        bolt.disableExplosionKnockback();
        bolt.setBranches(1);
        bolt.setSegments((int)(segments + WyHelper.randomWithRange(-segments / 4, segments / 4)));
    }

    // -------------------------------------------------------------------------
    // onEnd
    // -------------------------------------------------------------------------

    private void onEnd(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        IAbilityData props = AbilityDataCapability.get(entity);

        // Revert Lightning Jab → Normal
        LightningJabAbility lightningJab = (LightningJabAbility) props.getEquippedAbility(LightningJabAbility.INSTANCE);
        if (lightningJab != null) lightningJab.switchNormalMode(entity);

        // Revert Tornado Wrath
        TornadoWrathAbility tornadoWrath = (TornadoWrathAbility) props.getEquippedAbility(TornadoWrathAbility.INSTANCE);
        if (tornadoWrath != null) tornadoWrath.revertFromThunderous(entity);

        for (WeatherCloudReworkEntity cloud : activeClouds) {
            if (cloud != null && cloud.isAlive()) cloud.remove();
        }
        activeClouds.clear();
        spawnPositions.clear();
        targetScratch.clear();
        cachedEnemyPredicate = null;
        strikeTick     = 0;
        cloudCheckTick = 0;

        float holdFraction = Math.min(1.0f, (float) activeTicks / MAX_HOLD_TICKS);
        int scaledCooldown = MIN_COOLDOWN + (int)((MAX_COOLDOWN - MIN_COOLDOWN) * holdFraction);
        this.cooldownComponent.startCooldown(entity, (float) scaledCooldown);
        activeTicks = 0;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Wraps ModEntityPredicates.getEnemyFactions in a Predicate<LivingEntity>
     * compatible with level.getEntitiesOfClass.
     * Built once per activation and cached.
     */
    @SuppressWarnings("unchecked")
    private java.util.function.Predicate<LivingEntity> buildEnemyPredicate(LivingEntity caster) {
        // ModEntityPredicates.getEnemyFactions already returns a Predicate; cast if needed.
        Object raw = ModEntityPredicates.getEnemyFactions(caster);
        if (raw instanceof java.util.function.Predicate) {
            return (java.util.function.Predicate<LivingEntity>) raw;
        }
        // Fallback: accept everything (shouldn't happen)
        return e -> true;
    }

    // -------------------------------------------------------------------------
    // Static init
    // -------------------------------------------------------------------------

    static {
        INSTANCE = (new AbilityCore.Builder<>("Thunderstorm", AbilityCategory.DEVIL_FRUITS, ThunderstormAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        ContinuousComponent.getTooltip(MAX_HOLD_TICKS),
                        CooldownComponent.getTooltip((float) MAX_COOLDOWN)
                })
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceElement(SourceElement.LIGHTNING)
                .build();
    }
}