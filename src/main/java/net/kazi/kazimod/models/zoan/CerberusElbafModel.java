package net.kazi.kazimod.models.zoan;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraft.util.math.vector.Matrix4f;
import xyz.pixelatedw.mineminenomi.api.morph.MorphModel;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.WeakHashMap;

public final class CerberusElbafModel<T extends LivingEntity> extends MorphModel<T> {
    public static final ResourceLocation TEXTURE = new ResourceLocation("kazimod", "textures/models/zoan/cerberus_elbaf.png");
    public static final float FORM_SCALE = 1.65F;
    private static final Map<LivingEntity, Motion> MOTION = new WeakHashMap<>();
    private static final Map<LivingEntity, Motion> HYBRID_MOTION = new WeakHashMap<>();
    private static final class Data {
        private static final CerberusRig RIG = load();
        private static CerberusRig load() {
            try (InputStreamReader reader = new InputStreamReader(java.util.Objects.requireNonNull(
                    CerberusElbafModel.class.getResourceAsStream("/assets/kazimod/models/zoan/cerberus_elbaf.json")), StandardCharsets.UTF_8)) {
                return CerberusRig.read(reader);
            } catch (Exception ex) { throw new IllegalStateException("Cannot load Cerberus Elbaf model", ex); }
        }
    }
    private static final class HybridData {
        private static final CerberusRig RIG = load();
        private static CerberusRig load() {
            try (InputStreamReader reader = new InputStreamReader(java.util.Objects.requireNonNull(
                    CerberusElbafModel.class.getResourceAsStream("/assets/kazimod/models/zoan/cerberus_hybrid.json")), StandardCharsets.UTF_8)) {
                return CerberusRig.read(reader);
            } catch (Exception ex) { throw new IllegalStateException("Cannot load Cerberus hybrid", ex); }
        }
    }
    private static final class CloudState extends RenderState {
        private CloudState() { super(null, null, null); }
        private static RenderType create() {
            return RenderType.create("kazimod_cerberus_smoke", DefaultVertexFormats.POSITION_COLOR, 7, 65536, false, true,
                    RenderType.State.builder().setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                            .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_DEPTH_WRITE)
                            .setCullState(NO_CULL).createCompositeState(false));
        }
    }
    private static final RenderType CLOUD = CloudState.create();
    private static final class Motion {
        String action = "";
        String previousAction = "";
        float previousTime, lastTime;
        float since, firstSeen, previousSwing, strikeAt = -100, hadesArmedAt = -100;
        boolean hadesStrike;
        float gaitPhase, lastLimbSwing, lastAge, runBlend;
        float movementBlend, lookYaw, lookPitch;
        boolean gaitInitialized;
        int biteIndex = -1;
        final CerberusJumpMotion jump = new CerberusJumpMotion();
        Motion(float age) { since = firstSeen = lastAge = age; }
    }
    private final boolean headless;
    private final boolean hybrid;
    private final CerberusRig rig;
    private final List<CerberusRig.Node> rightHandPath, leftHandPath;
    private Map<String, CerberusRig.Pose> pose = Collections.emptyMap();
    private float age;
    private boolean visible = true;
    public CerberusElbafModel(boolean headless) { this(headless, false); }
    public CerberusElbafModel(boolean headless, boolean hybrid) {
        super(0); this.headless = headless; this.hybrid = hybrid; this.rig = hybrid ? HybridData.RIG : Data.RIG;
        // Blockbench's viewer-left arm is Minecraft's negative-X (right) arm.
        rightHandPath = hybrid ? rig.path("Hybrid left forearm") : Collections.emptyList();
        leftHandPath = hybrid ? rig.path("Hybrid right forearm") : Collections.emptyList();
    }

    @Override public void setupAnim(T entity, float limbSwing, float limbAmount, float age, float yaw, float pitch) {
        this.age = age;
        visible = !entity.isInvisible();
        pose = rig.pose();
        Motion motion = (hybrid ? HYBRID_MOTION : MOTION).computeIfAbsent(entity, e -> new Motion(age));
        float dt = MathHelper.clamp(age-motion.lastAge,0,2);
        motion.lastAge = age;
        boolean flying=entity instanceof PlayerEntity && ((PlayerEntity)entity).abilities.flying;
        motion.jump.update(age,entity.isOnGround(),entity.isInWater() || entity.isInLava()
                || entity.isPassenger() || entity.isFallFlying() || flying,entity.getDeltaMovement().y);
        rig.apply(pose, "idle", age/20, 1);
        float movement = MathHelper.clamp(limbAmount*2, 0, 1);
        if(hybrid) {
            float response=1-(float)Math.exp(-dt/3);
            motion.movementBlend += (movement-motion.movementBlend)*response;
            motion.lookYaw += (MathHelper.clamp(yaw,-45,45)-motion.lookYaw)*response;
            motion.lookPitch += (MathHelper.clamp(pitch,-35,35)-motion.lookPitch)*response;
            movement=motion.movementBlend;
        }
        double speedSquared = entity.getDeltaMovement().x*entity.getDeltaMovement().x + entity.getDeltaMovement().z*entity.getDeltaMovement().z;
        boolean running = entity.isSprinting() || speedSquared > .09;
        String action = "";
        float charge = 0;
        boolean riding = false;
        for (IAbility ability : AbilityDataCapability.get(entity).getEquippedAndPassiveAbilities()) {
            String type = ability.getClass().getSimpleName();
            boolean active = false, charging = false;
            float progress = 0, activeTime = 0;
            for (AbilityComponent<?> component : ability.getComponents().values()) {
                if (component instanceof ContinuousComponent) {
                    active |= ((ContinuousComponent)component).isContinuous();
                    activeTime = ((ContinuousComponent)component).getContinueTime()/20;
                }
                if (component instanceof ChargeComponent) {
                    ChargeComponent c = (ChargeComponent)component;
                    charging |= c.isCharging(); progress = c.getChargePercentage();
                }
            }
            if (type.equals("CerberusRideableAbility")) riding |= active;
            if (type.equals("HadesCrossAbility") && active) motion.hadesArmedAt = age;
            if (!active && !charging) continue;
            if (ability instanceof net.kazi.kazimod.preserved.cerberus.CerberusAbility) {
                net.kazi.kazimod.preserved.cerberus.CerberusAbility a = (net.kazi.kazimod.preserved.cerberus.CerberusAbility)ability;
                action = a.animation(activeTime);
                charge = MathHelper.clamp(activeTime/2,0,1);
                if(a.move == net.kazi.kazimod.preserved.cerberus.CerberusAbility.Move.FURY) running = true;
                break;
            }
            if (type.equals("GatesOfHellAbility")) { action = charging ? "gates_charge" : "gates_fire"; charge = progress; break; }
            if (type.equals("MaulingAbility")) { action = "mauling"; break; }
            // Fury uses the locomotion gallop, not a second animation layered over its legs.
            if (type.equals("HellHathNoFuryAbility")) { running = true; action = ""; break; }
            if (type.equals("HellHeadMotorAbility") || type.equals("HellHeadMotor2Ability")) action = "hell_head_motor_hold";
            if (type.equals("HadesCrossAbility") && entity.getAttackAnim(age-entity.tickCount) > 0) action = "hades_cross";
        }
        CerberusRig.Pose idleRoot=pose.get("Cerberus - Elbaf");
        if(idleRoot!=null) idleRoot.position[1] *= 1-movement;
        motion.runBlend += MathHelper.clamp((running?1:0)-motion.runBlend,-dt/6,dt/6);
        if (motion.gaitInitialized) motion.gaitPhase += MathHelper.clamp(limbSwing-motion.lastLimbSwing,0,3)/(6+motion.runBlend*4);
        motion.gaitInitialized = true;
        motion.lastLimbSwing = limbSwing;
        motion.gaitPhase %= 1;
        float groundedMovement=movement*(1-motion.jump.weight());
        rig.apply(pose,"walk",motion.gaitPhase*1.2F,groundedMovement*(1-motion.runBlend));
        rig.apply(pose,"run",motion.gaitPhase*.65F,groundedMovement*motion.runBlend);
        float swing = entity.getAttackAnim(MathHelper.clamp(age-entity.tickCount,0,1));
        if (swing > 0 && (motion.previousSwing <= 0 || swing < motion.previousSwing)) {
            if(age-motion.strikeAt>35)motion.biteIndex=-1;
            motion.strikeAt = age;
            motion.biteIndex = (motion.biteIndex + 1) % 3;
            motion.hadesStrike = age-motion.hadesArmedAt < 5;
        }
        motion.previousSwing = swing;
        // Cart ends Hades Cross on the first swing tick; retain its full follow-through after that packet.
        if (motion.hadesStrike && age-motion.strikeAt < 13) action = "hades_cross";
        if (!action.equals(motion.action)) {
            motion.previousAction = motion.action;
            motion.previousTime = motion.lastTime;
            motion.action = action;
            motion.since = age;
        }
        float elapsed = (age-motion.since)/20;
        float weight = MathHelper.clamp((age-motion.since)/4,0,1);
        if(hybrid)weight=weight*weight*(3-2*weight);
        if (weight < 1 && !motion.previousAction.isEmpty()) {
            rig.apply(pose,motion.previousAction,motion.previousTime+elapsed,1-weight);
        }
        if (!action.isEmpty()) {
            float time = action.equals("gates_charge") ? charge : action.equals("hades_cross") ? (age-motion.strikeAt)/20 : elapsed;
            motion.lastTime = time;
            rig.apply(pose, action, time, weight);
        } else if (age-motion.strikeAt < 9) {
            rig.apply(pose, "cerberus_bite_"+Math.max(0,motion.biteIndex), (age-motion.strikeAt)/20, 1);
        }
        if (headless) rig.apply(pose, "hell_head_motor_hold", age/20, 1);
        if (riding || entity.isVehicle()) rig.apply(pose, "rideable", age/20, 1);
        rig.apply(pose,"leap",motion.jump.time(),motion.jump.weight());
        if (age-motion.firstSeen < 24) rig.apply(pose, "guard_point", (age-motion.firstSeen)/20, 1);
        for (String head : new String[]{"Left", "Crown", "Right"}) {
            CerberusRig.Pose p = pose.computeIfAbsent("05 "+head+" head", k -> new CerberusRig.Pose());
            p.rotation[1] -= (hybrid ? motion.lookYaw : MathHelper.clamp(yaw,-45,45))*.5F;
            p.rotation[0] -= (hybrid ? motion.lookPitch : MathHelper.clamp(pitch,-35,35))*.5F;
        }
    }
    @Override public void renderToBuffer(MatrixStack stack, IVertexBuilder buffer, int light, int overlay, float r, float g, float b, float alpha) {
        stack.pushPose();
        stack.translate(0,1.5,0);
        stack.scale(1/16F,1/16F,1/16F);
        // MatrixStack.scale uses fastInvCubeRoot on the determinant. A one-axis reflection
        // feeds it a negative value and corrupts lighting normals; apply the reflection exactly.
        stack.last().pose().multiply(Matrix4f.createScaleMatrix(1,-1,1));
        stack.last().normal().mul(Matrix3f.createScaleMatrix(1,-1,1));
        draw(rig.nodes, stack, buffer, light, overlay, r,g,b,alpha, pose, headless, false);
        if (visible && alpha > .15F) {
            if(hybrid) { stack.translate(0,27,3); stack.scale(.72F,.72F,.72F); }
            IVertexBuilder cloud = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(CLOUD);
            CerberusCloudMesh.render((x,y,z,color,a) -> cloud.vertex(stack.last().pose(),x,y,z)
                    .color((color>>16)&255,(color>>8)&255,color&255,(int)(a*255)).endVertex(),age,alpha);
        }
        stack.popPose();
    }
    public static void draw(CerberusRig.Node[] nodes, MatrixStack stack, IVertexBuilder buffer, int light, int overlay,
                            float r,float g,float b,float alpha, Map<String,CerberusRig.Pose> pose, boolean headless, boolean headsOnly) {
        for (CerberusRig.Node node : nodes) {
            if (!node.visible || headless && node.name.startsWith("05 ")) continue;
            if (node.children != null) {
                if (headsOnly && node.name.startsWith("05 ")) {
                    drawGroup(node,stack,buffer,light,overlay,r,g,b,alpha,pose,false,false);
                } else if (headsOnly) {
                    drawGroup(node,stack,buffer,light,overlay,r,g,b,alpha,pose,false,true);
                } else drawGroup(node,stack,buffer,light,overlay,r,g,b,alpha,pose,headless,false);
            } else if (!headsOnly && node.quads != null) for (CerberusRig.Quad quad : node.quads) {
                // Negative Y conversion reverses winding, so emit the face in reverse order.
                for (int i=3;i>=0;i--) { float[] v=quad.vertices[i];
                    buffer.vertex(stack.last().pose(),v[0],v[1],v[2]).color(r,g,b,alpha).uv(v[3],v[4])
                            .overlayCoords(overlay).uv2(light).normal(stack.last().normal(),quad.normal[0],quad.normal[1],quad.normal[2]).endVertex();
                }
            }
        }
    }
    private static void drawGroup(CerberusRig.Node node, MatrixStack stack, IVertexBuilder buffer,int light,int overlay,
                                   float r,float g,float b,float alpha,Map<String,CerberusRig.Pose> pose,boolean headless,boolean headsOnly) {
        stack.pushPose();
        applyGroupTransform(node,stack,pose);
        draw(node.children,stack,buffer,light,overlay,r,g,b,alpha,pose,headless,headsOnly);
        stack.popPose();
    }
    private static void applyGroupTransform(CerberusRig.Node node, MatrixStack stack, Map<String,CerberusRig.Pose> pose) {
        CerberusRig.Pose p=pose.get(node.name);
        float[] o=node.origin, rot=node.rotation;
        if(p!=null) stack.translate(p.position[0],p.position[1],p.position[2]);
        stack.translate(o[0],o[1],o[2]);
        stack.mulPose(Vector3f.ZP.rotationDegrees(rot[2]+(p==null?0:p.rotation[2])));
        stack.mulPose(Vector3f.YP.rotationDegrees(rot[1]+(p==null?0:p.rotation[1])));
        stack.mulPose(Vector3f.XP.rotationDegrees(rot[0]+(p==null?0:p.rotation[0])));
        stack.translate(-o[0],-o[1],-o[2]);
    }
    public static void renderHeads(MatrixStack stack, IVertexBuilder buffer, int light, int overlay, float age) {
        Map<String,CerberusRig.Pose> p=Data.RIG.pose();
        Data.RIG.apply(p,"mauling",age/20,1);
        if(age<16) Data.RIG.apply(p,"hell_head_motor",age/20,1);
        draw(Data.RIG.nodes,stack,buffer,light,overlay,1,1,1,1,p,false,true);
    }
    @Override public boolean renderItemInHand(T entity, HandSide side, MatrixStack stack) { return false; }
    @Override public void translateToHand(HandSide side, MatrixStack stack) {
        if(!hybrid) { super.translateToHand(side,stack); return; }
        boolean right=side==HandSide.RIGHT;
        stack.translate(0,1.5,0);
        stack.scale(1/16F,1/16F,1/16F);
        stack.last().pose().multiply(Matrix4f.createScaleMatrix(1,-1,1));
        stack.last().normal().mul(Matrix3f.createScaleMatrix(1,-1,1));
        for(CerberusRig.Node bone : right ? rightHandPath : leftHandPath) applyGroupTransform(bone,stack,pose);
        stack.translate(right?-19:19,20,-.5);
        // Return to Minecraft item units/orientation, without mirroring the held item.
        stack.last().pose().multiply(Matrix4f.createScaleMatrix(1,-1,1));
        stack.last().normal().mul(Matrix3f.createScaleMatrix(1,-1,1));
        stack.scale(16,16,16);
        // HeldItemLayer adds its normal wrist offset after this hook. Cancel that
        // offset here because this attachment already sits at the animated palm.
        stack.translate((right?1:-1)/16.0,-.625,.125);
    }
    @Override public void renderFirstPersonArm(MatrixStack stack, IVertexBuilder buffer,int light,int overlay,float r,float g,float b,float alpha,HandSide side) {
        // Full-form paws are deliberately hidden in first person.
    }
    @Override public void renderFirstPersonLeg(MatrixStack stack,IVertexBuilder buffer,int light,int overlay,float r,float g,float b,float alpha,HandSide side) {
        // Do not substitute a paw during first-person leg/ability renders either.
    }
}
