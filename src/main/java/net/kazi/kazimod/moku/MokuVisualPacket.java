package net.kazi.kazimod.moku;

import java.util.*;
import java.util.function.Supplier;
import net.kazi.kazimod.init.KaziPacketHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.*;
import net.minecraftforge.fml.network.*;

/** Smoke visuals travel independently of Minecraft's particle quality settings. */
public final class MokuVisualPacket {
    private static final Map<World,Budget> BUDGETS=new WeakHashMap<>();
    private static final class Budget {long tick=Long.MIN_VALUE;int count;final Map<Long,Long> recent=new HashMap<>();}
    final ResourceLocation dimension;final int owner,kind;final Vector3d position;final float radius;
    public MokuVisualPacket(ResourceLocation dimension,int owner,int kind,Vector3d position,float radius){
        this.dimension=dimension;this.owner=owner;this.kind=kind;this.position=position;this.radius=radius;
    }
    public static void send(Entity owner,int kind,Vector3d pos,float radius) {
        if(owner.level.isClientSide||!allowed(owner.level,owner.getId(),kind))return;
        KaziPacketHandler.CHANNEL.send(PacketDistributor.NEAR.with(()->new PacketDistributor.TargetPoint(
                pos.x,pos.y,pos.z,160,owner.level.dimension())),
                new MokuVisualPacket(owner.level.dimension().location(),owner.getId(),kind,pos,radius));
    }
    static boolean allowed(World world,int owner,int kind) {
        Budget b=BUDGETS.computeIfAbsent(world,w->new Budget());long now=world.getGameTime();
        if(b.tick!=now){b.tick=now;b.count=0;}
        if(b.count>=32)return false;
        long key=((long)owner<<32)|(kind&0xffffffffL);Long last=b.recent.get(key);
        if(last!=null&&now-last>=0&&now-last<4)return false;
        if(b.recent.size()>=512)b.recent.clear();b.recent.put(key,now);b.count++;return true;
    }
    public static void encode(MokuVisualPacket p,PacketBuffer b){
        b.writeResourceLocation(p.dimension);b.writeInt(p.owner);b.writeByte(p.kind);
        b.writeDouble(p.position.x);b.writeDouble(p.position.y);b.writeDouble(p.position.z);b.writeFloat(p.radius);
    }
    public static MokuVisualPacket decode(PacketBuffer b){return new MokuVisualPacket(b.readResourceLocation(),b.readInt(),b.readUnsignedByte(),
            new Vector3d(b.readDouble(),b.readDouble(),b.readDouble()),b.readFloat());}
    public static void handle(MokuVisualPacket p,Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx=supplier.get();
        if(ctx.getDirection()==NetworkDirection.PLAY_TO_CLIENT)ctx.enqueueWork(()->Client.receive(p));ctx.setPacketHandled(true);
    }
    @OnlyIn(Dist.CLIENT)
    private static final class Client {
        static void receive(MokuVisualPacket p) {
            net.minecraft.client.world.ClientWorld w=net.minecraft.client.Minecraft.getInstance().level;
            if(w!=null&&w.dimension().location().equals(p.dimension))MokuEffects.emit(w,p.owner,p.kind,p.position,p.radius);
        }
    }
}
