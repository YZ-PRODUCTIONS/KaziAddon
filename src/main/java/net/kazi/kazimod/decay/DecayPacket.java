package net.kazi.kazimod.decay;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;
public final class DecayPacket {
 public final int id;public final CompoundNBT data;
 public DecayPacket(int id,CompoundNBT data){this.id=id;this.data=data;}
 public static void encode(DecayPacket p,PacketBuffer b){b.writeVarInt(p.id);b.writeNbt(p.data);}
 public static DecayPacket decode(PacketBuffer b){return new DecayPacket(b.readVarInt(),b.readNbt());}
 public static void handle(DecayPacket p,Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->DecayClient.receive(p)));c.get().setPacketHandled(true);}
}
