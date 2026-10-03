package net.kazi.kazimod.client.models.morphs;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.models.zoan.CerberusRig;
import net.kazi.kazimod.models.zoan.MammothMotion;
import net.kazi.kazimod.mammoth.MammothAbility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.*;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.MorphHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphModel;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class MammothModel<T extends LivingEntity> extends MorphModel<T> {
    public static final ResourceLocation FULL_TEXTURE=new ResourceLocation("kazimod","textures/models/zoan/mammoth_full.png");
    public static final ResourceLocation HYBRID_TEXTURE=new ResourceLocation("kazimod","textures/models/zoan/mammoth_hybrid.png");
    private static final Map<LivingEntity,State> STATES=new WeakHashMap<>();
    private static final class Data {
        static final CerberusRig FULL=load("mammoth_full"), HYBRID=load("mammoth_hybrid");
        static CerberusRig load(String name) {
            try(InputStreamReader r=new InputStreamReader(Objects.requireNonNull(MammothModel.class.getResourceAsStream(
                    "/assets/kazimod/models/zoan/"+name+".json")),StandardCharsets.UTF_8)) { return CerberusRig.read(r); }
            catch(Exception e) { throw new IllegalStateException("Cannot load mammoth rig: "+name,e); }
        }
    }
    private static final class State {
        final boolean hybrid;
        final MammothMotion motion;
        final MammothMotion.Input input=new MammothMotion.Input();
        int abilityTick=Integer.MIN_VALUE;
        ChargeComponent sweep,rushCharge;
        ContinuousComponent stomp,shot,ride,vacuum,rush;
        State(boolean hybrid) { this.hybrid=hybrid;motion=new MammothMotion(hybrid); }
        void abilities(LivingEntity entity) {
            if(abilityTick==entity.tickCount)return;
            abilityTick=entity.tickCount;sweep=rushCharge=null;stomp=shot=ride=vacuum=rush=null;
            for(IAbility a:AbilityDataCapability.get(entity).getEquippedAndPassiveAbilities()) {
                if(a instanceof MammothAbility){
                    MammothAbility mammoth=(MammothAbility)a;
                    switch(mammoth.move){
                        case SWEEP:sweep=mammoth.charge;break;
                        case STOMP:stomp=mammoth.continuous;break;
                        case VACUUM:vacuum=mammoth.continuous;break;
                        case STAMPEDE:rushCharge=mammoth.charge;rush=mammoth.continuous;break;
                    }
                    continue;
                }
                String name=a.getClass().getSimpleName();
                if(!name.equals("AncientSweepAbility")&&!name.equals("AncientStompAbility")
                        &&!name.equals("AncientTrunkShotAbility")&&!name.equals("MammothRideableAbility"))continue;
                for(AbilityComponent<?> c:a.getComponents().values()) {
                    if(name.equals("AncientSweepAbility")&&c instanceof ChargeComponent)sweep=(ChargeComponent)c;
                    if(c instanceof ContinuousComponent) {
                        if(name.equals("AncientStompAbility"))stomp=(ContinuousComponent)c;
                        if(name.equals("AncientTrunkShotAbility"))shot=(ContinuousComponent)c;
                        if(name.equals("MammothRideableAbility"))ride=(ContinuousComponent)c;
                    }
                }
            }
        }
    }
    private final boolean hybrid;
    private final CerberusRig rig;
    private final Map<String,List<CerberusRig.Node>> paths=new HashMap<>();
    private final PlayerModel<T> classicSkin=new PlayerModel<>(0,false), slimSkin=new PlayerModel<>(0,true);
    private final Vector4f vertex=new Vector4f(0,0,0,1);
    private final Vector3f normal=new Vector3f();
    private Map<String,CerberusRig.Pose> pose=Collections.emptyMap();
    private T wearer;
    private boolean filterBackFaces;

    public MammothModel(boolean hybrid) {
        super(0);this.hybrid=hybrid;rig=hybrid?Data.HYBRID:Data.FULL;
        if(hybrid)for(String n:new String[]{"head","body","left_arm","right_arm","left_leg","right_leg"})paths.put(n,rig.path(n));
    }
    public static void signal(LivingEntity entity,int action,long tick) {
        xyz.pixelatedw.mineminenomi.api.morph.MorphInfo info=MorphHelper.getZoanInfo(entity);
        if(info==null||!info.getForm().startsWith("mammoth_"))return;
        boolean hybrid=info.getForm().equals("mammoth_heavy");
        State state=state(entity,hybrid);
        state.motion.signal(action,entity.tickCount-MathHelper.clamp(entity.level.getGameTime()-tick,0,20));
    }
    private static State state(LivingEntity e,boolean hybrid) {
        State s=STATES.get(e);if(s==null||s.hybrid!=hybrid){s=new State(hybrid);STATES.put(e,s);}return s;
    }
    @Override public void setupAnim(T entity,float limbSwing,float limbAmount,float age,float yaw,float pitch) {
        wearer=entity;
        super.setupAnim(entity,0,0,age,yaw,pitch);
        State state=state(entity,hybrid);state.abilities(entity);
        MammothMotion.Input in=state.input;
        in.age=age;in.limbSwing=limbSwing;in.limbAmount=limbAmount;in.yaw=yaw;in.pitch=pitch;
        in.verticalSpeed=(float)entity.getDeltaMovement().y;in.grounded=entity.isOnGround();
        in.swimming=entity.isInWater()||entity.isInLava();
        in.stampeding=state.rush!=null&&state.rush.isContinuous();
        in.stampedeCharge=state.rushCharge!=null&&state.rushCharge.isCharging();
        in.stampedeProgress=in.stampedeCharge?state.rushCharge.getChargePercentage():0;
        in.vacuum=state.vacuum!=null&&state.vacuum.isContinuous();
        in.running=entity.isSprinting()||in.stampeding;in.crouching=entity.isCrouching();
        if(in.stampeding)in.limbAmount=Math.max(.8F,in.limbAmount);
        in.charging=state.sweep!=null&&state.sweep.isCharging();in.charge=in.charging?state.sweep.getChargePercentage():0;
        in.stomping=state.stomp!=null&&state.stomp.isContinuous();
        in.armed=state.shot!=null&&state.shot.isContinuous();
        in.rideable=entity.isVehicle()||(state.ride!=null&&state.ride.isContinuous());
        in.swing=entity.getAttackAnim(MathHelper.clamp(age-entity.tickCount,0,1));
        in.rightHand=(entity.getMainArm()==HandSide.RIGHT)^(entity.swingingArm==Hand.OFF_HAND);
        pose=state.motion.sample(rig,in);
        if(hybrid) {
            if(entity.isUsingItem()) {
                copyArm("right_arm",rightArm);copyArm("left_arm",leftArm);
            } else {
                if(!entity.getItemInHand(entity.getMainArm()==HandSide.RIGHT?Hand.MAIN_HAND:Hand.OFF_HAND).isEmpty())pose.computeIfAbsent("right_arm",k->new CerberusRig.Pose()).rotation[0]+=10;
                if(!entity.getItemInHand(entity.getMainArm()==HandSide.LEFT?Hand.MAIN_HAND:Hand.OFF_HAND).isEmpty())pose.computeIfAbsent("left_arm",k->new CerberusRig.Pose()).rotation[0]+=10;
            }
        }
    }
    private void copyArm(String name,ModelRenderer part) {
        CerberusRig.Pose p=pose.computeIfAbsent(name,k->new CerberusRig.Pose());
        p.rotation[0]=-part.xRot*180/(float)Math.PI;p.rotation[1]=part.yRot*180/(float)Math.PI;p.rotation[2]=-part.zRot*180/(float)Math.PI;
    }
    private static void reflectY(MatrixStack s) {
        s.last().pose().multiply(Matrix4f.createScaleMatrix(1,-1,1));
        s.last().normal().mul(Matrix3f.createScaleMatrix(1,-1,1));
    }
    private static void modelSpace(MatrixStack s) { s.translate(0,1.5,0);s.scale(1/16F,1/16F,1/16F);reflectY(s); }
    @Override public void renderToBuffer(MatrixStack s,IVertexBuilder buffer,int light,int overlay,float r,float g,float b,float alpha) {
        filterBackFaces=Minecraft.getInstance().screen==null;
        s.pushPose();modelSpace(s);draw(rig.nodes,s,buffer,light,overlay,r,g,b,alpha);
        if(hybrid&&wearer instanceof AbstractClientPlayerEntity)renderSkin((AbstractClientPlayerEntity)wearer,s,light,overlay,r,g,b,alpha);
        s.popPose();
    }
    private void draw(CerberusRig.Node[] nodes,MatrixStack s,IVertexBuilder buffer,int light,int overlay,float r,float g,float b,float alpha) {
        for(CerberusRig.Node n:nodes) {
            if(!n.visible)continue;
            if(n.children!=null) { s.pushPose();transform(n,s);draw(n.children,s,buffer,light,overlay,r,g,b,alpha);s.popPose(); }
            else if(n.quads!=null)for(CerberusRig.Quad q:n.quads) {
                normal.set(q.normal[0],q.normal[1],q.normal[2]);normal.transform(s.last().normal());
                float[] v=q.vertices[3];vertex.set(v[0],v[1],v[2],1);vertex.transform(s.last().pose());
                if(filterBackFaces&&normal.x()*vertex.x()+normal.y()*vertex.y()+normal.z()*vertex.z()>.001F)continue;
                for(int i=3;i>=0;i--){v=q.vertices[i];if(i!=3){vertex.set(v[0],v[1],v[2],1);vertex.transform(s.last().pose());}
                    buffer.vertex(vertex.x(),vertex.y(),vertex.z()).color(r,g,b,alpha).uv(v[3],v[4]).overlayCoords(overlay).uv2(light).normal(normal.x(),normal.y(),normal.z()).endVertex();}
            }
        }
    }
    private void transform(CerberusRig.Node n,MatrixStack s) {
        CerberusRig.Pose p=pose.get(n.name);float[]o=n.origin;
        if(p!=null)s.translate(p.position[0],p.position[1],p.position[2]);
        s.translate(o[0],o[1],o[2]);
        s.mulPose(Vector3f.ZP.rotationDegrees(n.rotation[2]+(p==null?0:p.rotation[2])));
        s.mulPose(Vector3f.YP.rotationDegrees(n.rotation[1]+(p==null?0:p.rotation[1])));
        s.mulPose(Vector3f.XP.rotationDegrees(n.rotation[0]+(p==null?0:p.rotation[0])));
        s.translate(-o[0],-o[1],-o[2]);
    }
    private void attach(String bone,MatrixStack s) { for(CerberusRig.Node n:paths.get(bone))transform(n,s); }
    private void renderSkin(AbstractClientPlayerEntity player,MatrixStack s,int light,int overlay,float r,float g,float b,float alpha) {
        PlayerModel<T> skin=player.getModelName().equals("slim")?slimSkin:classicSkin;
        IVertexBuilder buf=Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.entityTranslucent(player.getSkinTextureLocation()));
        skinPart("head",skin.head,player.isModelPartShown(PlayerModelPart.HAT)?skin.hat:null,s,buf,light,overlay,r,g,b,alpha);
        if(player.isSpectator())return;
        skinPart("body",skin.body,player.isModelPartShown(PlayerModelPart.JACKET)?skin.jacket:null,s,buf,light,overlay,r,g,b,alpha);
        skinPart("right_arm",skin.rightArm,player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE)?skin.rightSleeve:null,s,buf,light,overlay,r,g,b,alpha);
        skinPart("left_arm",skin.leftArm,player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE)?skin.leftSleeve:null,s,buf,light,overlay,r,g,b,alpha);
        skinPart("right_leg",skin.rightLeg,player.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG)?skin.rightPants:null,s,buf,light,overlay,r,g,b,alpha);
        skinPart("left_leg",skin.leftLeg,player.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG)?skin.leftPants:null,s,buf,light,overlay,r,g,b,alpha);
    }
    private void skinPart(String bone,ModelRenderer part,ModelRenderer outer,MatrixStack s,IVertexBuilder buf,int light,int overlay,float r,float g,float b,float a) {
        s.pushPose();attach(bone,s);CerberusRig.Node joint=paths.get(bone).get(paths.get(bone).size()-1);
        s.translate(joint.origin[0],joint.origin[1]-(bone.endsWith("_arm")?2:0),joint.origin[2]);reflectY(s);
        boolean head=bone.equals("head");s.scale(head?16:21.6F,16,head?16:18.4F);
        renderPart(part,s,buf,light,overlay,r,g,b,a);if(outer!=null)renderPart(outer,s,buf,light,overlay,r,g,b,a);
        s.popPose();
    }
    private static void renderPart(ModelRenderer part,MatrixStack s,IVertexBuilder buf,int light,int overlay,float r,float g,float b,float a) {
        part.x=part.y=part.z=part.xRot=part.yRot=part.zRot=0;part.visible=true;part.render(s,buf,light,overlay,r,g,b,a);
    }
    @Override public void translateToHand(HandSide side,MatrixStack s) {
        if(!hybrid)return;
        boolean right=side==HandSide.RIGHT;modelSpace(s);attach(right?"right_arm":"left_arm",s);
        boolean slim=wearer instanceof AbstractClientPlayerEntity&&((AbstractClientPlayerEntity)wearer).getModelName().equals("slim");
        double palm=slim?7.425:8.1;
        s.translate(right?-palm:palm,11,0);reflectY(s);s.scale(16,16,16);
        s.translate((right?1:-1)/16.0,-.625,.125);
    }
    @Override public boolean renderItemInHand(T entity,HandSide side,MatrixStack s) { return false; }
    @Override public void renderFirstPersonArm(MatrixStack s,IVertexBuilder b,int l,int o,float r,float g,float blue,float a,HandSide side) {}
    @Override public void renderFirstPersonLeg(MatrixStack s,IVertexBuilder b,int l,int o,float r,float g,float blue,float a,HandSide side) {}
}
