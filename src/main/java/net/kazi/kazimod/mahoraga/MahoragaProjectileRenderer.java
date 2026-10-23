package net.kazi.kazimod.mahoraga;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.*;

public final class MahoragaProjectileRenderer extends EntityRenderer<MahoragaProjectile> {
    public MahoragaProjectileRenderer(EntityRendererManager manager){super(manager);shadowRadius=0;}
    @Override public void render(MahoragaProjectile e,float yaw,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light){
        float age=e.tickCount+partial;Vector3d motion=e.getDeltaMovement();
        stack.pushPose();
        if(motion.lengthSqr()>.0001){
            stack.mulPose(Vector3f.YP.rotationDegrees((float)Math.toDegrees(Math.atan2(motion.x,motion.z))));
            stack.mulPose(Vector3f.XP.rotationDegrees((float)-Math.toDegrees(Math.atan2(motion.y,Math.sqrt(motion.x*motion.x+motion.z*motion.z)))));
        }
        if(e.impacted()){
            float p=Math.min(1,(e.impactTicks()+partial)/8F);
            MahoragaVfx.pressure(stack,buffers,age,1+p*3,1-p);
        }else if(e.kind()==0){
            MahoragaVfx.pressure(stack,buffers,age,.6F,.2F);
            stack.mulPose(Vector3f.XP.rotationDegrees(age*17));stack.mulPose(Vector3f.ZP.rotationDegrees(age*9));
            stack.scale(1.35F,1.35F,1.35F);stack.translate(-.5,-.5,-.5);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(e.block(),stack,buffers,light,OverlayTexture.NO_OVERLAY);
        }else MahoragaVfx.pressure(stack,buffers,age,1.3F,1);
        stack.popPose();
    }
    @Override public ResourceLocation getTextureLocation(MahoragaProjectile e){return AtlasTexture.LOCATION_BLOCKS;}
}
