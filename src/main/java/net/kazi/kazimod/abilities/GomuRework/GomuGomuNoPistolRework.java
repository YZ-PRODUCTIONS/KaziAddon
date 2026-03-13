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
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
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

    // =========================================================
    // STATIC FIELDS
    // =========================================================

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "gomu_gomu_no_pistol", new Pair[]{ImmutablePair.of("The user stretches their arm to punch the opponent.", (Object) null)});

    private static final TranslationTextComponent GOMU_GOMU_NO_PISTOL_NAME        = new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.gomu_gomu_no_pistol",        "Gomu Gomu no Pistol"));
    private static final TranslationTextComponent GOMU_GOMU_NO_JET_PISTOL_NAME    = new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.gomu_gomu_no_jet_pistol",    "Gomu Gomu no Jet Pistol"));
    private static final TranslationTextComponent GOMU_GOMU_NO_ELEPHANT_GUN_NAME  = new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.gomu_gomu_no_elephant_gun",  "Gomu Gomu no Elephant Gun"));
    private static final TranslationTextComponent GOMU_GOMU_NO_KING_KONG_GUN_NAME = new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.gomu_gomu_no_king_kong_gun", "Gomu Gomu no King Kong Gun"));
    private static final TranslationTextComponent GOMU_GOMU_NO_STAR_GUN_NAME      = new TranslationTextComponent(WyRegistry.registerName("ability.kazimod.gomu_gomu_no_star_gun",           "Gomu Gomu no Star Gun"));

    private static final ResourceLocation GOMU_GOMU_NO_PISTOL_ICON        = new ResourceLocation("mineminenomi", "textures/abilities/gomu_gomu_no_pistol.png");
    private static final ResourceLocation GOMU_GOMU_NO_JET_PISTOL_ICON    = new ResourceLocation("mineminenomi", "textures/abilities/gomu_gomu_no_jet_pistol.png");
    private static final ResourceLocation GOMU_GOMU_NO_ELEPHANT_GUN_ICON  = new ResourceLocation("mineminenomi", "textures/abilities/gomu_gomu_no_elephant_gun.png");
    private static final ResourceLocation GOMU_GOMU_NO_KING_KONG_GUN_ICON = new ResourceLocation("mineminenomi", "textures/abilities/gomu_gomu_no_king_kong_gun.png");
    private static final ResourceLocation GOMU_GOMU_NO_STAR_GUN_ICON      = new ResourceLocation("kazimod", "textures/abilities/gomu_gomu_no_star_gun.png");

    private static final int   NO_GEAR_COOLDOWN      = 30;
    private static final int   SECOND_GEAR_COOLDOWN  = 20;
    private static final int   THIRD_GEAR_COOLDOWN   = 120;
    private static final int   FOURTH_GEAR_COOLDOWN  = 80;
    private static final int   STAR_GUN_COOLDOWN     = 200;
    private static final float STAR_GUN_CHARGE_TICKS = 7.0F;

    /** Speed at which the Star Gun projectile is launched. Tweak this value to taste. */
    private static final float STAR_GUN_SPEED = 3.5F;

    /** Max range (in blocks) to pick up a lock-on target. */
    private static final double LOCK_ON_RANGE = 50.0;

    private static final IDescriptionLine NO_GEAR_NAME_DESC;
    private static final IDescriptionLine SECOND_GEAR_NAME_DESC;
    private static final IDescriptionLine THIRD_GEAR_NAME_DESC;
    private static final IDescriptionLine FOURTH_GEAR_NAME_DESC;
    private static final IDescriptionLine STAR_GUN_NAME_DESC;

    public static final AbilityCore<GomuGomuNoPistolRework> INSTANCE;

    // =========================================================
    // PISTOL MODE ENUM
    // =========================================================

    public enum PistolMode {
        NO_GEAR, GEAR_2, GEAR_3, GEAR_4, STAR_GUN
    }

    // =========================================================
    // COMPONENTS
    // =========================================================

    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);
    private final AltModeComponent<PistolMode> altModeComponent;
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final ChargeComponent chargeComponent = (new ChargeComponent(this))
            .addStartEvent(this::startChargeEvent)
            .addTickEvent(this::duringChargeEvent)
            .addEndEvent(this::endChargeEvent);

    // =========================================================
    // INSTANCE FIELDS
    // =========================================================

    private float speed;
    private float cooldown;
    private boolean isStarGunMode = false;

    /** The entity whose position we are tracking during Star Gun charge. Null if none. */
    private LivingEntity lockedTarget = null;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public GomuGomuNoPistolRework(AbilityCore<GomuGomuNoPistolRework> core) {
        super(core);
        this.altModeComponent = (new AltModeComponent<PistolMode>(this, PistolMode.class, PistolMode.NO_GEAR, true))
                .addChangeModeEvent(this::altModeChangeEvent);
        this.speed = 2.0F;
        this.cooldown = NO_GEAR_COOLDOWN;
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.projectileComponent,
                this.altModeComponent,
                this.animationComponent,
                this.chargeComponent
        });
        this.addUseEvent(this::useEvent);
    }

    // =========================================================
    // USE EVENT
    // =========================================================

    private void useEvent(LivingEntity entity, IAbility ability) {
        if (this.isStarGunMode) {
            if (!isGearFifthActive(entity)) {
                entity.sendMessage(
                        new StringTextComponent("Gear Fifth must be active to use Star Gun!"),
                        entity.getUUID()
                );
                return;
            }
            // Attempt to find a look-at target before charging begins
            this.lockedTarget = findLookTarget(entity);
            this.chargeComponent.startCharging(entity, STAR_GUN_CHARGE_TICKS);
        } else {
            AbilityProjectileEntity projectile = this.createProjectile(entity);
            this.projectileComponent.shoot(projectile, entity, this.speed, 0.0F);
            entity.swing(Hand.MAIN_HAND, true);
            entity.level.playSound((PlayerEntity) null, entity.blockPosition(), (SoundEvent) ModSounds.GOMU_SFX.get(), SoundCategory.PLAYERS, 2.0F, 1.0F);
            this.cooldownComponent.startCooldown(entity, this.cooldown);
        }
    }

    // =========================================================
    // LOCK-ON HELPER
    // =========================================================

    /**
     * Performs a ray-cast from the user's eye position in their look direction,
     * and returns the first {@link LivingEntity} hit within {@link #LOCK_ON_RANGE} blocks.
     * Returns {@code null} if nothing is found.
     */
    private LivingEntity findLookTarget(LivingEntity user) {
        Vector3d eyePos  = user.getEyePosition(1.0F);
        Vector3d lookVec = user.getViewVector(1.0F);
        Vector3d endPos  = eyePos.add(lookVec.scale(LOCK_ON_RANGE));

        // Expand generously — at 50 blocks a small box misses a lot
        AxisAlignedBB searchBox = new AxisAlignedBB(eyePos.x, eyePos.y, eyePos.z, endPos.x, endPos.y, endPos.z)
                .inflate(5.0D);

        List<Entity> candidates = user.level.getEntities(user, searchBox,
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator());

        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity candidate : candidates) {
            // Inflate hitbox more at range so aim doesn't need to be pixel-perfect
            AxisAlignedBB entityBox = candidate.getBoundingBox().inflate(1.5D);
            Optional<Vector3d> hit = entityBox.clip(eyePos, endPos);
            if (hit.isPresent()) {
                double dist = eyePos.distanceTo(hit.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = (LivingEntity) candidate;
                }
            }
        }

        return closest;
    }

    /**
     * Forces {@code user} to look at the world-position of {@code target},
     * updating both yaw and pitch on the server so the projectile fires correctly.
     */
    private static void forceLookAt(LivingEntity user, LivingEntity target) {
        Vector3d userEye   = user.getEyePosition(1.0F);
        Vector3d targetPos = target.position().add(0, target.getBbHeight() * 0.5D, 0);

        double tx = targetPos.x - userEye.x;
        double ty = targetPos.y - userEye.y;
        double tz = targetPos.z - userEye.z;

        double horizontalDist = Math.sqrt(tx * tx + tz * tz);

        float yaw   = (float)(Math.toDegrees(Math.atan2(tz, tx)) - 90.0D);
        float pitch = (float)(-Math.toDegrees(Math.atan2(ty, horizontalDist)));

        // Try every known mapping name for setRotation and the yaw/pitch fields
        // so this compiles regardless of which mapping set the mod uses
        try {
            java.lang.reflect.Method setRot = Entity.class.getMethod("setRotation", float.class, float.class);
            setRot.invoke(user, yaw, pitch);
        } catch (Exception e1) {
            try {
                // Obfuscated name fallback
                java.lang.reflect.Method setRot = Entity.class.getDeclaredMethod("func_70080_a", float.class, float.class);
                setRot.setAccessible(true);
                setRot.invoke(user, yaw, pitch);
            } catch (Exception e2) {
                // Last resort: directly set the yaw/pitch fields by their obfuscated names
                setFieldByAnyName(user, Entity.class,      new String[]{"rotationYaw",   "field_70177_z", "yRot"},  yaw);
                setFieldByAnyName(user, Entity.class,      new String[]{"rotationPitch", "field_70125_A", "xRot"},  pitch);
            }
        }

        // Head and body yaw — try MCP names then obfuscated names
        setFieldByAnyName(user, LivingEntity.class, new String[]{"rotationYawHead",     "field_70759_as", "yHeadRot"},      yaw);
        setFieldByAnyName(user, LivingEntity.class, new String[]{"prevRotationYawHead", "field_70758_at", "yHeadRotO"},     yaw);
        setFieldByAnyName(user, LivingEntity.class, new String[]{"renderYawOffset",     "field_70761_aq", "yBodyRot"},      yaw);
        setFieldByAnyName(user, LivingEntity.class, new String[]{"prevRenderYawOffset", "field_70757_ar", "yBodyRotO"},     yaw);
    }

    /**
     * Tries each name in {@code names} until one resolves on {@code clazz} or its superclasses.
     * Silently does nothing if none resolve.
     */
    private static void setFieldByAnyName(Object instance, Class<?> clazz, String[] names, float value) {
        for (String name : names) {
            Class<?> search = clazz;
            while (search != null) {
                try {
                    java.lang.reflect.Field f = search.getDeclaredField(name);
                    f.setAccessible(true);
                    f.set(instance, value);
                    return; // success
                } catch (NoSuchFieldException e) {
                    search = search.getSuperclass();
                } catch (IllegalAccessException e) {
                    break;
                }
            }
        }
    }

    // =========================================================
    // STAR GUN CHARGE EVENTS
    // =========================================================

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, ModAnimations.BODY_ROTATION_WIDE_ARMS);
        // If we found a target, snap to it immediately on the first frame
        if (this.lockedTarget != null && this.lockedTarget.isAlive()) {
            forceLookAt(entity, this.lockedTarget);
        }
    }

    private void duringChargeEvent(LivingEntity entity, IAbility ability) {
        AbilityHelper.setDeltaMovement(entity, 0.0D, entity.getDeltaMovement().y, 0.0D);

        // If we have a valid locked target, keep the user's look direction tracking it
        if (this.lockedTarget != null) {
            if (this.lockedTarget.isAlive()) {
                forceLookAt(entity, this.lockedTarget);
            } else {
                // Target died mid-charge — release the lock gracefully
                this.lockedTarget = null;
            }
        }
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        // Make sure we're still aimed at the target on the final frame before firing
        if (this.lockedTarget != null && this.lockedTarget.isAlive()) {
            forceLookAt(entity, this.lockedTarget);
        }
        // Release the lock regardless of outcome
        this.lockedTarget = null;

        this.animationComponent.stop(entity);

        GomuGomuNoStarGunProjectile projectile = new GomuGomuNoStarGunProjectile(entity.level, entity);
        projectile.setLaunched(true);
        // Use the dedicated Star Gun speed constant instead of the shared this.speed field
        this.projectileComponent.shoot(projectile, entity, STAR_GUN_SPEED, 0.0F);

        entity.swing(Hand.MAIN_HAND, true);
        entity.level.playSound((PlayerEntity) null, entity.blockPosition(), (SoundEvent) ModSounds.GOMU_SFX.get(), SoundCategory.PLAYERS, 2.0F, 1.0F);
        this.cooldownComponent.startCooldown(entity, STAR_GUN_COOLDOWN);
    }

    // =========================================================
    // ALT MODE
    // =========================================================

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

    // =========================================================
    // PROJECTILE FACTORY
    // =========================================================

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
        } else if (GomuHelper.hasGearSecondActive(props)) {
            projectile = new GomuGomuNoJetPistolProjectile(entity.level, entity, this);
            this.speed = 2.5F;
        } else {
            projectile = new GomuGomuNoPistolProjectile(entity.level, entity);
        }
        return projectile;
    }

    // =========================================================
    // GEAR FIFTH CHECK
    // =========================================================

    private static boolean isGearFifthActive(LivingEntity entity) {
        IAbilityData props = AbilityDataCapability.get(entity);
        GearFifthRework gearFifth = (GearFifthRework) props.getEquippedAbility(GearFifthRework.INSTANCE);
        if (gearFifth == null) return false;
        return gearFifth.getContinuousComponent().isContinuous();
    }

    // =========================================================
    // PUBLIC GEAR SWITCH HELPERS
    // =========================================================

    public void switchNoGear(LivingEntity entity)     { this.altModeComponent.setMode(entity, PistolMode.NO_GEAR);  }
    public void switchSecondGear(LivingEntity entity) { this.altModeComponent.setMode(entity, PistolMode.GEAR_2);   }
    public void switchThirdGear(LivingEntity entity)  { this.altModeComponent.setMode(entity, PistolMode.GEAR_3);   }
    public void switchFourthGear(LivingEntity entity) { this.altModeComponent.setMode(entity, PistolMode.GEAR_4);   }
    public void switchFifthGear(LivingEntity entity)  { this.altModeComponent.setMode(entity, PistolMode.STAR_GUN); }

    // =========================================================
    // STATIC INIT
    // =========================================================

    static {
        NO_GEAR_NAME_DESC     = IDescriptionLine.of(AbilityHelper.mentionText(GOMU_GOMU_NO_PISTOL_NAME));
        SECOND_GEAR_NAME_DESC = IDescriptionLine.of(AbilityHelper.mentionText(GOMU_GOMU_NO_JET_PISTOL_NAME));
        THIRD_GEAR_NAME_DESC  = IDescriptionLine.of(AbilityHelper.mentionText(GOMU_GOMU_NO_ELEPHANT_GUN_NAME));
        FOURTH_GEAR_NAME_DESC = IDescriptionLine.of(AbilityHelper.mentionText(GOMU_GOMU_NO_KING_KONG_GUN_NAME));
        STAR_GUN_NAME_DESC    = IDescriptionLine.of(AbilityHelper.mentionText(GOMU_GOMU_NO_STAR_GUN_NAME));

        INSTANCE = (new AbilityCore.Builder("Gomu Gomu no Pistol", AbilityCategory.DEVIL_FRUITS, GomuGomuNoPistolRework::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE, NO_GEAR_NAME_DESC,
                        AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(30.0F)})
                .addAdvancedDescriptionLine(new IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE, SECOND_GEAR_NAME_DESC, GomuHelper.SECOND_GEAR_REQ,
                        AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(20.0F)})
                .addAdvancedDescriptionLine(new IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE, THIRD_GEAR_NAME_DESC, GomuHelper.THIRD_GEAR_REQ,
                        AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(120.0F)})
                .addAdvancedDescriptionLine(new IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE, FOURTH_GEAR_NAME_DESC, GomuHelper.FOURTH_GEAR_REQ,
                        AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(80.0F)})
                .addAdvancedDescriptionLine(new IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE, STAR_GUN_NAME_DESC,
                        AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(200.0F)})
                .setSourceHakiNature(SourceHakiNature.HARDENING)
                .setSourceType(new SourceType[]{SourceType.FIST})
                .build();
    }
}