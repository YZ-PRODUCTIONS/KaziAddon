package net.kazi.kazimod.pika;

import java.util.*;
import java.util.function.Supplier;
import net.kazi.kazimod.init.KaziPacketHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.*;
import net.minecraftforge.fml.network.*;

public final class PikaVisualPacket {
    private static final Map<World,Budget> BUDGETS=new WeakHashMap<>();
    private static final class Budget {
        long tick=Long.MIN_VALUE; int count;
        final Map<String,Long> recent=new HashMap<>();
    }
    final ResourceLocation dimension;
    final int owner,kind;
    final Vector3d position;
    final float radius;
    public PikaVisualPacket(ResourceLocation dimension,int owner,int kind,Vector3d position,float radius) {
        this.dimension=dimension;this.owner=owner;this.kind=kind;this.position=position;this.radius=radius;
    }
    public static void send(Entity owner,int kind,Vector3d position) {
        send(owner.level,owner.getId(),kind,position,1);
    }
    public static void send(World world,int owner,int kind,Vector3d position,float radius) {
        if(world.isClientSide||!allowed(world,owner,kind,position))return;
        KaziPacketHandler.CHANNEL.send(PacketDistributor.NEAR.with(()->new PacketDistributor.TargetPoint(
                position.x,position.y,position.z,160,world.dimension())),
                new PikaVisualPacket(world.dimension().location(),owner,kind,position,Math.max(.1F,Math.min(24,radius))));
    }
    static boolean allowed(World world,int owner,int kind,Vector3d pos) {
        Budget b=BUDGETS.computeIfAbsent(world,w->new Budget());long now=world.getGameTime();
        if(b.tick!=now){b.tick=now;b.count=0;}
        if(b.count>=48)return false;
        boolean follows=kind==PikaVisuals.CHARGE||kind==PikaVisuals.LOGIA||kind==PikaVisuals.CUT_CHARGE;
        BlockPos cell=new BlockPos(pos.x/3,pos.y/3,pos.z/3);
        String key=kind+":"+(follows?Integer.toString(owner):cell.asLong());
        Long last=b.recent.get(key);
        if(last!=null&&now-last>=0&&now-last<(follows?4:2))return false;
        if(b.recent.size()>=512)b.recent.clear();
        b.recent.put(key,now);b.count++;return true;
    }
    public static void encode(PikaVisualPacket p,PacketBuffer b) {
        b.writeResourceLocation(p.dimension);b.writeInt(p.owner);b.writeByte(p.kind);
        b.writeDouble(p.position.x);b.writeDouble(p.position.y);b.writeDouble(p.position.z);b.writeFloat(p.radius);
    }
    public static PikaVisualPacket decode(PacketBuffer b) {
        return new PikaVisualPacket(b.readResourceLocation(),b.readInt(),b.readUnsignedByte(),
                new Vector3d(b.readDouble(),b.readDouble(),b.readDouble()),b.readFloat());
    }
    public static void handle(PikaVisualPacket p,Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx=supplier.get();
        if(ctx.getDirection()==NetworkDirection.PLAY_TO_CLIENT)ctx.enqueueWork(()->Client.receive(p));
        ctx.setPacketHandled(true);
    }
    @OnlyIn(Dist.CLIENT)
    private static final class Client {
        static void receive(PikaVisualPacket p) {
            net.minecraft.client.world.ClientWorld w=net.minecraft.client.Minecraft.getInstance().level;
            if(w!=null&&w.dimension().location().equals(p.dimension))PikaEffects.emit(w,p.owner,p.kind,p.position,p.radius);
        }
    }
}
