package net.kazi.kazimod.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class FlashbangPacket {

    public FlashbangPacket() {}

    public FlashbangPacket(PacketBuffer buf) {}

    public void encode(PacketBuffer buf) {}

    public static void handle(FlashbangPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(ClientHandler::handle);
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void handle() {
            net.kazi.kazimod.effects.FlashbangEffect.triggerFlash();
        }
    }
}
