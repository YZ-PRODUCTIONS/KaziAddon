package net.kazi.kazimod.client.asriel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kazi.kazimod.init.KaziSounds;
import net.kazi.kazimod.network.HyperGonerMusicPacket;
import net.kazi.kazimod.network.HyperGonerMusicPacket.Action;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.TickableSound;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Non-looping music follows the Goat owner even after the cinematic's camera is released. */
@Mod.EventBusSubscriber(modid = "kazimod", value = Dist.CLIENT)
public final class HyperGonerMusicClient {
    private static final Map<UUID, Music> PLAYING = new HashMap<>();

    private HyperGonerMusicClient() { }

    public static void receive(HyperGonerMusicPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        Music music = PLAYING.get(packet.session);
        if (packet.action == Action.STOP) {
            if (music != null) {
                mc.getSoundManager().stop(music);
                PLAYING.remove(packet.session);
            }
        } else if (packet.action == Action.POSITION) {
            if (music != null) {
                music.position = packet.position;
                music.dimension = packet.dimension;
            }
        } else if (music == null && mc.level != null
                && mc.level.dimension().location().equals(packet.dimension)) {
            music = new Music(packet);
            PLAYING.put(packet.session, music);
            mc.getSoundManager().play(music);
        }
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        // Do not tie the stream to a world/entity instance, health threshold, or cutscene timer.
        PLAYING.values().removeIf(music -> music.isStopped()
                || ++music.queuedTicks > 100 && !mc.getSoundManager().isActive(music));
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        for (Music music : PLAYING.values()) Minecraft.getInstance().getSoundManager().stop(music);
        PLAYING.clear();
    }

    private static final class Music extends TickableSound {
        final UUID caster;
        Vector3d position;
        ResourceLocation dimension;
        int queuedTicks;

        Music(HyperGonerMusicPacket packet) {
            super(KaziSounds.HOPES_AND_DREAMS_MUSIC.get(), SoundCategory.RECORDS);
            caster = packet.caster;
            position = packet.position;
            dimension = packet.dimension;
            volume = 1;
            pitch = 1;
            looping = false;
            relative = false;
            attenuation = AttenuationType.LINEAR;
            updatePosition();
        }

        @Override public boolean canStartSilent() { return true; }

        @Override public void tick() {
            Minecraft mc = Minecraft.getInstance();
            boolean sameDimension = mc.level != null && mc.level.dimension().location().equals(dimension);
            volume = sameDimension ? 1 : 0;
            if (sameDimension) {
                PlayerEntity player = mc.level.getPlayerByUUID(caster);
                if (player != null && !player.removed) position = player.position();
            }
            // Untracking is not death. Server updates retain the moving source outside tracking range.
            updatePosition();
        }

        private void updatePosition() { x = position.x; y = position.y + 1; z = position.z; }
    }
}
