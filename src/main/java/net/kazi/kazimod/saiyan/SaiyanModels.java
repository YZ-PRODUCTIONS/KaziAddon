package net.kazi.kazimod.saiyan;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.models.zoan.CerberusRig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.*;

/** Runtime export of the editable Blockbench hair and dragon rigs. */
public final class SaiyanModels {
    private static final String[] HAIR={"super_saiyan_god_hair","super_saiyan_hair","super_saiyan_2_hair","super_saiyan_3_hair","super_saiyan_god_hair","super_saiyan_blue_hair","ultra_instinct_hair"};
    private static final ResourceLocation BASE_HAIR_TEXTURE=new ResourceLocation("kazimod","textures/models/saiyan/ultra_instinct_hair.png");
    private static final Map<String,SaiyanModels> MODELS=new HashMap<>();
    private static final ResourceLocation DRAGON_GOD=new ResourceLocation("kazimod","textures/models/saiyan/dragon_fist_god.png"),
            DRAGON_BLUE=new ResourceLocation("kazimod","textures/models/saiyan/dragon_fist_blue.png"),
            DRAGON_SILVER=new ResourceLocation("kazimod","textures/models/saiyan/dragon_fist_silver.png");
    private static final Map<PlayerEntity,Sway> SWAY=new WeakHashMap<>();
    private final CerberusRig rig;
    private final ResourceLocation texture;
    private static final Map<ResourceLocation,RenderType> DRAGON_MATERIALS=new HashMap<>();
    private static final class EnergyMaterial extends RenderState {
        EnergyMaterial(){super(null,null,null);}
        static RenderType create(ResourceLocation texture){
            return RenderType.create("saiyan_dragon_energy",DefaultVertexFormats.NEW_ENTITY,7,65536,false,true,
                    RenderType.State.builder().setTextureState(new TextureState(texture,false,false))
                            .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL)
                            .setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).setDiffuseLightingState(NO_DIFFUSE_LIGHTING)
                            .setWriteMaskState(COLOR_WRITE).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(false));
        }
    }
    private static final class Sway {
        float pitch,roll,oldPitch,oldRoll,pitchVelocity,rollVelocity;
    }
    private SaiyanModels(String name){
        texture=new ResourceLocation("kazimod","textures/models/saiyan/"+name+".png");
        try(InputStream in=SaiyanModels.class.getResourceAsStream("/assets/kazimod/models/saiyan/"+name+".json")){
            if(in==null)throw new IOException("Missing Saiyan model: "+name);
            rig=CerberusRig.read(new InputStreamReader(in,StandardCharsets.UTF_8));
        }catch(IOException e){throw new IllegalStateException(e);}
    }
    private static SaiyanModels model(String name){return MODELS.computeIfAbsent(name,SaiyanModels::new);}
    public static void install(){
        for(PlayerRenderer renderer:Minecraft.getInstance().getEntityRenderDispatcher().getSkinMap().values())renderer.addLayer(new HairLayer(renderer));
    }
    public static void tick(){
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null){SWAY.clear();return;}
        SWAY.keySet().removeIf(p->p.removed||p.level!=mc.level||!SaiyanState.get(p).getBoolean("Active"));
        for(PlayerEntity p:mc.level.players()){
            if(!SaiyanState.get(p).getBoolean("Active")||p.distanceToSqr(mc.player)>96*96)continue;
            Sway s=SWAY.computeIfAbsent(p,k->new Sway());s.oldPitch=s.pitch;s.oldRoll=s.roll;
            double yaw=Math.toRadians(p.yHeadRot),dx=p.getX()-p.xOld,dz=p.getZ()-p.zOld;
            float forward=(float)(-Math.sin(yaw)*dx+Math.cos(yaw)*dz);
            float sideways=(float)(Math.cos(yaw)*dx+Math.sin(yaw)*dz);
            float pitch=MathHelper.clamp(forward*24+(float)p.getDeltaMovement().y*9,-14,28);
            float roll=MathHelper.clamp(-sideways*24-MathHelper.wrapDegrees(p.yHeadRot-p.yHeadRotO)*.45F,-20,20);
            s.pitchVelocity=(s.pitchVelocity+(pitch-s.pitch)*.19F)*.65F;
            s.rollVelocity=(s.rollVelocity+(roll-s.roll)*.19F)*.65F;
            s.pitch+=s.pitchVelocity;s.roll+=s.rollVelocity;
        }
    }
    private static final class HairLayer extends LayerRenderer<AbstractClientPlayerEntity,PlayerModel<AbstractClientPlayerEntity>> {
        HairLayer(PlayerRenderer renderer){super(renderer);}
        @Override public void render(MatrixStack stack,IRenderTypeBuffer buffers,int light,AbstractClientPlayerEntity player,float limb,float amount,float partial,float age,float yaw,float pitch){
            int form=SaiyanState.form(player);
            if(!SaiyanState.get(player).getBoolean("Active")||player.isInvisible()||player.isSpectator())return;
            // First-person arms never render player layers; keep inventory previews visible.
            SaiyanModels model=model(HAIR[form]);Map<String,CerberusRig.Pose> pose=model.rig.pose();
            float flying=SaiyanState.get(player).getBoolean("Flight")?1:0;
            model.rig.apply(pose,"idle",age/20,1-flying*.6F);
            model.rig.apply(pose,"flight",age/20,flying*.55F);
            Sway sway=SWAY.get(player);
            if(sway!=null){
                float bend=MathHelper.lerp(partial,sway.oldPitch,sway.pitch),lean=MathHelper.lerp(partial,sway.oldRoll,sway.roll);
                add(pose,"crown",-bend*.45F,-lean*.45F);
                for(int i=0;i<7;i++)add(pose,"long_lock_"+i,bend*.75F,lean*.55F);
                for(int i=0;i<3;i++)add(pose,"bang_"+i,bend*.18F,lean*.2F);
            }
            stack.pushPose();getParentModel().head.translateAndRotate(stack);stack.scale(1/16F,-1/16F,-1/16F);
            ResourceLocation texture=form==0?BASE_HAIR_TEXTURE:model.texture;
            model.draw(model.rig.nodes,pose,stack,buffers.getBuffer(RenderType.entityCutoutNoCull(texture)),light,1,form==0?0x252830:0xffffff);
            stack.popPose();
        }
    }
    private static void add(Map<String,CerberusRig.Pose> pose,String bone,float pitch,float roll){
        CerberusRig.Pose p=pose.computeIfAbsent(bone,k->new CerberusRig.Pose());p.rotation[0]+=pitch;p.rotation[2]+=roll;
    }
    public static void dragon(MatrixStack stack,IRenderTypeBuffer buffers,float age,float fade,int form){
        dragon(stack,buffers,age,fade,form,1,-1,false);
    }
    public static void dragon(MatrixStack stack,IRenderTypeBuffer buffers,float age,float fade,int form,float scale,float bite,boolean anchored){
        SaiyanModels model=model("dragon_fist");Map<String,CerberusRig.Pose> pose=model.rig.pose();
        model.rig.apply(pose,"surge",age/20,anchored?.45F:1);
        if(bite<0)model.rig.apply(pose,"roar",age/20,1);
        else pose.computeIfAbsent("jaw",k->new CerberusRig.Pose()).rotation[0]=MathHelper.lerp(bite,25,-9);
        // The whole animated tail is ahead of the fist, even at maximum growth.
        stack.pushPose();stack.scale(scale,scale,scale);
        float growth=scale>1?(float)(.15+.85*SaiyanFinisherMotion.smooth(age/7)):.7F+.3F*MathHelper.clamp(age/5,0,1);
        if(anchored){growth=1;stack.translate(0,3.0/16,-36.0/16);}
        else stack.translate(0,0,scale>1?6.9*growth:6.9);
        stack.scale(growth/16,growth/16,growth/16);
        ResourceLocation texture=form==4?DRAGON_GOD:form==5?DRAGON_BLUE:form==SaiyanBalance.ULTRA_INSTINCT?DRAGON_SILVER:model.texture;
        model.draw(model.rig.nodes,pose,stack,buffers.getBuffer(DRAGON_MATERIALS.computeIfAbsent(texture,EnergyMaterial::create)),15728880,fade*.54F,0xffffff);
        stack.popPose();
    }
    private void draw(CerberusRig.Node[] nodes,Map<String,CerberusRig.Pose> pose,MatrixStack stack,IVertexBuilder buffer,int light,float alpha,int tint){
        for(CerberusRig.Node node:nodes){
            if(!node.visible)continue;
            if(node.children!=null){
                stack.pushPose();CerberusRig.Pose p=pose.get(node.name);float[] o=node.origin,r=node.rotation;
                stack.translate(o[0],o[1],o[2]);
                stack.mulPose(Vector3f.ZP.rotationDegrees(r[2]+(p==null?0:p.rotation[2])));
                stack.mulPose(Vector3f.YP.rotationDegrees(r[1]+(p==null?0:p.rotation[1])));
                stack.mulPose(Vector3f.XP.rotationDegrees(r[0]+(p==null?0:p.rotation[0])));
                stack.translate(-o[0],-o[1],-o[2]);draw(node.children,pose,stack,buffer,light,alpha,tint);stack.popPose();
            }else if(node.quads!=null)for(CerberusRig.Quad q:node.quads)for(float[] v:q.vertices)
                buffer.vertex(stack.last().pose(),v[0],v[1],v[2]).color((tint>>16&255)/255F,(tint>>8&255)/255F,(tint&255)/255F,alpha).uv(v[3],v[4])
                        .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(stack.last().normal(),q.normal[0],q.normal[1],q.normal[2]).endVertex();
        }
    }
}
