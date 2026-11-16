package net.kazi.kazimod.leopard;

import java.util.Map;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.models.zoan.CerberusRig;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;

/** Bounded, bone-attached smoke ribbons; no particle entities or model cubes. */
public final class LeopardCloudVfx {
    private static final ResourceLocation SMOKE = new ResourceLocation("kazimod", "textures/vfx/moku_smoke.png");
    public static void render(CerberusRig rig, Map<String, CerberusRig.Pose> pose, MatrixStack stack,
                              IRenderTypeBuffer buffers, float age, float opacity) {
        IVertexBuilder out = buffers.getBuffer(RenderType.entityTranslucent(SMOKE));
        float time = age * .035F;
        stack.pushPose();
        attach(rig, pose, stack, "Chest");
        for (int side : new int[]{-1, 1}) {
            // An open, curled hagoromo loop, rising from the shoulder and curling inward.
            for (int i = 0; i < 25; i++) {
                float t = i / 24F, angle = t * 4.8F - 1.5F;
                float radius = 11 * (1 - .52F * t * t);
                float x = side * (10 + radius * (float)Math.cos(angle));
                float y = 44 + radius * (float)Math.sin(angle);
                float z = 3.8F + .6F * (float)Math.sin(time + t * 7);
                float size = (6.8F - 3 * t) * (1 + .09F * (float)Math.sin(time * 1.5F - i));
                puff(stack, out, x, y, z, size, time * .2F + side * t, i, opacity * .92F);
            }
        }
        stack.popPose();
        for (int side : new int[]{-1, 1}) {
            stack.pushPose(); attach(rig, pose, stack, "Forearm " + side);
            for (int i = 0; i < 10; i++) {
                float a = i * (float)Math.PI / 5 + time * .5F;
                puff(stack, out, side * 11.75F + 3.4F * (float)Math.cos(a),
                        20 + .6F * (float)Math.sin(a * 2 + time), 3.4F * (float)Math.sin(a),
                        4.3F, a, i, opacity * .9F);
            }
            stack.popPose();
        }
    }
    private static void attach(CerberusRig rig, Map<String, CerberusRig.Pose> pose, MatrixStack stack, String bone) {
        for (CerberusRig.Node node : rig.path(bone)) {
            CerberusRig.Pose p = pose.get(node.name);
            float[] o = node.origin, r = node.rotation;
            stack.translate(o[0] + (p == null ? 0 : p.position[0]), o[1] + (p == null ? 0 : p.position[1]), o[2] + (p == null ? 0 : p.position[2]));
            stack.mulPose(Vector3f.ZP.rotationDegrees(r[2] + (p == null ? 0 : p.rotation[2])));
            stack.mulPose(Vector3f.YP.rotationDegrees(r[1] + (p == null ? 0 : p.rotation[1])));
            stack.mulPose(Vector3f.XP.rotationDegrees(r[0] + (p == null ? 0 : p.rotation[0])));
            stack.translate(-o[0], -o[1], -o[2]);
        }
    }
    private static void puff(MatrixStack stack, IVertexBuilder out, float x, float y, float z,
                             float size, float spin, int tile, float alpha) {
        stack.pushPose(); stack.translate(x, y, z);
        stack.mulPose(Vector3f.ZP.rotation(spin));
        float u = (tile & 1) * .5F, v = ((tile >> 1) & 1) * .5F;
        for (int plane = 0; plane < 3; plane++) {
            stack.pushPose();
            if (plane == 1) stack.mulPose(Vector3f.YP.rotationDegrees(90));
            if (plane == 2) stack.mulPose(Vector3f.XP.rotationDegrees(90));
            for (int i = 0; i < 4; i++) {
                float px = i == 0 || i == 3 ? -size / 2 : size / 2;
                float py = i < 2 ? -size / 2 : size / 2;
                out.vertex(stack.last().pose(), px, py, 0).color(.028F, .017F, .047F, alpha)
                    .uv(u + (px > 0 ? .499F : .001F), v + (py > 0 ? .499F : .001F))
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880)
                    .normal(stack.last().normal(), 0, 0, 1).endVertex();
            }
            stack.popPose();
        }
        stack.popPose();
    }
    private LeopardCloudVfx() {}
}
