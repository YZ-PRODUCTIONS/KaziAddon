package net.kazi.kazimod.client.asriel;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import java.util.Arrays;
import net.kazi.kazimod.client.nagi.InnerDragonsCutsceneClient;
import net.kazi.kazimod.renderers.entities.VfxDetail;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only skull cinematic and posed caster. Never moves the real player or nearby entities. */
@Mod.EventBusSubscriber(modid="kazimod", value=Dist.CLIENT)
public final class HyperGonerVisuals extends RenderState {
    private static final RenderType BODY=material("kazimod_hyper_goner_body",false), GLOW=material("kazimod_hyper_goner_glow",true);
    private static final Batch SOLID=new Batch(), LIGHT=new Batch();
    private static ActorRenderer normal,slim;
    private static boolean drawingActor;
    private HyperGonerVisuals() { super(null,null,null); }
    private static RenderType material(String name,boolean glow) {
        return RenderType.create(name,DefaultVertexFormats.POSITION_COLOR,7,524288,false,!glow,
                RenderType.State.builder().setTransparencyState(glow?LIGHTNING_TRANSPARENCY:TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL).setWriteMaskState(glow?COLOR_WRITE:COLOR_DEPTH_WRITE).setDepthTestState(LEQUAL_DEPTH_TEST)
                .setShadeModelState(SMOOTH_SHADE).createCompositeState(false));
    }
    @SubscribeEvent public static void hideOriginal(RenderPlayerEvent.Pre event) {
        if(!drawingActor && InnerDragonsCutsceneClient.isGoat()
                && event.getPlayer().getUUID().equals(InnerDragonsCutsceneClient.subjectUuid())) event.setCanceled(true);
    }
    public static void render(RenderWorldLastEvent event) {
        Minecraft mc=Minecraft.getInstance();
        float partial=event.getPartialTicks(),age=InnerDragonsCutsceneClient.age(partial);
        Vector3d origin=InnerDragonsCutsceneClient.origin(partial),camera=mc.gameRenderer.getMainCamera().getPosition();
        MatrixStack stack=event.getMatrixStack();
        IRenderTypeBuffer.Impl buffers=mc.renderBuffers().bufferSource();
        float yaw=InnerDragonsCutsceneClient.yaw(partial);
        if(InnerDragonsCutsceneClient.subject() instanceof AbstractClientPlayerEntity) {
            AbstractClientPlayerEntity actor=(AbstractClientPlayerEntity)InnerDragonsCutsceneClient.subject();
            if(normal==null) { normal=new ActorRenderer(false);slim=new ActorRenderer(true); }
            stack.pushPose();
            try {
                drawingActor=true;
                stack.translate(origin.x-camera.x,origin.y-camera.y,origin.z-camera.z);
                ("slim".equals(actor.getModelName())?slim:normal).render(actor,yaw,partial,stack,buffers,15728880);
            } finally {drawingActor=false;stack.popPose();}
        }
        SOLID.clear();LIGHT.clear();
        HyperGonerMesh.render(SOLID,LIGHT,age,VfxDetail.levelForDistance(0));
        stack.pushPose();
        try {
            double radians=Math.toRadians(yaw);
            stack.translate(origin.x-camera.x+Math.sin(radians)*4,origin.y-camera.y+6,origin.z-camera.z-Math.cos(radians)*4);
            stack.mulPose(Vector3f.YP.rotationDegrees(-yaw));stack.scale(-1,1,1);
            SOLID.draw(stack,buffers.getBuffer(BODY));LIGHT.draw(stack,buffers.getBuffer(GLOW));
        } finally {stack.popPose();}
        buffers.endBatch();
    }
    public static void overlay(MatrixStack stack,int width,int height,float age) {
        float flash=HyperGonerMesh.flash(age);
        if(flash>.001F) net.minecraft.client.gui.AbstractGui.fill(stack,0,0,width,height,((int)(flash*255)<<24)|0xFFFFFF);
    }
    private static final class ActorRenderer extends PlayerRenderer {
        ActorRenderer(boolean slim) {
            super(Minecraft.getInstance().getEntityRenderDispatcher(),slim);
            this.model=new PlayerModel<AbstractClientPlayerEntity>(0,slim) {
                @Override public void setupAnim(AbstractClientPlayerEntity actor,float walk,float speed,float ticks,float yaw,float pitch) {
                    super.setupAnim(actor,0,0,ticks,0,0);
                    float age=InnerDragonsCutsceneClient.age(Minecraft.getInstance().getFrameTime());
                    float lift=HyperGonerMesh.smooth((age-12)/40)*HyperGonerMesh.fade(age);
                    rightArm.xRot=-.2F*lift;rightArm.yRot=0;rightArm.zRot=.95F*lift;
                    leftArm.xRot=-.2F*lift;leftArm.yRot=0;leftArm.zRot=-.95F*lift;
                    rightSleeve.copyFrom(rightArm);leftSleeve.copyFrom(leftArm);
                }
            };
            // Avoid drawing held items through the posed hands; inventory is untouched.
            this.layers.clear();
        }
        @Override protected void renderNameTag(AbstractClientPlayerEntity actor,ITextComponent text,MatrixStack stack,IRenderTypeBuffer buffers,int light) { }
    }
    private static final class Batch implements HyperGonerMesh.Sink {
        private float[] vertices=new float[32768];private int size;
        public void vertex(float x,float y,float z,int color,float alpha) {
            if(size+5>vertices.length) vertices=Arrays.copyOf(vertices,vertices.length*2);
            vertices[size++]=x;vertices[size++]=y;vertices[size++]=z;vertices[size++]=color;vertices[size++]=alpha;
        }
        void clear() {size=0;}
        void draw(MatrixStack stack,IVertexBuilder out) {
            for(int i=0;i<size;i+=5) {
                int rgb=(int)vertices[i+3];
                out.vertex(stack.last().pose(),vertices[i],vertices[i+1],vertices[i+2])
                        .color((rgb>>16&255)/255F,(rgb>>8&255)/255F,(rgb&255)/255F,vertices[i+4]).endVertex();
            }
        }
    }
}
