package xyz.pixelatedw.mineminenomi.api.helpers;
import net.minecraft.entity.player.PlayerEntity;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
public final class AbilityHelper {
    public static void emergencyStopAbility(PlayerEntity user, IAbility ability) { ability.stop(); }
}
