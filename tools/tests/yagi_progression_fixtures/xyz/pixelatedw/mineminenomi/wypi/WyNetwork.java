package xyz.pixelatedw.mineminenomi.wypi;
import net.minecraft.entity.player.PlayerEntity;
import xyz.pixelatedw.mineminenomi.packets.server.SSyncAbilityDataPacket;
public final class WyNetwork {
    public static void sendTo(SSyncAbilityDataPacket packet, PlayerEntity user) {
        user.syncPackets++; user.lastPacket = packet;
    }
}
