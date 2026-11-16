package net.kazi.kazimod.mochi;

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

public final class MochiVisualPacket {
    private static final Map<World,Budget> BUDGETS=new WeakHashMap<>();
    private static final class Budget {long tick=Long.MIN_VALUE;int count;final Map<Long,Long> recent=new HashMap<>();}
    final ResourceLocation dimension;final int owner,kind;final Vector3d position;final float radius;
    public MochiVisualPacket(ResourceLocation dimension,int owner,int kind,Vector3d position,float radius){
        this.dimension=dimension;this.owner=owner;this.kind=kind;this.position=position;this.radius=radius;
    }
    public static void send(Entity owner,int kind,Vector3d pos,float radius) {
        if(owner.level.isClientSide||!allowed(owner.level,owner.getId(),kind,pos))return;
        KaziPacketHandler.CHANNEL.send(PacketDistributor.NEAR.with(()->new PacketDistributor.TargetPoint(pos.x,pos.y,pos.z,128,owner.level.dimension())),
                new MochiVisualPacket(owner.level.dimension().location(),owner.getId(),kind,pos,radius));
    }
    static boolean allowed(World world,int owner,int kind,Vector3d position) {
        Budget b=BUDGETS.computeIfAbsent(world,w->new Budget());long now=world.getGameTime();
        if(b.tick!=now){b.tick=now;b.count=0;}
        if(b.count>=32)return false;
        // Ground effects retain distinct locations; only duplicates in the same cell coalesce.
        long cell=MochiVisuals.follows(kind)?0:net.minecraft.util.math.BlockPos.asLong((int)Math.floor(position.x/2),
                (int)Math.floor(position.y/2),(int)Math.floor(position.z/2));
        long key=cell^((long)owner*0x9e3779b97f4a7c15L)^(kind*7919L);Long last=b.recent.get(key);
        if(last!=null&&now-last>=0&&now-last<4)return false;
        if(b.recent.size()>=1024)b.recent.clear();b.recent.put(key,now);b.count++;return true;
    }
    public static void encode(MochiVisualPacket p,PacketBuffer b){b.writeResourceLocation(p.dimension);b.writeInt(p.owner);b.writeByte(p.kind);
        b.writeDouble(p.position.x);b.writeDouble(p.position.y);b.writeDouble(p.position.z);b.writeFloat(p.radius);}
    public static MochiVisualPacket decode(PacketBuffer b){return new MochiVisualPacket(b.readResourceLocation(),b.readInt(),b.readUnsignedByte(),
            new Vector3d(b.readDouble(),b.readDouble(),b.readDouble()),b.readFloat());}
    public static void handle(MochiVisualPacket p,Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx=supplier.get();if(ctx.getDirection()==NetworkDirection.PLAY_TO_CLIENT)ctx.enqueueWork(()->Client.receive(p));ctx.setPacketHandled(true);
    }
    @OnlyIn(Dist.CLIENT) private static final class Client {
        static void receive(MochiVisualPacket p) {
            net.minecraft.client.world.ClientWorld world=net.minecraft.client.Minecraft.getInstance().level;
            if(world!=null&&world.dimension().location().equals(p.dimension))MochiEffects.emit(world,p.owner,p.kind,p.position,p.radius);
        }
    }
}
