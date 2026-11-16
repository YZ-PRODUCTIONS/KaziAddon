package net.kazi.kazimod.client.asriel;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import net.kazi.kazimod.abilities.YagiAwakening.ChaosBeamPattern;
import net.kazi.kazimod.entities.asriel.ChaosBeamAttackEntity;
import net.kazi.kazimod.init.KaziSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.TickableSound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Each field owns rising charge cues, a single launch, and the original sustained hold loop. */
@Mod.EventBusSubscriber(modid = "kazimod", value = Dist.CLIENT)
public final class ChaosBeamSoundClient {
    private static final Map<ChaosBeamAttackEntity, Playback> PLAYING = new IdentityHashMap<>();
    private ChaosBeamSoundClient() { }

    @SubscribeEvent public static void joined(EntityJoinWorldEvent event) {
        if (event.getWorld().isClientSide && event.getEntity() instanceof ChaosBeamAttackEntity && PLAYING.size() < 96) {
            ChaosBeamAttackEntity field = (ChaosBeamAttackEntity) event.getEntity();
            PLAYING.computeIfAbsent(field, Playback::new);
        }
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Iterator<Playback> iterator = PLAYING.values().iterator();
        while (iterator.hasNext()) {
            Playback playback = iterator.next();
            if (!playback.tick()) { playback.stop(); iterator.remove(); }
        }
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        for (Playback playback : PLAYING.values()) playback.stop();
        PLAYING.clear();
    }

    private static final class Playback {
        private final ChaosBeamAttackEntity field;
        private MovingCue charge, launch, hold;
        private int chargeCue = -1;
        private int ticks;
        private boolean fired;
        Playback(ChaosBeamAttackEntity field) { this.field = field; }

        boolean tick() {
            if (field.removed || Minecraft.getInstance().level != field.level
                    || ++ticks > ChaosBeamPattern.CHARGE_TICKS + ChaosBeamPattern.FIRE_TICKS + 40) return false;
            if (!field.isInitialized()) return true;
            float age = field.getVisualAge(0);
            if (!field.isFiring()) {
                if (age >= ChaosBeamPattern.CHARGE_TICKS + 5) return false;
                int due = ChaosBeamAudioTiming.latestChargeCue(age);
                if (due > chargeCue) {
                    chargeCue = due;
                    // Late tracking skips old cues instead of replaying a burst of six sounds.
                    if (age - ChaosBeamAudioTiming.chargeTick(due) <= 2F) {
                        stopCue(charge);
                        charge = play(KaziSounds.CHAOS_BEAM_CHARGE_SFX.get(), false,
                                ChaosBeamAudioTiming.chargeVolume(due), ChaosBeamAudioTiming.chargePitch(due));
                    }
                }
            } else {
                if (age >= ChaosBeamPattern.FIRE_TICKS) return false;
                if (!fired) {
                    fired = true;
                    stopCue(charge); charge = null;
                    if (age <= 3F) launch = play(KaziSounds.CHAOS_BEAM_LAUNCH_SFX.get(), false, .8F, 1F);
                    hold = play(KaziSounds.CHAOS_BEAM_HOLD_SFX.get(), true, ChaosBeamAudioTiming.holdVolume(age),
                            ChaosBeamAudioTiming.holdPitch(age));
                }
            }
            return true;
        }

        private MovingCue play(SoundEvent sound, boolean loop, float volume, float pitch) {
            MovingCue cue = new MovingCue(field, sound, loop, volume, pitch);
            Minecraft.getInstance().getSoundManager().play(cue);
            return cue;
        }
        void stop() { stopCue(charge); stopCue(launch); stopCue(hold); }
    }

    private static void stopCue(MovingCue cue) {
        if (cue != null) { cue.finish(); Minecraft.getInstance().getSoundManager().stop(cue); }
    }

    private static final class MovingCue extends TickableSound {
        private final ChaosBeamAttackEntity field;
        private int ticks;
        MovingCue(ChaosBeamAttackEntity field, SoundEvent sound, boolean loop, float gain, float speed) {
            super(sound, SoundCategory.PLAYERS);
            this.field = field;
            looping = loop; delay = 0; volume = gain; pitch = speed;
            relative = false; attenuation = AttenuationType.LINEAR;
            updatePosition();
        }
        @Override public boolean canStartSilent() { return true; }
        @Override public void tick() {
            if (field.removed || Minecraft.getInstance().level != field.level
                    || ++ticks > ChaosBeamPattern.CHARGE_TICKS + ChaosBeamPattern.FIRE_TICKS + 40) { stop(); return; }
            updatePosition();
            if (looping) {
                float age = field.getVisualAge(0);
                if (!field.isFiring() || age >= ChaosBeamPattern.FIRE_TICKS) { stop(); return; }
                pitch = ChaosBeamAudioTiming.holdPitch(age);
                volume = ChaosBeamAudioTiming.holdVolume(age);
            }
        }
        private void updatePosition() { x = field.getX(); y = field.getY(); z = field.getZ(); }
        private void finish() { stop(); }
    }
}
