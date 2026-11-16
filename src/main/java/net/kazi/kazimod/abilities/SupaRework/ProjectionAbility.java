package net.kazi.kazimod.abilities.SupaRework;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.MrMagicalCart.cartaddon.init.CartParticleEffects;
import net.kazi.kazimod.entities.RhoAiasEntity;
import net.kazi.kazimod.entities.RealityMarbleWeaponEntity;
import net.kazi.kazimod.entities.projectiles.CaladbolgProjectile;
import net.kazi.kazimod.entities.projectiles.ReplicatedSwordEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.kazi.kazimod.init.KaziEntities;
import net.kazi.kazimod.init.KaziPacketHandler;
import net.kazi.kazimod.init.KaziSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.LightningDischargeEntity;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

/**
 * Supa awakening move with four deliberately separate behavior paths. The modes
 * are intentionally left as foundations until their individual effects are set.
 */
public class ProjectionAbility extends Ability {
    private static final int REPLICATION_SWORD_COUNT = 20;
    private static final int REPLICATION_SHOT_INTERVAL = 5;
    private static final float GALAXY_IMPACT_CHARGE = 40.0F;
    private static final float GALAXY_IMPACT_COOLDOWN = 400.0F;
    private static final float GALAXY_IMPACT_PROJECTILE_SPEED = 2.5F;
    private static final float RHO_AIAS_COOLDOWN = 15.0F * 20.0F;
    private static final double THIRD_COUNTER_HALF_WIDTH = 50.0D;
    private static final double THIRD_COUNTER_HALF_HEIGHT = 25.0D;
    private static final double THIRD_COUNTER_HALF_DEPTH = 50.0D;
    private static final int THIRD_MIN_PROJECTILE_AGE = 4;
    private static final int THIRD_SWORD_LIFETIME = 10;
    private static final float THIRD_DURATION = 400.0F;
    private static final float THIRD_COOLDOWN = 400.0F;
    private static final double THIRD_SHAKE_RADIUS = 100.0D;
    private static final int THIRD_SHAKE_TICKS = 40;
    private static final float THIRD_SHAKE_INTENSITY = 2.0F;

    public enum Mode {
        RHO_AIAS("Rho Aias"), SECOND("Caladbolg"),
        THIRD("Universe of Infinite Blades"), FOURTH("Instant Sword Replication");

        private final String displayName;

        Mode() {
            this.displayName = name();
        }

