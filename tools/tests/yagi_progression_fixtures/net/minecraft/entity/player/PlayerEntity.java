package net.minecraft.entity.player;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.packets.server.SSyncAbilityDataPacket;
public final class PlayerEntity {
    public final World level = new World();
    public boolean hasYagi = true, awakened = true;
    public IAbilityData data;
    public int syncPackets;
    public SSyncAbilityDataPacket lastPacket;
    public int getId() { return 73; }
}
