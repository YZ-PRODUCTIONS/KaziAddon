package net.kazi.kazimod.init;

import net.kazi.kazimod.network.AfterImagePacket;
import net.kazi.kazimod.network.CameraShakePacket;
import net.kazi.kazimod.network.FlashbangPacket;
import net.kazi.kazimod.network.SyncEntitySizePacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.Optional;

public class KaziPacketHandler {

    // Mammoth attack animation synchronization requires matching client/server packets.
    private static final String PROTOCOL_VERSION = "7";
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
        CHANNEL.registerMessage(
                id++,
                CameraShakePacket.class,
                CameraShakePacket::encode,
                CameraShakePacket::decode,
                CameraShakePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                id++,
                net.kazi.kazimod.network.BeamVisualPacket.class,
                net.kazi.kazimod.network.BeamVisualPacket::encode,
                net.kazi.kazimod.network.BeamVisualPacket::decode,
                net.kazi.kazimod.network.BeamVisualPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                id++,
                net.kazi.kazimod.network.RealityMarbleMusicPacket.class,
                net.kazi.kazimod.network.RealityMarbleMusicPacket::encode,
                net.kazi.kazimod.network.RealityMarbleMusicPacket::decode,
                net.kazi.kazimod.network.RealityMarbleMusicPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(id++, net.kazi.kazimod.network.MammothAnimationPacket.class,
                net.kazi.kazimod.network.MammothAnimationPacket::encode,
                net.kazi.kazimod.network.MammothAnimationPacket::decode,
                net.kazi.kazimod.network.MammothAnimationPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
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

    public static void sendCameraShake(ServerPlayerEntity player, int durationTicks, float intensity) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new CameraShakePacket(durationTicks, intensity)
        );
    }

}
