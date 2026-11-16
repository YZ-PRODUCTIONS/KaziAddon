package net.kazi.kazimod.ope;

import java.util.*;
import java.util.function.Supplier;
import net.kazi.kazimod.init.KaziPacketHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.*;
import net.minecraftforge.fml.network.*;

public final class OpeVisualPacket {
    public static final int CURTAIN = 0, RADIO = 1, SPLIT = 2;
    private static final Map<Entity, long[]> SENT = new WeakHashMap<>();
    final ResourceLocation dimension;
    final int id, kind, life;
    final UUID uuid;
    public OpeVisualPacket(ResourceLocation dimension, int id, UUID uuid, int kind, int life) {
        this.dimension = dimension; this.id = id; this.uuid = uuid; this.kind = kind; this.life = life;
    }
    private static OpeVisualPacket of(Entity entity, int kind, int life) {
        return new OpeVisualPacket(entity.level.dimension().location(), entity.getId(), entity.getUUID(), kind, life);
    }
    public static void refresh(Entity entity, int kind) {
        if (entity == null || entity.level.isClientSide) return;
        long now = entity.level.getGameTime();
        long[] last = SENT.computeIfAbsent(entity, e -> new long[]{Long.MIN_VALUE, Long.MIN_VALUE});
        if (last[kind] != Long.MIN_VALUE && now - last[kind] >= 0 && now - last[kind] < 4) return;
        last[kind] = now;
        send(entity, kind, 10);
    }
    public static void send(Entity entity, int kind, int life) {
        if (entity.level.isClientSide) return;
        KaziPacketHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), of(entity, kind, life));
    }
    public static void sendTo(ServerPlayerEntity player, Entity entity, int kind, int life) {
        KaziPacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), of(entity, kind, life));
    }
    public static void encode(OpeVisualPacket p, PacketBuffer b) {
        b.writeResourceLocation(p.dimension); b.writeInt(p.id); b.writeUUID(p.uuid); b.writeByte(p.kind); b.writeVarInt(p.life);
    }
    public static OpeVisualPacket decode(PacketBuffer b) {
        return new OpeVisualPacket(b.readResourceLocation(), b.readInt(), b.readUUID(), b.readUnsignedByte(), b.readVarInt());
    }
    public static void handle(OpeVisualPacket p, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        if (ctx.getDirection() == NetworkDirection.PLAY_TO_CLIENT) ctx.enqueueWork(() -> Client.receive(p));
        ctx.setPacketHandled(true);
    }
    @OnlyIn(Dist.CLIENT)
    private static final class Client {
        static void receive(OpeVisualPacket p) {
            World world = net.minecraft.client.Minecraft.getInstance().level;
            if (world != null && world.dimension().location().equals(p.dimension)) OpeEffects.receive(world, p);
        }
    }
}
