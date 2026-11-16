package net.kazi.kazimod.network;

import java.util.UUID;
import net.kazi.kazimod.abilities.cinematics.AwakeningCutsceneType;
import java.util.function.Supplier;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

/** The server selects the cinematic and supplies its authoritative shared timeline. */
public final class InnerDragonsCutscenePacket {
    public final int casterId;
    public final UUID casterUuid;
    public final long startTick;
    public final Vector3d origin;
    public final float yaw;
    public final boolean cancel;
    public final AwakeningCutsceneType type;

    public InnerDragonsCutscenePacket(int casterId, UUID casterUuid, long startTick,
                                      Vector3d origin, float yaw, boolean cancel) {
        this(casterId, casterUuid, startTick, origin, yaw, cancel, false);
    }

    public InnerDragonsCutscenePacket(int casterId, UUID casterUuid, long startTick,
            Vector3d origin, float yaw, boolean cancel, boolean goru) {
        this(casterId, casterUuid, startTick, origin, yaw, cancel,
                goru ? AwakeningCutsceneType.EA_DRAW : AwakeningCutsceneType.INNER_DRAGONS);
    }

    public InnerDragonsCutscenePacket(int casterId, UUID casterUuid, long startTick,
            Vector3d origin, float yaw, boolean cancel, AwakeningCutsceneType type) {
        this.type = java.util.Objects.requireNonNull(type);

        this.casterId = casterId;
        this.casterUuid = casterUuid;
        this.startTick = startTick;
        this.origin = origin;
        this.yaw = yaw;
        this.cancel = cancel;
    }

    public static void encode(InnerDragonsCutscenePacket packet, PacketBuffer buffer) {
        buffer.writeInt(packet.casterId);
        buffer.writeUUID(packet.casterUuid);
        buffer.writeLong(packet.startTick);
        buffer.writeDouble(packet.origin.x);
        buffer.writeDouble(packet.origin.y);
        buffer.writeDouble(packet.origin.z);
        buffer.writeFloat(packet.yaw);
        buffer.writeBoolean(packet.cancel);
        buffer.writeVarInt(packet.type.wireId);
    }

    public static InnerDragonsCutscenePacket decode(PacketBuffer buffer) {
        return new InnerDragonsCutscenePacket(buffer.readInt(), buffer.readUUID(), buffer.readLong(),
                new Vector3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                buffer.readFloat(), buffer.readBoolean(), AwakeningCutsceneType.fromWire(buffer.readVarInt()));
    }

    public static void handle(InnerDragonsCutscenePacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientHandler.receive(packet));
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void receive(InnerDragonsCutscenePacket packet) {
            net.kazi.kazimod.client.nagi.InnerDragonsCutsceneClient.receive(packet);
        }
    }
}
