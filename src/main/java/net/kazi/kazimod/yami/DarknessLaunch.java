package net.kazi.kazimod.yami;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.MrMagicalCart.cartaddon.init.CartBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.blocks.traps.TrapAbilityBlock;
import xyz.pixelatedw.mineminenomi.init.ModBlocks;

/** Contact is counted once per server tick, not once per overlapping block. */
public final class DarknessLaunch {
    static final int HOLD_TICKS = 100;
    static final int RELEASE_TICKS = 30;
    // With normal living-entity gravity and drag, this rises 20 blocks in 21 ticks.
    static final double LAUNCH_SPEED = 1.9988269041676014;
    private static final Map<LivingEntity, Contact> CONTACTS = Collections.synchronizedMap(new WeakHashMap<>());

    private static final class Contact {
        final ResourceLocation dimension;
        long lastTick = Long.MIN_VALUE;
        long releaseUntil;
        int ticks;
        boolean pending;
        Contact(LivingEntity entity) { dimension = entity.level.dimension().location(); }
    }

    private DarknessLaunch() {}

    public static boolean isDarkness(Block block) {
        return block == ModBlocks.DARKNESS.get() || block == CartBlocks.REWORKED_DARKNESS_BLOCK.get();
    }

    private static Contact state(LivingEntity entity) {
        Contact state = CONTACTS.get(entity);
        if (state != null && !state.dimension.equals(entity.level.dimension().location())) {
            CONTACTS.remove(entity);
            return null;
        }
        return state;
    }

    public static boolean bypasses(Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;
        Contact state = state((LivingEntity) entity);
        long now = entity.level.getGameTime();
        return entity.isAlive() && state != null && state.releaseUntil > now
                && state.releaseUntil - now <= RELEASE_TICKS;
    }

    public static boolean contact(Block block, Entity entity) {
        if (!isDarkness(block) || !(entity instanceof LivingEntity)) return false;
        LivingEntity living = (LivingEntity) entity;
        if (!living.isAlive() || living.isSpectator()
                || living instanceof PlayerEntity && ((PlayerEntity) living).isCreative()) {
            CONTACTS.remove(living);
            return false;
        }
        if (bypasses(living)) return true;
        if (living.level.isClientSide) return false;
        if (((TrapAbilityBlock) block).check(living)) {
            CONTACTS.remove(living);
            return false;
        }
        Contact state = state(living);
        if (state == null) {
            state = new Contact(living);
            CONTACTS.put(living, state);
        }
        long now = living.level.getGameTime();
        if (state.lastTick == now) return false;
        state.ticks = state.lastTick == now - 1 ? state.ticks + 1 : 1;
        state.lastTick = now;
        if (state.ticks < HOLD_TICKS) return false;

        beginRelease(living);
        state(living).pending = true;
        return true;
    }

    public static void finishTick(LivingEntity entity) {
        if (entity.level.isClientSide) return;
        Contact state = state(entity);
        if (state == null || !state.pending) return;
        state.pending = false;
        if (!entity.isAlive()) { CONTACTS.remove(entity); return; }
        // Apply after travel, so this tick's gravity does not consume part of the launch.
        state.releaseUntil = entity.level.getGameTime() + RELEASE_TICKS;
        entity.makeStuckInBlock(Blocks.AIR.defaultBlockState(), Vector3d.ZERO);
        entity.stopRiding();
        DarknessLaunchPacket.send(entity);
        Vector3d motion = entity.getDeltaMovement();
        AbilityHelper.setDeltaMovement(entity, motion.x, LAUNCH_SPEED, motion.z);
        entity.hasImpulse = true;
    }

    static void beginRelease(LivingEntity entity) {
        Contact state = new Contact(entity);
        state.releaseUntil = entity.level.getGameTime() + RELEASE_TICKS;
        CONTACTS.put(entity, state);
        // Clear the previous block callback's sticky multiplier before applying velocity.
        entity.makeStuckInBlock(Blocks.AIR.defaultBlockState(), Vector3d.ZERO);
        entity.fallDistance = 0;
    }
}
