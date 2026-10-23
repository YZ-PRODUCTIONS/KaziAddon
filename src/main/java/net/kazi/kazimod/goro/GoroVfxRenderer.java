package net.kazi.kazimod.goro;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.*;
import xyz.pixelatedw.mineminenomi.api.math.VectorHelper;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;

public final class GoroVfxRenderer extends EntityRenderer<GoroVfxEntity> {
    public GoroVfxRenderer(EntityRendererManager manager) { super(manager);shadowRadius=0; }
    @Override public void render(GoroVfxEntity fx,float yaw,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light) {
        Entity owner=fx.owner(); Vector3d origin=lerp(fx,partial),position=origin;
        int kind=fx.kind();float age=fx.tickCount+partial;
        boolean burst=kind==GoroVfxEntity.BURST || kind==GoroVfxEntity.IMPACT;
        float progress=MathHelper.clamp(age/fx.life(),0,1),alpha=burst?(float)Math.pow(1-progress,1.3):1;
        if(owner!=null) {
            position=lerp(owner,partial);
            if(kind==GoroVfxEntity.HAND && owner instanceof LivingEntity) {
                LivingEntity living=(LivingEntity)owner;
                position=VectorHelper.calculateRotationBasedOffsetPosition(position,MathHelper.rotLerp(partial,living.yBodyRotO,living.yBodyRot),.5,1.15,.8);
            } else position=position.add(0,owner.getBbHeight()*.5,0);
            if(owner==Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson())alpha*=.16F;
        }
        stack.pushPose();stack.translate(position.x-origin.x,position.y-origin.y,position.z-origin.z);
        GoroMesh.effect(stack,buffers,kind,fx.radius(),age,progress,alpha,fx.getId());stack.popPose();
    }
    public static void lightning(LightningEntity bolt,float partial,MatrixStack stack,IRenderTypeBuffer buffers) {
        int kind=((GoroBoltVisuals)bolt).kazi$goroKind();
        if(kind<=0 || bolt.isInvisible())return;
        float fade=MathHelper.clamp((bolt.getLife()-partial)/10F,0,1);
        stack.pushPose();stack.mulPose(Vector3f.YN.rotationDegrees(bolt.yRot));
        stack.mulPose(Vector3f.XP.rotationDegrees(bolt.xRot+90));
        boolean far=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(bolt.position())>128*128;
        GoroMesh.beam(stack,buffers,Math.max(.12F,bolt.getSize()*2.2F),bolt.getLength(),bolt.tickCount+partial,fade,bolt.getId(),kind==1,far);
        stack.popPose();
    }
    private static Vector3d lerp(Entity e,float p) { return new Vector3d(MathHelper.lerp(p,e.xOld,e.getX()),MathHelper.lerp(p,e.yOld,e.getY()),MathHelper.lerp(p,e.zOld,e.getZ())); }
    @Override public ResourceLocation getTextureLocation(GoroVfxEntity entity) { return null; }
}
