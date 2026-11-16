package net.kazi.kazimod.effects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kazi.kazimod.entities.EaVfxEntity;
import net.kazi.kazimod.init.KaziSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.TickableSound;
import net.minecraft.entity.Entity;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Positional, per-cast audio. Client-only so dedicated servers never load sound classes. */
@Mod.EventBusSubscriber(modid = "kazimod", value = Dist.CLIENT)
public final class EaSoundController {
    private static final int CANCEL_FADE_TICKS = 8;
    private static final int START_WINDOW_TICKS = 6;
    private static final double AUDIBLE_RADIUS = 128.0D;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private EaSoundController() { }

    /** Call once from the effect's client tick, including its cancelled/fading phase. */
    public static void tick(EaVfxEntity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || entity.level != minecraft.level) return;
        Session session = SESSIONS.computeIfAbsent(entity.getUUID(), ignored -> new Session(entity));
        if (entity.isCancelled() || !entity.isAlive()) {
            session.fade();
            return;
        }

        if (session.started) return;
        session.started = true;

        // Keep the supplied chant and release on one continuous timeline. The
        // nine-second visual transition must neither interrupt nor replay it.
        // Ogg playback cannot seek: clients tracking an older cast stay silent.
        if (entity.isReleased() || entity.getPhaseAge(0.0F) > START_WINDOW_TICKS
                || distanceVolume(entity) <= 0.0F) return;
        EaSound sound = new EaSound(entity);
        session.sounds.add(sound);
        minecraft.getSoundManager().play(sound);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        Iterator<Session> sessions = SESSIONS.values().iterator();
        while (sessions.hasNext()) {
            Session session = sessions.next();
            if (minecraft.level == null || session.entity.level != minecraft.level) {
                for (EaSound sound : session.sounds) minecraft.getSoundManager().stop(sound);
                sessions.remove();
                continue;
            }
            if (!session.entity.isAlive() || session.entity.isCancelled()) session.fade();
            // SoundEngine can finish a non-looping sample without calling stop()
            // on its TickableSound. Allow the asynchronous startup a few ticks,
            // then release finished samples so removed casts never leak sessions.
            session.sounds.removeIf(sound -> sound.isStopped()
                    || ++sound.queuedTicks > 10 && !minecraft.getSoundManager().isActive(sound));
            if (!session.entity.isAlive() && session.sounds.isEmpty()) sessions.remove();
        }
    }

    private static float distanceVolume(Entity source) {
        Entity listener = Minecraft.getInstance().getCameraEntity();
        if (listener == null) return 0.0F;
        double distance = Math.sqrt(source.distanceToSqr(listener));
        double remaining = Math.max(0.0D, Math.min(1.0D, (AUDIBLE_RADIUS - distance) / (AUDIBLE_RADIUS - 16.0D)));
        // Smooth falloff keeps a cast's audible boundary from clicking or popping.
        return (float)(remaining * remaining * (3.0D - 2.0D * remaining));
    }

    private static final class Session {
        private final EaVfxEntity entity;
        private final List<EaSound> sounds = new ArrayList<>(2);
        private boolean started;

        private Session(EaVfxEntity entity) { this.entity = entity; }

        private void fade() {
            for (EaSound sound : this.sounds) sound.fading = true;
        }
    }

    private static final class EaSound extends TickableSound {
        private final EaVfxEntity entity;
        private static final float BASE_VOLUME = 0.9F;
        private static final int LIFETIME = EaVfxEntity.CHARGE_TICKS
                + EaVfxEntity.RELEASE_TICKS + EaVfxEntity.FADE_TICKS;
        private boolean fading;
        private int age;
        private int fadeAge;
        private int queuedTicks;

        private EaSound(EaVfxEntity entity) {
            super(KaziSounds.EA_CAST_SFX.get(), SoundCategory.PLAYERS);
            this.entity = entity;
            this.volume = BASE_VOLUME * distanceVolume(entity);
            this.pitch = 1.0F;
            this.looping = false;
            this.relative = false;
            // Distance is handled above so a 128-block range does not require a
            // deafening source volume. Mono samples still retain positional panning.
            this.attenuation = AttenuationType.NONE;
            updatePosition();
        }

        @Override
        public void tick() {
            if (Minecraft.getInstance().level != this.entity.level) {
                this.stop();
                return;
            }
            if (!this.entity.isAlive() || this.entity.isCancelled()) this.fading = true;
            updatePosition();
            float envelope = Math.min(1.0F, ++this.age / 2.0F);
            if (this.fading) envelope *= Math.max(0.0F, 1.0F - ++this.fadeAge / (float)CANCEL_FADE_TICKS);
            // The assets include their designed decay. This short terminal taper is
            // a safety net if resource packs replace them with longer sound files.
            envelope *= Math.max(0.0F, Math.min(1.0F, (LIFETIME - this.age) / 4.0F));
            this.volume = BASE_VOLUME * distanceVolume(this.entity) * envelope;
            if (this.age >= LIFETIME || this.fadeAge >= CANCEL_FADE_TICKS) this.stop();
        }

        private void updatePosition() {
            this.x = this.entity.getX();
            this.y = this.entity.getY() + 1.0D;
            this.z = this.entity.getZ();
        }
    }
}
