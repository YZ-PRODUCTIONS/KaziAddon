package net.kazi.kazimod.network;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

/** Position updates do not restart playback; death stops the exact song session. */
public final class HyperGonerMusicPacket {
    public enum Action { START, POSITION, STOP }
    public final UUID session;
    public final UUID caster;
    public final ResourceLocation dimension;
    public final Vector3d position;
    public final Action action;

    public HyperGonerMusicPacket(UUID session, UUID caster, ResourceLocation dimension,
                                 Vector3d position, Action action) {
        if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z))
            throw new IllegalArgumentException("Invalid music source position");
        this.session = session;
        this.caster = caster;
        this.dimension = dimension;
        this.position = position;
        this.action = action;
    }

    public static void encode(HyperGonerMusicPacket packet, PacketBuffer buffer) {
        buffer.writeUUID(packet.session);
        buffer.writeUUID(packet.caster);
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeDouble(packet.position.x);
        buffer.writeDouble(packet.position.y);
        buffer.writeDouble(packet.position.z);
        buffer.writeEnum(packet.action);
    }

    public static HyperGonerMusicPacket decode(PacketBuffer buffer) {
        return new HyperGonerMusicPacket(buffer.readUUID(), buffer.readUUID(), buffer.readResourceLocation(),
                new Vector3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readEnum(Action.class));
    }

    public static void handle(HyperGonerMusicPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT)
            context.enqueueWork(() -> ClientHandler.receive(packet));
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        static void receive(HyperGonerMusicPacket packet) {
            net.kazi.kazimod.client.asriel.HyperGonerMusicClient.receive(packet);
        }
    }
}
