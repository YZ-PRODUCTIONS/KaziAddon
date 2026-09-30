package net.kazi.kazimod.models.abilities;

/** Fixed sphere topology and reusable animation scratch, independent of Minecraft/client state. */
final class KokuSphereGrid {
    private static final Grid[] GRIDS = new Grid[25];
    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    static {
        for (int latitude : new int[]{4, 6, 8, 12, 16, 24}) GRIDS[latitude] = new Grid(latitude);
    }

    private KokuSphereGrid() { }

    static void render(KokuVfxMesh.Sink out, float radius, float phase, float distortion,
                       int color, float alpha, boolean turbulent, int latitude) {
        Grid grid = GRIDS[latitude];
        Scratch scratch = SCRATCH.get();
        // A shared corner used by four quads is animated once, not four times.
        // All topology/trigonometry independent of phase is cached permanently.
        for (int row = 0; row <= grid.latitude; row++) {
            float lat = grid.lat[row];
            float latFlow = (float) Math.sin(lat * 7.0F + phase) * 2.0F;
            float latBand = (float) Math.cos(lat * 9.0F - phase);
            for (int column = 0; column <= grid.longitude; column++) {
                int index = row * grid.stride + column;
                float lon = grid.lon[column];
                float flow = (float) Math.sin(lon * 6.0F + latFlow - phase * 2.0F) * latBand
                        + (float) Math.sin(lon * 13.0F - lat * 11.0F + phase * 3.0F) * 0.3F;
                scratch.radius[index] = radius * (1.0F + flow * distortion);
                float band = turbulent ? 0.16F + 0.84F * (float) Math.pow(Math.abs(flow) / 1.3F, 2.3D) : 1.0F;
                scratch.alpha[index] = Math.max(0.0F, Math.min(1.0F, alpha * band));
            }
        }
        for (int row = 0; row < grid.latitude; row++) {
            for (int column = 0; column < grid.longitude; column++) {
                int top = row * grid.stride + column;
                int bottom = top + grid.stride;
                emit(out, grid, scratch, top, color);
                emit(out, grid, scratch, bottom, color);
                emit(out, grid, scratch, bottom + 1, color);
                emit(out, grid, scratch, top + 1, color);
            }
        }
    }

    private static void emit(KokuVfxMesh.Sink out, Grid grid, Scratch scratch, int index, int color) {
        float radius = scratch.radius[index];
        out.vertex(grid.x[index] * radius, grid.y[index] * radius, grid.z[index] * radius,
                color, scratch.alpha[index]);
    }

    private static final class Scratch {
        final float[] radius = new float[25 * 49];
        final float[] alpha = new float[25 * 49];
    }

    private static final class Grid {
        final int latitude, longitude, stride;
        final float[] lat, lon, x, y, z;

        Grid(int latitude) {
            this.latitude = latitude;
            this.longitude = latitude * 2;
            this.stride = longitude + 1;
            lat = new float[latitude + 1];
            lon = new float[stride];
            x = new float[(latitude + 1) * stride];
            y = new float[x.length];
            z = new float[x.length];
            for (int column = 0; column <= longitude; column++) {
                lon[column] = (float) Math.PI * 2 * (float) column / (float) longitude;
            }
            for (int row = 0; row <= latitude; row++) {
                lat[row] = -1.5707964F + (float) Math.PI * (float) row / (float) latitude;
                float cosLat = (float) Math.cos(lat[row]);
                float sinLat = (float) Math.sin(lat[row]);
                for (int column = 0; column <= longitude; column++) {
                    int index = row * stride + column;
                    x[index] = cosLat * (float) Math.cos(lon[column]);
                    y[index] = sinLat;
                    z[index] = cosLat * (float) Math.sin(lon[column]);
                }
            }
        }
    }
}
