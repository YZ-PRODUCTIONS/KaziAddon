package net.kazi.kazimod.network;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

/** Cosmetic metadata only; never changes a projectile's actual damage or collision size. */
public final class BeamVisualPacket {
    public final ResourceLocation dimension;
    public final int entityId;
    public final UUID uuid;
    public final float damage;
    public final boolean laser;

    public BeamVisualPacket(ResourceLocation dimension, int entityId, UUID uuid, float damage, boolean laser) {
        this.dimension = dimension;
        this.entityId = entityId;
        this.uuid = uuid;
        this.damage = Float.isFinite(damage) ? Math.max(0.0F, damage) : 0.0F;
        this.laser = laser;
    }

    public static void encode(BeamVisualPacket packet, PacketBuffer buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeInt(packet.entityId);
        buffer.writeUUID(packet.uuid);
        buffer.writeFloat(packet.damage);
        buffer.writeBoolean(packet.laser);
    }

    public static BeamVisualPacket decode(PacketBuffer buffer) {
        return new BeamVisualPacket(buffer.readResourceLocation(), buffer.readInt(), buffer.readUUID(),
                buffer.readFloat(), buffer.readBoolean());
    }

    public static void handle(BeamVisualPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            context.enqueueWork(() -> ClientHandler.handle(packet));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        static void handle(BeamVisualPacket packet) {
            net.kazi.kazimod.effects.ClientBeamVisuals.receive(packet);
        }
    }
}
