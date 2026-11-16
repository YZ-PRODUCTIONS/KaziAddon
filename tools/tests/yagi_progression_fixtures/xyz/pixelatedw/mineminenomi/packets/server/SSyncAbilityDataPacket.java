package xyz.pixelatedw.mineminenomi.packets.server;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
public final class SSyncAbilityDataPacket {
    public final int entityId;
    public final IAbilityData data;
    public SSyncAbilityDataPacket(int entityId, IAbilityData data) { this.entityId = entityId; this.data = data; }
}
