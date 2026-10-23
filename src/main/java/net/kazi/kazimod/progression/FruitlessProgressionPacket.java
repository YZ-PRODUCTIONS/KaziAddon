package net.kazi.kazimod.progression;

import java.util.function.Supplier;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

public final class FruitlessProgressionPacket {
    private final int playerId;
    private final boolean unlocked;

    public FruitlessProgressionPacket(int playerId, boolean unlocked) {
        this.playerId = playerId;
        this.unlocked = unlocked;
    }

    public static void encode(FruitlessProgressionPacket packet, PacketBuffer buffer) {
        buffer.writeVarInt(packet.playerId);
        buffer.writeBoolean(packet.unlocked);
    }

    public static FruitlessProgressionPacket decode(PacketBuffer buffer) {
        return new FruitlessProgressionPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(FruitlessProgressionPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> Client.accept(packet));
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class Client {
        static void accept(FruitlessProgressionPacket packet) {
            net.minecraft.client.entity.player.ClientPlayerEntity player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null && player.getId() == packet.playerId) {
                player.getPersistentData().putBoolean(FruitlessProgression.UNLOCK_TAG, packet.unlocked);
            }
        }
    }
}
