package net.kazi.kazimod.abilities.YagiAwakening;

import net.kazi.kazimod.init.KaziPacketHandler;
import net.kazi.kazimod.network.AsrielSoundPacket;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.PacketDistributor;

/** Original cannon cues replace one another within this caster's weapon channel. */
public final class ChaosBusterSounds {
    private static final double AUDIBLE_RANGE_SQUARED = 96.0D * 96.0D;

    private ChaosBusterSounds() {}

    public static void target(LivingEntity owner, Vector3d point) {
        play(owner, point, AsrielSoundPacket.Cue.BUSTER_TARGET, 1.0F, 1.0F);
    }

    public static void fire(LivingEntity owner, Vector3d point) {
        play(owner, point, AsrielSoundPacket.Cue.BUSTER_FIRE, 0.8F, 1.0F);
    }

    public static void charge(LivingEntity owner, Vector3d point) {
        play(owner, point, AsrielSoundPacket.Cue.BUSTER_CHARGE, 1.0F, 0.55F);
    }

    public static void beam(LivingEntity owner, Vector3d point) {
        play(owner, point, AsrielSoundPacket.Cue.BUSTER_BEAM, 1.0F, 1.1F);
    }

    public static void stop(LivingEntity owner) {
        play(owner, owner.position(), AsrielSoundPacket.Cue.BUSTER_STOP, 0.0F, 1.0F);
    }

    private static void play(LivingEntity owner, Vector3d point, AsrielSoundPacket.Cue cue, float volume, float pitch) {
        if (!(owner.level instanceof ServerWorld)) return;
        AsrielSoundPacket packet = new AsrielSoundPacket(owner.getUUID(), owner.level.dimension().location(),
                cue, point, volume, pitch);
        for (ServerPlayerEntity listener : ((ServerWorld) owner.level).players()) {
            // Stop reaches earlier listeners even if the caster moved beyond the audible range.
            if (cue == AsrielSoundPacket.Cue.BUSTER_STOP
                    || listener.position().distanceToSqr(point) <= AUDIBLE_RANGE_SQUARED) {
                KaziPacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> listener), packet);
            }
        }
    }
}
