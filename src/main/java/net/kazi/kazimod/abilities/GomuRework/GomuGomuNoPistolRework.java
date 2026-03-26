package net.kazi.kazimod.abilities.GomuRework;

import net.kazi.kazimod.entities.projectiles.GomuGomuNoStarGunProjectile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.abilities.gomu.GomuHelper;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.gomu.GomuGomuNoElephantGunProjectile;
import xyz.pixelatedw.mineminenomi.entities.projectiles.gomu.GomuGomuNoJetPistolProjectile;
import xyz.pixelatedw.mineminenomi.entities.projectiles.gomu.GomuGomuNoKingKongGunProjectile;
import xyz.pixelatedw.mineminenomi.entities.projectiles.gomu.GomuGomuNoPistolProjectile;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

import java.util.List;
import java.util.Optional;

public class GomuGomuNoPistolRework extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "gomu_gomu_no_pistol", new Pair[]{ImmutablePair.of("The user stretches their arm to punch the opponent.", (Object) null)});

    private static final TranslationTextComponent GOMU_GOMU_NO_PISTOL_NAME        = new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.gomu_gomu_no_pistol",        "Gomu Gomu no Pistol"));
    private static final TranslationTextComponent GOMU_GOMU_NO_JET_PISTOL_NAME    = new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.gomu_gomu_no_jet_pistol",    "Gomu Gomu no Jet Pistol"));
    private static final TranslationTextComponent GOMU_GOMU_NO_ELEPHANT_GUN_NAME  = new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.gomu_gomu_no_elephant_gun",  "Gomu Gomu no Elephant Gun"));
    private static final TranslationTextComponent GOMU_GOMU_NO_KING_KONG_GUN_NAME = new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.gomu_gomu_no_king_kong_gun", "Gomu Gomu no King Kong Gun"));
    private static final TranslationTextComponent GOMU_GOMU_NO_STAR_GUN_NAME      = new TranslationTextComponent(WyRegistry.registerName("ability.kazimod.gomu_gomu_no_star_gun",           "Gomu Gomu no Star Gun"));

    private static final ResourceLocation GOMU_GOMU_NO_PISTOL_ICON        = new ResourceLocation("mineminenomi", "textures/abilities/gomu_gomu_no_pistol.png");
    private static final ResourceLocation GOMU_GOMU_NO_JET_PISTOL_ICON    = new ResourceLocation("kazimod", "textures/abilities/gomu_gomu_no_jet_pistol.png");
    private static final ResourceLocation GOMU_GOMU_NO_ELEPHANT_GUN_ICON  = new ResourceLocation("mineminenomi", "textures/abilities/gomu_gomu_no_elephant_gun.png");
    private static final ResourceLocation GOMU_GOMU_NO_KING_KONG_GUN_ICON = new ResourceLocation("mineminenomi", "textures/abilities/gomu_gomu_no_king_kong_gun.png");
    private static final ResourceLocation GOMU_GOMU_NO_STAR_GUN_ICON      = new ResourceLocation("kazimod", "textures/abilities/gomu_gomu_no_star_gun.png");

    private static final int   NO_GEAR_COOLDOWN      = 30;
    private static final int   SECOND_GEAR_COOLDOWN  = 20;
    private static final int   THIRD_GEAR_COOLDOWN   = 120;
    private static final int   FOURTH_GEAR_COOLDOWN  = 80;
    private static final int   STAR_GUN_COOLDOWN     = 200;
    private static final float STAR_GUN_CHARGE_TICKS = 7.0F;
    private static final float STAR_GUN_SPEED        = 3.5F;
    private static final double LOCK_ON_RANGE        = 100.0;

    private static final IDescriptionLine NO_GEAR_NAME_DESC;
    private static final IDescriptionLine SECOND_GEAR_NAME_DESC;
    private static final IDescriptionLine THIRD_GEAR_NAME_DESC;
    private static final IDescriptionLine FOURTH_GEAR_NAME_DESC;
    private static final IDescriptionLine STAR_GUN_NAME_DESC;

    public static final AbilityCore<GomuGomuNoPistolRework> INSTANCE;

    public enum PistolMode { NO_GEAR, GEAR_2, GEAR_3, GEAR_4, STAR_GUN }

    private final ProjectileComponent projectileComponent  = new ProjectileComponent(this, this::createProjectile);
    private final AltModeComponent<PistolMode> altModeComponent;
    private final AnimationComponent animationComponent    = new AnimationComponent(this);
    private final ChargeComponent chargeComponent          = new ChargeComponent(this)
            .addStartEvent(this::startChargeEvent)
            .addTickEvent(this::duringChargeEvent)
            .addEndEvent(this::endChargeEvent);

    private float   speed;
    private float   cooldown;
    private boolean isStarGunMode = false;
    private LivingEntity lockedTarget = null;

    public GomuGomuNoPistolRework(AbilityCore<GomuGomuNoPistolRework> core) {
        super(core);
        this.altModeComponent = new AltModeComponent<>(this, PistolMode.class, PistolMode.NO_GEAR, true)
                .addChangeModeEvent(this::altModeChangeEvent);
        this.speed    = 2.0F;
        this.cooldown = NO_GEAR_COOLDOWN;
        this.isNew    = true;
        this.addComponents(new AbilityComponent[]{
                this.projectileComponent, this.altModeComponent,
                this.animationComponent, this.chargeComponent
        });
        this.addUseEvent(this::useEvent);
    }

    // ── Use ───────────────────────────────────────────────────────────────────

    private void useEvent(LivingEntity entity, IAbility ability) {
        if (this.isStarGunMode) {
            if (!isGearFifthActive(entity)) {
                entity.sendMessage(new StringTextComponent("Gear Fifth must be active to use Star Gun!"), entity.getUUID());
                return;
            }
            this.lockedTarget = findLookTarget(entity);
            this.chargeComponent.startCharging(entity, STAR_GUN_CHARGE_TICKS);
        } else {
            AbilityProjectileEntity projectile = this.createProjectile(entity);
            this.projectileComponent.shoot(projectile, entity, this.speed, 0.0F);
            entity.swing(Hand.MAIN_HAND, true);
            entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                    (SoundEvent) ModSounds.GOMU_SFX.get(), SoundCategory.PLAYERS, 2.0F, 1.0F);
            this.cooldownComponent.startCooldown(entity, this.cooldown);
        }
    }

    // ── Star Gun charge ───────────────────────────────────────────────────────

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, ModAnimations.BODY_ROTATION_WIDE_ARMS);
        if (this.lockedTarget != null && this.lockedTarget.isAlive()) forceLookAt(entity, this.lockedTarget);
    }

    private void duringChargeEvent(LivingEntity entity, IAbility ability) {
        AbilityHelper.setDeltaMovement(entity, 0.0D, entity.getDeltaMovement().y, 0.0D);
        if (this.lockedTarget != null) {
            if (this.lockedTarget.isAlive()) forceLookAt(entity, this.lockedTarget);
            else this.lockedTarget = null;
        }
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        if (this.lockedTarget != null && this.lockedTarget.isAlive()) forceLookAt(entity, this.lockedTarget);
        this.lockedTarget = null;
        this.animationComponent.stop(entity);
        GomuGomuNoStarGunProjectile projectile = new GomuGomuNoStarGunProjectile(entity.level, entity);
        projectile.setLaunched(true);
        this.projectileComponent.shoot(projectile, entity, STAR_GUN_SPEED, 0.0F);
        entity.swing(Hand.MAIN_HAND, true);
        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.GOMU_SFX.get(), SoundCategory.PLAYERS, 2.0F, 1.0F);
        this.cooldownComponent.startCooldown(entity, STAR_GUN_COOLDOWN);
    }

    // ── Projectile factory ────────────────────────────────────────────────────

    private AbilityProjectileEntity createProjectile(LivingEntity entity) {
        IAbilityData props = AbilityDataCapability.get(entity);
        AbilityProjectileEntity projectile;
        this.speed = 2.0F;
        if (GomuHelper.hasGearFourthActive(props)) {
            projectile = new GomuGomuNoKingKongGunProjectile(entity.level, entity, this);
            this.speed = 1.8F;
        } else if (GomuHelper.hasGearThirdActive(props)) {
            projectile = new GomuGomuNoElephantGunProjectile(entity.level, entity, this);
            this.speed = 1.8F;
        } else if (GomuHelper.hasGearSecondActive(props) || hasGearSecondReworkActive(props)) {
            // FIX: also check GearSecondRework — GomuHelper only checks the vanilla GearSecondAbility
            projectile = new GomuGomuNoJetPistolProjectile(entity.level, entity, this);
            this.speed = 2.5F;
        } else {
            projectile = new GomuGomuNoPistolProjectile(entity.level, entity);
        }
        return projectile;
    }

    // ── Alt mode ──────────────────────────────────────────────────────────────

    private void altModeChangeEvent(LivingEntity entity, IAbility ability, PistolMode mode) {
        this.isStarGunMode = false;
        switch (mode) {
            case GEAR_2:
                this.setDisplayIcon(GOMU_GOMU_NO_JET_PISTOL_ICON);
                this.setDisplayName(GOMU_GOMU_NO_JET_PISTOL_NAME);
                this.cooldown = SECOND_GEAR_COOLDOWN;
                break;
            case GEAR_3:
                this.setDisplayIcon(GOMU_GOMU_NO_ELEPHANT_GUN_ICON);
                this.setDisplayName(GOMU_GOMU_NO_ELEPHANT_GUN_NAME);
                this.cooldown = THIRD_GEAR_COOLDOWN;
                break;
            case GEAR_4:
                this.setDisplayIcon(GOMU_GOMU_NO_KING_KONG_GUN_ICON);
                this.setDisplayName(GOMU_GOMU_NO_KING_KONG_GUN_NAME);
                this.cooldown = FOURTH_GEAR_COOLDOWN;
                break;
            case STAR_GUN:
                this.setDisplayIcon(GOMU_GOMU_NO_STAR_GUN_ICON);
                this.setDisplayName(GOMU_GOMU_NO_STAR_GUN_NAME);
                this.cooldown = STAR_GUN_COOLDOWN;
                this.isStarGunMode = true;
                break;
            case NO_GEAR:
            default:
                this.setDisplayIcon(GOMU_GOMU_NO_PISTOL_ICON);
                this.setDisplayName(GOMU_GOMU_NO_PISTOL_NAME);
                this.cooldown = NO_GEAR_COOLDOWN;
                break;
        }
    }

    // ── Gear switch helpers ───────────────────────────────────────────────────

    public void switchNoGear(LivingEntity entity)     { this.altModeComponent.setMode(entity, PistolMode.NO_GEAR);  }
    public void switchSecondGear(LivingEntity entity) { this.altModeComponent.setMode(entity, PistolMode.GEAR_2);   }
    public void switchThirdGear(LivingEntity entity)  { this.altModeComponent.setMode(entity, PistolMode.GEAR_3);   }
    public void switchFourthGear(LivingEntity entity) { this.altModeComponent.setMode(entity, PistolMode.GEAR_4);   }
    public void switchFifthGear(LivingEntity entity)  { this.altModeComponent.setMode(entity, PistolMode.STAR_GUN); }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns true if GearSecondRework is equipped and its continuous component is active.
     * GomuHelper.hasGearSecondActive only checks the vanilla GearSecondAbility so this
     * covers the rework case.
     */
    private static boolean hasGearSecondReworkActive(IAbilityData props) {
        GearSecondRework g2 = (GearSecondRework) props.getEquippedAbility(GearSecondRework.INSTANCE);
        if (g2 == null) return false;
        return g2.isContinuous();
    }

    private static boolean isGearFifthActive(LivingEntity entity) {
        IAbilityData props = AbilityDataCapability.get(entity);
        GearFifthRework g5 = (GearFifthRework) props.getEquippedAbility(GearFifthRework.INSTANCE);
        return g5 != null && g5.getContinuousComponent().isContinuous();
    }

    private LivingEntity findLookTarget(LivingEntity user) {
        Vector3d eyePos  = user.getEyePosition(1.0F);
        Vector3d lookVec = user.getViewVector(1.0F);
        Vector3d endPos  = eyePos.add(lookVec.scale(LOCK_ON_RANGE));
        AxisAlignedBB searchBox = new AxisAlignedBB(eyePos.x, eyePos.y, eyePos.z, endPos.x, endPos.y, endPos.z).inflate(5.0D);
        List<Entity> candidates = user.level.getEntities(user, searchBox,
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator());
        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (Entity candidate : candidates) {
            Optional<Vector3d> hit = candidate.getBoundingBox().inflate(1.5D).clip(eyePos, endPos);
            if (hit.isPresent()) {
                double dist = eyePos.distanceTo(hit.get());
                if (dist < closestDist) { closestDist = dist; closest = (LivingEntity) candidate; }
            }
        }
        return closest;
    }

    private static void forceLookAt(LivingEntity user, LivingEntity target) {
        Vector3d userEye   = user.getEyePosition(1.0F);
        Vector3d targetPos = target.position().add(0, target.getBbHeight() * 0.5D, 0);
        double tx = targetPos.x - userEye.x, ty = targetPos.y - userEye.y, tz = targetPos.z - userEye.z;
        float yaw   = (float)(Math.toDegrees(Math.atan2(tz, tx)) - 90.0D);
        float pitch = (float)(-Math.toDegrees(Math.atan2(ty, Math.sqrt(tx * tx + tz * tz))));
        try {
            java.lang.reflect.Method m = Entity.class.getMethod("setRotation", float.class, float.class);
            m.invoke(user, yaw, pitch);
        } catch (Exception e1) {
            try {
                java.lang.reflect.Method m = Entity.class.getDeclaredMethod("func_70080_a", float.class, float.class);
                m.setAccessible(true); m.invoke(user, yaw, pitch);
            } catch (Exception e2) {
                setField(user, Entity.class,      new String[]{"rotationYaw",   "field_70177_z", "yRot"},  yaw);
                setField(user, Entity.class,      new String[]{"rotationPitch", "field_70125_A", "xRot"},  pitch);
            }
        }
        setField(user, LivingEntity.class, new String[]{"rotationYawHead",     "field_70759_as", "yHeadRot"},  yaw);
        setField(user, LivingEntity.class, new String[]{"prevRotationYawHead", "field_70758_at", "yHeadRotO"}, yaw);
        setField(user, LivingEntity.class, new String[]{"renderYawOffset",     "field_70761_aq", "yBodyRot"},  yaw);
        setField(user, LivingEntity.class, new String[]{"prevRenderYawOffset", "field_70757_ar", "yBodyRotO"}, yaw);
    }

    private static void setField(Object inst, Class<?> cls, String[] names, float val) {
        for (String name : names) {
            Class<?> c = cls;
            while (c != null) {
                try { java.lang.reflect.Field f = c.getDeclaredField(name); f.setAccessible(true); f.set(inst, val); return; }
                catch (NoSuchFieldException e) { c = c.getSuperclass(); }
                catch (IllegalAccessException e) { break; }
            }
        }
    }

    static {
        NO_GEAR_NAME_DESC     = IDescriptionLine.of(AbilityHelper.mentionText(GOMU_GOMU_NO_PISTOL_NAME));
        SECOND_GEAR_NAME_DESC = IDescriptionLine.of(AbilityHelper.mentionText(GOMU_GOMU_NO_JET_PISTOL_NAME));
        THIRD_GEAR_NAME_DESC  = IDescriptionLine.of(AbilityHelper.mentionText(GOMU_GOMU_NO_ELEPHANT_GUN_NAME));
        FOURTH_GEAR_NAME_DESC = IDescriptionLine.of(AbilityHelper.mentionText(GOMU_GOMU_NO_KING_KONG_GUN_NAME));
        STAR_GUN_NAME_DESC    = IDescriptionLine.of(AbilityHelper.mentionText(GOMU_GOMU_NO_STAR_GUN_NAME));
        INSTANCE = (new AbilityCore.Builder("Gomu Gomu no Pistol", AbilityCategory.DEVIL_FRUITS, GomuGomuNoPistolRework::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, NO_GEAR_NAME_DESC, AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(30.0F)})
                .addAdvancedDescriptionLine(new IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, SECOND_GEAR_NAME_DESC, GomuHelper.SECOND_GEAR_REQ, AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(20.0F)})
                .addAdvancedDescriptionLine(new IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, THIRD_GEAR_NAME_DESC, GomuHelper.THIRD_GEAR_REQ, AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(120.0F)})
                .addAdvancedDescriptionLine(new IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, FOURTH_GEAR_NAME_DESC, GomuHelper.FOURTH_GEAR_REQ, AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(80.0F)})
                .addAdvancedDescriptionLine(new IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, STAR_GUN_NAME_DESC, AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(200.0F)})
                .setSourceHakiNature(SourceHakiNature.HARDENING)
                .setSourceType(new SourceType[]{SourceType.FIST})
                .build();
    }
}