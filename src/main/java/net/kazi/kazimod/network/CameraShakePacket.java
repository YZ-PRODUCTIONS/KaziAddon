package net.kazi.kazimod.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class CameraShakePacket {
    private final int durationTicks;
    private final float intensity;

    public CameraShakePacket(int durationTicks, float intensity) {
        this.durationTicks = durationTicks;
        this.intensity = intensity;
    }

    public static void encode(CameraShakePacket msg, PacketBuffer buf) {
        buf.writeInt(msg.durationTicks);
        buf.writeFloat(msg.intensity);
    }

    public static CameraShakePacket decode(PacketBuffer buf) {
        return new CameraShakePacket(buf.readInt(), buf.readFloat());
    }

    public static void handle(CameraShakePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> ClientHandler.handle(msg));
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void handle(CameraShakePacket msg) {
            net.kazi.kazimod.client.CameraShakeHandler.shake(msg.durationTicks, msg.intensity);
        }
    }
}
