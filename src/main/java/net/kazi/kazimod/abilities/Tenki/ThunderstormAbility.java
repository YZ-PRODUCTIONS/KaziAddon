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

    private static final float BOLT_DAMAGE = 12.0F;
    private static final int   INNER_LIFE  = 24;
    private static final int   OUTER_LIFE  = 36;

    public static final AbilityCore<ThunderstormAbility> INSTANCE;

    private final ContinuousComponent continuousComponent =
            (new ContinuousComponent(this, true))
                    .addStartEvent(this::onStart)
                    .addTickEvent(this::onTick)
                    .addEndEvent(this::onEnd);

    private final List<WeatherCloudReworkEntity> activeClouds   = new ArrayList<>();
    private final List<Vector3d>                 spawnPositions = new ArrayList<>();
    private int strikeTick       = 0;
    private int cloudRefreshTick = 0;
    private int activeTicks      = 0;

    public ThunderstormAbility(AbilityCore<ThunderstormAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent});
        this.addCanUseCheck(this::requiresCloudyDay);
        this.addUseEvent(this::onUse);
    }

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

        cloud.setForm(CloudForm.THUNDER_STORM, false);
        cloud.setRadius(CLOUD_RADIUS);
        cloud.setLife((int) MAX_HOLD_TICKS);
        cloud.setPos(x, y, z);
        cloud.setDeltaMovement(0.0, 0.0, 0.0);
        entity.level.addFreshEntity(cloud);
        return cloud;
    }

    private void onStart(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        if (!activeClouds.isEmpty()) return;

        IAbilityData props = AbilityDataCapability.get(entity);

        // Switch Lightning Jab → Lightning Fury
        LightningJabAbility lightningJab = (LightningJabAbility) props.getEquippedAbility(LightningJabAbility.INSTANCE);
        if (lightningJab != null) lightningJab.switchFuryMode(entity);

        // Switch Tornado Wrath → Thunderous Wrath (takes priority over Gale Wrath)
        TornadoWrathAbility tornadoWrath = (TornadoWrathAbility) props.getEquippedAbility(TornadoWrathAbility.INSTANCE);
        if (tornadoWrath != null) tornadoWrath.switchThunderousWrath(entity);

        activeClouds.clear();
        spawnPositions.clear();
        strikeTick       = 0;
        cloudRefreshTick = 0;
        activeTicks      = 0;

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

    private void onTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

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

        if (cloudRefreshTick++ % 4 == 0) {
            for (int i = 0; i < activeClouds.size(); i++) {
                WeatherCloudReworkEntity cloud = activeClouds.get(i);
                Vector3d pos = spawnPositions.get(i);
                cloud.setPos(pos.x, pos.y, pos.z);
                cloud.setDeltaMovement(0.0, 0.0, 0.0);
                cloud.setRadius(CLOUD_RADIUS);
            }
        }

        if (++strikeTick >= STRIKE_INTERVAL) {
            strikeTick = 0;
            fireStrikes(entity);
        }
    }

    private void fireStrikes(LivingEntity caster) {
        List<LivingEntity> targets = WyHelper.getNearbyEntities(
                caster.position(), caster.level,
                60.0,
                ModEntityPredicates.getEnemyFactions(caster),
                new Class[]{LivingEntity.class}
        );
        targets.remove(caster);
        if (targets.isEmpty()) return;

        boolean blue = ClientConfig.INSTANCE.isGoroBlue();

        for (LivingEntity t : targets) {
            int worldTop = caster.level.getMaxBuildHeight() - 2;
            double startY = Math.min((double) worldTop, t.getY() + 96.0);
            double jitterX = (t.getRandom().nextDouble() - 0.5) * 1.5;
            double jitterZ = (t.getRandom().nextDouble() - 0.5) * 1.5;
            Vector3d start = new Vector3d(t.getX() + jitterX, startY, t.getZ() + jitterZ);
            float travelLength = (float)(startY - (t.getY() + t.getBbHeight() * 0.5)) + 8.0F;

            LightningEntity inner = new LightningEntity(caster, start.x, start.y, start.z, 0.0F, 90.0F, travelLength, 24.0F, this.getCore());
            LightningEntity outer = new LightningEntity(caster, start.x, start.y, start.z, 0.0F, 90.0F, travelLength, 24.0F, this.getCore());

            setBoltProps(inner, 0.34F, travelLength, 0.0F, INNER_LIFE, false, Color.WHITE, 1.0F);
            setBoltProps(outer, 0.4F,  travelLength, BOLT_DAMAGE, OUTER_LIFE, true,
                    blue ? ReworkedElThorAbility.BLUE_THUNDER : ReworkedElThorAbility.YELLOW_THUNDER, 1.0F);
            outer.seed = inner.seed;

            caster.level.addFreshEntity(inner);
            caster.level.addFreshEntity(outer);
            caster.level.playSound(
                    (PlayerEntity) null, t.blockPosition(),
                    ModSounds.EL_THOR_SFX.get(), SoundCategory.WEATHER,
                    2.0F, 1.0F
            );
        }
    }

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

    private void onEnd(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        IAbilityData props = AbilityDataCapability.get(entity);

        // Revert Lightning Jab → Normal
        LightningJabAbility lightningJab = (LightningJabAbility) props.getEquippedAbility(LightningJabAbility.INSTANCE);
        if (lightningJab != null) lightningJab.switchNormalMode(entity);

        // Revert Tornado Wrath — falls back to GALE_WRATH if Gale Storm is still on, else NORMAL
        TornadoWrathAbility tornadoWrath = (TornadoWrathAbility) props.getEquippedAbility(TornadoWrathAbility.INSTANCE);
        if (tornadoWrath != null) tornadoWrath.revertFromThunderous(entity);

        for (WeatherCloudReworkEntity cloud : activeClouds) {
            if (cloud != null && cloud.isAlive()) {
                cloud.remove();
            }
        }
        activeClouds.clear();
        spawnPositions.clear();
        strikeTick       = 0;
        cloudRefreshTick = 0;

        float holdFraction = Math.min(1.0f, (float) activeTicks / MAX_HOLD_TICKS);
        int scaledCooldown = MIN_COOLDOWN + (int)((MAX_COOLDOWN - MIN_COOLDOWN) * holdFraction);
        this.cooldownComponent.startCooldown(entity, (float) scaledCooldown);
        activeTicks = 0;
    }

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