package net.kazi.kazimod.abilities.YagiAwakening;
import net.minecraft.entity.player.PlayerEntity;
public final class YagiAwakeningSupport {
    public static boolean canUnlock(PlayerEntity user) { return user != null && user.hasYagi && user.awakened; }
}
