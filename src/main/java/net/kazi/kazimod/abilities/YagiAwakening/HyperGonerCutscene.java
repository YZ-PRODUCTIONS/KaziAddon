package net.kazi.kazimod.abilities.YagiAwakening;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsCutsceneCooldown;
import net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsCutscenes;
import net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsSequence;
import net.kazi.kazimod.abilities.cinematics.AwakeningCutsceneType;
import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

/** Awakening health reaction, independent of which Goat moves are bound to the hotbar. */
@Mod.EventBusSubscriber(modid = "kazimod")
public final class HyperGonerCutscene {
    private static final AwakeningCutsceneType TYPE = AwakeningCutsceneType.HYPER_GONER;
    private static final String NEXT_TICK = "kazimod:hyper_goner_next_cutscene";
    private static final Map<UUID, State> STATES = new HashMap<>();
    private HyperGonerCutscene() { }

    /** Reset only the saved replay timer, matching the other awakening cutscenes. */
    public static void resetCutsceneCooldown(LivingEntity user) {
        if (user.level.isClientSide) return;
        CompoundNBT data = user.getPersistentData();
        if (user instanceof PlayerEntity) data = data.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
        data.remove(NEXT_TICK);
    }

    public static boolean canPlay(LivingEntity user) {
        return canContinue(user) && user.getHealth() <= user.getMaxHealth() * .5F;
    }

    public static boolean canContinue(LivingEntity user) {
        return user != null && !user.removed && user.isAlive() && !user.isSpectator()
                && Float.isFinite(user.getHealth()) && Float.isFinite(user.getMaxHealth())
                && user.getHealth() > 0 && user.getMaxHealth() > 0
                && YagiAwakeningSupport.canUnlock(user)
                && !user.hasEffect(ModEffects.ABILITY_OFF.get())
                && !user.hasEffect(KaziEffects.IMAGINE_BROKEN.get())
                && !user.hasEffect(KaziEffects.LOST_WORKS_FRUIT_LOCK.get())
                && !AbilityHelper.isWeakenedByKairosekiOrWater(user);
    }

    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        PlayerEntity player = event.player;
        if (event.phase != TickEvent.Phase.END || !(player.level instanceof ServerWorld)) return;
        if (player.removed || !player.isAlive() || !YagiAwakeningSupport.canUnlock(player)) {
            clear(player);
            return;
        }
        State state = STATES.computeIfAbsent(player.getUUID(), ignored -> new State(player));
        if (state.player != player) {
            clear(state.player);
            state = new State(player);
            STATES.put(player.getUUID(), state);
        }
        if (state.world != player.level) {
            InnerDragonsCutscenes.cancel(player, TYPE);
            state.sequence.stop();
            state.world = player.level;
        }
        boolean eligible = canContinue(player);
        boolean started = state.sequence.update(player.getHealth(), player.getMaxHealth(),
                player.level.getGameTime(), eligible);
        if (!eligible) InnerDragonsCutscenes.cancel(player, TYPE);
        else if (started) {
            long now = ((ServerWorld) player.level).getServer().overworld().getGameTime();
            CompoundNBT root = player.getPersistentData();
            if (!root.contains(PlayerEntity.PERSISTED_NBT_TAG, 10))
                root.put(PlayerEntity.PERSISTED_NBT_TAG, new CompoundNBT());
            CompoundNBT data = root.getCompound(PlayerEntity.PERSISTED_NBT_TAG);
            if (InnerDragonsCutsceneCooldown.ready(now, data.getLong(NEXT_TICK))) {
                data.putLong(NEXT_TICK, InnerDragonsCutsceneCooldown.nextAllowedTick(now));
                InnerDragonsCutscenes.start(player, state.sequence.startTick(), TYPE);
                HyperGonerMusic.start(player);
            } else {
                // Consume this health crossing; do not queue a surprise replay after ten minutes.
                state.sequence.stop();
            }
        }
    }

    private static void clear(PlayerEntity player) {
        State state = STATES.get(player.getUUID());
        if (state != null && state.player == player) STATES.remove(player.getUUID());
        InnerDragonsCutscenes.cancel(player, TYPE);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { clear(event.getPlayer()); }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) { clear(event.getOriginal()); }
    @SubscribeEvent public static void stopped(FMLServerStoppedEvent event) { STATES.clear(); }

    private static final class State {
        final PlayerEntity player;
        World world;
        final InnerDragonsSequence sequence = new InnerDragonsSequence(.5, true, TYPE.durationTicks);
        State(PlayerEntity player) { this.player = player; this.world = player.level; }
    }
}
