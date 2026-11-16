package net.kazi.kazimod.leopard;
import java.util.function.Supplier;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.api.distmarker.*;
import net.kazi.kazimod.init.KaziPacketHandler;
public final class LeopardGrabPacket {
    private final int owner,target;
    public LeopardGrabPacket(int owner,int target){this.owner=owner;this.target=target;}
    public static void send(LivingEntity user,int target){if(!user.level.isClientSide)KaziPacketHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->user),new LeopardGrabPacket(user.getId(),target));}
    public static void encode(LeopardGrabPacket p,PacketBuffer b){b.writeInt(p.owner);b.writeInt(p.target);}
    public static LeopardGrabPacket decode(PacketBuffer b){return new LeopardGrabPacket(b.readInt(),b.readInt());}
    public static void handle(LeopardGrabPacket p,Supplier<NetworkEvent.Context> supplier){NetworkEvent.Context ctx=supplier.get();ctx.enqueueWork(()->Client.accept(p));ctx.setPacketHandled(true);}
    @OnlyIn(Dist.CLIENT) private static final class Client {
        static void accept(LeopardGrabPacket p){net.minecraft.client.Minecraft mc=net.minecraft.client.Minecraft.getInstance();if(mc.level==null)return;net.minecraft.entity.Entity e=mc.level.getEntity(p.owner);if(e!=null){e.getPersistentData().putInt("kaziLeopardGrab",p.target);e.getPersistentData().putLong("kaziLeopardGrabUntil",mc.level.getGameTime()+12);}}
    }
}
