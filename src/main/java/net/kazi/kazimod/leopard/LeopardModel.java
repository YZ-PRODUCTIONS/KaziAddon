package net.kazi.kazimod.leopard;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.models.zoan.CerberusRig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.HandSide;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.MorphAbility2;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.MorphHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphModel;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;

public final class LeopardModel<T extends LivingEntity> extends MorphModel<T> {
    private static final String[] NAMES = {"full", "hybrid", "awakened"};
    private static final CerberusRig[] RIGS = new CerberusRig[3];
    private static final ResourceLocation[] TEXTURES = {
            new ResourceLocation("kazimod", "textures/zoan/leopard_full.png"),
            new ResourceLocation("kazimod", "textures/zoan/leopard_hybrid.png"),
            new ResourceLocation("kazimod", "textures/zoan/leopard_awakened.png")};
    private static final Map<LivingEntity, State[]> STATES = new WeakHashMap<>();
    private static final class State {
        final LeopardAnimation animation = new LeopardAnimation();
        int abilityTick = Integer.MIN_VALUE;
        ContinuousComponent transformation;
        boolean leap, claws, barrage, kamisori;
        LeopardAltMode hunt;
        float barrageBlend, lastAge=Float.NaN;
        void abilities(LivingEntity entity) {
            if (abilityTick == entity.tickCount) return;
            abilityTick = entity.tickCount; transformation = null; leap = false; claws = false; barrage=false;kamisori=false;hunt=null;
            for (IAbility ability : AbilityDataCapability.get(entity).getEquippedAndPassiveAbilities()) {
                LeopardAltMode modes=LeopardAltMode.get(ability);
                if(modes!=null&&modes.active()){if(modes.barrage)barrage=true;else hunt=modes;}
                if(ability instanceof KamisoriAbility)kamisori=((KamisoriAbility)ability).active();
                boolean form = ability instanceof MorphAbility2 && ((MorphAbility2)ability).getTransformation() == MorphHelper.getZoanInfo(entity);
                boolean jump = ability instanceof xyz.pixelatedw.mineminenomi.abilities.nekoleopard.FerociousLeapAbility && (modes==null||!modes.alternate());
                boolean claw = ability instanceof xyz.pixelatedw.mineminenomi.abilities.nekoleopard.ClawStrikeAbility && (modes==null||!modes.alternate());
                if (!form && !jump && !claw) continue;
                for (AbilityComponent<?> component : ability.getComponents().values()) if (component instanceof ContinuousComponent) {
                    ContinuousComponent continuous = (ContinuousComponent)component;
                    if (continuous.isContinuous()) { if (form) transformation = continuous; if (jump) leap = true; if (claw) claws = true; }
                }
            }
            leap|=kamisori||hunt!=null&&!hunt.pinning();
        }
    }
    private final int kind;
    private final CerberusRig rig;
    private Map<String, CerberusRig.Pose> pose = Collections.emptyMap();
    private float cloudAge;
    private boolean showClouds;
    private net.minecraft.util.math.vector.Vector3d grabbedPosition;
    private float grabbedRadius;
    private boolean dashTrail;
    public LeopardModel(int kind) { super(0); this.kind = kind; this.rig = rig(kind); }
    public static ResourceLocation texture(int kind) { return TEXTURES[kind]; }
    public static CerberusRig rig(int kind) {
        if (RIGS[kind] == null) {
            try (InputStream in = LeopardModel.class.getResourceAsStream("/assets/kazimod/models/zoan/leopard_" + NAMES[kind] + ".json")) {
                if (in == null) throw new IllegalStateException("Missing leopard model: " + NAMES[kind]);
                RIGS[kind] = CerberusRig.read(new InputStreamReader(in, StandardCharsets.UTF_8));
            } catch (java.io.IOException ex) { throw new IllegalStateException("Cannot load leopard model", ex); }
        }
        return RIGS[kind];
    }
    @Override public void setupAnim(T entity, float swing, float amount, float age, float yaw, float pitch) {
        State[] variants = STATES.computeIfAbsent(entity, key -> new State[3]);
        State state = variants[kind]; if (state == null) variants[kind] = state = new State();
        state.abilities(entity);
        dashTrail=state.kamisori&&!entity.isInvisible();
        cloudAge = age;
        showClouds = !entity.isInvisible() && !(entity == net.minecraft.client.Minecraft.getInstance().player
                && net.minecraft.client.Minecraft.getInstance().options.getCameraType().isFirstPerson());
        float partial = MathHelper.clamp(age - entity.tickCount, 0, 1);
        boolean leftHand = (entity.getMainArm() == HandSide.LEFT) != (entity.swingingArm == Hand.OFF_HAND);
        state.animation.update(age, amount, entity.isSprinting(), entity.isOnGround(), entity.isInWater(),
                entity.isCrouching(), entity.isBlocking(), state.leap, state.claws, entity.getAttackAnim(partial),
                leftHand, yaw, pitch, (float)entity.getDeltaMovement().y, entity.yBodyRot);
        float transforming = state.transformation != null ? (state.transformation.getContinueTime() + partial) / 20 : -1;
        pose = state.animation.sample(rig, state.leap, transforming);
        if(state.hunt!=null&&state.hunt.pinning()) {
            pose=rig.pose();rig.apply(pose,"idle",age/20,1);
            float strike=state.hunt.strikeTime()+partial/20;
            Map<String,CerberusRig.Pose> claws=rig.pose();
            rig.apply(claws,strike<.3F?"slash_right":"slash_left",(strike%.3F)/.3F*.55F,1);
            pose.putAll(claws);
        }
        if(state.kamisori){pose.computeIfAbsent("Body",k->new CerberusRig.Pose()).rotation[0]-=25;}
        float dt=Float.isFinite(state.lastAge)?MathHelper.clamp(age-state.lastAge,0,4):1;
        state.lastAge=age;
        state.barrageBlend=MathHelper.lerp(1-(float)Math.exp(-dt/1.3F),state.barrageBlend,state.barrage?1:0);
        if(state.barrageBlend>.001F)LeopardCombatVisual.barrage(pose,age,state.barrageBlend);
        LivingEntity captive=kind==2?LeopardCombatVisual.target(entity):null;
        grabbedPosition=captive==null?null:LeopardCombatVisual.localTarget(entity,captive,partial);
        if(captive!=null){grabbedRadius=Math.max(5,captive.getBbWidth()*8+2);pose.put("Tail Grab",new CerberusRig.Pose());}
    }
    @Override public void renderToBuffer(MatrixStack stack, IVertexBuilder buffer, int light, int overlay, float r, float g, float b, float alpha) {
        stack.pushPose();
        try {
            stack.translate(0, 1.5, 0);
            stack.scale(1/16F, 1/16F, 1/16F);
            stack.last().pose().multiply(Matrix4f.createScaleMatrix(1, -1, 1));
            stack.last().normal().mul(Matrix3f.createScaleMatrix(1, -1, 1));
            draw(rig.nodes, pose, stack, buffer, light, overlay, r, g, b, alpha, true);
            if(grabbedPosition!=null)LeopardCombatVisual.tail(stack,buffer,grabbedPosition,grabbedRadius,cloudAge,light,overlay);
            if(dashTrail) {
                IVertexBuilder trail=net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource()
                        .getBuffer(net.minecraft.client.renderer.RenderType.entityTranslucent(texture(kind)));
                for(int i=1;i<=2;i++){stack.pushPose();stack.translate(0,0,i*9);draw(rig.nodes,pose,stack,trail,light,overlay,.8F,.85F,1F,alpha*(i==1?.19F:.08F),true);stack.popPose();}
            }
            if (kind == 2 && showClouds) LeopardCloudVfx.render(rig, pose, stack,
                    net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource(), cloudAge, alpha);
        } finally { stack.popPose(); }
    }
    public static void draw(CerberusRig.Node[] nodes, Map<String, CerberusRig.Pose> pose, MatrixStack stack, IVertexBuilder buffer,
                            int light, int overlay, float r, float g, float b, float alpha, boolean reversed) {
        for (CerberusRig.Node node : nodes) {
            if(node.name.equals("Tail 0")&&pose.containsKey("Tail Grab"))continue;
            if (!node.visible) continue;
            if (node.children != null) {
                stack.pushPose();
                CerberusRig.Pose p = pose.get(node.name);
                float[] origin = node.origin, rotation = node.rotation;
                stack.translate(origin[0] + (p == null ? 0 : p.position[0]), origin[1] + (p == null ? 0 : p.position[1]), origin[2] + (p == null ? 0 : p.position[2]));
                stack.mulPose(Vector3f.ZP.rotationDegrees(rotation[2] + (p == null ? 0 : p.rotation[2])));
                stack.mulPose(Vector3f.YP.rotationDegrees(rotation[1] + (p == null ? 0 : p.rotation[1])));
                stack.mulPose(Vector3f.XP.rotationDegrees(rotation[0] + (p == null ? 0 : p.rotation[0])));
                stack.translate(-origin[0], -origin[1], -origin[2]);
                draw(node.children, pose, stack, buffer, light, overlay, r, g, b, alpha, reversed);
                stack.popPose();
            } else if (node.quads != null) {
                for (CerberusRig.Quad quad : node.quads) for (int i = 0; i < 4; i++) {
                    float[] v = quad.vertices[reversed ? 3 - i : i];
                    buffer.vertex(stack.last().pose(), v[0], v[1], v[2]).color(r, g, b, alpha).uv(v[3], v[4])
                            .overlayCoords(overlay).uv2(light).normal(stack.last().normal(), quad.normal[0], quad.normal[1], quad.normal[2]).endVertex();
                }
            }
        }
    }
    @Override public boolean renderItemInHand(T user, HandSide side, MatrixStack stack) { return false; }
    @Override public void renderFirstPersonArm(MatrixStack s, IVertexBuilder b, int l, int o, float r, float g, float blue, float a, HandSide side) {}
    @Override public void renderFirstPersonLeg(MatrixStack s, IVertexBuilder b, int l, int o, float r, float g, float blue, float a, HandSide side) {}
}
