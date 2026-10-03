package net.kazi.kazimod.effects;

import net.kazi.kazimod.entities.GuraVfxEntity;
import net.minecraft.world.World;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

/** Adapter for the base mod's explosion visual callback; spawns geometry, never particles. */
public class GuraFractureEffect extends ParticleEffect {
    @Override public void spawn(World world, double x, double y, double z, double sx, double sy, double sz) {
        GuraVfxEntity.spawn(world, x, y + .8, z, 5, GuraVfxEntity.AIR);
    }
}
