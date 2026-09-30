package net.kazi.kazimod.abilities.Koku;

import net.kazi.kazimod.abilities.GomuRework.GearFifthRework;
import net.kazi.kazimod.abilities.KamaRework.DomainExpansionMalevolentShrine;
import net.kazi.kazimod.entities.InfiniteVoidBarrierEntity;
import net.kazi.kazimod.events.DomainClashManager;
import net.kazi.kazimod.init.*;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.*;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.server.SStopSoundPacket;
import net.minecraft.particles.*;
import net.minecraft.potion.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.*;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.*;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.entities.SphereEntity;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

import java.awt.Color;
import java.util.*;

public class DomainExpansionInfiniteVoidAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION;
    private static final float CHARGE_TIME      = 100.0f;
    private static final float DOMAIN_DURATION  = 200.0f;
    private static final float MIN_COOLDOWN     = 1200.0f;
    private static final float MAX_COOLDOWN     = 2400.0f;
    private static final float RADIUS           = 30.0f;
    private static final float DAMAGE           = 5.0f;
    private static final int   DAMAGE_INTERVAL  = 40;
    private static final int   STREAK_PHASE_END = 60;
    private static final int   SPARKLE_INTERVAL = 2;
    private static final int   SPARKLE_COUNT    = 20;
    private static final Color SPHERE_COLOR;
    public static final AbilityCore<DomainExpansionInfiniteVoidAbility> INSTANCE;

    private final ChargeComponent     chargeComponent;
    private final ContinuousComponent domainComponent;
    private final AnimationComponent  animationComponent;
    private SphereEntity              visualSphere;
    private InfiniteVoidBarrierEntity barrierEntity;
    private Vector3d                  lockedPosition;
    private int damageTicker;
    private int domainTick;
    private int sparkleTick;
    private int streakTickCounter;
    private final Map<BlockPos, BlockState> replacedBlocks;
    private final Map<BlockPos, BlockState> clashReplacedBlocks;

    // Clash visuals — sphere + barrier shown during clash, no effects applied
    private SphereEntity              clashSphere;
    private InfiniteVoidBarrierEntity clashBarrier;
    private Vector3d                  clashPosition;

    public DomainExpansionInfiniteVoidAbility(final AbilityCore<DomainExpansionInfiniteVoidAbility> core) {
        super(core);
        this.chargeComponent    = new ChargeComponent(this).addStartEvent(this::onChargeStart).addTickEvent(this::onChargeTick).addEndEvent(this::onChargeEnd);
        this.domainComponent    = new ContinuousComponent(this, true).addStartEvent(this::onDomainStart).addTickEvent(this::onDomainTick).addEndEvent(this::onDomainEnd);
        this.animationComponent = new AnimationComponent(this);
        this.visualSphere       = null;
        this.barrierEntity      = null;
        this.lockedPosition     = null;
        this.damageTicker       = 0;
        this.domainTick         = 0;
        this.sparkleTick        = 0;
        this.streakTickCounter  = 0;
        this.replacedBlocks     = new HashMap<>();
        this.clashReplacedBlocks = new HashMap<>();
        this.clashSphere        = null;
        this.clashBarrier       = null;
        this.clashPosition      = null;
        this.isNew = true;
        this.addComponents(this.chargeComponent, this.domainComponent, this.animationComponent);
        this.addCanUseCheck(this::canUseCheck);
        this.addUseEvent(this::onUseEvent);
        this.addTickEvent(this::onAbilityTick);
    }

    private AbilityUseResult canUseCheck(final LivingEntity entity, final IAbility ability) {
        if (DomainClashManager.isInClash(entity.getUUID())) return AbilityUseResult.fail(null);
        final IAbilityData data = AbilityDataCapability.get(entity);
        final HollowPurpleAbility hollowPurple = data.getEquippedAbility(HollowPurpleAbility.INSTANCE);
        if (hollowPurple != null && hollowPurple.isCharging()) return AbilityUseResult.fail(null);
        if (!entity.level.isClientSide) {
            for (final LivingEntity other : entity.level.getEntitiesOfClass(LivingEntity.class,
                    entity.getBoundingBox().inflate(500.0), e -> e != entity && e.isAlive())) {
                final IAbilityData otherData = AbilityDataCapability.get(other);
                if (otherData == null) continue;
                final DomainExpansionMalevolentShrine shrine =
                        otherData.getEquippedAbility(DomainExpansionMalevolentShrine.INSTANCE);
                if (shrine != null && shrine.isDomainActive()) return AbilityUseResult.fail(null);
                final DomainExpansionInfiniteVoidAbility void2 =
                        otherData.getEquippedAbility(DomainExpansionInfiniteVoidAbility.INSTANCE);
                if (void2 != null && void2.isDomainActive()) return AbilityUseResult.fail(null);
            }
        }
        return AbilityUseResult.success();
    }

    private void onUseEvent(final LivingEntity entity, final IAbility ability) {
        if (this.domainComponent.isContinuous()) {
            this.domainComponent.stopContinuity(entity);
        } else if (!this.chargeComponent.isCharging()) {
            this.chargeComponent.startCharging(entity, CHARGE_TIME);
        }
    }

    private void onChargeStart(final LivingEntity entity, final IAbility ability) {
        this.animationComponent.start(entity, KaziAnimations.GOJO_DOMAIN);
        if (!entity.level.isClientSide) {
            entity.level.playSound(null, entity.blockPosition(),
                    KaziSounds.INFINITE_VOID_SFX.get(), SoundCategory.PLAYERS, 1.0f, 1.0f);
            for (final LivingEntity other : entity.level.getEntitiesOfClass(LivingEntity.class,
                    entity.getBoundingBox().inflate(500.0), e -> e != entity && e.isAlive())) {
                final IAbilityData otherData = AbilityDataCapability.get(other);
                if (otherData == null) continue;
                final DomainExpansionMalevolentShrine shrine =
                        otherData.getEquippedAbility(DomainExpansionMalevolentShrine.INSTANCE);
                if (shrine != null && shrine.isCharging()) {
                    DomainClashManager.startClash(entity, other);
                    startClashVisuals(entity);
                    shrine.startClashVisuals(other, entity);
                    this.chargeComponent.stopCharging(entity);
                    this.animationComponent.stop(entity);
                    shrine.stopChargingNoCD(other);
                    // Set clash cooldown — stopCooldown first so startCooldown isn't ignored
                    super.cooldownComponent.stopCooldown(entity);
                    super.cooldownComponent.startCooldown(entity, MAX_COOLDOWN);
                    this.startKokuTechniqueCooldowns(entity, MAX_COOLDOWN * 0.25F);
                    shrine.startCooldownForClash(other);
                    return;
                }
            }
        }
    }

    private void onChargeTick(final LivingEntity entity, final IAbility ability) {}

    private void onChargeEnd(final LivingEntity entity, final IAbility ability) {
        this.animationComponent.stop(entity);
        if (!DomainClashManager.isInClash(entity.getUUID())) {
            this.domainComponent.triggerContinuity(entity, DOMAIN_DURATION);
        }
    }

    // ── Clash visual management ───────────────────────────────────────────────

    public void startClashVisuals(final LivingEntity entity) {
        if (entity.level.isClientSide) return;
        stopClashVisuals(entity);
        this.clashPosition = new Vector3d(entity.getX(), entity.getY(), entity.getZ());

        // The existing collision barrier now renders the imported domain shell.

        this.clashBarrier = new InfiniteVoidBarrierEntity(
                (EntityType<? extends Entity>) KaziEntities.INFINITE_VOID_BARRIER.get(), entity.level);
        this.clashBarrier.setSpawner(entity);
        this.clashBarrier.setRadius(RADIUS);
        this.clashBarrier.yRot = entity.yRot;
        this.clashBarrier.beginVoidExpansion();
        this.clashBarrier.setPos(this.clashPosition.x, this.clashPosition.y, this.clashPosition.z);
        entity.level.addFreshEntity(this.clashBarrier);

        this.replaceBlocksAt(entity, this.clashPosition, this.clashReplacedBlocks);

        entity.level.playSound(null, entity.blockPosition(),
                KaziSounds.INFINITE_VOID_MUSIC_SFX.get(), SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    private void stopClashVisuals(final LivingEntity entity) {
        if (this.clashSphere  != null) { this.clashSphere.remove();  this.clashSphere  = null; }
        if (this.clashBarrier != null) { this.clashBarrier.remove(); this.clashBarrier = null; }
        this.restoreBlocks(entity, this.clashReplacedBlocks);
        this.clashPosition = null;
        stopMusic(entity);
    }

    // Tick event — spawns streak particles while clash is active
    private void onAbilityTick(final LivingEntity entity, final IAbility ability) {
        if (entity.level.isClientSide) return;
        if (this.clashPosition == null) return;
        if (this.clashSphere != null && this.clashSphere.isAlive()) {
            this.clashSphere.setPos(this.clashPosition.x, this.clashPosition.y, this.clashPosition.z);
        }
        if (this.clashBarrier != null && this.clashBarrier.isAlive()) {
            this.clashBarrier.setPos(this.clashPosition.x, this.clashPosition.y, this.clashPosition.z);
        }
        // Clash uses the same imported shell without changing clash resolution.
    }

    // ── Domain events ─────────────────────────────────────────────────────────

    private void onDomainStart(final LivingEntity entity, final IAbility ability) {
        if (entity.level.isClientSide) return;
        this.damageTicker      = 0;
        this.domainTick        = 0;
        this.sparkleTick       = 2;
        this.streakTickCounter = 0;
        this.replacedBlocks.clear();
        this.lockedPosition = new Vector3d(entity.getX(), entity.getY(), entity.getZ());
        (this.barrierEntity = new InfiniteVoidBarrierEntity(
                (EntityType<? extends Entity>) KaziEntities.INFINITE_VOID_BARRIER.get(),
                entity.level)).setSpawner(entity);
        this.barrierEntity.setRadius(RADIUS);
        this.barrierEntity.yRot = entity.yRot;
        this.barrierEntity.beginVoidExpansion();
        this.barrierEntity.setPos(this.lockedPosition.x, this.lockedPosition.y, this.lockedPosition.z);
        entity.level.addFreshEntity(this.barrierEntity);
        entity.level.playSound(null, entity.blockPosition(),
                KaziSounds.INFINITE_VOID_MUSIC_SFX.get(), SoundCategory.PLAYERS, 1.0f, 1.0f);
        this.replaceBlocksAt(entity, this.lockedPosition, this.replacedBlocks);
    }

    private void onDomainTick(final LivingEntity entity, final IAbility ability) {
        if (entity.level.isClientSide) return;
        ++this.damageTicker;
        ++this.domainTick;
        if (this.visualSphere != null && this.visualSphere.isAlive()) {
            this.visualSphere.setPos(this.lockedPosition.x, this.lockedPosition.y, this.lockedPosition.z);
        }
        // Streaks, stars, and the domain core are rendered client-side.

        final AxisAlignedBB domainBox = new AxisAlignedBB(
                this.lockedPosition.x - RADIUS, this.lockedPosition.y - RADIUS, this.lockedPosition.z - RADIUS,
                this.lockedPosition.x + RADIUS, this.lockedPosition.y + RADIUS, this.lockedPosition.z + RADIUS);
        final List<LivingEntity> targets = entity.level.getEntitiesOfClass(LivingEntity.class, domainBox,
                t -> t != entity && t.isAlive() && t.position().distanceTo(this.lockedPosition) <= RADIUS);
        for (final LivingEntity target : targets) {
            target.addEffect(new EffectInstance((Effect) ModEffects.MOVEMENT_BLOCKED.get(), 10, 5, false, false));
            target.addEffect(new EffectInstance((Effect) ModEffects.NO_HANDS.get(), 10, 0, false, false));
            target.addEffect(new EffectInstance((Effect) ModEffects.DIZZY.get(), 10, 0, false, false));
            target.addEffect(new EffectInstance(Effects.BLINDNESS, 10, 3, false, false));
            target.addEffect(new EffectInstance(Effects.DAMAGE_RESISTANCE, 10, 3, false, false));
            if (this.damageTicker >= DAMAGE_INTERVAL) target.hurt(DamageSource.MAGIC, DAMAGE);
        }
        if (this.damageTicker >= DAMAGE_INTERVAL) this.damageTicker = 0;
    }

    private void onDomainEnd(final LivingEntity entity, final IAbility ability) {
        if (!entity.level.isClientSide) {
            this.stopMusic(entity);
            final float cooldown = this.scaledCooldown();
            this.cleanup(entity);
            super.cooldownComponent.startCooldown(entity, cooldown);
            this.startKokuTechniqueCooldowns(entity, cooldown * 0.25F);
        }
        this.animationComponent.stop(entity);
    }

    // ── Clash resolution ──────────────────────────────────────────────────────

    public void resolveClashWin(final LivingEntity entity) {
        stopClashVisuals(entity);
        // Winner: clear the clash cooldown entirely
        super.cooldownComponent.stopCooldown(entity);
        DomainClashManager.clearResolved(entity.getUUID());
        if (entity instanceof ServerPlayerEntity)
            ((ServerPlayerEntity) entity).sendMessage(
                    new StringTextComponent(TextFormatting.GREEN + "You have won the domain clash!"),
                    entity.getUUID());
    }

    public void resolveClashLoss(final LivingEntity entity) {
        stopClashVisuals(entity);
        // FIX: stopCooldown first so startCooldown isn't ignored by the isOnCooldown guard.
        // The clash-start already set MAX_COOLDOWN but startCooldown silently bails if
        // already on cooldown, so the loser's intended MIN_COOLDOWN was never applied.
        super.cooldownComponent.stopCooldown(entity);
        super.cooldownComponent.startCooldown(entity, MAX_COOLDOWN);
        DomainClashManager.clearResolved(entity.getUUID());
        if (entity instanceof ServerPlayerEntity)
            ((ServerPlayerEntity) entity).sendMessage(
                    new StringTextComponent(TextFormatting.RED + "You have lost the domain clash."),
                    entity.getUUID());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void replaceBlocksAt(final LivingEntity entity, final Vector3d centerVec,
                                 final Map<BlockPos, BlockState> targetMap) {
        if (entity.level.isClientSide) return;
        if (centerVec == null) return;
        targetMap.clear();
        final BlockPos center = new BlockPos(centerVec.x, centerVec.y, centerVec.z);
        final int r = (int) RADIUS;
        for (int x = -r; x <= r; ++x) {
            for (int y = -r; y <= r; ++y) {
                for (int z = -r; z <= r; ++z) {
                    if (x * x + y * y + z * z <= r * r) {
                        final BlockPos pos = center.offset(x, y, z);
                        final BlockState state = entity.level.getBlockState(pos);
                        if (!state.isAir() && !state.is(Blocks.BEDROCK) && !state.is(Blocks.BARRIER)
                                && !state.is(Blocks.WATER) && !state.is(Blocks.LAVA)
                                && !state.is(Blocks.OBSIDIAN)) {
                            if (state.getBlock() != KaziBlocks.INFINITE_VOID_FLOOR.get()) {
                                if (state.getDestroySpeed((IBlockReader) entity.level, pos) >= 0.0f) {
                                    targetMap.put(pos.immutable(), state);
                                    entity.level.setBlock(pos,
                                            ((Block) KaziBlocks.INFINITE_VOID_FLOOR.get()).defaultBlockState(), 3);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void restoreBlocks(final LivingEntity entity, final Map<BlockPos, BlockState> targetMap) {
        if (entity.level.isClientSide) return;
        for (final Map.Entry<BlockPos, BlockState> entry : targetMap.entrySet()) {
            final BlockPos pos = entry.getKey();
            if (entity.level.getBlockState(pos).getBlock() == KaziBlocks.INFINITE_VOID_FLOOR.get())
                entity.level.setBlock(pos, entry.getValue(), 3);
        }
        targetMap.clear();
    }

    private void stopMusic(final LivingEntity entity) {
        if (entity.level.isClientSide) return;
        final ResourceLocation musicId = KaziSounds.INFINITE_VOID_MUSIC_SFX.get().getRegistryName();
        final SStopSoundPacket packet = new SStopSoundPacket(musicId, SoundCategory.PLAYERS);
        for (final ServerPlayerEntity player : ((ServerWorld) entity.level).players())
            player.connection.send((IPacket<?>) packet);
    }

    private float scaledCooldown() {
        return MIN_COOLDOWN + MIN_COOLDOWN * Math.min(this.domainTick / DOMAIN_DURATION, 1.0f);
    }

    private void cleanup(final LivingEntity entity) {
        if (this.visualSphere  != null) { this.visualSphere.remove();  this.visualSphere  = null; }
        if (this.barrierEntity != null) { this.barrierEntity.remove(); this.barrierEntity = null; }
        this.restoreBlocks(entity, this.replacedBlocks);
        this.lockedPosition    = null;
        this.damageTicker      = 0;
        this.domainTick        = 0;
        this.sparkleTick       = 0;
        this.streakTickCounter = 0;
    }

    public boolean isDomainActive()       { return this.domainComponent.isContinuous(); }
    @Override public boolean isCharging() { return this.chargeComponent.isCharging(); }

    public void startCooldownForClash(final LivingEntity entity) {
        // Always stop first so the new value isn't swallowed by the isOnCooldown guard
        super.cooldownComponent.stopCooldown(entity);
        super.cooldownComponent.startCooldown(entity, MAX_COOLDOWN);
        this.startKokuTechniqueCooldowns(entity, MAX_COOLDOWN * 0.25F);
    }

    private void startKokuTechniqueCooldowns(final LivingEntity entity, final float duration) {
        final IAbilityData data = AbilityDataCapability.get(entity);
        final AbilityCore<?>[] kokuTechniques = new AbilityCore<?>[]{
                RedAbility.INSTANCE,
                LapseBlueAbility.INSTANCE,
                MaxOutputLapseBlueAbility.INSTANCE,
                HollowPurpleAbility.INSTANCE,
                InfinityAbility.INSTANCE
        };

        for (final AbilityCore<?> core : kokuTechniques) {
            final IAbility kokuAbility = data.getEquippedAbility(core);
            if (kokuAbility == null) continue;
            kokuAbility.getComponent(ModAbilityKeys.COOLDOWN).ifPresent(component -> {
                final CooldownComponent cooldown = (CooldownComponent) component;
                cooldown.stopCooldown(entity);
                cooldown.startCooldown(entity, duration);
            });
        }
    }

    public void stopChargingNoCD(final LivingEntity entity) {
        if (this.chargeComponent.isCharging()) {
            this.chargeComponent.stopCharging(entity);
            this.animationComponent.stop(entity);
        }
    }

    public void stopChargingAndCooldown(final LivingEntity entity) {
        stopChargingNoCD(entity);
        resolveClashLoss(entity);
    }

    public void cleanupClash(final LivingEntity entity) {
        stopClashVisuals(entity);
    }

    private static boolean canUnlock(LivingEntity user) {
        return DevilFruitCapability.get(user).hasAwakenedFruit();
    }

    static {
        DESCRIPTION  = (ITextComponent[]) AbilityHelper.registerDescriptionText("kazimod",
                "domain_expansion_infinite_void",
                (Pair) ImmutablePair.of((Object) "Entities within are overwhelmed by infinite information, leaving them stunned and helpless.",
                        (Object) null));
        SPHERE_COLOR = new Color(0, 0, 0, 255);
        INSTANCE     = new AbilityCore.Builder<DomainExpansionInfiniteVoidAbility>(
                "Domain Expansion: Infinite Void", AbilityCategory.DEVIL_FRUITS,
                DomainExpansionInfiniteVoidAbility::new)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(AbilityDescriptionLine.NEW_LINE,
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        ContinuousComponent.getTooltip(DOMAIN_DURATION),
                        CooldownComponent.getTooltip(MIN_COOLDOWN))
                .setSourceElement(SourceElement.SHOCKWAVE)
                .setSourceType(SourceType.INTERNAL)
                .setUnlockCheck(DomainExpansionInfiniteVoidAbility::canUnlock)
                .build();
    }
}
