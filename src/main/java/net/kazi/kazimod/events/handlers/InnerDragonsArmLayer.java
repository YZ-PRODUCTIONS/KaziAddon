package net.kazi.kazimod.events.handlers;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.client.nagi.InnerDragonsArmRenderer;
import net.kazi.kazimod.client.nagi.InnerDragonsCutsceneClient;
import net.kazi.kazimod.client.nagi.InnerDragonsCutsceneVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShootableItem;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The replacement is cosmetic: equipment, attacks and the unaffected left arm keep working. */
@Mod.EventBusSubscriber(modid = "kazimod", value = Dist.CLIENT)
public final class InnerDragonsArmLayer
        extends LayerRenderer<AbstractClientPlayerEntity, PlayerModel<AbstractClientPlayerEntity>> {
    public InnerDragonsArmLayer(PlayerRenderer renderer) { super(renderer); }

    @Override public void render(MatrixStack stack, IRenderTypeBuffer buffers, int light, AbstractClientPlayerEntity player,
                                 float walk, float walkAmount, float partial, float age, float yaw, float pitch) {
        if (player.isInvisible() || player.isSpectator() || !InnerDragonsCutsceneVisuals.isArmMissing(player)) return;
        stack.pushPose();
        try {
            if (getParentModel().young) { stack.translate(0, .75, 0); stack.scale(.5F, .5F, .5F); }
            InnerDragonsArmRenderer.attached(stack, buffers, getParentModel(), player, partial);
        } finally { stack.popPose(); }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void hand(RenderHandEvent event) {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator() || !InnerDragonsCutsceneVisuals.isArmMissing(player)) return;
        if (InnerDragonsCutsceneClient.active()) { event.setCanceled(true); return; }
        Hand right = player.getMainArm() == HandSide.RIGHT ? Hand.MAIN_HAND : Hand.OFF_HAND;
        boolean rightPass = event.getHand() == right;
        if (rightPass) event.setCanceled(true);
        if (player.isInvisible()) return;
        // Bows and charged crossbows can suppress the right-hand pass altogether. Draw
        // its replacement once from the surviving left pass without canceling that hand.
        if (!rightPass && vanillaRendersHand(player, right)) return;
        Hand swinging = player.swingingArm == null ? Hand.MAIN_HAND : player.swingingArm;
        float swing = swinging == right ? player.getAttackAnim(event.getPartialTicks()) : 0;
        InnerDragonsArmRenderer.firstPerson(event.getMatrixStack(), event.getBuffers(), player,
                event.getPartialTicks(), swing, rightPass ? event.getEquipProgress() : 0);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void arm(RenderArmEvent event) {
        // A two-handed map held in the LEFT main hand renders both arms inside one
        // left-hand pass. Hide that nested right arm too; the head is drawn only above.
        if (event.getArm() == HandSide.RIGHT && InnerDragonsCutsceneVisuals.isArmMissing(event.getPlayer()))
            event.setCanceled(true);
    }

    private static boolean vanillaRendersHand(ClientPlayerEntity player, Hand hand) {
        boolean main = true, off = true;
        ItemStack mainItem = player.getMainHandItem(), offItem = player.getOffhandItem();
        if (player.isUsingItem()) {
            if (player.getUseItem().getItem() instanceof ShootableItem) {
                main = player.getUsedItemHand() == Hand.MAIN_HAND;
                off = !main;
            }
            if (player.getUsedItemHand() == Hand.MAIN_HAND && chargedCrossbow(offItem)) off = false;
        } else {
            if (chargedCrossbow(mainItem)) off = false;
            if (chargedCrossbow(offItem)) { main = !mainItem.isEmpty(); off = !main; }
        }
        return hand == Hand.MAIN_HAND ? main : off;
    }

    private static boolean chargedCrossbow(ItemStack stack) {
        return stack.getItem() == Items.CROSSBOW && CrossbowItem.isCharged(stack);
    }
}
