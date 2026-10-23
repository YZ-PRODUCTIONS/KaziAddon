package net.kazi.kazimod.mixin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.models.abilities.GuraVfxMesh;
import net.kazi.kazimod.renderers.entities.KokuVfxRenderer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import xyz.pixelatedw.mineminenomi.events.passives.GuraPassiveEvents;
import xyz.pixelatedw.mineminenomi.models.abilities.SphereModel;

@Mixin(value = GuraPassiveEvents.class, remap = false)
public abstract class GuraBubbleRendererMixin {
    @Redirect(method = "onEntityRendered", at = @At(value = "INVOKE", target =
            "Lxyz/pixelatedw/mineminenomi/models/abilities/SphereModel;renderToBuffer(Lcom/mojang/blaze3d/matrix/MatrixStack;Lcom/mojang/blaze3d/vertex/IVertexBuilder;IIFFFF)V", remap = true))
    private static void kazimod$worldBubble(SphereModel<?> model, MatrixStack stack, IVertexBuilder old,
            int light, int overlay, float r, float g, float b, float a, RenderLivingEvent.Post<?, ?> event) {
        GuraVfxMesh.bubble(KokuVfxRenderer.sink(stack, event.getBuffers()), .5F,
                event.getEntity().tickCount + event.getPartialRenderTick(), 1);
    }
    @Redirect(method = "onHandRendering", at = @At(value = "INVOKE", target =
            "Lxyz/pixelatedw/mineminenomi/models/abilities/SphereModel;renderToBuffer(Lcom/mojang/blaze3d/matrix/MatrixStack;Lcom/mojang/blaze3d/vertex/IVertexBuilder;IIFFFF)V", remap = true))
    private static void kazimod$handBubble(SphereModel<?> model, MatrixStack stack, IVertexBuilder old,
            int light, int overlay, float r, float g, float b, float a, RenderHandEvent event) {
        float age = Minecraft.getInstance().player == null ? 0 : Minecraft.getInstance().player.tickCount + event.getPartialTicks();
        GuraVfxMesh.bubble(KokuVfxRenderer.sink(stack, event.getBuffers()), .5F, age, 1);
    }
}
