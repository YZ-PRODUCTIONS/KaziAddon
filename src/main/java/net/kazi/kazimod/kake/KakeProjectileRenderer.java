package net.kazi.kazimod.kake;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.*;
import net.kazi.kazimod.entities.GiantDiceEntity;

public final class KakeProjectileRenderer<T extends Entity> extends EntityRenderer<T> {
    private final int kind;
    public KakeProjectileRenderer(EntityRendererManager manager,int kind){super(manager);this.kind=kind;shadowRadius=0;}
    public void render(T e,float yaw,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light){
        float age=e.tickCount+partial;boolean giant=kind==4,far=entityRenderDispatcher.camera.getPosition().distanceToSqr(e.position())>80*80;
        Vector3d motion=e.getDeltaMovement();if(motion.lengthSqr()<.001)motion=new Vector3d(e.getX()-e.xOld,e.getY()-e.yOld,e.getZ()-e.zOld);
        stack.pushPose();
        if(giant){GiantDiceEntity dice=(GiantDiceEntity)e;
            KakeMesh.Sink warning=KakeMesh.offset(KakeRender.sink(stack,buffers,true),0,dice.visualGround()-e.getY()+.08,0);
            KakeMesh.ring(warning,4,.12,0,KakeMesh.GOLD,.7F,32);
            if(!dice.isStomped())KakeMesh.digit(KakeMesh.rotate(warning,-Math.PI/2,0),7,.4,KakeMesh.PALE);
            stack.translate(0,1.7,0);
        }
        else if(motion.lengthSqr()>.0001){stack.mulPose(Vector3f.YP.rotationDegrees((float)Math.toDegrees(Math.atan2(motion.x,motion.z))));stack.mulPose(Vector3f.XP.rotationDegrees((float)-Math.toDegrees(Math.atan2(motion.y,Math.sqrt(motion.x*motion.x+motion.z*motion.z)))));}
        if(!giant)KakeMesh.trail(KakeRender.sink(stack,buffers,true),kind,age,far);
        stack.pushPose();
        if(kind==1||giant){float speed=giant&&((GiantDiceEntity)e).isDescending()?28:7;stack.mulPose(Vector3f.YP.rotationDegrees(age*speed));stack.mulPose(Vector3f.XP.rotationDegrees(age*speed*.63F));}
        else stack.mulPose(Vector3f.ZP.rotationDegrees(age*(kind==2?24:17)));
        float size=giant?3.4F:kind==1?1.1F:kind==3?1.3F:1;
        if(giant&&((GiantDiceEntity)e).isStomped())size*=Math.max(0,1-((GiantDiceEntity)e).stompAge()/14F);
        stack.scale(size,size,size);
        for(boolean glow:new boolean[]{false,true}){KakeMesh.Sink sink=KakeRender.sink(stack,buffers,glow);
            if(kind==0)KakeMesh.coin(sink,glow);else if(kind==1||giant)KakeMesh.dice(sink,glow);else if(kind==2)KakeMesh.card(sink,e.getId(),glow);else KakeMesh.chip(sink,e.getId(),glow);}
        stack.popPose();stack.popPose();
    }
    public ResourceLocation getTextureLocation(T e){return new ResourceLocation("kazimod","textures/abilities/casino_roll.png");}
}
