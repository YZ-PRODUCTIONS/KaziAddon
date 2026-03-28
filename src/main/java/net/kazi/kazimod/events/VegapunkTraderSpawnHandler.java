package net.kazi.kazimod.events;

import net.kazi.kazimod.config.KaziConfig;
import net.kazi.kazimod.entities.VegapunkTraderEntity;
import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

import java.util.List;

@Mod.EventBusSubscriber(modid = "kazimod")
public class VegapunkTraderSpawnHandler {

    // 1 real-life hour = 20 ticks/sec * 60 sec * 60 min = 72,000 ticks
    private static final int SPAWN_INTERVAL = 72000;
    private static int tickCounter = 0;

    // Use WorldTickEvent — it provides event.world directly.
    // Only process on the overworld (World.OVERWORLD) so the counter
    // doesn't increment once per loaded dimension per tick.
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        // Only run on the server-side overworld to avoid double-counting
        if (event.world.isClientSide) return;
        if (event.world.dimension() != World.OVERWORLD) return;
        if (KaziConfig.INSTANCE.disableVegapunkSpawns.get()) return;

        tickCounter++;
        if (tickCounter < SPAWN_INTERVAL) return;
        tickCounter = 0;

        ServerWorld serverWorld = (ServerWorld) event.world;

        List<ServerPlayerEntity> players = serverWorld.players();
        if (players.isEmpty()) return;

        // Pick exactly 1 random player
        ServerPlayerEntity target = players.get(
                serverWorld.random.nextInt(players.size())
        );

        EntityType<VegapunkTraderEntity> entityType = KaziEntities.VEGAPUNK_TRADER.get();
        BlockPos playerPos = target.blockPosition();
        BlockPos spawnPos = WyHelper.findOnGroundSpawnLocation(serverWorld, entityType, playerPos, 5);
        if (spawnPos == null) spawnPos = playerPos;

        VegapunkTraderEntity trader = entityType.spawn(
                serverWorld, null, null, target, spawnPos, SpawnReason.EVENT, true, false
        );

        if (trader != null) {
            String dimName  = serverWorld.dimension().location().getPath();
            int x = spawnPos.getX();
            int y = spawnPos.getY();
            int z = spawnPos.getZ();
            String playerName = target.getName().getString();

            StringTextComponent message = new StringTextComponent(
                    "\u00A76[\u00A7eVegapunk Trader\u00A76] \u00A7fDr. Vegapunk's trader has appeared near \u00A7b" +
                            playerName +
                            "\u00A7f at \u00A7a" + x + ", " + y + ", " + z +
                            "\u00A7f in the \u00A7d" + dimName +
                            "\u00A7f! Selling human modification canisters for \u00A7c100,000 Belly\u00A7f each."
            );

            // Broadcast to every online player
            for (ServerPlayerEntity player : serverWorld.players()) {
                player.sendMessage(message, player.getUUID());
            }
        }
    }
}
