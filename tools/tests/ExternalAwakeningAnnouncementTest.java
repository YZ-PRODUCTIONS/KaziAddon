package net.kazi.kazimod.events;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit;

import java.util.Optional;

public final class ExternalAwakeningAnnouncementTest {
    private static final ServerPlayerEntity WATCHER = new ServerPlayerEntity();

    public static void main(String[] args) {
        ServerLifecycleHooks.server.list.players.add(WATCHER);
        for (String fruit : new String[]{"mineminenomi:supa_supa_no_mi", "cartaddon:goru_goru_no_mi"}) {
            ServerPlayerEntity player = player(fruit, false);
            login(player);
            awaken(player);
            tick(player);
            expect(1, "genuine " + fruit + " awakening broadcasts once");
            tick(player);
            expect(0, "remaining awakened does not repeat");

            player = player(fruit, true);
            login(player);
            tick(player);
            expect(0, "already-awakened login is silent");

            player = player(fruit, false);
            ExternalAwakeningAnnouncementHandler.onLogin(new PlayerEvent.PlayerLoggedInEvent(player));
            awaken(player);
            tick(player);
            expect(0, "late login restoration is silent");

            player = player(fruit, false);
            login(player);
            ExternalAwakeningAnnouncementHandler.onRespawn(new PlayerEvent.PlayerRespawnEvent(player));
            awaken(player);
            tick(player);
            expect(0, "respawn restoration is silent");

            player = player(fruit, false);
            login(player);
            ExternalAwakeningAnnouncementHandler.onChangedDimension(new PlayerEvent.PlayerChangedDimensionEvent(player));
            awaken(player);
            tick(player);
            expect(0, "dimension restoration is silent");

            ServerPlayerEntity original = player(fruit, false);
            login(original);
            player = player(fruit, false);
            ExternalAwakeningAnnouncementHandler.onClone(new PlayerEvent.Clone(player, original));
            awaken(player);
            tick(player);
            awaken(original);
            tick(original);
            expect(0, "clone restoration and discarded original are silent");

            player = player(fruit, false);
            login(player);
            ExternalAwakeningAnnouncementHandler.onLogout(new PlayerEvent.PlayerLoggedOutEvent(player));
            awaken(player);
            tick(player);
            expect(0, "logout clears observed state");

            player = player(fruit, false);
            login(player);
            awaken(player);
            AwakeningEssenceDeathHandler.broadcastAwakening(player, player.fruit);
            tick(player);
            expect(1, "explicit announcement is not repeated by tracker");
        }

        for (String fruit : new String[]{"mineminenomi:goro_goro_no_mi", "mineminenomi:kame_kame_no_mi"}) {
            ServerPlayerEntity player = player(fruit, false);
            login(player);
            awaken(player);
            tick(player);
            expect(0, "unrelated fruit is ignored");
        }

        ServerPlayerEntity changedFruit = player("mineminenomi:supa_supa_no_mi", false);
        login(changedFruit);
        changedFruit.fruit = new Fruit("cartaddon:goru_goru_no_mi", true);
        tick(changedFruit);
        expect(0, "changing to an already-awakened fruit is silent");

        PlayerEntity client = new PlayerEntity();
        client.fruit = new Fruit("mineminenomi:supa_supa_no_mi", true);
        ExternalAwakeningAnnouncementHandler.onPlayerTick(new TickEvent.PlayerTickEvent(client, TickEvent.Phase.END));
        expect(0, "client player is ignored");

        ServerPlayerEntity startTick = player("mineminenomi:supa_supa_no_mi", false);
        login(startTick);
        awaken(startTick);
        ExternalAwakeningAnnouncementHandler.onPlayerTick(new TickEvent.PlayerTickEvent(startTick, TickEvent.Phase.START));
        expect(0, "START phase is ignored");
        tick(startTick);
        expect(1, "END phase observes the awakening");
        System.out.println("External awakening announcement regression checks passed.");
    }

    private static ServerPlayerEntity player(String fruit, boolean awakened) {
        ServerPlayerEntity player = new ServerPlayerEntity();
        player.fruit = new Fruit(fruit, awakened);
        return player;
    }

    private static void login(ServerPlayerEntity player) {
        ExternalAwakeningAnnouncementHandler.onLogin(new PlayerEvent.PlayerLoggedInEvent(player));
        tick(player);
    }

    private static void awaken(ServerPlayerEntity player) {
        ((Fruit) player.fruit).awakened = true;
    }

    private static void tick(ServerPlayerEntity player) {
        ExternalAwakeningAnnouncementHandler.onPlayerTick(new TickEvent.PlayerTickEvent(player, TickEvent.Phase.END));
    }

    private static void expect(int count, String scenario) {
        if (WATCHER.messages != count) throw new AssertionError(scenario + ": " + WATCHER.messages + " messages, expected " + count);
        WATCHER.messages = 0;
    }

    private static final class Fruit implements IDevilFruit {
        private final ResourceLocation id;
        private boolean awakened;

        private Fruit(String fruit, boolean awakened) {
            String[] parts = fruit.split(":", 2);
            id = new ResourceLocation(parts[0], parts[1]);
            this.awakened = awakened;
        }

        public Optional<ResourceLocation> getDevilFruit() { return Optional.of(id); }
        public boolean hasAwakenedFruit() { return awakened; }
    }
}
