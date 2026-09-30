package net.kazi.kazimod.renderers.items;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.models.items.PairedSwordsMesh;
import net.kazi.kazimod.abilities.SupaRework.EnhancementAbility;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.tileentity.ItemStackTileEntityRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class PairedSwordsItemRenderer extends ItemStackTileEntityRenderer {
    private static final ThreadLocal<Boolean> ENHANCED = new ThreadLocal<>();

    /** Holder-dependent model state is scoped to one render, never saved into a transferable item. */
    public static void withHolder(LivingEntity holder, Runnable render) {
        Boolean previous = ENHANCED.get();
        ENHANCED.set(EnhancementAbility.hasFirstSwordEnhancement(holder));
        try {
            render.run();
        } finally {
            if (previous == null) ENHANCED.remove();
            else ENHANCED.set(previous);
        }
    }
    // Vanilla material keeps the model resource-pack compatible, with no generated textures to load.
    private static final RenderType METAL = RenderType.entityCutoutNoCull(
            new ResourceLocation("minecraft", "textures/block/white_concrete.png"));

    @Override
    public void renderByItem(ItemStack item, TransformType transform, MatrixStack stack,
                             IRenderTypeBuffer buffers, int light, int overlay) {
        stack.pushPose();
        // ItemRenderer offsets built-in models by -0.5; the mesh is already grip-centered.
        stack.translate(0.5, 0.5, 0.5);
        boolean left = transform == TransformType.FIRST_PERSON_LEFT_HAND
                || transform == TransformType.THIRD_PERSON_LEFT_HAND;
        boolean firstPerson = transform == TransformType.FIRST_PERSON_LEFT_HAND
                || transform == TransformType.FIRST_PERSON_RIGHT_HAND;
        boolean thirdPerson = transform == TransformType.THIRD_PERSON_LEFT_HAND
                || transform == TransformType.THIRD_PERSON_RIGHT_HAND;
        if (firstPerson || thirdPerson) {
            // The JSON supplies vanilla item/handheld transforms, including left-hand
            // mirroring. Align our upright, grip-centered mesh to a vanilla sword's
            // lower-left grip and diagonal blade before those transforms are applied.
            stack.translate(-0.25, -0.25, 0);
            stack.mulPose(Vector3f.ZP.rotationDegrees(-45));
            // Preserve the requested model sizes; the JSON already supplies these factors.
            float scale = firstPerson ? 0.465F / 0.68F : 0.9F / 0.85F;
            stack.scale(scale, scale, scale);
            blade(item, !left, stack, buffers, light, overlay);
        } else {
            // Both swords appear together in the inventory, item frames and dropped stack.
            float scale = transform == TransformType.GROUND ? 0.28F : 0.43F;
            stack.scale(scale, scale, scale);
            stack.translate(0, -0.60, 0);
            for (int side : new int[]{-1, 1}) {
                stack.pushPose();
                stack.translate(side * 0.26, 0, side * 0.045);
                stack.mulPose(Vector3f.ZP.rotationDegrees(side * -12));
                stack.mulPose(Vector3f.YP.rotationDegrees(-18));
                blade(item, side < 0, stack, buffers, light, overlay);
                stack.popPose();
            }
        }
        stack.popPose();
    }

    private static void blade(ItemStack item, boolean dark, MatrixStack stack,
                               IRenderTypeBuffer buffers, int light, int overlay) {
        IVertexBuilder out = ItemRenderer.getFoilBufferDirect(buffers, METAL, true, item.hasFoil());
        MatrixStack.Entry pose = stack.last();
        PairedSwordsMesh.emit(dark, Boolean.TRUE.equals(ENHANCED.get()), (x, y, z, nx, ny, nz, color) ->
                out.vertex(pose.pose(), x, y, z)
                        .color((color >> 16) & 255, (color >> 8) & 255, color & 255, 255)
                        .uv(0.5F, 0.5F).overlayCoords(overlay).uv2(light)
                        .normal(pose.normal(), nx, ny, nz).endVertex());
    }
}
