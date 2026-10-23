package net.kazi.kazimod.client.zoan;

import com.mojang.blaze3d.matrix.MatrixStack;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Pose;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.api.helpers.MorphHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.items.AkumaNoMiItem;

@Mod.EventBusSubscriber(modid = "kazimod", value = Dist.CLIENT)
public final class ZoanTransitions {
    private static final Map<AbstractClientPlayerEntity, State> STATES = new WeakHashMap<>();
    private static Object level;
    private static Context rendering;

    private static final class State {
        MorphInfo observed;
        Transition transition;
        State(MorphInfo observed) { this.observed = observed; }
    }

    private static final class Transition {
        final MorphInfo from, to;
        final ZoanTransitionMotion motion;
        final int start;
        final float fromHeight, toHeight, fromWidth, toWidth;
        EntityRenderer<?> sourceRenderer, targetRenderer;
        final Map<Object, Map<ModelRenderer, Bone>> joints = new IdentityHashMap<>();
        final Map<Object, ZoanTransitionPose> restPoses = new IdentityHashMap<>();
        Transition(MorphInfo from, MorphInfo to, int start) {
            this.from = from;
            this.to = to;
            this.start = start;
            MorphInfo form = to != null ? to : from;
            String name = form.getForm() + " " + form.getRegistryName();
            motion = ZoanTransitionMotion.forForm(name, form.isPartial());
            fromHeight = height(from);
            toHeight = height(to);
            fromWidth = width(from, fromHeight);
            toWidth = width(to, toHeight);
        }
        float time(LivingEntity player, float partial) {
            return ZoanTransitionMotion.clamp((player.tickCount + partial - start) / motion.duration);
        }
    }

    private enum Bone { HEAD, JAW, BODY, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG, LEFT_WING, RIGHT_WING, TAIL, TRUNK, NECK }
    private static final class Context {
        final LivingEntity player;
        final Transition transition;
        final float time;
        final float alpha;
        final boolean human, source;
        final Map<ModelRenderer, Bone> bones;
        Context(LivingEntity player, Transition transition, float time, float alpha, boolean human, boolean source, Map<ModelRenderer, Bone> bones) {
            this.player = player; this.transition = transition; this.time = time;
            this.alpha = alpha;
            this.human = human; this.source = source; this.bones = bones;
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (level != mc.level) { STATES.clear(); rendering = null; level = mc.level; }
        if (mc.level == null || mc.isPaused()) return;
        for (AbstractClientPlayerEntity player : mc.level.players()) {
            MorphInfo current = MorphHelper.getZoanInfo(player);
            State state = STATES.get(player);
            // Joining, respawning and first tracking an already transformed player are not transformations.
            if (state == null) { STATES.put(player, new State(current)); continue; }
            if (!player.isAlive() || player.isSpectator()) { state.observed = current; state.transition = null; continue; }
            if (current != state.observed) {
                boolean fromZoan = isZoan(state.observed), toZoan = isZoan(current);
                state.transition = (toZoan && (state.observed == null || fromZoan)) || (current == null && fromZoan)
                        ? new Transition(state.observed, current, player.tickCount) : null;
                state.observed = current;
            }
            if (state.transition != null && player.tickCount - state.transition.start >= state.transition.motion.duration)
                state.transition = null;
        }
        STATES.keySet().removeIf(player -> player.removed || player.level != mc.level);
    }

    private static boolean isZoan(MorphInfo info) {
        if (info == null) return false;
        AkumaNoMiItem fruit = info.getDevilFruit();
        return fruit != null && fruit.getType() != null && fruit.getType().name().endsWith("ZOAN");
    }

    private static float height(MorphInfo info) {
        if (info == null) return 1.8F;
        float height = (float)Math.max(1, info.getEyeHeight() * 1.12);
        Map<Pose, EntitySize> sizes = info.getSizes();
        if (sizes != null && sizes.get(Pose.STANDING) != null) height = Math.max(height, sizes.get(Pose.STANDING).height);
        return Math.min(64, height);
    }

    private static float width(MorphInfo info, float height) {
        if (info == null) return .65F;
        Map<Pose, EntitySize> sizes = info.getSizes();
        float width = sizes != null && sizes.get(Pose.STANDING) != null ? sizes.get(Pose.STANDING).width : height * .45F;
        if (info.getForm().contains("world_turtle")) width = Math.max(width, 22);
        return Math.min(48, Math.max(.65F, width));
    }

