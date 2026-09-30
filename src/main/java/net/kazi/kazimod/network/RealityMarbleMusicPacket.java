package net.kazi.kazimod.network;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

public final class RealityMarbleMusicPacket {
    public enum SecondarySound { NONE, INFINITE_CREATION, MUGEN }

    private final UUID session;
    private final boolean playing;
    private final SecondarySound secondary;

    public RealityMarbleMusicPacket(UUID session, boolean playing) {
        this(session, playing, SecondarySound.NONE);
    }

    public RealityMarbleMusicPacket(UUID session, boolean playing, SecondarySound secondary) {
        this.session = session;
        this.playing = playing;
        this.secondary = secondary;
    }

    public static void encode(RealityMarbleMusicPacket packet, PacketBuffer buffer) {
        buffer.writeUUID(packet.session);
        buffer.writeBoolean(packet.playing);
        buffer.writeEnum(packet.secondary);
    }

    public static RealityMarbleMusicPacket decode(PacketBuffer buffer) {
        return new RealityMarbleMusicPacket(buffer.readUUID(), buffer.readBoolean(), buffer.readEnum(SecondarySound.class));
    }

    public static void handle(RealityMarbleMusicPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientHandler.handle(packet));
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void handle(RealityMarbleMusicPacket packet) {
            net.kazi.kazimod.effects.RealityMarbleMusic.receive(packet.session, packet.playing, packet.secondary);
        }
    }
}
