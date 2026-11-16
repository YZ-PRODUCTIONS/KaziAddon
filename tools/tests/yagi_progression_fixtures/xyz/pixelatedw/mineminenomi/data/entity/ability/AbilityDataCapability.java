package xyz.pixelatedw.mineminenomi.data.entity.ability;
import net.minecraft.entity.player.PlayerEntity;
public final class AbilityDataCapability {
    public static IAbilityData get(PlayerEntity user) { return user.data; }
}
