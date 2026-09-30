package net.kazi.kazimod.events;

import java.util.Map;
import java.util.WeakHashMap;
import net.kazi.kazimod.entities.EnteiBlastEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.mera.DaiEnkaiEnteiProjectile;

@Mod.EventBusSubscriber(modid = "kazimod")
public final class EnteiVisualEvents {
    private static final Map<DaiEnkaiEnteiProjectile, Impact> PROJECTILES = new WeakHashMap<>();

    private EnteiVisualEvents() { }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void joined(EntityJoinWorldEvent event) {
        if (event.getWorld().isClientSide || !(event.getEntity() instanceof DaiEnkaiEnteiProjectile)) return;
        DaiEnkaiEnteiProjectile projectile = (DaiEnkaiEnteiProjectile) event.getEntity();
        if (PROJECTILES.containsKey(projectile)) return;
        Impact visual = new Impact();
        PROJECTILES.put(projectile, visual);
        AbilityProjectileEntity.IOnBlockImpact original = projectile.onBlockImpactEvent;
        projectile.onBlockImpactEvent = hit -> {
            visual.show(projectile, hit);
            original.onImpact(hit);
        };
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void explosion(ExplosionEvent.Start event) {
        if (event.getWorld().isClientSide || !(event.getExplosion() instanceof ExplosionAbility)) return;
        ExplosionAbility explosion = (ExplosionAbility) event.getExplosion();
        Entity owner = explosion.getExploder();
        BlockPos center = new BlockPos(explosion.getPosition());
        // The base ability's hold-timeout path calls the projectile method directly, not its callback.
        // Observe only a matching Entei explosion; never cancel or modify the explosion.
        for (Map.Entry<DaiEnkaiEnteiProjectile, Impact> entry : PROJECTILES.entrySet()) {
            DaiEnkaiEnteiProjectile projectile = entry.getKey();
            if (entry.getValue().shown || !projectile.isAlive() || projectile.level != event.getWorld()
                    || projectile.getThrower() != owner || !projectile.blockPosition().equals(center)
                    || Math.abs(explosion.getStaticDamage() - projectile.getSize() * 2.0F) > 0.001D) continue;
            entry.getValue().show(projectile, center);
            break;
        }
    }

    private static final class Impact {
        boolean shown;
        void show(DaiEnkaiEnteiProjectile projectile, BlockPos hit) {
            if (!shown) {
                shown = true;
                EnteiBlastEntity.spawn(projectile, hit);
            }
        }
    }
}
