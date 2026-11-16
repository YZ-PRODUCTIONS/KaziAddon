package net.kazi.kazimod.events;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit;

import java.util.Map;
import java.util.WeakHashMap;

/** Announces Supa/Goru awakenings granted outside Kazi's essence trials. */
@Mod.EventBusSubscriber(modid = "kazimod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ExternalAwakeningAnnouncementHandler {
    private static final ResourceLocation SUPA = new ResourceLocation("mineminenomi", "supa_supa_no_mi");
    private static final ResourceLocation GORU = new ResourceLocation("cartaddon", "goru_goru_no_mi");
    private static final Map<ServerPlayerEntity, ObservedFruit> OBSERVED = new WeakHashMap<>();

    private ExternalAwakeningAnnouncementHandler() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity player = (ServerPlayerEntity) event.player;
        IDevilFruit fruit = DevilFruitCapability.get(player);
        ObservedFruit current = snapshot(fruit, false);
        if (current == null) {
            OBSERVED.remove(player);
            return;
        }

        ObservedFruit previous = OBSERVED.put(player, current);
        if (previous != null && !previous.restoring && previous.fruit.equals(current.fruit)
                && !previous.awakened && current.awakened) {
            AwakeningEssenceDeathHandler.broadcastAwakening(player, fruit);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        baseline(event.getPlayer());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        baseline(event.getPlayer());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        baseline(event.getPlayer());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClone(PlayerEvent.Clone event) {
        OBSERVED.remove(event.getOriginal());
        baseline(event.getPlayer());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        OBSERVED.remove(event.getPlayer());
    }

    // Explicit essence announcements must not be repeated by the next player tick.
    static void recordAnnouncement(ServerPlayerEntity player, IDevilFruit fruit) {
        remember(player, snapshot(fruit, false));
    }

    private static void baseline(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity) {
            // Rebaseline once more on the next END tick, after capability restoration.
            remember((ServerPlayerEntity) player, snapshot(DevilFruitCapability.get(player), true));
        }
    }

    private static void remember(ServerPlayerEntity player, ObservedFruit state) {
        if (state == null) OBSERVED.remove(player);
        else OBSERVED.put(player, state);
    }

    private static ObservedFruit snapshot(IDevilFruit fruit, boolean restoring) {
        ResourceLocation id = fruit == null ? null : fruit.getDevilFruit().orElse(null);
        if (!SUPA.equals(id) && !GORU.equals(id)) return null;
        return new ObservedFruit(id, fruit.hasAwakenedFruit(), restoring);
    }

    private static final class ObservedFruit {
        private final ResourceLocation fruit;
        private final boolean awakened;
        private final boolean restoring;

        private ObservedFruit(ResourceLocation fruit, boolean awakened, boolean restoring) {
            this.fruit = fruit;
            this.awakened = awakened;
            this.restoring = restoring;
        }
    }
}
