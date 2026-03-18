package net.kazi.kazimod.abilities.Gojo;

import net.kazi.kazimod.abilities.KamaRework.DomainExpansionMalevolentShrine;
import net.kazi.kazimod.entities.InfiniteVoidBarrierEntity;
import net.kazi.kazimod.events.DomainClashManager;
import net.kazi.kazimod.init.KaziAnimations;
import net.kazi.kazimod.init.KaziBlocks;
import net.kazi.kazimod.init.KaziEntities;
import net.kazi.kazimod.init.KaziParticleEffects;
import net.kazi.kazimod.init.KaziParticleTypes;
import net.kazi.kazimod.init.KaziSounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SStopSoundPacket;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
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
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.entities.SphereEntity;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.particles.data.SimpleParticleData;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DomainExpansionInfiniteVoidAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "domain_expansion_infinite_void",
            new Pair[]{ImmutablePair.of("Entities within are overwhelmed by infinite information, leaving them stunned and helpless.", (Object) null)}
    );

    private static final float CHARGE_TIME      = 60.0F;
    private static final float DOMAIN_DURATION  = 300.0F;
    private static final float MIN_COOLDOWN     = 60.0F  * 20.0F;
    private static final float MAX_COOLDOWN     = 120.0F * 20.0F;
    private static final float RADIUS           = 30.0F;
    private static final float DAMAGE           = 5.0F;
    private static final int   DAMAGE_INTERVAL  = 40;
    private static final int   STREAK_PHASE_END = 60;
    private static final int   SPARKLE_INTERVAL = 2;
    private static final int   SPARKLE_COUNT    = 20;

    private static final Color SPHERE_COLOR = new Color(0, 0, 0, 255);

    public static final AbilityCore<DomainExpansionInfiniteVoidAbility> INSTANCE;

    private final ChargeComponent chargeComponent =
            (new ChargeComponent(this))
                    .addStartEvent(this::onChargeStart)
                    .addTickEvent(this::onChargeTick)
                    .addEndEvent(this::onChargeEnd);

    private final ContinuousComponent domainComponent =
            (new ContinuousComponent(this, true))
                    .addStartEvent(this::onDomainStart)
                    .addTickEvent(this::onDomainTick)
                    .addEndEvent(this::onDomainEnd);

    private final AnimationComponent animationComponent = new AnimationComponent(this);

    private SphereEntity              visualSphere   = null;
    private InfiniteVoidBarrierEntity barrierEntity  = null;
    private Vector3d                  lockedPosition = null;

    private int damageTicker      = 0;
    private int domainTick        = 0;
    private int sparkleTick       = 0;
    private int streakTickCounter = 0;

    private final Map<BlockPos, BlockState> replacedBlocks = new HashMap<>();

    public DomainExpansionInfiniteVoidAbility(AbilityCore<DomainExpansionInfiniteVoidAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.chargeComponent,
                this.domainComponent,
                this.animationComponent
        });
        this.addCanUseCheck(this::canUseCheck);
        this.addUseEvent(this::onUseEvent);
    }

    private AbilityUseResult canUseCheck(LivingEntity entity, IAbility ability) {
        if (DomainClashManager.isInClash(entity.getUUID())) return AbilityUseResult.fail(null);

        IAbilityData data = AbilityDataCapability.get(entity);
        HollowPurpleAbility hollowPurple =
                (HollowPurpleAbility) data.getEquippedAbility(HollowPurpleAbility.INSTANCE);
        if (hollowPurple != null && hollowPurple.isCharging()) return AbilityUseResult.fail(null);

        if (!entity.level.isClientSide) {
            for (LivingEntity other : entity.level.getEntitiesOfClass(LivingEntity.class,
                    entity.getBoundingBox().inflate(500),
                    e -> e != entity && e.isAlive())) {
                IAbilityData otherData = AbilityDataCapability.get(other);
                DomainExpansionMalevolentShrine shrine = (DomainExpansionMalevolentShrine)
                        otherData.getEquippedAbility(DomainExpansionMalevolentShrine.INSTANCE);
                if (shrine != null && shrine.isDomainActive()) return AbilityUseResult.fail(null);
                DomainExpansionInfiniteVoidAbility void2 = (DomainExpansionInfiniteVoidAbility)
                        otherData.getEquippedAbility(DomainExpansionInfiniteVoidAbility.INSTANCE);
                if (void2 != null && void2.isDomainActive()) return AbilityUseResult.fail(null);
            }
        }
        return AbilityUseResult.success();
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.domainComponent.isContinuous()) {
            this.domainComponent.stopContinuity(entity);
        } else if (!this.chargeComponent.isCharging()) {
            this.chargeComponent.startCharging(entity, CHARGE_TIME);
        }
    }

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, KaziAnimations.GOJO_DOMAIN);
        if (!entity.level.isClientSide) {
            entity.level.playSound(null, entity.blockPosition(),
                    KaziSounds.INFINITE_VOID_SFX.get(), SoundCategory.PLAYERS, 1.0F, 1.0F);
            for (LivingEntity other : entity.level.getEntitiesOfClass(LivingEntity.class,
                    entity.getBoundingBox().inflate(500),
                    e -> e != entity && e.isAlive())) {
                IAbilityData otherData = AbilityDataCapability.get(other);
                DomainExpansionMalevolentShrine shrine = (DomainExpansionMalevolentShrine)
                        otherData.getEquippedAbility(DomainExpansionMalevolentShrine.INSTANCE);
                if (shrine != null && shrine.isCharging()) {
                    DomainClashManager.startClash(entity, other);
                    this.chargeComponent.stopCharging(entity);
                    this.animationComponent.stop(entity);
                    shrine.stopChargingNoCD(other);
                    return;
                }
            }
        }
    }

    private void onChargeTick(LivingEntity entity, IAbility ability) {}

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);
        if (!DomainClashManager.isInClash(entity.getUUID())) {
            this.domainComponent.triggerContinuity(entity, DOMAIN_DURATION);
        }
    }

    private void onDomainStart(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        damageTicker = 0; domainTick = 0; sparkleTick = SPARKLE_INTERVAL; streakTickCounter = 0;
        replacedBlocks.clear();
        lockedPosition = new Vector3d(entity.getX(), entity.getY(), entity.getZ());

        visualSphere = new SphereEntity(entity.level, entity);
        visualSphere.setColor(SPHERE_COLOR);
        visualSphere.setRadius(RADIUS);
        visualSphere.setDetailLevel(32);
        visualSphere.setAnimationSpeed(1);
        visualSphere.setPos(lockedPosition.x, lockedPosition.y, lockedPosition.z);
        entity.level.addFreshEntity(visualSphere);

        barrierEntity = new InfiniteVoidBarrierEntity(KaziEntities.INFINITE_VOID_BARRIER.get(), entity.level);
        barrierEntity.setSpawner(entity);
        barrierEntity.setRadius(RADIUS);
        barrierEntity.setPos(lockedPosition.x, lockedPosition.y, lockedPosition.z);
        entity.level.addFreshEntity(barrierEntity);

        entity.level.playSound(null, entity.blockPosition(),
                KaziSounds.INFINITE_VOID_MUSIC_SFX.get(), SoundCategory.PLAYERS, 1.0F, 1.0F);
        replaceBlocks(entity);
    }

    private void replaceBlocks(LivingEntity entity) {
        if (entity.level.isClientSide) return;
        BlockPos center = new BlockPos(lockedPosition.x, lockedPosition.y, lockedPosition.z);
        int r = (int) RADIUS;
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + y * y + z * z > r * r) continue;
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = entity.level.getBlockState(pos);
                    if (state.isAir()) continue;
                    if (state.is(Blocks.BEDROCK) || state.is(Blocks.BARRIER)
                            || state.is(Blocks.COMMAND_BLOCK) || state.is(Blocks.CHAIN_COMMAND_BLOCK)
                            || state.is(Blocks.REPEATING_COMMAND_BLOCK) || state.is(Blocks.END_PORTAL_FRAME)
                            || state.getBlock() == KaziBlocks.INFINITE_VOID_FLOOR.get()) continue;
                    if (state.getDestroySpeed(entity.level, pos) < 0) continue;
                    replacedBlocks.put(pos.immutable(), state);
                    entity.level.setBlock(pos, KaziBlocks.INFINITE_VOID_FLOOR.get().defaultBlockState(), 3);
                }
            }
        }
    }

    private void restoreBlocks(LivingEntity entity) {
        if (entity.level.isClientSide) return;
        for (Map.Entry<BlockPos, BlockState> entry : replacedBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            if (entity.level.getBlockState(pos).getBlock() == KaziBlocks.INFINITE_VOID_FLOOR.get())
                entity.level.setBlock(pos, entry.getValue(), 3);
        }
        replacedBlocks.clear();
    }

    private void stopMusic(LivingEntity entity) {
        if (entity.level.isClientSide) return;
        ResourceLocation musicId = KaziSounds.INFINITE_VOID_MUSIC_SFX.get().getRegistryName();
        SStopSoundPacket packet = new SStopSoundPacket(musicId, SoundCategory.PLAYERS);
        for (ServerPlayerEntity player : ((ServerWorld) entity.level).players())
            player.connection.send(packet);
    }

    private float scaledCooldown() {
        float progress = Math.min((float) domainTick / DOMAIN_DURATION, 1.0F);
        return MIN_COOLDOWN + (MAX_COOLDOWN - MIN_COOLDOWN) * progress;
    }

    private void onDomainTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        damageTicker++;
        domainTick++;

        if (visualSphere != null && visualSphere.isAlive())
            visualSphere.setPos(lockedPosition.x, lockedPosition.y, lockedPosition.z);

        ServerWorld sw = (ServerWorld) entity.level;

        // ── Phase 2 — void orb + sparkles ────────────────────────────────────
        if (domainTick > STREAK_PHASE_END) {
            WyHelper.spawnParticleEffect(
                    (ParticleEffect) KaziParticleEffects.INFINITE_VOID.get(),
                    entity, lockedPosition.x, lockedPosition.y + 1.0, lockedPosition.z);

            sparkleTick++;
            if (sparkleTick >= SPARKLE_INTERVAL) {
                sparkleTick = 0;
                for (int i = 0; i < SPARKLE_COUNT; i++) {
                    double rx = lockedPosition.x + (entity.level.random.nextDouble() * 2 - 1) * RADIUS * 0.8;
                    double ry = lockedPosition.y + (entity.level.random.nextDouble() * 2 - 1) * RADIUS * 0.8;
                    double rz = lockedPosition.z + (entity.level.random.nextDouble() * 2 - 1) * RADIUS * 0.8;
                    double dist = Math.sqrt(
                            Math.pow(rx - lockedPosition.x, 2) +
                                    Math.pow(ry - lockedPosition.y, 2) +
                                    Math.pow(rz - lockedPosition.z, 2));
                    if (dist > RADIUS) continue;
                    for (ServerPlayerEntity player : sw.players())
                        sw.sendParticles(player, net.minecraft.particles.ParticleTypes.END_ROD,
                                true, rx, ry, rz, 1, 0.0, 0.05, 0.0, 0.08);
                }
            }
        }

        // ── Effects on targets ────────────────────────────────────────────────
        AxisAlignedBB domainBox = new AxisAlignedBB(
                lockedPosition.x - RADIUS, lockedPosition.y - RADIUS, lockedPosition.z - RADIUS,
                lockedPosition.x + RADIUS, lockedPosition.y + RADIUS, lockedPosition.z + RADIUS);
        List<LivingEntity> targets = entity.level.getEntitiesOfClass(LivingEntity.class, domainBox,
                t -> t != entity && t.isAlive() && t.position().distanceTo(lockedPosition) <= RADIUS);
        for (LivingEntity target : targets) {
            target.addEffect(new EffectInstance((Effect) ModEffects.MOVEMENT_BLOCKED.get(), 10, 5, false, false));
            target.addEffect(new EffectInstance((Effect) ModEffects.NO_HANDS.get(), 10, 0, false, false));
            target.addEffect(new EffectInstance((Effect) ModEffects.DIZZY.get(), 10, 0, false, false));
            target.addEffect(new EffectInstance(Effects.DAMAGE_RESISTANCE, 10, 3, false, false));
            if (damageTicker >= DAMAGE_INTERVAL) target.hurt(DamageSource.MAGIC, DAMAGE);
        }
        if (damageTicker >= DAMAGE_INTERVAL) damageTicker = 0;
    }

    private void onDomainEnd(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            stopMusic(entity);
            float cooldown = scaledCooldown();
            cleanup(entity);
            super.cooldownComponent.startCooldown(entity, cooldown);
        }
        this.animationComponent.stop(entity);
    }

    private void cleanup(LivingEntity entity) {
        if (visualSphere != null)  { visualSphere.remove();  visualSphere  = null; }
        if (barrierEntity != null) { barrierEntity.remove(); barrierEntity = null; }
        restoreBlocks(entity);
        lockedPosition = null;
        damageTicker = 0; domainTick = 0; sparkleTick = 0; streakTickCounter = 0;
    }

    public boolean isDomainActive() { return this.domainComponent.isContinuous(); }
    public boolean isCharging()     { return this.chargeComponent.isCharging(); }

    public void stopChargingNoCD(LivingEntity entity) {
        if (this.chargeComponent.isCharging()) {
            this.chargeComponent.stopCharging(entity);
            this.animationComponent.stop(entity);
        }
    }

    public void stopChargingAndCooldown(LivingEntity entity) {
        stopChargingNoCD(entity);
        resolveClashLoss(entity);
    }

    public void resolveClashWin(LivingEntity entity) {
        if (entity instanceof ServerPlayerEntity)
            ((ServerPlayerEntity) entity).sendMessage(
                    new StringTextComponent(TextFormatting.GREEN + "You have won the domain clash!"),
                    entity.getUUID());
    }

    public void resolveClashLoss(LivingEntity entity) {
        if (entity instanceof ServerPlayerEntity)
            ((ServerPlayerEntity) entity).sendMessage(
                    new StringTextComponent(TextFormatting.RED + "You have lost the domain clash."),
                    entity.getUUID());
        super.cooldownComponent.startCooldown(entity, MAX_COOLDOWN);
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Domain Expansion: Infinite Void",
                AbilityCategory.DEVIL_FRUITS, DomainExpansionInfiniteVoidAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        ContinuousComponent.getTooltip(DOMAIN_DURATION),
                        CooldownComponent.getTooltip(MIN_COOLDOWN)
                })
                .setSourceElement(SourceElement.SHOCKWAVE)
                .setSourceType(new SourceType[]{SourceType.INTERNAL})
                .build();
    }
}