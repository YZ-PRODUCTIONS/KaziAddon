package net.kazi.kazimod.doku;

import com.mojang.blaze3d.matrix.MatrixStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.*;
import xyz.pixelatedw.mineminenomi.models.entities.projectiles.HydraModel;

public final class DokuGasRenderer extends EntityRenderer<DokuGasEntity> {
    private static final ResourceLocation HYDRA=new ResourceLocation("mineminenomi","textures/models/projectiles/hydra_full.png");
    private final HydraModel<Entity> head=new HydraModel<>();
    public DokuGasRenderer(EntityRendererManager manager){
        super(manager);shadowRadius=0;
        head.neck.visible=false;head.spine.visible=false;head.thorn1.visible=false;head.thorn2.visible=false;
        head.thorn3.visible=false;head.thorn4.visible=false;head.thorn5.visible=false;head.thorn6.visible=false;
    }
    @Override public void render(DokuGasEntity gas,float yaw,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light){
        float age=gas.tickCount+partial,alpha=gas.opacity(partial);
        Entity owner=gas.owner();Vector3d origin=lerp(gas,partial),position=owner!=null&&!gas.fading()?lerp(owner,partial):origin;
        double distance=entityRenderDispatcher.camera.getPosition().distanceToSqr(position);
        boolean far=distance>64*64,first=owner==Minecraft.getInstance().player&&Minecraft.getInstance().options.getCameraType().isFirstPerson();
        if(first&&gas.kind()==DokuGasEntity.DEMON)return;
        if(distance<gas.radius()*gas.radius()*.6)alpha*=.38F;
        stack.pushPose();stack.translate(position.x-origin.x,position.y-origin.y,position.z-origin.z);
        if(gas.kind()==DokuGasEntity.ROAD||gas.kind()==DokuGasEntity.PROJECTILE){
            List<Vector3d> points=new ArrayList<>();points.add(new Vector3d(0,gas.kind()==DokuGasEntity.ROAD?-.35:0,0));
            for(Vector3d point:gas.trail())if(point.distanceToSqr(position)<1600)points.add(point.subtract(position));
            if(gas.kind()==DokuGasEntity.ROAD){
                DokuGasMesh.road(stack,buffers,points,gas.radius(),age,gas.red(),alpha);
                if(owner!=null){
                    stack.pushPose();stack.translate(0,-.5,0);stack.mulPose(Vector3f.YP.rotationDegrees(180-MathHelper.rotLerp(partial,owner.yRotO,owner.yRot)));
                    stack.mulPose(Vector3f.XP.rotationDegrees(-MathHelper.lerp(partial,owner.xRotO,owner.xRot)));
                    float scale=gas.red()?3.8F:3;stack.scale(scale,-scale,scale);
                    head.jaw2.xRot=.23F+MathHelper.sin(age*.09F)*.035F;
                    head.renderToBuffer(stack,buffers.getBuffer(RenderType.entityTranslucent(HYDRA)),15728880,OverlayTexture.NO_OVERLAY,1,gas.red()?.22F:1,gas.red()?.3F:1,alpha*(first?.12F:.65F));
                    stack.popPose();
                }
            }
            DokuGasMesh.wisps(stack,buffers,entityRenderDispatcher.cameraOrientation(),points,age,gas.radius(),gas.red(),alpha,far);
        }
        DokuGasMesh.volume(stack,buffers,entityRenderDispatcher.cameraOrientation(),gas.kind(),age,gas.radius(),gas.red(),alpha,gas.getId(),far);
        stack.popPose();
    }
    private static Vector3d lerp(Entity e,float p){return new Vector3d(MathHelper.lerp(p,e.xOld,e.getX()),MathHelper.lerp(p,e.yOld,e.getY()),MathHelper.lerp(p,e.zOld,e.getZ()));}
    @Override public ResourceLocation getTextureLocation(DokuGasEntity gas){return DokuGasMesh.TEXTURE;}
}
