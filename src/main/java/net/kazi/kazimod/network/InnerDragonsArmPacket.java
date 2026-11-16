package net.kazi.kazimod.network;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

/** The transformed arm persists independently of the temporary cinematic camera. */
public final class InnerDragonsArmPacket {
    public final int entityId;
    public final UUID uuid;
    public final ResourceLocation dimension;
    public final boolean transformed;

    public InnerDragonsArmPacket(int entityId, UUID uuid, ResourceLocation dimension, boolean transformed) {
        this.entityId = entityId;
        this.uuid = uuid;
        this.dimension = dimension;
        this.transformed = transformed;
    }

    public static void encode(InnerDragonsArmPacket packet, PacketBuffer buffer) {
        buffer.writeInt(packet.entityId);
        buffer.writeUUID(packet.uuid);
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeBoolean(packet.transformed);
    }

    public static InnerDragonsArmPacket decode(PacketBuffer buffer) {
        return new InnerDragonsArmPacket(buffer.readInt(), buffer.readUUID(), buffer.readResourceLocation(), buffer.readBoolean());
    }

    public static void handle(InnerDragonsArmPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientHandler.receive(packet));
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void receive(InnerDragonsArmPacket packet) {
            net.kazi.kazimod.client.nagi.InnerDragonsArmClient.receive(packet);
        }
    }
}
