package net.minecraft.entity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.World;
public class LivingEntity extends Entity {
    public boolean alive = true, hostile = true, invulnerable;
    public float health = 100, maxHealth = 100;
    public net.minecraft.util.math.vector.Vector3d aimPoint;
    public float getHealth() { return health; }
    public float getMaxHealth() { return maxHealth; }
    public int damageAttempts, successfulHits;
    public final List<Float> damageAmounts = new ArrayList<>();
    public final List<Long> hitTimes = new ArrayList<>();
    public LivingEntity(World world) { super(new EntityType<LivingEntity>(), world); }
    @Override public boolean isAlive() { return alive && !removed; }
}
