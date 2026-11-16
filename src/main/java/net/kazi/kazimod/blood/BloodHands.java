package net.kazi.kazimod.blood;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.client.event.RenderHandEvent;

/** Draws the player's own skin and sleeve, without changing held equipment or camera rotation. */
final class BloodHands {
    static void render(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayerEntity p = mc.player;
        if (p == null || !p.isAlive() || p.isInvisible() || p.isSpectator() || mc.getCameraEntity() != p
                || p.hasEffect(xyz.pixelatedw.mineminenomi.init.ModEffects.NO_HANDS.get())) return;
        float partial = event.getPartialTicks(), age = BloodPose.age(p, partial);
        float gesture = BloodPose.gestureAge(p, partial);
        int action = BloodFeatures.action(p);
        boolean holding=action<0&&BloodPose.holdingNova(p);
        if (action != 0 && action != 1 && !(action < 0 && gesture < 12) && !holding) return;
        event.setCanceled(true);
        Hand pass = p.isUsingItem() && p.getUsedItemHand() == Hand.OFF_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
        if (event.getHand() != pass) return;
        PlayerRenderer renderer = (PlayerRenderer)mc.getEntityRenderDispatcher().getRenderer(p);
        PlayerModel<AbstractClientPlayerEntity> model = renderer.getModel();
        float duration = BloodFeatures.scale(p) ? 22 : 30;
        float compression = BloodMesh.smooth((age - 3) / (duration - 8));
        float release = action == 0 ? BloodPose.releaseAge(p, partial) : -1;
        float enter = BloodMesh.smooth(age / 5);
        float exit = release < 0 ? 1 : 1 - BloodMesh.smooth((release - BloodFeatures.state(p).getInt("FiringTicks")) / 6);
        double gap = .23 - compression * .14, y = -.27 + compression * .11, z = -.7 - compression * .14;
        if (action == 1 || holding) { gap = .22 - BloodMesh.smooth(age / 8) * .13; y = -.21; z = -.8; }
        if (gesture < 12 && action != 0) {
            float close = BloodMesh.smooth(gesture / 3);
            gap = .22 - close * .13; y = -.15; z = -.82;
            enter = 1; exit = 1 - BloodMesh.smooth((gesture - 5) / 7);
        }
        if (release >= 0) z += Math.sin(Math.min(1, release / 6) * Math.PI) * .075;
        float mix = enter * exit;
        gap = MathHelper.lerp(mix, .46, gap); y = MathHelper.lerp(mix, -.63, y); z = MathHelper.lerp(mix, -.35, z);
        draw(event, p, model.rightArm, model.rightSleeve, 1, new Vector3d(gap, y, z));
        draw(event, p, model.leftArm, model.leftSleeve, -1, new Vector3d(-gap, y, z));
        if (action == 0) {
            IVertexBuilder out = event.getBuffers().getBuffer(BloodMesh.TYPE);
            Vector3d forward = new Vector3d(0, 0, -1);
            MatrixStack s=event.getMatrixStack();s.pushPose();
            s.translate(0,-.055,-1.03);s.scale(.65F,.65F,.65F);
            if (release < 0) BloodEffects.convergence(out, s.last().pose(), Vector3d.ZERO, forward, age, duration, 1);
            s.popPose();
        }
    }

    private static void draw(RenderHandEvent event, AbstractClientPlayerEntity p, ModelRenderer arm,
                              ModelRenderer sleeve, int side, Vector3d hand) {
        MatrixStack s = event.getMatrixStack();
        Vector3d shoulder = new Vector3d(side * .5, -.73, .12), d = hand.subtract(shoulder);
        s.pushPose();
        try {
            s.translate(shoulder.x, shoulder.y, shoulder.z);
            s.mulPose(Vector3f.YP.rotation((float)Math.atan2(d.x, d.z)));
            s.mulPose(Vector3f.XP.rotation((float)Math.atan2(Math.sqrt(d.x * d.x + d.z * d.z), d.y)));
            s.scale(.82F, (float)(d.length() / .75), .82F);
            // Skin arm cubes run from -2 to 10 pixels; align their full length to the endpoints.
            s.translate(side * (p.getModelName().equals("slim") ? .03125 : .0625), .125, 0);
            IVertexBuilder skin = event.getBuffers().getBuffer(RenderType.entityCutoutNoCull(p.getSkinTextureLocation()));
            part(arm, s, skin, event.getLight());
            if (p.isModelPartShown(side > 0 ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE))
                part(sleeve, s, skin, event.getLight());
        } finally { s.popPose(); }
    }

    private static void part(ModelRenderer part, MatrixStack s, IVertexBuilder out, int light) {
        float x = part.x, y = part.y, z = part.z, rx = part.xRot, ry = part.yRot, rz = part.zRot;
        boolean visible = part.visible;
        try {
            part.x = part.y = part.z = part.xRot = part.yRot = part.zRot = 0;
            part.visible = true;
            part.render(s, out, light, OverlayTexture.NO_OVERLAY);
        } finally {
            part.x = x; part.y = y; part.z = z; part.xRot = rx; part.yRot = ry; part.zRot = rz; part.visible = visible;
        }
    }

    private BloodHands() { }
}
