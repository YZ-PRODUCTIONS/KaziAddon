package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.client.AfterImageStore;
import net.kazi.kazimod.client.AfterImageStore.Snapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = "kazimod", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class AfterImageRenderHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            AfterImageStore.INSTANCE.tick();
        }
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        List<Snapshot> snapshots = AfterImageStore.INSTANCE.getSnapshots();
        if (snapshots.isEmpty()) {
            return;
        }

        Vector3d cam = mc.gameRenderer.getMainCamera().getPosition();
        MatrixStack stack = event.getMatrixStack();
        IRenderTypeBuffer.Impl buffer = mc.renderBuffers().bufferSource();

        for (Snapshot snap : snapshots) {
            float alpha = snap.getAlpha();
            if (alpha <= 0.01F) {
                continue;
            }

            // Find the skin owner in loaded players.
            AbstractClientPlayerEntity skinPlayer = null;
            for (AbstractClientPlayerEntity p : mc.level.players()) {
                if (p.getGameProfile().getName().equals(snap.skinName)) {
                    skinPlayer = p;
                    break;
                }
            }
            if (skinPlayer == null) {
                continue;
            }

            net.minecraft.client.renderer.entity.EntityRenderer<?> rawRenderer =
                    mc.getEntityRenderDispatcher().getRenderer(skinPlayer);
            if (!(rawRenderer instanceof PlayerRenderer)) {
                continue;
            }

            PlayerRenderer pr = (PlayerRenderer) rawRenderer;
            PlayerModel<AbstractClientPlayerEntity> model = pr.getModel();

            stack.pushPose();

            // Move to the ghost's position in camera-relative space.
            stack.translate(
                    snap.x - cam.x,
                    snap.y - cam.y,
                    snap.z - cam.z
            );

            // Vanilla LivingEntityRenderer.render() does these exact transforms
            // before calling model.renderToBuffer:
            //   1. scale(−1, −1, 1)  — flips X and Y so model faces correct way
            //   2. translate(0, −1.501, 0) — shifts pivot to foot level
            //   3. rotate by body yaw
            stack.scale(-1.0F, -1.0F, 1.0F);
            stack.translate(0.0D, -1.501D, 0.0D);
            stack.mulPose(Vector3f.YP.rotationDegrees(snap.yaw));

            model.young     = false;
            model.crouching = false;
            model.riding    = false;
            model.rightArmPose = BipedModel.ArmPose.EMPTY;
            model.leftArmPose  = BipedModel.ArmPose.EMPTY;

            model.prepareMobModel(skinPlayer, 0.0F, 0.0F, event.getPartialTicks());
            model.setupAnim(skinPlayer, 0.0F, 0.0F, 0.0F, snap.yaw, snap.pitch);

            ResourceLocation skin = skinPlayer.getSkinTextureLocation();
            IVertexBuilder builder = buffer.getBuffer(RenderType.entityTranslucentCull(skin));

            model.renderToBuffer(stack, builder, 15728880,
                    OverlayTexture.NO_OVERLAY,
                    0.70F, 0.86F, 1.00F,
                    alpha * 0.63F);

            stack.popPose();
        }

        buffer.endBatch();
    }
}