package net.kazi.kazimod.abilities.KamaRework;

import java.awt.Color;
import java.util.List;

import net.kazi.kazimod.abilities.Koku.DomainExpansionInfiniteVoidAbility;
import net.kazi.kazimod.entities.MalevolentShrineEntity;
import net.kazi.kazimod.events.DomainClashManager;
import net.kazi.kazimod.init.KaziAnimations;
import net.kazi.kazimod.init.KaziEntities;
import net.kazi.kazimod.init.KaziParticleEffects;
import net.kazi.kazimod.init.KaziSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SStopSoundPacket;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUseResult;
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
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.entities.SphereEntity;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class DomainExpansionMalevolentShrine extends Ability {

    private static final ITextComponent[] DESCRIPTION =
            xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper.registerDescriptionText(
                    "kazimod", "domain_expansion_malevolent_shrine",
                    new Pair[]{ImmutablePair.of("The pinnacle of power", (Object) null)});

    private static final float  COOLDOWN                = 2400.0F;
    private static final float  CHARGE_TIME             = 100.0F;
    private static final float  HOLD_TIME               = 400.0F;
    private static final float  RANGE                   = 150.0F;
    private static final float  DAMAGE                  = 3.0F;
    private static final int    DAMAGE_INTERVAL_TICKS   = 20;
    private static final int    PARTICLE_INTERVAL_TICKS = 5;
    private static final double SPAWN_BEHIND_DISTANCE   = 3.0;
    private static final int    PARTICLE_COUNT          = 4020;
    private static final float  MIN_COOLDOWN            = 1200.0F;
    private static final float  MAX_COOLDOWN            = 3000.0F;

    public static final AbilityCore<DomainExpansionMalevolentShrine> INSTANCE;

    private final AnimationComponent  animationComponent  = new AnimationComponent(this);
    private final ChargeComponent     chargeComponent     = new ChargeComponent(this)
            .addStartEvent(this::onChargeStart)
            .addTickEvent(this::onChargeTick)
            .addEndEvent(this::onChargeEnd);
    private final ContinuousComponent continuousComponent = new ContinuousComponent(this)
            .addStartEvent(100, this::onStartContinuousEvent)
            .addTickEvent(100, this::onTickContinuousEvent)
            .addEndEvent(100, this::onEndContinuousEvent);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent      rangeComponent      = new RangeComponent(this);
    private final CooldownComponent   cooldownComponent   = new CooldownComponent(this)
            .addEndEvent(this::onCooldownEnd);

    private final Interval damageInterval   = new Interval(DAMAGE_INTERVAL_TICKS);
    private final Interval particleInterval = new Interval(PARTICLE_INTERVAL_TICKS);

    private net.kazi.kazimod.entities.KamaVfxEntity constructionVisual;
    private SphereEntity           domainEntity      = null;
    private MalevolentShrineEntity shrineEntity      = null;
    private MalevolentShrineEntity clashShrineEntity = null;
    private Vector3d               activationPos     = null;
    private int                    activeTicks       = 0;
    private boolean                cleanedUpEarly    = false;

    public DomainExpansionMalevolentShrine(AbilityCore<DomainExpansionMalevolentShrine> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.animationComponent, this.chargeComponent, this.continuousComponent,
                this.dealDamageComponent, this.rangeComponent, this.cooldownComponent});
        this.addCanUseCheck(this::canUseCheck);
        this.addUseEvent(this::onUseEvent);
        this.addTickEvent(this::onClashTick);
        this.addRemoveEvent((entity, ability) -> stopConstructionVisual());
    }

    private AbilityUseResult canUseCheck(LivingEntity entity, IAbility ability) {
        if (DomainClashManager.isInClash(entity.getUUID())) return AbilityUseResult.fail(null);
        if (!entity.level.isClientSide) {
            for (LivingEntity other : entity.level.getEntitiesOfClass(LivingEntity.class,
                    entity.getBoundingBox().inflate(500), e -> e != entity && e.isAlive())) {
                IAbilityData otherData = AbilityDataCapability.get(other);
                if (otherData == null) continue;
                DomainExpansionInfiniteVoidAbility va =
                        (DomainExpansionInfiniteVoidAbility) otherData.getEquippedAbility(
                                DomainExpansionInfiniteVoidAbility.INSTANCE);
                if (va != null && va.isDomainActive()) return AbilityUseResult.fail(null);
                DomainExpansionMalevolentShrine s2 =
                        (DomainExpansionMalevolentShrine) otherData.getEquippedAbility(INSTANCE);
                if (s2 != null && s2.isDomainActive()) return AbilityUseResult.fail(null);
            }
        }
        return AbilityUseResult.success();
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.continuousComponent.isContinuous()) {
            this.continuousComponent.stopContinuity(entity);
        } else if (!this.chargeComponent.isCharging()) {
            this.chargeComponent.startCharging(entity, CHARGE_TIME);
        }
    }

    private void onClashTick(LivingEntity entity, IAbility ability) {
        if (!this.chargeComponent.isCharging()) stopConstructionVisual();
        if (entity.level.isClientSide) return;
        if (!DomainClashManager.isInClash(entity.getUUID())) return;
        if (clashShrineEntity != null && clashShrineEntity.isAlive()) {
            clashShrineEntity.setDeltaMovement(Vector3d.ZERO);
        }
    }

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, KaziAnimations.SUKUNA_DOMAIN);

        if (!entity.level.isClientSide) {
            stopConstructionVisual();
            this.constructionVisual = net.kazi.kazimod.entities.KamaVfxEntity.shrineCharge(
                    entity, ability, (int) CHARGE_TIME, (float) SPAWN_BEHIND_DISTANCE);
            entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                    KaziSounds.SHRINE_START_SFX.get(), SoundCategory.PLAYERS, 5.0F, 1.0F);

            for (LivingEntity other : entity.level.getEntitiesOfClass(LivingEntity.class,
                    entity.getBoundingBox().inflate(500), e -> e != entity && e.isAlive())) {
                IAbilityData otherData = AbilityDataCapability.get(other);
                if (otherData == null) continue;
                DomainExpansionInfiniteVoidAbility voidAbility =
                        (DomainExpansionInfiniteVoidAbility) otherData.getEquippedAbility(
                                DomainExpansionInfiniteVoidAbility.INSTANCE);
                if (voidAbility != null && voidAbility.isCharging()) {
                    DomainClashManager.startClash(entity, other);
                    voidAbility.startClashVisuals(other);
                    this.startClashVisuals(entity, other);

                    // FIX: stopCooldown first so startCooldown isn't silently ignored
                    this.cooldownComponent.stopCooldown(entity);
                    this.cooldownComponent.startCooldown(entity, COOLDOWN);
                    this.startKamaTechniqueCooldowns(entity, COOLDOWN * 0.5F);
                    voidAbility.startCooldownForClash(other);

                    this.chargeComponent.stopCharging(entity);
                    this.animationComponent.stop(entity);
                    voidAbility.stopChargingNoCD(other);
                    return;
                }
            }
        }
    }

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        if (this.constructionVisual != null) this.constructionVisual.refresh(this.chargeComponent.getChargePercentage());
    }

    private void stopConstructionVisual() {
        if (this.constructionVisual != null) this.constructionVisual.remove();
        this.constructionVisual = null;
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        stopConstructionVisual();
        this.animationComponent.stop(entity);
        if (!DomainClashManager.isInClash(entity.getUUID())) {
            this.damageInterval.restartIntervalToZero();
            this.particleInterval.restartIntervalToZero();
            this.continuousComponent.triggerContinuity(entity, HOLD_TIME);
        }
    }

    private void onCooldownEnd(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);
        if (this.chargeComponent.isCharging()) {
            this.chargeComponent.forceStopCharging(entity);
        }
        if (this.continuousComponent.isContinuous()) {
            this.continuousComponent.stopContinuity(entity);
        }
        this.activeTicks = 0;
        this.cleanedUpEarly = false;
    }

    private void onStartContinuousEvent(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        this.activationPos  = new Vector3d(entity.getX(), entity.getY(), entity.getZ());
        this.activeTicks    = 0;
        this.cleanedUpEarly = false;

        // Imported slashes and sky tint replace the old translucent sphere.

        Vector3d look = entity.getLookAngle();
        this.shrineEntity = new MalevolentShrineEntity(KaziEntities.MALEVOLENT_SHRINE.get(), entity.level);
        this.shrineEntity.moveTo(
                entity.getX() - look.x * SPAWN_BEHIND_DISTANCE,
                entity.getY(),
                entity.getZ() - look.z * SPAWN_BEHIND_DISTANCE,
                entity.yRot + 180.0F, 0.0F);
        this.shrineEntity.yBodyRot = entity.yRot + 180.0F;
        this.shrineEntity.setDeltaMovement(Vector3d.ZERO);
        this.shrineEntity.setNoGravity(true);
        this.shrineEntity.setInvulnerable(true);
        this.shrineEntity.activateDomainVisual(RANGE, this.activationPos);
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

            // The domain mesh supplies its ambient slashes; damage below is unchanged.

            if (damageInterval.canTick()) {
                double r = RANGE;
                AxisAlignedBB box = new AxisAlignedBB(
                        origin.x - r, origin.y - r, origin.z - r,
                        origin.x + r, origin.y + r, origin.z + r);
                List<LivingEntity> targets = entity.level.getEntitiesOfClass(
                        LivingEntity.class, box,
                        c -> c != entity && c != shrineEntity && c.isAlive()
                                && c.distanceToSqr(origin.x, origin.y, origin.z) <= r * r);
                for (LivingEntity target : targets) {
                    AbilityDamageSource source =
                            (AbilityDamageSource) dealDamageComponent.getDamageSource(entity);
                    source.setInternal();
                    source.setSlash();
                    source.markIndirectDamage();
                    if (dealDamageComponent.hurtTarget(entity, target, DAMAGE, source)) {
                        net.kazi.kazimod.entities.KamaVfxEntity.slash(entity, target.getX(), target.getEyeY(), target.getZ());
                        ((ServerWorld) entity.level).playSound(null, target.blockPosition(),
                                KaziSounds.CLEAVE_HIT_SFX.get(), SoundCategory.PLAYERS, 4.0F, 1.0F);
                    }
                }
            }
        }
        if (shrineEntity != null && shrineEntity.isAlive()) {
            shrineEntity.setDeltaMovement(Vector3d.ZERO);
        }
    }

    private void onEndContinuousEvent(LivingEntity entity, IAbility ability) {
        int held = this.activeTicks;
        this.activeTicks = 0;
        cleanupEntities(entity);
        if (!entity.level.isClientSide && !this.cleanedUpEarly) {
            float ratio    = Math.min(1.0F, (float) held / HOLD_TIME);
            float cooldown = MIN_COOLDOWN + ratio * (MAX_COOLDOWN - MIN_COOLDOWN);
            this.cooldownComponent.stopCooldown(entity);
            this.cooldownComponent.startCooldown(entity, cooldown);
            this.startKamaTechniqueCooldowns(entity, cooldown * 0.5F);
        }
        this.cleanedUpEarly = false;
    }

    private void cleanupEntities(LivingEntity entity) {
        if (domainEntity      != null) { domainEntity.remove();      domainEntity      = null; }
        if (shrineEntity      != null) { shrineEntity.remove();      shrineEntity      = null; }
        cleanupClash(entity);
        activationPos = null;
        if (!entity.level.isClientSide) {
            ResourceLocation loc = net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS
                    .getKey(KaziSounds.SHRINE_MUSIC_SFX.get());
            SStopSoundPacket pkt = new SStopSoundPacket(loc, SoundCategory.PLAYERS);
            PlayerList pl = ((ServerWorld) entity.level).getServer().getPlayerList();
            pl.getPlayers().forEach(p -> p.connection.send(pkt));
        }
    }

    private void sendMessage(LivingEntity entity, String text) {
        if (entity instanceof ServerPlayerEntity) {
            ((ServerPlayerEntity) entity).sendMessage(
                    new StringTextComponent(text), entity.getUUID());
        }
    }

    public boolean isDomainActive() { return continuousComponent.isContinuous(); }
    public boolean isCharging()     { return chargeComponent.isCharging(); }

    /**
     * Called by void ability when a clash starts so shrine is also on cooldown.
     * FIX: stopCooldown first so the new value isn't silently ignored.
     */
    public void startCooldownForClash(LivingEntity entity) {
        this.cooldownComponent.stopCooldown(entity);
        this.cooldownComponent.startCooldown(entity, COOLDOWN);
        this.startKamaTechniqueCooldowns(entity, COOLDOWN * 0.5F);
    }

    private void startKamaTechniqueCooldowns(LivingEntity entity, float duration) {
        IAbilityData data = AbilityDataCapability.get(entity);
        AbilityCore<?>[] kamaTechniques = new AbilityCore<?>[]{
                DismantleAbility.INSTANCE,
                CleaveAbility.INSTANCE,
                SpiderwebCleaveAbility.INSTANCE,
                FugaAbility.INSTANCE
        };

        for (AbilityCore<?> core : kamaTechniques) {
            IAbility kamaAbility = data.getEquippedAbility(core);
            if (kamaAbility == null) continue;
            kamaAbility.getComponent(ModAbilityKeys.COOLDOWN).ifPresent(component -> {
                CooldownComponent cooldown = (CooldownComponent) component;
                cooldown.stopCooldown(entity);
                cooldown.startCooldown(entity, duration);
            });
        }
    }

    public void stopChargingNoCD(LivingEntity entity) {
        if (this.chargeComponent.isCharging()) {
            this.chargeComponent.stopCharging(entity);
            this.animationComponent.stop(entity);
        }
    }

    public void resolveClashWin(LivingEntity entity) {
        cleanupClash(entity);
        // Winner: clear cooldown entirely
        this.cooldownComponent.stopCooldown(entity);
        DomainClashManager.clearResolved(entity.getUUID());
        sendMessage(entity, TextFormatting.GREEN + "You have won the domain clash!");
    }

    public void resolveClashLoss(LivingEntity entity) {
        if (clashShrineEntity != null) { clashShrineEntity.remove(); clashShrineEntity = null; }
        // FIX: stopCooldown first — startCooldown is ignored if already on cooldown.
        // Without this, the loser stays locked at the COOLDOWN set during clash start
        // and the MIN_COOLDOWN intended here was silently swallowed.
        this.cooldownComponent.stopCooldown(entity);
        this.cooldownComponent.startCooldown(entity, MIN_COOLDOWN);
        DomainClashManager.clearResolved(entity.getUUID());
        sendMessage(entity, TextFormatting.RED + "You have lost the domain clash.");
    }

    public void stopChargingAndCooldown(LivingEntity entity) {
        stopChargingNoCD(entity);
        resolveClashLoss(entity);
    }

    public void startClashVisuals(LivingEntity entity, LivingEntity voidUser) {
        stopConstructionVisual();
        if (entity.level.isClientSide) return;
        cleanupClash(entity);

        Vector3d look = entity.getLookAngle();
        this.clashShrineEntity = new MalevolentShrineEntity(
                KaziEntities.MALEVOLENT_SHRINE.get(), entity.level);
        this.clashShrineEntity.moveTo(
                entity.getX() - look.x * SPAWN_BEHIND_DISTANCE,
                entity.getY(),
                entity.getZ() - look.z * SPAWN_BEHIND_DISTANCE,
                entity.yRot + 180.0F, 0.0F);
        this.clashShrineEntity.yBodyRot = entity.yRot + 180.0F;
        this.clashShrineEntity.setDeltaMovement(Vector3d.ZERO);
        this.clashShrineEntity.setNoGravity(true);
        this.clashShrineEntity.setInvulnerable(true);
        this.clashShrineEntity.activateDomainVisual(RANGE, entity.position());
        entity.level.addFreshEntity(this.clashShrineEntity);

        if (voidUser != null && voidUser.isAlive()) {
            double barrierRadius = 30.0;
            double distToVoidUser = entity.distanceTo(voidUser);
            if (distToVoidUser > barrierRadius) {
                Vector3d toVoid = voidUser.position().subtract(entity.position());
                if (toVoid.lengthSqr() > 0.001D) {
                    toVoid = toVoid.normalize();
                    double spawnDist = barrierRadius * 0.75;
                    entity.teleportTo(
                            voidUser.getX() - toVoid.x * spawnDist,
                            voidUser.getY(),
                            voidUser.getZ() - toVoid.z * spawnDist);
                }
            }
        }
    }

    public void cleanupClash(LivingEntity entity) {
        if (clashShrineEntity != null) { clashShrineEntity.remove(); clashShrineEntity = null; }
    }

    private static boolean canUnlock(LivingEntity user) {
        return DevilFruitCapability.get(user).hasAwakenedFruit();
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Domain Expansion: Malevolent Shrine",
                AbilityCategory.DEVIL_FRUITS, DomainExpansionMalevolentShrine::new))
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
                .setUnlockCheck(DomainExpansionMalevolentShrine::canUnlock)
                .build();
    }
}
