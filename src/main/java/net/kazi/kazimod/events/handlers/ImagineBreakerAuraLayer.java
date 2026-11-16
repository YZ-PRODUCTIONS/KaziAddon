package net.kazi.kazimod.events.handlers;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.client.nagi.ImagineBreakerAuraRenderer;
import net.kazi.kazimod.client.nagi.InnerDragonsCutsceneVisuals;
import net.kazi.kazimod.renderers.entities.VfxDetail;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.util.HandSide;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Illusion Shatter's aura follows the right arm; mesh names retain their original Imagine Breaker prefix. */
@Mod.EventBusSubscriber(modid = "kazimod", value = Dist.CLIENT)
public final class ImagineBreakerAuraLayer
        extends LayerRenderer<AbstractClientPlayerEntity, PlayerModel<AbstractClientPlayerEntity>> {
    private static final PlayerModel<AbstractClientPlayerEntity> FIRST_PERSON_DEFAULT = new PlayerModel<>(0, false);
    private static final PlayerModel<AbstractClientPlayerEntity> FIRST_PERSON_SLIM = new PlayerModel<>(0, true);

    public ImagineBreakerAuraLayer(PlayerRenderer renderer) { super(renderer); }

    @Override
    public void render(MatrixStack stack, IRenderTypeBuffer buffers, int light, AbstractClientPlayerEntity player,
                       float walk, float walkAmount, float partial, float age, float yaw, float pitch) {
        if (!ImagineBreakerAuraRenderer.isActive(player) || InnerDragonsCutsceneVisuals.isArmMissing(player)) return;
        stack.pushPose();
        try {
            if (getParentModel().young) {
                stack.translate(0, .75, 0);
                stack.scale(.5F, .5F, .5F);
            }
            ImagineBreakerAuraRenderer.render(stack, buffers, getParentModel(), player, partial,
                    VfxDetail.level(player, Minecraft.getInstance().getEntityRenderDispatcher(), 1), false);
        } finally {
            stack.popPose();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderFirstPersonArm(RenderArmEvent event) {
        AbstractClientPlayerEntity player = event.getPlayer();
        if (event.getArm() != HandSide.RIGHT || !ImagineBreakerAuraRenderer.isActive(player)) return;
        PlayerModel<AbstractClientPlayerEntity> model = "slim".equals(player.getModelName())
                ? FIRST_PERSON_SLIM : FIRST_PERSON_DEFAULT;
        // RenderArmEvent fires before vanilla sets up the hand. Match that setup on a
        // private model, without altering the player renderer or replacing the vanilla arm.
        model.attackTime = 0;
        model.crouching = false;
        model.swimAmount = 0;
        model.setupAnim(player, 0, 0, 0, 0, 0);
        model.rightArm.xRot = 0;
        ImagineBreakerAuraRenderer.render(event.getPoseStack(), event.getMultiBufferSource(), model,
                player, Minecraft.getInstance().getFrameTime(), VfxDetail.levelForDistance(0), true);
    }
}
