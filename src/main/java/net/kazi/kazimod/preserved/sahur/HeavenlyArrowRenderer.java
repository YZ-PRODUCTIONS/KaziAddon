package net.kazi.kazimod.preserved.sahur;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.*;
import net.kazi.kazimod.entities.projectiles.LightArrowProjectile;

public final class HeavenlyArrowRenderer extends EntityRenderer<LightArrowProjectile>{
    public HeavenlyArrowRenderer(EntityRendererManager manager){super(manager);shadowRadius=0;}
    public void render(LightArrowProjectile e,float yaw,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light){
        Vector3d d=e.getDeltaMovement().normalize();stack.pushPose();
        stack.mulPose(Vector3f.YP.rotation((float)Math.atan2(-d.x,-d.z)));
        stack.mulPose(Vector3f.XP.rotation((float)Math.asin(d.y)));
        IVertexBuilder b=buffers.getBuffer(SahurLightRenderer.TYPE);
        SahurLightMesh.arrow((x,y,z,c,a)->b.vertex(stack.last().pose(),x,y,z).color(c>>16&255,c>>8&255,c&255,(int)(a*255)).endVertex(),e.tickCount+partial);
        stack.popPose();
    }
    public ResourceLocation getTextureLocation(LightArrowProjectile e){return new ResourceLocation("kazimod","textures/abilities/arrows_of_light.png");}
}
