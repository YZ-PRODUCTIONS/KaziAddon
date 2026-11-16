package net.kazi.kazimod.petal;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.ParticleStatus;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.vector.*;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.kazi.kazimod.models.zoan.CerberusRig;
import net.kazi.kazimod.tusk.TuskClient;
public final class PetalClient {
 private static final Map<String,CerberusRig> MODELS=new HashMap<>();
 public static void init(IEventBus bus){bus.addListener((FMLClientSetupEvent e)->{RenderingRegistry.registerEntityRenderingHandler(PetalFeatures.FIELD.get(),FieldRenderer::new);e.enqueueWork(()->Minecraft.getInstance().getEntityRenderDispatcher().getSkinMap().values().forEach(r->r.addLayer(new SwordLayer(r))));});MinecraftForge.EVENT_BUS.addListener(PetalClient::firstPerson);MinecraftForge.EVENT_BUS.addListener(PetalClient::heldItem);}
 public static void receive(PetalPacket packet){Minecraft mc=Minecraft.getInstance();if(mc.level==null||packet.data==null)return;Entity p=mc.level.getEntity(packet.id);if(p instanceof LivingEntity)p.getPersistentData().put(PetalState.TAG,packet.data);}
 public static CerberusRig model(String name){return MODELS.computeIfAbsent(name,k->{try(InputStream in=PetalClient.class.getResourceAsStream("/assets/kazimod/models/entities/petal_"+name+".json")){return CerberusRig.read(new InputStreamReader(in,StandardCharsets.UTF_8));}catch(IOException e){throw new IllegalStateException(e);}});}
 public static void model(String name,MatrixStack s,IRenderTypeBuffer b,float alpha){TuskClient.draw(model(name).nodes,Collections.emptyMap(),s,b.getBuffer(RenderType.entityTranslucent(new ResourceLocation("kazimod","textures/entities/petal_"+name+".png"))),15728880,alpha);}
 private static final class SwordLayer extends LayerRenderer<AbstractClientPlayerEntity,PlayerModel<AbstractClientPlayerEntity>> {
  SwordLayer(PlayerRenderer r){super(r);}
  @Override public void render(MatrixStack s,IRenderTypeBuffer b,int light,AbstractClientPlayerEntity p,float walk,float amount,float partial,float age,float yaw,float pitch){
   if(!PetalState.get(p).getBoolean("Fruit")||!p.isAlive()||p.isInvisible())return;int action=PetalState.action(p);float t=p.level.getGameTime()-PetalState.get(p).getLong("Start")+partial;
   if(!PetalState.scattered(p)||action==6&&t<4||action==8){if(p.getMainHandItem().isEmpty()||action==8){s.pushPose();getParentModel().rightArm.translateAndRotate(s);s.translate(0,.7,-.08);s.mulPose(Vector3f.XP.rotationDegrees(-90));s.scale(1/16F,1/16F,1/16F);if(action==8)HakuteikenVisual.sword(s,b,t,1);else model("sword",s,b,action==6?Math.max(0,1-t/4):1);s.popPose();}}
   if(PetalState.scattered(p)&&action!=8&&(action!=6||t>=38)){s.pushPose();s.translate(0,1.5,0);s.scale(1,-1,1);PetalMesh.orbit(s,b,p.tickCount+partial,1,PetalState.bankai(p),.7F);s.popPose();}
  }
 }
 public static void pose(PlayerModel<?> m,LivingEntity p,float time){if(!(p instanceof PlayerEntity)||!PetalState.get(p).getBoolean("Fruit"))return;int action=PetalState.action(p);if(action<0)return;float t=p.level.getGameTime()-PetalState.get(p).getLong("Start")+(time-p.tickCount),blend=MathHelper.clamp(t/5,0,1);float pitch=action==6?MathHelper.lerp(HakuteikenVisual.smooth((t-4)/10),-.8F,.12F):action==1?-1.4F:action==2?-1.2F:action==8?-1.8F:-.7F;m.rightArm.xRot=MathHelper.lerp(blend,m.rightArm.xRot,pitch);m.rightArm.yRot=action==2?-.6F:0;m.leftArm.xRot=MathHelper.lerp(blend,m.leftArm.xRot,action==6?.2F:-.3F);if(action==8){
    float ready=HakuteikenVisual.smooth((t-12)/40),rush=HakuteikenVisual.smooth((t-HakuteikenTiming.CHARGE)/4),fade=1-HakuteikenVisual.smooth((t-HakuteikenTiming.RUSH_END)/16);
    m.rightArm.xRot=MathHelper.lerp(fade,m.rightArm.xRot,MathHelper.lerp(rush,-.65F-ready*.35F,-1.65F));
    m.rightArm.yRot=MathHelper.lerp(fade,m.rightArm.yRot,MathHelper.lerp(rush,-.8F,-.12F));
    m.rightArm.zRot=.08F*fade;
    m.leftArm.xRot=MathHelper.lerp(fade,m.leftArm.xRot,MathHelper.lerp(rush,-.55F-ready*.2F,.65F));
    m.leftArm.yRot=.65F*(1-rush)*fade;m.leftArm.zRot=-.12F*fade;
    m.body.xRot+=.22F*rush*fade;
    m.rightLeg.xRot=MathHelper.lerp(fade,m.rightLeg.xRot,-.12F-.35F*rush);
    m.leftLeg.xRot=MathHelper.lerp(fade,m.leftLeg.xRot,.12F+.25F*rush);
    m.jacket.copyFrom(m.body);m.leftPants.copyFrom(m.leftLeg);m.rightPants.copyFrom(m.rightLeg);
   }m.rightSleeve.copyFrom(m.rightArm);m.leftSleeve.copyFrom(m.leftArm);}
 private static void heldItem(RenderHandEvent event){PlayerEntity p=Minecraft.getInstance().player;if(p!=null&&PetalState.get(p).getBoolean("Fruit")&&PetalState.action(p)==8)event.setCanceled(true);}
 private static void firstPerson(RenderWorldLastEvent e){Minecraft mc=Minecraft.getInstance();PlayerEntity p=mc.player;if(p==null||!mc.options.getCameraType().isFirstPerson()||!PetalState.get(p).getBoolean("Fruit")||(!p.getMainHandItem().isEmpty()||PetalState.scattered(p))&&PetalState.action(p)!=8)return;
  MatrixStack s=e.getMatrixStack();Vector3d at=p.getEyePosition(e.getPartialTicks()).subtract(mc.gameRenderer.getMainCamera().getPosition());s.pushPose();s.translate(at.x,at.y,at.z);s.mulPose(Vector3f.YP.rotationDegrees(180-p.yRot));s.translate(-.35,-.55,-.6);s.mulPose(Vector3f.XP.rotationDegrees(-20));s.scale(1/16F,1/16F,1/16F);IRenderTypeBuffer.Impl b=mc.renderBuffers().bufferSource();if(PetalState.action(p)==8)HakuteikenVisual.sword(s,b,p.level.getGameTime()-PetalState.get(p).getLong("Start")+e.getPartialTicks(),.45F);else model("sword",s,b,.9F);s.popPose();b.endBatch();}
 public static void drawField(PetalField f,float partial,MatrixStack s,IRenderTypeBuffer b){float age=f.age(partial);LivingEntity p=f.owner();Minecraft mc=Minecraft.getInstance();boolean self=p==mc.player&&mc.options.getCameraType().isFirstPerson();float alpha=self&&(f.kind()==2||f.kind()==8)?.15F:.9F;int quality=mc.options.particles==ParticleStatus.MINIMAL?100:mc.options.particles==ParticleStatus.DECREASED?180:320;
  s.pushPose();Vector3d end=f.end().subtract(f.position());float facing=f.yaw();
  if(p!=null&&(f.kind()==1||f.kind()==2||f.kind()==8)){Vector3d delta=p.getPosition(partial).subtract(f.getPosition(partial));s.translate(delta.x,delta.y,delta.z);facing=MathHelper.rotLerp(partial,p.yRotO,p.yRot);}
  if(f.kind()==1){double eye=p==null?1.6:p.getEyeHeight();s.translate(0,eye,0);end=end.subtract(0,eye,0);if(p!=null){Vector3d start=p.getEyePosition(partial),to=start.add(p.getViewVector(partial).scale(24));end=p.level.clip(new RayTraceContext(start,to,RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,p)).getLocation().subtract(start);}}
  else if(f.kind()==0||f.kind()==2||f.kind()==3||f.kind()==6||f.kind()==8){s.mulPose(Vector3f.YP.rotationDegrees(180-facing));}
  if(f.kind()==8)HakuteikenVisual.render(s,b,age,self?.12F:1,quality,self);else if(f.kind()==6)PetalSwordVisual.bankai(s,b,age,f.life(),quality);else if(f.kind()==7)PetalSwordVisual.arena(s,b,age,f.life(),f.size(),quality);else PetalMesh.field(f.kind(),age,f.life(),f.size(),end,f.yaw(),s,b,alpha,quality);
  if(f.kind()==0&&age<12){s.pushPose();s.translate(.35,1.1,-.5);s.scale(.0625F,.0625F,.0625F);model("sword",s,b,1-age/12);s.popPose();}
  if(f.kind()==6&&age>=4&&age<18){s.pushPose();s.translate(.35,2.4-Math.max(0,age-3)*.22,-.6);s.mulPose(Vector3f.ZP.rotationDegrees(180));s.scale(.0625F,.0625F,.0625F);model("sword",s,b,MathHelper.clamp((18-age)/5,0,1));s.popPose();}
  if(f.kind()==7){float phase=age%PetalField.SENKEI_SWORD_INTERVAL;if(phase<14)for(LivingEntity target:f.swordTargets()){Vector3d at=target.getPosition(partial).subtract(f.getPosition(partial));PetalMesh.field(10,age,f.life(),1,at,f.yaw(),s,b,alpha,quality);s.pushPose();s.translate(at.x,at.y+target.getBbHeight()+8*Math.max(0,1-phase/12),at.z);s.mulPose(Vector3f.YP.rotationDegrees(180-target.yRot));PetalSwordVisual.sword(s,b,Math.min(1,age/10),quality>100);s.popPose();}}
  s.popPose();
 }
 private static final class FieldRenderer extends EntityRenderer<PetalField>{FieldRenderer(EntityRendererManager m){super(m);}@Override public ResourceLocation getTextureLocation(PetalField f){return new ResourceLocation("kazimod","textures/entities/petal_sword.png");}@Override public void render(PetalField f,float yaw,float partial,MatrixStack s,IRenderTypeBuffer b,int light){drawField(f,partial,s,b);}}
}
