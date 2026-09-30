package net.kazi.kazimod.abilities.GoroRework;

import java.awt.Color;
import javax.annotation.Nullable;
import net.kazi.kazimod.api.KaziRegistry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.Util;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUseResult;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.config.ClientConfig;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.RaigoProjectile;
import xyz.pixelatedw.mineminenomi.init.ModI18n;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

/** Kazi-owned copy of Mine Mine no Mi 0.10.11's Raigo and Mamaragan modes. */
public class RaigoRework extends Ability {
    private static final ResourceLocation DEFAULT_ICON =
            new ResourceLocation("mineminenomi", "textures/abilities/raigo.png");
    private static final ResourceLocation ALT_ICON =
            new ResourceLocation("mineminenomi", "textures/abilities/alts/raigo.png");
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "raigo",
            new Pair[]{
                    ImmutablePair.of(
                            "The user shapes a vast quantity of prior-made thunderclouds into one giant, dark sphere charged with inordinate electricity.",
                            null),
                    ImmutablePair.of(
                            "The user causes the nearby thunderclouds to shower everything below them with dozens of massive lightning bolts that create fire wherever they strike",
                            null),
                    ImmutablePair.of("Can only be used during a thunderstorm.", null)
            });
    private static final ITextComponent MAMARAGAN_NAME = new TranslationTextComponent(
            KaziRegistry.registerName("ability.kazimod.mamaragan", "Mamaragan"));
    private static final ITextComponent RAIGO_NAME = new TranslationTextComponent(
            KaziRegistry.registerName("ability.kazimod.raigo", "Raigo"));
    private static final ResourceLocation MAMARAGAN_ICON =
            new ResourceLocation("mineminenomi", "textures/abilities/mamaragan.png");
    private static final ResourceLocation RAIGO_ICON = DEFAULT_ICON;

    public static final AbilityCore<RaigoRework> INSTANCE =
            new AbilityCore.Builder<RaigoRework>(
                    "Raigo", AbilityCategory.DEVIL_FRUITS, RaigoRework::new)
                    .addAdvancedDescriptionLine(
                            (e, a) -> RAIGO_NAME.copy()
                                    .setStyle(Style.EMPTY.withColor(TextFormatting.GREEN)),
                            (e, a) -> DESCRIPTION[0],
                            AbilityDescriptionLine.NEW_LINE,
                            (e, a) -> ModI18n.ABILITY_DESCRIPTION_DURING_THUNDERSTORM.copy(),
                            CooldownComponent.getTooltip(700.0F),
                            ChargeComponent.getTooltip(100.0F),
                            (e, a) -> ModI18n.ABILITY_DESCRIPTION_WITHOUT_THUNDERSTORM.copy(),
                            CooldownComponent.getTooltip(1700.0F),
                            ChargeComponent.getTooltip(240.0F),
                            AbilityDescriptionLine.NEW_LINE)
                    .addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips())
                    .addAdvancedDescriptionLine(
                            AbilityDescriptionLine.NEW_LINE,
                            (e, a) -> MAMARAGAN_NAME.copy()
                                    .setStyle(Style.EMPTY.withColor(TextFormatting.GREEN)),
                            (e, a) -> DESCRIPTION[1],
                            (e, a) -> DESCRIPTION[2],
                            AbilityDescriptionLine.NEW_LINE,
                            CooldownComponent.getTooltip(1400.0F),
                            ContinuousComponent.getTooltip(1200.0F))
                    .setSourceElement(SourceElement.LIGHTNING)
                    .setIcon(DEFAULT_ICON)
                    .build();

    private final ChargeComponent chargeComponent =
            new ChargeComponent(this)
                    .addStartEvent(this::onChargeStart)
                    .addTickEvent(this::onChargeTick)
                    .addEndEvent(this::onChargeEnd);
    private final ContinuousComponent continuousComponent =
            new ContinuousComponent(this, true)
                    .addTickEvent(this::onContinuityTick)
                    .addEndEvent(this::onContinuityEnd);
    private final ProjectileComponent projectileComponent =
            new ProjectileComponent(this, this::createProjectile);
    private final AltModeComponent<Mode> altModeComponent =
            new AltModeComponent<>(this, Mode.class, Mode.RAIGO)
                    .addChangeModeEvent(this::onAltModeChange);
    private final Interval spawnLightningInterval = new Interval(10);

    private RaigoProjectile raigoProjectile;
    private boolean isThundering;

    public RaigoRework(AbilityCore<RaigoRework> core) {
        super(core);
        updateDisplayIcon();
        this.isNew = true;
        this.addComponents(
                chargeComponent, continuousComponent, altModeComponent, projectileComponent);
        this.addUseEvent(this::onUseEvent);
        this.addEquipEvent((entity, ability) -> updateDisplayIcon());
    }

    private void updateDisplayIcon() {
        this.setDisplayIcon(ClientConfig.INSTANCE.isGoroBlue() ? ALT_ICON : DEFAULT_ICON);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        AbilityUseResult thunderstormResult = canUseEvent(entity, ability);
        if (altModeComponent.getCurrentMode() == Mode.MAMARAGAN) {
            if (!continuousComponent.isContinuous() && thunderstormResult.isFail()) {
                if (thunderstormResult.getMessage() != null) {
                    entity.sendMessage(thunderstormResult.getMessage(), Util.NIL_UUID);
                }
                return;
            }
            spawnLightningInterval.restartIntervalToZero();
            continuousComponent.triggerContinuity(entity, 1200.0F);
        } else {
            if (continuousComponent.isContinuous()) return;
            if (!chargeComponent.isCharging()) {
                isThundering = thunderstormResult.isSuccess();
                spawnLightningInterval.restartIntervalToZero();
                chargeComponent.startCharging(entity, isThundering ? 240.0F : 100.0F);
            } else if (chargeComponent.getChargeTime() >= 20.0F) {
                chargeComponent.stopCharging(entity);
            }
        }
    }

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        RayTraceResult mop = WyHelper.rayTraceBlocksAndEntities(entity);
        raigoProjectile = new RaigoProjectile(entity.level, entity, this);
        raigoProjectile.moveTo(
                mop.getLocation().x, 128.0D, mop.getLocation().z, 0.0F, 0.0F);
        entity.level.addFreshEntity(raigoProjectile);
    }

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        if (raigoProjectile == null || !raigoProjectile.isAlive() || canUse(entity).isFail()) {
            chargeComponent.stopCharging(entity);
            return;
        }
        raigoProjectile.setLife(raigoProjectile.getMaxLife());
        raigoProjectile.increaseSize(isThundering ? 0.08F : 0.0032F);
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            continuousComponent.startContinuity(entity, WyHelper.secondsToTicks(10.0F));
        }
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        if (altModeComponent.getCurrentMode() == Mode.RAIGO) {
            if (raigoProjectile == null || !raigoProjectile.isAlive()) {
                continuousComponent.stopContinuity(entity);
                return;
            }
            raigoProjectile.setLife(raigoProjectile.getMaxLife());
            AbilityHelper.setDeltaMovement(raigoProjectile, 0.0D, -0.75D, 0.0D);
        } else if (spawnLightningInterval.canTick()) {
            if (canUse(entity).isFail()) {
                continuousComponent.stopContinuity(entity);
                return;
            }
            spawnMamaraganBolt(entity);
        }
    }

    private void spawnMamaraganBolt(LivingEntity entity) {
        double offsetX = WyHelper.randomWithRange(-200, 200);
        double offsetZ = WyHelper.randomWithRange(-200, 200);
        float multiplier = 0.46F;
        Vector3d pos = new Vector3d(
                entity.getX() + offsetX, 128.0D, entity.getZ() + offsetZ);
        LightningEntity boltInner = new LightningEntity(
                entity, pos.x, pos.y, pos.z, 0.0F, 90.0F, 144.0F, 24.0F, getCore());
        LightningEntity boltOuter = new LightningEntity(
                entity, pos.x, pos.y, pos.z, 0.0F, 90.0F, 144.0F, 24.0F, getCore());
        setBoltProperties(boltInner, 2.0F, 0.0F, 90, 40, false, Color.WHITE, multiplier);
        setBoltProperties(
                boltOuter, 2.5F, 70.0F, 100, 9999, true,
                ClientConfig.INSTANCE.isGoroBlue()
                        ? ElThorRework.BLUE_THUNDER : ElThorRework.YELLOW_THUNDER,
                multiplier);
        boltOuter.seed = boltInner.seed;
        entity.level.addFreshEntity(boltInner);
        entity.level.addFreshEntity(boltOuter);
        entity.level.playSound(
                null, boltInner.blockPosition(), ModSounds.EL_THOR_SFX.get(),
                SoundCategory.PLAYERS, 30.0F, 1.0F);
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        if (altModeComponent.getCurrentMode() == Mode.RAIGO) {
            if (raigoProjectile != null
                    && raigoProjectile.isAlive()
                    && raigoProjectile.getLife() < raigoProjectile.getMaxLife()) {
                raigoProjectile.onBlockImpactEvent(raigoProjectile.blockPosition());
                raigoProjectile.remove();
            }
            raigoProjectile = null;
            cooldownComponent.startCooldown(entity, isThundering ? 700.0F : 1700.0F);
        } else {
            cooldownComponent.startCooldown(entity, 1400.0F);
        }
    }

    private void onAltModeChange(LivingEntity entity, IAbility ability, Mode mode) {
        if (mode == Mode.MAMARAGAN) {
            this.setDisplayName(MAMARAGAN_NAME);
            this.setDisplayIcon(MAMARAGAN_ICON);
        } else {
            this.setDisplayName(RAIGO_NAME);
            this.setDisplayIcon(RAIGO_ICON);
        }
    }

    private AbilityUseResult canUseEvent(LivingEntity entity, IAbility ability) {
        return entity.level.isThundering()
                ? AbilityUseResult.success()
                : AbilityUseResult.fail(
                        new TranslationTextComponent(ModI18n.ABILITY_MESSAGE_NEED_THUNDERSTORM));
    }

    private void setBoltProperties(
            LightningEntity bolt, float size, float damage, int timeAlive, int resetTime,
            boolean explodes, @Nullable Color color, float multiplier) {
        bolt.setBlocksAffectedLimit(30000);
        bolt.setAngle(160);
        bolt.setBranches(1);
        bolt.setSegments(1);
        bolt.setSize(size * multiplier);
        bolt.setBoxSizeDivision(0.225D);
        bolt.setLightningMovement(false);
        bolt.setExplosion(explodes ? (int) (10.0F * multiplier) : 0, true, 0.25F);
        if (color != null) bolt.setColor(color);
        bolt.setMaxLife(timeAlive);
        bolt.setDamage(damage * multiplier);
        bolt.setTargetTimeToReset(resetTime);
    }

    private RaigoProjectile createProjectile(LivingEntity entity) {
        return new RaigoProjectile(entity.level, entity, this);
    }

    public enum Mode {
        MAMARAGAN,
        RAIGO
    }
}
