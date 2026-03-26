package net.kazi.kazimod.abilities.GomuRework;

import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.init.CartParticleEffects;
import net.kazi.kazimod.entities.projectiles.GomuGomuNoBajrangGunReworkProjectile;
import net.kazi.kazimod.entities.projectiles.GomuGomuNoRedRocProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine.IDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.HakiHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;
import xyz.pixelatedw.mineminenomi.abilities.gomu.GomuHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;

public class GomuGomuNoRedRocAbility extends Ability {

    public enum RedRocMode {
        RED_ROC,
        BAJRANG_GUN
    }

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("kazimod", "gomu_gomu_no_red_roc", new Pair[]{ImmutablePair.of("The user winds up, stretches and grows their arm to punch the opponent with powerful haki and flames.", (Object)null)});
    private static final TranslationTextComponent GOMU_GOMU_NO_RED_ROC_NAME = new TranslationTextComponent(WyRegistry.registerName("ability.kazimod.gomu_gomu_no_red_roc", "Gomu Gomu no Red Roc"));
    private static final TranslationTextComponent GOMU_GOMU_NO_BAJRANG_GUN_NAME = new TranslationTextComponent(WyRegistry.registerName("ability.kazimod.gomu_gomu_no_bajrang_gun", "Gomu Gomu no Bajrang Gun"));
    private static final ResourceLocation GOMU_GOMU_NO_RED_ROC_ICON = new ResourceLocation("kazimod", "textures/abilities/gomu_gomu_no_red_roc.png");
    private static final ResourceLocation GOMU_GOMU_NO_BAJRANG_GUN_ICON = new ResourceLocation("kazimod", "textures/abilities/gomu_gomu_no_bajrang_gun.png");

    private static final int RED_ROC_COOLDOWN = 800;
    private static final int BAJRANG_GUN_COOLDOWN = 12000;

    // Red Roc charge/dash constants
    private static final int RED_ROC_CHARGE_TIME = 30;
    private static final float DASH_DURATION = 10.0F;
    private static final float DASH_SPEED = 1.0F;

    // Bajrang Gun charge: 8 seconds = 160 ticks
    private static final int BAJRANG_GUN_CHARGE_TIME = 240;

    private static final float PROJECTILE_SPAWN_OFFSET = 3.0F;

    // Projectile speeds
    private static final float RED_ROC_PROJECTILE_SPEED = 2.0F;
    private static final float BAJRANG_GUN_PROJECTILE_SPEED = 0.01F; // Half of Red Roc

    // Vertical launch heights on charge start
    private static final float RED_ROC_VERTICAL_BOOST = 3.5F;
    private static final float BAJRANG_GUN_VERTICAL_BOOST = 5.0F; // 2x Red Roc

    private static final IDescriptionLine RED_ROC_NAME_DESC;
    private static final IDescriptionLine BAJRANG_GUN_NAME_DESC;
    public static final AbilityCore<GomuGomuNoRedRocAbility> INSTANCE;

    // Tracks the current mode since AltModeComponent has no getMode() method
    private RedRocMode currentMode = RedRocMode.RED_ROC;

    // Alt mode to toggle between Red Roc and Bajrang Gun
    private final AltModeComponent<RedRocMode> altModeComponent =
            (new AltModeComponent<>(this, RedRocMode.class, RedRocMode.RED_ROC, true))
                    .addChangeModeEvent(this::altModeChangeEvent);

    private final ChargeComponent chargeComponent =
            new ChargeComponent(this)
                    .addStartEvent(this::startChargeEvent)
                    .addTickEvent(this::tickChargeEvent)
                    .addEndEvent(this::endChargeEvent);

    private final ContinuousComponent continuousComponent =
            new ContinuousComponent(this)
                    .addStartEvent(this::startDashEvent)
                    .addTickEvent(this::tickDashEvent)
                    .addEndEvent(this::endDashEvent);

    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final Interval particleInterval = new Interval(2);

