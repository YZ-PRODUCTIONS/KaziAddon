package net.kazi.kazimod.client.goru;

import java.util.*;
import net.kazi.kazimod.init.KaziSounds;
import net.kazi.kazimod.network.GoruCutsceneMusicPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.TickableSound;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** A non-looping positional stream; camera changes and scene cancellation cannot stop it. */
@Mod.EventBusSubscriber(modid = "kazimod", value = Dist.CLIENT)
public final class GoruCutsceneMusicClient {
    private static final Map<UUID, Music> PLAYING = new HashMap<>();
    private GoruCutsceneMusicClient() { }
    public static void receive(GoruCutsceneMusicPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        Music sound = PLAYING.get(packet.session);
        if (packet.action == 2) {
            if (sound != null) { mc.getSoundManager().stop(sound); PLAYING.remove(packet.session); }
        } else if (packet.action == 1) {
            if (sound != null) sound.position = packet.position;
        } else if (packet.action == 0 && mc.level != null && sound == null) {
            sound = new Music(packet, mc.level);
            PLAYING.put(packet.session, sound);
            mc.getSoundManager().play(sound);
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        PLAYING.values().removeIf(sound -> {
            if (mc.level != sound.world) { mc.getSoundManager().stop(sound); return true; }
            return sound.isStopped() || ++sound.queuedTicks > 20 && !mc.getSoundManager().isActive(sound);
        });
    }
    private static final class Music extends TickableSound {
        final UUID caster;
        final ClientWorld world;
        Vector3d position;
        int queuedTicks;
        Music(GoruCutsceneMusicPacket packet, ClientWorld world) {
            super(KaziSounds.GORU_CUTSCENE_MUSIC.get(), SoundCategory.PLAYERS);
            this.caster = packet.caster; this.world = world; this.position = packet.position;
            volume = 1; pitch = 1; looping = false; relative = false;
            attenuation = AttenuationType.LINEAR;
            updatePosition();
        }
        @Override public boolean canStartSilent() { return true; }
        @Override public void tick() {
            if (Minecraft.getInstance().level != world) { stop(); return; }
            PlayerEntity player = world.getPlayerByUUID(caster);
            if (player != null) {
                if (!player.isAlive()) { stop(); return; }
                position = player.position();
            }
            updatePosition();
        }
        private void updatePosition() { x = position.x; y = position.y + 1; z = position.z; }
    }
}