        Mode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return this.displayName;
        }
    }

    private static final ResourceLocation[] MODE_ICONS = new ResourceLocation[]{
            new ResourceLocation("kazimod", "textures/abilities/supa_rho_aias.png"),
            new ResourceLocation("kazimod", "textures/abilities/supa_caladbolg.png"),
            new ResourceLocation("kazimod", "textures/abilities/supa_projectile_counter.png"),
            new ResourceLocation("kazimod", "textures/abilities/supa_instant_replication.png")
    };
    private static final Pair<String, Object[]>[] DESCRIPTION = new Pair[]{
            ImmutablePair.<String, Object[]>of("Rho Aias: Projects a seven-petal shield that grants invulnerability while maintained.", null),
            ImmutablePair.<String, Object[]>of("Caladbolg: Fires a charged explosive projectile that erupts on impact.", null),
            ImmutablePair.<String, Object[]>of("Universe of Infinite Blades: Automatically destroys nearby projectiles with summoned swords.", null),
            ImmutablePair.<String, Object[]>of("Instant Sword Replication: Opens portals that rapidly launch weakly homing swords at a target.", null)
    };

    public static final AbilityCore<ProjectionAbility> INSTANCE = new AbilityCore.Builder<>(
            "Projection", AbilityCategory.DEVIL_FRUITS, ProjectionAbility::new)
            .addDescriptionLine(AbilityHelper.registerDescriptionText("kazimod", "projection", DESCRIPTION))
            .setIcon(MODE_ICONS[Mode.RHO_AIAS.ordinal()])
            .setSourceHakiNature(SourceHakiNature.IMBUING)
            .setSourceType(new SourceType[]{SourceType.SLASH})
            .setUnlockCheck(user -> DevilFruitCapability.get(user).hasAwakenedFruit())
            .build();

    private final AltModeComponent<Mode> altModeComponent =
            new ProjectionAltModeComponent(this)
                    .addChangeModeEvent(this::onModeChanged);
    private final ContinuousComponent continuousComponent = new ContinuousComponent(this)
            .addStartEvent(100, this::startContinuous)
            .addTickEvent(100, this::tickContinuous)
            .addEndEvent(100, this::endContinuous);
    private final ChargeComponent chargeComponent = new ChargeComponent(this)
            .addStartEvent(this::startCharge)
            .addTickEvent(this::tickCharge)
            .addEndEvent(this::endCharge);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final CooldownComponent cooldownComponent = new CooldownComponent(this);
    private final DamageTakenComponent damageTakenComponent = new DamageTakenComponent(this)
            .addOnAttackEvent(this::onRhoAiasDamageTaken);
    private RhoAiasEntity rhoAiasEntity;
    private final List<RealityMarbleWeaponEntity> counterSwords = new ArrayList<>();
    private boolean preventGalaxyImpactFallDamage;
    private Mode currentMode = Mode.RHO_AIAS;
    private Mode activeMode = Mode.RHO_AIAS;
    private final List<ReplicatedSwordEntity> replicatedSwords = new ArrayList<>();
    private int replicationTicks;
    private int swordsFired;
    private int replicationChargeTicks;

    public ProjectionAbility(AbilityCore<ProjectionAbility> core) {
        super(core);
        this.isNew = true;
        this.setDisplayIcon(MODE_ICONS[Mode.RHO_AIAS.ordinal()]);
        this.addComponents(new AbilityComponent[]{
                this.altModeComponent, this.continuousComponent, this.chargeComponent,
                this.animationComponent, this.cooldownComponent, this.damageTakenComponent
        });
        this.addUseEvent(this::onUse);
        this.addTickEvent(this::onAbilityTick);
        this.addRemoveEvent((entity, ability) -> clearReplicatedSwords());
    }

    private void onModeChanged(LivingEntity entity, IAbility ability, Mode mode) {
        this.currentMode = mode;
        this.setDisplayIcon(MODE_ICONS[mode.ordinal()]);
    }

    @Override
    public void load(CompoundNBT nbt) {
        super.load(nbt);
        this.currentMode = this.altModeComponent.getCurrentMode();
        this.setDisplayIcon(MODE_ICONS[this.currentMode.ordinal()]);
    }

    /** Finds the next unlocked mode, wrapping around instead of stopping on a lock. */
    private static Mode findNextAvailableMode(LivingEntity entity, Mode from) {
        Mode[] modes = Mode.values();
        for (int offset = 1; offset < modes.length; offset++) {
            Mode candidate = modes[(from.ordinal() + offset) % modes.length];
            if (isModeAvailable(entity, candidate)) return candidate;
        }
        return from;
    }

    private static boolean isModeAvailable(LivingEntity entity, Mode mode) {
        IAbilityData abilityData = AbilityDataCapability.get(entity);
        if (abilityData == null) return false;
        RealityMarbleAbility marble = abilityData.getEquippedAbility(RealityMarbleAbility.INSTANCE);
        boolean realityMarbleActive = marble != null && marble.isActive();
        boolean mugenNoKenseiActive = marble != null
                && marble.isModeActive(RealityMarbleAbility.Mode.MUGEN_NO_KENSEI);
        boolean infiniteCreationActive = marble != null
                && marble.isModeActive(RealityMarbleAbility.Mode.INFINITE_CREATION_OF_SWORDS);

        boolean allowed;
        switch (mode) {
            case RHO_AIAS:
                // The base Projection is always available, including inside either marble.
                allowed = true;
                break;
            case SECOND:
                allowed = !realityMarbleActive;
                break;
            case THIRD:
                allowed = mugenNoKenseiActive;
                break;
            case FOURTH:
                allowed = infiniteCreationActive;
                break;
            default:
                allowed = false;
                break;
        }
        return allowed;
    }

    /**
     * Selects only an available mode before AltModeComponent announces it. This
     * avoids the transient invalid selection and its chat message entirely.
     */
    private static final class ProjectionAltModeComponent extends AltModeComponent<Mode> {
        private ProjectionAltModeComponent(ProjectionAbility ability) {
            super(ability, Mode.class, Mode.RHO_AIAS);
        }

        @Override
        public void load(CompoundNBT nbt) {
            String storedMode = nbt.getString("currentMode");
            if ("FIRST".equals(storedMode) || "Rho Aias".equals(storedMode)) {
                nbt.putString("currentMode", Mode.RHO_AIAS.name());
            } else if (Mode.SECOND.toString().equals(storedMode)) {
                nbt.putString("currentMode", Mode.SECOND.name());
            } else if (Mode.THIRD.toString().equals(storedMode)
                    || "Legendary Sword: Imagine Victorious Caliburn".equals(storedMode)) {
                nbt.putString("currentMode", Mode.THIRD.name());
            } else if (Mode.FOURTH.toString().equals(storedMode)) {
                nbt.putString("currentMode", Mode.FOURTH.name());
            }
            super.load(nbt);
        }

        @Override
        public void setNextInCycle(LivingEntity entity) {
            ProjectionAbility ability = (ProjectionAbility) getAbility();
            if (ability.chargeComponent.isCharging() || ability.continuousComponent.isContinuous()) return;
            Mode current = this.getCurrentMode();
            Mode next = findNextAvailableMode(entity, current);
            if (next != current) this.setMode(entity, next);
        }
    }

    private void onUse(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        if (this.activeMode == Mode.FOURTH && this.continuousComponent.isContinuous()) {
            this.continuousComponent.stopContinuity(entity);
            return;
        }
        if (this.chargeComponent.isCharging()) return;
        if (this.continuousComponent.isContinuous() && this.activeMode != Mode.RHO_AIAS) return;
        if (!isModeAvailable(entity, this.currentMode)) return;
        switch (this.currentMode) {
            case RHO_AIAS:
                useFirstProjection(entity, ability);
                break;
            case SECOND:
                useSecondProjection(entity, ability);
                break;
            case THIRD:
                useThirdProjection(entity, ability);
                break;
            case FOURTH:
                useFourthProjection(entity, ability);
                break;
            default:
                break;
        }
    }

    private void onAbilityTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        if (!isReplicatingSwords()) clearReplicatedSwords();
        this.counterSwords.removeIf(sword -> {
            if (sword == null || !sword.isAlive()) return true;
            if (sword.tickCount < THIRD_SWORD_LIFETIME) return false;
            sword.remove();
            return true;
        });
    }

    /** Reserved for Projection's first mode. */
    private void useFirstProjection(LivingEntity entity, IAbility ability) {
        // Golden Guard's original 6-second defensive window.
        this.activeMode = Mode.RHO_AIAS;
        this.continuousComponent.triggerContinuity(entity, 120.0F);
    }

    private void startContinuous(LivingEntity entity, IAbility ability) {
        if (this.activeMode == Mode.RHO_AIAS) {
            this.startRhoAias(entity, ability);
        } else if (this.activeMode == Mode.THIRD && entity.level instanceof ServerWorld) {
            for (ServerPlayerEntity player : ((ServerWorld) entity.level).players()) {
                if (entity.distanceToSqr(player) <= THIRD_SHAKE_RADIUS * THIRD_SHAKE_RADIUS) {
                    KaziPacketHandler.sendCameraShake(player, THIRD_SHAKE_TICKS, THIRD_SHAKE_INTENSITY);
                }
            }
        }
    }

    private void tickContinuous(LivingEntity entity, IAbility ability) {
        // activeMode is server-authoritative. Do not let a stale client-side mode
        // alter player movement while continuity packets are being synchronized.
        if (entity.level.isClientSide) return;
        if (this.activeMode == Mode.RHO_AIAS) {
            this.tickRhoAias(entity, ability);
        } else if (this.activeMode == Mode.THIRD) {
            this.counterProjectiles(entity);
        } else if (this.activeMode == Mode.FOURTH) {
            if (!entity.isAlive() || isDisabled() || this.replicatedSwords.size() != REPLICATION_SWORD_COUNT) {
                this.continuousComponent.stopContinuity(entity);
                return;
            }
            if (this.replicationTicks++ % REPLICATION_SHOT_INTERVAL == 0 && this.swordsFired < REPLICATION_SWORD_COUNT) {
                ReplicatedSwordEntity sword = this.replicatedSwords.get(this.swordsFired++);
                if (sword.isAlive()) sword.launch(entity.pick(64.0D, 0.0F, false).getLocation());
            }
        }
    }

    private void endContinuous(LivingEntity entity, IAbility ability) {
        if (this.activeMode == Mode.RHO_AIAS) {
            this.endRhoAias(entity, ability);
        } else if (this.activeMode == Mode.SECOND) {
            this.endGalaxyImpact(entity);
        } else if (this.activeMode == Mode.THIRD) {
            this.cooldownComponent.startCooldown(entity, THIRD_COOLDOWN);
        } else if (this.activeMode == Mode.FOURTH && !entity.level.isClientSide) {
            clearReplicatedSwords();
            this.cooldownComponent.startCooldown(entity, 400.0F);
        }
    }

    /** Golden Guard's guard stance and point-arms animation. */
    private void startRhoAias(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, ModAnimations.POINT_ARMS);
        if (!entity.level.isClientSide) {
            if (this.rhoAiasEntity != null && this.rhoAiasEntity.isAlive()) {
                this.rhoAiasEntity.remove();
            }
            this.rhoAiasEntity = new RhoAiasEntity(KaziEntities.RHO_AIAS.get(), entity.level);
            this.rhoAiasEntity.setOwner(entity);
            entity.level.addFreshEntity(this.rhoAiasEntity);
        }
    }

    /** Maintains the original slow-fall behavior while the shield is active. */
    private void tickRhoAias(LivingEntity entity, IAbility ability) {
        AbilityHelper.slowEntityFall(entity);
    }

    /**
     * Extends Kami-e-style damage cancellation across the entire continuous
     * Rho Aias hold instead of using a short protection window.
     */
    private float onRhoAiasDamageTaken(LivingEntity entity, IAbility ability,
                                       DamageSource damageSource, float damage) {
        if (this.activeMode == Mode.RHO_AIAS && super.isContinuous()) {
            return 0.0F;
        }
        if (this.activeMode == Mode.SECOND
                && this.preventGalaxyImpactFallDamage
                && damageSource == DamageSource.FALL) {
            this.preventGalaxyImpactFallDamage = false;
            return 0.0F;
        }
        return damage;
    }

    /** Rho Aias guard cooldown. */
    private void endRhoAias(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);
        if (!entity.level.isClientSide && this.rhoAiasEntity != null && this.rhoAiasEntity.isAlive()) {
            this.rhoAiasEntity.release();
        }
        this.rhoAiasEntity = null;
        this.cooldownComponent.startCooldown(entity, RHO_AIAS_COOLDOWN);
    }

    /** Galaxy Impact, using Kazi's reduced-damage and reduced-speed projectile clone. */
    private void useSecondProjection(LivingEntity entity, IAbility ability) {
        if (this.chargeComponent.isCharging() || this.continuousComponent.isContinuous()) {
            return;
        }

        this.activeMode = Mode.SECOND;
        this.chargeComponent.startCharging(entity, GALAXY_IMPACT_CHARGE);
    }

    private void startCharge(LivingEntity entity, IAbility ability) {
        if (this.activeMode == Mode.FOURTH) {
            if (!entity.level.isClientSide) {
                clearReplicatedSwords();
                this.replicationTicks = 0;
                this.swordsFired = 0;
                this.replicationChargeTicks = 0;
                spawnReplicatedSword(entity);
            }
            return;
        }
        if (this.activeMode != Mode.SECOND) {
            return;
        }

        if (!entity.level.isClientSide) {
            // The entity overload follows the caster and includes the caster as a listener.
            entity.level.playSound(null, entity, KaziSounds.CALADBOLG_CHARGE_SFX.get(),
                    SoundCategory.PLAYERS, 1.0F, 1.0F);
        }

        this.preventGalaxyImpactFallDamage = true;
        AbilityHelper.setDeltaMovement(entity, entity.getDeltaMovement().x, 2.0D,
                entity.getDeltaMovement().z);
        this.animationComponent.start(entity, ModAnimations.CHARGE_PUNCH);
        entity.addEffect(new EffectInstance((Effect) ModEffects.DIZZY.get(), 30, 0));
    }

    private void tickCharge(LivingEntity entity, IAbility ability) {
        if (this.activeMode == Mode.FOURTH) {
            if (!entity.level.isClientSide && ++this.replicationChargeTicks % 2 == 0
                    && this.replicatedSwords.size() < REPLICATION_SWORD_COUNT) spawnReplicatedSword(entity);
            return;
        }
        if (this.activeMode != Mode.SECOND) {
            return;
        }

        AbilityHelper.slowEntityFall(entity);
        RayTraceResult hit = WyHelper.rayTraceBlocksAndEntities(entity, -0.5D);
        if (this.chargeComponent.getChargeTime() > 35.0F) {
            WyHelper.spawnParticleEffect((ParticleEffect) CartParticleEffects.GALAXY_IMPACT.get(),
                    entity, hit.getLocation().x, hit.getLocation().y, hit.getLocation().z);
        }
    }

    private void endCharge(LivingEntity entity, IAbility ability) {
        if (this.activeMode == Mode.FOURTH) {
            if (!entity.level.isClientSide) {
                while (this.replicatedSwords.size() < REPLICATION_SWORD_COUNT) spawnReplicatedSword(entity);
                this.continuousComponent.startContinuity(entity, REPLICATION_SWORD_COUNT * REPLICATION_SHOT_INTERVAL);
            }
            return;
        }
        if (this.activeMode != Mode.SECOND) {
            return;
        }
        CaladbolgProjectile projectile = new CaladbolgProjectile(entity.level, entity);
        entity.level.addFreshEntity(projectile);
        projectile.shootFromRotation(entity, entity.xRot, entity.yRot,
                0.0F, GALAXY_IMPACT_PROJECTILE_SPEED, 0.0F);
        if (entity.level instanceof ServerWorld) {
            ((ServerWorld) entity.level).getChunkSource().broadcastAndSend(
                    entity, new SAnimateHandPacket(entity, 0));
        }
        this.animationComponent.stop(entity);
        this.cooldownComponent.startCooldown(entity, GALAXY_IMPACT_COOLDOWN);
    }

    private void endGalaxyImpact(LivingEntity entity) {
        this.cooldownComponent.startCooldown(entity, GALAXY_IMPACT_COOLDOWN);
    }

    /**
     * Mugen's projectile counter. A brief four-tick grace period prevents a
     * projectile from being deleted on the exact tick it is fired.
     */
    private void useThirdProjection(LivingEntity entity, IAbility ability) {
        if (this.continuousComponent.isContinuous()) return;
        this.preventGalaxyImpactFallDamage = false;
        this.activeMode = Mode.THIRD;
        this.continuousComponent.triggerContinuity(entity, THIRD_DURATION);
    }

    private void counterProjectiles(LivingEntity entity) {
        // Fixed world-aligned bounds: turning or changing pose cannot resize the counter area.
        AxisAlignedBB counterBox = new AxisAlignedBB(
                entity.getX() - THIRD_COUNTER_HALF_WIDTH, entity.getY() - THIRD_COUNTER_HALF_HEIGHT,
                entity.getZ() - THIRD_COUNTER_HALF_DEPTH,
                entity.getX() + THIRD_COUNTER_HALF_WIDTH, entity.getY() + THIRD_COUNTER_HALF_HEIGHT,
                entity.getZ() + THIRD_COUNTER_HALF_DEPTH);
        List<Entity> nearby = entity.level.getEntities(entity, counterBox,
                ProjectionAbility::isCounterableProjectile);

        for (Entity projectile : nearby) {
            if (!projectile.isAlive()
                    || projectile.tickCount < THIRD_MIN_PROJECTILE_AGE
                    || !isCounterableProjectile(projectile)
                    || isFriendlyProjectile(entity, projectile)) {
                continue;
            }
            spawnCounterSword(entity, projectile);
            spawnCounterImpact(entity, projectile);
            projectile.remove();
        }
    }

    /** Counter projectile entities while preserving the marble's visual swords. */
    private static boolean isCounterableProjectile(Entity projectile) {
        if (projectile instanceof RealityMarbleWeaponEntity) return false;
        return projectile instanceof AbilityProjectileEntity || projectile instanceof ProjectileEntity;
    }

    private static boolean isFriendlyProjectile(LivingEntity caster, Entity projectile) {
        Entity owner = projectile instanceof AbilityProjectileEntity
                ? ((AbilityProjectileEntity) projectile).getThrower() : null;
        if (owner == null && projectile instanceof ProjectileEntity) {
            owner = ((ProjectileEntity) projectile).getOwner();
        }
        if (owner == null) return false;
        return owner == caster || caster.isAlliedTo(owner) || owner.isAlliedTo(caster)
                || (owner instanceof LivingEntity
                    && !ModEntityPredicates.getEnemyFactions(caster).test(owner));
    }

    private void spawnCounterSword(LivingEntity caster, Entity projectile) {
        RealityMarbleWeaponEntity sword = new RealityMarbleWeaponEntity(
                KaziEntities.REALITY_MARBLE_WEAPON.get(), caster.level);
        float yRotation = caster.getRandom().nextFloat() * 360.0F;
        float xRotation = caster.getRandom().nextFloat() * 90.0F - 45.0F;
        float zRotation = 205.0F + caster.getRandom().nextFloat() * 40.0F;
        sword.moveTo(projectile.getX(), projectile.getY(), projectile.getZ(), yRotation, xRotation);
        sword.yRot = yRotation;
        sword.yRotO = yRotation;
        sword.xRot = xRotation;
        sword.xRotO = xRotation;
        sword.setYRotation(yRotation);
        sword.setZRotation(zRotation);
        sword.setWeapon(new ItemStack(Items.DIAMOND_SWORD));
        caster.level.addFreshEntity(sword);
        this.counterSwords.add(sword);
    }

    /** A compact blast and blue electrical discharge based on Caladbolg's palette. */
    private static void spawnCounterImpact(LivingEntity caster, Entity projectile) {
        double x = projectile.getX();
        double y = projectile.getY();
        double z = projectile.getZ();
        caster.level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE,
                SoundCategory.PLAYERS, 0.7F, 1.25F);
        caster.level.playSound(null, x, y, z, SoundEvents.LIGHTNING_BOLT_IMPACT,
                SoundCategory.PLAYERS, 0.8F, 1.35F);

        if (caster.level instanceof ServerWorld) {
            ((ServerWorld) caster.level).sendParticles(ParticleTypes.EXPLOSION,
                    x, y, z, 8, 0.25D, 0.25D, 0.25D, 0.02D);
        }

        LightningDischargeEntity lightning = new LightningDischargeEntity(
                caster, x, y, z, caster.yRot, caster.xRot);
        lightning.setAliveTicks(18);
        lightning.setLightningLength(5.0F);
        lightning.setColor(new Color(35, 145, 255, 150));
        lightning.setOutlineColor(new Color(210, 245, 255, 220));
        lightning.setRenderTransparent();
        lightning.setDetails(12);
        lightning.setDensity(16);
        lightning.setSize(0.8F);
        lightning.setSkipSegments(1);
        caster.level.addFreshEntity(lightning);
    }

    /** Two seconds of portal formation followed by twenty shots over five seconds. */
    private void useFourthProjection(LivingEntity entity, IAbility ability) {
        this.activeMode = Mode.FOURTH;
        this.chargeComponent.startCharging(entity, 40.0F);
    }

    public boolean isReplicatingSwords() {
        return this.activeMode == Mode.FOURTH && !isDisabled()
                && (this.chargeComponent.isCharging() || this.continuousComponent.isContinuous());
    }

    private void spawnReplicatedSword(LivingEntity caster) {
        // A randomized elevated halo leaves the caster's body and sightline clear.
        double angle = caster.getRandom().nextDouble() * Math.PI * 2;
        double radius = 2.5D + caster.getRandom().nextDouble() * 3.5D;
        Vector3d offset = new Vector3d(Math.cos(angle) * radius,
                1.5D + caster.getRandom().nextDouble() * 5.0D, Math.sin(angle) * radius);
        ReplicatedSwordEntity sword = new ReplicatedSwordEntity(caster, offset);
        caster.level.addFreshEntity(sword);
        this.replicatedSwords.add(sword);
    }

    private void clearReplicatedSwords() {
        for (ReplicatedSwordEntity sword : this.replicatedSwords) {
            if (sword.isAlive() && !sword.isFired()) sword.remove();
        }
        this.replicatedSwords.clear();
    }

}
