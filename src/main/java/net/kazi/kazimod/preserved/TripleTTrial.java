package net.kazi.kazimod.preserved;

import net.kazi.kazimod.config.KaziConfig;
import net.kazi.kazimod.entities.InfiniteVoidBarrierEntity;
import net.kazi.kazimod.events.AwakeningEssenceDeathHandler;
import net.kazi.kazimod.init.KaziEntities;
import net.kazi.kazimod.init.KaziSounds;
import net.kazi.kazimod.items.AwakeningEssenceItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Stats;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.config.CommonConfig;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit;
import xyz.pixelatedw.mineminenomi.entities.SphereEntity;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import java.awt.Color;
import java.util.UUID;

public final class TripleTTrial {
    private static final ResourceLocation FRUIT = new ResourceLocation("kazimod", "ki_ki_no_mi_model_tung_tung_tung_sahur");

    // Null leaves the baseline's use method completely in charge of every other fruit.
    public static ActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        IDevilFruit fruit = DevilFruitCapability.get(user);
        if (fruit == null || !fruit.getDevilFruit().filter(FRUIT::equals).isPresent()) return null;
        ItemStack stack = user.getItemInHand(hand);
        if (world.isClientSide) return ActionResult.pass(stack);
        ServerPlayerEntity player = (ServerPlayerEntity) user;
        ServerWorld level = (ServerWorld) world;
        if (!CommonConfig.INSTANCE.hasAwakeningsEnabled()) return fail(player, stack, "Awakenings are disabled on this server.");
        if (fruit.hasAwakenedFruit()) return fail(player, stack, "Your fruit is already awakened.");
        String active = player.getPersistentData().getString(AwakeningEssenceItem.ACTIVE_TRIAL_BOSS_TAG);
        if (!active.isEmpty()) {
            try {
                UUID id = UUID.fromString(active);
                for (ServerWorld dimension : level.getServer().getAllLevels()) {
                    Entity boss = dimension.getEntity(id);
                    if (boss != null && boss.isAlive()) return fail(player, stack, "You already have an awakening challenge in progress.");
                }
            } catch (IllegalArgumentException ignored) { }
            player.getPersistentData().remove(AwakeningEssenceItem.ACTIVE_TRIAL_BOSS_TAG);
        }
        long now = System.currentTimeMillis();
        long remaining = player.getPersistentData().getLong(AwakeningEssenceItem.COOLDOWN_END_TAG) - now;
        if (remaining > 0) return fail(player, stack, "Wait " + ((remaining + 999) / 1000) + " seconds before starting another awakening challenge.");
        if (KaziConfig.INSTANCE.awakeningEssenceRequirePlayerKills.get()
                && player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAYER_KILLS)) < KaziConfig.INSTANCE.awakeningEssenceRequiredPlayerKills.get())
            return fail(player, stack, "You do not have enough player kills to use Awakening Essence.");
        for (ServerPlayerEntity other : level.players()) {
            if (other != player && player.distanceTo(other) <= 150) return fail(player, stack, "Another player is within 150 blocks. Clear the area before starting the trial.");
        }
        BlockPos spawn = null;
        for (int i = 0; i < 10 && spawn == null; i++) {
            double angle = player.getRandom().nextDouble() * Math.PI * 2;
            spawn = WyHelper.findOnGroundSpawnLocation(level, KaziEntities.TRUE_FORM_REBELUS_BOSS.get(),
                    player.blockPosition().offset((int) (Math.cos(angle) * 50), 0, (int) (Math.sin(angle) * 50)), 5);
        }
        if (spawn == null) return fail(player, stack, "No valid spawn location found nearby.");
        Entity boss = KaziEntities.TRUE_FORM_REBELUS_BOSS.get().spawn(level, null, null, player, spawn, SpawnReason.EVENT, true, false);
        if (boss == null) return fail(player, stack, "Failed to summon awakening boss.");
        boss.getPersistentData().putString(AwakeningEssenceItem.SUMMONER_TAG, player.getUUID().toString());
        AwakeningEssenceDeathHandler.registerTrialBoss(boss.getUUID());
        player.getPersistentData().putString(AwakeningEssenceItem.ACTIVE_TRIAL_BOSS_TAG, boss.getUUID().toString());
        player.getPersistentData().putLong(AwakeningEssenceItem.COOLDOWN_END_TAG, now + 300000);
        InfiniteVoidBarrierEntity barrier = new InfiniteVoidBarrierEntity(KaziEntities.AWAKENING_BARRIER.get(), world);
        barrier.moveTo(player.getX(), player.getY(), player.getZ(), 0, 0);
        barrier.setRadius(150);
        barrier.setSpawner(player);
        level.addFreshEntity(barrier);
        boss.getPersistentData().putString(AwakeningEssenceItem.BARRIER_TAG, barrier.getUUID().toString());
        SphereEntity sphere = new SphereEntity(world, player);
        sphere.moveTo(player.getX(), player.getY(), player.getZ(), 0, 0);
        sphere.setRadius(150);
        sphere.setColor(new Color(0, 0, 0, 180));
        sphere.setDetailLevel(16);
        level.addFreshEntity(sphere);
        boss.getPersistentData().putString(AwakeningEssenceItem.SPHERE_TAG, sphere.getUUID().toString());
        level.playSound(null, spawn, KaziSounds.TUNG_SFX.get(), SoundCategory.PLAYERS, 3, 1);
        player.sendMessage(new StringTextComponent("The awakening trial has begun! Defeat the boss to awaken your fruit!"), player.getUUID());
        return ActionResult.success(stack);
    }
    private static ActionResult<ItemStack> fail(PlayerEntity player, ItemStack stack, String message) {
        player.sendMessage(new StringTextComponent(message), player.getUUID());
        return ActionResult.fail(stack);
    }
}
