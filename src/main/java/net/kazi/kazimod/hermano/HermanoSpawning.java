package net.kazi.kazimod.hermano;

import net.kazi.kazimod.events.WorldBossSpawning;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.server.ServerWorld;

public final class HermanoSpawning extends WorldBossSpawning.Slot<HermanoEntity> {
    private static final String NAME = "kazimod_el_hermano";

    public HermanoSpawning() {
        super(NAME, HermanoFeatures.BOSS, HermanoEntity::setNaturalBoss);
    }

    public static HermanoSpawning get(ServerWorld world) {
        return world.getServer().overworld().getDataStorage().computeIfAbsent(HermanoSpawning::new, NAME);
    }

    public static void announce(HermanoEntity boss) {
        WorldBossSpawning.announce(boss, "message.kazimod.el_hermano.spawn", TextFormatting.GOLD);
    }
}
