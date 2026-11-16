package net.kazi.kazimod.yami;

import java.util.function.Supplier;
import java.util.Map;
import java.util.HashMap;
import java.util.WeakHashMap;
import net.kazi.kazimod.init.KaziPacketHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.*;
import net.minecraftforge.fml.network.*;

/** Darkness is an ability visual, independent of particle quality and source-entity lifetime. */
public final class YamiVisualPacket {
    private static final Map<net.minecraft.world.World,PoolBudget> POOL_BUDGETS=new WeakHashMap<>();
    private static final Map<Entity,DebrisBudget> DEBRIS_BUDGETS=new WeakHashMap<>();
    private static final class PoolBudget {
        final Map<net.minecraft.util.math.BlockPos,Long> cells=new HashMap<>();
        long tick=Long.MIN_VALUE;int count;
    }
    private static final class DebrisBudget {long tick=Long.MIN_VALUE;int count;}
    private final ResourceLocation dimension;
    private final int owner,kind;
    private final Vector3d position;
    private Vector3d destination;
    private int blockState;
    public YamiVisualPacket(ResourceLocation dimension,int owner,int kind,Vector3d position) {
        this.dimension=dimension;this.owner=owner;this.kind=kind;this.position=position;
    }
    public static void send(Entity owner,int kind,Vector3d position) {
        if(owner.level.isClientSide)return;
        if(kind==0&&!poolAllowed(owner,position))return;
        send(owner,new YamiVisualPacket(owner.level.dimension().location(),owner.getId(),kind,position));
    }
    private static boolean poolAllowed(Entity owner,Vector3d position) {
        PoolBudget budget=POOL_BUDGETS.computeIfAbsent(owner.level,w->new PoolBudget());long now=owner.level.getGameTime();
        if(budget.tick!=now){budget.tick=now;budget.count=0;}
        if(budget.count>=32)return false;
        net.minecraft.util.math.BlockPos cell=new net.minecraft.util.math.BlockPos(position.x/4,position.y/2,position.z/4);
        Long last=budget.cells.get(cell);if(last!=null&&now-last>=0&&now-last<4)return false;
        if(budget.cells.size()>=512)budget.cells.clear();
        budget.cells.put(cell,now);budget.count++;return true;
    }
    public static void debris(Entity owner,Vector3d start,Vector3d end,int state) {
        if(owner.level.isClientSide)return;
        DebrisBudget budget=DEBRIS_BUDGETS.computeIfAbsent(owner,e->new DebrisBudget());long now=owner.level.getGameTime();
        if(budget.tick!=now){budget.tick=now;budget.count=0;}
        if(budget.count++>=2)return;
        YamiVisualPacket packet=new YamiVisualPacket(owner.level.dimension().location(),owner.getId(),9,start);
        packet.destination=end;packet.blockState=state;send(owner,packet);
    }
    private static void send(Entity owner,YamiVisualPacket packet) {
        Vector3d position=packet.position;
        KaziPacketHandler.CHANNEL.send(PacketDistributor.NEAR.with(()->new PacketDistributor.TargetPoint(
                position.x,position.y,position.z,160,owner.level.dimension())),
                packet);
    }
    public static void encode(YamiVisualPacket p,PacketBuffer b) {
        b.writeResourceLocation(p.dimension);b.writeInt(p.owner);b.writeInt(p.kind);
        b.writeDouble(p.position.x);b.writeDouble(p.position.y);b.writeDouble(p.position.z);
        if(p.kind==9){b.writeDouble(p.destination.x);b.writeDouble(p.destination.y);b.writeDouble(p.destination.z);b.writeVarInt(p.blockState);}
    }
    public static YamiVisualPacket decode(PacketBuffer b) {
        YamiVisualPacket p=new YamiVisualPacket(b.readResourceLocation(),b.readInt(),b.readInt(),new Vector3d(b.readDouble(),b.readDouble(),b.readDouble()));
        if(p.kind==9){p.destination=new Vector3d(b.readDouble(),b.readDouble(),b.readDouble());p.blockState=b.readVarInt();}
        return p;
    }
    public static void handle(YamiVisualPacket p,Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context=supplier.get();
        if(context.getDirection()==NetworkDirection.PLAY_TO_CLIENT)context.enqueueWork(()->Client.receive(p));
        context.setPacketHandled(true);
    }
    @OnlyIn(Dist.CLIENT)
    private static final class Client {
        static void receive(YamiVisualPacket p) {
            net.minecraft.client.world.ClientWorld world=net.minecraft.client.Minecraft.getInstance().level;
            if(world==null||!world.dimension().location().equals(p.dimension))return;
            if(p.kind==-1)YamiEffects.stopCoat(world,p.owner);
            else if(p.kind==-2)YamiEffects.stop(world,p.owner,YamiEffects.PORTAL);
            else if(p.kind==-3)YamiEffects.stop(world,p.owner,YamiEffects.TORNADO);
            else if(p.kind==9)YamiEffects.debris(world,p.position,p.destination,p.blockState);
            else if(p.kind>=0&&p.kind<=YamiEffects.TORNADO)YamiEffects.emit(world,world.getEntity(p.owner),p.kind,p.position.x,p.position.y,p.position.z);
        }
    }
}
