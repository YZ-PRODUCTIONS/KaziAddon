package net.kazi.kazimod.alchemy;

import java.util.function.Supplier;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

public final class AlchemyPacket {
    public final int id;
    public final CompoundNBT data;

    public AlchemyPacket(int id, CompoundNBT data) {
        this.id = id;
        this.data = data == null ? new CompoundNBT() : data.copy();
    }

    public static void encode(AlchemyPacket packet, PacketBuffer buffer) {
        buffer.writeVarInt(packet.id);
        buffer.writeNbt(packet.data);
    }

    public static AlchemyPacket decode(PacketBuffer buffer) {
        return new AlchemyPacket(buffer.readVarInt(), buffer.readNbt());
    }

    public static void handle(AlchemyPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> AlchemyClient.receive(packet)));
        }
        context.setPacketHandled(true);
    }
}
