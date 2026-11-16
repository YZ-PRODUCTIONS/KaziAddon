package net.kazi.kazimod.events.handlers;

import com.mojang.blaze3d.matrix.MatrixStack;
import java.util.List;
import net.kazi.kazimod.KaziMod;
import net.kazi.kazimod.items.PairedSwordsItem;
import net.kazi.kazimod.renderers.items.PairedSwordsItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.HeldItemLayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.vector.Vector3f;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.api.helpers.RendererHelper;
import xyz.pixelatedw.mineminenomi.mixins.client.ILivingRendererMixin;

/** Overrides only the held-item visuals for a pair; all ordinary items retain their original layer. */
public final class PairedSwordsLayer extends LayerRenderer<AbstractClientPlayerEntity, PlayerModel<AbstractClientPlayerEntity>> {
    private final LayerRenderer<AbstractClientPlayerEntity, PlayerModel<AbstractClientPlayerEntity>> original;

    private PairedSwordsLayer(PlayerRenderer renderer,
                              LayerRenderer<AbstractClientPlayerEntity, PlayerModel<AbstractClientPlayerEntity>> original) {
        super(renderer);
        this.original = original;
    }

    @SuppressWarnings("unchecked")
    public static void install(PlayerRenderer renderer) {
        // Reuse Mine Mine no Mi's existing accessor: no new mixin or equipment swap.
        List<LayerRenderer<AbstractClientPlayerEntity, PlayerModel<AbstractClientPlayerEntity>>> layers =
                ((ILivingRendererMixin<AbstractClientPlayerEntity, PlayerModel<AbstractClientPlayerEntity>>)
                        (Object)renderer).getLayers();
        for (LayerRenderer<?, ?> layer : layers) {
            if (layer instanceof PairedSwordsLayer) return;
        }
        for (int i = 0; i < layers.size(); i++) {
            if (layers.get(i).getClass() == HeldItemLayer.class) {
                layers.set(i, new PairedSwordsLayer(renderer, layers.get(i)));
                return;
            }
        }
        KaziMod.LOGGER.warn("Could not install paired-sword visuals: player renderer has no vanilla held-item layer");
    }

    @Override
    public void render(MatrixStack stack, IRenderTypeBuffer buffers, int light, AbstractClientPlayerEntity player,
                        float walk, float walkAmount, float partial, float age, float yaw, float pitch) {
        PairedSwordsItemRenderer.withHolder(player, () -> renderHeldItems(
                stack, buffers, light, player, walk, walkAmount, partial, age, yaw, pitch));
    }

    private void renderHeldItems(MatrixStack stack, IRenderTypeBuffer buffers, int light, AbstractClientPlayerEntity player,
                                  float walk, float walkAmount, float partial, float age, float yaw, float pitch) {
        ItemStack pair;
        if (player.isSpectator() || player.hasEffect(ModEffects.NO_HANDS.get())) {
            original.render(stack, buffers, light, player, walk, walkAmount, partial, age, yaw, pitch);
            return;
        } else if (PairedSwordsItem.isPair(player.getMainHandItem())) {
            pair = player.getMainHandItem();
        } else if (PairedSwordsItem.isPair(player.getOffhandItem()) && player.getMainHandItem().isEmpty()) {
            pair = player.getOffhandItem();
        } else {
            original.render(stack, buffers, light, player, walk, walkAmount, partial, age, yaw, pitch);
            return;
        }
        stack.pushPose();
        if (getParentModel().young) {
            stack.translate(0, 0.75, 0);
            stack.scale(0.5F, 0.5F, 0.5F);
        }
        renderBlade(stack, buffers, light, player, pair, HandSide.RIGHT);
        renderBlade(stack, buffers, light, player, pair, HandSide.LEFT);
        stack.popPose();
    }

    private void renderBlade(MatrixStack stack, IRenderTypeBuffer buffers, int light,
                               AbstractClientPlayerEntity player, ItemStack pair, HandSide arm) {
        boolean left = arm == HandSide.LEFT;
        TransformType transform = left ? TransformType.THIRD_PERSON_LEFT_HAND : TransformType.THIRD_PERSON_RIGHT_HAND;
        stack.pushPose();
        getParentModel().translateToHand(arm, stack);
        stack.mulPose(Vector3f.XP.rotationDegrees(-90));
        stack.mulPose(Vector3f.YP.rotationDegrees(180));
        stack.translate((left ? -1 : 1) / 16.0, 0.125, -0.625);
        // Preserve the held-weapon animation hook normally applied by MMNM's vanilla-layer mixin.
        RendererHelper.animationHeldItem(player, pair, transform, arm, stack, buffers, light);
        Minecraft.getInstance().getItemInHandRenderer().renderItem(player, pair,
                transform, left, stack, buffers, light);
        stack.popPose();
    }
}
