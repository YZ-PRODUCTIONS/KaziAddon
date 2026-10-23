package net.kazi.kazimod.tusk;

import java.util.function.Supplier;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

public final class TuskPacket {
    public final int entity;
    public final CompoundNBT data;
    public TuskPacket(int entity, CompoundNBT data) { this.entity = entity; this.data = data; }
    public static void encode(TuskPacket p, PacketBuffer buffer) { buffer.writeVarInt(p.entity); buffer.writeNbt(p.data); }
    public static TuskPacket decode(PacketBuffer buffer) { return new TuskPacket(buffer.readVarInt(), buffer.readNbt()); }
    public static void handle(TuskPacket p, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TuskClient.receive(p)));
        context.get().setPacketHandled(true);
    }
}
