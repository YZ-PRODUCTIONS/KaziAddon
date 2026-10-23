package net.kazi.kazimod.shikigami;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.models.zoan.CerberusRig;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class ShikigamiRenderer extends EntityRenderer<ShikigamiEntity> {
    private final CerberusRig rig;
    private final ResourceLocation texture;
    private final Map<ShikigamiEntity,Visual> visuals=new WeakHashMap<>();
    private static final class Visual {
        float last,stride,walk,air,transition;
        ShikigamiEntity.Action action;
        Map<String,CerberusRig.Pose> previous,from;
    }
    public static void init(IEventBus bus){bus.addListener((FMLClientSetupEvent event)->{
        for(ShikigamiKind kind:ShikigamiKind.values())RenderingRegistry.registerEntityRenderingHandler(ShikigamiFeatures.type(kind),manager->new ShikigamiRenderer(manager,kind));
    });}
    public ShikigamiRenderer(EntityRendererManager manager,ShikigamiKind kind){
        super(manager);texture=new ResourceLocation("kazimod","textures/entities/"+kind.id+".png");shadowRadius=kind.width*.5F;
        try(InputStream stream=ShikigamiRenderer.class.getResourceAsStream("/assets/kazimod/models/entities/"+kind.id+".json")){
            if(stream==null)throw new IOException("Missing "+kind.id+" geometry");rig=CerberusRig.read(new InputStreamReader(stream,StandardCharsets.UTF_8));
        }catch(IOException error){throw new IllegalStateException("Cannot load "+kind.id,error);}
    }
    @Override public void render(ShikigamiEntity e,float yaw,float partial,MatrixStack stack,IRenderTypeBuffer buffers,int light){
        float time=e.tickCount+partial,age=e.age(partial),speed=MathHelper.lerp(partial,e.animationSpeedOld,e.animationSpeed);
        Visual v=visuals.computeIfAbsent(e,k->new Visual());float dt=MathHelper.clamp(time-v.last,0,2);v.last=time;
        v.walk+=(MathHelper.clamp(speed*3,0,1)-v.walk)*Math.min(1,dt*.3F);
        v.air+=((e.isOnGround()?0:1)-v.air)*Math.min(1,dt*.3F);
        Map<String,CerberusRig.Pose> pose=rig.pose();ShikigamiEntity.Action action=e.action();
        boolean moving=action==ShikigamiEntity.Action.IDLE||action==ShikigamiEntity.Action.RUNUP||action==ShikigamiEntity.Action.CHARGE;
        if(moving){
            float run=ShikigamiVfx.smooth((speed-.25F)/.3F);if(action!=ShikigamiEntity.Action.IDLE)run=1;
            v.stride=(v.stride+dt*Math.max(.02F,speed)/MathHelper.lerp(run,7,10))%1;
            rig.apply(pose,"idle",time/20,1-v.walk*.8F);
            rig.apply(pose,"walk",v.stride*1.2F,v.walk*(1-run)*(1-v.air));
            rig.apply(pose,"run",v.stride*.65F,v.walk*run*(1-v.air));
            rig.apply(pose,"airborne",time/20,v.air);
            if(action==ShikigamiEntity.Action.CHARGE){
                pose.computeIfAbsent("Head",k->new CerberusRig.Pose()).rotation[0]-=18;
                pose.computeIfAbsent("Body",k->new CerberusRig.Pose()).rotation[0]-=6;
            }
            if(action!=ShikigamiEntity.Action.CHARGE||e.kind==ShikigamiKind.OX){
                CerberusRig.Pose head=pose.computeIfAbsent("Head",k->new CerberusRig.Pose());
                head.rotation[1]-=MathHelper.clamp(MathHelper.rotLerp(partial,e.yHeadRotO,e.yHeadRot)-MathHelper.rotLerp(partial,e.yBodyRotO,e.yBodyRot),-22,22);
            }
        }else rig.apply(pose,action.name().toLowerCase(Locale.ROOT),age/20,1);
        if(v.action!=action){v.action=action;v.from=v.previous;v.transition=time;}
        float blend=ShikigamiVfx.smooth((time-v.transition)/4);
        if(v.from!=null&&blend<1){Set<String>bones=new HashSet<>(v.from.keySet());bones.addAll(pose.keySet());
            for(String bone:bones){CerberusRig.Pose a=v.from.get(bone),b=pose.computeIfAbsent(bone,k->new CerberusRig.Pose());
                for(int i=0;i<3;i++){b.rotation[i]=MathHelper.lerp(blend,a==null?0:a.rotation[i],b.rotation[i]);b.position[i]=MathHelper.lerp(blend,a==null?0:a.position[i],b.position[i]);}}
        }else v.from=null;v.previous=pose;
        shadowRadius=e.portal()?0:e.kind.width*.5F;
        if(e.portal())ShikigamiVfx.portal(stack,buffers,age,e.dismissing()?50:e.kind==ShikigamiKind.ELEPHANT?24:70,e.kind.width*.8F+.5F);
        if(action==ShikigamiEntity.Action.LAND)ShikigamiVfx.impact(stack,buffers,age);
        if(action==ShikigamiEntity.Action.TONGUE&&e.prey()!=null)ShikigamiVfx.tongue(e,partial,stack,buffers);
        if(action==ShikigamiEntity.Action.RUNUP||action==ShikigamiEntity.Action.CHARGE)ShikigamiVfx.dust(e,time,stack,buffers);
        stack.pushPose();stack.mulPose(Vector3f.YP.rotationDegrees(180-MathHelper.rotLerp(partial,e.yBodyRotO,e.yBodyRot)));
        float reveal=1;
        if(action==ShikigamiEntity.Action.SUMMON){float duration=e.kind==ShikigamiKind.ELEPHANT?24:70;
            stack.translate(0,-e.kind.height*(1-ShikigamiVfx.smooth(age/(duration*.85F))),0);reveal=.015F+.985F*ShikigamiVfx.smooth((age-duration*.8F)/(duration*.2F));}
        if(e.dismissing()){stack.translate(0,-e.kind.height*ShikigamiVfx.smooth((age-6)/42),0);reveal=1-.985F*ShikigamiVfx.smooth(age/12);}
        if(e.deathTime>0)stack.mulPose(Vector3f.ZP.rotationDegrees(Math.min(90,(e.deathTime+partial)*4.5F)));
        float scale=e.kind.modelScale/16;stack.scale(scale,scale,scale);
        draw(rig.nodes,pose,stack,buffers.getBuffer(RenderType.entityCutoutNoCull(texture)),light,reveal);
        stack.popPose();super.render(e,yaw,partial,stack,buffers,light);
    }
    private static void draw(CerberusRig.Node[] nodes,Map<String,CerberusRig.Pose> pose,MatrixStack stack,IVertexBuilder buffer,int light,float tint){
        for(CerberusRig.Node node:nodes){if(!node.visible)continue;
            if(node.children!=null){stack.pushPose();CerberusRig.Pose p=pose.get(node.name);float[] o=node.origin,r=node.rotation;
                stack.translate(o[0]+(p==null?0:p.position[0]),o[1]+(p==null?0:p.position[1]),o[2]+(p==null?0:p.position[2]));
                stack.mulPose(Vector3f.ZP.rotationDegrees(r[2]+(p==null?0:p.rotation[2])));stack.mulPose(Vector3f.YP.rotationDegrees(r[1]+(p==null?0:p.rotation[1])));stack.mulPose(Vector3f.XP.rotationDegrees(r[0]+(p==null?0:p.rotation[0])));
                stack.translate(-o[0],-o[1],-o[2]);draw(node.children,pose,stack,buffer,light,tint);stack.popPose();
            }else if(node.quads!=null)for(CerberusRig.Quad q:node.quads)for(float[] p:q.vertices)
                buffer.vertex(stack.last().pose(),p[0],p[1],p[2]).color(tint,tint,tint,1).uv(p[3],p[4]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(stack.last().normal(),q.normal[0],q.normal[1],q.normal[2]).endVertex();
        }
    }
    @Override public ResourceLocation getTextureLocation(ShikigamiEntity e){return texture;}
}
