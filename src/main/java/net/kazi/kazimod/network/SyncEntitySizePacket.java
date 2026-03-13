package net.kazi.kazimod.network;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncEntitySizePacket {

    private final int entityId;

    public SyncEntitySizePacket(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(SyncEntitySizePacket msg, PacketBuffer buf) {
        buf.writeInt(msg.entityId);
    }

    public static SyncEntitySizePacket decode(PacketBuffer buf) {
        return new SyncEntitySizePacket(buf.readInt());
    }

    public static void handle(SyncEntitySizePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientHandler.handle(msg));
        ctx.get().setPacketHandled(true);
    }

    // Separate inner class with @OnlyIn so the classloader never touches
    // Minecraft/ClientWorld on the server — exactly as mine-mine-no-mi does it
    @OnlyIn(Dist.CLIENT)
    public static class ClientHandler {
        public static void handle(SyncEntitySizePacket msg) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            Entity entity = mc.level.getEntity(msg.entityId);
            if (entity == null) return;

            // Exact same call as SRecalculateEyeHeightPacket$ClientHandler:
            // func_213323_x_ = refreshDimensions()
            entity.refreshDimensions();
        }
    }
}