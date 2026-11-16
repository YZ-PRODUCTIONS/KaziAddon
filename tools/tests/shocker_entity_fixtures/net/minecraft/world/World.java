package net.minecraft.world;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
public class World {
    public boolean isClientSide;
    public long gameTime;
    public final List<Entity> entities = new ArrayList<>();
    public final Map<Entity, Long> spawnTimes = new IdentityHashMap<>();
    public final List<String> sounds = new ArrayList<>();
    public long getGameTime() { return gameTime; }
    public boolean addFreshEntity(Entity entity) { entities.add(entity); spawnTimes.put(entity, gameTime); return true; }
    public Entity getEntity(int id) {
        for (Entity entity : entities) if (!entity.removed && entity.getId() == id) return entity;
        return null;
    }
    public <T extends Entity> List<T> getEntitiesOfClass(Class<T> type, AxisAlignedBB area) {
        List<T> result = new ArrayList<>();
        for (Entity entity : entities) if (!entity.removed && type.isInstance(entity) && area.intersects(entity.getBoundingBox())) result.add(type.cast(entity));
        return result;
    }
    public void tickEntities() { for (Entity entity : new ArrayList<>(entities)) if (!entity.removed) entity.tick(); }
}
