package net.minecraft.world.server;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
public final class ServerWorld extends World {
    public Entity getEntity(UUID id) {
        for (Entity entity : entities) if (!entity.removed && entity.getUUID().equals(id)) return entity;
        return null;
    }
}
