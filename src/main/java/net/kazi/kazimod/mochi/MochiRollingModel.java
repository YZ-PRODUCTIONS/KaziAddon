package net.kazi.kazimod.mochi;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.vector.Vector3f;
import xyz.pixelatedw.mineminenomi.api.morph.MorphModel;

public final class MochiRollingModel extends MorphModel<LivingEntity> {
    private final boolean carved;private float age,speed;private boolean far;
    public MochiRollingModel(boolean carved){super(0);this.carved=carved;}
    @Override public void setupAnim(LivingEntity e,float swing,float amount,float age,float yaw,float pitch) {
        this.age=age;this.speed=e.isSprinting()?.85F:e.getDeltaMovement().lengthSqr()>.1?.65F:.25F;far=!MochiEffects.detailed(e);
    }
    @Override public void renderToBuffer(MatrixStack s,IVertexBuilder unused,int light,int overlay,float r,float g,float b,float alpha) {
        s.pushPose();s.translate(0,carved?1.06:.32,0);s.mulPose(Vector3f.YP.rotationDegrees(90));s.mulPose(Vector3f.ZP.rotation(age*speed));
        MochiMesh mesh=new MochiMesh(s,Minecraft.getInstance().renderBuffers().bufferSource(),age,far,light);
        mesh.ring(carved?.34:1.05,carved?.16:.40,carved?MochiMesh.CREAM:MochiMesh.HAKI,!carved);
        s.popPose();
    }
    @Override public void renderFirstPersonArm(MatrixStack s,IVertexBuilder b,int l,int o,float r,float g,float blue,float a,HandSide side){}
    @Override public void renderFirstPersonLeg(MatrixStack s,IVertexBuilder b,int l,int o,float r,float g,float blue,float a,HandSide side){}
}
