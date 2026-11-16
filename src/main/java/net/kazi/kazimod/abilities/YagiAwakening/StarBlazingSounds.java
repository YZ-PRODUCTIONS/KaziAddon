package net.kazi.kazimod.abilities.YagiAwakening;

import net.kazi.kazimod.init.KaziPacketHandler;
import net.kazi.kazimod.network.AsrielSoundPacket;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.PacketDistributor;

/** Server-owned positional cues shared by Star Blazing and Galacta Blazing. */
public final class StarBlazingSounds {
    private static final double AUDIBLE_RANGE_SQUARED = 96.0D * 96.0D;

    private StarBlazingSounds() {}

    public static void cast(LivingEntity owner) {
        play(owner, owner.position(), AsrielSoundPacket.Cue.CAST, 0.8F, 1.0F);
    }

    public static void summon(LivingEntity owner, Vector3d point) {
        play(owner, point, AsrielSoundPacket.Cue.SUMMON,
                0.7F, 1.0F + owner.getRandom().nextFloat() * 0.2F - owner.getRandom().nextFloat() * 0.3F);
    }

    public static void finale(LivingEntity owner, Vector3d point) {
        // This asset already includes the original 0.4 pitch; Minecraft clamps runtime pitch to 0.5.
        play(owner, point, AsrielSoundPacket.Cue.FINALE, 1.0F, 1.0F);
    }

    public static void burst(LivingEntity owner, Vector3d point, boolean finisher) {
        play(owner, point, AsrielSoundPacket.Cue.BURST,
                finisher ? 0.8F : 0.4F, finisher ? 1.1F : 2.0F);
    }

    private static void play(LivingEntity owner, Vector3d point, AsrielSoundPacket.Cue cue, float volume, float pitch) {
        if (!(owner.level instanceof ServerWorld)) return;
        AsrielSoundPacket packet = new AsrielSoundPacket(owner.getUUID(), owner.level.dimension().location(),
                cue, point, volume, pitch);
        // Explicit range keeps distant strikes audible without increasing their source volume.
        for (ServerPlayerEntity listener : ((ServerWorld) owner.level).players()) {
            if (listener.position().distanceToSqr(point) <= AUDIBLE_RANGE_SQUARED) {
                KaziPacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> listener), packet);
            }
        }
    }
}
