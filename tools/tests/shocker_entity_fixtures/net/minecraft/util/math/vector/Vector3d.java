package net.minecraft.util.math.vector;
public final class Vector3d {
    public final double x, y, z;
    public Vector3d(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
    public Vector3d add(double x, double y, double z) { return new Vector3d(this.x + x, this.y + y, this.z + z); }
}
