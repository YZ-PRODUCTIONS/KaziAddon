package net.kazi.kazimod.saiyan;

import java.util.function.Supplier;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

public final class SaiyanPacket {
    final int entity;
    final CompoundNBT data;
    public SaiyanPacket(int entity, CompoundNBT data) { this.entity = entity; this.data = data; }
    public static void encode(SaiyanPacket p, PacketBuffer buf) { buf.writeVarInt(p.entity); buf.writeNbt(p.data); }
    public static SaiyanPacket decode(PacketBuffer buf) { return new SaiyanPacket(buf.readVarInt(), buf.readNbt()); }
    public static void handle(SaiyanPacket p, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SaiyanClient.receive(p)));
        context.get().setPacketHandled(true);
    }
}
