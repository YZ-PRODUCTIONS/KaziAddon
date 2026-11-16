package net.kazi.kazimod.alchemy;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;

/** A dorsal forearm overlay, never a replacement arm or player skin. Coordinates are model pixels. */
final class AlchemyBlade {
    private static final ResourceLocation METAL = new ResourceLocation("minecraft", "textures/block/iron_block.png");
    private static final RenderType TYPE = RenderType.entityCutoutNoCull(METAL);

    static void render(MatrixStack stack, IRenderTypeBuffer buffers, int light, AbstractClientPlayerEntity player,
                       ModelRenderer arm, float partial) {
        if (!player.isAlive() || player.isInvisible() || player.isSpectator() || !arm.visible) return;
        CompoundNBT state = AlchemyFeatures.state(player);
        if (!state.getBoolean("Blade")) return;
        int action = AlchemyFeatures.action(player);
        float age = player.level.getGameTime() - state.getLong("Start") + partial;
        float growth = action == 0 ? AlchemyMesh.smooth((age - 7) / 6) : 1;
        float charge = action == 0 ? AlchemyMesh.smooth((age - 4) / 3) * (1 - AlchemyMesh.smooth((age - 12) / 5)) : 0;
        long contact = state.getLong("ComboContact");
        if (action == 4 && contact > 0) {
            float hitAge = player.level.getGameTime() - contact + partial;
            for (int i = 0; i < 4; i++) {
                float t = hitAge - i * 10;
                if (t >= 0 && t < 5) charge = Math.max(charge, (1 - AlchemyMesh.smooth(t / 5)) * .8F);
            }
        }
        if (growth <= .001F && charge <= .001F) return;
        double center = AlchemyClient.slim(player) ? -.5 : -1;
        double width = AlchemyClient.slim(player) ? 3 : 4;
        stack.pushPose();
        try {
            arm.translateAndRotate(stack);
            stack.scale(.0625F, .0625F, .0625F);
            if (growth > .001F) {
                IVertexBuilder metal = buffers.getBuffer(TYPE);
                // Narrow dorsal plates leave the hand, upper arm, and inner forearm skin visible.
                box(stack, metal, light, center, 5.2, -2.36, width + .48, 1.0, .32, .63F);
                box(stack, metal, light, center, 8.65, -2.36, width + .48, 1.15, .32, .72F);
                box(stack, metal, light, center, 6.9, -2.51, width * .79, 2.65, .28, .82F);
                for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++)
                    box(stack, metal, light, center + (i == 0 ? -1 : 1) * (width * .5 - .25),
                            j == 0 ? 5.2 : 8.65, -2.62, .3, .3, .20, .92F);
                double tip = 10.8 + 12.2 * growth;
                Vector3d[] edge = {
                    new Vector3d(center - 1.0, 6.5, -2.70),
                    new Vector3d(center - 1.55, 10.5, -2.70),
                    new Vector3d(center - .32, tip, -2.70),
                    new Vector3d(center + 1.22, 10.0, -2.70),
                    new Vector3d(center + 1.0, 6.5, -2.70)
                };
                Vector3d ridge = new Vector3d(center - .2, 10.2, -3.20);
                Vector3d back = new Vector3d(center - .2, 10.2, -2.58);
                for (int i = 0; i < edge.length; i++) {
                    Vector3d a = edge[i], b = edge[(i + 1) % edge.length];
                    face(stack, metal, light, a, b, ridge, ridge, i % 2 == 0 ? .98F : .69F);
                    face(stack, metal, light, b, a, back, back, .52F);
                }
            }
            if (charge > .005F) {
                IVertexBuilder energy = buffers.getBuffer(AlchemyMesh.ENERGY);
                for (int line = 0; line < 3; line++) {
                    Vector3d last = new Vector3d(center - 1 + line, 4.7, -2.92);
                    for (int i = 1; i <= 5; i++) {
                        double y = 4.7 + i * (2.3 + growth * .9);
                        double x = center - 1 + line + Math.sin(i * 2.9 + age * .55 + line) * .34;
                        Vector3d point = new Vector3d(x, y, -3.24);
                        AlchemyMesh.filament(energy, stack.last().pose(), last, point, .14, charge);
                        last = point;
                    }
                }
            }
        } finally { stack.popPose(); }
    }

    private static void box(MatrixStack s, IVertexBuilder out, int light, double x, double y, double z,
                             double w, double h, double d, float shade) {
        Vector3d[] p = {new Vector3d(x-w/2,y-h/2,z-d/2), new Vector3d(x+w/2,y-h/2,z-d/2),
                new Vector3d(x+w/2,y+h/2,z-d/2), new Vector3d(x-w/2,y+h/2,z-d/2),
                new Vector3d(x-w/2,y-h/2,z+d/2), new Vector3d(x+w/2,y-h/2,z+d/2),
                new Vector3d(x+w/2,y+h/2,z+d/2), new Vector3d(x-w/2,y+h/2,z+d/2)};
        face(s,out,light,p[0],p[3],p[2],p[1],shade); face(s,out,light,p[5],p[6],p[7],p[4],shade*.8F);
        face(s,out,light,p[4],p[7],p[3],p[0],shade*.72F); face(s,out,light,p[1],p[2],p[6],p[5],shade*.9F);
        face(s,out,light,p[3],p[7],p[6],p[2],shade); face(s,out,light,p[4],p[0],p[1],p[5],shade*.65F);
    }

    private static void face(MatrixStack s, IVertexBuilder out, int light, Vector3d a, Vector3d b,
                              Vector3d c, Vector3d d, float shade) {
        Vector3d normal = b.subtract(a).cross(c.subtract(a)).normalize();
        AlchemyMesh.vertex(out,s.last().pose(),s.last().normal(),a,normal,shade*.94F,shade*.97F,shade,1,.05F,.95F,light);
        AlchemyMesh.vertex(out,s.last().pose(),s.last().normal(),b,normal,shade*.94F,shade*.97F,shade,1,.05F,.05F,light);
        AlchemyMesh.vertex(out,s.last().pose(),s.last().normal(),c,normal,shade*.94F,shade*.97F,shade,1,.95F,.05F,light);
        AlchemyMesh.vertex(out,s.last().pose(),s.last().normal(),d,normal,shade*.94F,shade*.97F,shade,1,.95F,.95F,light);
    }

    private AlchemyBlade() {}
}
