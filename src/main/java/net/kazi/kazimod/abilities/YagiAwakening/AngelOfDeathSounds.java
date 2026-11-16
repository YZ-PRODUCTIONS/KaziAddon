package net.kazi.kazimod.abilities.YagiAwakening;

import net.kazi.kazimod.init.KaziPacketHandler;
import net.kazi.kazimod.network.AsrielSoundPacket;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.PacketDistributor;

/** Original cast and hand-burst cues use their own per-caster replacement channel. */
public final class AngelOfDeathSounds {
    private AngelOfDeathSounds() { }

    public static void cast(LivingEntity owner) {
        play(owner, owner.getEyePosition(1), AsrielSoundPacket.Cue.ANGEL_CAST, .7F, 1.2F);
    }

    public static void fire(LivingEntity owner, Vector3d position) {
        play(owner, position, AsrielSoundPacket.Cue.ANGEL_FIRE, .9F, .8F);
    }

    public static void stop(LivingEntity owner) {
        play(owner, owner.position(), AsrielSoundPacket.Cue.ANGEL_STOP, 0, 1);
    }

    private static void play(LivingEntity owner, Vector3d position, AsrielSoundPacket.Cue cue,
                             float volume, float pitch) {
        if (!(owner.level instanceof ServerWorld)) return;
        AsrielSoundPacket packet = new AsrielSoundPacket(owner.getUUID(), owner.level.dimension().location(),
                cue, position, volume, pitch);
        for (ServerPlayerEntity listener : ((ServerWorld) owner.level).players()) {
            if (cue == AsrielSoundPacket.Cue.ANGEL_STOP || listener.position().distanceToSqr(position) <= 96 * 96)
                KaziPacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> listener), packet);
        }
    }
}
