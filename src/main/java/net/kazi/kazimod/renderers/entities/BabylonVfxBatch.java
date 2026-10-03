package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import java.util.Arrays;
import net.kazi.kazimod.models.abilities.BabylonFlightMesh;
import net.kazi.kazimod.models.abilities.BabylonPortalMesh;
import net.kazi.kazimod.models.abilities.BabylonVisualTimeline;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;

/** Reusable QUAD storage. Item rendering may flush buffers, so submit these afterwards. */
final class BabylonVfxBatch implements BabylonPortalMesh.Sink, BabylonFlightMesh.Sink {
    private float[] vertices;
    private int size;
    private double ox, oy, oz, rx = 1, rz, ux, uy = 1, uz, fx, fy, fz = 1;

    BabylonVfxBatch(int capacity) { vertices = new float[Math.max(4, capacity) * 7]; }

    void clear() {
        size = 0;
        ox = oy = oz = rz = ux = uz = fx = fy = 0;
        rx = uy = fz = 1;
    }

    /** Bake local gate coordinates into the anchored entity's axes. */
    void setFrame(Vector3d offset, Vector3d forward) {
        double yaw = Math.atan2(forward.x, forward.z);
        rx = Math.cos(yaw); rz = -Math.sin(yaw);
        fx = forward.x; fy = forward.y; fz = forward.z;
        ux = fy * rz; uy = fz * rx - fx * rz; uz = -fy * rx;
        ox = offset.x; oy = offset.y; oz = offset.z;
    }

    @Override public void vertex(double x, double y, double z, float r, float g, float b, float a) {
        if (size + 7 > vertices.length) vertices = Arrays.copyOf(vertices, vertices.length * 2);
        vertices[size++] = (float) (ox + x * rx + y * ux + z * fx);
        vertices[size++] = (float) (oy + y * uy + z * fy);
        vertices[size++] = (float) (oz + x * rz + y * uz + z * fz);
        vertices[size++] = r; vertices[size++] = g; vertices[size++] = b; vertices[size++] = a;
    }

    void draw(Matrix4f pose, IVertexBuilder out, Vector3d localCamera) {
        for (int quad = 0; quad + 27 < size; quad += 28) {
            if (vertices[quad + 6] + vertices[quad + 13] + vertices[quad + 20] + vertices[quad + 27] < .0001F) continue;
            for (int i = quad; i < quad + 28; i += 7) {
                double dx = vertices[i] - localCamera.x, dy = vertices[i + 1] - localCamera.y,
                        dz = vertices[i + 2] - localCamera.z;
                double distanceSquared = dx * dx + dy * dy + dz * dz;
                float visibility = distanceSquared >= 2.25 ? 1F
                        : BabylonVisualTimeline.smooth((float) ((Math.sqrt(distanceSquared) - .3) / 1.2));
                out.vertex(pose, vertices[i], vertices[i + 1], vertices[i + 2])
                        .color(vertices[i + 3], vertices[i + 4], vertices[i + 5], vertices[i + 6] * visibility).endVertex();
            }
        }
    }

    static Vector3d localCamera(Entity entity, EntityRendererManager renderer, float partial, Vector3d direction) {
        Vector3d origin = new Vector3d(MathHelper.lerp(partial, entity.xOld, entity.getX()),
                MathHelper.lerp(partial, entity.yOld, entity.getY()),
                MathHelper.lerp(partial, entity.zOld, entity.getZ()));
        Vector3d delta = renderer.camera.getPosition().subtract(origin);
        Vector3d forward = direction.lengthSqr() < .000001 ? new Vector3d(0, 0, 1) : direction.normalize();
        double yaw = Math.atan2(forward.x, forward.z);
        Vector3d right = new Vector3d(Math.cos(yaw), 0, -Math.sin(yaw));
        Vector3d up = forward.cross(right);
        return new Vector3d(delta.dot(right), delta.dot(up), delta.dot(forward));
    }
}
