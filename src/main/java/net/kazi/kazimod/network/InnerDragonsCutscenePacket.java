package net.kazi.kazimod.network;

import java.util.UUID;
import net.kazi.kazimod.abilities.cinematics.AwakeningCutsceneType;
import java.util.function.Supplier;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

/** The server selects the cinematic and supplies its authoritative shared timeline. */
public final class InnerDragonsCutscenePacket {
    public final int casterId;
    public final UUID casterUuid;
    public final long startTick;
    public final Vector3d origin;
    public final float yaw;
    public final boolean cancel;
    public final AwakeningCutsceneType type;
    public final ClashPartner clash;
    public final BankaiRelease release;

    public static final class BankaiRelease {
        public final int fieldId, skinParts;
        public final String name;
        public BankaiRelease(int fieldId, String name, int skinParts) {
            this.fieldId = fieldId;
            this.name = name;
            this.skinParts = skinParts;
        }
    }

    /** The Koku user is the caster. Only a clash also carries a Kama user and the two skin profile names. */
    public static final class ClashPartner {
        public final int id;
        public final UUID uuid;
        public final Vector3d origin;
        public final String kokuName, kamaName;
        public final int kokuParts, kamaParts;
        public ClashPartner(int id, UUID uuid, Vector3d origin, String kokuName, String kamaName) {
            this(id, uuid, origin, kokuName, kamaName, 127, 127);
        }
        public ClashPartner(int id, UUID uuid, Vector3d origin, String kokuName, String kamaName, int kokuParts, int kamaParts) {
            this.id = id;
            this.uuid = uuid;
            this.origin = origin;
            this.kokuName = kokuName;
            this.kamaName = kamaName;
            this.kokuParts = kokuParts;
            this.kamaParts = kamaParts;
        }
    }

    public InnerDragonsCutscenePacket(int casterId, UUID casterUuid, long startTick,
                                      Vector3d origin, float yaw, boolean cancel) {
        this(casterId, casterUuid, startTick, origin, yaw, cancel, false);
    }

    public InnerDragonsCutscenePacket(int casterId, UUID casterUuid, long startTick,
            Vector3d origin, float yaw, boolean cancel, boolean goru) {
        this(casterId, casterUuid, startTick, origin, yaw, cancel,
                goru ? AwakeningCutsceneType.EA_DRAW : AwakeningCutsceneType.INNER_DRAGONS);
    }

    public InnerDragonsCutscenePacket(int casterId, UUID casterUuid, long startTick,
            Vector3d origin, float yaw, boolean cancel, AwakeningCutsceneType type) {
        this(casterId, casterUuid, startTick, origin, yaw, cancel, type, null);
    }

    public InnerDragonsCutscenePacket(int casterId, UUID casterUuid, long startTick,
            Vector3d origin, float yaw, boolean cancel, AwakeningCutsceneType type, ClashPartner clash) {
        this(casterId, casterUuid, startTick, origin, yaw, cancel, type, clash, null);
    }

    public InnerDragonsCutscenePacket(int casterId, UUID casterUuid, long startTick,
            Vector3d origin, float yaw, boolean cancel, AwakeningCutsceneType type, ClashPartner clash, BankaiRelease release) {
        this.type = java.util.Objects.requireNonNull(type);
        if ((type == AwakeningCutsceneType.DOMAIN_CLASH) != (clash != null))
            throw new IllegalArgumentException("A domain clash needs two participants");
        this.clash = clash;
        if ((type == AwakeningCutsceneType.SENBONZAKURA_KAGEYOSHI) != (release != null))
            throw new IllegalArgumentException("A Bankai camera needs a release field");
        this.release = release;
        this.casterId = casterId;
        this.casterUuid = casterUuid;
        this.startTick = startTick;
        this.origin = origin;
        this.yaw = yaw;
        this.cancel = cancel;
    }

    public static void encode(InnerDragonsCutscenePacket packet, PacketBuffer buffer) {
        buffer.writeInt(packet.casterId);
        buffer.writeUUID(packet.casterUuid);
        buffer.writeLong(packet.startTick);
        buffer.writeDouble(packet.origin.x);
        buffer.writeDouble(packet.origin.y);
        buffer.writeDouble(packet.origin.z);
        buffer.writeFloat(packet.yaw);
        buffer.writeBoolean(packet.cancel);
        buffer.writeVarInt(packet.type.wireId);
        if (packet.clash != null) {
            ClashPartner b = packet.clash;
            buffer.writeVarInt(b.id);
            buffer.writeUUID(b.uuid);
            buffer.writeDouble(b.origin.x);
            buffer.writeDouble(b.origin.y);
            buffer.writeDouble(b.origin.z);
            buffer.writeUtf(b.kokuName, 16);
            buffer.writeUtf(b.kamaName, 16);
            buffer.writeByte(b.kokuParts);
            buffer.writeByte(b.kamaParts);
        }
        if (packet.release != null) {
            buffer.writeVarInt(packet.release.fieldId);
            buffer.writeUtf(packet.release.name, 16);
            buffer.writeByte(packet.release.skinParts);
        }
    }

    public static InnerDragonsCutscenePacket decode(PacketBuffer buffer) {
        int id = buffer.readInt();
        UUID uuid = buffer.readUUID();
        long start = buffer.readLong();
        Vector3d origin = new Vector3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        float yaw = buffer.readFloat();
        boolean cancel = buffer.readBoolean();
        AwakeningCutsceneType type = AwakeningCutsceneType.fromWire(buffer.readVarInt());
        ClashPartner clash = type != AwakeningCutsceneType.DOMAIN_CLASH ? null : new ClashPartner(
                buffer.readVarInt(), buffer.readUUID(),
                new Vector3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                buffer.readUtf(16), buffer.readUtf(16), buffer.readUnsignedByte(), buffer.readUnsignedByte());
        BankaiRelease release = type == AwakeningCutsceneType.SENBONZAKURA_KAGEYOSHI
                ? new BankaiRelease(buffer.readVarInt(), buffer.readUtf(16), buffer.readUnsignedByte()) : null;
        return new InnerDragonsCutscenePacket(id, uuid, start, origin, yaw, cancel, type, clash, release);
    }

    public static void handle(InnerDragonsCutscenePacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientHandler.receive(packet));
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void receive(InnerDragonsCutscenePacket packet) {
            net.kazi.kazimod.client.nagi.InnerDragonsCutsceneClient.receive(packet);
        }
    }
}
