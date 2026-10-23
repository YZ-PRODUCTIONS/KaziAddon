package net.kazi.kazimod.goro;

import com.mojang.blaze3d.matrix.MatrixStack;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only logia flashes reuse the same meshes without sending more entities. */
@Mod.EventBusSubscriber(modid="kazimod",value=Dist.CLIENT)
public final class GoroAmbientEffects {
    private static final Map<Integer,Integer> ACTIVE=new LinkedHashMap<>();
    private static World world;
    public static void flash(Entity owner) {
        if(world!=owner.level){ACTIVE.clear();world=owner.level;}
        if(ACTIVE.size()<64 || ACTIVE.containsKey(owner.getId()))ACTIVE.put(owner.getId(),8);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        if(Minecraft.getInstance().level!=world){ACTIVE.clear();world=Minecraft.getInstance().level;}
        if(!Minecraft.getInstance().isPaused()) {
            ACTIVE.replaceAll((id,life)->life-1);ACTIVE.values().removeIf(life->life<=0);
        }
    }
    @SubscribeEvent public static void render(RenderWorldLastEvent event) {
        Minecraft mc=Minecraft.getInstance();if(world==null || world!=mc.level || ACTIVE.isEmpty())return;
        MatrixStack stack=event.getMatrixStack();Vector3d camera=mc.gameRenderer.getMainCamera().getPosition();
        IRenderTypeBuffer.Impl buffers=mc.renderBuffers().bufferSource();float p=event.getPartialTicks();
        for(Map.Entry<Integer,Integer> entry:ACTIVE.entrySet()) {
            Entity owner=world.getEntity(entry.getKey());if(owner==null || !owner.isAlive())continue;
            float alpha=MathHelper.clamp((entry.getValue()-p)/5,0,1);
            if(owner==mc.player && mc.options.getCameraType().isFirstPerson())alpha*=.12F;
            stack.pushPose();stack.translate(MathHelper.lerp(p,owner.xOld,owner.getX())-camera.x,
                    MathHelper.lerp(p,owner.yOld,owner.getY())+owner.getBbHeight()*.5-camera.y,
                    MathHelper.lerp(p,owner.zOld,owner.getZ())-camera.z);
            GoroMesh.effect(stack,buffers,GoroVfxEntity.CAGE,Math.max(1,owner.getBbHeight()*.6F),owner.tickCount+p,0,alpha,owner.getId());
            stack.popPose();
        }
        buffers.endBatch();
    }
}
