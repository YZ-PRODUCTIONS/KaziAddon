package net.kazi.kazimod.abilities.YagiAwakening;
import net.minecraft.entity.Entity;
public final class ShockerBreakerSounds {
    public static void strike(Entity marker, int pulse) { marker.level.sounds.add(marker.level.getGameTime() + ":" + pulse + ":" + marker.getId()); }
}
