import net.kazi.kazimod.client.nagi.InnerDragonsArmMesh;

/** Executes the replacement's real mesh; no Minecraft client or inventory is needed. */
public final class InnerDragonsArmMeshTest {
    private static int checks;
    private static final class Capture implements InnerDragonsArmMesh.Sink {
        int count;
        float minX = 10, maxX = -10, minY = 10, maxY = -10, maxZ = -10, maxAlpha;
        public void vertex(float x, float y, float z, int color, float alpha) {
            require(Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z), "non-finite geometry");
            require(Math.abs(x) <= .4F && Math.abs(y) <= .6F && z >= -.3F && z <= 1F, "escaped limb bounds");
            require(alpha > 0 && alpha < 1, "replacement must remain translucent");
            require((color >> 16 & 255) >= 0x90 && (color >> 8 & 255) >= 0xA9
                    && (color & 255) >= 0xBA, "replacement must remain white/pale gray");
            count++;
            minX = Math.min(minX, x); maxX = Math.max(maxX, x);
            minY = Math.min(minY, y); maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z); maxAlpha = Math.max(maxAlpha, alpha);
        }
    }
    public static void main(String[] args) {
        int expected = -1;
        for (int age = 0; age <= 1000; age += 31) for (float attack : new float[]{0, .5F, 1}) {
            Capture out = new Capture();
            InnerDragonsArmMesh.render(out, age, attack, 1);
            if (expected < 0) expected = out.count;
            require(out.count == expected && out.count % 4 == 0, "unstable or incomplete quads");
            require(out.count < 1500, "replacement exceeded its small geometry budget");
            require(out.minX <= -.35F && out.maxX >= .35F && out.maxY >= .49F && out.maxZ >= .95F,
                    "horns, cheek spines or muzzle missing");
        }
        Capture closed = new Capture(), open = new Capture(), fading = new Capture();
        InnerDragonsArmMesh.render(closed, 0, 0, 1);
        InnerDragonsArmMesh.render(open, 0, 1, 1);
        InnerDragonsArmMesh.render(fading, 0, 0, .25F);
        require(open.minY < closed.minY - .05F, "punching must open the lower jaw");
        require(Math.abs(fading.maxAlpha - closed.maxAlpha * .25F) < .00001F, "reveal must fade every surface");
        Capture invalid = new Capture();
        InnerDragonsArmMesh.render(invalid, Float.NaN, 0, 1);
        InnerDragonsArmMesh.render(invalid, 0, Float.POSITIVE_INFINITY, 1);
        InnerDragonsArmMesh.render(invalid, 0, 0, Float.NaN);
        InnerDragonsArmMesh.render(invalid, 0, 0, 0);
        require(invalid.count == 0, "invalid/invisible mesh must emit nothing");
        System.out.println("Inner Dragons arm: " + checks + " geometry checks passed; " + expected + " vertices per head.");
    }
    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
