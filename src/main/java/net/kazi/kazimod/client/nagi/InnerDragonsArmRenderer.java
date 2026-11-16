package net.kazi.kazimod.client.nagi;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsSequence;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;

/** Spectral replacement limb. Uses one modest translucent mesh and no per-frame textures or particles. */
public final class InnerDragonsArmRenderer extends RenderState {
    private static final RenderType HEAD = RenderType.create("kazimod_inner_dragon_arm",
            DefaultVertexFormats.POSITION_COLOR, 7, 32768, false, true,
            RenderType.State.builder().setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE)
                    .setDepthTestState(LEQUAL_DEPTH_TEST).setShadeModelState(SMOOTH_SHADE)
                    .createCompositeState(false));
    private InnerDragonsArmRenderer() { super(null, null, null); }

    public static void attached(MatrixStack stack, IRenderTypeBuffer buffers,
                                PlayerModel<AbstractClientPlayerEntity> model, AbstractClientPlayerEntity player,
                                float partial) {
        stack.pushPose();
        try {
            model.rightArm.translateAndRotate(stack);
            stack.translate("slim".equals(player.getModelName()) ? -.03125 : -.0625, 0, 0);
            // The neck replaces the shoulder; a punch points the dragon's muzzle along the arm.
            stack.mulPose(Vector3f.XP.rotationDegrees(-90));
            float reveal = reveal(player, partial);
            stack.scale(reveal, reveal, reveal);
            render(stack, buffers, player, partial, player.getAttackAnim(partial), reveal);
        } finally { stack.popPose(); }
    }

    public static void firstPerson(MatrixStack stack, IRenderTypeBuffer buffers, AbstractClientPlayerEntity player,
                                   float partial, float swing, float equip) {
        stack.pushPose();
        try {
            float root = MathHelper.sqrt(swing);
            stack.translate(.46F - .22F * MathHelper.sin(root * (float) Math.PI),
                    -.38F + .12F * MathHelper.sin(root * (float) Math.PI * 2) - equip * .35F,
                    -.63F - .25F * MathHelper.sin(swing * (float) Math.PI));
            stack.mulPose(Vector3f.YP.rotationDegrees(180 - MathHelper.sin(root * (float) Math.PI) * 12));
            stack.mulPose(Vector3f.XP.rotationDegrees(9 + MathHelper.sin(swing * (float) Math.PI) * 10));
            stack.scale(.72F, .72F, .72F);
            render(stack, buffers, player, partial, swing, 1);
        } finally { stack.popPose(); }
    }

    private static float reveal(AbstractClientPlayerEntity player, float partial) {
        if (!InnerDragonsCutsceneClient.active()
                || !player.getUUID().equals(InnerDragonsCutsceneClient.subjectUuid())) return 1;
        float t = MathHelper.clamp((InnerDragonsCutsceneClient.age(partial)
                - InnerDragonsSequence.ARM_BREAK_TICK) / 12F, 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static void render(MatrixStack stack, IRenderTypeBuffer buffers, AbstractClientPlayerEntity player,
                               float partial, float attack, float opacity) {
        Matrix4f pose = stack.last().pose();
        IVertexBuilder vertices = buffers.getBuffer(HEAD);
        InnerDragonsArmMesh.render((x, y, z, rgb, alpha) -> vertices.vertex(pose, x, y, z)
                .color(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, Math.round(alpha * 255)).endVertex(),
                player.tickCount + partial, attack, opacity);
    }
}