    /** Called before MMNM replaces the normal renderer, including the legacy morph path. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static boolean render(RenderLivingEvent.Pre<?, ?> event) {
        if (rendering != null && rendering.player == event.getEntity()) return true;
        if (!(event.getEntity() instanceof AbstractClientPlayerEntity)) return false;
        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity)event.getEntity();
        State state = STATES.get(player);
        if (state == null || state.transition == null || !player.isAlive() || player.isSpectator() || player.isInvisible()) return false;
        Transition transition = state.transition;
        float time = transition.time(player, event.getPartialRenderTick());
        if (time >= 1) return false;
        event.setCanceled(true);
        float sourceAlpha = ZoanTransitionMotion.sourceOpacity(time);
        float targetAlpha = ZoanTransitionMotion.targetOpacity(time);
        if (sourceAlpha > .005F) renderPass(event, player, transition, time, true, sourceAlpha);
        if (targetAlpha > .005F) renderPass(event, player, transition, time, false, targetAlpha);
        float growth = ZoanTransitionMotion.reveal(time);
        ZoanTransitionVfx.render(event.getMatrixStack(), event.getBuffers(), transition.motion, time,
                MathHelper.lerp(growth, transition.fromHeight, transition.toHeight),
                MathHelper.lerp(growth, transition.fromWidth, transition.toWidth));
        return true;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void renderPass(RenderLivingEvent.Pre<?, ?> event, AbstractClientPlayerEntity player,
                                   Transition transition, float time, boolean source, float alpha) {
        MorphInfo info = source ? transition.from : transition.to;
        EntityRenderer renderer = source ? transition.sourceRenderer : transition.targetRenderer;
        if (renderer == null) {
            renderer = info == null ? event.getRenderer() : info.getRendererFactory(player).createRenderFor(Minecraft.getInstance().getEntityRenderDispatcher());
            if (renderer instanceof xyz.pixelatedw.mineminenomi.renderers.morphs.ZoanMorphRenderer2)
                ((xyz.pixelatedw.mineminenomi.renderers.morphs.ZoanMorphRenderer2)renderer).setOriginalRenderer(event.getRenderer());
            if (source) transition.sourceRenderer = renderer; else transition.targetRenderer = renderer;
        }
        Map<ModelRenderer, Bone> bones = renderer instanceof LivingRenderer
                ? transition.joints.computeIfAbsent(((LivingRenderer)renderer).getModel(), ZoanTransitions::bones) : new IdentityHashMap<>();
        Context previous = rendering;
        MatrixStack stack = event.getMatrixStack();
        Object model = renderer instanceof LivingRenderer ? ((LivingRenderer)renderer).getModel() : null;
        // Full-form models are new when cached; shared player/partial models must retain their current pose.
        ZoanTransitionPose restPose = model == null ? null : info == null || info.isPartial()
                ? ZoanTransitionPose.capture(model)
                : transition.restPoses.computeIfAbsent(model, ZoanTransitionPose::capture);
        stack.pushPose();
        try {
            if (restPose != null) restPose.restore();
            rendering = new Context(player, transition, time, alpha, info == null, source, bones);
            transform(stack, player, transition, time, source);
            renderer.render(player, player.yRot, event.getPartialRenderTick(), stack,
                    alpha > .999F ? event.getBuffers() : new ZoanTransitionBuffer(event.getBuffers(), alpha), event.getLight());
        } finally {
            if (restPose != null) restPose.restore();
            rendering = previous;
            stack.popPose();
        }
    }

    public static IRenderTypeBuffer buffers(IRenderTypeBuffer original) {
        return rendering == null || rendering.alpha > .999F ? original : new ZoanTransitionBuffer(original, rendering.alpha);
    }

    private static void transform(MatrixStack stack, LivingEntity player, Transition transition, float time, boolean source) {
        if (source) {
            float brace = ZoanTransitionMotion.windup(time);
            stack.translate(0, -.11F * brace, 0);
            stack.scale(1 + brace * .035F, 1 - brace * .065F, 1 + brace * .035F);
            return;
        }
        float x = ZoanTransitionMotion.scale(transition.fromWidth, transition.toWidth, time);
        float y = ZoanTransitionMotion.scale(transition.fromHeight, transition.toHeight, time);
        float release = ZoanTransitionMotion.release(time);
        float tilt = 0;
        switch (transition.motion.shape) {
            case BEAST: tilt = -7 * release; break;
            case SHELL: tilt = 4 * release; break;
            case WINGED: stack.translate(0, .32F * release, 0); tilt = -5 * release; break;
            case SERPENT: stack.mulPose(Vector3f.YP.rotationDegrees(14 * release)); break;
            case GIANT: tilt = 3 * release; break;
            default: tilt = -3 * release;
        }
        stack.scale(x, y, x);
        stack.mulPose(Vector3f.YP.rotationDegrees(-player.yBodyRot));
        stack.mulPose(Vector3f.XP.rotationDegrees(tilt));
        stack.mulPose(Vector3f.YP.rotationDegrees(player.yBodyRot));
    }

    public static void playerPose(PlayerModel<?> model, LivingEntity player) {
        Context context = rendering;
        if (context == null || context.player != player || !context.human) return;
        float brace = context.source ? ZoanTransitionMotion.windup(context.time) : 1 - ZoanTransitionMotion.reveal(context.time);
        float flare = context.source ? 0 : ZoanTransitionMotion.release(context.time);
        boolean wings = context.transition.motion.shape == ZoanTransitionMotion.Shape.WINGED;
        model.head.xRot = .28F * brace - .18F * flare;
        model.body.xRot = .18F * brace;
        model.rightArm.xRot = -.9F * brace;
        model.leftArm.xRot = -.9F * brace;
        model.rightArm.yRot = -.35F * brace;
        model.leftArm.yRot = .35F * brace;
        model.rightArm.zRot = -.18F * brace - (wings ? 1.1F : .45F) * flare;
        model.leftArm.zRot = .18F * brace + (wings ? 1.1F : .45F) * flare;
        model.rightLeg.xRot = -.25F * brace;
        model.leftLeg.xRot = -.25F * brace;
        model.rightLeg.zRot = .12F * brace;
        model.leftLeg.zRot = -.12F * brace;
        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
    }

    private static Map<ModelRenderer, Bone> bones(Object model) {
        Map<ModelRenderer, Bone> result = new IdentityHashMap<>();
        if (model instanceof PlayerModel) {
            PlayerModel<?> m = (PlayerModel<?>)model;
            result.put(m.head, Bone.HEAD); result.put(m.hat, Bone.HEAD);
            result.put(m.body, Bone.BODY); result.put(m.jacket, Bone.BODY);
            result.put(m.leftArm, Bone.LEFT_ARM); result.put(m.leftSleeve, Bone.LEFT_ARM);
            result.put(m.rightArm, Bone.RIGHT_ARM); result.put(m.rightSleeve, Bone.RIGHT_ARM);
            result.put(m.leftLeg, Bone.LEFT_LEG); result.put(m.leftPants, Bone.LEFT_LEG);
            result.put(m.rightLeg, Bone.RIGHT_LEG); result.put(m.rightPants, Bone.RIGHT_LEG);
        }
        Map<Bone, Field> roots = new EnumMap<>(Bone.class);
        for (Class<?> type = model.getClass(); type != null && type != PlayerModel.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || !ModelRenderer.class.isAssignableFrom(field.getType())) continue;
                Bone bone = bone(field.getName());
                if (bone == null) continue;
                Field existing = roots.get(bone);
                if (existing == null || field.getName().length() < existing.getName().length()) roots.put(bone, field);
            }
        }
        // Animate the root of each limb, not every segment in a chain.
        roots.forEach((bone, field) -> {
            try { field.setAccessible(true); Object part = field.get(model); if (part != null) result.put((ModelRenderer)part, bone); }
            catch (ReflectiveOperationException | SecurityException ignored) { }
        });
        return result;
    }

    private static Bone bone(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        boolean left = n.contains("left") || n.startsWith("l_") || n.endsWith("_l");
        if (n.contains("wing")) return left ? Bone.LEFT_WING : Bone.RIGHT_WING;
        if (n.contains("jaw") || n.contains("mouth")) return Bone.JAW;
        if (n.contains("trunk")) return Bone.TRUNK;
        if (n.contains("head")) return Bone.HEAD;
        if (n.contains("neck")) return Bone.NECK;
        if (n.contains("tail")) return Bone.TAIL;
        if (n.contains("arm")) return left ? Bone.LEFT_ARM : Bone.RIGHT_ARM;
        if (n.contains("leg")) return left ? Bone.LEFT_LEG : Bone.RIGHT_LEG;
        if (n.equals("body") || n.equals("torso") || n.equals("chest")) return Bone.BODY;
        return null;
    }

    public static void rigJoint(String name, MatrixStack stack) {
        if (rendering == null || rendering.human) return;
        String n = name.toLowerCase(Locale.ROOT);
        if (n.contains("lower") || n.contains("paw") || n.contains("foot") || n.contains("shin")
                || n.contains("tongue") || n.contains("collar") || n.contains("cheek") || n.contains("tusk")
                || n.contains("ear") && !n.contains("rear") || n.contains("middle") || n.contains("tip")) return;
        Bone bone;
        if (n.contains("flipper")) {
            boolean left = n.contains("left") || n.endsWith("2");
            bone = n.contains("rear") ? (left ? Bone.LEFT_LEG : Bone.RIGHT_LEG) : (left ? Bone.LEFT_ARM : Bone.RIGHT_ARM);
        } else if (n.contains("jaw")) bone = Bone.JAW;
        else if (n.contains("neck")) bone = Bone.NECK;
        else if (n.endsWith("head")) bone = Bone.HEAD;
        else if (n.endsWith("body")) bone = Bone.BODY;
        else bone = bone(n);
        if (bone != null) joint(bone, stack, rendering, true);
    }

    /** Adds a temporary joint rotation to the matrix; never changes the underlying model. */
    public static void joint(ModelRenderer part, MatrixStack stack) {
        Context context = rendering;
        if (context == null || context.human) return;
        Bone bone = context.bones.get(part);
        if (bone == null) return;
        joint(bone, stack, context, false);
    }

