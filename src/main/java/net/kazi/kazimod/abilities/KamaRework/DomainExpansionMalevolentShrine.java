package net.kazi.kazimod.abilities.KamaRework;

import java.awt.Color;
import java.util.List;

import net.kazi.kazimod.entities.MalevolentShrineEntity;
import net.kazi.kazimod.init.KaziAnimations;
import net.kazi.kazimod.init.KaziEntities;
import net.kazi.kazimod.init.KaziParticleEffects;
import net.kazi.kazimod.init.KaziSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.server.SStopSoundPacket;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.entities.SphereEntity;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class DomainExpansionMalevolentShrine extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "domain_expansion_malevolent_shrine",
            new Pair[]{ImmutablePair.of("The pinnacle of power", (Object) null)}
    );

    private static final float  COOLDOWN                = 2400.0F;
    private static final float  CHARGE_TIME             = 60.0F;
    private static final float  HOLD_TIME               = 600.0F;
    private static final float  RANGE                   = 150.0F;
    private static final float  DAMAGE                  = 6.0F;
    private static final int    DAMAGE_INTERVAL_TICKS   = 20;
    private static final int    PARTICLE_INTERVAL_TICKS = 5;
    private static final double SPAWN_BEHIND_DISTANCE   = 3.0;
    private static final int    PARTICLE_COUNT          = 4020;

    private static final float MIN_COOLDOWN = 1200.0F;
    private static final float MAX_COOLDOWN = 3600.0F;

    public static final AbilityCore<DomainExpansionMalevolentShrine> INSTANCE;

    // AnimationComponent handles play/stop — same pattern as MountainEaterAbility
    private final AnimationComponent animationComponent = new AnimationComponent(this);

    private final ChargeComponent chargeComponent = new ChargeComponent(this)
            .addStartEvent(this::onChargeStart)
            .addEndEvent(this::onChargeEnd);

    private final ContinuousComponent continuousComponent = new ContinuousComponent(this)
            .addStartEvent(100, this::onStartContinuousEvent)
            .addTickEvent(100, this::onTickContinuousEvent)
            .addEndEvent(100, this::onEndContinuousEvent);

    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent      rangeComponent      = new RangeComponent(this);
    private final CooldownComponent   cooldownComponent   = new CooldownComponent(this);

    private final Interval damageInterval   = new Interval(DAMAGE_INTERVAL_TICKS);
    private final Interval particleInterval = new Interval(PARTICLE_INTERVAL_TICKS);

    private SphereEntity           domainEntity;
    private MalevolentShrineEntity shrineEntity;
    private Vector3d activationPos  = null;
    private int      activeTicks    = 0;
    private boolean  cleanedUpEarly = false;

    public DomainExpansionMalevolentShrine(AbilityCore<DomainExpansionMalevolentShrine> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.animationComponent,   // must be registered as a component
                this.chargeComponent,
                this.continuousComponent,
                this.dealDamageComponent,
                this.rangeComponent,
                this.cooldownComponent
        });
        this.addUseEvent(this::onUseEvent);
    }

    // ── Use ──────────────────────────────────────────────────────────────────

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.continuousComponent.isContinuous()) {
            this.continuousComponent.stopContinuity(entity);
        } else if (!this.chargeComponent.isCharging()) {
            this.chargeComponent.startCharging(entity, CHARGE_TIME);
        }
    }

    // ── Charge phase ──────────────────────────────────────────────────────────

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        // Play the arm-raise animation for the duration of the charge
        this.animationComponent.start(entity, KaziAnimations.SUKUNA_DOMAIN);

        if (!entity.level.isClientSide) {
            entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                    KaziSounds.SHRINE_START_SFX.get(), SoundCategory.PLAYERS, 5.0F, 1.0F);
        }
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        // Stop the charge animation before going active
        this.animationComponent.stop(entity);

        this.damageInterval.restartIntervalToZero();
        this.particleInterval.restartIntervalToZero();
        this.continuousComponent.triggerContinuity(entity, HOLD_TIME);
    }

    // ── Active phase ──────────────────────────────────────────────────────────

    private void onStartContinuousEvent(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        this.activationPos  = new Vector3d(entity.getX(), entity.getY(), entity.getZ());
        this.activeTicks    = 0;
        this.cleanedUpEarly = false;

        this.domainEntity = new SphereEntity(entity.level, entity);
        this.domainEntity.setColor(new Color(139, 0, 0, 80));
        this.domainEntity.setRadius(RANGE);
        this.domainEntity.setDetailLevel(32);
        this.domainEntity.setAnimationSpeed(1);
        entity.level.addFreshEntity(this.domainEntity);

        Vector3d look  = entity.getLookAngle();
        double behindX = entity.getX() - look.x * SPAWN_BEHIND_DISTANCE;
        double behindY = entity.getY();
        double behindZ = entity.getZ() - look.z * SPAWN_BEHIND_DISTANCE;

        this.shrineEntity = new MalevolentShrineEntity(KaziEntities.MALEVOLENT_SHRINE.get(), entity.level);
        this.shrineEntity.moveTo(behindX, behindY, behindZ, entity.yRot + 180.0F, 0.0F);
        this.shrineEntity.yBodyRot = entity.yRot + 180.0F;
        this.shrineEntity.setDeltaMovement(new Vector3d(0, 0, 0));
        this.shrineEntity.setNoGravity(true);
        this.shrineEntity.setInvulnerable(true);
        entity.level.addFreshEntity(this.shrineEntity);

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                KaziSounds.CLEAVE_START_SFX.get(), SoundCategory.PLAYERS, 5.0F, 0.7F);
        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                KaziSounds.SHRINE_MUSIC_SFX.get(), SoundCategory.PLAYERS, 0.25F, 1.0F);
    }

    private void onTickContinuousEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            boolean ownerGone = !entity.isAlive()
                    || (entity instanceof PlayerEntity &&
                    ((ServerWorld) entity.level).getServer().getPlayerList()
                            .getPlayer(entity.getUUID()) == null);

            if (ownerGone) {
                this.cleanedUpEarly = true;
                cleanupEntities(entity);
                this.continuousComponent.stopContinuity(entity);
                return;
            }

            this.activeTicks++;

            Vector3d origin = this.activationPos != null ? this.activationPos
                    : new Vector3d(entity.getX(), entity.getY(), entity.getZ());

            if (particleInterval.canTick()) {
                for (int i = 0; i < PARTICLE_COUNT; i++) {
                    double offsetX = (entity.getRandom().nextDouble() * 2.0 - 1.0) * RANGE;
                    double offsetY = (entity.getRandom().nextDouble() * 2.0 - 1.0) * RANGE;
                    double offsetZ = (entity.getRandom().nextDouble() * 2.0 - 1.0) * RANGE;
                    if (offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ <= RANGE * RANGE) {
                        WyHelper.spawnParticleEffect(
                                (ParticleEffect) KaziParticleEffects.DISMANTLE.get(),
                                entity,
                                origin.x + offsetX,
                                origin.y + offsetY,
                                origin.z + offsetZ
                        );
                    }
                }
            }

            if (damageInterval.canTick()) {
                double r = RANGE;
                AxisAlignedBB searchBox = new AxisAlignedBB(
                        origin.x - r, origin.y - r, origin.z - r,
                        origin.x + r, origin.y + r, origin.z + r
                );

                List<LivingEntity> targets = entity.level.getEntitiesOfClass(
                        LivingEntity.class,
                        searchBox,
                        candidate -> candidate != entity && candidate != this.shrineEntity
                                && candidate.isAlive()
                                && candidate.distanceToSqr(origin.x, origin.y, origin.z) <= r * r
                );

                for (LivingEntity target : targets) {
                    AbilityDamageSource source = (AbilityDamageSource) this.dealDamageComponent.getDamageSource(entity);
                    source.setInternal();
                    source.setSlash();
                    source.markIndirectDamage();

                    if (this.dealDamageComponent.hurtTarget(entity, target, DAMAGE, source)) {
                        WyHelper.spawnParticleEffect(
                                (ParticleEffect) KaziParticleEffects.DISMANTLE.get(),
                                entity,
                                target.getX(),
                                target.getEyeY(),
                                target.getZ()
                        );

                        ((ServerWorld) entity.level).playSound(null, target.blockPosition(),
                                KaziSounds.CLEAVE_HIT_SFX.get(), SoundCategory.PLAYERS, 4.0F, 1.0F);
                    }
                }
            }
        }

        if (this.shrineEntity != null && this.shrineEntity.isAlive()) {
            this.shrineEntity.setDeltaMovement(new Vector3d(0, 0, 0));
        }
    }

    private void onEndContinuousEvent(LivingEntity entity, IAbility ability) {
        int heldTicks = this.activeTicks;
        this.activeTicks = 0;

        cleanupEntities(entity);

        if (!entity.level.isClientSide && !this.cleanedUpEarly) {
            float ratio    = Math.min(1.0F, (float) heldTicks / HOLD_TIME);
            float cooldown = MIN_COOLDOWN + ratio * (MAX_COOLDOWN - MIN_COOLDOWN);
            this.cooldownComponent.startCooldown(entity, cooldown);
        }

        this.cleanedUpEarly = false;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void cleanupEntities(LivingEntity entity) {
        if (this.domainEntity != null) {
            this.domainEntity.remove();
            this.domainEntity = null;
        }

        if (this.shrineEntity != null) {
            this.shrineEntity.remove();
            this.shrineEntity = null;
        }

        this.activationPos = null;

        if (!entity.level.isClientSide) {
            ResourceLocation musicLocation = net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS
                    .getKey(KaziSounds.SHRINE_MUSIC_SFX.get());
            SStopSoundPacket stopPacket = new SStopSoundPacket(musicLocation, SoundCategory.PLAYERS);
            PlayerList playerList = ((ServerWorld) entity.level).getServer().getPlayerList();
            playerList.getPlayers().forEach(player -> player.connection.send(stopPacket));
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Domain Expansion: Malevolent Shrine", AbilityCategory.DEVIL_FRUITS, DomainExpansionMalevolentShrine::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN),
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        ContinuousComponent.getTooltip(HOLD_TIME),
                        DealDamageComponent.getTooltip(DAMAGE),
                        RangeComponent.getTooltip(RANGE, RangeType.AOE)
                })
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceType(new SourceType[]{SourceType.INTERNAL})
                .setSourceElement(SourceElement.SHOCKWAVE)
                .build();
    }
}