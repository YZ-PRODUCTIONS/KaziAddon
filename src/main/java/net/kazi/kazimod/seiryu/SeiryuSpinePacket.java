package net.kazi.kazimod.seiryu;

import java.util.function.Supplier;
import net.kazi.kazimod.init.KaziPacketHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.*;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.fml.network.*;

public final class SeiryuSpinePacket {
    public final int owner;
    public final double scale;
    public final Vector3d head;
    public final Vector3d[] points;
    public SeiryuSpinePacket(int owner,Vector3d head,Vector3d[] points){this(owner,head,points,1);}
    public SeiryuSpinePacket(int owner,Vector3d head,Vector3d[] points,double scale){this.owner=owner;this.head=head;this.points=points.clone();this.scale=scale;}
    public static void send(LivingEntity owner,Vector3d[] points) {
        KaziPacketHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->owner),new SeiryuSpinePacket(owner.getId(),owner.position(),points,SeiryuFlight.scale(owner)));
    }
    public static void sendTo(PlayerEntity player,LivingEntity owner,Vector3d[] points) {
        if(player instanceof ServerPlayerEntity)KaziPacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(()->(ServerPlayerEntity)player),new SeiryuSpinePacket(owner.getId(),owner.position(),points,SeiryuFlight.scale(owner)));
    }
    public static void encode(SeiryuSpinePacket p,PacketBuffer b) {
        b.writeVarInt(p.owner);
        b.writeFloat((float)p.scale);
        b.writeDouble(p.head.x);b.writeDouble(p.head.y);b.writeDouble(p.head.z);
        Vector3d origin=p.points[0];b.writeDouble(origin.x);b.writeDouble(origin.y);b.writeDouble(origin.z);
        for(int i=1;i<p.points.length;i++) {
            Vector3d v=p.points[i].subtract(origin);b.writeFloat((float)v.x);b.writeFloat((float)v.y);b.writeFloat((float)v.z);
        }
    }
    public static SeiryuSpinePacket decode(PacketBuffer b) {
        int owner=b.readVarInt();double scale=b.readFloat();Vector3d head=new Vector3d(b.readDouble(),b.readDouble(),b.readDouble());
        Vector3d[] points=new Vector3d[SeiryuSpine.SEGMENTS+1];
        points[0]=new Vector3d(b.readDouble(),b.readDouble(),b.readDouble());
        for(int i=1;i<points.length;i++)points[i]=points[0].add(b.readFloat(),b.readFloat(),b.readFloat());
        return new SeiryuSpinePacket(owner,head,points,scale);
    }
    public static void handle(SeiryuSpinePacket p,Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context=supplier.get();
        context.enqueueWork(()->SeiryuClient.accept(p));context.setPacketHandled(true);
    }
}
