package net.kazi.kazimod.pika;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.*;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;

public final class PikaBeamRenderer {
    public static void render(LightningEntity beam,float partial,MatrixStack stack,IRenderTypeBuffer buffers) {
        if(beam.isInvisible()||beam.getSegments()<0)return;
        int kind=((PikaBeamVisuals)beam).kazi$pikaKind();Minecraft mc=Minecraft.getInstance();
        float age=beam.tickCount+partial,alpha=MathHelper.clamp((beam.getLife()-partial)/5,0,1);
        float radius=Math.max(.045F,beam.getSize()*(kind==2?1:1.6F));
        float length=beam.getLength()*Math.min(1,age/2);
        if(kind==2)alpha*=.55F;
        if(mc.player!=null&&beam.getThrower()==mc.player&&mc.options.getCameraType().isFirstPerson())alpha*=.55F;
        boolean far=beam.distanceToSqr(mc.gameRenderer.getMainCamera().getPosition())>80*80;
        Quaternion camera=mc.getEntityRenderDispatcher().cameraOrientation();
        PikaMesh.flare(stack,buffers,camera,kind==2?.65F:1.1F,alpha,0,far);
        stack.pushPose();stack.mulPose(Vector3f.YN.rotationDegrees(beam.yRot));stack.mulPose(Vector3f.XP.rotationDegrees(beam.xRot));
        PikaMesh.beam(stack,buffers,radius,length,age,alpha,far);
        stack.popPose();
    }
}
