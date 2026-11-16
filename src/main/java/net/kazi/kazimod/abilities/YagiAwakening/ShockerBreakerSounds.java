package net.kazi.kazimod.abilities.YagiAwakening;

import net.kazi.kazimod.init.KaziSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SPlaySoundEffectPacket;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.server.ServerWorld;

/** Independent impact sounds for every pillar, audible even to the distant caster. */
public final class ShockerBreakerSounds {
    private static final double RANGE_SQUARED = 96D * 96D;
    private ShockerBreakerSounds() { }

    public static void cast(Entity caster) {
        play(caster, KaziSounds.SHOCKER_BREAKER_CAST_SFX.get(), .8F, 1F);
    }

    public static void strike(Entity marker, int pulse) {
        // Compensate for the clip's encoding headroom and positional falloff while retaining the source pitch.
        play(marker, (pulse % 2 == 0 ? KaziSounds.SHOCKER_BREAKER_STRIKE_SFX
                : KaziSounds.SHOCKER_BREAKER_STRIKE_ALT_SFX).get(), 1F, .6F);
    }

    private static void play(Entity source, SoundEvent sound, float volume, float pitch) {
        if (!(source.level instanceof ServerWorld)) return;
        // Each packet starts a new sound instance; earlier pillars continue playing underneath it.
        SPlaySoundEffectPacket packet = new SPlaySoundEffectPacket(sound, SoundCategory.PLAYERS,
                source.getX(), source.getY(), source.getZ(), volume, pitch);
        // Vanilla World.playSound broadcasts quiet clips only16blocks, shorter than this move's48block aim range.
        for (ServerPlayerEntity listener : ((ServerWorld) source.level).players()) {
            if (listener.distanceToSqr(source) <= RANGE_SQUARED) listener.connection.send(packet);
        }
    }
}