    public GomuGomuNoRedRocAbility(AbilityCore<GomuGomuNoRedRocAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.altModeComponent,
                this.chargeComponent,
                this.continuousComponent,
                this.projectileComponent,
                this.animationComponent
        });
        this.addUseEvent(this::useEvent);
    }

    /* ================= ALT MODE ================= */

    private void altModeChangeEvent(LivingEntity entity, IAbility ability, RedRocMode mode) {
        this.currentMode = mode;
        switch (mode) {
            case BAJRANG_GUN:
                this.setDisplayName(GOMU_GOMU_NO_BAJRANG_GUN_NAME);
                this.setDisplayIcon(GOMU_GOMU_NO_BAJRANG_GUN_ICON);
                break;
            case RED_ROC:
            default:
                this.setDisplayName(GOMU_GOMU_NO_RED_ROC_NAME);
                this.setDisplayIcon(GOMU_GOMU_NO_RED_ROC_ICON);
                break;
        }
    }

    public void switchRedRoc(LivingEntity entity) {
        this.altModeComponent.setMode(entity, RedRocMode.RED_ROC);
    }

    public void switchBajrangGun(LivingEntity entity) {
        this.altModeComponent.setMode(entity, RedRocMode.BAJRANG_GUN);
    }

    /* ================= USE ================= */
    private void useEvent(LivingEntity entity, IAbility ability) {
        RedRocMode mode = this.currentMode;

        if (mode == RedRocMode.BAJRANG_GUN) {
            // Bajrang Gun requires Hao Infusion
            if (!HakiHelper.hasInfusionActive(entity) && entity instanceof PlayerEntity) {
                entity.sendMessage(new StringTextComponent("You need to activate Hao Infusion to use this move!"), entity.getUUID());
                return;
            }

            // Bajrang Gun requires Gear Fifth to be active
            IAbilityData props = AbilityDataCapability.get(entity);
            GearFifthRework gearFifth = (GearFifthRework) props.getEquippedAbility(GearFifthRework.INSTANCE);
            boolean gearFifthActive = gearFifth != null && gearFifth.getContinuousComponent() != null && gearFifth.getContinuousComponent().isContinuous();
            if (!gearFifthActive) {
                if (entity instanceof PlayerEntity) {
                    entity.sendMessage(new StringTextComponent("Bajrang Gun can only be used during Gear Fifth!"), entity.getUUID());
                }
                return;
            }
        } else {
            // Red Roc requires Hao Infusion and no active gear
            if (!HakiHelper.hasInfusionActive(entity) && entity instanceof PlayerEntity) {
                entity.sendMessage(new StringTextComponent("You need to activate Hao Infusion to use this move!"), entity.getUUID());
                return;
            }

            IAbilityData props = AbilityDataCapability.get(entity);
            if (GomuHelper.hasGearSecondActive(props) || GomuHelper.hasGearThirdActive(props) || GomuHelper.hasGearFourthActive(props)) {
                if (entity instanceof PlayerEntity) {
                    entity.sendMessage(new StringTextComponent("Red Roc cannot be used while a Gear is active!"), entity.getUUID());
                }
                return;
            }
        }

        if (!this.chargeComponent.isCharging() && !this.continuousComponent.isContinuous()) {
            int chargeTime = (mode == RedRocMode.BAJRANG_GUN) ? BAJRANG_GUN_CHARGE_TIME : RED_ROC_CHARGE_TIME;
            this.chargeComponent.startCharging(entity, chargeTime);
        }
    }

    /* ================= CHARGE ================= */

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        float verticalBoost = (this.currentMode == RedRocMode.BAJRANG_GUN) ? BAJRANG_GUN_VERTICAL_BOOST : RED_ROC_VERTICAL_BOOST;
        AbilityHelper.setDeltaMovement(entity, entity.getDeltaMovement().x, verticalBoost, entity.getDeltaMovement().z);
        this.animationComponent.start(entity, ModAnimations.CHARGE_PUNCH);
        entity.addEffect(new EffectInstance((Effect) ModEffects.DIZZY.get(), 30, 0));

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.HAKI_RELEASE_SFX.get(),
                SoundCategory.PLAYERS, 2.5F, 0.5F + entity.getRandom().nextFloat());
    }

    private void tickChargeEvent(LivingEntity entity, IAbility ability) {
        AbilityHelper.slowEntityFall(entity);

        RayTraceResult mop = WyHelper.rayTraceBlocksAndEntities(entity, -0.5);
        double i = mop.getLocation().x;
        double j = mop.getLocation().y;
        double k = mop.getLocation().z;

        if (this.chargeComponent.getChargeTime() > 15.0F && this.currentMode != RedRocMode.BAJRANG_GUN) {
            WyHelper.spawnParticleEffect((ParticleEffect) CartParticleEffects.GALAXY_IMPACT.get(), entity, i, j, k);
        }

        if (this.chargeComponent.getChargeTime() % 10.0F == 0.0F) {
            entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                    (SoundEvent) ModSounds.HAKI_RELEASE_SFX.get(),
                    SoundCategory.PLAYERS, 3.0F, 0.5F + entity.getRandom().nextFloat());
        }
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);

        RedRocMode mode = this.currentMode;
        if (mode == RedRocMode.BAJRANG_GUN) {
            // Bajrang Gun skips the dash — fire immediately
            fireProjectile(entity);
        } else {
            // Red Roc does the dash before firing
            this.continuousComponent.startContinuity(entity, DASH_DURATION);
        }
    }

    /* ================= DASH (Red Roc only) ================= */

    private void startDashEvent(LivingEntity entity, IAbility ability) {
        this.particleInterval.restartIntervalToZero();
        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.DASH_ABILITY_SWOOSH_SFX.get(),
                SoundCategory.PLAYERS, 2.0F, 1.0F);
    }

    private void tickDashEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            Vector3d dir = entity.getLookAngle().normalize().scale(DASH_SPEED);
            AbilityHelper.setDeltaMovement(entity, dir);

            if (this.particleInterval.canTick()) {
                RayTraceResult mop = WyHelper.rayTraceBlocksAndEntities(entity, -0.5);
                double i = mop.getLocation().x;
                double j = mop.getLocation().y;
                double k = mop.getLocation().z;
                WyHelper.spawnParticleEffect((ParticleEffect) CartParticleEffects.GALAXY_IMPACT.get(), entity, i, j, k);
            }
        }
    }

    private void endDashEvent(LivingEntity entity, IAbility ability) {
        fireProjectile(entity);
    }

    /* ================= SHARED FIRE LOGIC ================= */

    private void fireProjectile(LivingEntity entity) {
        AbilityProjectileEntity projectile = this.createProjectile(entity);
        float speed = (this.currentMode == RedRocMode.BAJRANG_GUN) ? BAJRANG_GUN_PROJECTILE_SPEED : RED_ROC_PROJECTILE_SPEED;
        this.projectileComponent.shoot(projectile, entity, speed, 0.0F);

        Vector3d dir = projectile.getDeltaMovement().normalize();
        projectile.setPos(
                projectile.getX() + dir.x * PROJECTILE_SPAWN_OFFSET,
                projectile.getY() + dir.y * PROJECTILE_SPAWN_OFFSET,
                projectile.getZ() + dir.z * PROJECTILE_SPAWN_OFFSET
        );

        entity.swing(Hand.MAIN_HAND, true);
        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.GOMU_SFX.get(),
                SoundCategory.PLAYERS, 2.0F, 1.0F);

        int cooldown = (this.currentMode == RedRocMode.BAJRANG_GUN) ? BAJRANG_GUN_COOLDOWN : RED_ROC_COOLDOWN;
        this.cooldownComponent.startCooldown(entity, (float) cooldown);
    }

    /* ================= PROJECTILE ================= */

    private AbilityProjectileEntity createProjectile(LivingEntity entity) {
        if (this.currentMode == RedRocMode.BAJRANG_GUN) {
            return new GomuGomuNoBajrangGunReworkProjectile(entity.level, entity);
        }
        return new GomuGomuNoRedRocProjectile(entity.level, entity);
    }

    /* ================= HELPERS ================= */

    // Exposed so GearFifthRework can switch the alt mode when Gear Fifth activates/ends
    public AltModeComponent<RedRocMode> getAltModeComponent() {
        return this.altModeComponent;
    }

    static {
        RED_ROC_NAME_DESC = IDescriptionLine.of(AbilityHelper.mentionText(GOMU_GOMU_NO_RED_ROC_NAME));
        BAJRANG_GUN_NAME_DESC = IDescriptionLine.of(AbilityHelper.mentionText(GOMU_GOMU_NO_BAJRANG_GUN_NAME));
        INSTANCE = (new AbilityCore.Builder("Gomu Gomu no Red Roc", AbilityCategory.DEVIL_FRUITS, GomuGomuNoRedRocAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        RED_ROC_NAME_DESC,
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip((float) RED_ROC_COOLDOWN)
                })
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        BAJRANG_GUN_NAME_DESC,
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip((float) BAJRANG_GUN_COOLDOWN)
                })
                .setSourceHakiNature(SourceHakiNature.HARDENING)
                .setSourceType(new SourceType[]{SourceType.FIST})
                .build();
    }
}