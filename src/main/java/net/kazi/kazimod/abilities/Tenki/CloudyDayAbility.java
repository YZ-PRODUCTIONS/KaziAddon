package net.kazi.kazimod.abilities.Tenki;

import net.MrMagicalCart.cartaddon.abilities.aowrework.entities.projectiles.WeatherCloudReworkEntity;
import net.MrMagicalCart.cartaddon.abilities.aowrework.entities.projectiles.WeatherCloudReworkEntity.CloudForm;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.registries.ForgeRegistries;
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

    public static final AbilityCore<CloudyDayAbility> INSTANCE;

    private final ContinuousComponent continuousComponent =
            (new ContinuousComponent(this, true))
                    .addStartEvent(this::onStart)
                    .addTickEvent(this::onTick)
                    .addEndEvent(this::onEnd);

    private final List<WeatherCloudReworkEntity> activeClouds = new ArrayList<>();
    private final List<Vector3d> spawnPositions = new ArrayList<>();
    private int cloudRefreshTick = 0;
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

    private void onUse(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        if (this.cooldownComponent.isOnCooldown()) return;

        if (this.continuousComponent.isContinuous()) {
            this.continuousComponent.stopContinuity(entity);
        } else {
            this.continuousComponent.startContinuity(entity, MAX_HOLD_TICKS);
        }
    }

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

        cloud.setForm(CloudForm.RAIN, false);
        cloud.setRadius(CLOUD_RADIUS);
        cloud.setLife((int) MAX_HOLD_TICKS);
        cloud.setPos(x, y, z);
        cloud.setDeltaMovement(0.0, 0.0, 0.0);
        entity.level.addFreshEntity(cloud);
        return cloud;
    }

    private void onStart(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        if (!activeClouds.isEmpty()) return; // prevent double-spawn

        activeClouds.clear();
        spawnPositions.clear();
        cloudRefreshTick = 0;

        double castX = entity.getX();
        double castY = entity.getY() + CLOUD_HEIGHT_OFFSET;
        double castZ = entity.getZ();
        castPosition = new Vector3d(castX, entity.getY(), castZ);

        for (int dx = -CLOUD_GRID_RADIUS; dx <= CLOUD_GRID_RADIUS; dx++) {
            for (int dz = -CLOUD_GRID_RADIUS; dz <= CLOUD_GRID_RADIUS; dz++) {
                double px = castX + dx * CLOUD_GRID_SPACING;
                double py = castY;
                double pz = castZ + dz * CLOUD_GRID_SPACING;
                WeatherCloudReworkEntity cloud = spawnCloud(entity, px, py, pz);
                if (cloud != null) {
                    activeClouds.add(cloud);
                    spawnPositions.add(new Vector3d(px, py, pz));
                }
            }
        }
    }

    private void onTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        activeClouds.removeIf(c -> c == null || !c.isAlive());

        if (activeClouds.isEmpty()) {
            this.continuousComponent.stopContinuity(entity);
            return;
        }

        if (cloudRefreshTick++ % 4 == 0) {
            for (int i = 0; i < activeClouds.size(); i++) {
                WeatherCloudReworkEntity cloud = activeClouds.get(i);
                Vector3d pos = spawnPositions.get(i);
                cloud.setPos(pos.x, pos.y, pos.z);
                cloud.setDeltaMovement(0.0, 0.0, 0.0);
                cloud.setRadius(CLOUD_RADIUS);
                cloud.setLife((int) MAX_HOLD_TICKS);
            }
        }
    }

    private void onEnd(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        for (WeatherCloudReworkEntity cloud : activeClouds) {
            if (cloud != null && cloud.isAlive()) {
                cloud.remove();
            }
        }
        activeClouds.clear();
        spawnPositions.clear();
        cloudRefreshTick = 0;
        castPosition = null;

        this.cooldownComponent.startCooldown(entity, (float) COOLDOWN);
    }

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