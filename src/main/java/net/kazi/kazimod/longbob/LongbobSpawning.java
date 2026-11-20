package net.kazi.kazimod.longbob;

import net.kazi.kazimod.events.WorldBossSpawning;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.server.ServerWorld;

public final class LongbobSpawning extends WorldBossSpawning.Slot<LongbobEntity> {
    private static final String NAME = "kazimod_longbobthegod";

    public LongbobSpawning() {
        super(NAME, LongbobFeatures.BOSS, LongbobEntity::setNaturalBoss);
    }

    public static LongbobSpawning get(ServerWorld world) {
        return world.getServer().overworld().getDataStorage().computeIfAbsent(LongbobSpawning::new, NAME);
    }

    public static void announce(LongbobEntity boss) {
        WorldBossSpawning.announce(boss, "message.kazimod.longbobthegod.spawn", TextFormatting.GREEN);
    }
}
