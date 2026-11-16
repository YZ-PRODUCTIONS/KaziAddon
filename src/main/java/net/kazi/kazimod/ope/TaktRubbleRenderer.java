package net.kazi.kazimod.ope;

import java.util.Random;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.MrMagicalCart.cartaddon.entities.projectiles.opeextra.TaktTossProjectile;
import net.minecraft.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;

public final class TaktRubbleRenderer extends EntityRenderer<TaktTossProjectile> {
    public TaktRubbleRenderer(EntityRendererManager manager) { super(manager); shadowRadius = 2.5F; }
    @Override public ResourceLocation getTextureLocation(TaktTossProjectile entity) { return AtlasTexture.LOCATION_BLOCKS; }
    @Override public void render(TaktTossProjectile entity, float yaw, float partial, MatrixStack stack, IRenderTypeBuffer buffers, int light) {
        int[] palette = ((TaktRubbleData)entity).kazi$rubblePalette();
        Random random = new Random(entity.getUUID().getLeastSignificantBits());
        float age = entity.tickCount + partial;
        stack.pushPose(); stack.translate(0, entity.getBbHeight() * .5, 0);
        stack.mulPose(Vector3f.YP.rotationDegrees(age * 1.1F));
        stack.mulPose(Vector3f.XP.rotationDegrees(age * .36F));
        double radius = Math.max(1.8, entity.getBbWidth() * .46);
        int count = entity.distanceToSqr(entityRenderDispatcher.camera.getPosition()) > 48 * 48 ? 20 : 38;
        for (int i = 0; i < count; i++) {
            double y = 1 - 2 * (i + .5) / count, angle = i * 2.39996323;
            double circle = Math.sqrt(1 - y * y);
            double r = radius * (.78 + random.nextDouble() * .16);
            float size = (float)(radius * (.46 + random.nextDouble() * .18));
            BlockState state = palette.length == 0 ? Blocks.STONE.defaultBlockState() : Block.stateById(palette[i % palette.length]);
            stack.pushPose(); stack.translate(Math.cos(angle) * circle * r, y * r, Math.sin(angle) * circle * r);
            stack.mulPose(Vector3f.YP.rotationDegrees(random.nextFloat() * 90));
            stack.mulPose(Vector3f.XP.rotationDegrees(random.nextFloat() * 36 - 18));
            stack.scale(size, size, size); stack.translate(-.5, -.5, -.5);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, stack, buffers, light, OverlayTexture.NO_OVERLAY);
            stack.popPose();
        }
        stack.popPose();
        super.render(entity, yaw, partial, stack, buffers, light);
    }
}
