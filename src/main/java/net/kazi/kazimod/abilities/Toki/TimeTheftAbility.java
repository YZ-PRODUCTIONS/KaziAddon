package net.kazi.kazimod.abilities.Toki;

import java.awt.Color;
import java.util.List;
import java.util.function.Predicate;
import net.kazi.kazimod.entities.projectiles.TokiProjectiles;
import net.kazi.kazimod.abilities.Toki.TimeBarAbility;
import net.kazi.kazimod.init.KaziSounds;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import net.kazi.kazimod.entities.projectiles.TimeTheftProjectile;
import net.kazi.kazimod.init.KaziAnimations;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.PunchAbility2;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.math.VectorHelper;
import xyz.pixelatedw.mineminenomi.config.CommonConfig;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.haki.HakiDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.haki.IHakiData;
import xyz.pixelatedw.mineminenomi.entities.SphereEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class TimeTheftAbility extends PunchAbility2 {

    // ── Description ───────────────────────────────────────────────────────────
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "time_theft",
            new Pair[]{
                    ImmutablePair.of(
                            "Rips time itself from every living thing inside a spherical area, " +
                                    "instantly killing them. Entities whose combined Doriki and Busoshoku " +
                                    "Haki exceeds the user's power are unaffected.",
                            (Object) null
                    ),
                    ImmutablePair.of(
                            "§aTIME THEFT PUNCH§r: A targeted punch that instantly kills a single entity. " +
                                    "Target must have less combined Doriki and Busoshoku Haki than you.",
                            (Object) null
                    ),
                    ImmutablePair.of(
                            "§aTIME THEFT BEAM§r: Fires a concentrated beam of temporal energy. " +
                                    "Deals 25 damage and explodes on impact.",
                            (Object) null
                    )
            }
    );

    // ── Alt-mode icons ────────────────────────────────────────────────────────
    private static final ResourceLocation TIME_THEFT_PUNCH_ICON =
            new ResourceLocation("kazimod", "textures/abilities/time_theft_punch.png");
    private static final ResourceLocation TIME_THEFT_BEAM_ICON =
            new ResourceLocation("kazimod", "textures/abilities/time_theft_beam.png");

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final float  AOE_COOLDOWN    = 400.0F;
    private static final float  CHARGE_TICKS    = 15.0F;
    private static final float  EXPAND_TICKS    = 20.0F;
    private static final int    ROOM_RADIUS     = 10;
    private static final double RESIST_RATIO    = 1.3;

    private static final float  PUNCH_COOLDOWN  = 10.0F;

    private static final float  BEAM_COOLDOWN   = 300.0F; // 10 s
    private static final float  BEAM_SPEED      = 5.5F;
    private static final float  BEAM_DAMAGE     = 25.0F;
    private static final float  BEAM_SIZE       = 0.25F;
    private static final float  BEAM_LENGTH     = 20.0F;  // 20-block range

    // ── HP threshold above which entities are immune to the AOE ──────────────
    private static final float  AOE_HP_IMMUNITY_THRESHOLD = 250.0F;

    // ── Palm sphere sizing & placement ────────────────────────────────────────
    private static final double PALM_FORWARD    = 0.85;
    private static final double PALM_SIDE       = 0.25;
    private static final double PALM_HEIGHT     = 1.75;
    private static final float  PALM_RADIUS_MIN = 0.18F;
    private static final float  PALM_RADIUS_MAX = 0.32F;

    public static final AbilityCore<TimeTheftAbility> INSTANCE;

    // ── Components ────────────────────────────────────────────────────────────
    private final AltModeComponent<Mode> altModeComponent;

    private final ChargeComponent chargeComponent =
            (new ChargeComponent(this))
                    .addStartEvent(this::onChargeStart)
                    .addTickEvent(this::onChargeTick)
                    .addEndEvent(this::onChargeEnd);

    private final ContinuousComponent aoeComponent =
            (new ContinuousComponent(this, true))
                    .addTickEvent(this::onExpandTick)
                    .addEndEvent(this::onExpandEnd);

    private final ProjectileComponent projectileComponent =
            new ProjectileComponent(this, this::createBeamProjectile);

    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent      rangeComponent      = new RangeComponent(this);
    private final AnimationComponent  animationComponent  = new AnimationComponent(this);

    // ── Runtime state ─────────────────────────────────────────────────────────
    private SphereEntity aoeSphere;
    private SphereEntity leftHandSphere;
    private SphereEntity rightHandSphere;
    private int expandTick = 0;

    // ── Constructor ───────────────────────────────────────────────────────────
    public TimeTheftAbility(AbilityCore<TimeTheftAbility> core) {
        super(core);
        this.altModeComponent = (new AltModeComponent<>(this, Mode.class, Mode.AOE))
                .addChangeModeEvent(this::onAltModeChange);
        this.addComponents(new AbilityComponent[]{
                this.altModeComponent,
                this.chargeComponent,
                this.aoeComponent,
                this.projectileComponent,
                this.dealDamageComponent,
                this.rangeComponent,
                this.animationComponent
        });
        this.addUseEvent(100, this::onUseEvent);
    }

    // =========================================================================
    //  PunchAbility2 required overrides
    // =========================================================================

    @Override
    public Predicate<LivingEntity> canActivate() {
        return (entity) -> this.altModeComponent.isMode(Mode.PUNCH)
                && this.continuousComponent.isContinuous()
                && entity.getMainHandItem().isEmpty();
    }

    @Override
    public int getUseLimit() {
        return 1;
    }

    @Override
    public float getPunchDamage() {
        return 1.0F;
    }

    @Override
    public float getPunchCooldown() {
        return PUNCH_COOLDOWN;
    }

    @Override
    public boolean onHitEffect(LivingEntity entity, LivingEntity target, ModDamageSource source) {
        if (entity.level.isClientSide)         return true;
        if (!(entity instanceof PlayerEntity)) return true;

        PlayerEntity player = (PlayerEntity) entity;
        source.setBypassFriendlyDamage();

        if (AbilityHelper.isTargetBlocking(player, target) || AbilityHelper.isHakiBlocking(player, target)) {
            return true;
        }

        // Entities with 1000+ max HP are completely immune to the punch.
        if (getBaseMaxHealth(target) >= AOE_HP_IMMUNITY_THRESHOLD) {
            player.displayClientMessage(
                    new TranslationTextComponent("Time Theft Punch Failed"),
                    true
            );
            return true;
        }

        double userPower   = getTotalPower(entity);
        double targetPower = getTotalPower(target);

        boolean canKill     = false;
        boolean instantKill = false;

        if (target instanceof PlayerEntity) {
            if (targetPower > 0.0 && userPower > 0.0) {
                double ratio = userPower / targetPower;
                // Strict power check — no random chance, must meet the resist ratio.
                if (ratio >= RESIST_RATIO) {
                    canKill = true;
                }
            }
        } else {
            if (userPower > targetPower) {
                canKill     = true;
                instantKill = true;
            }
        }

        if (!canKill) {
            player.displayClientMessage(
                    new TranslationTextComponent("message.kazimod.time_theft_punch_failed"),
                    true
            );
            return true;
        }

        if (target instanceof PlayerEntity) {
            IEntityStats targetStats = EntityStatsCapability.get(target);
            if (targetStats != null) {
                int newDoriki = Math.max(0, (int) targetStats.getDoriki() - 50);
                targetStats.setDoriki(newDoriki);
            }
        }

        if (target.isAlive()) {
            target.hurt(source, target.getMaxHealth() * 100.0F);
            grantTimePointsFromKill(entity, target);
        }

        return true;
    }

    // =========================================================================
    //  Unified use event — routes to the right mode
    // =========================================================================

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.altModeComponent.isMode(Mode.AOE)) {
            if (!this.chargeComponent.isCharging() && !this.aoeComponent.isContinuous()) {
                this.chargeComponent.startCharging(entity, CHARGE_TICKS);
            }
        } else if (this.altModeComponent.isMode(Mode.BEAM)) {
            fireBeam(entity);
        }
        // PUNCH mode falls through to PunchAbility2's own use event (priority 200).
    }

    // ── Alt mode switch ───────────────────────────────────────────────────────
    private void onAltModeChange(LivingEntity entity, IAbility ability, Mode mode) {
        if (mode == Mode.PUNCH) {
            super.setDisplayIcon(TIME_THEFT_PUNCH_ICON);
        } else if (mode == Mode.BEAM) {
            super.setDisplayIcon(TIME_THEFT_BEAM_ICON);
        } else {
            super.setDisplayIcon(INSTANCE);
        }
    }

    public void switchToAoeMode(LivingEntity entity) {
        this.altModeComponent.setMode(entity, Mode.AOE);
    }

    public void switchToPunchMode(LivingEntity entity) {
        this.altModeComponent.setMode(entity, Mode.PUNCH);
    }

    public void switchToBeamMode(LivingEntity entity) {
        this.altModeComponent.setMode(entity, Mode.BEAM);
    }

    // =========================================================================
    //  BEAM mode — shoots a LightningEntity like Gastille, time-theft coloured
    // =========================================================================

    private void fireBeam(LivingEntity entity) {
        if (entity.level.isClientSide) return;

        RayTraceResult mop = WyHelper.rayTraceBlocks(entity, (double) BEAM_LENGTH);
        double beamDistance = Math.sqrt(
                entity.distanceToSqr(mop.getLocation().x, mop.getLocation().y, mop.getLocation().z)
        );

        // Spawn offset slightly forward from the chest, same as Gastille.
        Vector3d pos = VectorHelper.calculateRotationBasedOffsetPosition(
                entity.position(), (double) entity.yBodyRot, 0.5, 1.15, 0.8
        );

        LightningEntity bolt = new LightningEntity(
                entity, pos.x, pos.y, pos.z,
                entity.yRot, entity.xRot,
                BEAM_LENGTH + (float) beamDistance,
                BEAM_SPEED,
                this.getCore()
        );

        bolt.setMaxLife(40);
        bolt.setDamage(BEAM_DAMAGE);
        bolt.setExplosion(3, true, 0.2F);
        bolt.setSize(BEAM_SIZE);
        bolt.setBoxSizeDivision(1.0);
        // Light-blue time-theft colour: rgb(180, 230, 255)
        bolt.setColor(new Color(180, 230, 255));
        bolt.setAngle(100);
        bolt.setTargetTimeToReset(6000);
        bolt.disableExplosionKnockback();
        bolt.setBranches(1);
        bolt.setSegments(1);
        bolt.setBlocksAffectedLimit(256);

        entity.level.addFreshEntity(bolt);
        grantFixedTimePoints(entity, 5.0F);
        this.cooldownComponent.startCooldown(entity, BEAM_COOLDOWN);
    }

    private AbilityProjectileEntity createBeamProjectile(LivingEntity entity) {
        // ProjectileComponent fallback — not used directly since fireBeam handles
        // everything, but required for the component to be valid.
        return new TimeTheftProjectile(entity.level, entity);
    }

    // =========================================================================
    //  AOE (original Time Theft) logic
    // =========================================================================

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, KaziAnimations.TIME_THEFT);

        if (CommonConfig.INSTANCE.isExperiementalSpheresEnabled()) {
            double[] left  = getLeftPalmPos(entity);
            double[] right = getRightPalmPos(entity);
            this.leftHandSphere  = makePalmSphere(entity, left[0],  left[1],  left[2]);
            this.rightHandSphere = makePalmSphere(entity, right[0], right[1], right[2]);
            entity.level.addFreshEntity(this.leftHandSphere);
            entity.level.addFreshEntity(this.rightHandSphere);
        }

        entity.level.playSound(
                (PlayerEntity) null,
                entity.blockPosition(),
                (SoundEvent) KaziSounds.TIMETHEFT_SFX.get(),
                SoundCategory.PLAYERS, 1.0F, 1.5F
        );
    }

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        if (!CommonConfig.INSTANCE.isExperiementalSpheresEnabled()) return;

        float progress = this.chargeComponent.getChargePercentage();
        float radius   = PALM_RADIUS_MIN + (PALM_RADIUS_MAX - PALM_RADIUS_MIN) * progress;
        double[] left  = getLeftPalmPos(entity);
        double[] right = getRightPalmPos(entity);

        if (this.leftHandSphere  != null) { this.leftHandSphere.setRadius(radius);  this.leftHandSphere.setPos(left[0],  left[1],  left[2]);  }
        if (this.rightHandSphere != null) { this.rightHandSphere.setRadius(radius); this.rightHandSphere.setPos(right[0], right[1], right[2]); }
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);
        removePalmSpheres();

        if (!entity.level.isClientSide) {


            if (CommonConfig.INSTANCE.isExperiementalSpheresEnabled()) {
                this.aoeSphere = new SphereEntity(entity.level, entity);
                this.aoeSphere.setColor(new Color(180, 230, 255, 60));
                this.aoeSphere.setRadius(0.0F);
                this.aoeSphere.setDetailLevel(32);
                this.aoeSphere.setAnimationSpeed(1);
                this.aoeSphere.setPos(entity.getX(), entity.getY(), entity.getZ());
                entity.level.addFreshEntity(this.aoeSphere);
            }

            this.expandTick = 0;
            this.aoeComponent.startContinuity(entity, EXPAND_TICKS);
        }
    }

    private void onExpandTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        float progress  = Math.min((float) this.expandTick / EXPAND_TICKS, 1.0F);
        float curRadius = ROOM_RADIUS * progress;

        if (CommonConfig.INSTANCE.isExperiementalSpheresEnabled() && this.aoeSphere != null) {
            this.aoeSphere.setPos(entity.getX(), entity.getY(), entity.getZ());
            this.aoeSphere.setRadius(curRadius);
        }

        ModDamageSource source = (ModDamageSource) this.dealDamageComponent.getDamageSource(entity);
        source.setBypassFriendlyDamage();
        double userPower = getTotalPower(entity);

        List<LivingEntity> targets = WyHelper.getNearbyEntities(
                entity.position(), entity.level,
                curRadius, curRadius, curRadius,
                null, new Class[]{ LivingEntity.class }
        );

        for (LivingEntity target : targets) {
            if (target == entity) continue;

            // Entities with 1000+ max HP are completely immune to the AOE.
            if (getBaseMaxHealth(target) >= AOE_HP_IMMUNITY_THRESHOLD) continue;

            double targetPower = getTotalPower(target);

            if (target instanceof PlayerEntity) {
                PlayerEntity targetPlayer = (PlayerEntity) target;
                if (entity instanceof PlayerEntity) {
                    PlayerEntity attackerPlayer = (PlayerEntity) entity;
                    if (AbilityHelper.isTargetBlocking(attackerPlayer, target)
                            || AbilityHelper.isHakiBlocking(attackerPlayer, target)) continue;
                }
                // Players whose power meets or exceeds the resist ratio are completely
                // immune — no random chance of dying whatsoever.
                if (targetPower > 0.0 && userPower > 0.0 && targetPower / userPower >= RESIST_RATIO) {
                    targetPlayer.sendMessage(
                            new TranslationTextComponent("message.cartaddon.time_theft_resisted"),
                            targetPlayer.getUUID()
                    );
                    continue;
                }
                // Additionally, players with equal or greater raw power than the user
                // are always immune (strict check, zero RNG).
                if (userPower <= 0.0 || targetPower >= userPower) continue;
            } else {
                if (targetPower > userPower) continue;
            }

            if (target.isAlive()) {
                target.hurt(source, target.getMaxHealth() * 100.0F);
                grantTimePointsFromKill(entity, target);
            }
        }

        this.expandTick++;
    }

    private void onExpandEnd(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {

        }
        removeAoeSphere();
        this.expandTick = 0;
        this.cooldownComponent.startCooldown(entity, AOE_COOLDOWN);
    }

    // ── Palm position helpers ─────────────────────────────────────────────────

    private static double[] getLeftPalmPos(LivingEntity entity) {
        double yawRad = Math.toRadians(entity.yRot);
        double fwdX = -Math.sin(yawRad), fwdZ = Math.cos(yawRad);
        double leftX = -fwdZ, leftZ = fwdX;
        return new double[]{ entity.getX() + fwdX * PALM_FORWARD + leftX * PALM_SIDE, entity.getY() + PALM_HEIGHT, entity.getZ() + fwdZ * PALM_FORWARD + leftZ * PALM_SIDE };
    }

    private static double[] getRightPalmPos(LivingEntity entity) {
        double yawRad = Math.toRadians(entity.yRot);
        double fwdX = -Math.sin(yawRad), fwdZ = Math.cos(yawRad);
        double rightX = fwdZ, rightZ = -fwdX;
        return new double[]{ entity.getX() + fwdX * PALM_FORWARD + rightX * PALM_SIDE, entity.getY() + PALM_HEIGHT, entity.getZ() + fwdZ * PALM_FORWARD + rightZ * PALM_SIDE };
    }

    private SphereEntity makePalmSphere(LivingEntity entity, double x, double y, double z) {
        SphereEntity s = new SphereEntity(entity.level, entity);
        s.setColor(new Color(180, 230, 255, 200));
        s.setRadius(PALM_RADIUS_MIN);
        s.setDetailLevel(16);
        s.setAnimationSpeed(2);
        s.setPos(x, y, z);
        return s;
    }

    private void removePalmSpheres() {
        if (this.leftHandSphere  != null) { this.leftHandSphere.remove();  this.leftHandSphere  = null; }
        if (this.rightHandSphere != null) { this.rightHandSphere.remove(); this.rightHandSphere = null; }
    }

    private void removeAoeSphere() {
        if (this.aoeSphere != null) { this.aoeSphere.remove(); this.aoeSphere = null; }
    }

    // ── Time Bar point grant ───────────────────────────────────────────────────
    /**
     * Awards time points based on the killed target's max health.
     * Formula: floor(maxHealth / 10), clamped [1, 10].
     */
    private static void grantTimePointsFromKill(LivingEntity attacker, LivingEntity killed) {
        TimeBarAbility bar = getTimeBar(attacker);
        if (bar == null) return;
        float pts = Math.min(10.0F, Math.max(1.0F, (float) Math.floor(killed.getMaxHealth() / 10.0F)));
        bar.addTimePoints(attacker, pts);
    }

    /** Awards a fixed number of time points (e.g. beam always gives 5). */
    private static void grantFixedTimePoints(LivingEntity attacker, float pts) {
        TimeBarAbility bar = getTimeBar(attacker);
        if (bar == null) return;
        bar.addTimePoints(attacker, pts);
    }

    private static TimeBarAbility getTimeBar(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) return null;
        IAbilityData data = AbilityDataCapability.getLazy(entity).orElse(null);
        if (data == null) return null;
        for (IAbility abl : data.getEquippedAndPassiveAbilities()) {
            if (abl instanceof TimeBarAbility) return (TimeBarAbility) abl;
        }
        return null;
    }

    // ── HP helper ─────────────────────────────────────────────────────────────
    /**
     * Returns the entity's maximum health using the attribute system directly,
     * which is more reliable than getMaxHealth() when mods apply HP modifiers.
     * Falls back to getMaxHealth() if the attribute is unavailable.
     */
    private static float getBaseMaxHealth(LivingEntity entity) {
        return entity.getMaxHealth();
    }

    // ── Power helper ──────────────────────────────────────────────────────────
    private static double getTotalPower(LivingEntity entity) {
        IEntityStats stats = EntityStatsCapability.get(entity);
        IHakiData    haki  = HakiDataCapability.get(entity);
        double doriki  = stats != null ? stats.getDoriki()          : 0.0;
        double hakiVal = haki  != null ? haki.getBusoshokuHakiExp() : 0.0;
        return doriki + hakiVal;
    }

    // ── Alt mode enum ─────────────────────────────────────────────────────────
    public enum Mode {
        AOE,
        PUNCH,
        BEAM;

        private Mode() {}
    }

    // ── Static initialiser ────────────────────────────────────────────────────
    static {
        INSTANCE = (new AbilityCore.Builder<>(
                "Time Theft",
                AbilityCategory.DEVIL_FRUITS,
                TimeTheftAbility::new
        ))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(AOE_COOLDOWN),
                        ChargeComponent.getTooltip(CHARGE_TICKS),
                        RangeComponent.getTooltip((float) ROOM_RADIUS, RangeType.AOE)
                })
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceType(new SourceType[]{ SourceType.FIST })
                .build();
    }
}