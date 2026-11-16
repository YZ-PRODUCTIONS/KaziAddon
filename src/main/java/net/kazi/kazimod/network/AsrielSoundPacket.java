package net.kazi.kazimod.network;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

/** A fixed set of awakening cues; channel ownership is always caster-specific. */
public final class AsrielSoundPacket {
    public enum Channel {
        CAST, SUMMON, BURST, BUSTER_WEAPON,
        SABER_SUMMON, SABER_WINDUP, SABER_VOICE, SABER_CUT, SABER_SPARKLES, ANGEL_WEAPON, ANGEL_SUMMON;

        public boolean isSaber() {
            return this == SABER_SUMMON || this == SABER_WINDUP || this == SABER_VOICE
                    || this == SABER_CUT || this == SABER_SPARKLES;
        }
    }

    public enum Cue {
        CAST(Channel.CAST), SUMMON(Channel.SUMMON), FINALE(Channel.SUMMON), BURST(Channel.BURST),
        BUSTER_TARGET(Channel.BUSTER_WEAPON), BUSTER_FIRE(Channel.BUSTER_WEAPON),
        BUSTER_CHARGE(Channel.BUSTER_WEAPON), BUSTER_BEAM(Channel.BUSTER_WEAPON),
        BUSTER_STOP(Channel.BUSTER_WEAPON),
        SABER_APPEAR(Channel.SABER_SUMMON), SABER_GRAB(Channel.SABER_SUMMON),
        SABER_PULLBACK(Channel.SABER_WINDUP), SABER_CHARGE(Channel.SABER_WINDUP),
        SABER_VOICE(Channel.SABER_VOICE), SABER_CUT(Channel.SABER_CUT),
        SABER_SPARKLES(Channel.SABER_SPARKLES), SABER_STOP(Channel.SABER_SUMMON),
        ANGEL_CAST(Channel.ANGEL_SUMMON), ANGEL_FIRE(Channel.ANGEL_WEAPON), ANGEL_STOP(Channel.ANGEL_WEAPON);

        public final Channel channel;
        Cue(Channel channel) { this.channel = channel; }
    }

    public final UUID casterUuid;
    public final ResourceLocation dimension;
    public final Cue cue;
    public final Vector3d position;
    public final float volume;
    public final float pitch;

    public AsrielSoundPacket(UUID casterUuid, ResourceLocation dimension, Cue cue,
                                  Vector3d position, float volume, float pitch) {
        this.casterUuid = Objects.requireNonNull(casterUuid, "casterUuid");
        this.dimension = Objects.requireNonNull(dimension, "dimension");
        this.cue = Objects.requireNonNull(cue, "cue");
        this.position = Objects.requireNonNull(position, "position");
        if (!validCoordinate(position.x) || !validCoordinate(position.y) || !validCoordinate(position.z)
                || !Float.isFinite(volume) || volume < 0.0F || volume > 1.0F
                || !Float.isFinite(pitch) || pitch < 0.5F || pitch > 2.0F) {
            throw new IllegalArgumentException("Invalid Asriel sound parameters");
        }
        this.volume = volume;
        this.pitch = pitch;
    }

    private static boolean validCoordinate(double value) {
        // Allow effect offsets just outside the vanilla world border as well as the caster's position.
        return Double.isFinite(value) && Math.abs(value) <= 32000000.0D;
    }

    public static void encode(AsrielSoundPacket packet, PacketBuffer buffer) {
        buffer.writeUUID(packet.casterUuid);
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeByte(packet.cue.ordinal());
        buffer.writeDouble(packet.position.x);
        buffer.writeDouble(packet.position.y);
        buffer.writeDouble(packet.position.z);
        buffer.writeFloat(packet.volume);
        buffer.writeFloat(packet.pitch);
    }

    public static AsrielSoundPacket decode(PacketBuffer buffer) {
        UUID casterUuid = buffer.readUUID();
        ResourceLocation dimension = buffer.readResourceLocation();
        int cueId = buffer.readUnsignedByte();
        if (cueId >= Cue.values().length) throw new IllegalArgumentException("Unknown Asriel sound cue");
        return new AsrielSoundPacket(casterUuid, dimension, Cue.values()[cueId],
                new Vector3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(AsrielSoundPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            context.enqueueWork(() -> ClientHandler.receive(packet));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void receive(AsrielSoundPacket packet) {
            net.kazi.kazimod.client.asriel.AsrielSoundClient.receive(packet);
        }
    }
}
