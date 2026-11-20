package net.kazi.kazimod.jerome;

import net.kazi.kazimod.events.WorldBossSpawning;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.server.ServerWorld;

public final class JeromeSpawning extends WorldBossSpawning.Slot<JeromeEntity> {
    private static final String NAME = "kazimod_jerome";

    public JeromeSpawning() {
        super(NAME, JeromeFeatures.BOSS, JeromeEntity::setNaturalBoss);
    }

    public static JeromeSpawning get(ServerWorld world) {
        return world.getServer().overworld().getDataStorage().computeIfAbsent(JeromeSpawning::new, NAME);
    }

    public static void announce(JeromeEntity boss) {
        WorldBossSpawning.announce(boss, "message.kazimod.jerome.spawn", TextFormatting.GOLD);
    }
}


