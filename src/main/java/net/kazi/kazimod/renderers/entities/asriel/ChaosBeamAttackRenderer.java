package net.kazi.kazimod.renderers.entities.asriel;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kazi.kazimod.entities.asriel.ChaosBeamAttackEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;

public final class ChaosBeamAttackRenderer extends EntityRenderer<ChaosBeamAttackEntity> {
    private static final ResourceLocation UNUSED = new ResourceLocation("minecraft", "textures/block/white_wool.png");
    public ChaosBeamAttackRenderer(EntityRendererManager manager) { super(manager); }
    @Override public ResourceLocation getTextureLocation(ChaosBeamAttackEntity entity) { return UNUSED; }
    @Override public boolean shouldRender(ChaosBeamAttackEntity entity, ClippingHelper frustum, double x, double y, double z) {
        if (!entity.isInitialized() || !entity.shouldRender(x,y,z)) return false;
        Vector3d end = entity.position().add(entity.getAttackDirection().scale(entity.isFiring() ? entity.getLength() : 0));
        return frustum.isVisible(new AxisAlignedBB(entity.position(), end).inflate(ChaosBeamAttackVisuals.cullingRadius()));
    }
    @Override public void render(ChaosBeamAttackEntity entity, float yaw, float partial, MatrixStack stack, IRenderTypeBuffer buffers, int light) {
        if (!entity.isInitialized()) return;
        int detail = AsrielRenderSupport.lineDetail(entity, entityRenderDispatcher, entity.getAttackDirection(),
                entity.isFiring() ? entity.getLength() : 0, entity.isFiring() ? ChaosBeamAttackVisuals.cullingRadius() : 3);
        stack.pushPose();
        AsrielRenderSupport.orient(stack, entity.getAttackDirection());
        float age = entity.getVisualAge(partial);
        ChaosBeamAttackVisuals.render(AsrielRenderSupport.sink(buffers, stack, entity.hasDarkPalette(), age, .12F),
                age, entity.isFiring(), entity.getLength(), detail);
        stack.popPose();
    }
}
