package net.kazi.kazimod.mochi;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.MrMagicalCart.cartaddon.entities.mobs.ability.KuriMochiEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;

public final class MochiKuriRenderer extends EntityRenderer<KuriMochiEntity> {
    public MochiKuriRenderer(EntityRendererManager manager){super(manager);}
    @Override public boolean shouldRender(KuriMochiEntity e,ClippingHelper camera,double x,double y,double z){return e.distanceToSqr(x,y,z)<128*128&&camera.isVisible(e.getBoundingBox().inflate(4));}
    @Override public void render(KuriMochiEntity e,float yaw,float partial,MatrixStack s,IRenderTypeBuffer b,int light) {
        float age=(e.level.getGameTime()%100000)+partial;
        s.pushPose();s.translate(0,1.5,0);s.mulPose(Vector3f.YP.rotationDegrees(180-MathHelper.rotLerp(partial,e.yBodyRotO,e.yBodyRot)));
        MochiMesh mesh=new MochiMesh(s,b,age,!MochiEffects.detailed(e),light);
        mesh.ring(1.25,.36,MochiMesh.CREAM,true);mesh.ringDrips(1.25,.36,6,e.getId());s.popPose();
    }
    @Override public ResourceLocation getTextureLocation(KuriMochiEntity e){return new ResourceLocation("cartaddon","textures/models/kuri_mochi.png");}
}
