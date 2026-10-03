package net.kazi.kazimod.effects;

import java.util.HashMap;
import java.util.Iterator;
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

/** A positional wind loop follows each charge and tapers into its release burst. */
@Mod.EventBusSubscriber(modid = "kazimod", value = Dist.CLIENT)
public final class WindsRaptureSoundController {
    private static final int FADE_TICKS = 5;
    private static final double AUDIBLE_RADIUS = 64.0D;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private WindsRaptureSoundController() { }

    /** The entity invokes this only after synchronized cast metadata has arrived. */
    public static void tick(EaVfxEntity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || entity.level != minecraft.level) return;
        Session session = SESSIONS.computeIfAbsent(entity.getUUID(), ignored -> new Session(entity));
        if (entity.isCancelled() || !entity.isAlive()) {
            session.fade();
            return;
        }
        if (entity.isReleased()) {
            if (session.charge != null) session.charge.fading = true;
            if (!session.released) {
                session.released = true;
                // Do not replay a burst to clients that start tracking an older cast.
                if (entity.getPhaseAge(0) <= 6 && distanceVolume(entity) > 0) {
                    session.burst = new WindSound(entity, false);
                    minecraft.getSoundManager().play(session.burst);
                }
            }
        } else if (!session.started) {
            session.started = true;
            session.charge = new WindSound(entity, true);
            minecraft.getSoundManager().play(session.charge);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        Iterator<Session> sessions = SESSIONS.values().iterator();
        while (sessions.hasNext()) {
            Session session = sessions.next();
            if (minecraft.level == null || session.entity.level != minecraft.level) {
                if (session.charge != null) minecraft.getSoundManager().stop(session.charge);
                if (session.burst != null) minecraft.getSoundManager().stop(session.burst);
                sessions.remove();
                continue;
            }
            if (!session.entity.isAlive() || session.entity.isCancelled()) session.fade();
            if (finished(session.charge, minecraft)) session.charge = null;
            if (finished(session.burst, minecraft)) session.burst = null;
            if (!session.entity.isAlive() && session.charge == null && session.burst == null) sessions.remove();
        }
    }

    private static boolean finished(WindSound sound, Minecraft minecraft) {
        return sound == null || sound.isStopped()
                || ++sound.queuedTicks > 10 && !minecraft.getSoundManager().isActive(sound);
    }

    private static float distanceVolume(Entity source) {
        Entity listener = Minecraft.getInstance().getCameraEntity();
        if (listener == null) return 0;
        double distance = Math.sqrt(source.distanceToSqr(listener));
        double remaining = Math.max(0, Math.min(1, (AUDIBLE_RADIUS - distance) / (AUDIBLE_RADIUS - 8)));
        return (float)(remaining * remaining * (3 - 2 * remaining));
    }

    private static final class Session {
        private final EaVfxEntity entity;
        private WindSound charge;
        private WindSound burst;
        private boolean started;
        private boolean released;

        private Session(EaVfxEntity entity) { this.entity = entity; }

        private void fade() {
            if (charge != null) charge.fading = true;
            if (burst != null) burst.fading = true;
        }
    }

    private static final class WindSound extends TickableSound {
        private final EaVfxEntity entity;
        private final boolean charge;
        private boolean fading;
        private int age;
        private int fadeAge;
        private int queuedTicks;

        private WindSound(EaVfxEntity entity, boolean charge) {
            super(charge ? KaziSounds.WINDS_RAPTURE_CHARGE_SFX.get()
                    : KaziSounds.WINDS_RAPTURE_RELEASE_SFX.get(), SoundCategory.PLAYERS);
            this.entity = entity;
            this.charge = charge;
            this.looping = charge;
            this.delay = 0;
            this.relative = false;
            // The mono assets retain directional panning. Apply a moderate 64-block
            // falloff ourselves instead of increasing volume to extend vanilla range.
            this.attenuation = AttenuationType.NONE;
            this.pitch = charge ? 0.85F : 1;
            this.volume = (charge ? 0.55F : 0.85F) * distanceVolume(entity);
            updatePosition();
        }

        @Override
        public void tick() {
            if (Minecraft.getInstance().level != entity.level) { stop(); return; }
            if (!entity.isAlive() || entity.isCancelled() || charge && entity.isReleased()) fading = true;
            updatePosition();
            float progress = entity.getChargeProgress(0);
            this.pitch = charge ? 0.85F + 0.40F * progress : 1;
            float envelope = Math.min(1, ++age / 3.0F);
            if (fading) envelope *= Math.max(0, 1 - ++fadeAge / (float)FADE_TICKS);
            this.volume = (charge ? 0.55F + 0.25F * progress : 0.85F)
                    * distanceVolume(entity) * envelope;
            // Bounds also clean up a looping resource-pack replacement if its cast
            // disappears before a final metadata packet can reach this client.
            if (fadeAge >= FADE_TICKS || age >= (charge ? 80 : 32)) stop();
        }

        private void updatePosition() {
            this.x = entity.getX();
            this.y = entity.getY();
            this.z = entity.getZ();
        }
    }
}
