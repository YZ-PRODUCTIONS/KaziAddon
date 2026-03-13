package net.kazi.kazimod.network;

import net.kazi.kazimod.client.AfterImageStore;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent server -> client whenever the ability spawns an after-image.
 * Register this in your network channel alongside your other packets.
 *
 * Example registration (in your NetworkHandler / PacketHandler class):
 *
 *   CHANNEL.registerMessage(idx++, AfterImagePacket.class,
 *       AfterImagePacket::encode,
 *       AfterImagePacket::decode,
 *       AfterImagePacket::handle);
 */
public class AfterImagePacket {

    /** Lifetime in ticks for each ghost (0.5 s). */
    public static final int DEFAULT_LIFE = 10;

    private final double x, y, z;
    private final float  yaw, pitch;
    private final String skinName;
    private final int    ticks;

    public AfterImagePacket(double x, double y, double z,
                            float yaw, float pitch,
                            String skinName, int ticks) {
        this.x = x; this.y = y; this.z = z;
        this.yaw = yaw; this.pitch = pitch;
        this.skinName = skinName;
        this.ticks = ticks;
    }

    public static void encode(AfterImagePacket msg, PacketBuffer buf) {
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeFloat(msg.yaw);
        buf.writeFloat(msg.pitch);
        buf.writeUtf(msg.skinName, 64);
        buf.writeInt(msg.ticks);
    }

    public static AfterImagePacket decode(PacketBuffer buf) {
        return new AfterImagePacket(
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readFloat(), buf.readFloat(),
                buf.readUtf(64),
                buf.readInt()
        );
    }

    public static void handle(AfterImagePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() ->
                // DistExecutor ensures this only touches client classes on the client.
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        AfterImageStore.INSTANCE.addSnapshot(new AfterImageStore.Snapshot(
                                msg.x, msg.y, msg.z,
                                msg.yaw, msg.pitch,
                                msg.skinName,
                                msg.ticks
                        ))
                )
        );
        ctx.setPacketHandled(true);
    }
}