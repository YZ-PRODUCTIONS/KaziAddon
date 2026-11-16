package net.kazi.kazimod.network;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

/** One server-authored title, timed independently of whether the caster is entity-tracked. */
public final class ChaosBeamTitlePacket {
    public static final int MAX_TEXT_LENGTH = 128;
    public final UUID session;
    public final ResourceLocation dimension;
    public final long startTick;
    public final String text;

    public ChaosBeamTitlePacket(UUID session, ResourceLocation dimension, long startTick, String text) {
        this.session = Objects.requireNonNull(session, "session");
        this.dimension = Objects.requireNonNull(dimension, "dimension");
        this.text = Objects.requireNonNull(text, "text");
        if (text.isEmpty() || text.length() > MAX_TEXT_LENGTH)
            throw new IllegalArgumentException("Invalid Chaos Beam title length");
        this.startTick = startTick;
    }

    public static void encode(ChaosBeamTitlePacket packet, PacketBuffer buffer) {
        buffer.writeUUID(packet.session);
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeLong(packet.startTick);
        buffer.writeUtf(packet.text, MAX_TEXT_LENGTH);
    }

    public static ChaosBeamTitlePacket decode(PacketBuffer buffer) {
        return new ChaosBeamTitlePacket(buffer.readUUID(), buffer.readResourceLocation(), buffer.readLong(),
                buffer.readUtf(MAX_TEXT_LENGTH));
    }

    public static void handle(ChaosBeamTitlePacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT)
            context.enqueueWork(() -> ClientHandler.receive(packet));
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void receive(ChaosBeamTitlePacket packet) {
            net.kazi.kazimod.client.asriel.ChaosBeamTitleOverlay.receive(packet);
        }
    }
}
