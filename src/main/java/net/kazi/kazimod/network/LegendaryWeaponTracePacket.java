package net.kazi.kazimod.network;

import java.util.function.Supplier;
import net.kazi.kazimod.abilities.SupaRework.LegendaryWeaponTrace;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

/** Keeps borrowed-move prerequisites and cancellation in step with the server. */
public final class LegendaryWeaponTracePacket {
    private final int entityId;
    private final CompoundNBT state;

    public LegendaryWeaponTracePacket(int entityId, CompoundNBT state) {
        this.entityId = entityId;
        this.state = state == null ? new CompoundNBT() : state.copy();
    }

    public static void encode(LegendaryWeaponTracePacket packet, PacketBuffer buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeNbt(packet.state);
    }

    public static LegendaryWeaponTracePacket decode(PacketBuffer buffer) {
        return new LegendaryWeaponTracePacket(buffer.readVarInt(), buffer.readNbt());
    }

    public static void handle(LegendaryWeaponTracePacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            context.enqueueWork(() -> ClientHandler.accept(packet));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        static void accept(LegendaryWeaponTracePacket packet) {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.level == null) return;
            Entity entity = minecraft.level.getEntity(packet.entityId);
            if (!(entity instanceof LivingEntity)) return;
            if (packet.state.isEmpty()) entity.getPersistentData().remove(LegendaryWeaponTrace.TAG);
            else entity.getPersistentData().put(LegendaryWeaponTrace.TAG, packet.state.copy());
        }
    }
}