    private static void joint(Bone bone, MatrixStack stack, Context context, boolean yUp) {
        float amount = context.source ? ZoanTransitionMotion.windup(context.time) * .35F : ZoanTransitionMotion.release(context.time);
        float x = 0, y = 0, z = 0;
        switch (bone) {
            case HEAD: x = -.24F * amount; break;
            case JAW: x = .28F * amount; break;
            case BODY: x = .06F * amount; break;
            case NECK: x = -.08F * amount; break;
            case LEFT_ARM: x = -.22F * amount; z = .23F * amount; break;
            case RIGHT_ARM: x = -.22F * amount; z = -.23F * amount; break;
            case LEFT_LEG: x = .12F * amount; z = -.06F * amount; break;
            case RIGHT_LEG: x = .12F * amount; z = .06F * amount; break;
            case LEFT_WING: z = -.85F * fold(context); y = .22F * amount; break;
            case RIGHT_WING: z = .85F * fold(context); y = -.22F * amount; break;
            case TAIL: y = (float)Math.sin(context.time * Math.PI * 2) * amount * .12F; break;
            case TRUNK: x = -.30F * amount; break;
        }
        if (yUp) { x = -x; z = -z; }
        if (z != 0) stack.mulPose(Vector3f.ZP.rotation(z));
        if (y != 0) stack.mulPose(Vector3f.YP.rotation(y));
        if (x != 0) stack.mulPose(Vector3f.XP.rotation(x));
        if (!context.source && context.transition.motion.shape == ZoanTransitionMotion.Shape.SHELL
                && bone != Bone.BODY && bone != Bone.TAIL) {
            float extend = .15F + .85F * ZoanTransitionMotion.reveal(context.time);
            stack.scale(extend, extend, extend);
        }
    }

    private static float fold(Context context) {
        return context.source ? ZoanTransitionMotion.windup(context.time) * .35F : 1 - ZoanTransitionMotion.reveal(context.time);
    }

    @SubscribeEvent
    public static void camera(EntityViewRenderEvent.CameraSetup event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.options.getCameraType().isFirstPerson()) return;
        State state = STATES.get(mc.player);
        if (state == null || state.transition == null || !mc.player.isAlive()) return;
        float time = state.transition.time(mc.player, (float)event.getRenderPartialTicks());
        float envelope = (float)Math.sin(time * Math.PI);
        event.setPitch(event.getPitch() + 1.3F * envelope * (float)Math.sin(time * Math.PI * 2));
        event.setRoll(event.getRoll() + .4F * envelope * (float)Math.sin(time * Math.PI * 4));
    }
}
