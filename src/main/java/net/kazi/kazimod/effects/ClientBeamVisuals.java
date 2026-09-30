package net.kazi.kazimod.effects;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.kazi.kazimod.network.BeamVisualPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;

@Mod.EventBusSubscriber(modid = "kazimod", value = Dist.CLIENT)
public final class ClientBeamVisuals {
    private static final Map<UUID, Entry> DATA = new LinkedHashMap<>();
    private static ClientWorld world;

    private ClientBeamVisuals() { }

    private static void bindWorld() {
        ClientWorld next = Minecraft.getInstance().level;
        if (world != next) {
            DATA.clear();
            world = next;
        }
    }

    public static void receive(BeamVisualPacket packet) {
        bindWorld();
        if (world == null || !world.dimension().location().equals(packet.dimension)) return;
        DATA.put(packet.uuid, new Entry(packet, world.getGameTime()));
        // Also allow packets arriving just before their entity spawn without an unbounded cache.
        while (DATA.size() > 4096) DATA.remove(DATA.keySet().iterator().next());
    }

    public static BeamVisualPacket get(LightningEntity beam) {
        bindWorld();
        Entry entry = DATA.get(beam.getUUID());
        return entry == null || entry.packet.entityId != beam.getId() ? null : entry.packet;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        bindWorld();
        if (world == null || world.getGameTime() % 20 != 0) return;
        DATA.values().removeIf(entry -> {
            Entity entity = world.getEntity(entry.packet.entityId);
            boolean present = entity instanceof LightningEntity && entity.isAlive()
                    && entity.getUUID().equals(entry.packet.uuid);
            return !present && world.getGameTime() - entry.received > 40;
        });
    }

    private static final class Entry {
        final BeamVisualPacket packet;
        final long received;
        Entry(BeamVisualPacket packet, long received) { this.packet = packet; this.received = received; }
    }
}
