package net.kazi.kazimod.abilities.SupaRework;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.kazi.kazimod.entities.RealityMarbleGearEntity;
import net.kazi.kazimod.entities.RealityMarbleWeaponEntity;
import net.kazi.kazimod.entities.UnlimitedLostWorksEntity;
import net.kazi.kazimod.effects.LostWorksFruitLockEffect;
import net.kazi.kazimod.init.KaziBlocks;
import net.kazi.kazimod.init.KaziEntities;
import net.kazi.kazimod.init.KaziPacketHandler;
import net.kazi.kazimod.network.RealityMarbleMusicPacket;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;
import xyz.pixelatedw.mineminenomi.entities.SphereEntity;

public class RealityMarbleAbility extends Ability {
    private static final double CHARGE_MUSIC_RANGE_SQR = 150.0D * 150.0D;
    public enum Mode {
        MUGEN_NO_KENSEI("Universe of Endless Blades: Mugen no Kensei"),
        INFINITE_CREATION_OF_SWORDS("Unlimited Blade Works: Infinite Creation of Swords"),
        VOID_CREATION_OF_SWORDS("Unlimited Lost Works");

        private final String displayName;

        Mode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return this.displayName;
        }
    }

    private static final int HORIZONTAL_RADIUS = 90;
    private static final int REPLACEMENT_MIN_Y_OFFSET = -5;
    private static final int REPLACEMENT_MAX_Y_OFFSET = 10;
    private static final int WEAPON_MIN_Y_OFFSET = -10;
    private static final int WEAPON_MAX_Y_OFFSET = 10;
    // 25% slower than the original 1024-block batch.
    private static final int BLOCKS_PER_TICK = 768;
    private static final int RESTORE_BLOCKS_PER_TICK = 768;
    private static final int WEAPON_DISPLAY_COUNT = 96;
    private static final int GEAR_DISPLAY_COUNT = 5;
    private static final int WEAPON_SPAWN_AREA_SIZE = 95;
    /** All Reality Marble weapon and gear displays keep this much horizontal clearance. */
    private static final double DISPLAY_MIN_HORIZONTAL_SPACING = 5.0D;
    /** FIRST and SECOND use Malevolent Shrine's visual sphere at this radius. */
    private static final float MARBLE_SPHERE_RADIUS = 90.0F;
    // Round the longest soundtrack's duration up to a 160-second hold.
    private static final float MARBLE_HOLD_TICKS = 160.0F * 20.0F;
    private static final float MARBLE_MIN_COOLDOWN = 30.0F * 20.0F;
    private static final float FIRST_COOLDOWN = 300.0F * 20.0F;
    private static final float SECOND_COOLDOWN = 300.0F * 20.0F;
    private static final float THIRD_COOLDOWN = 50.0F * 20.0F;
    private static final float FIRST_CHARGE = 6.0F * 20.0F;
    private static final float SECOND_CHARGE = 6.0F * 20.0F;
    private static final Map<UUID, ActiveMarble> ACTIVE_MARBLES = new HashMap<>();
    private static final Map<UUID, LogoutRestoration> PENDING_LOGOUT_RESTORATIONS = new HashMap<>();

    private static final ResourceLocation FIRST_MODE_ICON =
            new ResourceLocation("kazimod", "textures/abilities/supa_mugen_no_kensei.png");
    private static final ResourceLocation SECOND_MODE_ICON =
            new ResourceLocation("kazimod", "textures/abilities/supa_infinite_creation.png");
    private static final ResourceLocation THIRD_MODE_ICON =
            new ResourceLocation("kazimod", "textures/abilities/supa_lost_works.png");
    private static final ResourceLocation BLUE_MARBLE_SKY =
            new ResourceLocation("kazimod", "textures/skyboxes/reality_marble_blue_sky.png");
    private static final ResourceLocation ORANGE_MARBLE_SKY =
            new ResourceLocation("kazimod", "textures/skyboxes/reality_marble_orange_sky.png");
    private static final TranslationTextComponent REALITY_MARBLE_NAME = new TranslationTextComponent(
            WyRegistry.registerName("ability.kazimod.reality_marble", "Reality Marble"));
    private static final Pair<String, Object[]>[] DESCRIPTION = new Pair[]{
            ImmutablePair.<String, Object[]>of("Universe of Endless Blades: Mugen no Kensei: Creates a pale-sand Reality Marble filled with projected weapons.", null),
            ImmutablePair.<String, Object[]>of("Unlimited Blade Works: Infinite Creation of Swords: Creates an orange Reality Marble filled with projected weapons.", null),
            ImmutablePair.<String, Object[]>of("Unlimited Lost Works: Traps a target in a delayed barrage that disables their Devil Fruit abilities.", null)
    };

    public static final AbilityCore<RealityMarbleAbility> INSTANCE = new AbilityCore.Builder<>(
            "Reality Marble", AbilityCategory.DEVIL_FRUITS, RealityMarbleAbility::new)
            .addDescriptionLine(AbilityHelper.registerDescriptionText("kazimod", "reality_marble", DESCRIPTION))
            .addAdvancedDescriptionLine(AbilityDescriptionLine.NEW_LINE,
                    ChargeComponent.getTooltip(0.0F, SECOND_CHARGE),
                    ContinuousComponent.getTooltip(),
                    CooldownComponent.getTooltip(MARBLE_MIN_COOLDOWN, FIRST_COOLDOWN))
            .setIcon(FIRST_MODE_ICON)
            .setSourceHakiNature(SourceHakiNature.IMBUING)
            .setSourceType(new SourceType[]{SourceType.SLASH})
            .setUnlockCheck(user -> DevilFruitCapability.get(user).hasAwakenedFruit())
            .build();

    private final AltModeComponent<Mode> altModeComponent =
            new RealityMarbleAltModeComponent(this)
                    .addChangeModeEvent(this::onModeChanged);
    private final ChargeComponent chargeComponent = new ChargeComponent(this)
            .addStartEvent(this::onChargeStart)
            .addTickEvent(this::onChargeTick)
            .addEndEvent(this::onChargeEnd);
    private final ContinuousComponent continuousComponent = new ContinuousComponent(this, true)
            .addStartEvent(this::onContinuityStart)
            .addTickEvent(this::onContinuityTick)
            .addEndEvent(this::onContinuityEnd);
    private final RangeComponent lostWorksRange = new RangeComponent(this);
    private final DealDamageComponent lostWorksDamage = new DealDamageComponent(this);
    private final AnimationComponent lostWorksAnimation = new AnimationComponent(this);
    private UnlimitedLostWorksEntity lostWorks;
    private Mode currentMode = Mode.MUGEN_NO_KENSEI;
    private Mode activeMode = Mode.MUGEN_NO_KENSEI;
    private final Deque<BlockPos> firstModeReplacementQueue = new ArrayDeque<>();
    private final Deque<BlockPos> secondModeReplacementQueue = new ArrayDeque<>();
    private final Deque<BlockPos> firstModeRestorationQueue = new ArrayDeque<>();
    private final Deque<BlockPos> secondModeRestorationQueue = new ArrayDeque<>();
    private final Map<BlockPos, BlockState> firstModeReplacedBlocks = new HashMap<>();
    private final Map<BlockPos, BlockState> secondModeReplacedBlocks = new HashMap<>();
    private final List<RealityMarbleWeaponEntity> weaponDisplays = new ArrayList<>();
    private final List<RealityMarbleGearEntity> gearDisplays = new ArrayList<>();
    private final List<RealityMarbleGearEntity> aerialGearDisplays = new ArrayList<>();
    private BlockPos firstModeCenter;
    private BlockPos secondModeCenter;
    private SphereEntity firstModeSphere;
    private SphereEntity secondModeSphere;
    private UUID musicSession;
    private EffectInstance chargeMovementLock;
    private final List<ServerPlayerEntity> musicListeners = new ArrayList<>();

    public RealityMarbleAbility(AbilityCore<RealityMarbleAbility> core) {
        super(core);
        this.isNew = true;
        this.setDisplayName(REALITY_MARBLE_NAME);
        this.setDisplayIcon(FIRST_MODE_ICON);
        this.addComponents(new AbilityComponent[]{
                this.altModeComponent,
                this.chargeComponent,
                this.continuousComponent, this.lostWorksRange, this.lostWorksDamage, this.lostWorksAnimation
        });
        this.addUseEvent(this::onUseEvent);
        this.addTickEvent(this::onAbilityTick);
        this.addRemoveEvent((entity, ability) -> {
            finishLostWorks(entity);
            this.lostWorksAnimation.stop(entity);
            clearChargeMovementLock(entity);
            fadeChargeMusic(entity);
        });
    }

    private void onModeChanged(LivingEntity entity, IAbility ability, Mode mode) {
        updateModeDisplay(mode);
    }

    private void updateModeDisplay(Mode mode) {
        this.currentMode = mode;
        this.setDisplayName(REALITY_MARBLE_NAME);
        switch (mode) {
            case INFINITE_CREATION_OF_SWORDS:
                this.setDisplayIcon(SECOND_MODE_ICON);
                break;
            case VOID_CREATION_OF_SWORDS:
                this.setDisplayIcon(THIRD_MODE_ICON);
                break;
            case MUGEN_NO_KENSEI:
            default:
                this.setDisplayIcon(FIRST_MODE_ICON);
                break;
        }
    }

    /** Migrates legacy FIRST/SECOND/THIRD selections to their renamed modes. */
    private static final class RealityMarbleAltModeComponent extends AltModeComponent<Mode> {
        private RealityMarbleAltModeComponent(RealityMarbleAbility ability) {
            super(ability, Mode.class, Mode.MUGEN_NO_KENSEI);
        }

        @Override
        public void load(CompoundNBT nbt) {
            String storedMode = nbt.getString("currentMode");
            if ("FIRST".equals(storedMode)
                    || "Unlimited Blade Works: Mugen no Kensei".equals(storedMode)
                    || "Universe of Endless Blades: Mugen no Kensei".equals(storedMode)) {
                nbt.putString("currentMode", Mode.MUGEN_NO_KENSEI.name());
            } else if ("SECOND".equals(storedMode)
                    || "Unlimited Blade Works: Infinite Creation of Swords".equals(storedMode)) {
                nbt.putString("currentMode", Mode.INFINITE_CREATION_OF_SWORDS.name());
            } else if ("THIRD".equals(storedMode)
                    || "Unlimited Lost Works: Void Creation Of Swords".equals(storedMode)
                    || "Unlimited Lost Works".equals(storedMode)) {
                nbt.putString("currentMode", Mode.VOID_CREATION_OF_SWORDS.name());
            }
            super.load(nbt);
        }
    }

    /** Discards legacy per-mode display names saved before the alt-mode rename. */
    @Override
    public void load(CompoundNBT nbt) {
        super.load(nbt);
        updateModeDisplay(this.altModeComponent.getCurrentMode());
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        // A single activation owns the entire delayed sequence; pressing again cannot cancel or replace it.
        if (this.lostWorks != null) return;
        if (!this.firstModeRestorationQueue.isEmpty() || !this.secondModeRestorationQueue.isEmpty()) {
            return;
        }
        if (this.continuousComponent.isContinuous()) {
            this.continuousComponent.stopContinuity(entity);
        } else if (!this.chargeComponent.isCharging()) {
            // Lock the selected mode so changing the displayed alt mode during the
            // charge cannot alter the charge time, behavior, or resulting cooldown.
            this.activeMode = this.currentMode;
            if (this.activeMode == Mode.VOID_CREATION_OF_SWORDS) {
                useThirdMode(entity, ability);
            } else {
                this.chargeComponent.startCharging(entity, getChargeTime(this.activeMode));
            }
        }
    }

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide || !isContinuousMarbleMode(this.activeMode)) return;
        applyChargeMovementLock(entity);
        playChargeMusic(entity);
    }

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide || !isContinuousMarbleMode(this.activeMode)) return;
        applyChargeMovementLock(entity);
    }

    private void applyChargeMovementLock(LivingEntity entity) {
        EffectInstance existing = entity.getEffect(ModEffects.MOVEMENT_BLOCKED.get());
        // A short refresh also expires safely if ticking stops unexpectedly.
        entity.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(), 3, 0, false, false));
        if (existing == null) this.chargeMovementLock = entity.getEffect(ModEffects.MOVEMENT_BLOCKED.get());
    }

    private void clearChargeMovementLock(LivingEntity entity) {
        if (entity.level.isClientSide) return;
        EffectInstance current = entity.getEffect(ModEffects.MOVEMENT_BLOCKED.get());
        // Leave a longer or stronger lock applied by another ability intact.
        if (current != null && current == this.chargeMovementLock
                && current.getDuration() <= 3 && current.getAmplifier() == 0) {
            entity.removeEffect(ModEffects.MOVEMENT_BLOCKED.get());
        }
        this.chargeMovementLock = null;
    }

    private void playChargeMusic(LivingEntity entity) {
        if (!(entity.level instanceof ServerWorld)) {
            return;
        }

        fadeChargeMusic(entity);
        this.musicSession = UUID.randomUUID();
        for (ServerPlayerEntity player : ((ServerWorld) entity.level).players()) {
            double dx = player.getX() - entity.getX();
            double dy = player.getY() - entity.getY();
            double dz = player.getZ() - entity.getZ();
            if (dx * dx + dy * dy + dz * dz > CHARGE_MUSIC_RANGE_SQR) {
                continue;
            }

            this.musicListeners.add(player);
            KaziPacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new RealityMarbleMusicPacket(this.musicSession, true,
                            this.activeMode == Mode.INFINITE_CREATION_OF_SWORDS
                                    ? RealityMarbleMusicPacket.SecondarySound.INFINITE_CREATION
                                    : RealityMarbleMusicPacket.SecondarySound.MUGEN));
        }
    }

    private void fadeChargeMusic(LivingEntity entity) {
        if (entity.level.isClientSide || this.musicSession == null) return;
        for (ServerPlayerEntity player : this.musicListeners) {
            KaziPacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new RealityMarbleMusicPacket(this.musicSession, false));
        }
        this.musicListeners.clear();
        this.musicSession = null;
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        clearChargeMovementLock(entity);
        if (this.activeMode == Mode.VOID_CREATION_OF_SWORDS) {
            // Handles legacy saves that still contain the old third-mode charge.
            useThirdMode(entity, ability);
            return;
        }
        this.continuousComponent.startContinuity(entity,
                isContinuousMarbleMode(this.activeMode) ? MARBLE_HOLD_TICKS : -1.0F);
    }

    private static boolean isContinuousMarbleMode(Mode mode) {
        return mode == Mode.MUGEN_NO_KENSEI
                || mode == Mode.INFINITE_CREATION_OF_SWORDS;
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {
        switch (this.activeMode) {
            case MUGEN_NO_KENSEI:
                useFirstMode(entity, ability);
                break;
            case INFINITE_CREATION_OF_SWORDS:
                useSecondMode(entity, ability);
                break;
            case VOID_CREATION_OF_SWORDS:
                // Lost Works runs as a single-use target sequence, not a continuous mode.
                break;
            default:
                break;
        }
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        switch (this.activeMode) {
            case MUGEN_NO_KENSEI:
                processFirstModeReplacement(entity);
                maintainSphere(this.firstModeSphere, this.firstModeCenter);
                break;
            case INFINITE_CREATION_OF_SWORDS:
                processSecondModeReplacement(entity);
                maintainSphere(this.secondModeSphere, this.secondModeCenter);
                break;
            case VOID_CREATION_OF_SWORDS:
                break;
            default:
                break;
        }
    }

    private void onAbilityTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        if (this.lostWorks != null && !isLostWorksActive(this.lostWorks)) finishLostWorks(entity);
        if (!this.chargeComponent.isCharging()) clearChargeMovementLock(entity);
        if (!this.chargeComponent.isCharging() && !this.continuousComponent.isContinuous()) {
            fadeChargeMusic(entity);
        }
        processRestoration(entity, this.firstModeRestorationQueue, this.firstModeReplacedBlocks);
        processRestoration(entity, this.secondModeRestorationQueue, this.secondModeReplacedBlocks);
        if (!this.continuousComponent.isContinuous()) {
            if (this.firstModeRestorationQueue.isEmpty() && this.firstModeReplacedBlocks.isEmpty()) {
                this.firstModeCenter = null;
            }
            if (this.secondModeRestorationQueue.isEmpty() && this.secondModeReplacedBlocks.isEmpty()) {
                this.secondModeCenter = null;
            }
            if (this.firstModeCenter == null && this.secondModeCenter == null) {
                ACTIVE_MARBLES.remove(entity.getUUID());
            }
        }
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        fadeChargeMusic(entity);
        // A stopped mode must not leave queued terrain work running in the background.
        this.firstModeReplacementQueue.clear();
        this.secondModeReplacementQueue.clear();
        if (!entity.level.isClientSide) {
            switch (this.activeMode) {
            case MUGEN_NO_KENSEI:
                queueRestoration(this.firstModeReplacedBlocks, this.firstModeRestorationQueue,
                            this.firstModeCenter);
                    removeFirstModeSphere();
                    break;
            case INFINITE_CREATION_OF_SWORDS:
                queueRestoration(this.secondModeReplacedBlocks, this.secondModeRestorationQueue,
                            this.secondModeCenter);
                    removeSecondModeSphere();
                    break;
                case VOID_CREATION_OF_SWORDS:
                default:
                    break;
            }
        }
        removeWeaponDisplays();
        removeGearDisplays();
        float cooldown = getCooldown(this.activeMode);
        if (isContinuousMarbleMode(this.activeMode)) {
            // Scale from the 30-second minimum to the mode's maximum using active hold time only.
            float heldTicks = Math.max(0.0F,
                    Math.min(MARBLE_HOLD_TICKS, this.continuousComponent.getContinueTime()));
            cooldown = MARBLE_MIN_COOLDOWN
                    + (cooldown - MARBLE_MIN_COOLDOWN) * (heldTicks / MARBLE_HOLD_TICKS);
        }
        this.cooldownComponent.startCooldown(entity, cooldown);
    }

    /** Reserved exclusively for the first Reality Marble design. */
    private void useFirstMode(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide && this.firstModeReplacementQueue.isEmpty()) {
            this.firstModeReplacedBlocks.clear();
            this.firstModeRestorationQueue.clear();
            this.firstModeCenter = entity.blockPosition().immutable();
            beginMarbleTracking(entity, this.firstModeCenter);
            removeFirstModeSphere();
            this.firstModeSphere = createMarbleSphere(entity, this.firstModeCenter, BLUE_MARBLE_SKY);
            queueFirstModeReplacement(entity.blockPosition());
            spawnFirstModeWeapons(entity, entity.blockPosition());
            spawnModeGears(entity, entity.blockPosition(), true);
            spawnAerialModeGears(entity, entity.blockPosition(), true);
        }
    }

    /** Reserved exclusively for the second Reality Marble design. */
    private void useSecondMode(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide && this.secondModeReplacementQueue.isEmpty()) {
            this.secondModeReplacedBlocks.clear();
            this.secondModeRestorationQueue.clear();
            this.secondModeCenter = entity.blockPosition().immutable();
            beginMarbleTracking(entity, this.secondModeCenter);
            removeSecondModeSphere();
            this.secondModeSphere = createMarbleSphere(entity, this.secondModeCenter, ORANGE_MARBLE_SKY);
            queueSecondModeReplacement(entity.blockPosition());
            spawnSecondModeWeapons(entity, entity.blockPosition());
            spawnModeGears(entity, entity.blockPosition(), false);
            spawnAerialModeGears(entity, entity.blockPosition(), false);
        }
    }

    /** Instant, non-damaging Dismantle-style acquisition; the marked target is struck 15 seconds later. */
    private void useThirdMode(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        this.cooldownComponent.startCooldown(entity, THIRD_COOLDOWN);
        this.lostWorksAnimation.start(entity, ModAnimations.UPPER_SLASH, 10);
        LivingEntity target = UnlimitedLostWorksAttack.findTarget(entity, this.lostWorksRange);
        if (target == null) {
            return;
        }
        this.lostWorks = new UnlimitedLostWorksEntity(KaziEntities.UNLIMITED_LOST_WORKS.get(), entity.level);
        this.lostWorks.begin(entity, target, this);
        if (!entity.level.addFreshEntity(this.lostWorks)) {
            clearLostWorks();
            return;
        }
        this.lostWorks.playMarkSound();
        LostWorksFruitLockEffect.applyToTarget(target);
    }

    private void finishLostWorks(LivingEntity entity) {
        if (this.lostWorks == null) return;
        clearLostWorks();
    }

    private void clearLostWorks() {
        if (this.lostWorks != null) {
            this.lostWorks.remove();
            this.lostWorks = null;
        }
    }

    public boolean isLostWorksActive(UnlimitedLostWorksEntity sequence) {
        return sequence != null && sequence == this.lostWorks && sequence.isAlive()
                && !this.getComponent(ModAbilityKeys.DISABLE).map(disable -> disable.isDisabled()).orElse(false);
    }

    public boolean hitLostWorks(LivingEntity caster, LivingEntity target, boolean applyEffects) {
        return UnlimitedLostWorksAttack.pierce(caster, target, this.lostWorksDamage, applyEffects);
    }

    private static float getChargeTime(Mode mode) {
        switch (mode) {
            case INFINITE_CREATION_OF_SWORDS:
                return SECOND_CHARGE;
            case VOID_CREATION_OF_SWORDS:
                return 0.0F;
            case MUGEN_NO_KENSEI:
            default:
                return FIRST_CHARGE;
        }
    }

    private static float getCooldown(Mode mode) {
        switch (mode) {
            case INFINITE_CREATION_OF_SWORDS:
                return SECOND_COOLDOWN;
            case VOID_CREATION_OF_SWORDS:
                return THIRD_COOLDOWN;
            case MUGEN_NO_KENSEI:
            default:
                return FIRST_COOLDOWN;
        }
    }

    /** Whether this exact Reality Marble mode is currently running for its user. */
    public boolean isModeActive(Mode mode) {
        if (mode == Mode.VOID_CREATION_OF_SWORDS) return isLostWorksActive(this.lostWorks);
        return this.continuousComponent.isContinuous() && this.activeMode == mode;
    }

    /** Whether any Reality Marble mode is currently running. */
    public boolean isActive() {
        return this.continuousComponent.isContinuous() || isLostWorksActive(this.lostWorks);
    }

    /** FIRST's weapon-field path remains separate for later mode-specific behavior. */
    private void spawnFirstModeWeapons(LivingEntity entity, BlockPos center) {
        spawnWedgedWeapons(entity, center, WEAPON_DISPLAY_COUNT);
    }

    /** SECOND's weapon-field path remains separate for later mode-specific behavior. */
    private void spawnSecondModeWeapons(LivingEntity entity, BlockPos center) {
        spawnWedgedWeapons(entity, center, WEAPON_DISPLAY_COUNT);
    }

    private void spawnWedgedWeapons(LivingEntity entity, BlockPos center, int count) {
        removeWeaponDisplays();
        List<Item> weapons = new ArrayList<>();
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            if (item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem) {
                weapons.add(item);
            }
        }
        if (weapons.isEmpty()) return;

        Random random = entity.getRandom();
        spawnWedgedWeapon(entity, center.getX(), center.getZ(), weapons, random);
        // Random rejection sampling keeps the weapon field organic rather than grid-aligned.
        int attempts = count * 128;
        while (this.weaponDisplays.size() < count && attempts-- > 0) {
            int x = center.getX() + random.nextInt(WEAPON_SPAWN_AREA_SIZE)
                    - WEAPON_SPAWN_AREA_SIZE / 2;
            int z = center.getZ() + random.nextInt(WEAPON_SPAWN_AREA_SIZE)
                    - WEAPON_SPAWN_AREA_SIZE / 2;
            if (!isFarEnoughFromOtherWeapons(x, z)) continue;
            spawnWedgedWeapon(entity, x, z, weapons, random);
        }
    }

    /** Keeps every random display at least 2.5 blocks apart in the X/Z plane. */
    private boolean isFarEnoughFromOtherWeapons(int x, int z) {
        double candidateX = x + 0.5D;
        double candidateZ = z + 0.5D;
        double minimumDistanceSquared = DISPLAY_MIN_HORIZONTAL_SPACING * DISPLAY_MIN_HORIZONTAL_SPACING;
        for (Entity display : this.weaponDisplays) {
            if (display == null || !display.isAlive()) continue;
            double deltaX = display.getX() - candidateX;
            double deltaZ = display.getZ() - candidateZ;
            if (deltaX * deltaX + deltaZ * deltaZ < minimumDistanceSquared) return false;
        }
        for (Entity display : this.gearDisplays) {
            if (display == null || !display.isAlive()) continue;
            double deltaX = display.getX() - candidateX;
            double deltaZ = display.getZ() - candidateZ;
            if (deltaX * deltaX + deltaZ * deltaZ < minimumDistanceSquared) return false;
        }
        return true;
    }

    private void spawnWedgedWeapon(LivingEntity entity, int x, int z,
                                   List<Item> weapons, Random random) {
        BlockPos spawnPos = findWeaponSpawnPosition(entity, x, z);
        if (spawnPos == null) return;

        RealityMarbleWeaponEntity display = new RealityMarbleWeaponEntity(
                KaziEntities.REALITY_MARBLE_WEAPON.get(), entity.level);
        float xRotation = random.nextFloat() * 90.0F - 45.0F;
        float yRotation = random.nextFloat() * 360.0F;
        float zRotation = 205.0F + random.nextFloat() * 40.0F;
        display.moveTo(x + 0.5D, spawnPos.getY() + 0.15D, z + 0.5D, yRotation, xRotation);
        display.yRot = yRotation;
        display.yRotO = yRotation;
        display.xRot = xRotation;
        display.xRotO = xRotation;
        display.setYRotation(yRotation);
        display.setZRotation(zRotation);
        display.setWeapon(new ItemStack(weapons.get(random.nextInt(weapons.size()))));
        entity.level.addFreshEntity(display);
        this.weaponDisplays.add(display);
    }

    /** Spawns five randomly placed gears using the same safe terrain and spacing rules as swords. */
    private void spawnModeGears(LivingEntity entity, BlockPos center, boolean mugenVariant) {
        removeGroundGearDisplays();
        Random random = entity.getRandom();
        int attempts = GEAR_DISPLAY_COUNT * 128;
        while (this.gearDisplays.size() < GEAR_DISPLAY_COUNT && attempts-- > 0) {
            int x = center.getX() + random.nextInt(WEAPON_SPAWN_AREA_SIZE)
                    - WEAPON_SPAWN_AREA_SIZE / 2;
            int z = center.getZ() + random.nextInt(WEAPON_SPAWN_AREA_SIZE)
                    - WEAPON_SPAWN_AREA_SIZE / 2;
            if (!isFarEnoughFromOtherGears(x, z)) continue;

            BlockPos spawnPos = findWeaponSpawnPosition(entity, x, z);
            if (spawnPos == null) continue;
            RealityMarbleGearEntity gear = new RealityMarbleGearEntity(
                    KaziEntities.REALITY_MARBLE_GEAR.get(), entity.level);
            float yaw = random.nextFloat() * 360.0F;
            gear.moveTo(x + 0.5D, spawnPos.getY() + 2.15D, z + 0.5D, yaw, 0.0F);
            gear.yRot = yaw;
            gear.yRotO = yaw;
            gear.setSpinOffset(random.nextFloat() * 360.0F);
            gear.setMugenVariant(mugenVariant);
            entity.level.addFreshEntity(gear);
            this.gearDisplays.add(gear);
        }
    }

    /** Keeps ground gears clear of every existing display in the marble field. */
    private boolean isFarEnoughFromOtherGears(int x, int z) {
        double candidateX = x + 0.5D;
        double candidateZ = z + 0.5D;
        double minimumDistanceSquared = DISPLAY_MIN_HORIZONTAL_SPACING * DISPLAY_MIN_HORIZONTAL_SPACING;
        for (RealityMarbleWeaponEntity weapon : this.weaponDisplays) {
            if (weapon == null || !weapon.isAlive()) continue;
            double deltaX = weapon.getX() - candidateX;
            double deltaZ = weapon.getZ() - candidateZ;
            if (deltaX * deltaX + deltaZ * deltaZ < minimumDistanceSquared) return false;
        }
        for (RealityMarbleGearEntity gear : this.gearDisplays) {
            if (gear == null || !gear.isAlive()) continue;
            double deltaX = gear.getX() - candidateX;
            double deltaZ = gear.getZ() - candidateZ;
            if (deltaX * deltaX + deltaZ * deltaZ < minimumDistanceSquared) return false;
        }
        for (RealityMarbleGearEntity gear : this.aerialGearDisplays) {
            if (gear == null || !gear.isAlive()) continue;
            double deltaX = gear.getX() - candidateX;
            double deltaZ = gear.getZ() - candidateZ;
            if (deltaX * deltaX + deltaZ * deltaZ < minimumDistanceSquared) return false;
        }
        return true;
    }

    /** A separate five-gear layer, twenty blocks above the regular gear field. */
    private void spawnAerialModeGears(LivingEntity entity, BlockPos center, boolean mugenVariant) {
        removeAerialGearDisplays();
        Random random = entity.getRandom();
        int attempts = GEAR_DISPLAY_COUNT * 128;
        while (this.aerialGearDisplays.size() < GEAR_DISPLAY_COUNT && attempts-- > 0) {
            int x = center.getX() + random.nextInt(WEAPON_SPAWN_AREA_SIZE)
                    - WEAPON_SPAWN_AREA_SIZE / 2;
            int z = center.getZ() + random.nextInt(WEAPON_SPAWN_AREA_SIZE)
                    - WEAPON_SPAWN_AREA_SIZE / 2;
            if (!isFarEnoughFromAerialGears(x, z)) continue;

            BlockPos spawnPos = findWeaponSpawnPosition(entity, x, z);
            if (spawnPos == null) continue;
            RealityMarbleGearEntity gear = new RealityMarbleGearEntity(
                    KaziEntities.REALITY_MARBLE_GEAR.get(), entity.level);
            float yaw = random.nextFloat() * 360.0F;
            gear.moveTo(x + 0.5D, spawnPos.getY() + 20.15D, z + 0.5D, yaw, 0.0F);
            gear.yRot = yaw;
            gear.yRotO = yaw;
            gear.setSpinOffset(random.nextFloat() * 360.0F);
            gear.setMugenVariant(mugenVariant);
            entity.level.addFreshEntity(gear);
            this.aerialGearDisplays.add(gear);
        }
    }

    private boolean isFarEnoughFromAerialGears(int x, int z) {
        double candidateX = x + 0.5D;
        double candidateZ = z + 0.5D;
        double minimumDistanceSquared = DISPLAY_MIN_HORIZONTAL_SPACING * DISPLAY_MIN_HORIZONTAL_SPACING;
        for (RealityMarbleWeaponEntity weapon : this.weaponDisplays) {
            if (weapon == null || !weapon.isAlive()) continue;
            double deltaX = weapon.getX() - candidateX;
            double deltaZ = weapon.getZ() - candidateZ;
            if (deltaX * deltaX + deltaZ * deltaZ < minimumDistanceSquared) return false;
        }
        for (RealityMarbleGearEntity gear : this.aerialGearDisplays) {
            if (gear == null || !gear.isAlive()) continue;
            double deltaX = gear.getX() - candidateX;
            double deltaZ = gear.getZ() - candidateZ;
            if (deltaX * deltaX + deltaZ * deltaZ < minimumDistanceSquared) return false;
        }
        for (RealityMarbleGearEntity gear : this.gearDisplays) {
            if (gear == null || !gear.isAlive()) continue;
            double deltaX = gear.getX() - candidateX;
            double deltaZ = gear.getZ() - candidateZ;
            if (deltaX * deltaX + deltaZ * deltaZ < minimumDistanceSquared) return false;
        }
        return true;
    }

    /** Finds the highest open block directly above solid ground in the marble band. */
    private static BlockPos findWeaponSpawnPosition(LivingEntity entity, int x, int z) {
        BlockPos center = entity.blockPosition();
        for (int y = center.getY() + WEAPON_MAX_Y_OFFSET; y >= center.getY() + WEAPON_MIN_Y_OFFSET; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = entity.level.getBlockState(pos);
            BlockState below = entity.level.getBlockState(pos.below());
            if (!state.getMaterial().isSolid() && state.getFluidState().isEmpty()
                    && below.getMaterial().isSolid()) {
                return pos;
            }
        }
        return null;
    }

    private void removeWeaponDisplays() {
        for (RealityMarbleWeaponEntity display : this.weaponDisplays) {
            if (display != null && display.isAlive()) display.remove();
        }
        this.weaponDisplays.clear();
    }

    private void removeGearDisplays() {
        removeGroundGearDisplays();
        removeAerialGearDisplays();
    }

    private void removeGroundGearDisplays() {
        for (RealityMarbleGearEntity gear : this.gearDisplays) {
            if (gear != null && gear.isAlive()) gear.remove();
        }
        this.gearDisplays.clear();
    }

    private void removeAerialGearDisplays() {
        for (RealityMarbleGearEntity gear : this.aerialGearDisplays) {
            if (gear != null && gear.isAlive()) gear.remove();
        }
        this.aerialGearDisplays.clear();
    }

    /** Creates a translucent, fully-wrapped sky sphere for an active Reality Marble mode. */
    private static SphereEntity createMarbleSphere(LivingEntity entity, BlockPos center, ResourceLocation skyTexture) {
        SphereEntity sphere = new SphereEntity(entity.level, entity);
        sphere.setTexture(true, skyTexture);
        sphere.setColor(new Color(255, 255, 255, 160));
        sphere.setRadius(MARBLE_SPHERE_RADIUS);
        sphere.setDetailLevel(24);
        sphere.setAnimationSpeed(1);
        sphere.setPos(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D);
        entity.level.addFreshEntity(sphere);
        return sphere;
    }

    private static void maintainSphere(SphereEntity sphere, BlockPos center) {
        if (sphere != null && sphere.isAlive() && center != null) {
            sphere.setPos(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D);
        }
    }

    private void removeFirstModeSphere() {
        if (this.firstModeSphere != null) this.firstModeSphere.remove();
        this.firstModeSphere = null;
    }

    private void removeSecondModeSphere() {
        if (this.secondModeSphere != null) this.secondModeSphere.remove();
        this.secondModeSphere = null;
    }

    private static void beginMarbleTracking(LivingEntity entity, BlockPos center) {
        ACTIVE_MARBLES.put(entity.getUUID(), new ActiveMarble(entity.level, center));
    }

    private static void trackMarbleBlock(LivingEntity entity, BlockPos pos, BlockState originalState) {
        ActiveMarble activeMarble = ACTIVE_MARBLES.get(entity.getUUID());
        if (activeMarble != null && activeMarble.world == entity.level) {
            activeMarble.originalStates.putIfAbsent(pos.immutable(), originalState);
        }
    }

    /** Restores temporary terrain before a disconnect can stop an integrated server. */
    private void queueLogoutRestoration(LivingEntity entity) {
        finishLostWorks(entity);
        clearChargeMovementLock(entity);
        fadeChargeMusic(entity);
        ActiveMarble trackedMarble = ACTIVE_MARBLES.remove(entity.getUUID());
        if (trackedMarble != null) {
            queueLogoutRestoration(entity.getUUID(), trackedMarble.world,
                    trackedMarble.originalStates, trackedMarble.center);
        } else {
            Map<BlockPos, BlockState> replacedBlocks = new HashMap<>();
            replacedBlocks.putAll(this.firstModeReplacedBlocks);
            replacedBlocks.putAll(this.secondModeReplacedBlocks);
            BlockPos center = this.activeMode == Mode.INFINITE_CREATION_OF_SWORDS ? this.secondModeCenter : this.firstModeCenter;
            queueLogoutRestoration(entity.getUUID(), entity.level, replacedBlocks, center);
        }

        this.firstModeReplacementQueue.clear();
        this.secondModeReplacementQueue.clear();
        this.firstModeRestorationQueue.clear();
        this.secondModeRestorationQueue.clear();
        this.firstModeReplacedBlocks.clear();
        this.secondModeReplacedBlocks.clear();
        this.firstModeCenter = null;
        this.secondModeCenter = null;
        removeFirstModeSphere();
        removeSecondModeSphere();
        removeWeaponDisplays();
        removeGearDisplays();
    }

    private static void queueLogoutRestoration(UUID playerId, World world,
                                               Map<BlockPos, BlockState> originalStates,
                                               BlockPos center) {
        if (originalStates.isEmpty()) return;
        // A dedicated server can afford queued restoration, but an integrated
        // server often shuts down immediately after PlayerLoggedOutEvent. Restore
        // synchronously so temporary terrain can never survive that shutdown.
        PENDING_LOGOUT_RESTORATIONS.remove(playerId);
        for (Map.Entry<BlockPos, BlockState> entry : originalStates.entrySet()) {
            BlockPos pos = entry.getKey();
            if (isCustomMarbleTerrain(world.getBlockState(pos).getBlock())) {
                world.setBlock(pos, entry.getValue(), 3);
            }
        }
    }

    /** FIRST's temporary terrain-conversion path. */
    private void queueFirstModeReplacement(BlockPos center) {
        queueCircularReplacement(center, this.firstModeReplacementQueue);
    }

    private void processFirstModeReplacement(LivingEntity entity) {
        for (int remaining = BLOCKS_PER_TICK;
             remaining > 0 && !this.firstModeReplacementQueue.isEmpty(); remaining--) {
            replaceWithCustomMarbleTerrain(entity, this.firstModeReplacementQueue.removeFirst(),
                    this.firstModeReplacedBlocks, KaziBlocks.CUSTOM_COARSE_SAND.get());
        }
    }

    /** SECOND's temporary terrain-conversion path, deliberately independent of FIRST. */
    private void queueSecondModeReplacement(BlockPos center) {
        queueCircularReplacement(center, this.secondModeReplacementQueue);
    }

    private void processSecondModeReplacement(LivingEntity entity) {
        for (int remaining = BLOCKS_PER_TICK;
             remaining > 0 && !this.secondModeReplacementQueue.isEmpty(); remaining--) {
            replaceWithCustomMarbleTerrain(entity, this.secondModeReplacementQueue.removeFirst(),
                    this.secondModeReplacedBlocks, KaziBlocks.CUSTOM_COARSE_DIRT.get());
        }
    }

    private static void queueCircularReplacement(BlockPos center, Deque<BlockPos> queue) {
        queue.clear();
        List<BlockPos> columns = new ArrayList<>();
        for (int x = -HORIZONTAL_RADIUS; x <= HORIZONTAL_RADIUS; x++) {
            for (int z = -HORIZONTAL_RADIUS; z <= HORIZONTAL_RADIUS; z++) {
                if (x * x + z * z <= HORIZONTAL_RADIUS * HORIZONTAL_RADIUS) {
                    columns.add(new BlockPos(x, 0, z));
                }
            }
        }
        columns.sort(Comparator.comparingInt(pos ->
                pos.getX() * pos.getX() + pos.getZ() * pos.getZ()));
        for (BlockPos column : columns) {
            for (int y = REPLACEMENT_MIN_Y_OFFSET; y <= REPLACEMENT_MAX_Y_OFFSET; y++) {
                queue.addLast(center.offset(column.getX(), y, column.getZ()).immutable());
            }
        }
    }

    private static void replaceWithCustomMarbleTerrain(LivingEntity entity, BlockPos pos,
                                                        Map<BlockPos, BlockState> replacedBlocks,
                                                        Block replacementBlock) {
        BlockState state = entity.level.getBlockState(pos);
        if (state.isAir() || state.is(Blocks.BEDROCK) || state.is(Blocks.BARRIER)
                || !state.getFluidState().isEmpty() || state.hasTileEntity()
                || state.getDestroySpeed(entity.level, pos) < 0.0F
                || isCustomMarbleTerrain(state.getBlock())) {
            return;
        }
        replacedBlocks.putIfAbsent(pos.immutable(), state);
        trackMarbleBlock(entity, pos, state);
        entity.level.setBlock(pos, replacementBlock.defaultBlockState(), 3);
    }

    private static boolean isCustomMarbleTerrain(Block block) {
        return block == KaziBlocks.CUSTOM_COARSE_DIRT.get()
                || block == KaziBlocks.CUSTOM_COARSE_SAND.get();
    }

    private static void queueRestoration(Map<BlockPos, BlockState> replacedBlocks,
                                         Deque<BlockPos> restorationQueue, BlockPos center) {
        restorationQueue.clear();
        List<BlockPos> positions = new ArrayList<>(replacedBlocks.keySet());
        if (center != null) {
            positions.sort(Comparator.comparingInt((BlockPos pos) -> {
                int x = pos.getX() - center.getX();
                int z = pos.getZ() - center.getZ();
                return x * x + z * z;
            }).reversed());
        }
        restorationQueue.addAll(positions);
    }

    private static void processRestoration(LivingEntity entity, Deque<BlockPos> restorationQueue,
                                           Map<BlockPos, BlockState> replacedBlocks) {
        for (int remaining = RESTORE_BLOCKS_PER_TICK;
             remaining > 0 && !restorationQueue.isEmpty(); remaining--) {
            BlockPos pos = restorationQueue.removeFirst();
            BlockState originalState = replacedBlocks.remove(pos);
            if (originalState != null
                    && isCustomMarbleTerrain(entity.level.getBlockState(pos).getBlock())) {
                entity.level.setBlock(pos, originalState, 3);
            }
        }
    }

    @Mod.EventBusSubscriber(modid = "kazimod")
    public static class LogoutCleanupHandler {
        @SubscribeEvent
        public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            IAbilityData abilityData = AbilityDataCapability.get(event.getPlayer());
            RealityMarbleAbility ability = abilityData.getEquippedAbility(INSTANCE);
            if (ability != null) {
                ability.queueLogoutRestoration(event.getPlayer());
            } else {
                ActiveMarble trackedMarble = ACTIVE_MARBLES.remove(event.getPlayer().getUUID());
                if (trackedMarble != null) {
                    queueLogoutRestoration(event.getPlayer().getUUID(), trackedMarble.world,
                            trackedMarble.originalStates, trackedMarble.center);
                }
            }
        }

        @SubscribeEvent
        public static void onWorldTick(TickEvent.WorldTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.world.isClientSide) return;

            List<UUID> queuedPlayers = new ArrayList<>();
            for (Map.Entry<UUID, LogoutRestoration> entry : PENDING_LOGOUT_RESTORATIONS.entrySet()) {
                if (entry.getValue().world == event.world) queuedPlayers.add(entry.getKey());
            }
            for (UUID playerId : queuedPlayers) {
                LogoutRestoration cleanup = PENDING_LOGOUT_RESTORATIONS.get(playerId);
                if (cleanup == null) continue;
                for (int remaining = RESTORE_BLOCKS_PER_TICK;
                     remaining > 0 && !cleanup.positions.isEmpty(); remaining--) {
                    BlockPos pos = cleanup.positions.removeFirst();
                    BlockState originalState = cleanup.originalStates.remove(pos);
                    if (originalState != null
                            && isCustomMarbleTerrain(cleanup.world.getBlockState(pos).getBlock())) {
                        cleanup.world.setBlock(pos, originalState, 3);
                    }
                }
                if (cleanup.positions.isEmpty()) PENDING_LOGOUT_RESTORATIONS.remove(playerId);
            }
        }
    }

    private static class LogoutRestoration {
        private final World world;
        private final Deque<BlockPos> positions;
        private final Map<BlockPos, BlockState> originalStates;

        private LogoutRestoration(World world, Deque<BlockPos> positions,
                                  Map<BlockPos, BlockState> originalStates) {
            this.world = world;
            this.positions = positions;
            this.originalStates = originalStates;
        }
    }

    private static class ActiveMarble {
        private final World world;
        private final BlockPos center;
        private final Map<BlockPos, BlockState> originalStates = new HashMap<>();

        private ActiveMarble(World world, BlockPos center) {
            this.world = world;
            this.center = center;
        }
    }
}
