package net.kazi.kazimod.network;

import java.util.function.Supplier;
import java.util.UUID;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

/** Global cast lifecycle; each client blends simultaneous casts by their strongest tint. */
public final class EaCrimsonOverlayPacket {
    public enum Action { CHARGE, RELEASE, CANCEL }
    public final UUID session;
    public final Action action;
    public final boolean winds;

    public EaCrimsonOverlayPacket(UUID session,Action action,boolean winds) {
        this.session=session;
        this.action=action;
        this.winds=winds;
    }

    public static void encode(EaCrimsonOverlayPacket packet,PacketBuffer buffer) {
        buffer.writeUUID(packet.session);
        buffer.writeEnum(packet.action);
        buffer.writeBoolean(packet.winds);
    }
    public static EaCrimsonOverlayPacket decode(PacketBuffer buffer) {
        return new EaCrimsonOverlayPacket(buffer.readUUID(),buffer.readEnum(Action.class),buffer.readBoolean());
    }

    public static void handle(EaCrimsonOverlayPacket packet,Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context=supplier.get();
        if(context.getDirection()==NetworkDirection.PLAY_TO_CLIENT)context.enqueueWork(() -> ClientHandler.receive(packet));
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        static void receive(EaCrimsonOverlayPacket packet) {net.kazi.kazimod.client.goru.EaCrimsonOverlay.receive(packet);}
    }
}
