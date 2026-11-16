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

    // Visual messages and beam data require matching client/server versions.
    private static final String PROTOCOL_VERSION = "21";
    private static int id = 0;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("kazimod", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        CHANNEL.registerMessage(id++, net.kazi.kazimod.ope.WillePacket.class,
                net.kazi.kazimod.ope.WillePacket::encode, net.kazi.kazimod.ope.WillePacket::decode,
                net.kazi.kazimod.ope.WillePacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, net.kazi.kazimod.ope.OpeVisualPacket.class,
                net.kazi.kazimod.ope.OpeVisualPacket::encode,
                net.kazi.kazimod.ope.OpeVisualPacket::decode,
                net.kazi.kazimod.ope.OpeVisualPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, net.kazi.kazimod.mochi.MochiVisualPacket.class,
                net.kazi.kazimod.mochi.MochiVisualPacket::encode,
                net.kazi.kazimod.mochi.MochiVisualPacket::decode,
                net.kazi.kazimod.mochi.MochiVisualPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, net.kazi.kazimod.yami.DarknessLaunchPacket.class,
                net.kazi.kazimod.yami.DarknessLaunchPacket::encode,
                net.kazi.kazimod.yami.DarknessLaunchPacket::decode,
                net.kazi.kazimod.yami.DarknessLaunchPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, net.kazi.kazimod.moku.MokuVisualPacket.class,
                net.kazi.kazimod.moku.MokuVisualPacket::encode,
                net.kazi.kazimod.moku.MokuVisualPacket::decode,
                net.kazi.kazimod.moku.MokuVisualPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, net.kazi.kazimod.pika.PikaVisualPacket.class,
                net.kazi.kazimod.pika.PikaVisualPacket::encode,
                net.kazi.kazimod.pika.PikaVisualPacket::decode,
                net.kazi.kazimod.pika.PikaVisualPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, net.kazi.kazimod.yami.YamiVisualPacket.class,
                net.kazi.kazimod.yami.YamiVisualPacket::encode,
                net.kazi.kazimod.yami.YamiVisualPacket::decode,
                net.kazi.kazimod.yami.YamiVisualPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, net.kazi.kazimod.progression.FruitlessProgressionPacket.class,
                net.kazi.kazimod.progression.FruitlessProgressionPacket::encode,
                net.kazi.kazimod.progression.FruitlessProgressionPacket::decode,
                net.kazi.kazimod.progression.FruitlessProgressionPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, net.kazi.kazimod.seiryu.SeiryuSpinePacket.class,
                net.kazi.kazimod.seiryu.SeiryuSpinePacket::encode,
                net.kazi.kazimod.seiryu.SeiryuSpinePacket::decode,
                net.kazi.kazimod.seiryu.SeiryuSpinePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
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
        CHANNEL.registerMessage(id++, net.kazi.kazimod.leopard.LeopardGrabPacket.class,
                net.kazi.kazimod.leopard.LeopardGrabPacket::encode,
                net.kazi.kazimod.leopard.LeopardGrabPacket::decode,
                net.kazi.kazimod.leopard.LeopardGrabPacket::handle,
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
