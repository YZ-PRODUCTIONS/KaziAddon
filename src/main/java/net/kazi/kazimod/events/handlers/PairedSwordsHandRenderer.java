package net.kazi.kazimod.events.handlers;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.items.PairedSwordsItem;
import net.kazi.kazimod.renderers.items.PairedSwordsItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShootableItem;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

/** Companion hand rendering/posing without modifying equipment. */
@Mod.EventBusSubscriber(modid = "kazimod", value = Dist.CLIENT)
public final class PairedSwordsHandRenderer {
    private PairedSwordsHandRenderer() { }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void poseCompanionArm(RenderPlayerEvent.Pre event) {
        PlayerEntity player = event.getPlayer();
        if (player.isSpectator() || player.hasEffect(ModEffects.NO_HANDS.get())) return;
        HandSide companion;
        if (PairedSwordsItem.isPair(player.getMainHandItem())) {
            companion = player.getMainArm().getOpposite();
        } else if (PairedSwordsItem.isPair(player.getOffhandItem()) && player.getMainHandItem().isEmpty()) {
            companion = player.getMainArm();
        } else return;

        // PlayerRenderer has already assigned the equipment poses, but setupAnim has
        // not run yet. Pose the actual arm/sleeve as well as its attached blade. Vanilla
        // resets these poses on every render, so nothing persists after unequipping.
        if (companion == HandSide.LEFT) {
            event.getRenderer().getModel().leftArmPose = BipedModel.ArmPose.ITEM;
        } else {
            event.getRenderer().getModel().rightArmPose = BipedModel.ArmPose.ITEM;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderPairedHands(RenderHandEvent event) {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator() || player.hasEffect(ModEffects.NO_HANDS.get())) return;

        ItemStack mainPair = player.getMainHandItem();
        if (PairedSwordsItem.isPair(mainPair)) {
            // Vanilla omits the offhand pass for a charged crossbow, and omits the
            // main-hand pass while using an offhand bow/crossbow. Draw BOTH blades
            // from the surviving pass, then cancel either original item render.
            Hand visiblePass = player.isUsingItem() && player.getUsedItemHand() == Hand.OFF_HAND
                    && player.getUseItem().getItem() instanceof ShootableItem ? Hand.OFF_HAND : Hand.MAIN_HAND;
            event.setCanceled(true);
            if (event.getHand() != visiblePass) return;
            Hand swinging = player.swingingArm == null ? Hand.MAIN_HAND : player.swingingArm;
            float attack = player.getAttackAnim(event.getPartialTicks());
            renderSwordHand(event, player, mainPair, Hand.MAIN_HAND, swinging == Hand.MAIN_HAND ? attack : 0);
            renderSwordHand(event, player, mainPair, Hand.OFF_HAND, swinging == Hand.OFF_HAND ? attack : 0);
            return;
        }

        // Preserve offhand-only pairing when the main hand is empty; never hide an
        // ordinary main-hand item just because this weapon was placed in the offhand.
        if (PairedSwordsItem.isPair(event.getItemStack())) {
            renderSwordHand(event, player, event.getItemStack(), event.getHand(), event.getSwingProgress());
            event.setCanceled(true);
            return;
        }
        if (!event.getItemStack().isEmpty() || !player.getItemInHand(event.getHand()).isEmpty()) return;
        Hand other = event.getHand() == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
        ItemStack pair = player.getItemInHand(other);
        if (!PairedSwordsItem.isPair(pair)) return;
        renderSwordHand(event, player, pair, event.getHand(), event.getSwingProgress());
        event.setCanceled(true);
    }

    private static void renderSwordHand(RenderHandEvent event, ClientPlayerEntity player,
                                         ItemStack pair, Hand hand, float swing) {
        HandSide arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        int sign = arm == HandSide.RIGHT ? 1 : -1;
        // Vanilla's idle/equip/swing transforms. Calling renderItem directly does NOT fire this event again.
        float root = MathHelper.sqrt(swing);
        MatrixStack stack = event.getMatrixStack();
        stack.pushPose();
        stack.translate(sign * -0.4F * MathHelper.sin(root * (float)Math.PI),
                0.2F * MathHelper.sin(root * (float)Math.PI * 2), -0.2F * MathHelper.sin(swing * (float)Math.PI));
        stack.translate(sign * 0.56, -0.52 - event.getEquipProgress() * 0.6, -0.72);
        stack.mulPose(Vector3f.YP.rotationDegrees(sign * (45 - MathHelper.sin(swing * swing * (float)Math.PI) * 20)));
        stack.mulPose(Vector3f.ZP.rotationDegrees(sign * MathHelper.sin(root * (float)Math.PI) * -20));
        stack.mulPose(Vector3f.XP.rotationDegrees(MathHelper.sin(root * (float)Math.PI) * -80));
        stack.mulPose(Vector3f.YP.rotationDegrees(sign * -45));
        PairedSwordsItemRenderer.withHolder(player, () -> Minecraft.getInstance().getItemInHandRenderer().renderItem(player, pair,
                arm == HandSide.LEFT ? TransformType.FIRST_PERSON_LEFT_HAND : TransformType.FIRST_PERSON_RIGHT_HAND,
                arm == HandSide.LEFT, stack, event.getBuffers(), event.getLight()));
        stack.popPose();
    }
}
