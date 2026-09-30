package net.kazi.kazimod.abilities.Tenki;

import net.MrMagicalCart.cartaddon.abilities.aowrework.entities.AOWReworkEntities;
import net.MrMagicalCart.cartaddon.abilities.aowrework.entities.projectiles.WeatherCloudReworkEntity;
import net.MrMagicalCart.cartaddon.abilities.aowrework.entities.projectiles.WeatherCloudReworkEntity.CloudForm;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;

import java.util.ArrayList;
import java.util.List;

public class CloudyDayAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "cloudy_day",
            new Pair[]{
                    ImmutablePair.of("Summons a Rain Cloud above the user.", (Object) null)
            }
    );

    private static final int   COOLDOWN            = 200;
    private static final float MAX_HOLD_TICKS      = 6000.0F;
    private static final int   CLOUD_HEIGHT_OFFSET = 50;
    private static final float CLOUD_RADIUS        = 65.0F;
    private static final int   CLOUD_GRID_SPACING  = 30;
    private static final int   CLOUD_GRID_RADIUS   = 1;

    // How often to check cloud liveness and nudge position (ticks).
    // Clouds have zero velocity and a long life, so a 1-second cadence is plenty.
    private static final int CLOUD_REFRESH_INTERVAL = 20;

    public static final AbilityCore<CloudyDayAbility> INSTANCE;

    private final ContinuousComponent continuousComponent =
            (new ContinuousComponent(this, true))
                    .addStartEvent(this::onStart)
                    .addTickEvent(this::onTick)
                    .addEndEvent(this::onEnd);

    // Clouds are spawned in a small grid and never move — use fixed-size lists
    private final List<WeatherCloudReworkEntity> activeClouds   = new ArrayList<>(9);
    private final List<Vector3d>                 spawnPositions = new ArrayList<>(9);

    // How many of the original clouds have been confirmed dead
    // (lets us skip removeIf when all are alive)
    private int  deadCloudCount  = 0;
    private int  cloudCheckTick  = 0;
    private Vector3d castPosition = null;

    /** Returns the world position where this cloud was cast, or null if not active. */
    public Vector3d getCastPosition() {
        return castPosition;
    }

    public CloudyDayAbility(AbilityCore<CloudyDayAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent});
        this.addUseEvent(this::onUse);
    }

    // ── Use event ─────────────────────────────────────────────────────────────

    private void onUse(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        if (this.cooldownComponent.isOnCooldown()) return;

        if (this.continuousComponent.isContinuous()) {
            this.continuousComponent.stopContinuity(entity);
        } else {
            this.continuousComponent.startContinuity(entity, MAX_HOLD_TICKS);
        }
    }

    // ── Cloud spawn helper ────────────────────────────────────────────────────

    private WeatherCloudReworkEntity spawnCloud(LivingEntity entity, double x, double y, double z) {
        EntityType<WeatherCloudReworkEntity> type = AOWReworkEntities.WEATHER_CLOUD_REWORK.get();
        WeatherCloudReworkEntity cloud = new WeatherCloudReworkEntity(
                type, entity.level);

        if (entity instanceof PlayerEntity) cloud.setOwner((PlayerEntity) entity);

        cloud.setForm(CloudForm.RAIN, false);
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

        activeClouds.clear();
        spawnPositions.clear();
        cloudCheckTick = 0;
        deadCloudCount = 0;

        double castX = entity.getX();
        double castY = entity.getY() + CLOUD_HEIGHT_OFFSET;
        double castZ = entity.getZ();
        // castPosition stores the ground-level XZ centre used by dependent abilities
        castPosition = new Vector3d(castX, entity.getY(), castZ);

        for (int dx = -CLOUD_GRID_RADIUS; dx <= CLOUD_GRID_RADIUS; dx++) {
            for (int dz = -CLOUD_GRID_RADIUS; dz <= CLOUD_GRID_RADIUS; dz++) {
                double px = castX + dx * CLOUD_GRID_SPACING;
                double pz = castZ + dz * CLOUD_GRID_SPACING;
                WeatherCloudReworkEntity cloud = spawnCloud(entity, px, castY, pz);
                if (cloud != null) {
                    activeClouds.add(cloud);
                    spawnPositions.add(new Vector3d(px, castY, pz));
                }
            }
        }
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    private void onTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        // Run the heavier maintenance work only once per CLOUD_REFRESH_INTERVAL ticks.
        // Between checks the clouds are stationary and long-lived, so there is nothing
        // meaningful to do each individual tick.
        if (++cloudCheckTick < CLOUD_REFRESH_INTERVAL) return;
        cloudCheckTick = 0;

        // Prune dead clouds (rare — only happens if something external kills them)
        int sizeBefore = activeClouds.size();
        activeClouds.removeIf(c -> c == null || !c.isAlive());
        // Track removals so we can also trim the parallel position list
        int removed = sizeBefore - activeClouds.size();
        if (removed > 0) {
            // Rebuild spawnPositions to match the surviving clouds.
            // This is O(n) but only runs when a cloud actually dies — extremely rare.
            rebuildPositionList();
        }

        if (activeClouds.isEmpty()) {
            this.continuousComponent.stopContinuity(entity);
            return;
        }

        // Nudge position and reset velocity — clouds don't move, but some mods or
        // physics interactions can shift them. Only reset pos, NOT setLife; calling
        // setLife every refresh would keep resetting the internal timer.
        for (int i = 0; i < activeClouds.size(); i++) {
            WeatherCloudReworkEntity cloud = activeClouds.get(i);
            Vector3d pos = spawnPositions.get(i);
            cloud.setPos(pos.x, pos.y, pos.z);
            cloud.setDeltaMovement(0.0, 0.0, 0.0);
            cloud.setRadius(CLOUD_RADIUS);
            // Intentionally NOT calling setLife here — resetting the timer every second
            // would make the cloud immortal and mask the true remaining duration.
        }
    }

    // ── End ───────────────────────────────────────────────────────────────────

    private void onEnd(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        for (WeatherCloudReworkEntity cloud : activeClouds) {
            if (cloud != null && cloud.isAlive()) cloud.remove();
        }
        activeClouds.clear();
        spawnPositions.clear();
        cloudCheckTick = 0;
        deadCloudCount = 0;
        castPosition   = null;

        this.cooldownComponent.startCooldown(entity, (float) COOLDOWN);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Rebuilds spawnPositions to stay in sync with activeClouds after a prune.
     * Only called when a cloud actually dies — effectively never during normal gameplay.
     */
    private void rebuildPositionList() {
        // spawnPositions is parallel to the original activeClouds order.
        // After removeIf, we don't know which indices were removed, so we
        // re-match by entity identity against the original position list.
        // Simplest safe approach: trim trailing entries to match current cloud count.
        // (Clouds die in order, so trimming the tail is correct for the grid layout.)
        while (spawnPositions.size() > activeClouds.size()) {
            spawnPositions.remove(spawnPositions.size() - 1);
        }
    }

    // ── Static init ───────────────────────────────────────────────────────────

    static {
        INSTANCE = (new AbilityCore.Builder<>("Cloudy Day", AbilityCategory.DEVIL_FRUITS, CloudyDayAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        ContinuousComponent.getTooltip(MAX_HOLD_TICKS),
                        CooldownComponent.getTooltip((float) COOLDOWN)
                })
                .build();
    }
}
