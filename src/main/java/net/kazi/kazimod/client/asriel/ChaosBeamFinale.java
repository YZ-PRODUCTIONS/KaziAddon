package net.kazi.kazimod.client.asriel;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.client.nagi.InnerDragonsCutsceneClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Ultra-detail finale: world-space spiral arms and camera-facing, luminous five-point stars. */
@Mod.EventBusSubscriber(modid = "kazimod", value = Dist.CLIENT)
public final class ChaosBeamFinale extends RenderState {
    private static final int ARMS = 7;
    private static final int STARS_PER_ARM = 30;
    private static final double TAU = 2 * Math.PI;
    private static final int[] COLORS = {0xffacda, 0xffcb8a, 0xfff2ac, 0xc8ffb7, 0x99fbff, 0xb5c2ff, 0xe6a8ff};
    private static final double[][] STAR = outline(10, .41);
    private static final double[][] HALO = outline(8, 1);
    private static ClientWorld returnWorld;
    private static LivingEntity returnViewer;
    private static long returnStart;
    private static final RenderType LIGHT = RenderType.create("kazimod_chaos_finale_stars",
            DefaultVertexFormats.POSITION_COLOR, 7, 524288, false, false,
            RenderType.State.builder().setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE).setShadeModelState(SMOOTH_SHADE).createCompositeState(false));

    private ChaosBeamFinale() { super(null, null, null); }

    public static void world(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getInstance();
        float partial = event.getPartialTicks(), age = InnerDragonsCutsceneClient.age(partial);
        if (age >= ChaosBeamFinaleMotion.FULL_WHITE_TICK) return;
        Vector3d origin = InnerDragonsCutsceneClient.origin(partial);
        Vector3d camera = mc.gameRenderer.getMainCamera().getPosition();
        MatrixStack stack = event.getMatrixStack();
        IRenderTypeBuffer.Impl buffers = mc.renderBuffers().bufferSource();
        LivingEntity actor = InnerDragonsCutsceneClient.subject();
        double launchRadius = actor == null ? .5 : Math.max(.4, actor.getBbWidth() * .5 + .08);
        if (InnerDragonsCutsceneClient.fallbackSubject() && actor != null) {
            mc.getEntityRenderDispatcher().render(actor, origin.x-camera.x, origin.y-camera.y, origin.z-camera.z,
                    InnerDragonsCutsceneClient.yaw(partial), partial, stack, buffers, 15728880);
            buffers.endBatch();
        }
        IVertexBuilder vertices = buffers.getBuffer(LIGHT);
        stack.pushPose();
        try {
            stack.translate(origin.x-camera.x, origin.y-camera.y + (actor == null ? 1.4 : actor.getBbHeight() * .72),
                    origin.z-camera.z);
            for (int arm = 0; arm < ARMS; arm++) {
                for (int slot = 0; slot < STARS_PER_ARM; slot++) {
                    double launch = slot * 9.0 + (arm % 3) * .8;
                    if (age < launch) continue;
                    int color = COLORS[(arm + slot / 5) % COLORS.length];
                    for (int trail = 3; trail >= 0; trail--) {
                        // Each trail sample follows the same star at an earlier time.
                        double sampleAge = age - trail * 1.5;
                        double flight = sampleAge - launch;
                        if (flight < 0) continue;
                        double whiteout = ChaosBeamFinaleMotion.whiteout(sampleAge);
                        double radius = ChaosBeamFinaleMotion.radius(launchRadius, flight, whiteout);
                        double angle = arm * TAU / ARMS + launch * .052 + flight * .032;
                        double y = (radius - launchRadius) * (.22 + .13 * Math.sin(angle + sampleAge * .015));
                        double size = ChaosBeamFinaleMotion.size(flight, whiteout) * (1 + .18*Math.sin(slot + sampleAge*.15));
                        float alpha = (float)ChaosBeamFinaleMotion.smooth(flight / 5);
                        if (alpha <= 0) continue;
                        stack.pushPose();
                        stack.translate(Math.cos(angle)*radius, y, Math.sin(angle)*radius);
                        stack.mulPose(mc.gameRenderer.getMainCamera().rotation());
                        Matrix4f pose = stack.last().pose();
                        if (trail == 0) {
                            fan(vertices, pose, size*1.85, angle, color, alpha*.38F, false);
                            fan(vertices, pose, size, angle+age*.025, 0xfffbea, alpha, true);
                        } else fan(vertices, pose, size*(1-trail*.19), angle, color, alpha*(.4F/trail), false);
                        stack.popPose();
                    }
                }
            }
        } finally { stack.popPose(); }
        buffers.endBatch(LIGHT);
    }

    /** Triangle fan encoded as degenerate quads; soft halos fade to transparent at their rim. */
    private static void fan(IVertexBuilder out, Matrix4f pose, double size, double rotation,
                            int rgb, float alpha, boolean star) {
        double[][] rim = star ? STAR : HALO;
        double cos = size*Math.cos(rotation), sin = size*Math.sin(rotation);
        for (int i=0; i<rim.length-1; i++) {
            double[] a = rim[i], b = rim[i+1];
            vertex(out, pose, 0, 0, rgb, alpha);
            vertex(out, pose, cos*a[0]-sin*a[1], sin*a[0]+cos*a[1], rgb, star ? alpha : 0);
            vertex(out, pose, cos*b[0]-sin*b[1], sin*b[0]+cos*b[1], rgb, star ? alpha : 0);
            vertex(out, pose, 0, 0, rgb, alpha);
        }
    }

    private static double[][] outline(int edges, double innerRadius) {
        double[][] rim = new double[edges+1][2];
        for (int i=0; i<=edges; i++) {
            double r = (i&1)==0 ? 1 : innerRadius;
            rim[i][0] = r*Math.cos(i*TAU/edges);
            rim[i][1] = r*Math.sin(i*TAU/edges);
        }
        return rim;
    }

    private static void vertex(IVertexBuilder out, Matrix4f pose, double x, double y, int rgb, float alpha) {
        out.vertex(pose, (float)x, (float)y, 0).color(rgb>>16&255, rgb>>8&255, rgb&255, (int)(255*alpha)).endVertex();
    }

    public static void render(MatrixStack stack, int width, int height, float age) {
        drawWhite(stack, width, height, ChaosBeamFinaleMotion.whiteout(age));
    }

    /** The normal camera is restored under full white before this final reveal begins. */
    public static void beginReturnFade(ClientWorld world, LivingEntity viewer, long startTick) {
        returnWorld = world;
        returnViewer = viewer;
        returnStart = startTick;
    }

    public static void clearReturnFade() {
        returnWorld = null;
        returnViewer = null;
    }

    private static double returnOpacity(float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (returnWorld == null) return 0;
        if (mc.level != returnWorld || mc.player != returnViewer || returnViewer.removed || !returnViewer.isAlive()) {
            clearReturnFade();
            return 0;
        }
        double age = returnWorld.getGameTime() - returnStart + (mc.isPaused() ? 0 : partial);
        if (age < 0 || age >= ChaosBeamFinaleMotion.RETURN_FADE_TICKS) {
            clearReturnFade();
            return 0;
        }
        return ChaosBeamFinaleMotion.returnOpacity(age);
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) returnOpacity(0);
    }

    @SubscribeEvent public static void returnOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL)
            drawWhite(event.getMatrixStack(), event.getWindow().getGuiScaledWidth(),
                    event.getWindow().getGuiScaledHeight(), returnOpacity(event.getPartialTicks()));
    }

    private static void drawWhite(MatrixStack stack, int width, int height, double opacity) {
        int alpha = (int)(255*opacity);
        if (alpha <= 0) return;
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableAlphaTest();
        try {
            AbstractGui.fill(stack, 0, 0, width, height, alpha<<24 | 0xffffff);
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableAlphaTest();
            RenderSystem.color4f(1, 1, 1, 1);
        }
    }

    @SubscribeEvent public static void hiddenHud(TickEvent.RenderTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase == TickEvent.Phase.END && mc.options.hideGui && mc.screen == null) {
            if (InnerDragonsCutsceneClient.isChaosFinale())
                render(new MatrixStack(), mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(),
                        InnerDragonsCutsceneClient.age(mc.getFrameTime()));
            else
                drawWhite(new MatrixStack(), mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(),
                        returnOpacity(mc.getFrameTime()));
        }
    }
}
