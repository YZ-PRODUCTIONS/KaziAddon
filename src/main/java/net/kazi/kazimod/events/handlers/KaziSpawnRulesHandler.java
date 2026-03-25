package net.kazi.kazimod.events.handlers;

import net.minecraft.entity.SpawnReason;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import xyz.pixelatedw.mineminenomi.entities.mobs.OPEntity;

/**
 * Allows /summon and spawn eggs to work on any kazimod OPEntity subclass.
 *
 * Root cause: OPEntity.checkSpawnRules() only bypasses its local-difficulty
 * and position checks when SpawnReason == SPAWNER. The natural spawn handler
 * passes ignoreSpawnRules=true so it never hits this, but /summon passes
 * false, causing checkSpawnRules to run and reject the spawn → "Unable to
 * summon entity". This affects every OPEntity subclass (GojoBoss, Vegapunk, etc).
 *
 * Register in KaziMod constructor:
 *   MinecraftForge.EVENT_BUS.register(new KaziSpawnRulesHandler());
 */
public class KaziSpawnRulesHandler {

    @SubscribeEvent
    public void onCheckSpawn(LivingSpawnEvent.CheckSpawn event) {
        if (!(event.getEntity() instanceof OPEntity)) return;

        // Only override for manual/command spawns - leave natural spawning alone
        SpawnReason reason = event.getSpawnReason();
        if (reason == SpawnReason.COMMAND || reason == SpawnReason.SPAWN_EGG) {
            event.setResult(Event.Result.ALLOW);
        }
    }
}