package net.kazi.kazimod.yami;

import java.util.function.Supplier;
import net.kazi.kazimod.init.KaziPacketHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

public final class DarknessLaunchPacket {
    final ResourceLocation dimension;
    final int entityId;

    public DarknessLaunchPacket(ResourceLocation dimension, int entityId) {
        this.dimension = dimension;
        this.entityId = entityId;
    }

    static void send(LivingEntity entity) {
        KaziPacketHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                new DarknessLaunchPacket(entity.level.dimension().location(), entity.getId()));
    }

    public static void encode(DarknessLaunchPacket packet, PacketBuffer buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeInt(packet.entityId);
    }

    public static DarknessLaunchPacket decode(PacketBuffer buffer) {
        return new DarknessLaunchPacket(buffer.readResourceLocation(), buffer.readInt());
    }

    public static void handle(DarknessLaunchPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) context.enqueueWork(() -> Client.receive(packet));
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class Client {
        static void receive(DarknessLaunchPacket packet) {
            net.minecraft.client.world.ClientWorld world = net.minecraft.client.Minecraft.getInstance().level;
            if (world == null || !world.dimension().location().equals(packet.dimension)) return;
            Entity entity = world.getEntity(packet.entityId);
            if (entity instanceof LivingEntity) DarknessLaunch.beginRelease((LivingEntity) entity);
        }
    }
}
