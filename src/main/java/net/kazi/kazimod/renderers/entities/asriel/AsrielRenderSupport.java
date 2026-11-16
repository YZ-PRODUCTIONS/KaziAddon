package net.kazi.kazimod.renderers.entities.asriel;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.kazi.kazimod.renderers.entities.VfxDetail;

final class AsrielRenderSupport {
    private static final class States extends RenderState {
        private States() { super(null,null,null); }
        static final TransparencyState GLOW=LIGHTNING_TRANSPARENCY;
        static final TransparencyState INK=new TransparencyState("asriel_glow_and_ink", () -> {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        }, () -> {
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        });
        static final AlphaState ALPHA=NO_ALPHA;
        static final CullState CULL=NO_CULL;
        static final WriteMaskState WRITE=COLOR_WRITE;
        static final DepthTestState DEPTH=LEQUAL_DEPTH_TEST;
        static final ShadeModelState SHADE=SMOOTH_SHADE;
    }
    static final RenderType GLOW=RenderType.create("kazimod_asriel_light",DefaultVertexFormats.POSITION_COLOR,7,16384,
            RenderType.State.builder().setTransparencyState(States.GLOW).setCullState(States.CULL)
                    .setWriteMaskState(States.WRITE).setDepthTestState(States.DEPTH)
                    .setShadeModelState(States.SHADE).createCompositeState(false));
    // Premultiplied emission keeps the glow; alpha only masks the background under dark ink.
    // Bright portions intentionally have zero ink alpha and must not be alpha-tested away.
    private static final RenderType INK=RenderType.create("kazimod_asriel_flowing_ink",DefaultVertexFormats.POSITION_COLOR,7,16384,false,true,
            RenderType.State.builder().setTransparencyState(States.INK).setCullState(States.CULL)
                    .setAlphaState(States.ALPHA)
                    .setWriteMaskState(States.WRITE).setDepthTestState(States.DEPTH)
                    .setShadeModelState(States.SHADE).createCompositeState(false));
    static AsrielVisualGeometry.Sink sink(IVertexBuilder vertices, MatrixStack stack) {
        Matrix4f matrix=stack.last().pose();
        return (x,y,z,r,g,b,a)->vertices.vertex(matrix,(float)x,(float)y,(float)z).color(r,g,b,a).endVertex();
    }
    static AsrielVisualGeometry.Sink sink(IRenderTypeBuffer buffers, MatrixStack stack, boolean dark, float age, float patternScale) {
        AsrielVisualGeometry.Sink out = sink(buffers.getBuffer(dark ? INK : GLOW), stack);
        if (!dark) return out;
        return AsrielVisualGeometry.flowingPalette(out,
                net.kazi.kazimod.abilities.YagiAwakening.YagiAwakeningMath.LOW_HEALTH_VISUAL_BRIGHTNESS,
                age, patternScale);
    }
    static void orient(MatrixStack stack,Vector3d direction) {
        if(direction.lengthSqr()<.0001)return;
        Vector3d d=direction.normalize();
        stack.mulPose(Vector3f.YP.rotationDegrees((float)Math.toDegrees(Math.atan2(d.x,d.z))));
        stack.mulPose(Vector3f.XP.rotationDegrees((float)-Math.toDegrees(Math.asin(Math.max(-1,Math.min(1,d.y))))));
    }
    /** Keep detail beside the entire beam, while reducing distant overlapping pillars' cost. */
    static int lineDetail(Entity entity,EntityRendererManager renderer,Vector3d direction,double length,double radius) {
        Vector3d camera=renderer.camera.getPosition();
        double dx=camera.x-entity.getX(),dy=camera.y-entity.getY(),dz=camera.z-entity.getZ();
        double along=Math.max(0,Math.min(length,dx*direction.x+dy*direction.y+dz*direction.z));
        double x=dx-direction.x*along,y=dy-direction.y*along,z=dz-direction.z*along;
        return VfxDetail.levelForDistance(Math.max(0,Math.sqrt(x*x+y*y+z*z)-radius));
    }
    static boolean visible(Entity entity,ClippingHelper frustum,double x,double y,double z,double radius,double height) {
        return entity.shouldRender(x,y,z) && frustum.isVisible(new AxisAlignedBB(entity.getX()-radius,entity.getY()-radius,
                entity.getZ()-radius,entity.getX()+radius,entity.getY()+height,entity.getZ()+radius));
    }
    private AsrielRenderSupport() { }
}
