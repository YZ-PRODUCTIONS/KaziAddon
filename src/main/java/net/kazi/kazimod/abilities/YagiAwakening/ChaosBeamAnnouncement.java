package net.kazi.kazimod.abilities.YagiAwakening;

import java.util.UUID;
import net.kazi.kazimod.init.KaziPacketHandler;
import net.kazi.kazimod.network.ChaosBeamTitlePacket;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.PacketDistributor;

/** Snapshot the audience when the Chaos Beam charge successfully begins. */
public final class ChaosBeamAnnouncement {
    public static final double RADIUS = 100D;
    public static final String TEXT = "WHY WONT YOU LET ME WIN";

    private ChaosBeamAnnouncement() { }

    public static void send(LivingEntity caster) {
        if (!(caster.level instanceof ServerWorld) || !caster.isAlive() || caster.removed) return;
        ServerWorld world = (ServerWorld) caster.level;
        ChaosBeamTitlePacket packet = new ChaosBeamTitlePacket(UUID.randomUUID(), world.dimension().location(),
                world.getGameTime(), TEXT);
        for (ServerPlayerEntity viewer : world.players()) {
            if (viewer.isAlive() && !viewer.removed && viewer.distanceToSqr(caster) <= RADIUS * RADIUS)
                KaziPacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer), packet);
        }
    }
}
