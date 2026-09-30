package net.kazi.kazimod.renderers.entities;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.entities.projectiles.ReplicatedSwordEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;

/** Layered cyan projection seals; faceted swords emerge before becoming blue comets. */
public class ReplicatedSwordRenderer extends EntityRenderer<ReplicatedSwordEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/block/white_wool.png");
    public ReplicatedSwordRenderer(EntityRendererManager manager) { super(manager); }
    @Override public ResourceLocation getTextureLocation(ReplicatedSwordEntity sword) {
        return TEXTURE;
    }
    @Override public boolean shouldRender(ReplicatedSwordEntity sword, ClippingHelper frustum,
                                           double x, double y, double z) {
        return VfxDetail.visible(sword, frustum, x, y, z, sword.isFired() ? 7 : 2.5);
    }
    @Override public void render(ReplicatedSwordEntity sword, float yaw, float partial, MatrixStack stack,
                                  IRenderTypeBuffer buffer, int light) {
        boolean fired = sword.isFired();
        float age = sword.getVisualAge(partial);
        int detail = VfxDetail.level(sword, this.entityRenderDispatcher, fired ? 6 : 2);
        int seed = sword.getId();
        stack.pushPose();
        if (fired) GaeBolgVfxRenderer.orient(stack, sword.getDeltaMovement());
        else {
            LivingEntity caster = sword.getThrower();
            if (caster != null) GaeBolgVfxRenderer.orient(stack, caster.getViewVector(partial));
            else {
                stack.mulPose(Vector3f.YP.rotationDegrees(sword.yRot));
                stack.mulPose(Vector3f.XP.rotationDegrees(sword.xRot));
            }
        }
        // A modest roll exposes the broad blade faces instead of presenting only their thin edge.
        stack.mulPose(Vector3f.ZP.rotationDegrees(25 + Math.floorMod(seed, 3) * 25));
        Matrix4f pose = stack.last().pose();
        IVertexBuilder metal = buffer.getBuffer(RealityMarbleGearModel.METAL);
        ReplicatedSwordGeometry.sword(fired ? 1 : ReplicatedSwordGeometry.formation(age), seed,
                (x,y,z,r,g,b,a) -> metal.vertex(pose,(float)x,(float)y,(float)z).color(r,g,b,a).endVertex());
        IVertexBuilder glow = buffer.getBuffer(CaladbolgVisualGeometry.ENERGY);
        ReplicatedSwordGeometry.Sink energy = (x,y,z,r,g,b,a) ->
                glow.vertex(pose,(float)x,(float)y,(float)z).color(r,g,b,a).endVertex();
        if (fired) ReplicatedSwordGeometry.flight(sword.getFlightVisualAge(partial), seed, detail, energy);
        else ReplicatedSwordGeometry.portal(age, seed, detail, energy);
        stack.popPose();
    }
}
