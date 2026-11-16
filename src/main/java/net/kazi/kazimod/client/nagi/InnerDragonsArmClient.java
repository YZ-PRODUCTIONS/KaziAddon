package net.kazi.kazimod.client.nagi;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsSequence;
import net.kazi.kazimod.network.InnerDragonsArmPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Only the server decides whether the passive is active; health alone cannot grant this appearance. */
@Mod.EventBusSubscriber(modid = "kazimod", value = Dist.CLIENT)
public final class InnerDragonsArmClient {
    private static final Map<UUID, Integer> TRANSFORMED = new HashMap<>();
    private static ClientWorld world;

    private InnerDragonsArmClient() { }

    public static void receive(InnerDragonsArmPacket packet) {
        checkWorld();
        if (world == null || !world.dimension().location().equals(packet.dimension)) return;
        if (packet.transformed) TRANSFORMED.put(packet.uuid, packet.entityId);
        else TRANSFORMED.remove(packet.uuid);
    }

    public static boolean isTransformed(Entity entity) {
        checkWorld();
        if (!(entity instanceof LivingEntity) || entity.level != world || entity.removed
                || !entity.isAlive() || entity.isSpectator()) return false;
        LivingEntity living = (LivingEntity) entity;
        if (!InnerDragonsSequence.belowThreshold(living.getHealth(), living.getMaxHealth())) return false;
        Integer id = TRANSFORMED.get(entity.getUUID());
        return id != null && id == entity.getId();
    }

    private static void checkWorld() {
        ClientWorld current = Minecraft.getInstance().level;
        if (world != current) {
            TRANSFORMED.clear();
            world = current;
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) checkWorld();
    }

    @SubscribeEvent
    public static void respawn(ClientPlayerNetworkEvent.RespawnEvent event) {
        // Respawning in the same dimension can reuse the local player's entity ID.
        TRANSFORMED.remove(event.getNewPlayer().getUUID());
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        TRANSFORMED.clear();
        world = null;
    }
}
