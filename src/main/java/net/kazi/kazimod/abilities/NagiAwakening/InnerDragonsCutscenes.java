package net.kazi.kazimod.abilities.NagiAwakening;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kazi.kazimod.abilities.cinematics.AwakeningCutsceneType;
import net.kazi.kazimod.abilities.cinematics.ChaosBeamFinaleLift;
import net.kazi.kazimod.abilities.YagiAwakening.ChaosBeamRecoil;
import net.kazi.kazimod.abilities.YagiAwakening.ChaosBeamHealing;
import net.kazi.kazimod.init.KaziPacketHandler;
import net.kazi.kazimod.network.InnerDragonsCutscenePacket;
import net.kazi.kazimod.petal.PetalField;
import net.kazi.kazimod.saiyan.SaiyanFeatures;
import net.kazi.kazimod.saiyan.SaiyanState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import xyz.pixelatedw.mineminenomi.api.events.WyLivingAttackEvent;
import xyz.pixelatedw.mineminenomi.api.events.WyLivingDamageEvent;
import xyz.pixelatedw.mineminenomi.api.events.WyLivingHurtEvent;

/** Server-owned audience snapshots ensure every original viewer also receives early cleanup. */
@Mod.EventBusSubscriber(modid = "kazimod")
public final class InnerDragonsCutscenes {
    public static final double VIEW_RADIUS = 100;
    private static final long INSTINCT_REPLAY_TICKS = 20 * 60 * 20;
    private static final String NEXT_INSTINCT_CUTSCENE = "KaziNextInstinctCutscene";
    private static final Map<UUID, Scene> SCENES = new HashMap<>();
    private static final Map<LivingEntity, ReturnProtection> RETURN_PROTECTION = new IdentityHashMap<>();
    private static final List<FinaleEffects> PENDING_EFFECTS = new ArrayList<>();

    private InnerDragonsCutscenes() { }

    public static void start(LivingEntity user, long startTick) {
        start(user, startTick, false);
    }

    public static void start(LivingEntity user, long startTick, boolean goru) {
        start(user, startTick, goru ? AwakeningCutsceneType.EA_DRAW : AwakeningCutsceneType.INNER_DRAGONS);
    }

    public static void start(LivingEntity user, long startTick, AwakeningCutsceneType type) {
        start(user, startTick, type, null, null);
    }

    public static void startDomainClash(LivingEntity gojo, LivingEntity sukuna) {
        start(gojo, gojo.level.getGameTime(), AwakeningCutsceneType.DOMAIN_CLASH, sukuna, null);
    }

    public static void startKageyoshi(LivingEntity user, PetalField release) {
        if (!(user instanceof ServerPlayerEntity) || release == null || release.removed
                || release.level != user.level || release.kind() != 6) return;
        start(user, user.level.getGameTime(), AwakeningCutsceneType.SENBONZAKURA_KAGEYOSHI, null, release);
    }

    private static void start(LivingEntity user, long startTick, AwakeningCutsceneType type, LivingEntity opponent, PetalField release) {
        if (!(user.level instanceof ServerWorld)) return;
        ServerWorld world = (ServerWorld) user.level;
        if ((type == AwakeningCutsceneType.DOMAIN_CLASH) != (opponent != null)) return;
        if ((type == AwakeningCutsceneType.SENBONZAKURA_KAGEYOSHI) != (release != null)) return;
        Scene previous = SCENES.get(user.getUUID());
        if (type.cameraOnly() && previous != null && previous.isActive()) return;
        if (type != AwakeningCutsceneType.DOMAIN_CLASH && isClashLocked(user)) return;
        long replayClock = 0;
        CompoundNBT instinctHistory = null;
        if (type == AwakeningCutsceneType.ULTRA_INSTINCT) {
            if (!(user instanceof ServerPlayerEntity)) return;
            replayClock = world.getServer().overworld().getGameTime();
            // Forge copies this tag on respawn. Keep it separate from the fruit's runtime state.
            instinctHistory = user.getPersistentData().getCompound(PlayerEntity.PERSISTED_NBT_TAG);
            if (replayClock < instinctHistory.getLong(NEXT_INSTINCT_CUTSCENE)) return;
        }
        cancel(user);
        Scene scene = new Scene(user, world, startTick, type, release);
        if (instinctHistory != null && !scene.isActive()) return;
        scene.opponent = opponent;
        SCENES.put(user.getUUID(), scene);
        if (type == AwakeningCutsceneType.EA_DRAW)
            net.kazi.kazimod.abilities.GoruRework.GoruCutsceneMusic.start(user);
        InnerDragonsCutscenePacket packet = scene.packet(false);
        for (ServerPlayerEntity viewer : scene.world.players()) {
            if (viewer.isAlive() && !viewer.removed && (viewer.distanceToSqr(user) <= VIEW_RADIUS * VIEW_RADIUS
                    || opponent != null && viewer.distanceToSqr(opponent) <= VIEW_RADIUS * VIEW_RADIUS)
                    && available(viewer, type)) {
                // The client keeps its first scene. Do not grant an extra protection window
                // for a second scene whose start packet it would ignore.
                scene.viewers.put(viewer.getUUID(), viewer);
                send(viewer, packet);
            }
        }
        if (instinctHistory != null) {
            if (scene.viewers.isEmpty()) {
                SCENES.remove(user.getUUID());
                return;
            }
            instinctHistory.putLong(NEXT_INSTINCT_CUTSCENE, replayClock + INSTINCT_REPLAY_TICKS);
            user.getPersistentData().put(PlayerEntity.PERSISTED_NBT_TAG, instinctHistory);
        }
        if (opponent != null) {
            scene.pin(user);
            scene.pin(opponent);
            for (LivingEntity viewer : scene.viewers.values()) scene.pin(viewer);
        }
    }

