package net.kazi.kazimod.gomu;

import java.util.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.*;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.api.helpers.HakiHelper;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModResources;
import xyz.pixelatedw.mineminenomi.renderers.abilities.AbilityProjectileRenderer;

@Mod.EventBusSubscriber(modid = "kazimod", value = Dist.CLIENT)
public final class GomuLimbClient {
    private static final Set<AbilityProjectileEntity> ACTIVE = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final List<ReturnLimb> RETURNS = new ArrayList<>();
    private static final Map<LivingEntity, Anchors> ANCHORS = new WeakHashMap<>();
    private static final Deque<Context> CONTEXT = new ArrayDeque<>();
    private static final Deque<Object> MODELS = new ArrayDeque<>();
    private static World world;

    private static void world(World current) {
        if (world == current) return;
        world = current; ACTIVE.clear(); RETURNS.clear(); ANCHORS.clear(); CONTEXT.clear(); MODELS.clear();
    }

    @SubscribeEvent public static void join(EntityJoinWorldEvent event) {
        if (!event.getWorld().isClientSide || !(event.getEntity() instanceof AbilityProjectileEntity)) return;
        AbilityProjectileEntity p = (AbilityProjectileEntity)event.getEntity();
        if (!GomuLimbs.supports(p)) return;
        world(p.level); ACTIVE.add(p); p.noCulling = true;
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        world(Minecraft.getInstance().level);
        if (world == null) return;
        RETURNS.removeIf(r -> ++r.age > 4 || !r.owner.isAlive() || r.owner.level != world);
        Iterator<AbilityProjectileEntity> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            AbilityProjectileEntity p = iterator.next();
            if (!p.removed && (p.getThrower() == null || p.getThrower().isAlive())) continue;
            LivingEntity owner = p.getThrower();
            GomuLimbData data = (GomuLimbData)p;
            if (owner != null && owner.isAlive() && data.kazi$arm() != 0 && data.kazi$reach() > .2F) {
                RETURNS.removeIf(r -> r.owner == owner && r.side == data.kazi$arm());
                RETURNS.add(new ReturnLimb(p, owner, data.kazi$arm()));
            }
            iterator.remove();
        }
    }

    public static boolean stretched(LivingEntity owner, int side) {
        for (AbilityProjectileEntity p : ACTIVE) if (!p.removed && p.getThrower() == owner
                && ((GomuLimbData)p).kazi$arm() == side) return true;
        for (ReturnLimb r : RETURNS) if (r.owner == owner && r.side == side) return true;
        return false;
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void hand(RenderHandEvent event) {
        AbstractClientPlayerEntity player = Minecraft.getInstance().player;
        if (player == null) return;
        int side = player.getMainArm() == HandSide.RIGHT ? 1 : -1;
        if (event.getHand() == Hand.OFF_HAND) side = -side;
        if (stretched(player, side)) event.setCanceled(true);
    }

    public static boolean render(AbilityProjectileEntity p, float partial, MatrixStack stack, IRenderTypeBuffer buffer,
                                 int light, AbilityProjectileRenderer<?, ?> renderer) {
        if (!GomuLimbs.supports(p)) return false;
        LivingEntity owner = p.getThrower();
        GomuLimbData data = (GomuLimbData)p;
        if (data.kazi$arm() == 0) return false;
        if (owner == null || !owner.isAlive() || p.isInvisible() || owner.isInvisible()) return true;
        Vector3d actual = new Vector3d(MathHelper.lerp(partial, p.xo, p.getX()),
                MathHelper.lerp(partial, p.yo, p.getY()), MathHelper.lerp(partial, p.zo, p.getZ()));
        Vector3d tip = actual;
        if (owner == Minecraft.getInstance().player) {
            float reach = Math.max(0, data.kazi$reach() - data.kazi$speed() * (1 - partial));
            tip = GomuLimbs.endpoint(p, reach, partial);
        }
        draw(p, owner, data.kazi$arm(), root(owner, data.kazi$arm(), partial).subtract(actual),
                tip.subtract(actual), partial, stack, buffer, light, renderer.getTextureLocation(p), 1);
        return true;
    }

    public static void draw(AbilityProjectileEntity p, LivingEntity owner, int side, Vector3d root, Vector3d tip,
                             float partial, MatrixStack stack, IRenderTypeBuffer buffer, int light, ResourceLocation texture, float alpha) {
        if (texture == null) return;
        boolean slim = owner instanceof AbstractClientPlayerEntity && ((AbstractClientPlayerEntity)owner).getModelName().equals("slim");
        boolean hardened = texture.equals(ModResources.BUSOSHOKU_HAKI_ARM);
        float r = 1, g = 1, b = 1;
        if (p.isAffectedByHaki() && HakiHelper.isProjectileHaoshokuInfused(p)) {
            int colour = HakiHelper.getHaoshokuColour(owner);
            r = (colour >> 16 & 255) / 255F; g = (colour >> 8 & 255) / 255F; b = (colour & 255) / 255F;
        }
        Vector3d right = GomuLimbs.right(MathHelper.rotLerp(partial, owner.yBodyRotO, owner.yBodyRot));
        double base = .25 * GomuLimbs.scale(owner), fist = Math.max(base, GomuLimbs.fistWidth(p));
        fist = Math.min(fist, Math.max(base, root.distanceTo(tip) * .65));
        GomuLimbMesh.draw(stack, buffer.getBuffer(RenderType.entityCutoutNoCull(texture)), root, tip, right,
                base, fist, side < 0 && !hardened, slim, false, light, r, g, b, alpha);
        if (!hardened && owner instanceof AbstractClientPlayerEntity
                && ((AbstractClientPlayerEntity)owner).isModelPartShown(side > 0 ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE)) {
            GomuLimbMesh.draw(stack, buffer.getBuffer(RenderType.entityTranslucent(texture)), root, tip, right,
                    base, fist, side < 0, slim, true, light, r, g, b, alpha);
        }
    }

    private static Vector3d root(LivingEntity owner, int side, float partial) {
        Anchors anchors = ANCHORS.get(owner);
        if (anchors != null && owner.level.getGameTime() - anchors.tick <= 1
                && !(owner == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson())) {
            Vector3d point = side > 0 ? anchors.right : anchors.left;
            if (point != null) return point;
        }
        return GomuLimbs.shoulder(owner, side, partial);
    }

    public static void begin(Entity entity, double x, double y, double z, float partial, MatrixStack stack) {
        if (!(entity instanceof LivingEntity) || !stretched((LivingEntity)entity, 1) && !stretched((LivingEntity)entity, -1)) {
            CONTEXT.push(Context.EMPTY); return;
        }
        Matrix4f inverse = stack.last().pose().copy();
        inverse.invert();
        CONTEXT.push(new Context(entity, new Vector3d(x, y, z), partial, inverse));
    }

    public static void end() {
        if (!CONTEXT.isEmpty()) CONTEXT.pop();
        // Other model mixins may cancel their render before our RETURN callback.
        if (CONTEXT.isEmpty()) MODELS.clear();
    }

    public static void beginModel(Object model) { MODELS.push(model); }
    public static void endModel() { if (!MODELS.isEmpty()) MODELS.pop(); }

    public static boolean skipPart(ModelRenderer part, MatrixStack stack) {
        if (CONTEXT.isEmpty() || !(CONTEXT.peek().entity instanceof LivingEntity)
                || MODELS.isEmpty() || !(MODELS.peek() instanceof BipedModel)) return false;
        BipedModel<?> model = (BipedModel<?>)MODELS.peek();
        if (part == model.rightArm) return skipArm(part, 1, stack);
        if (part == model.leftArm) return skipArm(part, -1, stack);
        if (model instanceof PlayerModel) {
            LivingEntity owner = (LivingEntity)CONTEXT.peek().entity;
            if (part == ((PlayerModel<?>)model).rightSleeve) return stretched(owner, 1);
            if (part == ((PlayerModel<?>)model).leftSleeve) return stretched(owner, -1);
        }
        return false;
    }

    public static boolean skipArm(ModelRenderer arm, int side, MatrixStack stack) {
        if (CONTEXT.isEmpty() || !(CONTEXT.peek().entity instanceof LivingEntity)) return false;
        Context ctx = CONTEXT.peek();
        LivingEntity owner = (LivingEntity)ctx.entity;
        if (!stretched(owner, side)) return false;
        anchor(arm, side, stack, ctx, owner);
        return true;
    }

    private static void anchor(ModelRenderer arm, int side, MatrixStack stack, Context ctx, LivingEntity owner) {
        Vector4f point = new Vector4f((arm.x - side) / 16, arm.y / 16, arm.z / 16, 1);
        point.transform(stack.last().pose()); point.transform(ctx.inverse);
        Vector3d pos = new Vector3d(point.x(), point.y(), point.z()).subtract(ctx.renderPosition).add(GomuLimbs.position(owner, ctx.partial));
        Anchors anchors = ANCHORS.computeIfAbsent(owner, e -> new Anchors());
        anchors.tick = owner.level.getGameTime();
        if (side > 0) anchors.right = pos; else anchors.left = pos;
    }


    @SubscribeEvent public static void returns(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || RETURNS.isEmpty()) return;
        Vector3d camera = mc.gameRenderer.getMainCamera().getPosition();
        IRenderTypeBuffer.Impl buffers = mc.renderBuffers().bufferSource();
        for (ReturnLimb limb : RETURNS) {
            if (limb.owner.isInvisible()) continue;
            Vector3d root = root(limb.owner, limb.side, event.getPartialTicks());
            float t = MathHelper.clamp((limb.age + event.getPartialTicks()) / 4, 0, 1);
            Vector3d tip = root.add(limb.tip.subtract(root).scale((1 - t) * (1 - t)));
            net.minecraft.client.renderer.entity.EntityRenderer<?> renderer = mc.getEntityRenderDispatcher().getRenderer(limb.projectile);
            if (renderer instanceof AbilityProjectileRenderer) draw(limb.projectile, limb.owner, limb.side, root.subtract(camera), tip.subtract(camera),
                    event.getPartialTicks(), event.getMatrixStack(), buffers, mc.getEntityRenderDispatcher().getPackedLightCoords(limb.owner, event.getPartialTicks()),
                    ((AbilityProjectileRenderer<?, ?>)renderer).getTextureLocation(limb.projectile), 1);
        }
    }

    private static final class Context {
        static final Context EMPTY = new Context(null, Vector3d.ZERO, 0, null);
        final Entity entity; final Vector3d renderPosition; final float partial; final Matrix4f inverse;
        Context(Entity entity, Vector3d pos, float partial, Matrix4f inverse) { this.entity = entity; renderPosition = pos; this.partial = partial; this.inverse = inverse; }
    }
    private static final class Anchors { Vector3d right, left; long tick; }
    private static final class ReturnLimb {
        final AbilityProjectileEntity projectile; final LivingEntity owner; final int side; final Vector3d tip; int age;
        ReturnLimb(AbilityProjectileEntity p, LivingEntity owner, int side) { projectile = p; this.owner = owner; this.side = side; tip = p.position(); }
    }
}
