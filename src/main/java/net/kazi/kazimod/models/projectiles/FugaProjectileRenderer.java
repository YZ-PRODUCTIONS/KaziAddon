package net.kazi.kazimod.models.projectiles;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.entities.projectiles.FugaProjectile;
import net.kazi.kazimod.models.abilities.KamaVfxMesh;
import net.kazi.kazimod.renderers.entities.KokuVfxRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;

public class FugaProjectileRenderer
extends EntityRenderer<FugaProjectile> {
    public FugaProjectileRenderer(EntityRendererManager manager) {
        super(manager);
    }

    public void render(FugaProjectile entity, float yaw, float partial, MatrixStack stack, IRenderTypeBuffer buffer, int light) {
        stack.pushPose();
        if (entity.isFinished()) {
            KamaVfxMesh.pillar(KokuVfxRenderer.sink(stack, buffer), entity.getEffectAge(partial));
        } else {
            stack.mulPose(Vector3f.YP.rotationDegrees(MathHelper.rotLerp((float)partial, (float)entity.yRotO, (float)entity.yRot)));
            stack.mulPose(Vector3f.XP.rotationDegrees(-MathHelper.lerp((float)partial, (float)entity.xRotO, (float)entity.xRot)));
            KamaVfxMesh.arrow(KokuVfxRenderer.sink(stack, buffer), (float)entity.tickCount + partial, Math.max(0.8f, entity.getSize()));
        }
        stack.popPose();
    }

    public ResourceLocation getTextureLocation(FugaProjectile entity) {
        return KokuVfxRenderer.UNUSED_TEXTURE;
    }
}
