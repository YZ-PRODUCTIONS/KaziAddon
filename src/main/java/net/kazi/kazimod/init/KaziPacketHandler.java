package net.kazi.kazimod.init;

import net.kazi.kazimod.network.AfterImagePacket;
import net.kazi.kazimod.network.FlashbangPacket;
import net.kazi.kazimod.network.SyncEntitySizePacket;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.Optional;

public class KaziPacketHandler {

    private static final String PROTOCOL_VERSION = "1";
    private static int id = 0;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("kazimod", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        CHANNEL.registerMessage(
                id++,
                SyncEntitySizePacket.class,
                SyncEntitySizePacket::encode,
                SyncEntitySizePacket::decode,
                SyncEntitySizePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                id++,
                AfterImagePacket.class,
                AfterImagePacket::encode,
                AfterImagePacket::decode,
                AfterImagePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                id++,
                FlashbangPacket.class,
                FlashbangPacket::encode,
                FlashbangPacket::new,
                FlashbangPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    public static void syncEntitySize(Entity entity) {
        CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                new SyncEntitySizePacket(entity.getId())
        );
    }

    public static void sendAfterImage(Entity entity, AfterImagePacket packet) {
        CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                packet
        );
    }

    public static void sendFlashbang(net.minecraft.entity.player.ServerPlayerEntity player) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new FlashbangPacket()
        );
    }
}