package net.kazi.kazimod.network;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

/** Independent from camera cancellation: start, source position, or caster death. */
public final class GoruCutsceneMusicPacket {
    public final UUID session, caster;
    public final Vector3d position;
    public final int action;
    public GoruCutsceneMusicPacket(UUID session, UUID caster, Vector3d position, int action) {
        this.session = session; this.caster = caster; this.position = position; this.action = action;
    }
    public static void encode(GoruCutsceneMusicPacket p, PacketBuffer b) {
        b.writeUUID(p.session); b.writeUUID(p.caster); b.writeDouble(p.position.x);
        b.writeDouble(p.position.y); b.writeDouble(p.position.z); b.writeByte(p.action);
    }
    public static GoruCutsceneMusicPacket decode(PacketBuffer b) {
        return new GoruCutsceneMusicPacket(b.readUUID(), b.readUUID(),
                new Vector3d(b.readDouble(), b.readDouble(), b.readDouble()), b.readUnsignedByte());
    }
    public static void handle(GoruCutsceneMusicPacket p, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> Client.receive(p));
        context.setPacketHandled(true);
    }
    @OnlyIn(Dist.CLIENT)
    private static final class Client {
        static void receive(GoruCutsceneMusicPacket p) {
            net.kazi.kazimod.client.goru.GoruCutsceneMusicClient.receive(p);
        }
    }
}
