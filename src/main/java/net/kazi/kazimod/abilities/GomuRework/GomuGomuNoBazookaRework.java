package net.kazi.kazimod.abilities.GomuRework;

import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.abilities.gomu.GomuHelper;
import xyz.pixelatedw.mineminenomi.abilities.gomu.GomuHelper.Gears;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.gomu.GomuGomuNoBazookaProjectile;
import xyz.pixelatedw.mineminenomi.entities.projectiles.gomu.GomuGomuNoGrizzlyMagnumProjectile;
import xyz.pixelatedw.mineminenomi.entities.projectiles.gomu.GomuGomuNoJetBazookaProjectile;
import xyz.pixelatedw.mineminenomi.entities.projectiles.gomu.GomuGomuNoLeoBazookaProjectile;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

public class GomuGomuNoBazookaRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "gomu_gomu_no_bazooka", new Pair[]{ImmutablePair.of("Hits the enemy with both hands to launch them away.", (Object)null)});

    private static final TranslationTextComponent GOMU_GOMU_NO_BAZOOKA_NAME      = new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.gomu_gomu_no_bazooka",      "Gomu Gomu no Bazooka"));
    private static final TranslationTextComponent GOMU_GOMU_NO_JET_BAZOOKA_NAME  = new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.gomu_gomu_no_jet_bazooka",  "Gomu Gomu no Jet Bazooka"));
    private static final TranslationTextComponent GOMU_GOMU_NO_GRIZZLY_MAGNUM_NAME = new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.gomu_gomu_no_grizzly_magnum", "Gomu Gomu no Grizzly Magnum"));
    private static final TranslationTextComponent GOMU_GOMU_NO_LEO_BAZOOKA_NAME  = new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.gomu_gomu_no_leo_bazooka",  "Gomu Gomu no Leo Bazooka"));
    private static final TranslationTextComponent GOMU_GOMU_NO_DAWN_CYMBAL_NAME  = new TranslationTextComponent(WyRegistry.registerName("ability.kazimod.gomu_gomu_no_dawn_cymbal",        "Gomu Gomu no Dawn Cymbal"));

    private static final ResourceLocation GOMU_GOMU_NO_BAZOOKA_ICON        = new ResourceLocation("mineminenomi", "textures/abilities/gomu_gomu_no_bazooka.png");
    private static final ResourceLocation GOMU_GOMU_NO_JET_BAZOOKA_ICON    = new ResourceLocation("kazimod", "textures/abilities/gomu_gomu_no_jet_bazooka.png");
    private static final ResourceLocation GOMU_GOMU_NO_GRIZZLY_MAGNUM_ICON = new ResourceLocation("mineminenomi", "textures/abilities/gomu_gomu_no_grizzly_magnum.png");
    private static final ResourceLocation GOMU_GOMU_NO_LEO_BAZOOKA_ICON    = new ResourceLocation("mineminenomi", "textures/abilities/gomu_gomu_no_leo_bazooka.png");
    private static final ResourceLocation GOMU_GOMU_NO_DAWN_CYMBAL_ICON    = new ResourceLocation("kazimod",      "textures/abilities/gomu_gomu_no_dawn_cymbal.png");

    private static final int   NO_GEAR_COOLDOWN        = 200, NO_GEAR_CHARGE_TIME        = 40;
    private static final int   SECOND_GEAR_COOLDOWN    = 140, SECOND_GEAR_CHARGE_TIME    = 30;
    private static final int   THIRD_GEAR_COOLDOWN     = 300, THIRD_GEAR_CHARGE_TIME     = 60;
    private static final int   FOURTH_GEAR_COOLDOWN    = 240, FOURTH_GEAR_CHARGE_TIME    = 40;
    private static final int   DAWN_CYMBAL_COOLDOWN    = 400, DAWN_CYMBAL_CHARGE_TIME    = 40;
    private static final float DAWN_CYMBAL_GRAB_RADIUS   = 15.0F;
    private static final float DAWN_CYMBAL_GRAB_DURATION = 10.0F;
    private static final float DAWN_CYMBAL_DAMAGE        = 65.0F;
    private static final double DAWN_CYMBAL_LAUNCH_DISTANCE = 20.0;

    private static final IDescriptionLine NO_GEAR_NAME_DESC;
    private static final IDescriptionLine SECOND_GEAR_NAME_DESC;
    private static final IDescriptionLine THIRD_GEAR_NAME_DESC;
    private static final IDescriptionLine FOURTH_GEAR_NAME_DESC;
    private static final IDescriptionLine DAWN_CYMBAL_NAME_DESC;

    public static final AbilityCore<GomuGomuNoBazookaRework> INSTANCE;

    private final AltModeComponent<Gears> altModeComponent;
    private final ChargeComponent chargeComponent;
    private final ProjectileComponent projectileComponent;
    private final AnimationComponent animationComponent;
    private final ContinuousComponent continuousComponent;
    private final DealDamageComponent dealDamageComponent;

    private float cooldown;
    private int chargeTime;
    private float projectileSpeed;
    private float projectileSpread;
    private Gears currentGear      = Gears.NO_GEAR;
    private boolean dawnCymbalActive = false;
    private List<LivingEntity> grabbedTargets = null;

    public GomuGomuNoBazookaRework(AbilityCore<GomuGomuNoBazookaRework> core) {
        super(core);
        this.altModeComponent     = new AltModeComponent<>(this, Gears.class, Gears.NO_GEAR, true)
                .addChangeModeEvent(this::altModeChangeEvent);
        this.chargeComponent      = new ChargeComponent(this)
                .addStartEvent(this::startChargeEvent).addEndEvent(this::endChargeEvent);
        this.projectileComponent  = new ProjectileComponent(this, this::createProjectile);
        this.animationComponent   = new AnimationComponent(this);
        this.continuousComponent  = new ContinuousComponent(this)
                .addStartEvent(this::startGrabEvent)
                .addTickEvent(this::tickGrabEvent)
                .addEndEvent(this::endGrabEvent);
        this.dealDamageComponent  = new DealDamageComponent(this);
        this.cooldown             = 200.0F;
        this.chargeTime           = 40;
        this.projectileSpeed      = 2.0F;
        this.projectileSpread     = 1.0F;
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.altModeComponent, this.chargeComponent, this.projectileComponent,
                this.animationComponent, this.continuousComponent, this.dealDamageComponent
        });
        this.addUseEvent(this::useEvent);
    }

    // ── Use ───────────────────────────────────────────────────────────────────

    private void useEvent(LivingEntity entity, IAbility ability) {
        if (this.dawnCymbalActive) {
            IAbilityData props = AbilityDataCapability.get(entity);
            GomuGomuNoGigantRework gigant = (GomuGomuNoGigantRework) props.getEquippedAbility(GomuGomuNoGigantRework.INSTANCE);
            if (gigant == null || !gigant.isContinuous()) {
                if (entity instanceof PlayerEntity)
                    entity.sendMessage(new StringTextComponent("Dawn Cymbal can only be used while Gomu Gomu no Gigant is active!"), entity.getUUID());
                return;
            }
        }
        this.chargeComponent.startCharging(entity, (float) this.chargeTime);
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, ModAnimations.GOMU_BAZOOKA);
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);
        if (this.dawnCymbalActive) {
            this.continuousComponent.startContinuity(entity, DAWN_CYMBAL_GRAB_DURATION);
            return;
        }
        AbilityProjectileEntity p1 = this.projectileComponent.getNewProjectile(entity);
        AbilityProjectileEntity p2 = this.projectileComponent.getNewProjectile(entity);
        Vector3d dirVec = Vector3d.ZERO;
        Direction dir = Direction.fromYRot(entity.yRot);
        dirVec = dirVec.add(Math.abs(dir.getNormal().getX()), Math.abs(dir.getNormal().getY()), Math.abs(dir.getNormal().getZ()));
        dirVec = dirVec.multiply(this.projectileSpread, 1.0F, this.projectileSpread);
        p1.moveTo(entity.getX() + dirVec.z, entity.getEyeY(), entity.getZ() + dirVec.x, 0, 0);
        p2.moveTo(entity.getX() - dirVec.z, entity.getEyeY(), entity.getZ() - dirVec.x, 0, 0);
        this.projectileComponent.shoot(p1, entity, this.projectileSpeed, 0);
        this.projectileComponent.shoot(p2, entity, this.projectileSpeed, 0);
        entity.swing(Hand.MAIN_HAND, true);
        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.GOMU_SFX.get(), SoundCategory.PLAYERS, 2.0F, 0.75F);
        this.cooldownComponent.startCooldown(entity, this.cooldown);
    }

    // ── Dawn Cymbal grab phase ─────────────────────────────────────────────────

    private void startGrabEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            this.grabbedTargets = WyHelper.getNearbyLiving(entity.position(), entity.level,
                    DAWN_CYMBAL_GRAB_RADIUS, DAWN_CYMBAL_GRAB_RADIUS, DAWN_CYMBAL_GRAB_RADIUS, null);
            this.grabbedTargets.remove(entity);
            for (LivingEntity t : this.grabbedTargets) {
                t.addEffect(new EffectInstance((Effect) ModEffects.GRABBED.get(), (int) DAWN_CYMBAL_GRAB_DURATION + 2, 3));
                t.addEffect(new EffectInstance((Effect) ModEffects.ANTI_KNOCKBACK.get(), (int) DAWN_CYMBAL_GRAB_DURATION + 2));
            }
            entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                    (SoundEvent) ModSounds.GOMU_SFX.get(), SoundCategory.PLAYERS, 3.0F, 0.6F);
        }
    }

    private void tickGrabEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide && this.grabbedTargets != null) {
            for (LivingEntity t : this.grabbedTargets) {
                Vector3d toEntity = entity.position().subtract(t.position()).normalize().scale(1.5);
                AbilityHelper.setDeltaMovement(t, toEntity.x, toEntity.y, toEntity.z);
            }
        }
    }

    private void endGrabEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide && this.grabbedTargets != null) {
            Vector3d launchTarget = entity.position().add(entity.getLookAngle().normalize().scale(DAWN_CYMBAL_LAUNCH_DISTANCE));
            for (LivingEntity t : this.grabbedTargets) {
                this.dealDamageComponent.hurtTarget(entity, t, DAWN_CYMBAL_DAMAGE);
                t.addEffect(new EffectInstance((Effect) ModEffects.DIZZY.get(), 100, 0));
                Vector3d dir = launchTarget.subtract(t.position()).normalize().scale(2.5);
                AbilityHelper.setDeltaMovement(t, dir.x, 0.8, dir.z);
            }
            entity.swing(Hand.MAIN_HAND, true);
            entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                    (SoundEvent) ModSounds.GOMU_SFX.get(), SoundCategory.PLAYERS, 3.0F, 0.5F);
            this.grabbedTargets = null;
        }
        this.cooldownComponent.startCooldown(entity, DAWN_CYMBAL_COOLDOWN);
    }

    // ── Projectile factory ────────────────────────────────────────────────────

    private AbilityProjectileEntity createProjectile(LivingEntity entity) {
        IAbilityData props = AbilityDataCapability.get(entity);
        AbilityProjectileEntity projectile;
        this.projectileSpeed  = 2.0F;
        this.projectileSpread = 1.0F;
        if (GomuHelper.hasGearFourthActive(props)) {
            projectile = new GomuGomuNoLeoBazookaProjectile(entity.level, entity, this);
            this.projectileSpeed = 3.0F; this.projectileSpread = 2.5F;
        } else if (GomuHelper.hasGearThirdActive(props)) {
            projectile = new GomuGomuNoGrizzlyMagnumProjectile(entity.level, entity, this);
            this.projectileSpeed = 1.8F; this.projectileSpread = 2.5F;
        } else if (GomuHelper.hasGearSecondActive(props) || hasGearSecondReworkActive(props)) {
            // FIX: also check GearSecondRework — GomuHelper only checks vanilla GearSecondAbility
            projectile = new GomuGomuNoJetBazookaProjectile(entity.level, entity, this);
            this.projectileSpeed = 3.0F;
        } else {
            projectile = new GomuGomuNoBazookaProjectile(entity.level, entity, this);
        }
        return projectile;
    }

    // ── Alt mode ──────────────────────────────────────────────────────────────

    private void altModeChangeEvent(LivingEntity entity, IAbility ability, Gears mode) {
        this.currentGear = mode;
        switch (mode) {
            case GEAR_2:
                this.dawnCymbalActive = false;
                this.setDisplayIcon(GOMU_GOMU_NO_JET_BAZOOKA_ICON);
                this.setDisplayName(GOMU_GOMU_NO_JET_BAZOOKA_NAME);
                this.cooldown = 140.0F; this.chargeTime = 30; break;
            case GEAR_3:
                this.dawnCymbalActive = false;
                this.setDisplayIcon(GOMU_GOMU_NO_GRIZZLY_MAGNUM_ICON);
                this.setDisplayName(GOMU_GOMU_NO_GRIZZLY_MAGNUM_NAME);
                this.cooldown = 300.0F; this.chargeTime = 60; break;
            case GEAR_4:
                this.dawnCymbalActive = false;
                this.setDisplayIcon(GOMU_GOMU_NO_LEO_BAZOOKA_ICON);
                this.setDisplayName(GOMU_GOMU_NO_LEO_BAZOOKA_NAME);
                this.cooldown = 240.0F; this.chargeTime = 40; break;
            case NO_GEAR: default:
                if (this.dawnCymbalActive) {
                    this.cooldown = DAWN_CYMBAL_COOLDOWN; this.chargeTime = DAWN_CYMBAL_CHARGE_TIME;
                } else {
                    this.setDisplayIcon(GOMU_GOMU_NO_BAZOOKA_ICON);
                    this.setDisplayName(GOMU_GOMU_NO_BAZOOKA_NAME);
                    this.cooldown = 200.0F; this.chargeTime = 40;
                }
        }
    }

    // ── Gear switch helpers ───────────────────────────────────────────────────

    public void switchNoGear(LivingEntity entity) {
        this.dawnCymbalActive = false;
        this.setDisplayIcon(GOMU_GOMU_NO_BAZOOKA_ICON);
        this.setDisplayName(GOMU_GOMU_NO_BAZOOKA_NAME);
        this.cooldown = 200.0F; this.chargeTime = 40;
        this.altModeComponent.setMode(entity, Gears.NO_GEAR);
    }

    public void switchSecondGear(LivingEntity entity) {
        this.dawnCymbalActive = false;
        this.altModeComponent.setMode(entity, Gears.GEAR_2);
    }

    public void switchThirdGear(LivingEntity entity) {
        this.dawnCymbalActive = false;
        this.altModeComponent.setMode(entity, Gears.GEAR_3);
    }

    public void switchFourthGear(LivingEntity entity) {
        this.dawnCymbalActive = false;
        this.altModeComponent.setMode(entity, Gears.GEAR_4);
    }

    public void switchDawnCymbal(LivingEntity entity) {
        this.dawnCymbalActive = true;
        this.setDisplayIcon(GOMU_GOMU_NO_DAWN_CYMBAL_ICON);
        this.setDisplayName(GOMU_GOMU_NO_DAWN_CYMBAL_NAME);
        this.cooldown = DAWN_CYMBAL_COOLDOWN; this.chargeTime = DAWN_CYMBAL_CHARGE_TIME;
        this.altModeComponent.setMode(entity, Gears.NO_GEAR);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static boolean hasGearSecondReworkActive(IAbilityData props) {
        GearSecondRework g2 = (GearSecondRework) props.getEquippedAbility(GearSecondRework.INSTANCE);
        return g2 != null && g2.isContinuous();
    }

    static {
        NO_GEAR_NAME_DESC     = IDescriptionLine.of(AbilityHelper.mentionText(GOMU_GOMU_NO_BAZOOKA_NAME));
        SECOND_GEAR_NAME_DESC = IDescriptionLine.of(AbilityHelper.mentionText(GOMU_GOMU_NO_JET_BAZOOKA_NAME));
        THIRD_GEAR_NAME_DESC  = IDescriptionLine.of(AbilityHelper.mentionText(GOMU_GOMU_NO_GRIZZLY_MAGNUM_NAME));
        FOURTH_GEAR_NAME_DESC = IDescriptionLine.of(AbilityHelper.mentionText(GOMU_GOMU_NO_LEO_BAZOOKA_NAME));
        DAWN_CYMBAL_NAME_DESC = IDescriptionLine.of(AbilityHelper.mentionText(GOMU_GOMU_NO_DAWN_CYMBAL_NAME));
        INSTANCE = (new AbilityCore.Builder("Gomu Gomu no Bazooka", AbilityCategory.DEVIL_FRUITS, GomuGomuNoBazookaRework::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, NO_GEAR_NAME_DESC, AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(200.0F), ChargeComponent.getTooltip(40.0F)})
                .addAdvancedDescriptionLine(new IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, SECOND_GEAR_NAME_DESC, GomuHelper.SECOND_GEAR_REQ, AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(140.0F), ChargeComponent.getTooltip(30.0F)})
                .addAdvancedDescriptionLine(new IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, THIRD_GEAR_NAME_DESC, GomuHelper.THIRD_GEAR_REQ, AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(300.0F), ChargeComponent.getTooltip(60.0F)})
                .addAdvancedDescriptionLine(new IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, FOURTH_GEAR_NAME_DESC, GomuHelper.FOURTH_GEAR_REQ, AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(240.0F), ChargeComponent.getTooltip(40.0F)})
                .addAdvancedDescriptionLine(new IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, DAWN_CYMBAL_NAME_DESC, AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip((float) DAWN_CYMBAL_COOLDOWN), ChargeComponent.getTooltip((float) DAWN_CYMBAL_CHARGE_TIME)})
                .setSourceHakiNature(SourceHakiNature.HARDENING)
                .setSourceType(new SourceType[]{SourceType.FIST})
                .build();
    }
}