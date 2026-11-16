package net.kazi.kazimod.ope;

import java.util.UUID;
import java.util.function.Supplier;
import net.kazi.kazimod.init.KaziPacketHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.*;
import net.minecraftforge.fml.network.*;

public final class WillePacket {
    final ResourceLocation dimension;
    final int id, mode, after;
    final UUID uuid;
    final float progress;
    final Vector3d direction;
    WillePacket(ResourceLocation dimension, int id, UUID uuid, int mode, float progress, int after, Vector3d direction) {
        this.dimension = dimension; this.id = id; this.uuid = uuid; this.mode = mode;
        this.progress = progress; this.after = after; this.direction = direction;
    }
    static void send(LivingEntity user, OpeWille.Cast cast, ServerPlayerEntity recipient) {
        WillePacket p = new WillePacket(user.level.dimension().location(), user.getId(), user.getUUID(),
                cast == null ? 0 : cast.puncture ? 2 : 1, cast == null ? 0 : cast.progress,
                cast == null || !cast.discharged ? -1 : (int)(user.level.getGameTime() - cast.fired),
                cast == null ? Vector3d.ZERO : cast.direction);
        KaziPacketHandler.CHANNEL.send(recipient == null ? PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> user)
                : PacketDistributor.PLAYER.with(() -> recipient), p);
    }
    public static void encode(WillePacket p, PacketBuffer b) {
        b.writeResourceLocation(p.dimension); b.writeInt(p.id); b.writeUUID(p.uuid); b.writeByte(p.mode);
        b.writeFloat(p.progress); b.writeInt(p.after); b.writeDouble(p.direction.x); b.writeDouble(p.direction.y); b.writeDouble(p.direction.z);
    }
    public static WillePacket decode(PacketBuffer b) {
        return new WillePacket(b.readResourceLocation(), b.readInt(), b.readUUID(), b.readUnsignedByte(), b.readFloat(), b.readInt(),
                new Vector3d(b.readDouble(), b.readDouble(), b.readDouble()));
    }
    public static void handle(WillePacket p, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) context.enqueueWork(() -> Client.receive(p));
        context.setPacketHandled(true);
    }
    @OnlyIn(Dist.CLIENT) private static final class Client {
        static void receive(WillePacket p) { WilleClient.receive(p); }
    }
}
