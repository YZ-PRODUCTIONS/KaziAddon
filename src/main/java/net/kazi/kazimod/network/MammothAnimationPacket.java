package net.kazi.kazimod.network;

import net.kazi.kazimod.init.KaziPacketHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import java.util.function.Supplier;

public final class MammothAnimationPacket {
    private final int entityId, action;
    private final long tick;
    public MammothAnimationPacket(int entityId,int action,long tick) { this.entityId=entityId;this.action=action;this.tick=tick; }
    public static void send(LivingEntity entity,int action) {
        if(!entity.level.isClientSide)KaziPacketHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->entity),
                new MammothAnimationPacket(entity.getId(),action,entity.level.getGameTime()));
    }
    public static void encode(MammothAnimationPacket p,PacketBuffer b) { b.writeVarInt(p.entityId);b.writeByte(p.action);b.writeLong(p.tick); }
    public static MammothAnimationPacket decode(PacketBuffer b) { return new MammothAnimationPacket(b.readVarInt(),b.readUnsignedByte(),b.readLong()); }
    public static void handle(MammothAnimationPacket p,Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx=supplier.get();ctx.enqueueWork(()->Client.accept(p));ctx.setPacketHandled(true);
    }
    @OnlyIn(Dist.CLIENT)
    private static final class Client {
        static void accept(MammothAnimationPacket p) {
            net.minecraft.client.Minecraft mc=net.minecraft.client.Minecraft.getInstance();
            if(mc.level==null||p.action>3)return;
            net.minecraft.entity.Entity e=mc.level.getEntity(p.entityId);
            if(e instanceof LivingEntity)net.kazi.kazimod.client.models.morphs.MammothModel.signal((LivingEntity)e,p.action,p.tick);
        }
    }
}
