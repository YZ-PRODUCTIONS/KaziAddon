package net.kazi.kazimod.abilities.YagiAwakening;

import net.kazi.kazimod.init.KaziPacketHandler;
import net.kazi.kazimod.network.AsrielSoundPacket;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.PacketDistributor;

/** Original sword-controller cues, with voice, cutting and sparkles allowed to overlap. */
public final class ChaosSaberSounds {
    private static final double RANGE_SQUARED = 96.0D * 96.0D;
    private ChaosSaberSounds() {}

    public static void appear(LivingEntity owner, Vector3d point) {
        play(owner, point, AsrielSoundPacket.Cue.SABER_APPEAR, 1.0F, 1.0F);
    }
    public static void grab(LivingEntity owner, Vector3d point) {
        play(owner, point, AsrielSoundPacket.Cue.SABER_GRAB, 1.0F, 1.0F);
    }
    public static void pullback(LivingEntity owner, Vector3d point) {
        play(owner, point, AsrielSoundPacket.Cue.SABER_PULLBACK, 0.9F, 1.0F);
    }
    public static void charge(LivingEntity owner, Vector3d point) {
        play(owner, point, AsrielSoundPacket.Cue.SABER_CHARGE, 1.0F, 1.0F);
    }
    public static void slash(LivingEntity owner, Vector3d point, boolean finale) {
        if (!finale) {
            // Minecraft clamps an individual source's gain at 1; the original voice requests 1.2.
            play(owner, point, AsrielSoundPacket.Cue.SABER_VOICE, 1.0F, 1.0F);
        }
        play(owner, point, AsrielSoundPacket.Cue.SABER_CUT, finale ? 0.9F : 0.7F, finale ? 1.0F : 1.4F);
    }
    public static void sparkles(LivingEntity owner, Vector3d point) {
        play(owner, point, AsrielSoundPacket.Cue.SABER_SPARKLES, 0.7F, 1.0F);
    }
    public static void stop(LivingEntity owner) {
        play(owner, owner.position(), AsrielSoundPacket.Cue.SABER_STOP, 0.0F, 1.0F);
    }
    private static void play(LivingEntity owner, Vector3d point, AsrielSoundPacket.Cue cue, float volume, float pitch) {
        if (!(owner.level instanceof ServerWorld)) return;
        AsrielSoundPacket packet = new AsrielSoundPacket(owner.getUUID(), owner.level.dimension().location(), cue, point, volume, pitch);
        for (ServerPlayerEntity listener : ((ServerWorld) owner.level).players()) {
            if (cue == AsrielSoundPacket.Cue.SABER_STOP || listener.position().distanceToSqr(point) <= RANGE_SQUARED) {
                KaziPacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> listener), packet);
            }
        }
    }
}
