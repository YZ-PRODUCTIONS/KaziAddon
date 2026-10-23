package net.kazi.kazimod.bomu;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;

public final class BomuVfxRenderer extends EntityRenderer<BomuVfxEntity> {
    public static final ResourceLocation UNUSED = new ResourceLocation("minecraft", "textures/misc/white.png");
    public BomuVfxRenderer(EntityRendererManager manager) { super(manager); shadowRadius = 0; }

    @Override public void render(BomuVfxEntity e, float yaw, float partial, MatrixStack stack, IRenderTypeBuffer buffers, int light) {
        float age=e.tickCount+partial;
        boolean far=entityRenderDispatcher.camera.getPosition().distanceToSqr(e.position())>64*64;
        stack.pushPose();
        if(e.kind()==BomuVfxEntity.HANDS) {
            Entity owner=e.owner();
            if(owner!=null) {
                stack.translate(MathHelper.lerp(partial,owner.xOld,owner.getX())-MathHelper.lerp(partial,e.xOld,e.getX()),
                        MathHelper.lerp(partial,owner.yOld,owner.getY())-MathHelper.lerp(partial,e.yOld,e.getY()),
                        MathHelper.lerp(partial,owner.zOld,owner.getZ())-MathHelper.lerp(partial,e.zOld,e.getZ()));
                float turn=owner instanceof LivingEntity?MathHelper.rotLerp(partial,((LivingEntity)owner).yBodyRotO,((LivingEntity)owner).yBodyRot):owner.yRot;
                stack.mulPose(Vector3f.YP.rotationDegrees(-turn));
                for(int side:new int[]{-1,1}) {
                    float swing=0;
                    if(owner instanceof LivingEntity) {
                        LivingEntity living=(LivingEntity)owner;
                        int mainSide=living.getMainArm()==net.minecraft.util.HandSide.RIGHT?-1:1;
                        if(side==mainSide) swing=MathHelper.sin(MathHelper.sqrt(living.getAttackAnim(partial))*(float)Math.PI);
                    }
                    stack.pushPose();stack.translate(side*.4,owner.getBbHeight()*.4+swing*.65,.12+swing*.65);
                    BomuMesh.render(stack,buffers,e.kind(),age,e.life(),e.radius(),e.getId()+side,far);
                    stack.popPose();
                }
            }
        } else {
            if(e.kind()==BomuVfxEntity.JET) {
                stack.mulPose(Vector3f.YP.rotationDegrees(e.yaw()));
                stack.mulPose(Vector3f.XP.rotationDegrees(e.pitch()));
            }
            BomuMesh.render(stack,buffers,e.kind(),age,e.life(),e.radius(),e.getId(),far);
        }
        stack.popPose();
    }
    @Override public ResourceLocation getTextureLocation(BomuVfxEntity entity) { return UNUSED; }
}
