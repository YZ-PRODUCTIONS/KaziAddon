package net.kazi.kazimod.network;

import net.kazi.kazimod.effects.FlashbangEffect;
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
        ctx.get().enqueueWork(() -> handleClient());
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient() {
        FlashbangEffect.triggerFlash();
    }
}