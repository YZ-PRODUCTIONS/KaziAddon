package net.kazi.kazimod.abilities.GoruRework;

import java.util.*;
import net.kazi.kazimod.init.KaziPacketHandler;
import net.kazi.kazimod.network.GoruCutsceneMusicPacket;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.network.PacketDistributor;

/** Audio outlives the cinematic and health threshold. Each activation has its own audience. */
@Mod.EventBusSubscriber(modid = "kazimod")
public final class GoruCutsceneMusic {
    private static final List<Session> SESSIONS = new ArrayList<>();
    private GoruCutsceneMusic() { }
    public static void start(LivingEntity caster) {
        if (!(caster.level instanceof ServerWorld)) return;
        Session session = new Session(caster, (ServerWorld) caster.level);
        for (ServerPlayerEntity player : session.world.players()) {
            if (player.distanceToSqr(caster) <= 100 * 100) session.listeners.add(player.getUUID());
        }
        SESSIONS.add(session);
        session.send(0);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Iterator<Session> iterator = SESSIONS.iterator();
        while (iterator.hasNext()) {
            Session session = iterator.next();
            if (!session.caster.isAlive()) {
                session.send(2); iterator.remove();
            } else if (++session.age > 2040) {
                // Full clip is 97.72 seconds. This only retires bookkeeping after natural completion.
                iterator.remove();
            } else if (session.age % 5 == 0) session.send(1);
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        SESSIONS.removeIf(session -> {
            if (session.caster != event.getEntityLiving()) return false;
            session.send(2); return true;
        });
    }
    @SubscribeEvent public static void stopped(FMLServerStoppedEvent event) { SESSIONS.clear(); }
    private static final class Session {
        final UUID id = UUID.randomUUID();
        final LivingEntity caster;
        final ServerWorld world;
        final List<UUID> listeners = new ArrayList<>();
        int age;
        Session(LivingEntity caster, ServerWorld world) { this.caster = caster; this.world = world; }
        void send(int action) {
            GoruCutsceneMusicPacket packet = new GoruCutsceneMusicPacket(id, caster.getUUID(), caster.position(), action);
            for (UUID listener : listeners) {
                ServerPlayerEntity player = world.getServer().getPlayerList().getPlayer(listener);
                if (player != null) KaziPacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        }
    }
}
