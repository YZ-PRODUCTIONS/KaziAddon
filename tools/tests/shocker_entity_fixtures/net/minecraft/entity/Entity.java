package net.minecraft.entity;
import java.util.UUID;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
public class Entity {
    private static int nextId;
    private final int id = ++nextId;
    private final UUID uuid = new UUID(0, id);
    private double x, y, z;
    public final World level;
    protected final EntityDataManager entityData = new EntityDataManager();
    public boolean noPhysics, noCulling, removed;
    public Entity(EntityType<?> type, World world) { level = world; defineSynchedData(); }
    public void tick() { }
    public UUID getUUID() { return uuid; }
    public int getId() { return id; }
    public void setPos(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public Vector3d position() { return new Vector3d(x, y, z); }
    public AxisAlignedBB getBoundingBox() { return new AxisAlignedBB(x - .3, y, z - .3, x + .3, y + 1.8, z + .3); }
    public double distanceToSqr(Entity other) {
        double dx = x - other.x, dy = y - other.y, dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }
    public void setNoGravity(boolean value) { }
    public void remove() { removed = true; }
    public boolean isAlive() { return !removed; }
    protected void defineSynchedData() { }
    protected void readAdditionalSaveData(CompoundNBT tag) { }
    protected void addAdditionalSaveData(CompoundNBT tag) { }
    public boolean isPickable() { return true; }
    public boolean hurt(DamageSource source, float amount) { return false; }
    public boolean shouldRenderAtSqrDistance(double distance) { return true; }
    public IPacket<?> getAddEntityPacket() { return null; }
}
