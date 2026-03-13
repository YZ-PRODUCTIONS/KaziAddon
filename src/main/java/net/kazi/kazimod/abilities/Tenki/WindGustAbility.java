package net.kazi.kazimod.abilities.Tenki;

import net.kazi.kazimod.entities.projectiles.WindGustProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.SwingTriggerComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

public class WindGustAbility extends Ability {

    public enum WindMode { NORMAL, WIND_FURY }

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("kazimod", "wind_gust", new Pair[]{
            ImmutablePair.of("The user slashes the air, launching a gust of wind at the opponent.", (Object) null)
    });

    private static final TranslationTextComponent WIND_GUST_NAME =
            new TranslationTextComponent(WyRegistry.registerName("ability.kazimod.wind_gust", "Wind Gust"));
    private static final TranslationTextComponent WIND_FURY_NAME =
            new TranslationTextComponent(WyRegistry.registerName("ability.kazimod.wind_fury", "Wind Fury"));
    private static final ResourceLocation WIND_GUST_ICON =
            new ResourceLocation("kazimod", "textures/abilities/wind_gust.png");
    private static final ResourceLocation WIND_FURY_ICON =
            new ResourceLocation("kazimod", "textures/abilities/wind_fury.png");

    // Normal mode stats
    public static final float COOLDOWN       = 360.0F;
    private static final int  NORMAL_SHOTS   = 3;
    private static final int  SHOT_INTERVAL  = 10; // ticks between each shot

    // Wind Fury stats
    private static final float FURY_DURATION = 200.0F; // 5 seconds
    private static final float FURY_COOLDOWN = 700.0F;

    public static final AbilityCore<WindGustAbility> INSTANCE;

    // Components
    private final ProjectileComponent        projectileComponent   = new ProjectileComponent(this, this::createProjectile);
    private final AltModeComponent<WindMode> altModeComponent;
    private final ContinuousComponent        furyComponent         =
            (new ContinuousComponent(this, true))
                    .addStartEvent(this::onFuryStart)
                    .addEndEvent(this::onFuryEnd);
    private final SwingTriggerComponent      swingTriggerComponent =
            (new SwingTriggerComponent(this)).addSwingEvent(this::onFurySwing);

    // Normal mode burst state — tracked manually without RepeaterAbility2
    private int  pendingShots    = 0;
    private int  shotCooldown    = 0;
    private WindMode currentMode = WindMode.NORMAL;

    public WindGustAbility(AbilityCore<WindGustAbility> core) {
        super(core);
        this.altModeComponent = (new AltModeComponent<>(this, WindMode.class, WindMode.NORMAL, true))
                .addChangeModeEvent(this::onModeChange);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.projectileComponent,
                this.altModeComponent,
                this.furyComponent,
                this.swingTriggerComponent
        });
        this.addUseEvent(this::onUseEvent);
        this.addTickEvent(this::onTick);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.currentMode == WindMode.WIND_FURY) {
            // Wind Fury: toggle the 5-second swing window
            if (this.furyComponent.isContinuous()) {
                this.furyComponent.stopContinuity(entity);
            } else {
                this.furyComponent.startContinuity(entity, FURY_DURATION);
            }
        } else {
            // Normal mode: queue up 3 shots to fire on tick
            if (this.pendingShots == 0 && !this.cooldownComponent.isOnCooldown()) {
                this.pendingShots = NORMAL_SHOTS;
                this.shotCooldown = 0;
            }
        }
    }

    // Fires queued normal-mode shots one at a time with interval spacing
    private void onTick(LivingEntity entity, IAbility ability) {
        if (this.currentMode != WindMode.NORMAL) return;
        if (this.pendingShots <= 0) return;

        if (this.shotCooldown > 0) {
            this.shotCooldown--;
            return;
        }

        this.projectileComponent.shoot(entity, 3.25F, 1.0F);
        this.pendingShots--;
        this.shotCooldown = SHOT_INTERVAL;

        if (this.pendingShots <= 0) {
            this.cooldownComponent.startCooldown(entity, COOLDOWN);
        }
    }

    // ── Wind Fury events ──────────────────────────────────────────────────────
    private void onFuryStart(LivingEntity entity, IAbility ability) {
        // swingTriggerComponent handles shooting
    }

    private void onFuryEnd(LivingEntity entity, IAbility ability) {
        this.cooldownComponent.startCooldown(entity, FURY_COOLDOWN);
    }

    private void onFurySwing(LivingEntity entity, IAbility ability) {
        if (!this.furyComponent.isContinuous()) return;

        IAbilityData props = AbilityDataCapability.get(entity);
        GaleStormAbility galeStorm = (GaleStormAbility) props.getEquippedAbility(GaleStormAbility.INSTANCE);

        if (galeStorm == null || !galeStorm.isContinuous()) {
            entity.sendMessage(new StringTextComponent("Gale Storm must be active to use Wind Fury!"), Util.NIL_UUID);
            this.furyComponent.stopContinuity(entity);
            return;
        }

        this.projectileComponent.shoot(entity, 3.5F, 0.8F);
    }

    // ── Mode switch ───────────────────────────────────────────────────────────
    private void onModeChange(LivingEntity entity, IAbility ability, WindMode mode) {
        this.currentMode = mode;
        this.pendingShots = 0;
        this.shotCooldown = 0;
        if (this.furyComponent.isContinuous()) {
            this.furyComponent.stopContinuity(entity);
        }
        switch (mode) {
            case WIND_FURY:
                this.setDisplayName(WIND_FURY_NAME);
                this.setDisplayIcon(WIND_FURY_ICON);
                break;
            case NORMAL:
            default:
                this.setDisplayName(WIND_GUST_NAME);
                this.setDisplayIcon(WIND_GUST_ICON);
                break;
        }
    }

    public void switchWindFury(LivingEntity entity) {
        this.altModeComponent.setMode(entity, WindMode.WIND_FURY);
    }

    public void switchNormal(LivingEntity entity) {
        if (this.furyComponent.isContinuous()) {
            this.furyComponent.stopContinuity(entity);
        }
        this.altModeComponent.setMode(entity, WindMode.NORMAL);
    }

    private WindGustProjectile createProjectile(LivingEntity entity) {
        return new WindGustProjectile(entity.level, entity);
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Wind Gust", AbilityCategory.DEVIL_FRUITS, WindGustAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN)
                })
                .addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips())
                .setSourceHakiNature(SourceHakiNature.IMBUING)
                .setSourceElement(SourceElement.AIR)
                .setSourceType(new SourceType[]{SourceType.SLASH, SourceType.INDIRECT})
                .build();
    }
}