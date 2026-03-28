package net.kazi.kazimod.items;

import net.kazi.kazimod.config.KaziConfig;
import net.kazi.kazimod.entities.InfiniteVoidBarrierEntity;
import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
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
import java.util.List;

public class AwakeningEssenceItem extends Item {

    public static final String REGISTRY_NAME = "awakening_essence";
    public static final String SUMMONER_TAG  = "AwakeningEssenceSummoner";
    public static final String BARRIER_TAG   = "AwakeningBarrierUUID";
    public static final String SPHERE_TAG    = "AwakenningSphereUUID";

    private static final ResourceLocation KOKU_KOKU_NO_MI = new ResourceLocation("kazimod",     "koku_koku_no_mi");
    private static final ResourceLocation KAMA_KAMA_NO_MI = new ResourceLocation("cartaddon",   "kama_kama_no_mi");
    private static final ResourceLocation GOMU_GOMU_NO_MI = new ResourceLocation("mineminenomi","gomu_gomu_no_mi");

    public AwakeningEssenceItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .tab(xyz.pixelatedw.mineminenomi.init.ModCreativeTabs.MISC));
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity playerIn, Hand hand) {
        ItemStack stack = playerIn.getItemInHand(hand);

        if (world.isClientSide) return ActionResult.pass(stack);

        ServerPlayerEntity player      = (ServerPlayerEntity) playerIn;
        ServerWorld        serverWorld = (ServerWorld) world;

        // ── Validation — item is NOT consumed on any failure path ─────────────

        if (!CommonConfig.INSTANCE.hasAwakeningsEnabled()) {
            player.sendMessage(new StringTextComponent("\u00a7cAwakenings are disabled on this server."),
                    player.getUUID());
            return ActionResult.fail(stack);
        }

        IDevilFruit devilFruit = DevilFruitCapability.get(player);
        java.util.Optional<?> fruit = devilFruit == null
                ? java.util.Optional.empty()
                : devilFruit.getDevilFruit();

        boolean isKoku = fruit.isPresent() && KOKU_KOKU_NO_MI.equals(fruit.get());
        boolean isKama = fruit.isPresent() && KAMA_KAMA_NO_MI.equals(fruit.get());
        boolean isGomu = fruit.isPresent() && GOMU_GOMU_NO_MI.equals(fruit.get());

        if (!isKoku && !isKama && !isGomu) {
            player.sendMessage(new StringTextComponent(
                            "\u00a7cOnly a user eligible for awakening can use this item."),
                    player.getUUID());
            return ActionResult.fail(stack);
        }

        if (devilFruit.hasAwakenedFruit()) {
            player.sendMessage(new StringTextComponent("\u00a7eYour fruit is already awakened."),
                    player.getUUID());
            return ActionResult.fail(stack);
        }

        if (KaziConfig.INSTANCE.awakeningEssenceRequirePlayerKills.get()) {
            int requiredKills = KaziConfig.INSTANCE.awakeningEssenceRequiredPlayerKills.get();
            int playerKills = player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAYER_KILLS));
            if (playerKills < requiredKills) {
                player.sendMessage(new StringTextComponent(
                                "\u00a7cYou need at least " + requiredKills + " player kills to use Awakening Essence. Current: " + playerKills),
                        player.getUUID());
                return ActionResult.fail(stack);
            }
        }

        List<ServerPlayerEntity> nearbyPlayers = serverWorld.players();
        for (ServerPlayerEntity nearby : nearbyPlayers) {
            if (nearby == player) continue;
            if (player.distanceTo(nearby) <= 150.0F) {
                player.sendMessage(new StringTextComponent(
                                "\u00a7cAnother player is within 150 blocks. Clear the area before starting the trial."),
                        player.getUUID());
                return ActionResult.fail(stack);
            }
        }

        BlockPos spawnPos = findSpawnPos(serverWorld, player, 50);
        if (spawnPos == null) {
            player.sendMessage(new StringTextComponent("\u00a7cNo valid spawn location found nearby."),
                    player.getUUID());
            return ActionResult.fail(stack);
        }

        // ── Spawn boss ────────────────────────────────────────────────────────

        Entity bossEntity = null;
        net.minecraft.util.SoundEvent entranceSound = null;
        if (isKoku) {
            bossEntity = KaziEntities.GOJO_BOSS.get().spawn(
                    serverWorld, null, null, player, spawnPos, SpawnReason.EVENT, true, false);
            entranceSound = net.kazi.kazimod.init.KaziSounds.GOJO_BOSS_ENTRANCE_SFX.get();
        } else if (isKama) {
            bossEntity = KaziEntities.SUKUNA_BOSS.get().spawn(
                    serverWorld, null, null, player, spawnPos, SpawnReason.EVENT, true, false);
            entranceSound = net.kazi.kazimod.init.KaziSounds.SUKUNA_BOSS_ENTRANCE_SFX.get();
        } else {
            // isGomu
            bossEntity = KaziEntities.LUFFY_BOSS.get().spawn(
                    serverWorld, null, null, player, spawnPos, SpawnReason.EVENT, true, false);
            entranceSound = net.kazi.kazimod.init.KaziSounds.LUFFY_BOSS_ENTRANCE_SFX.get();
        }

        if (bossEntity == null) {
            player.sendMessage(new StringTextComponent("\u00a7cFailed to summon awakening boss."),
                    player.getUUID());
            return ActionResult.fail(stack);
        }

        // Tag boss with summoner UUID
        bossEntity.getPersistentData().putString(SUMMONER_TAG, player.getUUID().toString());

        // Spawn barrier
        InfiniteVoidBarrierEntity barrier = new InfiniteVoidBarrierEntity(
                KaziEntities.AWAKENING_BARRIER.get(), world);
        barrier.moveTo(playerIn.getX(), playerIn.getY(), playerIn.getZ(), 0.0F, 0.0F);
        barrier.setRadius(150.0F);
        barrier.setSpawner(playerIn);
        serverWorld.addFreshEntity(barrier);
        bossEntity.getPersistentData().putString(BARRIER_TAG, barrier.getUUID().toString());

        // Spawn sphere
        SphereEntity sphere = new SphereEntity(world, playerIn);
        sphere.moveTo(playerIn.getX(), playerIn.getY(), playerIn.getZ(), 0.0F, 0.0F);
        sphere.setRadius(150.0F);
        sphere.setColor(new Color(0, 0, 0, 180));
        sphere.setDetailLevel(32);
        serverWorld.addFreshEntity(sphere);
        bossEntity.getPersistentData().putString(SPHERE_TAG, sphere.getUUID().toString());

        // ── Item is NOT consumed here ─────────────────────────────────────────
        // It will be removed from the player's inventory by AwakeningEssenceDeathHandler
        // only when the trial is successfully completed (boss killed by the summoner).

        // Play entrance music at the boss spawn position for all nearby players
        if (entranceSound != null) {
            serverWorld.playSound(
                    null,
                    bossEntity.blockPosition(),
                    entranceSound,
                    net.minecraft.util.SoundCategory.PLAYERS,
                    3.0F, 1.0F
            );
        }

        player.sendMessage(new StringTextComponent(
                        "\u00a76The awakening trial has begun! Defeat the boss to awaken your fruit!"),
                player.getUUID());

        return ActionResult.success(stack);
    }

    /**
     * Called by AwakeningEssenceDeathHandler after the player successfully completes
     * the trial. Finds and removes one AwakeningEssenceItem from the player's inventory.
     */
    public static void consumeFromInventory(ServerPlayerEntity player) {
        if (player.abilities.instabuild) return;
        for (ItemStack s : player.inventory.items) {
            if (s.getItem() instanceof AwakeningEssenceItem) {
                s.shrink(1);
                return;
            }
        }
    }

    private BlockPos findSpawnPos(ServerWorld serverWorld, PlayerEntity player, int radius) {
        BlockPos origin = player.blockPosition();
        java.util.Random rand = player.getRandom();
        for (int i = 0; i < 10; i++) {
            double angle = rand.nextDouble() * 2 * Math.PI;
            int dx = (int) (Math.cos(angle) * radius);
            int dz = (int) (Math.sin(angle) * radius);
            BlockPos candidate = new BlockPos(
                    origin.getX() + dx,
                    origin.getY(),
                    origin.getZ() + dz);
            BlockPos ground = WyHelper.findOnGroundSpawnLocation(
                    serverWorld, KaziEntities.GOJO_BOSS.get(), candidate, 5);
            if (ground != null) return ground;
        }
        return null;
    }
}
