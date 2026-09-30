package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.effects.LaserBeamProfile;
import net.kazi.kazimod.effects.LaserBeamVisualData;
import net.kazi.kazimod.models.abilities.LaserBeamMesh;
import net.kazi.kazimod.renderers.entities.KokuVfxRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.util.math.vector.Vector3f;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;

public final class LaserBeamVfxRenderer {
    private LaserBeamVfxRenderer() {
    }

    public static void render(LightningEntity beam, float partial, MatrixStack stack, IRenderTypeBuffer buffer) {
        if (beam.tickCount < 1 || beam.getSegments() < 0) {
            return;
        }
        LaserBeamVisualData data = (LaserBeamVisualData)beam;
        float age = (float)beam.tickCount + partial;
        float length = beam.getLength() * Math.min(1.0f, age / 2.0f);
        float opacity = LaserBeamProfile.opacity((float)beam.getLife() - partial, beam.getMaxLife()) * (float)beam.getAlpha() / 255.0f;
        stack.pushPose();
        stack.mulPose(Vector3f.YN.rotationDegrees(beam.yRot));
        stack.mulPose(Vector3f.XP.rotationDegrees(beam.xRot));
        stack.translate(0.0, 0.0, 0.1);
        // Match the original beam's configured width, independently of damage.
        float widthScale = LaserBeamProfile.widthScale(beam.getSize(), data.kazimod$getBeamDamage());
        stack.scale(widthScale, widthScale, 1.0F);
        LaserBeamMesh.render(KokuVfxRenderer.sink(stack, buffer), length, age, data.kazimod$getBeamDamage(), beam.getColor(), opacity);
        stack.popPose();
    }
}
