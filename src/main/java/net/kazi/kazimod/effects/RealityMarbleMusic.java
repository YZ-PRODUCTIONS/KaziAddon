package net.kazi.kazimod.effects;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kazi.kazimod.init.KaziSounds;
import net.kazi.kazimod.network.RealityMarbleMusicPacket.SecondarySound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.TickableSound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "kazimod", value = Dist.CLIENT)
public final class RealityMarbleMusic {
    private static final Map<UUID, List<MarbleSound>> SOUNDS = new HashMap<>();

    public static void receive(UUID session, boolean playing, SecondarySound secondary) {
        if (playing) {
            if (Minecraft.getInstance().level == null || SOUNDS.containsKey(session)) return;
            List<MarbleSound> sounds = new ArrayList<>(2);
            // Both marble themes use this shared layer; halve every layer so the
            // complete Mugen and Infinite mixes retain their existing balance.
            sounds.add(new MarbleSound(KaziSounds.REALITY_MARBLE_CHARGE_MUSIC_SFX.get(), 0.25F));
            if (secondary == SecondarySound.INFINITE_CREATION) {
                sounds.add(new MarbleSound(KaziSounds.INFINITE_CREATION_CHARGE_SECONDARY_SFX.get(), 0.5F));
            } else if (secondary == SecondarySound.MUGEN) {
                sounds.add(new MarbleSound(KaziSounds.MUGEN_CHARGE_SECONDARY_SFX.get(), 0.5F));
            }
            SOUNDS.put(session, sounds);
            for (MarbleSound sound : sounds) Minecraft.getInstance().getSoundManager().play(sound);
        } else {
            List<MarbleSound> sounds = SOUNDS.get(session);
            if (sounds != null) for (MarbleSound sound : sounds) sound.fading = true;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        SOUNDS.entrySet().removeIf(entry -> {
            List<MarbleSound> sounds = entry.getValue();
            if (minecraft.level == null) {
                for (MarbleSound sound : sounds) minecraft.getSoundManager().stop(sound);
                return true;
            }
            // Keep either layer reachable for cancellation after the other finishes.
            sounds.removeIf(sound -> sound.isStopped() || !minecraft.getSoundManager().isActive(sound));
            return sounds.isEmpty();
        });
    }

    private static final class MarbleSound extends TickableSound {
        private static final int FADE_TICKS = 20;
        private final float initialVolume;
        private boolean fading;
        private int fadeTicks;

        private MarbleSound(SoundEvent event) {
            this(event, 1.0F);
        }

        private MarbleSound(SoundEvent event, float volume) {
            super(event, SoundCategory.MUSIC);
            this.initialVolume = volume;
            this.volume = volume;
            this.pitch = 1.0F;
            this.looping = false;
            this.relative = true;
            this.attenuation = AttenuationType.NONE;
        }

        @Override
        public void tick() {
            if (fading) {
                this.volume = this.initialVolume * Math.max(0.0F, 1.0F - ++fadeTicks / (float) FADE_TICKS);
                if (fadeTicks >= FADE_TICKS) this.stop();
            }
        }
    }
}
