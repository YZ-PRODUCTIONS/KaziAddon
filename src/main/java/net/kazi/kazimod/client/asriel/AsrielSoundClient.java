package net.kazi.kazimod.client.asriel;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.kazi.kazimod.init.KaziSounds;
import net.kazi.kazimod.network.AsrielSoundPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Replaces each caster's previous cue without stopping another player's attack. */
@Mod.EventBusSubscriber(modid = "kazimod", value = Dist.CLIENT)
public final class AsrielSoundClient {
    private static final int MAX_PLAYBACKS = 192;
    private static final Map<SoundKey, Playback> PLAYBACKS = new HashMap<>();
    private static ClientWorld world;

    private AsrielSoundClient() {}

    public static void receive(AsrielSoundPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        synchronizeWorld(mc);
        if (mc.level == null || mc.player == null || !mc.level.dimension().location().equals(packet.dimension)) return;

        SoundHandler sounds = mc.getSoundManager();
        if (packet.cue == AsrielSoundPacket.Cue.SABER_STOP || packet.cue == AsrielSoundPacket.Cue.ANGEL_STOP) {
            Iterator<Map.Entry<SoundKey, Playback>> iterator = PLAYBACKS.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<SoundKey, Playback> entry = iterator.next();
                boolean matches = packet.cue == AsrielSoundPacket.Cue.SABER_STOP
                        ? entry.getKey().channel.isSaber()
                        : entry.getKey().channel == AsrielSoundPacket.Channel.ANGEL_SUMMON
                            || entry.getKey().channel == AsrielSoundPacket.Channel.ANGEL_WEAPON;
                if (entry.getKey().caster.equals(packet.casterUuid) && matches) {
                    sounds.stop(entry.getValue().sound);
                    iterator.remove();
                }
            }
            return;
        }
        SoundKey key = new SoundKey(packet.casterUuid, packet.cue.channel);
        Playback previous = PLAYBACKS.remove(key);
        if (previous != null) sounds.stop(previous.sound);
        if (packet.cue == AsrielSoundPacket.Cue.BUSTER_STOP) return;
        // Under extreme load, skip a new channel rather than interrupt an unrelated caster.
        if (PLAYBACKS.size() >= MAX_PLAYBACKS) return;

        boolean loop = packet.cue == AsrielSoundPacket.Cue.BUSTER_TARGET;
        SimpleSound sound = new SimpleSound(soundEvent(packet.cue).getLocation(), SoundCategory.PLAYERS,
                packet.volume, packet.pitch, loop, 0, ISound.AttenuationType.LINEAR,
                packet.position.x, packet.position.y, packet.position.z, false);
        // The longest aiming warning is 14 ticks. Expire a lost loop even if its caster disconnects.
        PLAYBACKS.put(key, new Playback(sound, loop ? 20 : 0));
        sounds.play(sound);
    }

    private static SoundEvent soundEvent(AsrielSoundPacket.Cue cue) {
        switch (cue) {
            case CAST: return KaziSounds.STAR_BLAZING_CAST_SFX.get();
            case SUMMON: return KaziSounds.STAR_BLAZING_SUMMON_SFX.get();
            case FINALE: return KaziSounds.STAR_BLAZING_FINALE_SFX.get();
            case BURST: return KaziSounds.STAR_BLAZING_BURST_SFX.get();
            case BUSTER_TARGET: return KaziSounds.CHAOS_BUSTER_TARGET_SFX.get();
            case BUSTER_FIRE: return KaziSounds.CHAOS_BUSTER_FIRE_SFX.get();
            case BUSTER_CHARGE: return KaziSounds.CHAOS_BUSTER_CHARGE_SFX.get();
            case BUSTER_BEAM: return KaziSounds.CHAOS_BUSTER_BEAM_SFX.get();
            case SABER_APPEAR: return KaziSounds.CHAOS_SABER_APPEAR_SFX.get();
            case SABER_GRAB: return KaziSounds.CHAOS_SABER_GRAB_SFX.get();
            case SABER_PULLBACK: return KaziSounds.CHAOS_SABER_PULLBACK_SFX.get();
            case SABER_CHARGE: return KaziSounds.CHAOS_SABER_CHARGE_SFX.get();
            case SABER_VOICE: return KaziSounds.CHAOS_SABER_VOICE_SFX.get();
            case SABER_CUT: return KaziSounds.CHAOS_SABER_CUT_SFX.get();
            case SABER_SPARKLES: return KaziSounds.CHAOS_SABER_SPARKLES_SFX.get();
            case ANGEL_CAST: return KaziSounds.ANGEL_OF_DEATH_CAST_SFX.get();
            case ANGEL_FIRE: return KaziSounds.ANGEL_OF_DEATH_FIRE_SFX.get();
            default: throw new IllegalArgumentException("Unknown Asriel sound cue");
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        synchronizeWorld(mc);
        SoundHandler sounds = mc.getSoundManager();
        Iterator<Playback> iterator = PLAYBACKS.values().iterator();
        while (iterator.hasNext()) {
            Playback playback = iterator.next();
            ++playback.ageTicks;
            if (playback.maxTicks > 0 && playback.ageTicks >= playback.maxTicks) {
                sounds.stop(playback.sound);
                iterator.remove();
                continue;
            }
            // Keep queued clips tracked while the audio engine finishes loading their buffers.
            if (playback.ageTicks > 20 && !sounds.isActive(playback.sound)) iterator.remove();
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        clear(Minecraft.getInstance().getSoundManager());
        world = null;
    }

    private static void synchronizeWorld(Minecraft mc) {
        if (world != mc.level) {
            clear(mc.getSoundManager());
            world = mc.level;
        }
    }

    private static void clear(SoundHandler sounds) {
        for (Playback playback : PLAYBACKS.values()) sounds.stop(playback.sound);
        PLAYBACKS.clear();
    }

    private static final class Playback {
        private final ISound sound;
        private final int maxTicks;
        private int ageTicks;
        private Playback(ISound sound, int maxTicks) { this.sound = sound; this.maxTicks = maxTicks; }
    }

    private static final class SoundKey {
        private final UUID caster;
        private final AsrielSoundPacket.Channel channel;

        private SoundKey(UUID caster, AsrielSoundPacket.Channel channel) {
            this.caster = caster;
            this.channel = channel;
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof SoundKey)) return false;
            SoundKey key = (SoundKey) other;
            return caster.equals(key.caster) && channel == key.channel;
        }

        @Override public int hashCode() { return 31 * caster.hashCode() + channel.hashCode(); }
    }
}
