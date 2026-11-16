package net.kazi.kazimod.ghoul;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
public final class GhoulPacket {
    public final int entity; public final CompoundNBT data;
    public GhoulPacket(int entity, CompoundNBT data) { this.entity=entity; this.data=data; }
    public static void encode(GhoulPacket p, PacketBuffer b) { b.writeVarInt(p.entity); b.writeNbt(p.data); }
    public static GhoulPacket decode(PacketBuffer b) { return new GhoulPacket(b.readVarInt(),b.readNbt()); }
    public static void handle(GhoulPacket p,Supplier<NetworkEvent.Context> c) {
        c.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->GhoulClient.receive(p))); c.get().setPacketHandled(true);
    }
}
