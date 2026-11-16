package net.kazi.kazimod.abilities.YagiAwakening;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.kazi.kazimod.init.KaziPacketHandler;
import net.kazi.kazimod.network.HyperGonerMusicPacket;
import net.kazi.kazimod.network.HyperGonerMusicPacket.Action;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.network.PacketDistributor;

/** A full song has its own lifetime, independent of the cutscene and awakening eligibility. */
@Mod.EventBusSubscriber(modid = "kazimod")
public final class HyperGonerMusic {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final double AUDIBLE_RADIUS = 100;
    // The supplied track is 181.32 seconds. Only retire bookkeeping here; never cut off audio.
    private static final int RETIRE_TICKS = 4500;

    private HyperGonerMusic() { }

    public static void start(PlayerEntity caster) {
        if (!(caster.level instanceof ServerWorld) || !caster.isAlive()
                || SESSIONS.containsKey(caster.getUUID())) return;
        Session session = new Session(caster);
        for (ServerPlayerEntity listener : ((ServerWorld) caster.level).players()) {
            if (listener.distanceToSqr(caster) <= AUDIBLE_RADIUS * AUDIBLE_RADIUS)
                session.listeners.add(listener.getUUID());
        }
        SESSIONS.put(caster.getUUID(), session);
        session.send(Action.START);
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Iterator<Session> iterator = SESSIONS.values().iterator();
        while (iterator.hasNext()) {
            Session session = iterator.next();
            // Entity removal also occurs on logout/dimension changes; it does not mean death.
            if (session.caster.getHealth() <= 0) {
                session.send(Action.STOP);
                iterator.remove();
            } else if (++session.age > RETIRE_TICKS) {
                iterator.remove();
            } else if (session.age % 5 == 0) {
                PlayerEntity current = session.server.getPlayerList().getPlayer(session.caster.getUUID());
                if (current != null) session.caster = current;
                session.updatePosition();
                session.send(Action.POSITION);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        if (event.getEntityLiving().level.isClientSide || event.isCanceled()) return;
        Session session = SESSIONS.remove(event.getEntityLiving().getUUID());
        if (session != null) session.send(Action.STOP);
    }

    @SubscribeEvent public static void stopped(FMLServerStoppedEvent event) { SESSIONS.clear(); }

    private static final class Session {
        final UUID id = UUID.randomUUID();
        final MinecraftServer server;
        final Set<UUID> listeners = new HashSet<>();
        PlayerEntity caster;
        ResourceLocation dimension;
        Vector3d position;
        int age;

        Session(PlayerEntity caster) {
            this.caster = caster;
            this.server = ((ServerWorld) caster.level).getServer();
            updatePosition();
        }

        void updatePosition() {
            dimension = caster.level.dimension().location();
            position = caster.position();
        }

        void send(Action action) {
            HyperGonerMusicPacket packet = new HyperGonerMusicPacket(id, caster.getUUID(), dimension, position, action);
            for (UUID listener : listeners) {
                ServerPlayerEntity player = server.getPlayerList().getPlayer(listener);
                if (player != null)
                    KaziPacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        }
    }
}
