package net.kazi.kazimod.yami;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;

public final class LiberationBlockRenderer extends EntityRenderer<LiberationBlockProjectile> {
    public LiberationBlockRenderer(EntityRendererManager manager){super(manager);}
    @Override public ResourceLocation getTextureLocation(LiberationBlockProjectile entity){return AtlasTexture.LOCATION_BLOCKS;}
    @Override public void render(LiberationBlockProjectile entity,float yaw,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light) {
        stack.pushPose();float age=entity.tickCount+partial;
        stack.mulPose(Vector3f.YP.rotationDegrees(age*11+entity.getId()*37));
        stack.mulPose(Vector3f.XP.rotationDegrees(age*7));stack.scale(.9F,.9F,.9F);stack.translate(-.5,-.5,-.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(entity.block(),stack,buffers,light,OverlayTexture.NO_OVERLAY);
        stack.popPose();
    }
}