    public static void cancel(LivingEntity user) {
        Scene scene = SCENES.remove(user.getUUID());
        if (scene != null) scene.close();
    }

    public static void cancel(LivingEntity user, AwakeningCutsceneType type) {
        Scene scene = SCENES.get(user.getUUID());
        if (scene != null && scene.user == user && scene.type == type) cancel(user);
    }

    /** Timed membership, not an invulnerability flag: existing creative/mod protection is untouched. */
    public static boolean isProtected(LivingEntity entity) {
        if (entity == null || entity.level.isClientSide || entity.removed || !entity.isAlive()) return false;
        ReturnProtection returning = RETURN_PROTECTION.get(entity);
        if (returning != null && returning.active(entity)) return true;
        for (Scene scene : SCENES.values()) {
            if (scene.protectsDamage() && entity.level == scene.world
                    && (entity == scene.user || entity == scene.opponent || scene.viewers.get(entity.getUUID()) == entity)) return true;
        }
        return false;
    }

    private static boolean available(ServerPlayerEntity viewer, AwakeningCutsceneType next) {
        for (Scene scene : SCENES.values()) {
            if (!scene.isActive() || viewer.level != scene.world || scene.viewers.get(viewer.getUUID()) != viewer) continue;
            // Cosmetic cameras yield to clashes and protected awakenings, but not to one another.
            if (!scene.type.cameraOnly() || next.cameraOnly()) return false;
            scene.viewers.remove(viewer.getUUID());
            send(viewer, scene.packet(true));
        }
        return true;
    }

