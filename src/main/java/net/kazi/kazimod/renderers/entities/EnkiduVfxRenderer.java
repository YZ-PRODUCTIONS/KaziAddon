package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import java.util.Arrays;
import net.kazi.kazimod.abilities.GoruRework.EnkiduAbility;
import net.kazi.kazimod.entities.EnkiduVfxEntity;
import net.kazi.kazimod.models.abilities.EnkiduMesh;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;

/** Two reusable draw batches for an entire cast; chain links are not individual entities. */
public final class EnkiduVfxRenderer extends EntityRenderer<EnkiduVfxEntity> {
    private static final ResourceLocation UNUSED = new ResourceLocation("minecraft", "textures/atlas/blocks.png");
    private static final RenderType METAL = States.metal(), GLOW = States.glow();
    private final Batch metal = new Batch(), glow = new Batch();
    public EnkiduVfxRenderer(EntityRendererManager manager) { super(manager); shadowRadius = 0; }
    @Override public ResourceLocation getTextureLocation(EnkiduVfxEntity entity) { return UNUSED; }
    @Override public boolean shouldRender(EnkiduVfxEntity entity, ClippingHelper frustum, double x, double y, double z) {
        Vector3d center = entity.getCenter(1);
        return entity.shouldRender(x,y,z) && frustum.isVisible(entity.getMode() == EnkiduAbility.Mode.ABSOLUTE
                ? new AxisAlignedBB(center,center).inflate(48)
                : new AxisAlignedBB(center,center).inflate(20,16,20));
    }
    @Override public void render(EnkiduVfxEntity entity, float yaw, float partial, MatrixStack stack,
                                 IRenderTypeBuffer buffers, int light) {
        float fade = entity.isFading() ? MathHelper.clamp(entity.getPhaseAge(partial) / EnkiduVfxEntity.FADE_TICKS, 0, 1) : 0;
        float opacity = 1 - fade * fade * (3 - 2 * fade);
        if (opacity <= .0039F) return;
        float age = entity.getVisualAge(partial);
        int phase = entity.getVisualPhase();
        float progress = phase == EnkiduVfxEntity.CHARGING ? age / entity.getMode().chargeTicks
                : phase == EnkiduVfxEntity.LAUNCHING ? age / EnkiduVfxEntity.TRAVEL_TICKS : 1;
        Vector3d center = entity.getCenter(partial);
        int detail = VfxDetail.level(entity, entityRenderDispatcher, entity.getMode() == EnkiduAbility.Mode.AREA ? 14 : 5);
        EnkiduVfxEntity.VisualTarget[] targets = entity.getVisualTargets();
        // Crowds share a bounded visual budget, while every target retains its own chains.
        if (targets.length > 6) detail = 2;
        else if (targets.length > 3) detail = Math.max(1, detail);
        metal.clear(); glow.clear();
        if ((entity.getMode() == EnkiduAbility.Mode.AREA || entity.getMode() == EnkiduAbility.Mode.ABSOLUTE)
                && phase == EnkiduVfxEntity.CHARGING) {
            // Gates open across the marked ground before choosing victims at release.
            EnkiduMesh.target(metal, glow, Math.max(1,detail), age, 0, progress, 4, 2,
                    opacity * .75F, fade, entity.getId());
        }
        for (EnkiduVfxEntity.VisualTarget target : targets) {
            Vector3d offset = target.position(entity.level, partial).subtract(center);
            metal.offset(offset); glow.offset(offset);
            EnkiduMesh.target(metal, glow, detail, age, phase, progress, target.width, target.height,
                    opacity, fade, target.seed);
        }
        stack.pushPose();
        stack.translate(center.x - MathHelper.lerp(partial, entity.xOld, entity.getX()),
                center.y - MathHelper.lerp(partial, entity.yOld, entity.getY()),
                center.z - MathHelper.lerp(partial, entity.zOld, entity.getZ()));
        Vector3d camera = entityRenderDispatcher.camera.getPosition().subtract(center);
        metal.draw(stack.last().pose(), buffers.getBuffer(METAL), camera);
        glow.draw(stack.last().pose(), buffers.getBuffer(GLOW), camera);
        stack.popPose();
    }
    private static final class Batch implements EnkiduMesh.Sink {
        float[] vertices = new float[4096 * 4];
        int[] colors = new int[4096];
        int count;
        float ox,oy,oz;
        void clear() { count=0;ox=0;oy=0;oz=0; }
        void offset(Vector3d p) { ox=(float)p.x;oy=(float)p.y;oz=(float)p.z; }
        @Override public void vertex(float x,float y,float z,int color,float alpha) {
            if(count==colors.length){colors=Arrays.copyOf(colors,count*2);vertices=Arrays.copyOf(vertices,vertices.length*2);}
            int offset=count*4;
            vertices[offset]=x+ox;vertices[offset+1]=y+oy;vertices[offset+2]=z+oz;vertices[offset+3]=alpha;
            colors[count++]=color;
        }
        void draw(Matrix4f pose,IVertexBuilder out,Vector3d camera) {
            for(int base=0;base<count;base+=4){
                int p=base*4;
                if(vertices[p+3]<=.0039F&&vertices[p+7]<=.0039F&&vertices[p+11]<=.0039F&&vertices[p+15]<=.0039F)continue;
                for(int v=base;v<base+4;v++){
                    int i=v*4,c=colors[v];
                    float near=EaVfxVisibility.proximity(vertices[i]-camera.x,vertices[i+1]-camera.y,vertices[i+2]-camera.z);
                    out.vertex(pose,vertices[i],vertices[i+1],vertices[i+2])
                            .color(c>>16&255,c>>8&255,c&255,MathHelper.clamp((int)(vertices[i+3]*near*255),0,255)).endVertex();
                }
            }
        }
    }
    private static final class States extends RenderState {
        private States(){super(null,null,null);}
        static RenderType metal(){return RenderType.create("kazimod_enkidu_gold",DefaultVertexFormats.POSITION_COLOR,
                7,262144,false,true,RenderType.State.builder().setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_DEPTH_WRITE).setCullState(NO_CULL).createCompositeState(false));}
        static RenderType glow(){return RenderType.create("kazimod_enkidu_gates",DefaultVertexFormats.POSITION_COLOR,
                7,262144,false,false,RenderType.State.builder().setTransparencyState(LIGHTNING_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).setCullState(NO_CULL).createCompositeState(false));}
    }
}
