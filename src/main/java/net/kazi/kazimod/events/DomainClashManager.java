package net.kazi.kazimod.events;

import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import net.kazi.kazimod.abilities.KamaRework.DomainExpansionMalevolentShrine;
import net.kazi.kazimod.abilities.Koku.DomainExpansionInfiniteVoidAbility;
import net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsCutscenes;
import net.kazi.kazimod.abilities.cinematics.AwakeningCutsceneType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;

@Mod.EventBusSubscriber(modid = "kazimod")
public final class DomainClashManager {
    private static final int DUEL_TICKS = 600;
    private static final Map<UUID, Duel> ACTIVE = new ConcurrentHashMap<>();
    private DomainClashManager() {}

    public static boolean isInClash(UUID id) { return ACTIVE.containsKey(id); }

    public static boolean startClash(LivingEntity gojo, DomainExpansionInfiniteVoidAbility voidCast,
            LivingEntity sukuna, DomainExpansionMalevolentShrine shrineCast) {
        if (!(gojo.level instanceof ServerWorld) || gojo.level != sukuna.level || gojo == sukuna
                || isInClash(gojo.getUUID()) || isInClash(sukuna.getUUID())
                || InnerDragonsCutscenes.isProtected(gojo) || InnerDragonsCutscenes.isProtected(sukuna)) return false;
        Duel duel = new Duel(gojo, voidCast, sukuna, shrineCast);
        // Both charge-end callbacks must see the clash before either charge is stopped.
        ACTIVE.put(gojo.getUUID(), duel);
        ACTIVE.put(sukuna.getUUID(), duel);
        voidCast.stopChargingNoCD(gojo);
        shrineCast.stopChargingNoCD(sukuna);
        voidCast.startCooldownForClash(gojo);
        shrineCast.startCooldownForClash(sukuna);
        shrineCast.startClashVisuals(sukuna, gojo);
        voidCast.startClashVisuals(gojo, sukuna);
        InnerDragonsCutscenes.startDomainClash(gojo, sukuna);
        return true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntityLiving();
        if (target.level.isClientSide) return;
        Duel duel = ACTIVE.get(target.getUUID());
        if (duel == null || duel.world != target.level || duel.world.getGameTime() < duel.fightAt) return;
        if (target == duel.gojo) duel.voidDamage += event.getAmount();
        else if (target == duel.sukuna) duel.shrineDamage += event.getAmount();
    }

    public static void tick() {
        if (ACTIVE.isEmpty()) return;
        for (Duel duel : new HashSet<>(ACTIVE.values())) {
            long now = duel.world.getGameTime();
            if (!duel.present(duel.gojo) || !duel.present(duel.sukuna)) {
                duel.finish(false);
            } else if (now >= duel.fightAt) {
                if (!duel.announced) {
                    duel.announced = true;
                    duel.ready(duel.gojo);
                    duel.ready(duel.sukuna);
                }
                if (duel.voidDamage >= duel.voidLimit || duel.shrineDamage >= duel.shrineLimit
                        || now >= duel.fightAt + DUEL_TICKS) duel.finish(true);
            }
        }
    }

    public static void abort(LivingEntity entity) {
        if (entity.level.isClientSide) return;
        Duel duel = ACTIVE.get(entity.getUUID());
        if (duel != null && (duel.gojo == entity || duel.sukuna == entity)) duel.finish(false);
    }

    @SubscribeEvent public static void died(LivingDeathEvent e) { abort(e.getEntityLiving()); }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) { abort(e.getPlayer()); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) { abort(e.getPlayer()); }
    @SubscribeEvent public static void stopped(FMLServerStoppingEvent e) {
        for (Duel duel : new HashSet<>(ACTIVE.values())) duel.finish(false);
    }

    private static final class Duel {
        final ServerWorld world;
        final LivingEntity gojo, sukuna;
        final DomainExpansionInfiniteVoidAbility voidCast;
        final DomainExpansionMalevolentShrine shrineCast;
        final long fightAt;
        final float voidLimit, shrineLimit;
        float voidDamage, shrineDamage;
        boolean announced;
        Duel(LivingEntity gojo, DomainExpansionInfiniteVoidAbility voidCast, LivingEntity sukuna,
                DomainExpansionMalevolentShrine shrineCast) {
            this.world = (ServerWorld) gojo.level;
            this.gojo = gojo; this.sukuna = sukuna;
            this.voidCast = voidCast; this.shrineCast = shrineCast;
            this.voidLimit = gojo.getMaxHealth() * .30F;
            this.shrineLimit = sukuna.getMaxHealth() * .30F;
            this.fightAt = world.getGameTime() + AwakeningCutsceneType.DOMAIN_CLASH.durationTicks;
        }
        boolean present(LivingEntity entity) {
            return !entity.removed && entity.isAlive() && entity.level == world;
        }
        void ready(LivingEntity entity) {
            if (entity instanceof ServerPlayerEntity)
                ((ServerPlayerEntity)entity).displayClientMessage(new StringTextComponent(
                        TextFormatting.GOLD + "Domain clash: fight!"), true);
        }
        void finish(boolean resolve) {
            ACTIVE.remove(gojo.getUUID());
            ACTIVE.remove(sukuna.getUUID());
            InnerDragonsCutscenes.cancel(gojo, AwakeningCutsceneType.DOMAIN_CLASH);
            if (!resolve) {
                voidCast.cleanupClash(gojo);
                shrineCast.cleanupClash(sukuna);
                return;
            }
            boolean a = voidDamage >= voidLimit, b = shrineDamage >= shrineLimit;
            // As before, simultaneous losses and timeouts clear both domain cooldowns.
            if (a && !b) voidCast.resolveClashLoss(gojo); else voidCast.resolveClashWin(gojo);
            if (b && !a) shrineCast.resolveClashLoss(sukuna); else shrineCast.resolveClashWin(sukuna);
        }
    }
}