    public static boolean isClashLocked(Entity entity) {
        if (entity == null || entity.level.isClientSide || SCENES.isEmpty()) return false;
        for (Scene scene : SCENES.values()) {
            if (scene.opponent != null && scene.pins.containsKey(entity)
                    && entity.level == scene.world && scene.isActive()) return true;
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void ability(xyz.pixelatedw.mineminenomi.api.events.ability.AbilityUseEvent.Pre event) {
        if (isClashLocked(event.getEntityLiving())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interact(net.minecraftforge.event.entity.player.PlayerInteractEvent event) {
        if (event.isCancelable() && isClashLocked(event.getPlayer())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breakBlock(net.minecraftforge.event.world.BlockEvent.BreakEvent event) {
        if (isClashLocked(event.getPlayer())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void nativeAttack(WyLivingAttackEvent event) {
        // MMNM can apply damage-triggered effects before entering vanilla's damage path.
        if (isProtected(event.getEntityLiving()) || isClashLocked(event.getSource().getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void nativeHurt(WyLivingHurtEvent event) {
        if (isProtected(event.getEntityLiving())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void nativeDamage(WyLivingDamageEvent event) {
        if (isProtected(event.getEntityLiving())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(LivingAttackEvent event) {
        if (isProtected(event.getEntityLiving()) || isClashLocked(event.getSource().getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hurt(LivingHurtEvent event) {
        if (isProtected(event.getEntityLiving())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damage(LivingDamageEvent event) {
        // Final fallback also blocks armor-piercing/true damage entering after attack handling.
        if (isProtected(event.getEntityLiving())) event.setCanceled(true);
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        RETURN_PROTECTION.entrySet().removeIf(entry -> !entry.getValue().active(entry.getKey()));
        Iterator<Scene> iterator = SCENES.values().iterator();
        while (iterator.hasNext()) {
            Scene scene = iterator.next();
            long age = scene.world.getGameTime() - scene.start;
            if (scene.user.removed || !scene.user.isAlive() || scene.user.level != scene.world
                    || !scene.eligible()) {
                scene.close();
                iterator.remove();
            } else if (age < 0 || age >= scene.type.durationTicks) {
                // Clients end at the same game-time boundary; don't truncate their last rendered frame.
                if (age < 0) scene.close();
                else scene.complete();
                iterator.remove();
            } else {
                if (scene.lift != null) scene.lift.tick(age);
                // A returning/respawned player must not inherit an old body's scene immunity.
                scene.viewers.values().removeIf(viewer -> viewer.removed || !viewer.isAlive()
                        || viewer.level != scene.world
                        || scene.world.getServer().getPlayerList().getPlayer(viewer.getUUID()) != viewer);
                if (scene.opponent != null) scene.holdPositions();
            }
        }
        applyFinishedEffects();
    }

    private static void applyFinishedEffects() {
        if (PENDING_EFFECTS.isEmpty()) return;
        List<LivingEntity> ready = new ArrayList<>();
        Iterator<FinaleEffects> iterator = PENDING_EFFECTS.iterator();
        while (iterator.hasNext()) {
            FinaleEffects effects = iterator.next();
            if (effects.caster.removed || !effects.caster.isAlive() || effects.caster.level != effects.world) {
                iterator.remove();
            } else if (effects.world.getGameTime() >= effects.after && !isProtected(effects.caster)) {
                iterator.remove();
                ready.add(effects.caster);
            }
        }
        // Consume each completed cast before callbacks, and heal allies before potentially lethal recoil.
        for (LivingEntity caster : ready) {
            ChaosBeamHealing.healAllies(caster);
            ChaosBeamRecoil.hurtCaster(caster);
        }
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        cancel(event.getPlayer());
        removeViewer(event.getPlayer());
    }

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        ChaosBeamFinaleLift.recover(event.getPlayer());
    }

    @SubscribeEvent public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        cancel(event.getPlayer());
        removeViewer(event.getPlayer());
    }

    @SubscribeEvent public static void cloned(PlayerEvent.Clone event) {
        cancel(event.getOriginal());
        removeViewer(event.getOriginal());
    }

    private static void removeViewer(LivingEntity entity) {
        RETURN_PROTECTION.remove(entity);
        PENDING_EFFECTS.removeIf(effects -> effects.caster == entity);
        for (Scene scene : SCENES.values()) {
            if (scene.viewers.get(entity.getUUID()) == entity) scene.viewers.remove(entity.getUUID());
            scene.pins.remove(entity);
        }
    }

    @SubscribeEvent public static void stopped(FMLServerStoppedEvent event) {
        for (Scene scene : SCENES.values()) if (scene.lift != null) scene.lift.finish(false);
        SCENES.clear();
        RETURN_PROTECTION.clear();
        PENDING_EFFECTS.clear();
    }

    private static void send(ServerPlayerEntity player, InnerDragonsCutscenePacket packet) {
        KaziPacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    private static final class FinaleEffects {
        final LivingEntity caster;
        final ServerWorld world;
        final long after;

        FinaleEffects(LivingEntity caster, ServerWorld world, long after) {
            this.caster = caster;
            this.world = world;
            this.after = after;
        }
    }

    /** Survives the caster starting another scene or departing while the original viewers fade back. */
    private static final class ReturnProtection {
        final ServerWorld world;
        final long until;

        ReturnProtection(ServerWorld world, long until) {
            this.world = world;
            this.until = until;
        }

        boolean active(LivingEntity entity) {
            return entity.level == world && !entity.removed && entity.isAlive() && world.getGameTime() < until;
        }
    }

    private static final class Scene {
        final LivingEntity user;
        final ServerWorld world;
        final long start;
        final AwakeningCutsceneType type;
        final PetalField release;
        final ChaosBeamFinaleLift lift;
        final Map<UUID, ServerPlayerEntity> viewers = new HashMap<>();
        final Map<LivingEntity, Vector3d> pins = new IdentityHashMap<>();
        LivingEntity opponent;
        boolean completed;

        Scene(LivingEntity user, ServerWorld world, long start, AwakeningCutsceneType type, PetalField release) {
            this.type = type;
            this.release = release;
            this.user = user;
            this.world = world;
            this.start = start;
            this.lift = type == AwakeningCutsceneType.CHAOS_BEAM_FINALE ? new ChaosBeamFinaleLift(user) : null;
        }

        InnerDragonsCutscenePacket packet(boolean cancel) {
            if (release != null) return new InnerDragonsCutscenePacket(user.getId(), user.getUUID(), start,
                    release.position(), release.yaw(), cancel, type, null,
                    new InnerDragonsCutscenePacket.BankaiRelease(release.getId(), profileName(user, "Bankai"), visibleParts(user)));
            InnerDragonsCutscenePacket.ClashPartner clash = opponent == null ? null :
                    new InnerDragonsCutscenePacket.ClashPartner(opponent.getId(), opponent.getUUID(),
                            opponent.position(), profileName(user, "Koku"), profileName(opponent, "Kama"),
                            visibleParts(user), visibleParts(opponent));
            return new InnerDragonsCutscenePacket(user.getId(), user.getUUID(), start,
                    user.position(), clash == null ? user.yBodyRot : user.yRot, cancel, type, clash);
        }

        private static String profileName(LivingEntity actor, String fallback) {
            return actor instanceof ServerPlayerEntity ? ((ServerPlayerEntity)actor).getGameProfile().getName() : fallback;
        }

        private static int visibleParts(LivingEntity actor) {
            if (!(actor instanceof ServerPlayerEntity)) return 127;
            return actor.getEntityData().get(net.kazi.kazimod.mixin.ClashSkinPartsAccessor.kazi$skinParts()) & 127;
        }

        boolean isActive() {
            long age = world.getGameTime() - start;
            return age >= 0 && age < type.durationTicks
                    && !user.removed && user.isAlive() && user.level == world
                    && eligible();
        }

        boolean protectsDamage() {
            if (type.cameraOnly()) return false;
            long age = world.getGameTime() - start;
            // Cover the boundary before END-tick cleanup transfers the audience to RETURN_PROTECTION.
            return isActive() || age >= type.durationTicks && age < type.protectionTicks;
        }

        void complete() {
            if (completed) return;
            completed = true;
            if (lift != null) lift.finish(true);
            if (type == AwakeningCutsceneType.CHAOS_BEAM_FINALE)
                PENDING_EFFECTS.add(new FinaleEffects(user, world, start + type.protectionTicks));
            if (type.returnFadeTicks == 0) return;
            ReturnProtection protection = new ReturnProtection(world, start + type.protectionTicks);
            protectReturn(user, protection);
            for (ServerPlayerEntity viewer : viewers.values()) protectReturn(viewer, protection);
        }

        private void protectReturn(LivingEntity entity, ReturnProtection protection) {
            if (!protection.active(entity)) return;
            ReturnProtection previous = RETURN_PROTECTION.get(entity);
            if (previous == null || previous.world != world || previous.until < protection.until)
                RETURN_PROTECTION.put(entity, protection);
        }

        boolean eligible() {
            if (release != null) return !release.removed;
            if (type == AwakeningCutsceneType.ULTRA_INSTINCT)
                return SaiyanFeatures.isInstinctTransforming(user) && SaiyanState.get(user).getLong("ActionStart") == start;
            if (type == AwakeningCutsceneType.DOMAIN_CLASH)
                return opponent != null && !opponent.removed && opponent.isAlive() && opponent.level == world;
            if (type == AwakeningCutsceneType.CHAOS_BEAM_FINALE)
                return net.kazi.kazimod.abilities.YagiAwakening.YagiAwakeningSupport.canUnlock(user);
            if (type == AwakeningCutsceneType.HYPER_GONER)
                return net.kazi.kazimod.abilities.YagiAwakening.HyperGonerCutscene.canContinue(user);
            return type == AwakeningCutsceneType.EA_DRAW ? net.kazi.kazimod.abilities.GoruRework.SeriousnessAbility.canContinueDraw(user)
                    : user.isAlive();
        }

        void pin(LivingEntity actor) {
            pins.put(actor, actor.position());
            actor.setDeltaMovement(Vector3d.ZERO);
            actor.fallDistance = 0;
        }

        void holdPositions() {
            pins.forEach((actor, anchor) -> {
                if (actor.level != world || actor.removed || !actor.isAlive()
                        || actor != user && actor != opponent && !viewers.containsKey(actor.getUUID())) return;
                actor.setDeltaMovement(Vector3d.ZERO);
                actor.fallDistance = 0;
                if (actor.position().distanceToSqr(anchor) > .0004)
                    actor.teleportTo(anchor.x, anchor.y, anchor.z);
            });
        }

        void close() {
            if (world.getGameTime() - start >= type.durationTicks) {
                complete();
                return;
            }
            if (lift != null) lift.finish(false);
            InnerDragonsCutscenePacket packet = packet(true);
            for (ServerPlayerEntity viewer : viewers.values()) {
                if (world.getServer().getPlayerList().getPlayer(viewer.getUUID()) == viewer) send(viewer, packet);
            }
        }
    }
}
