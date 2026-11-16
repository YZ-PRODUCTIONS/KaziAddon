package net.minecraft.util.math;
import net.minecraft.util.math.vector.Vector3d;
public final class AxisAlignedBB {
    public final double minX, minY, minZ, maxX, maxY, maxZ;
    public AxisAlignedBB(double x0, double y0, double z0, double x1, double y1, double z1) {
        minX = x0; minY = y0; minZ = z0; maxX = x1; maxY = y1; maxZ = z1;
    }
    public boolean intersects(AxisAlignedBB other) {
        return maxX >= other.minX && minX <= other.maxX && maxY >= other.minY && minY <= other.maxY
                && maxZ >= other.minZ && minZ <= other.maxZ;
    }
    public Vector3d getCenter() { return new Vector3d((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2); }
}
