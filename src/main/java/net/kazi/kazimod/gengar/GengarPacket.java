package net.kazi.kazimod.gengar;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
public final class GengarPacket {
    public final int entity; public final CompoundNBT data;
    public GengarPacket(int entity,CompoundNBT data){this.entity=entity;this.data=data;}
    public static void encode(GengarPacket p,PacketBuffer b){b.writeVarInt(p.entity);b.writeNbt(p.data);}
    public static GengarPacket decode(PacketBuffer b){return new GengarPacket(b.readVarInt(),b.readNbt());}
    public static void handle(GengarPacket p,Supplier<NetworkEvent.Context> c){
        c.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->GengarClient.receive(p)));c.get().setPacketHandled(true);
    }
}
