package net.kazi.kazimod.petal;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;
public final class PetalPacket {
 public final int id;public final CompoundNBT data;
 public PetalPacket(int id,CompoundNBT data){this.id=id;this.data=data;}
 public static void encode(PetalPacket p,PacketBuffer b){b.writeVarInt(p.id);b.writeNbt(p.data);}
 public static PetalPacket decode(PacketBuffer b){return new PetalPacket(b.readVarInt(),b.readNbt());}
 public static void handle(PetalPacket p,Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->PetalClient.receive(p)));c.get().setPacketHandled(true);}
}
