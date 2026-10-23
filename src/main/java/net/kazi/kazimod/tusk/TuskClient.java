package net.kazi.kazimod.tusk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.kazi.kazimod.models.zoan.CerberusRig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.*;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class TuskClient {
    private static final CerberusRig[] RIGS = new CerberusRig[4];
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[4];
    private static final Map<PlayerEntity, Visual> VISUALS = new WeakHashMap<>();
    private static final IRenderTypeBuffer.Impl BUFFERS = IRenderTypeBuffer.immediate(new BufferBuilder(131072));
    private static final String[] CLIPS = {"idle", "summon", "shoot", "glide", "shotgun", "hole", "burst", "slip", "wormhole", "barrage", "beatdown", "infinite", "dismiss", "dodge"};
    private static final ForgeConfigSpec CONFIG;
    private static final ForgeConfigSpec.IntValue HUD_X, HUD_Y;
    private static final ForgeConfigSpec.BooleanValue SHOW_HUD;
    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        SHOW_HUD = b.define("showTuskGauge", true);
        HUD_X = b.defineInRange("tuskGaugeX", -91, -4000, 4000);
        HUD_Y = b.defineInRange("tuskGaugeY", -90, -4000, 0);
        CONFIG = b.build();
        for (int i = 0; i < 4; i++) TEXTURES[i] = new ResourceLocation("kazimod", "textures/entities/tusk_act_" + (i + 1) + ".png");
    }
    private static final class Visual {
        float last, yaw, walk, offset, changed;
        int action = -1, act;
        Vector3d position;
        Map<String, CerberusRig.Pose> previous, from;
    }
    public static void init(IEventBus bus) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CONFIG, "kazimod-tusk-client.toml");
        bus.addListener((FMLClientSetupEvent event) -> {
            RenderingRegistry.registerEntityRenderingHandler(TuskFeatures.PROJECTILE.get(), NailRenderer::new);
            RenderingRegistry.registerEntityRenderingHandler(TuskFeatures.EFFECT.get(), EffectRenderer::new);
        });
        MinecraftForge.EVENT_BUS.addListener(TuskClient::world);
        MinecraftForge.EVENT_BUS.addListener(TuskClient::playerStand);
        MinecraftForge.EVENT_BUS.addListener(TuskClient::hud);
    }
    public static void receive(TuskPacket packet) {
        Minecraft mc = Minecraft.getInstance(); if (mc.level == null || packet.data == null) return;
        Entity entity = mc.level.getEntity(packet.entity);
        if (entity instanceof PlayerEntity) entity.getPersistentData().put(TuskState.TAG, packet.data);
    }
    public static CerberusRig rig(int act) {
        if (RIGS[act - 1] == null) {
            try (InputStream stream = TuskClient.class.getResourceAsStream("/assets/kazimod/models/entities/tusk_act_" + act + ".json")) {
                if (stream == null) throw new IOException("Missing Tusk Act " + act);
                RIGS[act - 1] = CerberusRig.read(new InputStreamReader(stream, StandardCharsets.UTF_8));
            } catch (IOException error) { throw new IllegalStateException("Cannot load Tusk model", error); }
        }
        return RIGS[act - 1];
    }
    private static void world(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getInstance(); if (mc.level == null || mc.player == null) return;
        if (!mc.options.getCameraType().isFirstPerson()) return;
        Vector3d camera = mc.gameRenderer.getMainCamera().getPosition(); float partial = event.getPartialTicks();
        // The local player has no entity render pass in first person.
        renderStands(event.getMatrixStack(), partial, camera, mc.player, Collections.singletonList(mc.player));
    }
    private static void playerStand(RenderPlayerEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        PlayerEntity player = event.getPlayer();
        if (player == mc.player && mc.options.getCameraType().isFirstPerson()) return;
        renderPlayerStand(event.getMatrixStack(), event.getPartialRenderTick(), mc.player, player, event.getBuffers());
    }
    static void renderPlayerStand(MatrixStack stack, float partial, PlayerEntity viewer, PlayerEntity player, IRenderTypeBuffer buffers) {
        // RenderPlayerEvent already translates to the interpolated player position.
        Vector3d origin = new Vector3d(MathHelper.lerp(partial, player.xOld, player.getX()),
                MathHelper.lerp(partial, player.yOld, player.getY()), MathHelper.lerp(partial, player.zOld, player.getZ()));
        renderStands(stack, partial, origin, viewer, Collections.singletonList(player), buffers);
    }
    static void renderStands(MatrixStack stack, float partial, Vector3d camera, PlayerEntity viewer, List<? extends PlayerEntity> players) {
        renderStands(stack, partial, camera, viewer, players, BUFFERS);
        BUFFERS.endBatch();
    }
    private static void renderStands(MatrixStack stack, float partial, Vector3d camera, PlayerEntity viewer, List<? extends PlayerEntity> players, IRenderTypeBuffer buffers) {
        for (PlayerEntity player : players) {
            CompoundNBT data = player.getPersistentData().getCompound(TuskState.TAG);
            int action = TuskState.action(player), act = TuskState.act(player);
            if (!data.getBoolean("Active") || player.isInvisible() || player.distanceToSqr(viewer) > 96 * 96
                    || !data.getBoolean("Manifest") && action != 12) { VISUALS.remove(player); continue; }
            Visual v = VISUALS.computeIfAbsent(player, p -> new Visual()); float time = player.tickCount + partial;
            float dt = MathHelper.clamp(time - v.last, 0, 2); v.last = time;
            float yaw = MathHelper.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
            if (v.position == null || v.act != act) { v.yaw = yaw; v.previous = null; v.from = null; v.act = act; }
            v.yaw += MathHelper.wrapDegrees(yaw - v.yaw) * Math.min(1, dt * .35F);
            boolean front = action == 9 || action == 10;
            v.offset += ((front ? 1 : 0) - v.offset) * Math.min(1, dt * .45F);
            double a = Math.toRadians(v.yaw), forward = MathHelper.lerp(v.offset, -.85F, 1.35F), side = MathHelper.lerp(v.offset, 1.05F, .15F);
            Vector3d base = new Vector3d(MathHelper.lerp(partial, player.xOld, player.getX()),
                    MathHelper.lerp(partial, player.yOld, player.getY()), MathHelper.lerp(partial, player.zOld, player.getZ()));
            Vector3d wanted = base.add(-Math.sin(a) * forward + Math.cos(a) * side, act == 1 ? .85 : act == 2 ? .65 : .05,
                    Math.cos(a) * forward + Math.sin(a) * side);
            if (v.position == null || v.position.distanceToSqr(wanted) > 36) v.position = wanted;
            else v.position = v.position.add(wanted.subtract(v.position).scale(Math.min(1, dt * .45)));
            v.walk += ((float) Math.min(1, Math.hypot(player.getDeltaMovement().x, player.getDeltaMovement().z) * 4) - v.walk) * Math.min(1, dt * .25F);
            float age = Math.max(0, player.level.getGameTime() - data.getLong("ActionStart") + partial);
            CerberusRig rig = rig(act); Map<String, CerberusRig.Pose> pose = rig.pose();
            rig.apply(pose, "idle", time / 20, action == 0 ? 1 - v.walk * .6F : .2F);
            if (action == 0) rig.apply(pose, "follow", time / 20, v.walk);
            else if (action < CLIPS.length) rig.apply(pose, CLIPS[action], age / 20, 1);
            if (v.action != action) { v.action = action; v.from = v.previous; v.changed = time; }
            float blend = TuskMesh.smooth((time - v.changed) / 3);
            if (v.from != null && blend < 1) {
                Set<String> bones = new HashSet<>(v.from.keySet()); bones.addAll(pose.keySet());
                for (String bone : bones) {
                    CerberusRig.Pose old = v.from.get(bone), now = pose.computeIfAbsent(bone, k -> new CerberusRig.Pose());
                    for (int i = 0; i < 3; i++) {
                        now.rotation[i] = MathHelper.lerp(blend, old == null ? 0 : old.rotation[i], now.rotation[i]);
                        now.position[i] = MathHelper.lerp(blend, old == null ? 0 : old.position[i], now.position[i]);
                    }
                }
            } else v.from = null;
            v.previous = pose;
            float alpha = action == 1 ? TuskMesh.smooth(age / 12) : action == 12 ? 1 - TuskMesh.smooth(age / 24) : action == 13 ? .3F : .94F;
            stack.pushPose(); Vector3d position = v.position.subtract(camera); stack.translate(position.x, position.y, position.z);
            stack.mulPose(Vector3f.YP.rotationDegrees(180 - v.yaw));
            float height = act == 1 ? .95F : act == 2 ? 1.35F : act == 3 ? 2.2F : 2.8F;
            TuskMesh.helix(TuskMesh.sink(stack, buffers, true), height * .32, height, 1.1, time * .035F, TuskMesh.PINK, .12F * alpha);
            if (action == 11) {
                float power = TuskMesh.smooth(age / 50);
                TuskMesh.helix(TuskMesh.sink(stack, buffers, true), .5 + power * .6, 2.6, 3, time * .2F, TuskMesh.GOLD, .5F * power);
                TuskMesh.ring(TuskMesh.sink(stack, buffers, true), .8 + power * .4, .025, .03, TuskMesh.GOLD, power * .8F);
            }
            if (action == 9) for (int i = 0; i < 4; i++) {
                double phase = (time * .25 + i * .25) % 1;
                TuskMesh.line(TuskMesh.sink(stack, buffers, true), new Vector3d((i % 2 == 0 ? -.4 : .4), 1.5 + Math.sin(i * 7) * .2, -.1),
                        new Vector3d((i % 2 == 0 ? -.4 : .4), 1.5 + Math.sin(i * 7) * .2, -.6 - phase * 1.2), .09 * (1 - phase), TuskMesh.PINK, (float) (1 - phase) * .5F);
            }
            float scale = (act == 1 ? .55F : act == 2 ? .7F : act == 3 ? .85F : 1) / 16;
            stack.scale(scale, scale, scale);
            draw(rig.nodes, pose, stack, buffers.getBuffer(RenderType.entityTranslucent(TEXTURES[act - 1])), 15728880, alpha);
            stack.popPose();
        }
    }
    public static void draw(CerberusRig.Node[] nodes, Map<String, CerberusRig.Pose> pose, MatrixStack stack, IVertexBuilder buffer, int light, float alpha) {
        for (CerberusRig.Node node : nodes) {
            if (!node.visible) continue;
            if (node.children != null) {
                stack.pushPose(); CerberusRig.Pose p = pose.get(node.name); float[] o = node.origin, r = node.rotation;
                stack.translate(o[0] + (p == null ? 0 : p.position[0]), o[1] + (p == null ? 0 : p.position[1]), o[2] + (p == null ? 0 : p.position[2]));
                stack.mulPose(Vector3f.ZP.rotationDegrees(r[2] + (p == null ? 0 : p.rotation[2])));
                stack.mulPose(Vector3f.YP.rotationDegrees(r[1] + (p == null ? 0 : p.rotation[1])));
                stack.mulPose(Vector3f.XP.rotationDegrees(r[0] + (p == null ? 0 : p.rotation[0])));
                stack.translate(-o[0], -o[1], -o[2]); draw(node.children, pose, stack, buffer, light, alpha); stack.popPose();
            } else if (node.quads != null) for (CerberusRig.Quad q : node.quads) for (float[] point : q.vertices)
                buffer.vertex(stack.last().pose(), point[0], point[1], point[2]).color(1, 1, 1, alpha).uv(point[3], point[4])
                        .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(stack.last().normal(), q.normal[0], q.normal[1], q.normal[2]).endVertex();
        }
    }
    private static void hud(RenderGameOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance(); PlayerEntity p = mc.player;
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL || p == null || p.isSpectator() || !SHOW_HUD.get() || mc.options.hideGui
                || !p.getPersistentData().getCompound(TuskState.TAG).getBoolean("Active")) return;
        int x = MathHelper.clamp(event.getWindow().getGuiScaledWidth() / 2 + HUD_X.get(), 2, Math.max(2, event.getWindow().getGuiScaledWidth() - 184));
        int y = MathHelper.clamp(event.getWindow().getGuiScaledHeight() + HUD_Y.get(), 14, Math.max(14, event.getWindow().getGuiScaledHeight() - 32));
        MatrixStack stack = event.getMatrixStack();
        AbstractGui.fill(stack, x, y, x + 182, y + 5, 0xc0201729);
        AbstractGui.fill(stack, x + 1, y + 1, x + 1 + Math.round(180 * TuskState.spin(p) / 100), y + 4, 0xffefd17b);
        mc.font.drawShadow(stack, "Tusk ACT " + TuskState.act(p), x, y - 11, 0xf3c0e2);
        String spin = (int) TuskState.spin(p) + "%"; mc.font.drawShadow(stack, spin, x + 182 - mc.font.width(spin), y - 11, 0xffe7b1);
        for (int i = 0; i < 10; i++) {
            int at = x + i * 18;
            AbstractGui.fill(stack, at + 1, y + 8, at + 15, y + 12, 0xb02a2034);
            float fill = MathHelper.clamp(TuskState.nails(p) - i, 0, 1);
            AbstractGui.fill(stack, at + 2, y + 9, at + 2 + Math.round(12 * fill), y + 11, fill >= 1 ? 0xffe9a7d3 : 0xff9877aa);
        }
        RenderSystem.color4f(1, 1, 1, 1);
    }
    public static void pose(PlayerModel<?> model, LivingEntity user, float age) {
        if (!user.getPersistentData().getCompound(TuskState.TAG).getBoolean("Active")) return;
        int action = TuskState.action(user); if (action == 0 || action == 1 || action == 12) return;
        CompoundNBT data = TuskState.get(user);
        float t = Math.max(0, user.level.getGameTime() - data.getLong("ActionStart") + age - user.tickCount);
        float remaining = Math.max(0, data.getLong("ActionEnd") - user.level.getGameTime());
        float blend = TuskMesh.smooth(t / 3) * TuskMesh.smooth(remaining / 3);
        if (action == 2 || action == 4 || action == 5 || action == 8 || action == 11) {
            model.rightArm.xRot = MathHelper.lerp(blend, model.rightArm.xRot, -1.45F + model.head.xRot);
            model.rightArm.yRot = MathHelper.lerp(blend, model.rightArm.yRot, -.12F);
            if (action == 11) { model.leftArm.xRot = MathHelper.lerp(blend, model.leftArm.xRot, -1.05F); model.leftArm.zRot = -.25F * blend; }
        } else if (action == 9 || action == 10) {
            model.rightArm.xRot = MathHelper.lerp(blend, model.rightArm.xRot, -1.1F + .25F * MathHelper.sin(t * .8F));
            model.leftArm.xRot = MathHelper.lerp(blend, model.leftArm.xRot, -1.1F - .25F * MathHelper.sin(t * .8F));
        } else if (action == 3 || action == 7 || action == 13) {
            model.body.xRot += .25F * blend;
            model.rightLeg.xRot = MathHelper.lerp(blend, model.rightLeg.xRot, -.5F);
            model.leftLeg.xRot = MathHelper.lerp(blend, model.leftLeg.xRot, .35F);
            model.rightArm.xRot = MathHelper.lerp(blend, model.rightArm.xRot, action == 3 ? -.5F : -1.1F);
            model.leftArm.xRot = MathHelper.lerp(blend, model.leftArm.xRot, action == 3 ? -.5F : -1.1F);
        }
        model.leftSleeve.copyFrom(model.leftArm); model.rightSleeve.copyFrom(model.rightArm);
        model.leftPants.copyFrom(model.leftLeg); model.rightPants.copyFrom(model.rightLeg); model.jacket.copyFrom(model.body);
    }
    private static final class NailRenderer extends EntityRenderer<TuskProjectile> {
        NailRenderer(EntityRendererManager manager) { super(manager); }
        @Override public void render(TuskProjectile e, float yaw, float partial, MatrixStack stack, IRenderTypeBuffer buffers, int light) {
            stack.pushPose(); float time = e.tickCount + partial;
            if (e.kind() == TuskProjectile.HOLE) {
                Direction face = e.face();
                if (face == Direction.DOWN) stack.mulPose(Vector3f.XP.rotationDegrees(180));
                else if (face == Direction.NORTH) stack.mulPose(Vector3f.XP.rotationDegrees(-90));
                else if (face == Direction.SOUTH) stack.mulPose(Vector3f.XP.rotationDegrees(90));
                else if (face == Direction.WEST) stack.mulPose(Vector3f.ZP.rotationDegrees(90));
                else if (face == Direction.EAST) stack.mulPose(Vector3f.ZP.rotationDegrees(-90));
                TuskMesh.portal(stack, buffers, time, .43F, Math.min(1, time / 5));
            } else {
                Vector3d d = e.getDeltaMovement().normalize();
                stack.mulPose(Vector3f.YP.rotationDegrees((float) Math.toDegrees(Math.atan2(d.x, d.z))));
                stack.mulPose(Vector3f.XP.rotationDegrees((float) -Math.toDegrees(Math.asin(MathHelper.clamp(d.y, -1, 1)))));
                TuskMesh.nail(stack, buffers, e.act(), time, e.kind() == TuskProjectile.INFINITE);
            }
            stack.popPose();
        }
        @Override public ResourceLocation getTextureLocation(TuskProjectile e) { return TEXTURES[0]; }
    }
    private static final class EffectRenderer extends EntityRenderer<TuskEffect> {
        EffectRenderer(EntityRendererManager manager) { super(manager); }
        @Override public void render(TuskEffect e, float yaw, float partial, MatrixStack stack, IRenderTypeBuffer buffers, int light) {
            float age = e.age(partial), fade = TuskMesh.smooth(age / 4) * (1 - TuskMesh.smooth((age - e.life() + 6) / 6));
            if (e.kind() == TuskEffect.PORTAL) TuskMesh.portal(stack, buffers, age, e.size() * fade, fade);
            else if (e.kind() == TuskEffect.ROTATION) {
                TuskMesh.Sink s = TuskMesh.sink(stack, buffers, true);
                TuskMesh.helix(s, .75, e.size(), 3.5, age * .19F, TuskMesh.GOLD, fade * .6F);
                TuskMesh.helix(s, .85, e.size(), 2.5, -age * .12F, TuskMesh.PINK, fade * .3F);
                TuskMesh.ring(s, 1, .025, .04, TuskMesh.GOLD, fade * .5F);
            } else TuskMesh.impact(stack, buffers, age, e.life(), e.size());
        }
        @Override public ResourceLocation getTextureLocation(TuskEffect e) { return TEXTURES[0]; }
    }
    private TuskClient() {}
}
