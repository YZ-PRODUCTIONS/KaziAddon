//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.YamiRework;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.MrMagicalCart.cartaddon.abilities.yamiextra.ReworkedAbsorbedBlocksAbility;
import net.MrMagicalCart.cartaddon.init.CartBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.protection.BlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.protection.DefaultProtectionRules;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class BlackHoleRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "black_hole", new Pair[]{ImmutablePair.of("The user spreads darkness over the target area, which engulfs and suffocates anyone and anything inside of it.", (Object)null)});
    private static final int RANGE = 32;
    private static final int CHARGE_TIME = 100;
    private static final int RELEASE_TIME = 400;
    private static final int MAX_COOLDOWN = 400;
    private static final int RELEASE_PER_TICK = 40;
    public static final AbilityCore<BlackHoleRework> INSTANCE;
    private static final BlockProtectionRule.IReplaceBlockRule PLACE_RULE = (world, pos, state) -> !state.getMaterial().isSolid() && world.getBlockState(pos.below()).getMaterial().isSolid();
    private static final Map<UUID, Deque<BlockPos>> TRACKED_DARKNESS = new HashMap<>();
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private BlockPos origin;
    private State state;
    private final ChargeComponent chargeComponent;
    private final ContinuousComponent continuousComponent;

    public BlackHoleRework(AbilityCore<BlackHoleRework> core) {
        super(core);
        this.state = BlackHoleRework.State.ABSORBING;
        this.chargeComponent = (new ChargeComponent(this, true)).addTickEvent(this::onChargeTick).addEndEvent(this::onChargeEnd);
        this.continuousComponent = (new ContinuousComponent(this, true)).addTickEvent(this::onContinuityTick).addEndEvent(this::onContinuityEnd);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.chargeComponent, this.continuousComponent, this.animationComponent});
        this.addUseEvent(this::onUse);
    }

    private void onUse(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            if (this.state == BlackHoleRework.State.ABSORBING) {
                if (!this.chargeComponent.isCharging()) {
                    this.animationComponent.start(entity, ModAnimations.RYU_NO_IBUKI);
                    this.chargeComponent.startCharging(entity, 100.0F);
                } else if (this.chargeComponent.getChargePercentage() >= 0.2F) {
                    this.chargeComponent.stopCharging(entity);
                }
            } else {
                this.continuousComponent.stopContinuity(entity);
            }

        }
    }

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            entity.addEffect(new EffectInstance((Effect) ModEffects.MOVEMENT_BLOCKED.get(), 5, 1, false, false));
            IAbilityData data = AbilityDataCapability.get(entity);
            ReworkedAbsorbedBlocksAbility absorbed = (ReworkedAbsorbedBlocksAbility)data.getPassiveAbility(ReworkedAbsorbedBlocksAbility.INSTANCE);
            if (absorbed != null) {
                if (this.state == BlackHoleRework.State.ABSORBING) {
                    int currentRange = Math.max(1, (int)(32.0F * this.chargeComponent.getChargePercentage()));
                    this.origin = entity.blockPosition();
                    int startY = Math.min(entity.level.getMaxBuildHeight() - 1, this.origin.getY() + 32);

                    for(int dx = -currentRange; dx <= currentRange; ++dx) {
                        for(int dz = -currentRange; dz <= currentRange; ++dz) {
                            if (dx * dx + dz * dz <= currentRange * currentRange) {
                                BlockPos.Mutable scan = new BlockPos.Mutable(this.origin.getX() + dx, startY, this.origin.getZ() + dz);

                                while(scan.getY() > 1) {
                                    BlockState surface = entity.level.getBlockState(scan);
                                    if (surface.getBlock() == CartBlocks.REWORKED_DARKNESS_BLOCK.get()) {
                                        break;
                                    }
                                    if (surface.getBlock() == Blocks.BLUE_ICE) {
                                        break;
                                    }

                                    if (surface.getMaterial().isSolidBlocking()) {
                                        BlockPos darknessPos = scan.above();
                                        BlockState existing = entity.level.getBlockState(darknessPos);
                                        if (existing.getBlock() != CartBlocks.REWORKED_DARKNESS_BLOCK.get() && PLACE_RULE.replace(entity.level, darknessPos, existing)) {
                                            BlockPos absorbedPos = scan.below(1);
                                            if (absorbedPos.getY() >= 0) {
                                                BlockState absorbedState = entity.level.getBlockState(absorbedPos);
                                                if (absorbedState.getBlock() == Blocks.BLUE_ICE) {
                                                    break;
                                                }
                                                BlockState darknessState = ((Block)CartBlocks.REWORKED_DARKNESS_BLOCK.get()).defaultBlockState();
                                                boolean placed = AbilityHelper.placeBlockIfAllowed(entity, darknessPos, darknessState, 3, DefaultProtectionRules.AIR_FOLIAGE);
                                                if (!placed) {
                                                    entity.level.setBlock(darknessPos, darknessState, 3);
                                                    placed = entity.level.getBlockState(darknessPos).getBlock() == CartBlocks.REWORKED_DARKNESS_BLOCK.get();
                                                }
                                                if (placed) {
                                                    WyHelper.spawnParticleEffect((ParticleEffect)ModParticleEffects.BLACK_HOLE.get(), entity, (double)darknessPos.getX(), (double)((float)darknessPos.getY() + 0.5F), (double)darknessPos.getZ());
                                                    trackDarkness(entity, darknessPos);
                                                    absorbed.addAbsorbedBlock(absorbedState, absorbedPos, darknessPos);
                                                }
                                            }
                                        }
                                        break;
                                    }

                                    scan.move(Direction.DOWN);
                                }
                            }
                        }
                    }

                    if (absorbed.getUncompressedBlocks().isEmpty() && this.chargeComponent.isCharging()) {
                        this.chargeComponent.stopCharging(entity);
                        this.cooldownComponent.startCooldown(entity, 400.0F);
                    }
                } else if (this.state == BlackHoleRework.State.RELEASING) {
                    List<ReworkedAbsorbedBlocksAbility.BlockData> blocks = absorbed.getUncompressedBlocks();

                    for(int remaining = RELEASE_PER_TICK; remaining-- > 0; ) {
                        if (!blocks.isEmpty()) {
                            ReworkedAbsorbedBlocksAbility.BlockData dataBlock = (ReworkedAbsorbedBlocksAbility.BlockData)blocks.get(blocks.size() - 1);
                            BlockPos darknessPos = dataBlock.getDarknessPos();
                            retractDarkness(entity, darknessPos);
                            absorbed.removeAbsorbedBlock(dataBlock);
                            untrackDarkness(entity, darknessPos);
                        } else {
                            BlockPos darknessPos = pollTrackedDarkness(entity);
                            if (darknessPos == null) {
                                break;
                            }
                            retractDarkness(entity, darknessPos);
                        }
                    }
                }

            }
        }
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);
        if (!entity.level.isClientSide) {
            IAbilityData data = AbilityDataCapability.get(entity);
            ReworkedAbsorbedBlocksAbility absorbed = (ReworkedAbsorbedBlocksAbility)data.getPassiveAbility(ReworkedAbsorbedBlocksAbility.INSTANCE);
            if (this.state == BlackHoleRework.State.ABSORBING) {
                if ((absorbed == null || absorbed.getUncompressedBlocks().isEmpty()) && !hasTrackedDarkness(entity)) {
                    return;
                }

                this.state = BlackHoleRework.State.RELEASING;
                this.continuousComponent.startContinuity(entity, (float)RELEASE_TIME);
            } else {
                cleanupTrackedDarkness(entity);
                this.state = BlackHoleRework.State.ABSORBING;
                this.cooldownComponent.startCooldown(entity, 400.0F);
            }

        }
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            entity.addEffect(new EffectInstance((Effect)ModEffects.MOVEMENT_BLOCKED.get(), 5, 1, false, false));
            IAbilityData data = AbilityDataCapability.get(entity);
            ReworkedAbsorbedBlocksAbility absorbed = (ReworkedAbsorbedBlocksAbility)data.getPassiveAbility(ReworkedAbsorbedBlocksAbility.INSTANCE);
            if ((absorbed == null || absorbed.getUncompressedBlocks().isEmpty()) && !hasTrackedDarkness(entity)) {
                this.state = BlackHoleRework.State.ABSORBING;
                this.continuousComponent.stopContinuity(entity);
                this.cooldownComponent.startCooldown(entity, 400.0F);
            }

        }
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            if (this.state == BlackHoleRework.State.RELEASING) {
                this.chargeComponent.startCharging(entity, 10.0F);
            }

        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Black Hole", AbilityCategory.DEVIL_FRUITS, BlackHoleRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(200.0F, 400.0F), ChargeComponent.getTooltip(100.0F), ContinuousComponent.getTooltip(), RangeComponent.getTooltip(32.0F, RangeType.AOE)}).build();
    }

    private static void trackDarkness(LivingEntity entity, BlockPos pos) {
        Deque<BlockPos> tracked = TRACKED_DARKNESS.computeIfAbsent(entity.getUUID(), id -> new ArrayDeque<>());
        BlockPos immutablePos = pos.immutable();
        if (!tracked.contains(immutablePos)) {
            tracked.addLast(immutablePos);
        }
    }

    private static void untrackDarkness(LivingEntity entity, BlockPos pos) {
        Deque<BlockPos> tracked = TRACKED_DARKNESS.get(entity.getUUID());
        if (tracked == null) return;
        tracked.remove(pos);
        if (tracked.isEmpty()) {
            TRACKED_DARKNESS.remove(entity.getUUID());
        }
    }

    private static BlockPos pollTrackedDarkness(LivingEntity entity) {
        Deque<BlockPos> tracked = TRACKED_DARKNESS.get(entity.getUUID());
        if (tracked == null || tracked.isEmpty()) return null;
        BlockPos pos = tracked.pollLast();
        if (tracked.isEmpty()) {
            TRACKED_DARKNESS.remove(entity.getUUID());
        }
        return pos;
    }

    private static boolean hasTrackedDarkness(LivingEntity entity) {
        Deque<BlockPos> tracked = TRACKED_DARKNESS.get(entity.getUUID());
        return tracked != null && !tracked.isEmpty();
    }

    private static void retractDarkness(LivingEntity entity, BlockPos darknessPos) {
        if (entity.level.getBlockState(darknessPos).getBlock() == CartBlocks.REWORKED_DARKNESS_BLOCK.get()) {
            entity.level.setBlock(darknessPos, Blocks.AIR.defaultBlockState(), 3);
            WyHelper.spawnParticleEffect((ParticleEffect)ModParticleEffects.BLACK_HOLE.get(), entity, (double)darknessPos.getX(), (double)((float)darknessPos.getY() + 0.5F), (double)darknessPos.getZ());
        }
    }

    private static void cleanupTrackedDarkness(LivingEntity entity) {
        cleanupTrackedDarkness(entity.getUUID(), entity.level);
    }

    private static void cleanupTrackedDarkness(UUID casterId, World world) {
        Deque<BlockPos> tracked = TRACKED_DARKNESS.remove(casterId);
        if (tracked == null) return;
        for (BlockPos pos : tracked) {
            if (world.getBlockState(pos).getBlock() == CartBlocks.REWORKED_DARKNESS_BLOCK.get()) {
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    @Mod.EventBusSubscriber(modid = "kazimod")
    public static class CleanupHandler {
        @SubscribeEvent
        public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            cleanupTrackedDarkness(event.getPlayer().getUUID(), event.getPlayer().level);
        }

        @SubscribeEvent
        public static void onLivingDeath(LivingDeathEvent event) {
            cleanupTrackedDarkness(event.getEntityLiving().getUUID(), event.getEntityLiving().level);
        }
    }

    private static enum State {
        ABSORBING,
        RELEASING;

        private State() {
        }
    }
}
