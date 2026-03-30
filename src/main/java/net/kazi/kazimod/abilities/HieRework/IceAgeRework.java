//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.HieRework;

import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.MrMagicalCart.cartaddon.init.CartBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.protection.BlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.protection.DefaultProtectionRules;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.BlockPlacingHelper;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class IceAgeRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "ice_age", new Pair[]{ImmutablePair.of("Freezes a large area around the user and everyone inside of it", (Object)null)});
    private static final int CHARGE_TIME = 100;
    private static final int COOLDOWN = 600;
    private static final int ICE_RANGE = 64;
    private static final float ENTITY_FREEZE_RANGE = 1.5F;
    private static final int ICE_RETRACT_PER_TICK = 30;
    public static final AbilityCore<IceAgeRework> INSTANCE;
    private static final BlockProtectionRule PROTECTION_RULE;
    private static final Map<UUID, TrackedIceField> TRACKED_ICE = new HashMap<>();
    private final BlockPlacingHelper blockPlacingHelper = new BlockPlacingHelper();
    private final ChargeComponent chargeComponent = (new ChargeComponent(this, (comp) -> (double)comp.getChargePercentage() > (double)0.5F)).addStartEvent(100, this::startChargeEvent).addTickEvent(100, this::duringChargeEvent).addEndEvent(100, this::stopChargeEvent);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);

    public IceAgeRework(AbilityCore<IceAgeRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.chargeComponent, this.rangeComponent, this.hitTrackerComponent});
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        this.chargeComponent.startCharging(entity, 100.0F);
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            this.hitTrackerComponent.clearHits();
            this.blockPlacingHelper.clearList();
            double radiusXZ = (double)64.0F;
            double radiusY = (double)9.0F;
            entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.ICE_AGE_SFX.get(), SoundCategory.PLAYERS, 10.0F, 1.0F);
            WyHelper.spawnParticleEffect((ParticleEffect)ModParticleEffects.ICE_AGE.get(), entity, entity.getX(), entity.getY(), entity.getZ());
            BlockPos.Mutable surfaceScan = new BlockPos.Mutable();
            int baseY = entity.blockPosition().getY();

            for(double x = -radiusXZ; x < radiusXZ; ++x) {
                for(double z = -radiusXZ; z < radiusXZ; ++z) {
                    double posX = entity.getX() + x + (!(x < -WyHelper.randomWithRange((int)(radiusXZ * (double)0.5F), (int)(radiusXZ * (double)0.75F))) && !(x > WyHelper.randomWithRange((int)(radiusXZ * (double)0.5F), (int)(radiusXZ * (double)0.75F))) ? (double)0.0F : WyHelper.randomWithRange(-5, 5));
                    double posZ = entity.getZ() + z + (!(z < -WyHelper.randomWithRange((int)(radiusXZ * (double)0.5F), (int)(radiusXZ * (double)0.75F))) && !(z > WyHelper.randomWithRange((int)(radiusXZ * (double)0.5F), (int)(radiusXZ * (double)0.75F))) ? (double)0.0F : WyHelper.randomWithRange(-5, 5));
                    int blockX = net.minecraft.util.math.MathHelper.floor(posX);
                    int blockZ = net.minecraft.util.math.MathHelper.floor(posZ);

                    for(int y = baseY + (int)radiusY; y >= baseY - (int)radiusY; --y) {
                        surfaceScan.set(blockX, y, blockZ);
                        BlockState surfaceState = entity.level.getBlockState(surfaceScan);
                        if (!surfaceState.getMaterial().isSolidBlocking()) {
                            continue;
                        }

                        BlockState blockAboveSurface = entity.level.getBlockState(surfaceScan.above());
                        boolean canTargetPos = surfaceState.getBlock() != Blocks.BEDROCK
                                && surfaceState.getBlock() != Blocks.BLUE_ICE
                                && blockAboveSurface.getBlock() != CartBlocks.REWORKED_DARKNESS_BLOCK.get();
                        if (canTargetPos) {
                            int priority = (int)(x * x + z * z);
                            this.blockPlacingHelper.addBlockPos(surfaceScan.immutable(), priority);
                        }
                        break;
                    }
                }
            }
        }

    }

    private void duringChargeEvent(LivingEntity entity, IAbility ability) {
        entity.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 2, 2, false, false));
        Set<BlockPos> blockList = this.blockPlacingHelper.getBlockList();
        int finished = blockList.size() / 100;
        Iterator<BlockPos> iterator = blockList.iterator();

        while(iterator.hasNext()) {
            BlockPos blockPos = (BlockPos)iterator.next();
            if (finished-- < 0) {
                break;
            }

            BlockState originalState = entity.level.getBlockState(blockPos);
            if (originalState.getBlock() == CartBlocks.REWORKED_DARKNESS_BLOCK.get()
                    || entity.level.getBlockState(blockPos.above()).getBlock() == CartBlocks.REWORKED_DARKNESS_BLOCK.get()) {
                iterator.remove();
                continue;
            }
            boolean placed = AbilityHelper.placeBlockIfAllowed(entity, blockPos, Blocks.BLUE_ICE.defaultBlockState(), 3, PROTECTION_RULE);
            if (!placed) {
                entity.level.setBlock(blockPos, Blocks.BLUE_ICE.defaultBlockState(), 3);
                placed = entity.level.getBlockState(blockPos).getBlock() == Blocks.BLUE_ICE;
            }
            if (placed) {
                trackIce(entity, blockPos, originalState);
            }

            for(LivingEntity target : this.rangeComponent.getTargetsInArea(entity, blockPos, 1.5F)) {
                if (this.hitTrackerComponent.canHit(target)) {
                    EffectInstance instance = new EffectInstance((Effect)ModEffects.FROZEN.get(), 100, 0);
                    target.addEffect(instance);
                }
            }

            iterator.remove();
        }

    }

    private void stopChargeEvent(LivingEntity entity, IAbility ability) {
        startRetracting(entity);
        this.cooldownComponent.startCooldown(entity, (float)COOLDOWN);
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Ice Age", AbilityCategory.DEVIL_FRUITS, IceAgeRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip((float)COOLDOWN), ChargeComponent.getTooltip(100.0F), RangeComponent.getTooltip(64.0F, RangeType.AOE)}).setSourceElement(SourceElement.ICE).build();
        PROTECTION_RULE = (new BlockProtectionRule.Builder(new BlockProtectionRule[]{DefaultProtectionRules.CORE_FOLIAGE_ORE_LIQUID})).addReplaceRules((world, pos, state) -> {
            if (state.getBlock().equals(Blocks.SNOW) && (Integer)state.getValue(SnowBlock.LAYERS) > 5) {
                world.setBlock(pos, Blocks.BLUE_ICE.defaultBlockState(), 3);
                return true;
            } else {
                return false;
            }
        }).build();
    }

    private static void trackIce(LivingEntity entity, BlockPos pos, BlockState originalState) {
        TrackedIceField field = TRACKED_ICE.computeIfAbsent(entity.getUUID(), id -> new TrackedIceField(entity.level));
        BlockPos immutable = pos.immutable();
        if (!field.positions.containsKey(immutable)) {
            field.positions.put(immutable, originalState);
            field.order.add(immutable);
        }
    }

    private static void startRetracting(LivingEntity entity) {
        TrackedIceField field = TRACKED_ICE.get(entity.getUUID());
        if (field != null) {
            field.retracting = true;
        }
    }

    private static void retractSome(UUID casterId, TrackedIceField field) {
        int remaining = ICE_RETRACT_PER_TICK;
        while (remaining-- > 0 && !field.positions.isEmpty()) {
            BlockPos pos = field.order.remove(field.order.size() - 1);
            BlockState originalState = field.positions.remove(pos);
            if (field.world.getBlockState(pos).getBlock() == Blocks.BLUE_ICE) {
                field.world.setBlock(pos, originalState, 3);
            }
        }
        if (field.positions.isEmpty()) {
            TRACKED_ICE.remove(casterId);
        }
    }

    private static void clearTrackedIce(UUID casterId, TrackedIceField field) {
        while (!field.positions.isEmpty()) {
            BlockPos pos = field.order.remove(field.order.size() - 1);
            BlockState originalState = field.positions.remove(pos);
            if (field.world.getBlockState(pos).getBlock() == Blocks.BLUE_ICE) {
                field.world.setBlock(pos, originalState, 3);
            }
        }
        TRACKED_ICE.remove(casterId);
    }

    private static void clearAllTrackedIce() {
        for (TrackedIceField field : TRACKED_ICE.values()) {
            while (!field.positions.isEmpty()) {
                BlockPos pos = field.order.remove(field.order.size() - 1);
                BlockState originalState = field.positions.remove(pos);
                if (field.world.getBlockState(pos).getBlock() == Blocks.BLUE_ICE) {
                    field.world.setBlock(pos, originalState, 3);
                }
            }
        }
        TRACKED_ICE.clear();
    }

    private static class TrackedIceField {
        private final net.minecraft.world.World world;
        private final Map<BlockPos, BlockState> positions = new LinkedHashMap<>();
        private final List<BlockPos> order = new LinkedList<>();
        private boolean retracting;

        private TrackedIceField(net.minecraft.world.World world) {
            this.world = world;
            this.retracting = false;
        }
    }

    @Mod.EventBusSubscriber(modid = "kazimod")
    public static class CleanupHandler {
        @SubscribeEvent
        public static void onWorldTick(TickEvent.WorldTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.world.isClientSide) return;

            java.util.List<UUID> retractingFields = new java.util.ArrayList<>();
            java.util.List<UUID> orphanedFields = new java.util.ArrayList<>();
            ServerWorld serverWorld = event.world instanceof ServerWorld ? (ServerWorld) event.world : null;

            for (Map.Entry<UUID, TrackedIceField> entry : TRACKED_ICE.entrySet()) {
                if (entry.getValue().world != event.world) {
                    continue;
                }

                if (serverWorld != null) {
                    net.minecraft.entity.Entity owner = serverWorld.getEntity(entry.getKey());
                    if (!(owner instanceof LivingEntity) || !owner.isAlive()) {
                        orphanedFields.add(entry.getKey());
                        continue;
                    }
                }

                if (entry.getValue().retracting) {
                    retractingFields.add(entry.getKey());
                }
            }

            for (UUID casterId : orphanedFields) {
                TrackedIceField field = TRACKED_ICE.get(casterId);
                if (field != null) {
                    clearTrackedIce(casterId, field);
                }
            }

            for (UUID casterId : retractingFields) {
                TrackedIceField field = TRACKED_ICE.get(casterId);
                if (field != null) {
                    retractSome(casterId, field);
                }
            }
        }

        @SubscribeEvent
        public static void onLivingDeath(LivingDeathEvent event) {
            UUID casterId = event.getEntityLiving().getUUID();
            TrackedIceField field = TRACKED_ICE.get(casterId);
            if (field != null) {
                clearTrackedIce(casterId, field);
            }
        }

        @SubscribeEvent
        public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
            UUID casterId = event.getPlayer().getUUID();
            TrackedIceField field = TRACKED_ICE.get(casterId);
            if (field != null) {
                clearTrackedIce(casterId, field);
            }
        }

        @SubscribeEvent
        public static void onServerStopping(FMLServerStoppingEvent event) {
            clearAllTrackedIce();
        }
    }
}
